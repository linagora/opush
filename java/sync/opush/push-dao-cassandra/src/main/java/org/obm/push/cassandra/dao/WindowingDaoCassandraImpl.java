/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package org.obm.push.cassandra.dao;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.obm.push.cassandra.dao.CassandraStructure.Windowing.Columns.CHANGE_INDEX;
import static org.obm.push.cassandra.dao.CassandraStructure.Windowing.Columns.CHANGE_TYPE;
import static org.obm.push.cassandra.dao.CassandraStructure.Windowing.Columns.CHANGE_VALUE;
import static org.obm.push.cassandra.dao.CassandraStructure.Windowing.Columns.ID;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex.Columns.COLLECTION_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex.Columns.DEVICE_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex.Columns.SYNC_KEY;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex.Columns.USER;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex.Columns.WINDOWING_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex.Columns.WINDOWING_INDEX;

import java.util.UUID;

import org.obm.push.bean.SyncKey;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.json.JSONService;
import org.obm.push.mail.EmailChanges;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.WindowingKey;
import org.obm.push.store.WindowingDao;
import org.slf4j.Logger;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class WindowingDaoCassandraImpl extends AbstractCassandraDao implements WindowingDao, CassandraStructure {

	private static final int ONLY_ONE_ITEM = 1;
	private static final int STARTING_WINDOWING_INDEX = 0;

	@Inject  
	@VisibleForTesting WindowingDaoCassandraImpl(Session session, JSONService jsonService, @Named(LoggerModule.CASSANDRA)Logger logger) {
		super(session, jsonService, logger);
	}

	@Override
	public EmailChanges popNextPendingElements(WindowingKey key, int maxSize, SyncKey newSyncKey) {
		Preconditions.checkArgument(key != null);
		Preconditions.checkArgument(maxSize > 0);
		Preconditions.checkArgument(newSyncKey != null);

		ResultSet windowingIndexResultSet = selectWindowingIndex(key);
		if (windowingIndexResultSet.isExhausted()) {
			logger.debug("No windowing index found, returning empty EmailChanges");
			return EmailChanges.builder().build();
		}
		return popChanges(key, maxSize, newSyncKey, windowingIndexResultSet.one());
	}

	@Override
	public void pushPendingElements(WindowingKey key, SyncKey newSyncKey, EmailChanges changes, int windowSize) {
		insertNewIndex(key, key.getSyncKey(), insertWindowingChanges(changes), STARTING_WINDOWING_INDEX);
	}

	@Override
	public boolean hasPendingElements(WindowingKey key) {
		ResultSet resultSetIndex = selectWindowingIndex(key);
		if (resultSetIndex.isExhausted()) {
			logger.debug("No current windowing");
			return false;
		}
		return true;
	}

	private ResultSet selectWindowingIndex(WindowingKey key) {
		Where statement = select(WINDOWING_ID, WINDOWING_INDEX)
			.from(WindowingIndex.TABLE)
			.where(eq(USER, key.getUser().getLoginAtDomain()))
			.and(eq(DEVICE_ID, key.getDeviceId().getDeviceId()))
			.and(eq(COLLECTION_ID, key.getCollectionId()))
			.and(eq(SYNC_KEY, UUID.fromString(key.getSyncKey().getSyncKey())));
		logger.debug("Select windowing index query: {}", statement.getQueryString());
		return session.execute(statement);
	}

	private EmailChanges popChanges(WindowingKey key, int maxSize, SyncKey newSyncKey, Row indexRow) {
		UUID windowingId = indexRow.getUUID(WINDOWING_ID);
		int windowingIndex = indexRow.getInt(WINDOWING_INDEX);
		logger.debug("Windowing index found Id:{} Index:{}", windowingId, windowingIndex);
		
		EmailChanges emailChanges = buildEmailChanges(maxSize, windowingId, windowingIndex);
		insertNewIndex(key, newSyncKey, windowingId, windowingIndex, emailChanges);
		return emailChanges;
	}

	private void insertNewIndex(WindowingKey key, SyncKey newSyncKey, UUID windowingId, int windowingIndex, EmailChanges emailChanges) {
		int nextWindowingIndex = windowingIndex + emailChanges.sumOfChanges();
		if (hasNextIndex(windowingId, nextWindowingIndex)) {
			insertNewIndex(key, newSyncKey, windowingId, nextWindowingIndex);
		}
	}

	private ResultSet selectChanges(int maxSize, UUID windowingId, int windowingIndex) {
		Select statement = select(CHANGE_TYPE, CHANGE_VALUE)
				.from(Windowing.TABLE)
				.where(eq(ID, windowingId))
				.and(gte(CHANGE_INDEX, windowingIndex))
				.limit(maxSize);
		logger.debug("Select changes query: {}", statement.getQueryString());
		return session.execute(statement);
	}

	private EmailChanges buildEmailChanges(int maxSize, UUID windowingId, int windowingIndex) {
		return buildEmailChanges(selectChanges(maxSize, windowingId, windowingIndex).all());
	}
	
	@VisibleForTesting EmailChanges buildEmailChanges(Iterable<Row> changeRows) {
		EmailChanges.Builder emailChangesBuilder = EmailChanges.builder();
		
		for (Row changeRow : changeRows) {
			String changeType = changeRow.getString(CHANGE_TYPE);
			String changeValueAsJson = changeRow.getString(CHANGE_VALUE);
			logger.debug("Windowing change found {} {}", changeType, changeValueAsJson);
			putChange(emailChangesBuilder, changeType, changeValueAsJson);
		}
		return emailChangesBuilder.build();
	}

	@VisibleForTesting void putChange(EmailChanges.Builder emailChangesBuilder, String changeTypeValue, String changeValueAsJson) {
		ChangeType changeType = ChangeType.fromValue(changeTypeValue);
		if (changeType == null) {
			logger.warn("Discarding a change, its ChangeType is unknown: {}", changeTypeValue);
			return;
		}
		
		Email email = jsonService.deserialize(Email.class, changeValueAsJson);
		switch (changeType) {
			case ADD:
				emailChangesBuilder.addition(email);
				break;
			case CHANGE:
				emailChangesBuilder.change(email);
				break;
			case DELETE:
				emailChangesBuilder.deletion(email);
				break;
		}
	}

	private boolean hasNextIndex(UUID windowingId, int newWindowingIndex) {
		ResultSet resultSet = selectChanges(ONLY_ONE_ITEM, windowingId, newWindowingIndex);
		return !resultSet.isExhausted();
	}

	private void insertNewIndex(WindowingKey key, SyncKey newSyncKey, UUID windowingId, int newWindowingIndex) {
		Insert statement = insertInto(WindowingIndex.TABLE)
			.value(USER, key.getUser().getLoginAtDomain())
			.value(DEVICE_ID, key.getDeviceId().getDeviceId())
			.value(COLLECTION_ID, key.getCollectionId())
			.value(SYNC_KEY, UUID.fromString(newSyncKey.getSyncKey()))
			.value(WINDOWING_ID, windowingId)
			.value(WINDOWING_INDEX, newWindowingIndex);
		logger.debug("Inserting {}", statement.getQueryString());
		session.execute(statement);
	}

	private UUID insertWindowingChanges(EmailChanges changes) {
		BatchStatement batch = new BatchStatement(Type.LOGGED);
		UUID windowingUUID = UUID.randomUUID();
		int index = STARTING_WINDOWING_INDEX;
		
		for (Email email : changes.additions()) {
			addInsertStatementInBatch(batch, windowingUUID, index++, email, ChangeType.ADD);
		}
		for (Email email : changes.changes()) {
			addInsertStatementInBatch(batch, windowingUUID, index++, email, ChangeType.CHANGE);
		}
		for (Email email : changes.deletions()) {
			addInsertStatementInBatch(batch, windowingUUID, index++, email, ChangeType.DELETE);
		}
		session.execute(batch);
		return windowingUUID;
	}

	private void addInsertStatementInBatch(BatchStatement batch, UUID windowingUUID, int changeIndex, Email email, ChangeType changeType) {
		Insert statement = insertInto(Windowing.TABLE)
			.value(ID, windowingUUID)
			.value(CHANGE_INDEX, changeIndex)
			.value(CHANGE_TYPE, changeType.asValue())
			.value(CHANGE_VALUE, jsonService.serialize(email));
		logger.debug("Inserting in batch {} the change {}", batch , statement.getQueryString());
		batch.add(statement);
	}
}
