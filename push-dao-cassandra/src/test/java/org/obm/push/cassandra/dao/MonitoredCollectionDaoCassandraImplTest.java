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

import java.util.Set;

import org.cassandraunit.CassandraCQLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.cassandra.dao.CassandraStructure.V1;
import org.obm.push.dao.testsuite.MonitoredCollectionDaoTest;
import org.obm.push.json.JSONService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;

public class MonitoredCollectionDaoCassandraImplTest extends MonitoredCollectionDaoTest {

	private static final String DAO_SCHEMA = new DaoTestsSchemaProducer().schemaForDAO(MonitoredCollectionDaoCassandraImpl.class);
	@Rule public CassandraCQLUnit cassandraCQLUnit = new OpushCassandraCQLUnit(DAO_SCHEMA);
	
	private Logger logger = LoggerFactory.getLogger(MonitoredCollectionDaoCassandraImplTest.class);
	private PublicJSONService jsonService;
	private SessionProvider sessionProvider;
	private MonitoredCollectionDaoV1 monitoredCollectionDaoV1;
	
	@Before
	public void init() {
		sessionProvider = new SessionProvider(cassandraCQLUnit.session);
		jsonService = new PublicJSONService();
		monitoredCollectionDao = new MonitoredCollectionDaoCassandraImpl(sessionProvider, jsonService, logger);
		monitoredCollectionDaoV1 = new MonitoredCollectionDaoV1(jsonService, sessionProvider);
	}
	
	@Test
	public void listShouldReturnV1WhenPutDoneByV1UsingCredentials() {
		Set<AnalysedSyncCollection> collections = buildListCollection(1);
		monitoredCollectionDaoV1.put(credentials, device, collections);
		
		assertThat(monitoredCollectionDao.list(credentials, device))
			.isEqualTo(collections);
	}
	
	@Test
	public void listShouldReturnEmptyWhenEmptyPutByV2() {
		Set<AnalysedSyncCollection> collections = buildListCollection(1);
		monitoredCollectionDaoV1.put(credentials, device, collections);
		
		monitoredCollectionDao.put(user, device, ImmutableSet.<AnalysedSyncCollection>of());
		
		assertThat(monitoredCollectionDao.list(credentials, device))
			.isEmpty();
	}
	
	@Test
	public void listShouldReturnV2WhenPutDoneByV1AndV2() {
		Set<AnalysedSyncCollection> collections1 = buildListCollection(1, 2);
		Set<AnalysedSyncCollection> collections2 = buildListCollection(3, 4);
		
		monitoredCollectionDaoV1.put(credentials, device, collections1);
		monitoredCollectionDao.put(user, device, collections2);
		
		assertThat(monitoredCollectionDao.list(credentials, device))
			.isEqualTo(collections2);
	}
	
	@Test
	public void listShouldReturnV1WhenPutDoneByBothAndReadByV1() {
		Set<AnalysedSyncCollection> collections1 = buildListCollection(1, 2);
		Set<AnalysedSyncCollection> collections2 = buildListCollection(3, 4);
		
		monitoredCollectionDaoV1.put(credentials, device, collections1);
		monitoredCollectionDao.put(user, device, collections2);
		
		assertThat(monitoredCollectionDaoV1.list(credentials, device))
			.isEqualTo(collections1);
	}
	
	@Test
	public void listShouldReturnV1WhenPutDoneByV2AndReadByV1() {
		Set<AnalysedSyncCollection> collections = buildListCollection(1, 2);
		
		monitoredCollectionDao.put(user, device, collections);
		
		assertThat(monitoredCollectionDaoV1.list(credentials, device)).isEmpty();
	}
	
	public static class MonitoredCollectionDaoV1 {
		
		private final Logger logger = LoggerFactory.getLogger("DAO V1");
		private final JSONService jsonService;
		private final Provider<Session> sessionProvider;
		
		public MonitoredCollectionDaoV1(JSONService jsonService, Provider<Session> sessionProvider) {
			this.jsonService = jsonService;
			this.sessionProvider = sessionProvider;
		}

		public Set<AnalysedSyncCollection> list(Credentials credentials, Device device) {
			Where query = select(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS)
					.from(V1.MonitoredCollection.TABLE.get())
					.where(eq(V1.MonitoredCollection.Columns.CREDENTIALS, jsonService.serialize(credentials)))
					.and(eq(V1.MonitoredCollection.Columns.DEVICE, jsonService.serialize(device)));
			logger.debug("Getting {}", query.getQueryString());
			ResultSet resultSet = sessionProvider.get().execute(query);
			if (resultSet.isExhausted()) {
				logger.debug("No result found, returning empty set");
				return ImmutableSet.<AnalysedSyncCollection> of();
			}
			Set<String> jsons = resultSet.one().getSet(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, String.class);
			logger.debug("Result found {}", jsons);
			return jsonService.deserializeSet(AnalysedSyncCollection.class, jsons);
		}

		public void put(Credentials credentials, Device device, Set<AnalysedSyncCollection> collections) {
			Insert query = insertInto(V1.MonitoredCollection.TABLE.get())
					.value(V1.MonitoredCollection.Columns.CREDENTIALS, jsonService.serialize(credentials))
					.value(V1.MonitoredCollection.Columns.DEVICE, jsonService.serialize(device))
					.value(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, jsonService.serializeSet(collections));
			logger.debug("Inserting {}", query.getQueryString());
			sessionProvider.get().execute(query);
		}
	}
}
