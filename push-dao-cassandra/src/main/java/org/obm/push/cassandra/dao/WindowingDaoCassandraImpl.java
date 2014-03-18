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
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex.Columns.WINDOWING_KIND;

import java.util.List;
import java.util.UUID;

import org.obm.breakdownduration.bean.Watch;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.change.WindowingChanges;
import org.obm.push.bean.change.WindowingChangesBuilder;
import org.obm.push.bean.change.WindowingItem;
import org.obm.push.bean.change.WindowingKey;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.json.JSONService;
import org.obm.push.store.WindowingDao;
import org.slf4j.Logger;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
@Watch(BreakdownGroups.CASSANDRA)
public class WindowingDaoCassandraImpl extends AbstractCassandraDao implements WindowingDao, CassandraStructure, CassandraDao {
	
	private static final int NO_PENDING_CHANGES = 0;
	private static final int ONLY_ONE_ITEM = 1;
	private static final int STARTING_WINDOWING_INDEX = 0;
	
	@Inject  
	@VisibleForTesting WindowingDaoCassandraImpl(Session session, JSONService jsonService,
			@Named(LoggerModule.CASSANDRA)Logger logger) {
		super(session, jsonService, logger);
	}

	@Override
	public <T extends WindowingItem> WindowingChangesBuilder<T> 
			popNextChanges(WindowingKey key, int maxSize, SyncKey newSyncKey, WindowingChangesBuilder<T> changesBuilder) {
		Preconditions.checkArgument(key != null);
		Preconditions.checkArgument(maxSize > 0);
		Preconditions.checkArgument(newSyncKey != null);
		Preconditions.checkArgument(changesBuilder != null);

		ResultSet windowingIndexResultSet = selectWindowingIndex(key);
		if (windowingIndexResultSet.isExhausted()) {
			logger.debug("No windowing index found, returning the given builder");
			return changesBuilder;
		} else {
			return popChanges(key, maxSize, newSyncKey, windowingIndexResultSet.one(), changesBuilder);
		}
	}

	@Override
	public <T extends WindowingItem> void pushPendingChanges(WindowingKey key, SyncKey newSyncKey, WindowingChanges<T> changes, PIMDataType kind, int windowSize) {
		insertNewIndex(key, key.getSyncKey(), insertWindowingChanges(changes), kind, STARTING_WINDOWING_INDEX);
	}

	@Override
	public boolean hasPendingChanges(WindowingKey key) {
		ResultSet resultSetIndex = selectWindowingIndex(key);
		if (resultSetIndex.isExhausted()) {
			logger.debug("No current windowing");
			return false;
		}
		return true;
	}

	private ResultSet selectWindowingIndex(WindowingKey key) {
		Where statement = select(WINDOWING_ID, WINDOWING_KIND, WINDOWING_INDEX)
			.from(WindowingIndex.TABLE.get())
			.where(eq(USER, key.getUser().getLoginAtDomain()))
			.and(eq(DEVICE_ID, key.getDeviceId().getDeviceId()))
			.and(eq(COLLECTION_ID, key.getCollectionId()))
			.and(eq(SYNC_KEY, UUID.fromString(key.getSyncKey().getSyncKey())));
		logger.debug("Select windowing index query: {}", statement.getQueryString());
		return session.execute(statement);
	}

	private <T extends WindowingItem> WindowingChangesBuilder<T> popChanges(
			WindowingKey key, int maxSize, SyncKey newSyncKey, Row indexRow, WindowingChangesBuilder<T> changesBuilder) {
		
		UUID windowingId = indexRow.getUUID(WINDOWING_ID);
		PIMDataType windowingDataType = recognizeKind(indexRow.getString(WINDOWING_KIND));
		int windowingIndex = indexRow.getInt(WINDOWING_INDEX);
		logger.debug("Windowing index found Id:{} Kind:{} Index:{}", windowingId, windowingDataType, windowingIndex);
		
		int changesCount = putChangesInBuilder(maxSize, windowingId, windowingIndex, changesBuilder);
		insertNewIndex(key, newSyncKey, windowingId, windowingDataType, windowingIndex, changesCount);
		return changesBuilder;
	}

	private PIMDataType recognizeKind(String type) {
		PIMDataType dataType = PIMDataType.recognizeDataType(type);
		Preconditions.checkArgument(!PIMDataType.UNKNOWN.equals(dataType));
		return dataType;
	}

	private void insertNewIndex(WindowingKey key, SyncKey newSyncKey, UUID windowingId,
			PIMDataType windowingKind, int windowingIndex, int changesCount) {
		
		int nextWindowingIndex = windowingIndex + changesCount;
		if (hasNextIndex(windowingId, nextWindowingIndex)) {
			insertNewIndex(key, newSyncKey, windowingId, windowingKind, nextWindowingIndex);
		}
	}

	private ResultSet selectChanges(int maxSize, UUID windowingId, int windowingIndex) {
		Select statement = select(CHANGE_TYPE, CHANGE_VALUE)
				.from(Windowing.TABLE.get())
				.where(eq(ID, windowingId))
				.and(gte(CHANGE_INDEX, windowingIndex))
				.limit(maxSize);
		logger.debug("Select changes query: {}", statement.getQueryString());
		return session.execute(statement);
	}

	private <T extends WindowingItem> int 
			putChangesInBuilder(int maxSize, UUID windowingId, int windowingIndex, WindowingChangesBuilder<T> changesBuilder) {
		
		List<Row> fittingChanges = selectChanges(maxSize, windowingId, windowingIndex).all();
		putChangesInBuilder(fittingChanges, changesBuilder);
		return fittingChanges.size();
	}
	
	@VisibleForTesting <T extends WindowingItem> void 
			putChangesInBuilder(Iterable<Row> changeRows, WindowingChangesBuilder<T> changesBuilder) {
		
		for (Row changeRow : changeRows) {
			String changeType = changeRow.getString(CHANGE_TYPE);
			String changeValueAsJson = changeRow.getString(CHANGE_VALUE);
			logger.debug("Windowing change found {} {}", changeType, changeValueAsJson);

			T changeValue = jsonService.deserialize(changesBuilder.getPIMDataClass(), changeValueAsJson);
			putChange(changesBuilder, changeType, changeValue);
		}
	}

	@VisibleForTesting <T extends WindowingItem> void 
			putChange(WindowingChangesBuilder<T> builder, String changeTypeValue, T changeValue) {
		
		ChangeType changeType = ChangeType.fromValue(changeTypeValue);
		if (changeType == null) {
			logger.warn("Discarding a change, its ChangeType is unknown: {}", changeTypeValue);
			return;
		}
		
		switch (changeType) {
			case ADD:
				builder.addition(changeValue);
				break;
			case CHANGE:
				builder.change(changeValue);
				break;
			case DELETE:
				builder.deletion(changeValue);
				break;
		}
	}

	private boolean hasNextIndex(UUID windowingId, int newWindowingIndex) {
		ResultSet resultSet = selectChanges(ONLY_ONE_ITEM, windowingId, newWindowingIndex);
		return !resultSet.isExhausted();
	}

	private void insertNewIndex(WindowingKey key, SyncKey newSyncKey, UUID windowingId, 
			PIMDataType windowingKind, int newWindowingIndex) {
		
		Insert statement = insertInto(WindowingIndex.TABLE.get())
			.value(USER, key.getUser().getLoginAtDomain())
			.value(DEVICE_ID, key.getDeviceId().getDeviceId())
			.value(COLLECTION_ID, key.getCollectionId())
			.value(SYNC_KEY, UUID.fromString(newSyncKey.getSyncKey()))
			.value(WINDOWING_ID, windowingId)
			.value(WINDOWING_KIND, windowingKind.asXmlValue())
			.value(WINDOWING_INDEX, newWindowingIndex);
		logger.debug("Inserting {}", statement.getQueryString());
		session.execute(statement);
	}

	private UUID insertWindowingChanges(WindowingChanges<?> changes) {
		BatchStatement batch = new BatchStatement(Type.LOGGED);
		UUID windowingUUID = UUID.randomUUID();
		int index = STARTING_WINDOWING_INDEX;
		
		for (WindowingItem item : changes.additions()) {
			addInsertStatementInBatch(batch, windowingUUID, index++, item, ChangeType.ADD);
		}
		for (WindowingItem item : changes.changes()) {
			addInsertStatementInBatch(batch, windowingUUID, index++, item, ChangeType.CHANGE);
		}
		for (WindowingItem item : changes.deletions()) {
			addInsertStatementInBatch(batch, windowingUUID, index++, item, ChangeType.DELETE);
		}
		session.execute(batch);
		return windowingUUID;
	}

	private void addInsertStatementInBatch(BatchStatement batch, UUID windowingUUID, int changeIndex, WindowingItem item, ChangeType changeType) {
		Insert statement = insertInto(Windowing.TABLE.get())
			.value(ID, windowingUUID)
			.value(CHANGE_INDEX, changeIndex)
			.value(CHANGE_TYPE, changeType.asValue())
			.value(CHANGE_VALUE, jsonService.serialize(item));
		logger.debug("Inserting in batch {} the change {}", batch , statement.getQueryString());
		batch.add(statement);
	}

	@Override
	public long countPendingChanges(WindowingKey windowingKey) {
		ResultSet windowingIndexResultSet = selectWindowingIndex(windowingKey);
		if (windowingIndexResultSet.isExhausted()) {
			logger.debug("No pending windowing, returning {} changes", NO_PENDING_CHANGES);
			return NO_PENDING_CHANGES;
		}
		
		Row indexRow = windowingIndexResultSet.one();
		UUID windowingId = indexRow.getUUID(WINDOWING_ID);
		PIMDataType windowingDataType = recognizeKind(indexRow.getString(WINDOWING_KIND));
		int windowingIndex = indexRow.getInt(WINDOWING_INDEX);
		logger.debug("Windowing index found Id:{} Kind:{} Index:{}", windowingId, windowingDataType, windowingIndex);
		
		Statement statement = select().countAll()
				.from(Windowing.TABLE.get())
				.where(eq(ID, windowingId))
				.and(gte(CHANGE_INDEX, windowingIndex));
		ResultSet resultSet = session.execute(statement);
		if (resultSet.isExhausted()) {
			return NO_PENDING_CHANGES;
		}
		return getCountSelectOnlyValue(resultSet);
	}
}
