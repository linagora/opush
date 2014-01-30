/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
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
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.obm.push.cassandra.dao.CassandraStructure.SnapshotIndex.Columns.COLLECTION_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.SnapshotIndex.Columns.DEVICE_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.SnapshotIndex.Columns.SNAPSHOT_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.SnapshotIndex.Columns.SYNC_KEY;
import static org.obm.push.cassandra.dao.CassandraStructure.SnapshotTable.Columns.ID;
import static org.obm.push.cassandra.dao.CassandraStructure.SnapshotTable.Columns.SNAPSHOT;

import java.util.UUID;

import org.obm.push.bean.DeviceId;
import org.obm.push.bean.SyncKey;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.json.JSONService;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.store.SnapshotDao;
import org.slf4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class SnapshotDaoCassandraImpl extends AbstractCassandraDao implements SnapshotDao, CassandraStructure {

	@Inject  
	@VisibleForTesting SnapshotDaoCassandraImpl(Session session, JSONService jsonService, @Named(LoggerModule.CASSANDRA)Logger logger) {
		super(session, jsonService, logger);
	}

	@Override
	public Snapshot get(DeviceId deviceId, SyncKey syncKey, Integer collectionId) {
		ResultSet snapshotIndexResultSet = selectSnapshotIndex(deviceId, syncKey, collectionId);
		if (snapshotIndexResultSet.isExhausted()) {
			logger.debug("No snapshot index found, returning null");
			return null;
		}
		
		UUID snapshotId  = snapshotIndexResultSet.one().getUUID(SNAPSHOT_ID);
		ResultSet snapshotResultSet = selectSnapshot(snapshotId);
		if (snapshotResultSet.isExhausted()) {
			logger.debug("No snapshot found, returning null");
			return null;
		}
		
		String snapshotAsJson = snapshotResultSet.one().getString(SNAPSHOT);
		return jsonService.deserialize(Snapshot.class, snapshotAsJson);
	}

	@Override
	public void put(Snapshot snapshot) {
		Preconditions.checkNotNull(snapshot);
		insertNewIndex(snapshot, insertSnapshot(snapshot));
	}

	private ResultSet selectSnapshotIndex(DeviceId deviceId, SyncKey syncKey, Integer collectionId) {
		Where statement = select(SNAPSHOT_ID).from(SnapshotIndex.TABLE)
				.where(eq(DEVICE_ID, deviceId.getDeviceId()))
				.and(eq(COLLECTION_ID, collectionId))
				.and(eq(SYNC_KEY, UUID.fromString(syncKey.getSyncKey())));
		logger.debug("Select snapshot index query: {}", statement.getQueryString());
		return session.execute(statement);
	}

	private ResultSet selectSnapshot(UUID snapshotId) {
		Where statement = select(SNAPSHOT).from(SnapshotTable.TABLE)
				.where(eq(ID, snapshotId));
		logger.debug("Select snapshot query: {}", statement.getQueryString());
		return session.execute(statement);
	}

	private UUID insertSnapshot(Snapshot snapshot) {
		UUID snapshotId = UUID.randomUUID();
		Insert statement = insertInto(SnapshotTable.TABLE)
				.value(ID, snapshotId)
				.value(SNAPSHOT, jsonService.serialize(snapshot));
		logger.debug("Inserting {}", statement.getQueryString());
		session.execute(statement);
		return snapshotId;
	}

	private void insertNewIndex(Snapshot snapshot, UUID snapshotId) {
		Insert statement = insertInto(SnapshotIndex.TABLE)
			.value(DEVICE_ID, snapshot.getDeviceId().getDeviceId())
			.value(COLLECTION_ID, snapshot.getCollectionId())
			.value(SYNC_KEY, UUID.fromString(snapshot.getSyncKey().getSyncKey()))
			.value(SNAPSHOT_ID, snapshotId);
		logger.debug("Inserting {}", statement.getQueryString());
		session.execute(statement);
	}
}
