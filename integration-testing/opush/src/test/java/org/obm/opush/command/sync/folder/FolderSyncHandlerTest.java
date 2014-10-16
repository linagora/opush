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
package org.obm.opush.command.sync.folder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;

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
import org.obm.opush.HierarchyChangesTestUtils;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.Users;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.FolderSyncStatus;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.exception.DaoException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.FolderSyncResponse;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.CollectionDao;
import org.obm.sync.push.client.OPClient;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@RunWith(GuiceRunner.class)
@GuiceModule(FolderSyncHandlerTestModule.class)
public class FolderSyncHandlerTest {
	
	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private HierarchyChangesTestUtils pushTestUtils;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private CollectionDao collectionDao;
	
	private OpushUser user;
	private CloseableHttpClient httpClient;

	@Before
	public void init() throws Exception {
		user = users.jaures;
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
		cassandraServer.start();
		httpClient = HttpClientBuilder.create().build();
	}
	
	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		Files.delete(configuration.dataDir);
		httpClient.close();
	}

	@Test
	public void testInitialFolderSyncContainsINBOX() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		FolderSyncKey newGeneratedSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");
		FolderSyncState newMappingSyncState = FolderSyncState.builder().syncKey(newGeneratedSyncKey).build();
		
		userAccessUtils.mockUsersAccess(user);
		pushTestUtils.mockHierarchyChangesOnlyInbox();
		pushTestUtils.mockNextGeneratedSyncKey(newGeneratedSyncKey);
		expectCollectionDaoAllocateFolderSyncState(newGeneratedSyncKey, newMappingSyncState);
		testUtils.expectCreateFolderMappingState();

//		CollectionDao collectionDao = classToInstanceMap.get(CollectionDao.class);
//		expectUserCollectionsNeverChange(collectionDao, fakeTestUsers, Collections.<Integer>emptySet());
//		mockCollectionDao(collectionDao, initialSyncKey, serverId);

		
		mocksControl.replay();
		
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);
		
		mocksControl.verify();
		
		assertThat(folderSyncResponse.getNewSyncKey()).isEqualTo(newGeneratedSyncKey);
		assertThat(folderSyncResponse.getStatus()).isEqualTo(FolderSyncStatus.OK);
		assertThat(folderSyncResponse.getCount()).isEqualTo(1);
		assertThat(folderSyncResponse.getCollectionsAddedAndUpdated()).hasSize(1);
		CollectionChange inbox = Iterables.getOnlyElement(folderSyncResponse.getCollectionsAddedAndUpdated());
		assertThat(inbox.getDisplayName()).isEqualTo("INBOX");
		assertThat(inbox.getFolderType()).isEqualTo(FolderType.DEFAULT_INBOX_FOLDER);
	}

	@Test
	public void testFolderSyncHasNoChange() throws Exception {
		FolderSyncKey newSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");

		FolderSyncKey newGeneratedSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");
		int newSyncStateId = 1156;
		FolderSyncState newSyncState = newFolderSyncState(newGeneratedSyncKey, newSyncStateId);
		
		userAccessUtils.mockUsersAccess(user);
		pushTestUtils.mockHierarchyChangesForMailboxes(buildHierarchyItemsChangeEmpty());
		pushTestUtils.mockNextGeneratedSyncKey(newGeneratedSyncKey);
		expectCollectionDaoFindFolderSyncState(newSyncKey, newSyncState);
		testUtils.expectCreateFolderMappingState();

		mocksControl.replay();
		
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(newSyncKey);

		mocksControl.verify();

		assertThat(folderSyncResponse.getNewSyncKey()).isEqualTo(newGeneratedSyncKey);
		assertThat(folderSyncResponse.getStatus()).isEqualTo(FolderSyncStatus.OK);
		assertThat(folderSyncResponse.getCount()).isEqualTo(0);
		assertThat(folderSyncResponse.getCollectionsAddedAndUpdated()).isEmpty();
	}
	
	@Test
	public void testFolderSyncHasChanges() throws Exception {
		FolderSyncKey newSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");

		FolderSyncKey newGeneratedSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");
		int newSyncStateId = 1156;
		FolderSyncState newSyncState = newFolderSyncState(newGeneratedSyncKey, newSyncStateId);
		
		CollectionId collectionId = CollectionId.of(4);
		CollectionId parentId = CollectionId.of(23);
		
		org.obm.push.bean.FolderType itemChangeType = org.obm.push.bean.FolderType.USER_CREATED_EMAIL_FOLDER;
		HierarchyCollectionChanges mailboxChanges = HierarchyCollectionChanges.builder()
			.changes(Lists.newArrayList(
					CollectionChange.builder()
						.collectionId(collectionId)
						.parentCollectionId(parentId)
						.displayName("aNewImapFolder")
						.folderType(itemChangeType)
						.isNew(true)
						.build()))
			.build();
//		CollectionDao collectionDao = classToInstanceMap.get(CollectionDao.class);
//		expectUserCollectionsNeverChange(collectionDao, fakeTestUsers, Collections.<Integer>emptySet());
//		mockCollectionDao(collectionDao, syncKey, serverId);
		
		userAccessUtils.mockUsersAccess(user);
		pushTestUtils.mockHierarchyChangesForMailboxes(mailboxChanges);
		pushTestUtils.mockNextGeneratedSyncKey(newGeneratedSyncKey);
		expectCollectionDaoFindFolderSyncState(newSyncKey, newSyncState);
		testUtils.expectCreateFolderMappingState();
		
		mocksControl.replay();
		
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(newSyncKey);

		mocksControl.verify();

		assertThat(folderSyncResponse.getNewSyncKey()).isEqualTo(newGeneratedSyncKey);
		assertThat(folderSyncResponse.getStatus()).isEqualTo(FolderSyncStatus.OK);
		assertThat(folderSyncResponse.getCount()).isEqualTo(1);
		assertThat(folderSyncResponse.getCollectionsAddedAndUpdated()).hasSize(1);
		CollectionChange inbox = Iterables.getOnlyElement(folderSyncResponse.getCollectionsAddedAndUpdated());
		assertThat(inbox.getDisplayName()).isEqualTo("aNewImapFolder");
		assertThat(inbox.getFolderType()).isEqualTo(FolderType.USER_CREATED_EMAIL_FOLDER);
	}

	@Test
	public void testFolderSyncHasDeletions() throws Exception {
		FolderSyncKey newSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");

		FolderSyncKey newGeneratedSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");
		int newSyncStateId = 1156;
		FolderSyncState newSyncState = newFolderSyncState(newGeneratedSyncKey, newSyncStateId);

		CollectionId collectionId = CollectionId.of(4);
		
		HierarchyCollectionChanges mailboxChanges = HierarchyCollectionChanges.builder()
			.deletions(Lists.newArrayList(
					CollectionDeletion.builder().collectionId(collectionId).build()))
			.build();
		
		userAccessUtils.mockUsersAccess(user);
		pushTestUtils.mockHierarchyChangesForMailboxes(mailboxChanges);
		pushTestUtils.mockNextGeneratedSyncKey(newGeneratedSyncKey);
		expectCollectionDaoFindFolderSyncState(newSyncKey, newSyncState);
		testUtils.expectCreateFolderMappingState();
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(newSyncKey);

		mocksControl.verify();

		assertThat(folderSyncResponse.getNewSyncKey()).isEqualTo(newGeneratedSyncKey);
		assertThat(folderSyncResponse.getStatus()).isEqualTo(FolderSyncStatus.OK);
		assertThat(folderSyncResponse.getCount()).isEqualTo(1);
		assertThat(folderSyncResponse.getCollectionsDeleted()).hasSize(1);
		CollectionDeletion inbox = Iterables.getOnlyElement(folderSyncResponse.getCollectionsDeleted());
		assertThat(inbox.getCollectionId()).isEqualTo(collectionId);
	}

	private HierarchyCollectionChanges buildHierarchyItemsChangeEmpty() {
		return HierarchyCollectionChanges.builder().build();
	}

	private void expectCollectionDaoAllocateFolderSyncState(FolderSyncKey folderSyncKey, FolderSyncState newSyncState) 
			throws DaoException {
	
		expect(collectionDao.allocateNewFolderSyncState(user.device, folderSyncKey)).andReturn(newSyncState);
	}

	private void expectCollectionDaoFindFolderSyncState(FolderSyncKey newSyncKey, FolderSyncState newSyncState) throws DaoException {
		
		expect(collectionDao.findFolderStateForKey(newSyncKey)).andReturn(FolderSyncState.builder().syncKey(newSyncKey).build());
		expectCollectionDaoAllocateFolderSyncState(newSyncState.getSyncKey(), newSyncState);
	}

	private FolderSyncState newFolderSyncState(FolderSyncKey newGeneratedSyncKey, int newSyncStateId) {
		return FolderSyncState.builder()
				.id(newSyncStateId)
				.syncKey(newGeneratedSyncKey)
				.build();
	}
}
