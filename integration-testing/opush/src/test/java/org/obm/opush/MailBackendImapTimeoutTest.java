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
package org.obm.opush;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.obm.DateUtils.date;

import java.util.Date;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Files;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.FolderSyncStatus;
import org.obm.push.bean.GetItemEstimateStatus;
import org.obm.push.bean.ItemOperationsStatus;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MeetingResponseStatus;
import org.obm.push.bean.MoveItemsStatus;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.PingStatus;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.calendar.CalendarBackend;
import org.obm.push.calendar.CalendarPath;
import org.obm.push.contacts.AddressBookId;
import org.obm.push.contacts.ContactsBackend;
import org.obm.push.exception.DaoException;
import org.obm.push.protocol.PingProtocol;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.FolderSyncResponse;
import org.obm.push.protocol.bean.MeetingHandlerResponse;
import org.obm.push.protocol.bean.PingResponse;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.FolderSyncStateBackendMappingDao;
import org.obm.push.store.HeartbeatDao;
import org.obm.push.task.TaskBackend;
import org.obm.push.utils.DateUtils;
import org.obm.sync.push.client.ItemOperationResponse;
import org.obm.sync.push.client.MoveItemsResponse;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.beans.GetItemEstimateSingleFolderResponse;
import org.obm.sync.push.client.commands.MoveItemsCommand.Move;

import com.google.inject.Inject;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;

@RunWith(GuiceRunner.class)
@GuiceModule(MailBackendImapTimeoutTestModule.class)
public class MailBackendImapTimeoutTest {

	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private GreenMail greenMail;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private ContactsBackend contactsBackend;
	@Inject private TaskBackend taskBackend;
	@Inject private CalendarBackend calendarBackend;
	@Inject private SyncDecoder syncDecoder;
	@Inject private PingProtocol pingProtocol;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private CollectionDao collectionDao;
	@Inject private FolderSyncStateBackendMappingDao folderSyncStateBackendMappingDao;
	@Inject private HeartbeatDao heartbeatDao;
	@Inject private DateService dateService;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private SyncKeyTestUtils syncKeyTestUtils;
	@Inject private FolderSnapshotDao folderSnapshotDao;

	private GreenMailUser greenMailUser;
	private ImapHostManager imapHostManager;
	private OpushUser user;
	private String mailbox;
	private String inboxCollectionPath;
	private CollectionId inboxCollectionId;
	private String trashCollectionPath;
	private CollectionId trashCollectionId;
	private CloseableHttpClient httpClient;

	@Before
	public void init() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
		user = users.jaures;
		greenMail.start();
		mailbox = user.user.getLoginAtDomain();
		greenMailUser = greenMail.setUser(mailbox, String.valueOf(user.password));
		imapHostManager = greenMail.getManagers().getImapHostManager();
		imapHostManager.createMailbox(greenMailUser, "Trash");

		inboxCollectionPath = testUtils.buildEmailInboxCollectionPath(user);
		inboxCollectionId = CollectionId.of(1234);
		trashCollectionPath = testUtils.buildEmailTrashCollectionPath(user);
		trashCollectionId = CollectionId.of(1645);
		
		bindCollectionIdToPath();

		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
	}

	private void bindCollectionIdToPath() throws Exception {
		expect(collectionDao.getCollectionPath(inboxCollectionId)).andReturn(inboxCollectionPath).anyTimes();
		expect(collectionDao.getCollectionPath(trashCollectionId)).andReturn(trashCollectionPath).anyTimes();
	}

	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		greenMail.stop();
		Files.delete(configuration.dataDir);
	}

	@Test
	public void testSyncHandler() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("8afde786-94c2-4a2a-af20-f9ebc93bf42d");
		SyncKey secondAllocatedSyncKey = new SyncKey("c8c5f1ba-abec-429c-9742-14e50f613060");
		int allocatedStateId = 3;
		int allocatedStateId2 = 4;
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, secondAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(allocatedStateId)
				.build();
		ItemSyncState currentAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(allocatedStateId2)
				.build();
		
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).once();
		expect(dateService.getCurrentDate()).andReturn(currentAllocatedState.getSyncDate());
		expectCollectionDaoPerformInitialSync(firstAllocatedState, inboxCollectionId);
		expect(collectionDao.findItemStateForKey(firstAllocatedSyncKey)).andReturn(firstAllocatedState);
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		opClient.syncEmail(syncDecoder, initialSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 25);
		greenMail.lockGreenmailAndReleaseAfter(20);
		SyncResponse syncResponse = opClient.syncEmail(syncDecoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 25);

		mocksControl.verify();
		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.SERVER_ERROR);
	}
	
	@Test
	public void testFolderSyncHandler() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("cf32d2cb-2f09-425b-b840-bee03c1dd18e");
		FolderSyncKey secondSyncKey = new FolderSyncKey("768380e9-c6d5-45c1-baaa-19c7405daffb");
		folderSnapshotDao.create(user.user, user.device, syncKey, FolderSnapshot.empty());
		int stateId = 3;
		int stateId2 = 4;
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(syncKey);
		
		FolderSyncState folderSyncState = FolderSyncState.builder()
				.syncKey(syncKey)
				.id(stateId)
				.build();
		FolderSyncState secondFolderSyncState = FolderSyncState.builder()
				.syncKey(secondSyncKey)
				.id(stateId2)
				.build();
		
		expect(collectionDao.allocateNewFolderSyncState(user.device, syncKey))
			.andReturn(secondFolderSyncState).anyTimes();
		expect(collectionDao.allocateNewFolderSyncState(user.device, secondSyncKey))
			.andReturn(secondFolderSyncState).anyTimes();
		
		UserDataRequest udr = new UserDataRequest(user.credentials, "FolderSync", user.device);
		expect(contactsBackend.getHierarchyChanges(udr, folderSyncState, secondFolderSyncState))
			.andReturn(HierarchyCollectionChanges.builder().build()).anyTimes();
		expect(taskBackend.getHierarchyChanges(udr, folderSyncState, secondFolderSyncState))
			.andReturn(HierarchyCollectionChanges.builder().build()).anyTimes();
		expect(calendarBackend.getHierarchyChanges(udr, folderSyncState, secondFolderSyncState))
			.andReturn(HierarchyCollectionChanges.builder().build()).anyTimes();

		expect(contactsBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.<AddressBookId>instance()).anyTimes();
		expect(calendarBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.<CalendarPath>instance()).anyTimes();
		expect(taskBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.instance()).anyTimes();
		
		folderSyncStateBackendMappingDao.createMapping(anyObject(PIMDataType.class), anyObject(FolderSyncState.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		greenMail.lockGreenmailAndReleaseAfter(20);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(syncKey);
		
		mocksControl.verify();
		assertThat(folderSyncResponse.getStatus()).isEqualTo(FolderSyncStatus.SERVER_ERROR);
	}
	
	@Test
	public void testGetItemEstimateHandler() throws Exception {
		SyncKey syncKey = new SyncKey("a7a6b55c-71d2-4754-98df-af6465a91481");
		int stateId = 3;
		
		userAccessUtils.mockUsersAccess(user);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(syncKey)
				.id(stateId)
				.build();
		
		expect(collectionDao.findItemStateForKey(syncKey))
			.andReturn(syncState);
		
		expect(dateService.getCurrentDate()).andReturn(syncState.getSyncDate());
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		greenMail.lockGreenmailAndReleaseAfter(20);
		GetItemEstimateSingleFolderResponse itemEstimateResponse = opClient.getItemEstimateOnMailFolder(syncKey, inboxCollectionId);
		
		mocksControl.verify();
		assertThat(itemEstimateResponse.getStatus()).isEqualTo(GetItemEstimateStatus.NEED_SYNC);
	}
	
	@Test
	public void testItemOperationsHandler() throws Exception {
		userAccessUtils.mockUsersAccess(user);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		greenMail.lockGreenmailAndReleaseAfter(20);
		ItemOperationResponse itemOperationResponse = opClient.itemOperationFetch(inboxCollectionId, inboxCollectionId.serverId(1));
		
		mocksControl.verify();
		assertThat(itemOperationResponse.getStatus()).isEqualTo(ItemOperationsStatus.SERVER_ERROR);
	}
	
	@Test
	public void testMeetingResponseHandler() throws Exception {
		userAccessUtils.mockUsersAccess(user);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		greenMail.lockGreenmailAndReleaseAfter(20);
		MeetingHandlerResponse meetingHandlerResponse = opClient.meetingResponse(inboxCollectionId, inboxCollectionId.serverId(1));
		
		mocksControl.verify();
		assertThat(meetingHandlerResponse.getItemChanges().iterator().next().getStatus()).isEqualTo(MeetingResponseStatus.SERVER_ERROR);
	}

	@Test
	public void testMoveItemsHandler() throws Exception {
		userAccessUtils.mockUsersAccess(user);

		expect(collectionDao.getCollectionMapping(user.device, trashCollectionPath))
			.andReturn(trashCollectionId).anyTimes();
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		greenMail.lockGreenmailAndReleaseAfter(20);
		MoveItemsResponse moveItemsResponse = opClient.moveItems(
				new Move(inboxCollectionId.serverId(1), inboxCollectionId, trashCollectionId));
		
		mocksControl.verify();
		assertThat(moveItemsResponse.getStatus()).isEqualTo(MoveItemsStatus.SERVER_ERROR);
	}
	
	@Ignore("Waiting for push mode in order to be checked")
	@Test
	public void testPingHandler() throws Exception {
		long heartbeat = 5;
		SyncKey syncKey = new SyncKey("35aff2e3-544d-4b3f-b6d8-cc9162a45dce");
		int stateId = 3;
		
		userAccessUtils.mockUsersAccess(user);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(syncKey)
				.id(stateId)
				.build();
		
		heartbeatDao.updateLastHeartbeat(user.device, heartbeat);
		expectLastCall();
		
		expect(collectionDao.lastKnownState(user.device, inboxCollectionId))
			.andReturn(syncState);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		greenMail.lockGreenmailAndReleaseAfter(20);
		PingResponse pingResponse = opClient.ping(pingProtocol, inboxCollectionId, heartbeat);
		
		mocksControl.verify();
		assertThat(pingResponse.getPingStatus()).isEqualTo(PingStatus.SERVER_ERROR);
	}

	private void expectCollectionDaoPerformInitialSync(ItemSyncState itemSyncState, CollectionId collectionId)
					throws DaoException {
		
		expect(collectionDao.updateState(user.device, collectionId, itemSyncState.getSyncKey(), itemSyncState.getSyncDate()))
			.andReturn(itemSyncState);
		collectionDao.resetCollection(user.device, inboxCollectionId);
		expectLastCall();
	}
}
