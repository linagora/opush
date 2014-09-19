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
import static org.obm.opush.IntegrationPushTestUtils.mockNextGeneratedSyncKey;
import static org.obm.opush.IntegrationTestUtils.appendToINBOX;
import static org.obm.opush.IntegrationTestUtils.buildWBXMLOpushClient;
import static org.obm.opush.IntegrationUserAccessUtils.mockUsersAccess;
import static org.obm.opush.command.sync.SyncTestUtils.assertEqualsWithoutApplicationData;
import static org.obm.opush.command.sync.SyncTestUtils.getCollectionWithId;
import static org.obm.opush.command.sync.SyncTestUtils.mockCollectionDaoPerformSync;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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
import org.obm.configuration.EmailConfiguration;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.ImapConnectionCounter;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.PendingQueriesLock;
import org.obm.opush.Users;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.CalendarBusyStatus;
import org.obm.push.bean.CalendarSensitivity;
import org.obm.push.bean.Device;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSAttachement;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.MSEventUid;
import org.obm.push.bean.MethodAttachment;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SnapshotKey;
import org.obm.push.bean.SyncCollectionCommandResponse;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncCollectionResponsesResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.exception.DaoException;
import org.obm.push.mail.MailboxService;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.service.DateService;
import org.obm.push.store.CalendarDao;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.DeviceDao;
import org.obm.push.store.DeviceDao.PolicyStatus;
import org.obm.push.store.ItemTrackingDao;
import org.obm.push.store.SnapshotDao;
import org.obm.push.utils.DateUtils;
import org.obm.push.utils.collection.ClassToInstanceAgregateView;
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
import org.obm.sync.push.client.beans.Folder;
import org.obm.sync.push.client.commands.SyncWithDataCommand;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;

@GuiceModule(SyncHandlerWithBackendTestModule.class)
@RunWith(GuiceRunner.class)
public class SyncHandlerWithBackendTest {

	private final static int ONE_WINDOWS_SIZE = 1;
	private final static int ONE_HUNDRED_WINDOWS_SIZE = 100;
	
	@Inject	Injector injector;
	@Inject	Users users;
	@Inject	OpushServer opushServer;
	@Inject	ClassToInstanceAgregateView<Object> classToInstanceMap;
	@Inject GreenMail greenMail;
	@Inject ImapConnectionCounter imapConnectionCounter;
	@Inject PendingQueriesLock pendingQueries;
	@Inject IMocksControl mocksControl;
	@Inject Configuration configuration;
	@Inject MailboxService mailboxService;
	@Inject SyncDecoder decoder;
	@Inject SyncWithDataCommand.Factory syncWithDataCommandFactory;
	@Inject PolicyConfigurationProvider policyConfigurationProvider;
	@Inject TransactionProvider transactionProvider;
	@Inject CassandraServer cassandraServer;
	
	private SnapshotDao snapshotDao;
	private ItemTrackingDao itemTrackingDao;
	private CollectionDao collectionDao;
	private CalendarDao calendarDao;
	private DateService dateService;
	private CalendarClient calendarClient;
	private BookClient bookClient;

	private GreenMailUser greenMailUser;
	private ImapHostManager imapHostManager;
	private OpushUser user;
	private String mailbox;
	private String inboxCollectionPath;
	private String calendarCollectionPath;
	private String contactCollectionPath;
	private int inboxCollectionId;
	private int calendarCollectionId;
	private int contactCollectionId;
	private String inboxCollectionIdAsString;
	private String calendarCollectionIdAsString;
	private String contactCollectionIdAsString;

	private CloseableHttpClient httpClient;

	@Before
	public void init() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
		user = users.jaures;
		greenMail.start();
		mailbox = user.user.getLoginAtDomain();
		greenMailUser = greenMail.setUser(mailbox, user.password);
		imapHostManager = greenMail.getManagers().getImapHostManager();
		imapHostManager.createMailbox(greenMailUser, "Trash");

		inboxCollectionPath = IntegrationTestUtils.buildEmailInboxCollectionPath(user);
		inboxCollectionId = 1234;
		inboxCollectionIdAsString = String.valueOf(inboxCollectionId);
		calendarCollectionPath = IntegrationTestUtils.buildCalendarCollectionPath(user);
		calendarCollectionId = 5678;
		calendarCollectionIdAsString = String.valueOf(calendarCollectionId);
		contactCollectionId = 7891;
		contactCollectionIdAsString = String.valueOf(contactCollectionId);
		contactCollectionPath = IntegrationTestUtils.buildContactCollectionPath(user, contactCollectionIdAsString);
		
		itemTrackingDao = classToInstanceMap.get(ItemTrackingDao.class);
		collectionDao = classToInstanceMap.get(CollectionDao.class);
		calendarDao = classToInstanceMap.get(CalendarDao.class);
		dateService = classToInstanceMap.get(DateService.class);
		calendarClient = classToInstanceMap.get(CalendarClient.class);
		bookClient = classToInstanceMap.get(BookClient.class);
		snapshotDao = injector.getInstance(SnapshotDao.class);

		bindCollectionIdToPath();
		
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
	}

	private void bindCollectionIdToPath() throws Exception {
		expect(collectionDao.getCollectionPath(inboxCollectionId)).andReturn(inboxCollectionPath).anyTimes();
		expect(collectionDao.getCollectionPath(calendarCollectionId)).andReturn(calendarCollectionPath).anyTimes();
		expect(collectionDao.getCollectionPath(contactCollectionId)).andReturn(contactCollectionPath).anyTimes();
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
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
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
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, 
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
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		String serverId = inboxCollectionId + ":2";
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
		
		expect(dateService.getCurrentDate()).andReturn(newSecondAllocatedState.getSyncDate()).times(4);
		expectCollectionDaoPerformInitialSync(newFirstAllocatedState, inboxCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, newFirstAllocatedSyncKey, newFirstAllocatedState, newSecondAllocatedState, inboxCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, newSecondAllocatedSyncKey, newSecondAllocatedState, newThirdAllocatedState, inboxCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, newThirdAllocatedSyncKey, newThirdAllocatedState, newFourthAllocatedState, inboxCollectionId);

		expect(itemTrackingDao.isServerIdSynced(newFirstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
		String serverId2 = inboxCollectionId + ":1";
		expect(itemTrackingDao.isServerIdSynced(newSecondAllocatedState, 
				new ServerId(serverId2)))
			.andReturn(false);
		
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse syncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, newFirstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse secondSyncResponse = opClient.syncEmail(decoder, newSecondAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse thirdSyncResponse = opClient.syncEmail(decoder, newThirdAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(syncResponse, inboxCollectionIdAsString);
		SyncCollectionResponse firstCollectionResponse = getCollectionWithId(firstSyncResponse, inboxCollectionIdAsString);
		SyncCollectionResponse secondCollectionResponse = getCollectionWithId(secondSyncResponse, inboxCollectionIdAsString);
		SyncCollectionResponse thirdCollectionResponse = getCollectionWithId(thirdSyncResponse, inboxCollectionIdAsString);

		assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId + ":2")
						.isNew(true)
						.build()));
		assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId + ":2")
						.isNew(true)
						.build()));
		assertThat(firstCollectionResponse.isMoreAvailable()).isTrue();
		assertEqualsWithoutApplicationData(secondCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId + ":1")
						.isNew(true)
						.build()));
		assertThat(secondCollectionResponse.isMoreAvailable()).isFalse();
		assertThat(thirdCollectionResponse.getItemChanges()).hasSize(0);
		
		assertEmailCountInMailbox(EmailConfiguration.IMAP_INBOX_NAME, 2);
		assertThat(pendingQueries.waitingClose(10, TimeUnit.SECONDS)).isTrue();
		assertThat(imapConnectionCounter.loginCounter.get()).isEqualTo(4);
		assertThat(imapConnectionCounter.closeCounter.get()).isEqualTo(4);
		assertThat(imapConnectionCounter.selectCounter.get()).isEqualTo(4);
		assertThat(imapConnectionCounter.listMailboxesCounter.get()).isEqualTo(0);
	}
	
	@Test
	public void testInitialSyncThenRecreatesAccountOnCalendars() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
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
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, 
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
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);

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
		
		String serverId = calendarCollectionIdAsString + ":" + msEvent.getUid().serializeToString();
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
		
		expectCollectionDaoPerformInitialSync(newFirstAllocatedState, calendarCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, newFirstAllocatedSyncKey, newFirstAllocatedState, newSecondAllocatedState, calendarCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, newSecondAllocatedSyncKey, newSecondAllocatedState, newThirdAllocatedState, calendarCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, newThirdAllocatedSyncKey, newThirdAllocatedState, newFourthAllocatedState, calendarCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(newSecondAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(newThirdAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(newFourthAllocatedState.getSyncDate()).once();
		expect(itemTrackingDao.isServerIdSynced(newFirstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
		String serverId2 = calendarCollectionIdAsString + ":" + msEvent2.getUid().serializeToString();
		expect(itemTrackingDao.isServerIdSynced(newSecondAllocatedState, 
				new ServerId(serverId2)))
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
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse syncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		
		opClient.syncEmail(decoder, initialSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, newFirstAllocatedSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse secondSyncResponse = opClient.syncEmail(decoder, newSecondAllocatedSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse thirdSyncResponse = opClient.syncEmail(decoder, newThirdAllocatedSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(syncResponse, calendarCollectionIdAsString);
		SyncCollectionResponse firstCollectionResponse = getCollectionWithId(firstSyncResponse, calendarCollectionIdAsString);
		SyncCollectionResponse secondCollectionResponse = getCollectionWithId(secondSyncResponse, calendarCollectionIdAsString);
		SyncCollectionResponse thirdCollectionResponse = getCollectionWithId(thirdSyncResponse, calendarCollectionIdAsString);

		assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		assertThat(firstCollectionResponse.isMoreAvailable()).isTrue();
		assertEqualsWithoutApplicationData(secondCollectionResponse.getItemChanges(), 
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
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
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
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, 
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
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, contactCollectionId);

		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		Contact contact = new Contact();
		contact.setUid(1);
		Contact contact2 = new Contact();
		contact2.setUid(2);
		
		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name(contactCollectionIdAsString)
					.uid(AddressBook.Id.valueOf(contactCollectionId))
					.readOnly(false)
					.build())).anyTimes();
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + contactCollectionId))
			.andReturn(contactCollectionId).anyTimes();
		
		expect(bookClient.firstListContactsChanged(user.accessToken, initialDate, contactCollectionId))
			.andReturn(new ContactChanges(ImmutableList.of(contact, contact2),
					ImmutableSet.<Integer> of(),
					secondDate));
		
		String serverId = contactCollectionIdAsString + ":" + contact.getUid();
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
		
		expectCollectionDaoPerformInitialSync(newFirstAllocatedState, contactCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, newFirstAllocatedSyncKey, newFirstAllocatedState, newSecondAllocatedState, contactCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, newSecondAllocatedSyncKey, newSecondAllocatedState, newThirdAllocatedState, contactCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, newThirdAllocatedSyncKey, newThirdAllocatedState, newFourthAllocatedState, contactCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(newSecondAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(newThirdAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(newFourthAllocatedState.getSyncDate()).once();
		expect(itemTrackingDao.isServerIdSynced(newFirstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
		String serverId2 = contactCollectionIdAsString + ":" + contact2.getUid();
		expect(itemTrackingDao.isServerIdSynced(newSecondAllocatedState, 
				new ServerId(serverId2)))
			.andReturn(false);
		
		expect(bookClient.firstListContactsChanged(user.accessToken, initialDate, contactCollectionId))
		.andReturn(new ContactChanges(ImmutableList.of(contact, contact2),
				ImmutableSet.<Integer> of(),
				newSecondDate));
	
		expect(bookClient.listContactsChanged(user.accessToken, newThirdDate, contactCollectionId))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(),
					ImmutableSet.<Integer> of(),
					newThirdDate));
		
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		opClient.syncEmail(decoder, initialSyncKey, contactCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse syncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, contactCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		
		opClient.syncEmail(decoder, initialSyncKey, contactCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, newFirstAllocatedSyncKey, contactCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse secondSyncResponse = opClient.syncEmail(decoder, newSecondAllocatedSyncKey, contactCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse thirdSyncResponse = opClient.syncEmail(decoder, newThirdAllocatedSyncKey, contactCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(syncResponse, contactCollectionIdAsString);
		SyncCollectionResponse firstCollectionResponse = getCollectionWithId(firstSyncResponse, contactCollectionIdAsString);
		SyncCollectionResponse secondCollectionResponse = getCollectionWithId(secondSyncResponse, contactCollectionIdAsString);
		SyncCollectionResponse thirdCollectionResponse = getCollectionWithId(thirdSyncResponse, contactCollectionIdAsString);

		assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		assertThat(firstCollectionResponse.isMoreAvailable()).isTrue();
		assertEqualsWithoutApplicationData(secondCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId2)
						.isNew(true)
						.build()));
		assertThat(secondCollectionResponse.isMoreAvailable()).isFalse();
		assertThat(thirdCollectionResponse.getItemChanges()).hasSize(0);
	}

	private void expectCollectionDaoPerformInitialSync(ItemSyncState itemSyncState, int collectionId)
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
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, secondAllocatedSyncKey, thirdAllocatedSyncKey);
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
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(4);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, secondAllocatedSyncKey, secondAllocatedState, thirdAllocatedState, inboxCollectionId);

		String serverId = inboxCollectionIdAsString + ":" + 1;
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, new ServerId(serverId))).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(new ServerId(serverId)));
		expectLastCall().once();
		itemTrackingDao.markAsDeleted(thirdAllocatedState, ImmutableSet.of(new ServerId(serverId)));
		expectLastCall().once();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		greenMail.deleteEmailFromInbox(greenMailUser, 1);
		greenMail.expungeInbox(greenMailUser);
		SyncResponse secondSyncResponse = opClient.syncWithCommand(decoder, secondAllocatedSyncKey, inboxCollectionIdAsString, SyncCommand.FETCH, serverId);
		
		mocksControl.verify();

		SyncCollectionResponse firstCollectionResponse = getCollectionWithId(firstSyncResponse, inboxCollectionIdAsString);

		assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		
		assertThat(secondSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse collectionResponse = getCollectionWithId(secondSyncResponse, inboxCollectionIdAsString);
		SyncCollectionResponsesResponse responses = collectionResponse.getResponses();
		List<SyncCollectionCommandResponse> fetches = responses.getCommandsForType(SyncCommand.FETCH);
		assertThat(fetches).containsOnly(SyncCollectionCommandResponse.builder()
				.syncStatus(SyncStatus.OBJECT_NOT_FOUND)
				.type(SyncCommand.FETCH)
				.serverId(serverId)
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
		appendToINBOX(greenMailUser, "eml/attachmentWithoutContentDisposition.eml");

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
		
		ServerId emailServerId = new ServerId(inboxCollectionIdAsString + ":" + 1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, secondAllocatedSyncKey);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, PIMDataType.EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(response, inboxCollectionIdAsString);
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
		appendToINBOX(greenMailUser, "eml/iCSAsAttachment.eml");

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
		
		ServerId emailServerId = new ServerId(inboxCollectionIdAsString + ":" + 1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, secondAllocatedSyncKey);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, PIMDataType.EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(response, inboxCollectionIdAsString);
		MSEmail mail = (MSEmail) Iterables.getOnlyElement(collectionResponse.getItemChanges()).getData();
		Set<MSAttachement> attachments = mail.getAttachments();
		assertThat(attachments.size()).isEqualTo(1);
		MSAttachement attachment = Iterables.getOnlyElement(attachments);
		
		assertThat(attachment.getMethod()).isEqualTo(MethodAttachment.NormalAttachment);
		assertThat(attachment.getDisplayName()).isEqualTo("attachment.ics");
	}
	
	@Test
	public void testForwardedEmailWithAttachments() throws Exception {
		appendToINBOX(greenMailUser, "eml/forwardedEmailWithAttachments.eml");

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
		
		ServerId emailServerId = new ServerId(inboxCollectionIdAsString + ":" + 1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, secondAllocatedSyncKey);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, PIMDataType.EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(response, inboxCollectionIdAsString);
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
		appendToINBOX(greenMailUser, "eml/invitation.eml");

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
		
		ServerId emailServerId = new ServerId(inboxCollectionIdAsString + ":" + 1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, secondAllocatedSyncKey);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		expect(calendarDao.getMSEventUidFor(anyObject(EventExtId.class), eq(user.device)))
			.andReturn(new MSEventUid("1"));
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, PIMDataType.EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(response, inboxCollectionIdAsString);
		MSEmail mail = (MSEmail) Iterables.getOnlyElement(collectionResponse.getItemChanges()).getData();
		Set<MSAttachement> attachments = mail.getAttachments();
		assertThat(attachments.size()).isEqualTo(0);
	}
	
	@Test
	public void testCancelInvitationDoesntShownInAttachments() throws Exception {
		appendToINBOX(greenMailUser, "eml/cancelInvitation.eml");

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
		
		ServerId emailServerId = new ServerId(inboxCollectionIdAsString + ":" + 1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();

		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, secondAllocatedSyncKey);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		expect(calendarDao.getMSEventUidFor(anyObject(EventExtId.class), eq(user.device)))
			.andReturn(new MSEventUid("1"));
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, PIMDataType.EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(response, inboxCollectionIdAsString);
		MSEmail mail = (MSEmail) Iterables.getOnlyElement(collectionResponse.getItemChanges()).getData();
		Set<MSAttachement> attachments = mail.getAttachments();
		assertThat(attachments.size()).isEqualTo(0);
	}
	
	@Test
	public void testModifiedOccurenceInvitationDoesntShownInAttachments() throws Exception {
		appendToINBOX(greenMailUser, "eml/modifiedOccurenceInvitation.eml");

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
		
		ServerId emailServerId = new ServerId(inboxCollectionIdAsString + ":" + 1);
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, emailServerId)).andReturn(false);
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(emailServerId));
		expectLastCall().once();
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, secondAllocatedSyncKey);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse response = opClient.sync(decoder, firstAllocatedSyncKey, inboxCollectionId, PIMDataType.EMAIL);
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(response, inboxCollectionIdAsString);
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
		
		LoginClient loginClient = classToInstanceMap.get(LoginClient.class);
		loginClient.logout(user.accessToken);
		expectLastCall().anyTimes();
		// Login is done in authentication
		expect(loginClient.authenticate(user.user.getLoginAtDomain(), user.password))
			.andReturn(user.accessToken).anyTimes();
		DeviceDao deviceDao = classToInstanceMap.get(DeviceDao.class);
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
		mockNextGeneratedSyncKey(classToInstanceMap, secondAllocatedSyncKey, secondAllocatedSyncKey);
		
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, contactCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(calendarClient.getSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(firstDate)
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		
		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name(contactCollectionIdAsString)
					.uid(AddressBook.Id.valueOf(contactCollectionId))
					.readOnly(false)
					.build()));
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + contactCollectionId))
			.andReturn(contactCollectionId);
		expect(bookClient.listContactsChanged(user.accessToken, firstDate, contactCollectionId))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(),
					ImmutableSet.<Integer> of(),
					firstDate));
		
		mocksControl.replay();
		opushServer.start();

		SyncResponse syncResponse = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient)
				.sync(decoder, firstAllocatedSyncKey,
					new Folder(calendarCollectionIdAsString),
					new Folder(contactCollectionIdAsString));
		
		mocksControl.verify();
		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
	}
	
	@Test
	public void testUserPasswordWithDegreeSentAsISO() throws Exception {
		String complexPassword = "password°";
		OpushUser user = users.buildUser("jaures", complexPassword, "Jean Jaures");
		String userEmail = user.user.getLoginAtDomain();
		greenMail.setUser(userEmail, complexPassword);
		bindCollectionIdToPath();

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
		
		LoginClient loginClient = classToInstanceMap.get(LoginClient.class);
		loginClient.logout(user.accessToken);
		expectLastCall().anyTimes();
		// Login is done in authentication
		expect(loginClient.authenticate(userEmail, complexPassword)).andReturn(user.accessToken).anyTimes();
		DeviceDao deviceDao = classToInstanceMap.get(DeviceDao.class);
		expect(deviceDao.getDevice(user.user, user.deviceId, user.userAgent, user.deviceProtocolVersion))
			.andReturn(user.device).anyTimes();
		expect(deviceDao.getPolicyKey(user.user, user.deviceId, PolicyStatus.ACCEPTED))
			.andReturn(5l).anyTimes();
		
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(firstDate).anyTimes();
		mockNextGeneratedSyncKey(classToInstanceMap, secondAllocatedSyncKey, thirdAllocatedSyncKey);
		
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, thirdAllocatedState, inboxCollectionId);

		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).times(2);
		expect(dateService.getCurrentDate()).andReturn(thirdAllocatedState.getSyncDate()).once();
		expect(calendarClient.getUserEmail(user.accessToken)).andReturn(user.user.getLoginAtDomain());
		expect(calendarClient.getSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder().lastSync(firstDate).build());
		
		mocksControl.replay();
		opushServer.start();
		
		SyncResponse syncResponse = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient)
				.sync(decoder, firstAllocatedSyncKey,
					new Folder(calendarCollectionIdAsString),
					new Folder(inboxCollectionIdAsString));
		
		mocksControl.verify();
		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		assertThat(getCollectionWithId(syncResponse, calendarCollectionIdAsString).getStatus()).isEqualTo(SyncStatus.OK);
		assertThat(getCollectionWithId(syncResponse, inboxCollectionIdAsString).getStatus()).isEqualTo(SyncStatus.OK);
	}

	@Test
	public void testEventSensitivityNotModifiedByDevices() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("b91c285a-46c3-436e-8ad5-4b851830150e");
		SyncKey secondAllocatedSyncKey = new SyncKey("96e8dcae-ac37-4b6f-a310-f7fcd5c3d858");
		SyncKey thirdAllocatedSyncKey = new SyncKey("82a066ae-c8c5-4a89-a706-0ea5e7750f5e");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int thirdAllocatedStateId = 5;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, 
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
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, secondAllocatedSyncKey, secondAllocatedState, thirdAllocatedState, calendarCollectionId);

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
		
		String serverId = calendarCollectionIdAsString + ":" + msEvent.getUid().serializeToString();
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
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
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse syncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, calendarCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		
		msEvent.setSensitivity(CalendarSensitivity.PERSONAL);
		String clientId = syncResponse.getProcessedClientIds().get(serverId);
		SyncResponse updatedSyncResponse = opClient.syncWithCommand(syncWithDataCommandFactory, user.device, secondAllocatedSyncKey, 
				calendarCollectionIdAsString, SyncCommand.CHANGE, serverId, clientId, msEvent);
		
		mocksControl.verify();

		SyncCollectionResponse collectionResponse = getCollectionWithId(syncResponse, calendarCollectionIdAsString);
		SyncCollectionResponse updatedCollectionResponse = getCollectionWithId(updatedSyncResponse, calendarCollectionIdAsString);

		assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		assertEqualsWithoutApplicationData(updatedCollectionResponse.getItemChanges(), 
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
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("d6b223c4-c7ea-499e-8f65-d94e3121efb8");
		SyncKey secondAllocatedSyncKey = new SyncKey("0e5e9ebc-5210-423f-a15d-5d360c031220");
		SyncKey newSecondAllocatedSyncKey = new SyncKey("279cf50e-9f28-4c92-8ac6-7d3e7cab1056");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int newSecondAllocatedStateId = 5;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, secondAllocatedSyncKey, newSecondAllocatedSyncKey);
		
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
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, newSecondAllocatedState, inboxCollectionId);
		
		String serverId = inboxCollectionId + ":2";
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false).times(2);
		
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		SyncResponse newFirstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_WINDOWS_SIZE);
		
		mocksControl.verify();

		assertThat(firstSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse firstCollectionResponse = getCollectionWithId(firstSyncResponse, inboxCollectionIdAsString);
		assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId + ":2")
						.isNew(true)
						.build()));
		
		assertThat(newFirstSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse newFirstCollectionResponse = getCollectionWithId(newFirstSyncResponse, inboxCollectionIdAsString);
		assertEqualsWithoutApplicationData(newFirstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId + ":2")
						.isNew(true)
						.build()));
	}

	@Test
	public void syncShouldRespondWhenAskingTwiceForFirstSyncKey() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("d6b223c4-c7ea-499e-8f65-d94e3121efb8");
		SyncKey secondAllocatedSyncKey = new SyncKey("0e5e9ebc-5210-423f-a15d-5d360c031220");
		SyncKey newSecondAllocatedSyncKey = new SyncKey("279cf50e-9f28-4c92-8ac6-7d3e7cab1056");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int newSecondAllocatedStateId = 5;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, secondAllocatedSyncKey, newSecondAllocatedSyncKey);
		
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
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, inboxCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, newSecondAllocatedState, inboxCollectionId);
		
		String serverId = inboxCollectionId + ":1";
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false).times(2);
		String serverId2 = inboxCollectionId + ":2";
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, 
				new ServerId(serverId2)))
			.andReturn(false).times(2);
		
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		sendTwoEmailsToImapServer();
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_HUNDRED_WINDOWS_SIZE);
		SyncResponse firstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_HUNDRED_WINDOWS_SIZE);
		SyncResponse newFirstSyncResponse = opClient.syncEmail(decoder, firstAllocatedSyncKey, inboxCollectionIdAsString, FilterType.THREE_DAYS_BACK, ONE_HUNDRED_WINDOWS_SIZE);
		
		mocksControl.verify();

		assertThat(firstSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse firstCollectionResponse = getCollectionWithId(firstSyncResponse, inboxCollectionIdAsString);
		assertEqualsWithoutApplicationData(firstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId + ":1")
						.isNew(true)
						.build(),
					ItemChange.builder()
						.serverId(inboxCollectionId + ":2")
						.isNew(true)
						.build()));
		
		assertThat(newFirstSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse newFirstCollectionResponse = getCollectionWithId(newFirstSyncResponse, inboxCollectionIdAsString);
		assertEqualsWithoutApplicationData(newFirstCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(inboxCollectionId + ":1")
						.isNew(true)
						.build(),
					ItemChange.builder()
						.serverId(inboxCollectionId + ":2")
						.isNew(true)
						.build()));
	}
}
