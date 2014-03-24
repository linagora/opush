/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2013  Linagora
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
import static org.obm.push.cassandra.dao.CassandraStructure.SyncedCollection.TABLE;
import static org.obm.push.cassandra.dao.CassandraStructure.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION;
import static org.obm.push.cassandra.dao.CassandraStructure.SyncedCollection.Columns.COLLECTION_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.SyncedCollection.Columns.CREDENTIALS;
import static org.obm.push.cassandra.dao.CassandraStructure.SyncedCollection.Columns.DEVICE;

import org.obm.breakdownduration.bean.Watch;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.json.JSONService;
import org.obm.push.store.SyncedCollectionDao;
import org.slf4j.Logger;

import com.datastax.driver.core.ResultSet;
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
public class SyncedCollectionDaoCassandraImpl extends AbstractCassandraDao implements SyncedCollectionDao, CassandraStructure, CassandraDao {

	@Inject  
	@VisibleForTesting SyncedCollectionDaoCassandraImpl(Provider<Session> sessionProvider, 
			JSONService jsonService, @Named(LoggerModule.CASSANDRA)Logger logger) {
		super(sessionProvider, jsonService, logger);
	}

	@Override
	public void put(Credentials credentials, Device device, AnalysedSyncCollection collection) {
		Insert query = insertInto(TABLE.get())
				.value(CREDENTIALS, jsonService.serialize(credentials))
				.value(DEVICE, jsonService.serialize(device))
				.value(COLLECTION_ID, collection.getCollectionId())
				.value(ANALYSED_SYNC_COLLECTION, jsonService.serialize(collection));
		logger.debug("Inserting {}", query.getQueryString());
		getSession().execute(query);
	}
	
	@Override
	public AnalysedSyncCollection get(Credentials credentials, Device device, Integer collectionId) {
		Where query = select(ANALYSED_SYNC_COLLECTION).from(TABLE.get())
				.where(eq(CREDENTIALS, jsonService.serialize(credentials)))
				.and(eq(DEVICE, jsonService.serialize(device)))
				.and(eq(COLLECTION_ID, collectionId));
		logger.debug("Getting {}", query.getQueryString());
		ResultSet resultSet = getSession().execute(query);
		if (resultSet.isExhausted()) {
			logger.debug("No result found, returning null");
			return null;
		}
		String json = resultSet.one().getString(ANALYSED_SYNC_COLLECTION);
		logger.debug("Result found {}", json);
		return jsonService.deserialize(AnalysedSyncCollection.class, json);
	}
}
