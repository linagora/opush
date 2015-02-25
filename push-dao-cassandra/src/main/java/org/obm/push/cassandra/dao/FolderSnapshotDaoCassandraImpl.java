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
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.obm.push.cassandra.dao.CassandraStructure.FolderSnapshot.TABLE;
import static org.obm.push.cassandra.dao.CassandraStructure.FolderSnapshot.Columns.DEVICE_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.FolderSnapshot.Columns.FOLDERS;
import static org.obm.push.cassandra.dao.CassandraStructure.FolderSnapshot.Columns.NEXT_COLLECTION_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.FolderSnapshot.Columns.SYNC_KEY;
import static org.obm.push.cassandra.dao.CassandraStructure.FolderSnapshot.Columns.USER;

import java.util.Iterator;
import java.util.Set;

import org.obm.breakdownduration.bean.Watch;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.Device;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.User;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.cassandra.dao.CassandraStructure.FolderMapping;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.json.JSONService;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.FolderSyncKey;
import org.slf4j.Logger;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
@Watch(BreakdownGroups.CASSANDRA)
public class FolderSnapshotDaoCassandraImpl extends AbstractCassandraDao implements FolderSnapshotDao, CassandraDao {
	
	@Inject  
	@VisibleForTesting FolderSnapshotDaoCassandraImpl(Provider<Session> sessionProvider, JSONService jsonService,
			@Named(LoggerModule.CASSANDRA)Logger logger) {
		super(sessionProvider, jsonService, logger);
	}

	@Override
	public void create(User user, Device device, FolderSyncKey folderSyncKey, FolderSnapshot snapshot)
			throws DaoException {
		
		BatchStatement batch = new BatchStatement(Type.LOGGED);
		
		insertSnapshot(batch, user, device, folderSyncKey, snapshot);
		insertMapping(batch, user, device, snapshot.getFolders());

		logger.debug("Executing batch of size: {}", batch.size());
		getSession().execute(batch);
	}

	private void insertSnapshot(BatchStatement batch, User user, Device device, 
			FolderSyncKey folderSyncKey, FolderSnapshot snapshot) {
		Insert query = insertInto(TABLE.get())
			.value(USER, user.getLoginAtDomain())
			.value(DEVICE_ID, device.getDevId().getDeviceId())
			.value(SYNC_KEY, folderSyncKey.asUUID())
			.value(NEXT_COLLECTION_ID, snapshot.getNextId())
			.value(FOLDERS, jsonService.serializeSet(snapshot.getFolders()));
		logger.debug("Batch will insert snapshot {}", query.getQueryString());
		batch.add(query);
	}

	private void insertMapping(BatchStatement batch, User user, Device device, Set<Folder> folders) {
		for (Folder folder : folders) {
			Insert query = insertInto(FolderMapping.TABLE.get())
				.value(FolderMapping.Columns.USER, user.getLoginAtDomain())
				.value(FolderMapping.Columns.DEVICE_ID, device.getDevId().getDeviceId())
				.value(FolderMapping.Columns.COLLECTION_ID, folder.getCollectionId().asInt())
				.value(FolderMapping.Columns.DATA_TYPE, folder.getFolderType().getPIMDataType().asXmlValue())
				.value(FolderMapping.Columns.BACKEND_ID, jsonService.serialize(folder.getBackendId()))
				.value(FolderMapping.Columns.FOLDER, jsonService.serialize(folder));
			logger.debug("Batch will insert mapping {}", query.getQueryString());
			batch.add(query);
		}
	}

	@Override
	public FolderSnapshot get(User user, Device device, FolderSyncKey folderSyncKey)
			throws DaoException, FolderSnapshotNotFoundException {

		Where query = select(NEXT_COLLECTION_ID, FOLDERS)
			.from(TABLE.get())
			.where(eq(USER, user.getLoginAtDomain()))
			.and(eq(DEVICE_ID, device.getDevId().getDeviceId()))
			.and(eq(SYNC_KEY, folderSyncKey.asUUID()));
		
		logger.debug("Getting {}", query.getQueryString());
		ResultSet resultSet = getSession().execute(query);
		if (resultSet.isExhausted()) {
			throw new FolderSnapshotNotFoundException("No result found for the request: " + query.getQueryString());
		}

		Row row = resultSet.one();
		return FolderSnapshot
				.nextId(row.getInt(NEXT_COLLECTION_ID))
				.folders(jsonService.deserializeSet(Folder.class, row.getSet(FOLDERS, String.class)));
	}

	@Override
	public Folder get(User user, Device device, CollectionId collectionId) throws CollectionNotFoundException {
		Where query = select(FolderMapping.Columns.FOLDER)
			.from(FolderMapping.TABLE.get())
			.where(eq(FolderMapping.Columns.USER, user.getLoginAtDomain()))
			.and(eq(FolderMapping.Columns.DEVICE_ID, device.getDevId().getDeviceId()))
			.and(eq(FolderMapping.Columns.COLLECTION_ID, collectionId.asInt()));
		
		logger.debug("Getting folder {}", query.getQueryString());
		ResultSet resultSet = getSession().execute(query);
		if (resultSet.isExhausted()) {
			throw new CollectionNotFoundException("No folder found for the request: " + query.getQueryString());
		}

		return jsonService.deserialize(Folder.class, resultSet.one().getString(FolderMapping.Columns.FOLDER));
	}

	@Override
	public Folder get(User user, Device device, PIMDataType pimDataType, BackendId backendId) throws CollectionNotFoundException {
		Where query = select(FolderMapping.Columns.COLLECTION_ID, FolderMapping.Columns.FOLDER)
			.from(FolderMapping.TABLE.get())
			.allowFiltering()
			.where(eq(FolderMapping.Columns.USER, user.getLoginAtDomain()))
			.and(eq(FolderMapping.Columns.DEVICE_ID, device.getDevId().getDeviceId()))
			.and(eq(FolderMapping.Columns.DATA_TYPE, pimDataType.asXmlValue()))
			.and(eq(FolderMapping.Columns.BACKEND_ID, jsonService.serialize(backendId)));
		
		logger.debug("Getting folder {}", query.getQueryString());
		ResultSet resultSet = getSession().execute(query);
		if (resultSet.isExhausted()) {
			throw new CollectionNotFoundException("No folder found for the request: " + query.getQueryString());
		}

		Iterator<Row> results = resultSet.iterator();
		Row lastKnownRow = results.next();
		int lastKnownCollectionId = 0;
		while (results.hasNext()) {
			Row row = results.next();
			if (lastKnownCollectionId < row.getInt(FolderMapping.Columns.COLLECTION_ID)) {
				lastKnownRow = row;
			}
		}
		
		return jsonService.deserialize(Folder.class, lastKnownRow.getString(FolderMapping.Columns.FOLDER));
	}
}
