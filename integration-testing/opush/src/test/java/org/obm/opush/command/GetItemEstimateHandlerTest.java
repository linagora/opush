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
package org.obm.opush.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Files;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.Users;
import org.obm.opush.command.sync.SyncTestUtils;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.GetItemEstimateStatus;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.HierarchyChangedException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.mail.exception.FilterTypeChangedException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.utils.DateUtils;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.beans.GetItemEstimateSingleFolderResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

@RunWith(GuiceRunner.class)
@GuiceModule(GetIemEstimateTestModule.class)
public class GetItemEstimateHandlerTest {

	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private SyncTestUtils syncTestUtils;
	@Inject private IContentsExporter contentsExporterBackend;
	
	private CloseableHttpClient httpClient;

	@Before
	public void init() throws Exception {
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
	}
	
	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		Files.delete(configuration.dataDir);
		httpClient.close();
	}

	@Test
	public void testGetItemEstimateWithValidCollectionAndSyncKey() throws Exception {
		SyncKey syncKey = new SyncKey("0dca9f9b-d9af-4840-bf28-d30476dfbe12");
		ItemSyncState expectedSyncState = ItemSyncState.builder().syncDate(DateUtils.getCurrentDate()).syncKey(syncKey).build();
		CollectionId collectionId = CollectionId.of(15105);
		Set<CollectionId> existingCollections = Sets.newHashSet(collectionId);
		mockAccessAndStateThenStart(existingCollections, syncKey, expectedSyncState);
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		
		GetItemEstimateSingleFolderResponse response =
				opClient.getItemEstimateOnMailFolder(syncKey, collectionId);

		assertThat(response.getStatus()).isEqualTo(GetItemEstimateStatus.OK);
		assertThat(response.getCollectionId()).isEqualTo(collectionId);
		assertThat(response.getEstimate()).isEqualTo(0);
	}

	@Test
	public void testGetItemEstimateWithUnexistingCollection() throws Exception {
		SyncKey syncKey = new SyncKey("0dca9f9b-d9af-4840-bf28-d30476dfbe12");
		ItemSyncState expectedSyncState = ItemSyncState.builder().syncDate(DateUtils.getCurrentDate()).syncKey(syncKey).build();
		CollectionId unexistingCollectionId = CollectionId.of(15105);
		Set<CollectionId> existingCollections = Collections.<CollectionId>emptySet();
		mockAccessAndStateThenStart(existingCollections, syncKey, expectedSyncState);
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		
		GetItemEstimateSingleFolderResponse response =
				opClient.getItemEstimateOnMailFolder(syncKey, unexistingCollectionId);

		assertThat(response.getStatus()).isEqualTo(GetItemEstimateStatus.INVALID_COLLECTION);
		assertThat(response.getCollectionId()).isNull();
		assertThat(response.getEstimate()).isNull();
	}

	@Test
	public void testGetItemEstimateWithInvalidSyncKey() throws Exception {
		SyncKey invalidSyncKey = new SyncKey("0dca9f9b-d9af-4840-bf28-d30476dfbe12");
		ItemSyncState expectedSyncState = null;
		CollectionId collectionId = CollectionId.of(15105);
		Set<CollectionId> existingCollections = Sets.newHashSet(collectionId);
		mockAccessAndStateThenStart(existingCollections, invalidSyncKey, expectedSyncState);
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		
		GetItemEstimateSingleFolderResponse response =
				opClient.getItemEstimateOnMailFolder(invalidSyncKey, collectionId);

		assertThat(response.getStatus()).isEqualTo(GetItemEstimateStatus.INVALID_SYNC_KEY);
		assertThat(response.getCollectionId()).isEqualTo(collectionId);
		assertThat(response.getEstimate()).isNull();
	}

	private void mockAccessAndStateThenStart(Set<CollectionId> existingCollections, SyncKey syncKey, ItemSyncState syncState)
			throws Exception {
		testUtils.expectSyncState(syncKey, syncState);

		DataDelta delta = DataDelta.builder().syncDate(new Date()).syncKey(syncKey).build();
		syncTestUtils.mockEmailSyncClasses(syncKey, existingCollections, delta, Arrays.asList(users.jaures));
		mocksControl.replay();
		opushServer.start();
	}

	@Test
	public void testGetItemEstimateWithHierarchyChangedException() throws Exception {
		SyncKey syncKey = new SyncKey("0dca9f9b-d9af-4840-bf28-d30476dfbe12");
		ItemSyncState syncState = ItemSyncState.builder().syncDate(DateUtils.getCurrentDate()).syncKey(syncKey).build();
		CollectionId collectionId = CollectionId.of(15105);
		Set<CollectionId> syncEmailCollectionsIds = Sets.newHashSet(collectionId);
		
		testUtils.expectSyncState(syncKey, syncState);

		userAccessUtils.mockUsersAccess(users.jaures);
		
		mockEmailSyncWithHierarchyChangedException(syncKey, syncEmailCollectionsIds);
		
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		
		GetItemEstimateSingleFolderResponse response =
				opClient.getItemEstimateOnMailFolder(syncKey, collectionId);

		assertThat(response.getStatus()).isEqualTo(GetItemEstimateStatus.INVALID_COLLECTION);
		assertThat(response.getCollectionId()).isNull();
		assertThat(response.getEstimate()).isNull();
	}

	private void mockEmailSyncWithHierarchyChangedException(SyncKey syncKey, Set<CollectionId> syncEmailCollectionsIds)
			throws DaoException, ConversionException, FilterTypeChangedException {

		testUtils.expectUserCollectionsNeverChange(users.jaures, syncEmailCollectionsIds);
		syncTestUtils.mockCollectionDaoForEmailSync(syncKey, syncEmailCollectionsIds);
		syncTestUtils.mockItemTrackingDao();
		
		expect(contentsExporterBackend.getItemEstimateSize(
				anyObject(UserDataRequest.class), 
				anyObject(PIMDataType.class),
				anyObject(AnalysedSyncCollection.class),
				anyObject(ItemSyncState.class)))
			.andThrow(new HierarchyChangedException(new NotAllowedException("Not allowed")));
	}
}
