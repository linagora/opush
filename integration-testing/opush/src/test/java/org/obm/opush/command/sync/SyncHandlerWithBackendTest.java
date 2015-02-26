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
package org.obm.opush.command.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.reportMatcher;
import static org.obm.DateUtils.date;
import static org.obm.push.bean.PIMDataType.CALENDAR;
import static org.obm.push.bean.PIMDataType.CONTACTS;
import static org.obm.push.bean.PIMDataType.EMAIL;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.MimeMessage;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Files;
import org.easymock.IMocksControl;
import org.easymock.internal.matchers.Equals;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.annotations.transactional.TransactionProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.ImapConnectionCounter;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.PendingQueriesLock;
import org.obm.opush.SyncKeyTestUtils;
import org.obm.opush.Users;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.CalendarBusyStatus;
import org.obm.push.bean.CalendarSensitivity;
import org.obm.push.bean.Device;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSAttachement;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.MSEventUid;
import org.obm.push.bean.MethodAttachment;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SnapshotKey;
import org.obm.push.bean.SyncCollectionCommandRequest;
import org.obm.push.bean.SyncCollectionCommandResponse;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncCollectionResponsesResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.hierarchy.AddressBookId;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.CalendarPath;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.configuration.OpushEmailConfiguration;
import org.obm.push.exception.DaoException;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.EncoderFactory;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.CalendarDao;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.DeviceDao;
import org.obm.push.store.DeviceDao.PolicyStatus;
import org.obm.push.store.ItemTrackingDao;
import org.obm.push.store.SnapshotDao;
import org.obm.push.utils.DateUtils;
import org.obm.sync.book.AddressBook;
import org.obm.sync.book.Contact;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventMeetingStatus;
import org.obm.sync.calendar.EventObmId;
import org.obm.sync.calendar.EventPrivacy;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.client.calendar.CalendarClient;
import org.obm.sync.client.login.LoginClient;
import org.obm.sync.items.ContactChanges;
import org.obm.sync.items.EventChanges;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.commands.Sync;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;

import fr.aliacom.obm.common.user.UserPassword;

@GuiceModule(SyncHandlerWithBackendTestModule.class)
@RunWith(GuiceRunner.class)
public class SyncHandlerWithBackendTest {

	private final static int ONE_WINDOWS_SIZE = 1;
	private final static int ONE_HUNDRED_WINDOWS_SIZE = 100;
	
	@Inject private	Users users;
	@Inject private	OpushServer opushServer;
	@Inject private GreenMail greenMail;
	@Inject private ImapConnectionCounter imapConnectionCounter;
	@Inject private PendingQueriesLock pendingQueries;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private SyncDecoder decoder;
	@Inject private EncoderFactory encoderFactory;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private TransactionProvider transactionProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private SnapshotDao snapshotDao;
	@Inject private ItemTrackingDao itemTrackingDao;
	@Inject private CollectionDao collectionDao;
	@Inject private CalendarDao calendarDao;
	@Inject private DateService dateService;
	@Inject private CalendarClient calendarClient;
	@Inject private BookClient bookClient;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private SyncKeyTestUtils syncKeyTestUtils;
	@Inject private LoginClient loginClient;
	@Inject private DeviceDao deviceDao;
	@Inject private FolderSnapshotDao folderSnapshotDao;
	@Inject private SyncTestUtils syncTestUtils;
	private GreenMailUser greenMailUser;
	private ImapHostManager imapHostManager;
	private OpushUser user;
	private String mailbox;
	private MailboxPath inboxPath;
	private CalendarPath calendarPath;
	private AddressBookId contactPath;
	private CollectionId inboxCollectionId;
	private CollectionId calendarCollectionId;
	private CollectionId contactCollectionId;
	private Folder inboxFolder;
	private Folder calendarFolder;
	private Folder contactFolder;

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

		inboxPath = MailboxPath.of(OpushEmailConfiguration.IMAP_INBOX_NAME);
		inboxCollectionId = CollectionId.of(1234);
		inboxFolder = Folder.builder()
				.backendId(inboxPath)
				.collectionId(inboxCollectionId)
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("displayName")
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.build();
		
		calendarPath = CalendarPath.of("jaures@sfio.fr");
		calendarCollectionId = CollectionId.of(5678);
		calendarFolder = Folder.builder()
				.backendId(calendarPath)
				.collectionId(calendarCollectionId)
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("cal")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build();
		
		contactCollectionId = CollectionId.of(7891);
		contactPath = AddressBookId.of(contactCollectionId.asInt());
		contactFolder = Folder.builder()
				.backendId(contactPath)
				.collectionId(contactCollectionId)
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("contacts")
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build();

		FolderSyncKey syncKey = new FolderSyncKey("4fd6280c-cbaa-46aa-a859-c6aad00f1ef3");
		folderSnapshotDao.create(user.user, user.device, syncKey, 
				FolderSnapshot.nextId(2).folders(ImmutableSet.of(inboxFolder, calendarFolder, contactFolder)));
		
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
	}


	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		greenMail.stop();
		httpClient.close();
		Files.delete(configuration.dataDir);
	}

	@Test
	public void testInitialSyncThenRecreatesAccountOnMails() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("d6b223c4-c7ea-499e-8f65-d94e3121efb8");
		SyncKey secondAllocatedSyncKey = new SyncKey("0e5e9ebc-5210-423f-a15d-5d360c031220");
		SyncKey newFirstAllocatedSyncKey = new SyncKey("52add403-4c77-40a3-9a2b-f593534557f1");
		SyncKey newSecondAllocatedSyncKey = new SyncKey("8cb67253-91be-4558-a9ab-5dc8a93155d4");
		SyncKey newThirdAllocatedSyncKey = new SyncKey("7916b925-2f28-4e60-beca-89641853d8a0");
		SyncKey newFourthAllocatedSyncKey = new SyncKey("29255644-73ad-4a9c-b8af-2ead31a38f01");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int newFirstAllocatedStateId = 5;
		int newSecondAllocatedStateId = 6;
		int newThirdAllocatedStateId = 7;
		int newFourthAllocatedStateId = 8;
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, 
				secondAllocatedSyncKey, newFirstAllocatedSyncKey, 
				newSecondAllocatedSyncKey, newThirdAllocatedSyncKey,
				newFourthAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState newFirstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(newFirstAllocatedSyncKey)
				.id(newFirstAllocatedStateId)
				.build();
		ItemSyncState newSecondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T17:22:53"))
				.syncKey(newSecondAllocatedSyncKey)
				.id(newSecondAllocatedStateId)
				.build();
		ItemSyncState newThirdAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T17:22:53"))
				.syncKey(newThirdAllocatedSyncKey)
				.id(newThirdAllocatedStateId)
				.build();
		ItemSyncState newFourthAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T17:22:53"))
				.syncKey(newFourthAllocatedSyncKey)
				.id(newFourthAllocatedStateId)
				.build();
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(3);
		expectCollectionDaoPerformInitialSync(firstAllocatedState, inboxCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		ServerId serverId = inboxCollectionId.serverId(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId)).andReturn(false);
		
		expect(dateService.getCurrentDate()).andReturn(newSecondAllocatedState.getSyncDate()).times(4);
		expectCollectionDaoPerformInitialSync(newFirstAllocatedState, inboxCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, newFirstAllocatedSyncKey, newFirstAllocatedState, newSecondAllocatedState, inboxCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, newSecondAllocatedSyncKey, newSecondAllocatedState, newThirdAllocatedState, inboxCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, newThirdAllocatedSyncKey, newThirdAllocatedState, newFourthAllocatedState, inboxCollectionId);

		expect(itemTrackingDao.isServerIdSynced(newFirstAllocatedState, serverId)).andReturn(false);
		ServerId serverId2 = inboxCollectionId.serverId(1);
		expect(itemTrackingDao.isServerIdSynced(newSecondAllocatedState, serverId2)).andReturn(false);
		
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse syncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, newFirstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse secondSyncResponse = opClient.syncEmail(decoder, newSecondAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse thirdSyncResponse = opClient.syncEmail(decoder, newThirdAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(syncResponse, inboxCollectionId);
		SyncCollectionResponse firstCollectionResponse = syncTestUtils.getCollectionWithId(firstSyncResponse, inboxCollectionId);
		SyncCollectionResponse secondCollectionResponse = syncTestUtils.getCollectionWithId(secondSyncResponse, inboxCollectionId);
		SyncCollectionResponse thirdCollectionResponse = syncTestUtils.getCollectionWithId(thirdSyncResponse, inboxCollectionId);

		syncTestUtils.assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId.serverId(2))
						.isNew(true)
						.build()));
		syncTestUtils.assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId.serverId(2))
						.isNew(true)
						.build()));
		assertThat(firstCollectionResponse.isMoreAvailable()).isTrue();
		syncTestUtils.assertEqualsWithoutApplicationData(secondCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId.serverId(1))
						.isNew(true)
						.build()));
		assertThat(secondCollectionResponse.isMoreAvailable()).isFalse();
		assertThat(thirdCollectionResponse.getItemChanges()).hasSize(0);
		
		assertEmailCountInMailbox(OpushEmailConfiguration.IMAP_INBOX_NAME, 2);
		assertThat(pendingQueries.waitingClose(10, TimeUnit.SECONDS)).isTrue();
		assertThat(imapConnectionCounter.loginCounter.get()).isEqualTo(4);
		assertThat(imapConnectionCounter.closeCounter.get()).isEqualTo(4);
		assertThat(imapConnectionCounter.selectCounter.get()).isEqualTo(4);
		assertThat(imapConnectionCounter.listMailboxesCounter.get()).isEqualTo(0);
	}
	
	@Test
	public void testInitialSyncThenRecreatesAccountOnCalendars() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("ba9cc33e-0be1-40f9-94ee-4a28760e7dbb");
		SyncKey secondAllocatedSyncKey = new SyncKey("2c24fbbc-6a94-4d6a-b9a7-7b4974a09a3c");
		SyncKey newFirstAllocatedSyncKey = new SyncKey("faacfa99-d6ef-406b-8c59-fc90a6710443");
		SyncKey newSecondAllocatedSyncKey = new SyncKey("c5dfc365-d7a0-4883-b407-69e8587df761");
		SyncKey newThirdAllocatedSyncKey = new SyncKey("0e5e9ebc-5210-423f-a15d-5d360c031220");
		SyncKey newFourthAllocatedSyncKey = new SyncKey("d974602b-29fe-49ba-bf82-03b413d1c2fb");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int newFirstAllocatedStateId = 5;
		int newSecondAllocatedStateId = 6;
		int newThirdAllocatedStateId = 7;
		int newFourthAllocatedStateId = 8;
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, 
				secondAllocatedSyncKey, newFirstAllocatedSyncKey, 
				newSecondAllocatedSyncKey, newThirdAllocatedSyncKey,
				newFourthAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		Date secondDate = date("2012-10-10T16:22:53");
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState newFirstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(newFirstAllocatedSyncKey)
				.id(newFirstAllocatedStateId)
				.build();
		ItemSyncState newSecondAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(newSecondAllocatedSyncKey)
				.id(newSecondAllocatedStateId)
				.build();
		ItemSyncState newThirdAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(newThirdAllocatedSyncKey)
				.id(newThirdAllocatedStateId)
				.build();
		ItemSyncState newFourthAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(newFourthAllocatedSyncKey)
				.id(newFourthAllocatedStateId)
				.build();
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expectCollectionDaoPerformInitialSync(firstAllocatedState, calendarCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);

		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		EventExtId eventExtId = new EventExtId("1");
		Event event = new Event();
		event.setUid(new EventObmId(1));
		event.setTitle("event");
		event.setExtId(eventExtId);
		event.setStartDate(secondDate);
		EventExtId eventExtId2 = new EventExtId("2");
		Event event2 = new Event();
		event2.setUid(new EventObmId(2));
		event2.setTitle("event2");
		event2.setExtId(eventExtId2);
		event2.setStartDate(secondDate);
		expect(calendarClient.getFirstSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.updates(Lists.newArrayList(event, event2))
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		
		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		Calendar calendar = DateUtils.getEpochCalendar(timeZone);
		MSEventUid msEventUid = new MSEventUid("1");
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(msEventUid);
		msEvent.setSubject("event");
		msEvent.setSensitivity(CalendarSensitivity.NORMAL);
		msEvent.setBusyStatus(CalendarBusyStatus.FREE);
		msEvent.setAllDayEvent(false);
		msEvent.setDtStamp(calendar.getTime());
		msEvent.setTimeZone(timeZone);
		MSEventUid msEventUid2 = new MSEventUid("2");
		MSEvent msEvent2 = new MSEvent();
		msEvent2.setUid(msEventUid2);
		msEvent2.setSubject("event2");
		msEvent2.setSensitivity(CalendarSensitivity.NORMAL);
		msEvent2.setBusyStatus(CalendarBusyStatus.FREE);
		msEvent2.setAllDayEvent(false);
		msEvent2.setDtStamp(calendar.getTime());
		msEvent2.setTimeZone(timeZone);
		expect(calendarDao.getMSEventUidFor(eventExtId, user.device))
			.andReturn(msEventUid).times(2);
		expect(calendarDao.getMSEventUidFor(eventExtId2, user.device))
			.andReturn(msEventUid2).times(2);
		
		ServerId serverId = calendarCollectionId.serverId(Integer.valueOf(msEvent.getUid().serializeToString()));
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId)).andReturn(false);
		
		expectCollectionDaoPerformInitialSync(newFirstAllocatedState, calendarCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, newFirstAllocatedSyncKey, newFirstAllocatedState, newSecondAllocatedState, calendarCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, newSecondAllocatedSyncKey, newSecondAllocatedState, newThirdAllocatedState, calendarCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, newThirdAllocatedSyncKey, newThirdAllocatedState, newFourthAllocatedState, calendarCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(newSecondAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(newThirdAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(newFourthAllocatedState.getSyncDate()).once();
		expect(itemTrackingDao.isServerIdSynced(newFirstAllocatedState, 
				serverId))
			.andReturn(false);
		ServerId serverId2 = calendarCollectionId.serverId(Integer.valueOf(msEvent2.getUid().serializeToString()));
		expect(itemTrackingDao.isServerIdSynced(newSecondAllocatedState, serverId2))
			.andReturn(false);
		
		expect(calendarClient.getFirstSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.updates(Lists.newArrayList(event, event2))
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		expect(calendarClient.getSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		AnalysedSyncCollection.Builder configuredSyncCollection = AnalysedSyncCollection.builder()
				.collectionId(calendarCollectionId)
				.windowSize(ONE_WINDOWS_SIZE)
				.options(SyncCollectionOptions.builder().filterType(FilterType.THREE_DAYS_BACK).build())
				.dataType(CALENDAR);
		opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(initialSyncKey).build()).build());
		SyncResponse syncResponse = opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(firstAllocatedSyncKey).build()).build());
		
		opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(initialSyncKey).build()).build());
		SyncResponse firstSyncResponse = opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(newFirstAllocatedSyncKey).build()).build());
		SyncResponse secondSyncResponse = opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(newSecondAllocatedSyncKey).build()).build());
		SyncResponse thirdSyncResponse = opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(newThirdAllocatedSyncKey).build()).build());
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(syncResponse, calendarCollectionId);
		SyncCollectionResponse firstCollectionResponse = syncTestUtils.getCollectionWithId(firstSyncResponse, calendarCollectionId);
		SyncCollectionResponse secondCollectionResponse = syncTestUtils.getCollectionWithId(secondSyncResponse, calendarCollectionId);
		SyncCollectionResponse thirdCollectionResponse = syncTestUtils.getCollectionWithId(thirdSyncResponse, calendarCollectionId);

		syncTestUtils.assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		syncTestUtils.assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		assertThat(firstCollectionResponse.isMoreAvailable()).isTrue();
		syncTestUtils.assertEqualsWithoutApplicationData(secondCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId2)
						.isNew(true)
						.build()));
		assertThat(secondCollectionResponse.isMoreAvailable()).isFalse();
		assertThat(thirdCollectionResponse.getItemChanges()).hasSize(0);
	}
	
	@Test
	public void testInitialSyncThenRecreatesAccountOnContacts() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("ba9cc33e-0be1-40f9-94ee-4a28760e7dbb");
		SyncKey secondAllocatedSyncKey = new SyncKey("2c24fbbc-6a94-4d6a-b9a7-7b4974a09a3c");
		SyncKey newFirstAllocatedSyncKey = new SyncKey("faacfa99-d6ef-406b-8c59-fc90a6710443");
		SyncKey newSecondAllocatedSyncKey = new SyncKey("c5dfc365-d7a0-4883-b407-69e8587df761");
		SyncKey newThirdAllocatedSyncKey = new SyncKey("0e5e9ebc-5210-423f-a15d-5d360c031220");
		SyncKey newFourthAllocatedSyncKey = new SyncKey("d974602b-29fe-49ba-bf82-03b413d1c2fb");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int newFirstAllocatedStateId = 5;
		int newSecondAllocatedStateId = 6;
		int newThirdAllocatedStateId = 7;
		int newFourthAllocatedStateId = 8;
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, 
				secondAllocatedSyncKey, newFirstAllocatedSyncKey, 
				newSecondAllocatedSyncKey, newThirdAllocatedSyncKey,
				newFourthAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		Date secondDate = date("2012-10-10T16:22:53");
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState newFirstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(newFirstAllocatedSyncKey)
				.id(newFirstAllocatedStateId)
				.build();
		Date newSecondDate = date("2012-10-10T17:22:53");
		ItemSyncState newSecondAllocatedState = ItemSyncState.builder()
				.syncDate(newSecondDate)
				.syncKey(newSecondAllocatedSyncKey)
				.id(newSecondAllocatedStateId)
				.build();
		Date newThirdDate = newSecondDate; // the sync date is not updated as we are in windowing
		ItemSyncState newThirdAllocatedState = ItemSyncState.builder()
				.syncDate(newThirdDate)
				.syncKey(newThirdAllocatedSyncKey)
				.id(newThirdAllocatedStateId)
				.build();
		Date newFourthDate = date("2012-10-10T17:22:53");
		ItemSyncState newFourthAllocatedState = ItemSyncState.builder()
				.syncDate(newFourthDate)
				.syncKey(newFourthAllocatedSyncKey)
				.id(newFourthAllocatedStateId)
				.build();
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expectCollectionDaoPerformInitialSync(firstAllocatedState, contactCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, contactCollectionId);

		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		Contact contact = new Contact();
		contact.setUid(1);
		Contact contact2 = new Contact();
		contact2.setUid(2);
		
		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name("contacts")
					.uid(AddressBook.Id.valueOf(contactCollectionId.asInt()))
					.readOnly(false)
					.build())).anyTimes();
		
		expect(bookClient.firstListContactsChanged(user.accessToken, initialDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.of(contact, contact2),
					ImmutableSet.<Integer> of(),
					secondDate));
		
		ServerId serverId = contactCollectionId.serverId(contact.getUid());
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId)).andReturn(false);
		
		expectCollectionDaoPerformInitialSync(newFirstAllocatedState, contactCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, newFirstAllocatedSyncKey, newFirstAllocatedState, newSecondAllocatedState, contactCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, newSecondAllocatedSyncKey, newSecondAllocatedState, newThirdAllocatedState, contactCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, newThirdAllocatedSyncKey, newThirdAllocatedState, newFourthAllocatedState, contactCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(newSecondAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(newThirdAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(newFourthAllocatedState.getSyncDate()).once();
		expect(itemTrackingDao.isServerIdSynced(newFirstAllocatedState, serverId)).andReturn(false);
		ServerId serverId2 = contactCollectionId.serverId(contact2.getUid());
		expect(itemTrackingDao.isServerIdSynced(newSecondAllocatedState, serverId2)).andReturn(false);
		
		expect(bookClient.firstListContactsChanged(user.accessToken, initialDate, contactCollectionId.asInt()))
		.andReturn(new ContactChanges(ImmutableList.of(contact, contact2),
				ImmutableSet.<Integer> of(),
				newSecondDate));
	
		expect(bookClient.listContactsChanged(user.accessToken, newThirdDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(),
					ImmutableSet.<Integer> of(),
					newThirdDate));
		
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		AnalysedSyncCollection.Builder configuredSyncCollection = AnalysedSyncCollection.builder().collectionId(contactCollectionId)
				.windowSize(ONE_WINDOWS_SIZE)
				.options(SyncCollectionOptions.builder().filterType(FilterType.THREE_DAYS_BACK).build())
				.dataType(CONTACTS);
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(initialSyncKey).build()).build());
		SyncResponse syncResponse = opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(firstAllocatedSyncKey).build()).build());
		
		opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(initialSyncKey).build()).build());
		SyncResponse firstSyncResponse = opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(newFirstAllocatedSyncKey).build()).build());
		SyncResponse secondSyncResponse = opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(newSecondAllocatedSyncKey).build()).build());
		SyncResponse thirdSyncResponse = opClient.run(Sync.builder(decoder).collection(configuredSyncCollection.syncKey(newThirdAllocatedSyncKey).build()).build());
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(syncResponse, contactCollectionId);
		SyncCollectionResponse firstCollectionResponse = syncTestUtils.getCollectionWithId(firstSyncResponse, contactCollectionId);
		SyncCollectionResponse secondCollectionResponse = syncTestUtils.getCollectionWithId(secondSyncResponse, contactCollectionId);
		SyncCollectionResponse thirdCollectionResponse = syncTestUtils.getCollectionWithId(thirdSyncResponse, contactCollectionId);

		syncTestUtils.assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		syncTestUtils.assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		assertThat(firstCollectionResponse.isMoreAvailable()).isTrue();
		syncTestUtils.assertEqualsWithoutApplicationData(secondCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId2)
						.isNew(true)
						.build()));
		assertThat(secondCollectionResponse.isMoreAvailable()).isFalse();
		assertThat(thirdCollectionResponse.getItemChanges()).hasSize(0);
	}

	private void expectCollectionDaoPerformInitialSync(ItemSyncState itemSyncState, CollectionId collectionId)
					throws DaoException {
		
		expect(collectionDao.updateState(user.device, collectionId, itemSyncState.getSyncKey(), itemSyncState.getSyncDate()))
			.andReturn(itemSyncState);
		collectionDao.resetCollection(user.device, collectionId);
		expectLastCall();
	}

	private void sendTwoEmailsToImapServer() throws InterruptedException {
		GreenMailUtil.sendTextEmail(mailbox, mailbox, "subject", "body", greenMail.getSmtp().getServerSetup());
		GreenMailUtil.sendTextEmail(mailbox, mailbox, "subject2", "body", greenMail.getSmtp().getServerSetup());
		greenMail.waitForIncomingEmail(2);
	}

	private void assertEmailCountInMailbox(String mailbox, Integer expectedNumberOfEmails) {
		MailFolder inboxFolder = imapHostManager.getFolder(greenMailUser, mailbox);
		assertThat(inboxFolder.getMessageCount()).isEqualTo(expectedNumberOfEmails);
	}

	@Test
	public void testFetchDeletedMail() throws Exception {
		GreenMailUtil.sendTextEmail(mailbox, mailbox, "subject", "body", greenMail.getSmtp().getServerSetup());
		greenMail.waitForIncomingEmail(1);
		
		SyncKey firstAllocatedSyncKey = new SyncKey("a181b4e9-7b87-42cf-9e8b-6de8184bed55");
		SyncKey secondAllocatedSyncKey = new SyncKey("6710d6e4-6101-4054-9566-086d6ecf3202");
		SyncKey thirdAllocatedSyncKey = new SyncKey("a65bfdaa-5a5d-437c-b43e-5454a25045ae");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int thirdAllocatedStateId = 5;
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey, thirdAllocatedSyncKey);
		initializeEmptySnapshotForSyncKey(firstAllocatedSyncKey);
		
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState thirdAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(thirdAllocatedSyncKey)
				.id(thirdAllocatedStateId)
				.build();
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, secondAllocatedSyncKey, secondAllocatedState, thirdAllocatedState, inboxCollectionId);

		ServerId serverId = inboxCollectionId.serverId(1);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(serverId));
		expectLastCall().once();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		greenMail.deleteEmailFromInbox(greenMailUser, 1);
		greenMail.expungeInbox(greenMailUser);
		SyncResponse secondSyncResponse = opClient.run(
				Sync.builder(decoder)
					.collection(AnalysedSyncCollection.builder().collectionId(inboxCollectionId)
							.syncKey(secondAllocatedSyncKey).dataType(EMAIL)
							.command(SyncCollectionCommandRequest.builder().type(SyncCommand.FETCH).serverId(serverId).build())
							.build())
					.build());
		
		mocksControl.verify();

		assertThat(firstSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse firstCollectionResponse = syncTestUtils.getCollectionWithId(firstSyncResponse, inboxCollectionId);
		syncTestUtils.assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), ImmutableList.of(
				ItemChange.builder().serverId(serverId).isNew(true).build()));

		SyncCollectionResponse inboxResponse = syncTestUtils.getCollectionWithId(secondSyncResponse, inboxCollectionId);
		SyncCollectionResponsesResponse responses = inboxResponse.getResponses();
		assertThat(inboxResponse.isMoreAvailable()).isFalse();
		assertThat(responses.getCommands()).hasSize(1);
		assertThat(responses.getCommandsForType(SyncCommand.FETCH)).containsOnly(
			SyncCollectionCommandResponse.builder()
				.status(SyncStatus.OBJECT_NOT_FOUND)
				.serverId(serverId).type(SyncCommand.FETCH)
				.build());
	}

	private void initializeEmptySnapshotForSyncKey(SyncKey firstAllocatedSyncKey) throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException, NotSupportedException {
		transactionProvider.get().begin();
		snapshotDao.put(
			SnapshotKey.builder()
				.syncKey(firstAllocatedSyncKey)
				.collectionId(inboxCollectionId)
				.deviceId(user.deviceId).build(),
			Snapshot.builder()
				.uidNext(1l)
				.filterType(FilterType.THREE_DAYS_BACK).build());
		transactionProvider.get().commit();
	}
	
	@Test
	public void testNoContentDispositionPartIsSentAsAttachment() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/attachmentWithoutContentDisposition.eml");

		SyncKey firstAllocatedSyncKey = new SyncKey("6d2645fc-33e6-4501-a8e6-42afe3e04398");
		SyncKey secondAllocatedSyncKey = new SyncKey("7f438c09-4bd4-4e18-be6a-9cb396d24df7");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(firstAllocatedSyncKey)
				.id(3)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(4)
				.build();
		
		ServerId emailServerId = inboxCollectionId.serverId(1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(response, inboxCollectionId);
		MSEmail mail = (MSEmail) Iterables.getOnlyElement(collectionResponse.getItemChanges()).getData();
		Set<MSAttachement> attachments = mail.getAttachments();
		assertThat(attachments.size()).isEqualTo(1);
		MSAttachement attachment = Iterables.getOnlyElement(attachments);
		
		assertThat(attachment.getMethod()).isEqualTo(MethodAttachment.NormalAttachment);
		assertThat(attachment.getDisplayName()).isEqualTo("TB_import.JPG");
		assertThat(attachment.getFileReference()).isNotEmpty();
		assertThat(attachment.getEstimatedDataSize()).isPositive();
		assertThat(attachment.getContentId()).isEqualTo("555343607@11062013-0EC1");
		assertThat(attachment.getContentLocation()).isEqualTo("location");
		assertThat(attachment.isInline()).isTrue();
	}
	
	@Test
	public void testMailWithICSAttachment() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/iCSAsAttachment.eml");

		SyncKey firstAllocatedSyncKey = new SyncKey("a548f9c2-4eab-4a81-bb29-7a9bbb2d32b3");
		SyncKey secondAllocatedSyncKey = new SyncKey("9ecb6879-5ec9-44f7-8b4d-91530cefb044");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(firstAllocatedSyncKey)
				.id(3)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(4)
				.build();
		
		ServerId emailServerId = inboxCollectionId.serverId(1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(response, inboxCollectionId);
		MSEmail mail = (MSEmail) Iterables.getOnlyElement(collectionResponse.getItemChanges()).getData();
		Set<MSAttachement> attachments = mail.getAttachments();
		assertThat(attachments.size()).isEqualTo(1);
		MSAttachement attachment = Iterables.getOnlyElement(attachments);
		
		assertThat(attachment.getMethod()).isEqualTo(MethodAttachment.NormalAttachment);
		assertThat(attachment.getDisplayName()).isEqualTo("attachment.ics");
	}
	
	@Test
	public void testForwardedEmailWithAttachments() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/forwardedEmailWithAttachments.eml");

		SyncKey firstAllocatedSyncKey = new SyncKey("626befef-21cc-4910-8e08-f9e966ca0495");
		SyncKey secondAllocatedSyncKey = new SyncKey("33521cf6-aa8c-424b-94c3-08068c24c310");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(firstAllocatedSyncKey)
				.id(3)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(4)
				.build();
		
		ServerId emailServerId = inboxCollectionId.serverId(1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(response, inboxCollectionId);
		MSEmail mail = (MSEmail) Iterables.getOnlyElement(collectionResponse.getItemChanges()).getData();
		Set<MSAttachement> attachments = mail.getAttachments();
		assertThat(attachments.size()).isEqualTo(2);
		
		assertThat(FluentIterable.from(attachments)
			.transform(new Function<MSAttachement, String>() {
				
				@Override
				public String apply(MSAttachement input) {
					return input.getDisplayName();
				}
			})).containsOnly("ATT00000.gif", "ATT00001.jpg");
	}
	
	@Test
	public void testInvitationDoesntShownInAttachments() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/invitation.eml");

		SyncKey firstAllocatedSyncKey = new SyncKey("bec49e8d-4bb1-43fa-beac-baa82e1b1e72");
		SyncKey secondAllocatedSyncKey = new SyncKey("838edf22-870f-4980-93e2-8df4058cba50");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(firstAllocatedSyncKey)
				.id(3)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(4)
				.build();
		
		ServerId emailServerId = inboxCollectionId.serverId(1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		expect(calendarDao.getMSEventUidFor(anyObject(EventExtId.class), eq(user.device)))
			.andReturn(new MSEventUid("1"));
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(response, inboxCollectionId);
		MSEmail mail = (MSEmail) Iterables.getOnlyElement(collectionResponse.getItemChanges()).getData();
		Set<MSAttachement> attachments = mail.getAttachments();
		assertThat(attachments.size()).isEqualTo(0);
	}
	
	@Test
	public void testCancelInvitationDoesntShownInAttachments() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/cancelInvitation.eml");

		SyncKey firstAllocatedSyncKey = new SyncKey("9d66d5cd-f636-466a-9309-ba84feda617f");
		SyncKey secondAllocatedSyncKey = new SyncKey("c701603f-f61b-4419-96a0-3098863fcd71");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(firstAllocatedSyncKey)
				.id(3)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(4)
				.build();
		
		ServerId emailServerId = inboxCollectionId.serverId(1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();

		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		expect(calendarDao.getMSEventUidFor(anyObject(EventExtId.class), eq(user.device)))
			.andReturn(new MSEventUid("1"));
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(response, inboxCollectionId);
		MSEmail mail = (MSEmail) Iterables.getOnlyElement(collectionResponse.getItemChanges()).getData();
		Set<MSAttachement> attachments = mail.getAttachments();
		assertThat(attachments.size()).isEqualTo(0);
	}
	
	@Test
	public void testModifiedOccurenceInvitationDoesntShownInAttachments() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/modifiedOccurenceInvitation.eml");

		SyncKey firstAllocatedSyncKey = new SyncKey("04a3200c-064d-491f-91f3-1c04e6d46dd5");
		SyncKey secondAllocatedSyncKey = new SyncKey("7ecea940-44bc-4525-a005-68a963408ebd");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(firstAllocatedSyncKey)
				.id(3)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(4)
				.build();
		
		ServerId emailServerId = inboxCollectionId.serverId(1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(response, inboxCollectionId);
		MSEmail mail = (MSEmail) Iterables.getOnlyElement(collectionResponse.getItemChanges()).getData();
		Set<MSAttachement> attachments = mail.getAttachments();
		assertThat(attachments.size()).isEqualTo(0);
	}

	@Test
	public void testOnlyOneOpushObmSyncConnectionUsed() throws Exception {
		SyncKey firstAllocatedSyncKey = new SyncKey("4a2c7db8-b532-40a0-92c3-bfebb8da8f00");
		SyncKey secondAllocatedSyncKey = new SyncKey("55df3cf4-b70d-4df2-ac48-d31646994321");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		
		Date firstDate = date("2012-10-09T16:22:53");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(firstDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(firstDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		
		loginClient.logout(user.accessToken);
		expectLastCall().anyTimes();
		// Login is done in authentication
		expect(loginClient.authenticate(user.user.getLoginAtDomain(), UserPassword.valueOf(String.valueOf(user.password))))
			.andReturn(user.accessToken).anyTimes();
		expect(deviceDao.getDevice(user.user, 
				user.deviceId, 
				user.userAgent,
				user.deviceProtocolVersion))
				.andReturn(
						new Device(user.device.getDatabaseId(), user.deviceType, user.deviceId, new Properties(), user.deviceProtocolVersion))
						.anyTimes();
		expect(deviceDao.getPolicyKey(user.user, user.deviceId, PolicyStatus.ACCEPTED))
			.andReturn(0l).anyTimes();
		
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(firstDate)
			.anyTimes();
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey, secondAllocatedSyncKey);
		
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, contactCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(calendarClient.getSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(firstDate)
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		
		expect(bookClient.listContactsChanged(user.accessToken, firstDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(),
					ImmutableSet.<Integer> of(),
					firstDate));
		
		mocksControl.replay();
		opushServer.start();

		SyncResponse syncResponse = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient)
				.run(Sync.builder(decoder)
						.collection(AnalysedSyncCollection.builder().collectionId(calendarCollectionId).syncKey(firstAllocatedSyncKey).dataType(CALENDAR)
								.options(SyncCollectionOptions.builder()
										.filterType(FilterType.ONE_WEEK_BACK)
										.build())
								.build())
						.collection(AnalysedSyncCollection.builder().collectionId(contactCollectionId).syncKey(firstAllocatedSyncKey).dataType(CONTACTS)
								.options(SyncCollectionOptions.builder()
										.filterType(FilterType.ONE_WEEK_BACK)
										.build())
								.build())
						.build());
		
		mocksControl.verify();
		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
	}
	
	@Test
	public void testUserPasswordWithDegreeSentAsISO() throws Exception {
		char[] complexPassword = "password°".toCharArray();
		OpushUser user = users.buildUser("jaures", complexPassword, "Jean Jaures");
		String userEmail = user.user.getLoginAtDomain();
		greenMail.setUser(userEmail, String.valueOf(complexPassword));

		SyncKey firstAllocatedSyncKey = new SyncKey("663e5b84-e6ba-472b-a385-18f7f92e99d6");
		SyncKey secondAllocatedSyncKey = new SyncKey("773e5b84-e6ba-472b-a385-18f7f92e99d6");
		SyncKey thirdAllocatedSyncKey = new SyncKey("883e5b84-e6ba-472b-a385-18f7f92e99d6");
		
		Date firstDate = date("2012-10-09T16:22:53");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(firstDate)
				.syncKey(firstAllocatedSyncKey)
				.id(3)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(firstDate)
				.syncKey(secondAllocatedSyncKey)
				.id(4)
				.build();
		ItemSyncState thirdAllocatedState = ItemSyncState.builder()
				.syncDate(firstDate)
				.syncKey(thirdAllocatedSyncKey)
				.id(5)
				.build();
		
		loginClient.logout(user.accessToken);
		expectLastCall().anyTimes();
		// Login is done in authentication
		expect(loginClient.authenticate(userEmail, UserPassword.valueOf(String.valueOf(complexPassword)))).andReturn(user.accessToken).anyTimes();
		expect(deviceDao.getDevice(user.user, user.deviceId, user.userAgent, user.deviceProtocolVersion))
			.andReturn(user.device).anyTimes();
		expect(deviceDao.getPolicyKey(user.user, user.deviceId, PolicyStatus.ACCEPTED))
			.andReturn(5l).anyTimes();
		
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(firstDate).anyTimes();
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey, thirdAllocatedSyncKey);
		
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, thirdAllocatedState, inboxCollectionId);

		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(dateService.getCurrentDate()).andReturn(thirdAllocatedState.getSyncDate()).once();
		expect(calendarClient.getUserEmail(user.accessToken)).andReturn(user.user.getLoginAtDomain());
		expect(calendarClient.getSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder().lastSync(firstDate).build());
		
		mocksControl.replay();
		opushServer.start();
		
		SyncResponse syncResponse = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient)
				.run(Sync.builder(decoder)
						.collection(AnalysedSyncCollection.builder().collectionId(calendarCollectionId).syncKey(firstAllocatedSyncKey).dataType(CALENDAR)
								.options(SyncCollectionOptions.builder()
										.filterType(FilterType.ONE_WEEK_BACK)
										.build())
								.build())
						.collection(AnalysedSyncCollection.builder().collectionId(inboxCollectionId).syncKey(firstAllocatedSyncKey).dataType(EMAIL)
								.options(SyncCollectionOptions.builder()
										.filterType(FilterType.ONE_WEEK_BACK)
										.build())
								.build())
						.build());
		
		mocksControl.verify();
		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		assertThat(syncTestUtils.getCollectionWithId(syncResponse, calendarCollectionId).getStatus()).isEqualTo(SyncStatus.OK);
		assertThat(syncTestUtils.getCollectionWithId(syncResponse, inboxCollectionId).getStatus()).isEqualTo(SyncStatus.OK);
	}

	@Test
	public void testEventSensitivityNotModifiedByDevices() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("b91c285a-46c3-436e-8ad5-4b851830150e");
		SyncKey secondAllocatedSyncKey = new SyncKey("96e8dcae-ac37-4b6f-a310-f7fcd5c3d858");
		SyncKey thirdAllocatedSyncKey = new SyncKey("82a066ae-c8c5-4a89-a706-0ea5e7750f5e");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int thirdAllocatedStateId = 5;
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, 
				secondAllocatedSyncKey, thirdAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		Date secondDate = date("2012-10-10T16:22:53");
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState thirdAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(thirdAllocatedSyncKey)
				.id(thirdAllocatedStateId)
				.build();
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expectCollectionDaoPerformInitialSync(firstAllocatedState, calendarCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, secondAllocatedSyncKey, secondAllocatedState, thirdAllocatedState, calendarCollectionId);

		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(thirdAllocatedState.getSyncDate()).once();
		EventExtId eventExtId = new EventExtId("1");
		EventObmId eventObmId = new EventObmId(1);
		Event event = new Event();
		event.setUid(eventObmId);
		event.setTitle("event");
		event.setExtId(eventExtId);
		event.setOwner(user.user.getEmail());
		event.setOwnerEmail(user.user.getEmail());
		event.setStartDate(secondDate);
		event.setMeetingStatus(EventMeetingStatus.IS_NOT_A_MEETING);
		event.setPrivacy(EventPrivacy.CONFIDENTIAL);
		
		// First Sync
		expect(calendarClient.getFirstSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.updates(Lists.newArrayList(event))
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain()).anyTimes();
		
		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		Calendar calendar = DateUtils.getEpochCalendar(timeZone);
		MSEventUid msEventUid = new MSEventUid("1");
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(msEventUid);
		msEvent.setSubject("event");
		msEvent.setSensitivity(CalendarSensitivity.CONFIDENTIAL);
		msEvent.setBusyStatus(CalendarBusyStatus.FREE);
		msEvent.setAllDayEvent(false);
		msEvent.setDtStamp(calendar.getTime());
		msEvent.setTimeZone(timeZone);
		msEvent.setStartTime(secondDate);
		msEvent.setEndTime(secondDate);
		expect(calendarDao.getMSEventUidFor(anyObject(EventExtId.class), eq(user.device)))
			.andReturn(msEventUid);
		
		ServerId serverId = calendarCollectionId.serverId(Integer.valueOf(msEvent.getUid().serializeToString()));
		String clientId = null;
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId)).andReturn(false);
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();

		// Second Sync
		expect(calendarDao.getEventExtIdFor(msEventUid, user.device))
			.andReturn(eventExtId);
		expect(calendarClient.getEventFromId(user.accessToken, user.user.getEmail(), eventObmId))
			.andReturn(event);
		// We check that the Privacy of the Event is not modified on update (only this field, other may vary
		expect(calendarClient.modifyEvent(eq(user.accessToken), eq(user.user.getEmail()), eventPrivacyEq(event), eq(true), eq(true)))
			.andReturn(event);
		expect(calendarClient.getSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.build());
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.run(Sync.builder(decoder)
						.collection(AnalysedSyncCollection.builder().collectionId(calendarCollectionId).dataType(CALENDAR)
								.syncKey(initialSyncKey)		
								.windowSize(ONE_WINDOWS_SIZE)
								.options(SyncCollectionOptions.builder().filterType(FilterType.THREE_DAYS_BACK).build())
								.build())
						.build());
		SyncResponse syncResponse = opClient.run(Sync.builder(decoder)
						.collection(AnalysedSyncCollection.builder().collectionId(calendarCollectionId).dataType(CALENDAR)
								.syncKey(firstAllocatedSyncKey)
								.windowSize(ONE_WINDOWS_SIZE)
								.options(SyncCollectionOptions.builder().filterType(FilterType.THREE_DAYS_BACK).build())
								.build())
						.build());
		
		msEvent.setSensitivity(CalendarSensitivity.PERSONAL);
		SyncResponse updatedSyncResponse = opClient.run(
				Sync.builder(decoder).encoder(encoderFactory).device(user.device)
					.collection(AnalysedSyncCollection.builder().collectionId(calendarCollectionId)
							.syncKey(secondAllocatedSyncKey).dataType(CALENDAR)
							.command(SyncCollectionCommandRequest.builder().type(SyncCommand.CHANGE)
									.serverId(serverId).clientId(clientId).applicationData(msEvent).build())									
							.options(SyncCollectionOptions.builder().filterType(FilterType.THREE_DAYS_BACK).build())
							.build())
					.build());
		
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(syncResponse, calendarCollectionId);
		SyncCollectionResponse updatedCollectionResponse = syncTestUtils.getCollectionWithId(updatedSyncResponse, calendarCollectionId);

		syncTestUtils.assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		syncTestUtils.assertEqualsWithoutApplicationData(updatedCollectionResponse.getItemChanges(), 
				ImmutableList.<ItemChange> of());
	}
	
	private static Event eventPrivacyEq(Event value) {
		reportMatcher(new EventPrivacyEquals(value));
		return null;
	}

	/*
	 * Checks only Event.privacy attribute 
	 */
	private static class EventPrivacyEquals extends Equals {

		public EventPrivacyEquals(Object expected) {
			super(expected);
		}
		
		@Override
		public boolean matches(final Object actual) {
			if (this.getExpected() == null) {
				return actual == null;
			}
			return ((Event) getExpected()).getPrivacy().equals(((Event) actual).getPrivacy());
		}
	}

	@Test
	public void syncShouldRespondWhenAskingTwiceForFirstSyncKeyAndWindowing() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("d6b223c4-c7ea-499e-8f65-d94e3121efb8");
		SyncKey secondAllocatedSyncKey = new SyncKey("0e5e9ebc-5210-423f-a15d-5d360c031220");
		SyncKey newSecondAllocatedSyncKey = new SyncKey("279cf50e-9f28-4c92-8ac6-7d3e7cab1056");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int newSecondAllocatedStateId = 5;
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, secondAllocatedSyncKey, newSecondAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState newSecondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(newSecondAllocatedSyncKey)
				.id(newSecondAllocatedStateId)
				.build();
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(3);
		expectCollectionDaoPerformInitialSync(firstAllocatedState, inboxCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, newSecondAllocatedState, inboxCollectionId);
		
		ServerId serverId = inboxCollectionId.serverId(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId)).andReturn(false).times(2);
		
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse newFirstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		
		mocksControl.verify();

		assertThat(firstSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse firstCollectionResponse = syncTestUtils.getCollectionWithId(firstSyncResponse, inboxCollectionId);
		syncTestUtils.assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId.serverId(2))
						.isNew(true)
						.build()));
		
		assertThat(newFirstSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse newFirstCollectionResponse = syncTestUtils.getCollectionWithId(newFirstSyncResponse, inboxCollectionId);
		syncTestUtils.assertEqualsWithoutApplicationData(newFirstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId.serverId(2))
						.isNew(true)
						.build()));
	}

	@Test
	public void syncShouldRespondWhenAskingTwiceForFirstSyncKey() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("d6b223c4-c7ea-499e-8f65-d94e3121efb8");
		SyncKey secondAllocatedSyncKey = new SyncKey("0e5e9ebc-5210-423f-a15d-5d360c031220");
		SyncKey newSecondAllocatedSyncKey = new SyncKey("279cf50e-9f28-4c92-8ac6-7d3e7cab1056");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int newSecondAllocatedStateId = 5;
		
		userAccessUtils.mockUsersAccess(Arrays.asList(user));
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, secondAllocatedSyncKey, newSecondAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState newSecondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:25:17"))
				.syncKey(newSecondAllocatedSyncKey)
				.id(newSecondAllocatedStateId)
				.build();
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(dateService.getCurrentDate()).andReturn(newSecondAllocatedState.getSyncDate()).times(1);
		expectCollectionDaoPerformInitialSync(firstAllocatedState, inboxCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, newSecondAllocatedState, inboxCollectionId);
		
		ServerId serverId = inboxCollectionId.serverId(1);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId)).andReturn(false).times(2);
		ServerId serverId2 = inboxCollectionId.serverId(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId2))
			.andReturn(false).times(2);
		
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_HUNDRED_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_HUNDRED_WINDOWS_SIZE);
		SyncResponse newFirstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, ONE_HUNDRED_WINDOWS_SIZE);
		
		mocksControl.verify();

		assertThat(firstSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse firstCollectionResponse = syncTestUtils.getCollectionWithId(firstSyncResponse, inboxCollectionId);
		syncTestUtils.assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId.serverId(1))
						.isNew(true)
						.build(),
					ItemChange.builder()
						.serverId(inboxCollectionId.serverId(2))
						.isNew(true)
						.build()));
		
		assertThat(newFirstSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse newFirstCollectionResponse = syncTestUtils.getCollectionWithId(newFirstSyncResponse, inboxCollectionId);
		syncTestUtils.assertEqualsWithoutApplicationData(newFirstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId.serverId(1))
						.isNew(true)
						.build(),
					ItemChange.builder()
						.serverId(inboxCollectionId.serverId(2))
						.isNew(true)
						.build()));
	}

	@Test
	public void syncShouldRespectFilterTypeDate() throws Exception {
		ServerSetup smtpServerSetup = greenMail.getSmtp().getServerSetup();
		MimeMessage oldMessage = GreenMailUtil.buildSimpleMessage(mailbox, "subject", "old message", smtpServerSetup);
		MimeMessage newMessage = GreenMailUtil.buildSimpleMessage(mailbox, "subject", "new message", smtpServerSetup);
		greenMailUser.deliver(oldMessage, date("2012-08-10T16:22:53"));
		greenMailUser.deliver(newMessage, date("2012-10-01T16:22:53"));
		
		SyncKey firstAllocatedSyncKey = new SyncKey("a181b4e9-7b87-42cf-9e8b-6de8184bed55");
		SyncKey secondAllocatedSyncKey = new SyncKey("6710d6e4-6101-4054-9566-086d6ecf3202");

		userAccessUtils.mockUsersAccess(Arrays.asList(user));
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, secondAllocatedSyncKey);
		
		Date epochPlusOneSecond = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(epochPlusOneSecond)
				.syncKey(firstAllocatedSyncKey)
				.id(3)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(4)
				.build();
		
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(epochPlusOneSecond);
		expectCollectionDaoPerformInitialSync(firstAllocatedState, inboxCollectionId);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);

		ServerId serverId = inboxCollectionId.serverId(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(serverId));
		expectLastCall().once();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		
		opClient.syncEmail(decoder, SyncKey.INITIAL_SYNC_KEY, inboxCollectionId, FilterType.ONE_MONTHS_BACK, ONE_HUNDRED_WINDOWS_SIZE);
		SyncResponse syncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionId, FilterType.ONE_MONTHS_BACK, ONE_HUNDRED_WINDOWS_SIZE);
		
		mocksControl.verify();

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse firstCollectionResponse = syncTestUtils.getCollectionWithId(syncResponse, inboxCollectionId);
		assertThat(firstCollectionResponse.getItemChanges()).hasSize(1);
		MSEmail email = (MSEmail) firstCollectionResponse.getItemChanges().get(0).getData();
		assertThat(CharStreams.toString(new InputStreamReader(email.getBody().getMimeData(), Charsets.UTF_8))).isEqualTo("new message");
	}

	@Test
	public void syncShouldUseDefaultWindowSizeWhenNone() throws Exception {
		int defaultWindowSize = 50;
		int extraWindowSizeItems = 10;
		testUtils.sendMultipleEmails(greenMail, mailbox, defaultWindowSize + extraWindowSizeItems);
		
		SyncKey firstAllocatedSyncKey = new SyncKey("a181b4e9-7b87-42cf-9e8b-6de8184bed55");
		SyncKey secondAllocatedSyncKey = new SyncKey("6710d6e4-6101-4054-9566-086d6ecf3202");

		userAccessUtils.mockUsersAccess(Arrays.asList(user));
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstAllocatedSyncKey, secondAllocatedSyncKey);
		
		Date epochPlusOneSecond = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(epochPlusOneSecond)
				.syncKey(firstAllocatedSyncKey)
				.id(3)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T16:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(4)
				.build();
		
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(epochPlusOneSecond);
		expectCollectionDaoPerformInitialSync(firstAllocatedState, inboxCollectionId);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);

		expect(itemTrackingDao.isServerIdSynced(eq(firstAllocatedState), anyObject(ServerId.class)))
			.andReturn(false).anyTimes();
		itemTrackingDao.markAsSynced(eq(secondAllocatedState), anyObject(Set.class));
		expectLastCall().once();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		
		opClient.syncWithoutOptions(decoder, SyncKey.INITIAL_SYNC_KEY, inboxCollectionId);
		SyncResponse syncResponse = opClient.syncWithoutOptions(decoder, firstAllocatedSyncKey, inboxCollectionId);
		
		mocksControl.verify();

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse firstCollectionResponse = syncTestUtils.getCollectionWithId(syncResponse, inboxCollectionId);
		assertThat(firstCollectionResponse.getItemChanges()).hasSize(defaultWindowSize);
	}
}
