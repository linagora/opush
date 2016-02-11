/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2016 Linagora
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
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingToSnapshot.TABLE;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingToSnapshot.Columns.COLLECTION_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingToSnapshot.Columns.DEVICE_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingToSnapshot.Columns.SNAPSHOT_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.WindowingToSnapshot.Columns.SYNC_KEY;

import java.util.UUID;

import org.obm.breakdownduration.bean.Watch;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.change.WindowingKey;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.exception.DaoException;
import org.obm.push.json.JSONService;
import org.obm.push.store.WindowingToSnapshotDao;
import org.slf4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
@Watch(BreakdownGroups.CASSANDRA)
public class WindowingToSnapshotDaoCassandraImpl extends AbstractCassandraDao implements WindowingToSnapshotDao, CassandraStructure, CassandraDao {
	
	@Inject
	@VisibleForTesting WindowingToSnapshotDaoCassandraImpl(Provider<Session> sessionProvider, 
			JSONService jsonService, @Named(LoggerModule.CASSANDRA)Logger logger) {
		super(sessionProvider, jsonService, logger);
	}

	@Override
	public Optional<UUID> get(WindowingKey key) {
		Where statement = select(SNAPSHOT_ID).from(TABLE.get())
				.where(eq(DEVICE_ID, key.getDeviceId().getDeviceId()))
				.and(eq(COLLECTION_ID, key.getCollectionId().asInt()))
				.and(eq(SYNC_KEY, UUID.fromString(key.getSyncKey().getSyncKey())));
		logger.debug("Selecting snapshot id query: {}", statement.getQueryString());

		ResultSet results = getSession().execute(statement);
		if (results.isExhausted()) {
			return Optional.absent();
		}
		return Optional.of(results.one().getUUID(SNAPSHOT_ID));
	}

	@Override
	public void startWindowing(WindowingKey key, UUID snapshotId) throws DaoException {
		recordSnapshotIdForKey(snapshotId, key);
	}

	@Override
	public void windowingInProgress(SyncKey newSyncKey, WindowingKey key) throws DaoException {
		Optional<UUID> snapshotId = get(key);
		if (!snapshotId.isPresent()) {
			logger.info("No snapshot found for snapshot key {}, cannot register windowing progress", key);
			return;
		}
		
		recordSnapshotIdForKey(snapshotId.get(), key.withSyncKey(newSyncKey));
	}

	private void recordSnapshotIdForKey(UUID snapshotId, WindowingKey key) {
		Insert statement = insertInto(TABLE.get())
			.value(DEVICE_ID, key.getDeviceId().getDeviceId())
			.value(COLLECTION_ID, key.getCollectionId().asInt())
			.value(SYNC_KEY, UUID.fromString(key.getSyncKey().getSyncKey()))
			.value(SNAPSHOT_ID, snapshotId);
		logger.debug("Inserting {}", statement.getQueryString());
		getSession().execute(statement);
	}

}
