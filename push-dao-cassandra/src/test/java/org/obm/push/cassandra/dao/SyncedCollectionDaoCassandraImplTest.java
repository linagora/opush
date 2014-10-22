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
import static org.assertj.core.api.Assertions.assertThat;

import org.cassandraunit.CassandraCQLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.SyncKey;
import org.obm.push.dao.testsuite.SyncedCollectionDaoTest;
import org.obm.push.json.JSONService;
import org.obm.push.protocol.bean.CollectionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.inject.Provider;

public class SyncedCollectionDaoCassandraImplTest extends SyncedCollectionDaoTest {

	private static final String DAO_SCHEMA = new DaoTestsSchemaProducer().schemaForDAO(SyncedCollectionDaoCassandraImpl.class);
	@Rule public CassandraCQLUnit cassandraCQLUnit = new OpushCassandraCQLUnit(DAO_SCHEMA);
	
	private Logger logger = LoggerFactory.getLogger(SyncedCollectionDaoCassandraImplTest.class);
	private PublicJSONService jsonService;
	private SessionProvider sessionProvider;
	private SyncedCollectionDaoV1 syncedCollectionDaoV1;
	
	@Before
	public void init() {
		sessionProvider = new SessionProvider(cassandraCQLUnit.session);
		jsonService = new PublicJSONService();
		syncedCollectionDao = new SyncedCollectionDaoCassandraImpl(sessionProvider, jsonService, logger);
		syncedCollectionDaoV1 = new SyncedCollectionDaoV1(jsonService, sessionProvider);
	}
	
	@Test
	public void getShouldReturnV1WhenPutDoneByV1UsingCredentials() {
		AnalysedSyncCollection collection = buildCollection(CollectionId.of(1), SyncKey.INITIAL_SYNC_KEY);
		syncedCollectionDaoV1.put(credentials, device, collection);
		
		assertThat(syncedCollectionDao.get(credentials, device, collection.getCollectionId()))
			.isEqualTo(collection);
	}
	
	@Test
	public void getShouldReturnV2WhenPutDoneByV1AndV2() {
		CollectionId collectionId = CollectionId.of(3);
		AnalysedSyncCollection collection1 = buildCollection(collectionId, new SyncKey("6146ebbb-e7c4-4731-84b8-f2772cc0efb0"));
		AnalysedSyncCollection collection2 = buildCollection(collectionId, new SyncKey("797bdfa3-9059-45ef-ade7-98797a14fd33"));
		
		syncedCollectionDaoV1.put(credentials, device, collection1);
		syncedCollectionDao.put(user, device, collection2);
		
		assertThat(syncedCollectionDao.get(credentials, device, collectionId))
			.isEqualTo(collection2);
	}
	
	@Test
	public void getShouldReturnV1WhenPutDoneByBothAndReadByV1() {
		CollectionId id = CollectionId.of(3);
		AnalysedSyncCollection collection1 = buildCollection(id, new SyncKey("0b7f500e-b6bc-4034-8884-ad5551fbbdbb"));
		AnalysedSyncCollection collection2 = buildCollection(id, new SyncKey("307e9aa6-58f4-4105-abac-bb8962026aa8"));
		
		syncedCollectionDaoV1.put(credentials, device, collection1);
		syncedCollectionDao.put(user, device, collection2);
		
		assertThat(syncedCollectionDaoV1.get(credentials, device, id)).isEqualTo(collection1);
	}
	
	@Test
	public void getShouldReturnV1WhenPutDoneByV2AndReadByV1() {
		CollectionId id = CollectionId.of(5);
		AnalysedSyncCollection collection = buildCollection(id, new SyncKey("3bacc1d6-ec37-4e6c-8ccd-3518477b8eba"));
		
		syncedCollectionDao.put(user, device, collection);
		
		assertThat(syncedCollectionDaoV1.get(credentials, device, id)).isNull();
	}
	
	public static class SyncedCollectionDaoV1 {
		
		private final Logger logger = LoggerFactory.getLogger("DAO V1");
		private final JSONService jsonService;
		private final Provider<Session> sessionProvider;
		
		public SyncedCollectionDaoV1(JSONService jsonService, Provider<Session> sessionProvider) {
			this.jsonService = jsonService;
			this.sessionProvider = sessionProvider;
		}

		public void put(Credentials credentials, Device device, AnalysedSyncCollection collection) {
			Insert query = insertInto(CassandraStructure.V1.SyncedCollection.TABLE.get())
					.value(CassandraStructure.V1.SyncedCollection.Columns.CREDENTIALS, jsonService.serialize(credentials))
					.value(CassandraStructure.V1.SyncedCollection.Columns.DEVICE, jsonService.serialize(device))
					.value(CassandraStructure.V1.SyncedCollection.Columns.COLLECTION_ID, collection.getCollectionId().asInt())
					.value(CassandraStructure.V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION, jsonService.serialize(collection));
			logger.debug("Inserting {}", query.getQueryString());
			sessionProvider.get().execute(query);
		}
		
		public AnalysedSyncCollection get(Credentials credentials, Device device, CollectionId collectionId) {
			Where query = select(CassandraStructure.V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION)
					.from(CassandraStructure.V1.SyncedCollection.TABLE.get())
					.where(eq(CassandraStructure.V1.SyncedCollection.Columns.CREDENTIALS, jsonService.serialize(credentials)))
					.and(eq(CassandraStructure.V1.SyncedCollection.Columns.DEVICE, jsonService.serialize(device)))
					.and(eq(CassandraStructure.V1.SyncedCollection.Columns.COLLECTION_ID, collectionId.asInt()));
			logger.debug("Getting {}", query.getQueryString());
			ResultSet resultSet = sessionProvider.get().execute(query);
			if (resultSet.isExhausted()) {
				logger.debug("No result found, returning null");
				return null;
			}
			String json = resultSet.one().getString(CassandraStructure.V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION);
			logger.debug("Result found {}", json);
			return jsonService.deserialize(AnalysedSyncCollection.class, json);
		}
	}
}
