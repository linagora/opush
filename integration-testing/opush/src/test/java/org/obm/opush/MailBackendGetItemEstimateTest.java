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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.configuration.EmailConfiguration;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.GetItemEstimateStatus;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.SyncKey;
import org.obm.push.exception.DaoException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.service.DateService;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.ItemTrackingDao;
import org.obm.push.utils.DateUtils;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.beans.GetItemEstimateSingleFolderResponse;

import com.google.inject.Inject;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;

@RunWith(GuiceRunner.class)
@GuiceModule(MailBackendTestModule.class)
public class MailBackendGetItemEstimateTest {

	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private GreenMail greenMail;
	@Inject private ImapConnectionCounter imapConnectionCounter;
	@Inject private PendingQueriesLock pendingQueries;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private SyncDecoder decoder;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private SyncKeyTestUtils syncKeyTestUtils;
	
	@Inject private ItemTrackingDao itemTrackingDao;
	@Inject private CollectionDao collectionDao;
	@Inject private DateService dateService;

	private ServerSetup smtpServerSetup;
	private GreenMailUser greenMailUser;
	private ImapHostManager imapHostManager;
	private OpushUser user;
	private String mailbox;
	private String inboxCollectionPath;
	private CollectionId inboxCollectionId;
	private CloseableHttpClient httpClient;

	@Before
	public void init() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
		user = users.jaures;
		greenMail.start();
		smtpServerSetup = greenMail.getSmtp().getServerSetup();
		mailbox = user.user.getLoginAtDomain();
		greenMailUser = greenMail.setUser(mailbox, String.valueOf(user.password));
		imapHostManager = greenMail.getManagers().getImapHostManager();
		imapHostManager.createMailbox(greenMailUser, "Trash");
		
		inboxCollectionPath = testUtils.buildEmailInboxCollectionPath(user);
		inboxCollectionId = CollectionId.of(1234);
		
		bindCollectionIdToPath();

		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
	}

	private void bindCollectionIdToPath() throws Exception {
		expect(collectionDao.getCollectionPath(inboxCollectionId)).andReturn(inboxCollectionPath).anyTimes();
	}

	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		greenMail.stop();
		Files.delete(configuration.dataDir);
	}

	@Test
	public void testGetItemEstimateWithInvalidSyncKey() throws Exception {
		SyncKey invalidSyncKey = new SyncKey("b5dcc20d-d781-418a-b2dc-91df8b313325");
		
		userAccessUtils.mockUsersAccess(user);
		
		expect(collectionDao.findItemStateForKey(invalidSyncKey)).andReturn(null);

		mocksControl.replay();
		
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		GetItemEstimateSingleFolderResponse itemEstimateResponse = opClient.getItemEstimateOnMailFolder(invalidSyncKey, inboxCollectionId);

		mocksControl.verify();
		
		assertThat(itemEstimateResponse.getStatus()).isEqualTo(GetItemEstimateStatus.INVALID_SYNC_KEY);
	}

	@Test
	public void testGetItemEstimateNoChange() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("419b040c-b801-4f59-9f77-64c35a52c29e");
		SyncKey lastSyncKey = new SyncKey("617b4ccd-2d67-4397-9e7e-57ef36fc7878");
		int lastStateId = 3;
		
		userAccessUtils.mockUsersAccess(user);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState lastSyncState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(lastSyncKey)
				.id(lastStateId)
				.build();
		
		expectInitialSyncWithTwoMails(firstAllocatedSyncKey, lastSyncKey);

		expect(dateService.getCurrentDate()).andReturn(DateUtils.getCurrentDate()).once();
		expect(collectionDao.findItemStateForKey(lastSyncKey)).andReturn(lastSyncState).once();
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();

		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 100);
		opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 100);

		GetItemEstimateSingleFolderResponse itemEstimateResponse = opClient.getItemEstimateOnMailFolder(lastSyncKey, inboxCollectionId);

		mocksControl.verify();
		
		assertThat(itemEstimateResponse.getEstimate()).isEqualTo(0);

		assertEmailCountInMailbox(EmailConfiguration.IMAP_INBOX_NAME, 2);
		assertThat(pendingQueries.waitingClose(10, TimeUnit.SECONDS)).isTrue();
		assertThat(imapConnectionCounter.loginCounter.get()).isEqualTo(2);
		assertThat(imapConnectionCounter.closeCounter.get()).isEqualTo(2);
		assertThat(imapConnectionCounter.selectCounter.get()).isEqualTo(2);
		assertThat(imapConnectionCounter.listMailboxesCounter.get()).isEqualTo(0);
	}

	@Test
	public void testGetItemEstimateNoChangeWithFilterTypeChanged() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("2917ed5b-6f92-4626-9edd-258af2159757");
		SyncKey lastSyncKey = new SyncKey("d604c7d1-39b6-4c15-8437-3aa46875cae7");
		int lastStateId = 3;
		
		userAccessUtils.mockUsersAccess(user);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState lastSyncState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(lastSyncKey)
				.id(lastStateId)
				.build();
		
		expectInitialSyncWithTwoMails(firstAllocatedSyncKey, lastSyncKey);

		expect(dateService.getCurrentDate()).andReturn(DateUtils.getCurrentDate()).once();
		expect(collectionDao.findItemStateForKey(lastSyncKey)).andReturn(lastSyncState).once();
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();

		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 25);
		opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 25);

		GetItemEstimateSingleFolderResponse itemEstimateResponse = opClient.getItemEstimateOnMailFolder(lastSyncKey, FilterType.ONE_MONTHS_BACK, inboxCollectionId);

		mocksControl.verify();
		
		assertThat(itemEstimateResponse.getStatus()).isEqualTo(GetItemEstimateStatus.INVALID_SYNC_KEY);

		assertEmailCountInMailbox(EmailConfiguration.IMAP_INBOX_NAME, 2);
		assertThat(pendingQueries.waitingClose(10, TimeUnit.SECONDS)).isTrue();
		assertThat(imapConnectionCounter.loginCounter.get()).isEqualTo(2);
		assertThat(imapConnectionCounter.closeCounter.get()).isEqualTo(2);
		assertThat(imapConnectionCounter.selectCounter.get()).isEqualTo(1);
		assertThat(imapConnectionCounter.listMailboxesCounter.get()).isEqualTo(0);
	}

	@Test
	public void testGetItemEstimateWithChanges() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("f0820039-8bf4-4357-b79d-e33990440118");
		SyncKey lastSyncKey = new SyncKey("f1856d19-7a4b-4422-ae84-bda5516bf4b8");
		int lastStateId = 3;
		
		userAccessUtils.mockUsersAccess(user);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState lastSyncState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(lastSyncKey)
				.id(lastStateId)
				.build();
		
		expectInitialSyncWithTwoMails(firstAllocatedSyncKey, lastSyncKey);

		expect(dateService.getCurrentDate()).andReturn(DateUtils.getCurrentDate()).once();
		expect(collectionDao.findItemStateForKey(lastSyncKey)).andReturn(lastSyncState).once();
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();

		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 100);
		opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 100);

		sendTwoEmailsToImapServer();
		GetItemEstimateSingleFolderResponse itemEstimateResponse = opClient.getItemEstimateOnMailFolder(lastSyncKey, inboxCollectionId);

		mocksControl.verify();
		
		assertThat(itemEstimateResponse.getEstimate()).isEqualTo(2);

		assertEmailCountInMailbox(EmailConfiguration.IMAP_INBOX_NAME, 4);
		assertThat(pendingQueries.waitingClose(10, TimeUnit.SECONDS)).isTrue();
		assertThat(imapConnectionCounter.loginCounter.get()).isEqualTo(2);
		assertThat(imapConnectionCounter.closeCounter.get()).isEqualTo(2);
		assertThat(imapConnectionCounter.selectCounter.get()).isEqualTo(2);
		assertThat(imapConnectionCounter.listMailboxesCounter.get()).isEqualTo(0);
	}

	private void expectInitialSyncWithTwoMails(SyncKey firstAllocatedSyncKey, SyncKey secondAllocatedSyncKey) throws Exception {
		int allocatedStateId = 3;
		int allocatedStateId2 = 4;
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, secondAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(allocatedStateId)
				.build();
		ItemSyncState allocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(allocatedStateId2)
				.build();
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).once();
		expect(dateService.getCurrentDate()).andReturn(allocatedState.getSyncDate()).times(2);
		expectCollectionDaoPerformInitialSync(firstAllocatedState);
		expectCollectionDaoPerformSync(firstAllocatedSyncKey, firstAllocatedState, allocatedState);
		
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, inboxCollectionId.serverId(1))).andReturn(false);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, inboxCollectionId.serverId(2))).andReturn(false);
	}

	private void expectCollectionDaoPerformSync(SyncKey requestSyncKey, ItemSyncState allocatedState, ItemSyncState newItemSyncState)
			throws DaoException {
		expect(collectionDao.findItemStateForKey(requestSyncKey)).andReturn(allocatedState);
		expect(collectionDao.updateState(user.device, inboxCollectionId, newItemSyncState.getSyncKey(), newItemSyncState.getSyncDate()))
				.andReturn(newItemSyncState);
	}

	private void expectCollectionDaoPerformInitialSync(ItemSyncState itemSyncState) throws DaoException {
		expect(collectionDao.updateState(user.device, inboxCollectionId, itemSyncState.getSyncKey(), itemSyncState.getSyncDate()))
			.andReturn(itemSyncState);
		collectionDao.resetCollection(user.device, inboxCollectionId);
		expectLastCall();
	}

	private void sendTwoEmailsToImapServer() throws InterruptedException {
		GreenMailUtil.sendTextEmail(mailbox, mailbox, "subject", "body", smtpServerSetup);
		GreenMailUtil.sendTextEmail(mailbox, mailbox, "subject2", "body", smtpServerSetup);
		greenMail.waitForIncomingEmail(2);
	}

	private void assertEmailCountInMailbox(String mailbox, Integer expectedNumberOfEmails) {
		MailFolder inboxFolder = imapHostManager.getFolder(greenMailUser, mailbox);
		Assertions.assertThat(inboxFolder.getMessageCount()).isEqualTo(expectedNumberOfEmails);
	}
}
