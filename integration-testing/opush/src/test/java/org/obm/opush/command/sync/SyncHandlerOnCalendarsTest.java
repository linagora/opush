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
import static org.obm.DateUtils.date;
import static org.obm.opush.IntegrationPushTestUtils.mockNextGeneratedSyncKey;
import static org.obm.opush.IntegrationTestUtils.buildWBXMLOpushClient;
import static org.obm.opush.IntegrationUserAccessUtils.mockUsersAccess;
import static org.obm.opush.command.sync.SyncTestUtils.assertEqualsWithoutApplicationData;
import static org.obm.opush.command.sync.SyncTestUtils.getCollectionWithId;
import static org.obm.opush.command.sync.SyncTestUtils.mockCollectionDaoPerformSync;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Files;
import org.easymock.IMocksControl;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.Users;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.CalendarBusyStatus;
import org.obm.push.bean.CalendarSensitivity;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.MSEventException;
import org.obm.push.bean.MSEventUid;
import org.obm.push.bean.MSRecurrence;
import org.obm.push.bean.RecurrenceType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.exception.DaoException;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.service.DateService;
import org.obm.push.store.CalendarDao;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.ItemTrackingDao;
import org.obm.push.utils.DateUtils;
import org.obm.push.utils.collection.ClassToInstanceAgregateView;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventObmId;
import org.obm.sync.calendar.EventRecurrence;
import org.obm.sync.client.calendar.CalendarClient;
import org.obm.sync.items.EventChanges;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.beans.Folder;
import org.obm.sync.push.client.commands.SyncWithDataCommand;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

@GuiceModule(SyncHandlerWithBackendTestModule.class)
@RunWith(GuiceRunner.class)
public class SyncHandlerOnCalendarsTest {

	@Inject	Users users;
	@Inject	OpushServer opushServer;
	@Inject	ClassToInstanceAgregateView<Object> classToInstanceMap;
	@Inject IMocksControl mocksControl;
	@Inject Configuration configuration;
	@Inject SyncDecoder decoder;
	@Inject SyncWithDataCommand.Factory syncWithDataCommandFactory;
	@Inject PolicyConfigurationProvider policyConfigurationProvider;
	@Inject CassandraServer cassandraServer;
	
	private ItemTrackingDao itemTrackingDao;
	private CollectionDao collectionDao;
	private CalendarDao calendarDao;
	private CalendarClient calendarClient;
	private DateService dateService;

	private OpushUser user;
	private String calendarCollectionPath;
	private int calendarCollectionId;
	private String calendarCollectionIdAsString;

	private CloseableHttpClient httpClient;

	@Before
	public void init() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
		user = users.jaures;

		calendarCollectionPath = IntegrationTestUtils.buildCalendarCollectionPath(user);
		calendarCollectionId = 5678;
		calendarCollectionIdAsString = String.valueOf(calendarCollectionId);
		
		itemTrackingDao = classToInstanceMap.get(ItemTrackingDao.class);
		collectionDao = classToInstanceMap.get(CollectionDao.class);
		calendarDao = classToInstanceMap.get(CalendarDao.class);
		dateService = classToInstanceMap.get(DateService.class);
		calendarClient = classToInstanceMap.get(CalendarClient.class);

		expect(collectionDao.getCollectionPath(calendarCollectionId)).andReturn(calendarCollectionPath).anyTimes();
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
	}

	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		httpClient.close();
		Files.delete(configuration.dataDir);
	}
	
	@Test
	public void clientMayAskForAnOldSyncKey() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("ba9cc33e-0be1-40f9-94ee-4a28760e7dbb");
		SyncKey secondAllocatedSyncKey = new SyncKey("2c24fbbc-6a94-4d6a-b9a7-7b4974a09a3c");
		SyncKey thirdAllocatedSyncKey = new SyncKey("54ad87c8-9324-4e1c-ae63-daae556be7be");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int thirdAllocatedStateId = 5;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, secondAllocatedSyncKey, thirdAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		DateTime secondDateTime = DateTime.parse("2012-10-10T16:22:53.000Z");
		Date secondDate = secondDateTime.toDate();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		
		ItemSyncState thirdAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-10T17:22:53.000Z"))
				.syncKey(thirdAllocatedSyncKey)
				.id(thirdAllocatedStateId)
				.build();
		
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expectCollectionDaoPerformInitialSync(initialSyncKey, firstAllocatedState, calendarCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, thirdAllocatedState, calendarCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate());
		expect(dateService.getCurrentDate()).andReturn(thirdAllocatedState.getSyncDate());

		// First Sync
		EventRecurrence eventRecurrence = new EventRecurrence();
		EventObmId eventObmId = new EventObmId(1);
		EventExtId eventExtId = new EventExtId("1");
		Event event = new Event();
		event.setUid(eventObmId);
		event.setExtId(eventExtId);
		event.setTitle("event");
		event.setRecurrence(eventRecurrence);
		event.setStartDate(secondDate);
		event.setOwner(user.user.getEmail());
		expect(calendarClient.getFirstSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.updates(ImmutableList.of(event))
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		
		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		Calendar calendar = DateUtils.getEpochCalendar(timeZone);
		MSRecurrence recurrence = new MSRecurrence();
		recurrence.setType(RecurrenceType.DAILY);
		MSEventUid msEventUid = new MSEventUid("1");
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(msEventUid);
		msEvent.setSubject("event");
		msEvent.setSensitivity(CalendarSensitivity.NORMAL);
		msEvent.setBusyStatus(CalendarBusyStatus.FREE);
		msEvent.setAllDayEvent(false);
		msEvent.setDtStamp(calendar.getTime());
		msEvent.setTimeZone(timeZone);
		msEvent.setRecurrence(recurrence);
		msEvent.setStartTime(secondDate);
		expect(calendarDao.getMSEventUidFor(eventExtId, user.device))
			.andReturn(msEventUid);
		
		ServerId serverId = new ServerId(calendarCollectionIdAsString + ":" + msEvent.getUid().serializeToString());
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(serverId));
		expectLastCall().once();
		itemTrackingDao.markAsSynced(thirdAllocatedState, ImmutableSet.of(serverId));
		expectLastCall().once();
		
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId))
			.andReturn(false)
			.times(2);
		
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse initialSyncResponse = opClient.sync(decoder, initialSyncKey, new Folder(calendarCollectionIdAsString));
		SyncResponse syncResponse = opClient.sync(decoder, firstAllocatedSyncKey, new Folder(calendarCollectionIdAsString));
		SyncResponse sameSyncResponse = opClient.sync(decoder, firstAllocatedSyncKey, new Folder(calendarCollectionIdAsString));
		
		mocksControl.verify();

		SyncCollectionResponse initialCollectionResponse = getCollectionWithId(initialSyncResponse, calendarCollectionIdAsString);
		assertThat(initialCollectionResponse.getItemChanges()).isEmpty();

		SyncCollectionResponse collectionResponse = getCollectionWithId(syncResponse, calendarCollectionIdAsString);
		assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
					.serverId(serverId.toString())
					.isNew(true)
					.build()));

		SyncCollectionResponse sameCollectionResponse = getCollectionWithId(sameSyncResponse, calendarCollectionIdAsString);
		assertEqualsWithoutApplicationData(sameCollectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId.toString())
						.isNew(true)
						.build()));
	}
	
	@Test
	public void syncShouldReturnServerErrorWhenTwoExceptionAtSameDate() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey firstAllocatedSyncKey = new SyncKey("ba9cc33e-0be1-40f9-94ee-4a28760e7dbb");
		SyncKey secondAllocatedSyncKey = new SyncKey("2c24fbbc-6a94-4d6a-b9a7-7b4974a09a3c");
		SyncKey thirdAllocatedSyncKey = new SyncKey("f909aa0f-cc7e-44b7-8395-2d6e69be54a4");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		
		mockUsersAccess(classToInstanceMap, Arrays.asList(user));
		mockNextGeneratedSyncKey(classToInstanceMap, firstAllocatedSyncKey, secondAllocatedSyncKey, thirdAllocatedSyncKey);
		
		Date initialDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(initialDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		DateTime secondDateTime = DateTime.parse("2012-10-10T16:22:53.000Z");
		Date secondDate = secondDateTime.toDate();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(secondDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		
		expect(dateService.getEpochPlusOneSecondDate()).andReturn(initialDate).anyTimes();
		
		expectCollectionDaoPerformInitialSync(firstAllocatedState, calendarCollectionId);
		mockCollectionDaoPerformSync(collectionDao, user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, calendarCollectionId);
		expect(collectionDao.findItemStateForKey(secondAllocatedSyncKey)).andReturn(secondAllocatedState);

		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate());

		// First Sync
		EventRecurrence eventRecurrence = new EventRecurrence();
		EventObmId eventObmId = new EventObmId(1);
		EventExtId eventExtId = new EventExtId("1");
		Event event = new Event();
		event.setUid(eventObmId);
		event.setExtId(eventExtId);
		event.setTitle("event");
		event.setRecurrence(eventRecurrence);
		event.setStartDate(secondDate);
		event.setOwner(user.user.getEmail());
		expect(calendarClient.getFirstSyncEventDate(eq(user.accessToken), eq(user.user.getLoginAtDomain()), anyObject(Date.class)))
			.andReturn(EventChanges.builder()
					.lastSync(secondDate)
					.updates(ImmutableList.of(event))
					.build());
		expect(calendarClient.getUserEmail(user.accessToken))
			.andReturn(user.user.getLoginAtDomain());
		
		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		Calendar calendar = DateUtils.getEpochCalendar(timeZone);
		MSRecurrence recurrence = new MSRecurrence();
		recurrence.setType(RecurrenceType.DAILY);
		MSEventUid msEventUid = new MSEventUid("1");
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(msEventUid);
		msEvent.setSubject("event");
		msEvent.setSensitivity(CalendarSensitivity.NORMAL);
		msEvent.setBusyStatus(CalendarBusyStatus.FREE);
		msEvent.setAllDayEvent(false);
		msEvent.setDtStamp(calendar.getTime());
		msEvent.setTimeZone(timeZone);
		msEvent.setRecurrence(recurrence);
		msEvent.setStartTime(secondDate);
		expect(calendarDao.getMSEventUidFor(eventExtId, user.device))
			.andReturn(msEventUid);
		
		String serverId = calendarCollectionIdAsString + ":" + msEvent.getUid().serializeToString();
		
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, 
				new ServerId(serverId)))
			.andReturn(false);
		
		// Update from device
		Date exceptionDate = secondDateTime.plusDays(1).toDate();
		
		MSRecurrence recurrenceUpdated = new MSRecurrence();
		recurrenceUpdated.setType(RecurrenceType.DAILY);
		MSEventException msEventException = new MSEventException();
		msEventException.setExceptionStartTime(exceptionDate);
		msEventException.setDeleted(true);
		msEventException.setStartTime(exceptionDate);
		msEventException.setEndTime(exceptionDate);
		msEventException.setSensitivity(CalendarSensitivity.NORMAL);
		msEventException.setBusyStatus(CalendarBusyStatus.FREE);
		msEventException.setAllDayEvent(false);
		
		MSEventException msEventException2 = new MSEventException();
		msEventException2.setExceptionStartTime(exceptionDate);
		msEventException2.setStartTime(exceptionDate);
		msEventException2.setEndTime(exceptionDate);
		msEventException2.setSensitivity(CalendarSensitivity.NORMAL);
		msEventException2.setBusyStatus(CalendarBusyStatus.FREE);
		msEventException2.setAllDayEvent(false);
		msEventException2.setSubject("modified subject");
		
		String clientId = "123";
		MSEvent msEventUpdated = new MSEvent();
		msEventUpdated.setUid(msEventUid);
		msEventUpdated.setSubject("event");
		msEventUpdated.setSensitivity(CalendarSensitivity.NORMAL);
		msEventUpdated.setBusyStatus(CalendarBusyStatus.FREE);
		msEventUpdated.setAllDayEvent(false);
		msEventUpdated.setDtStamp(calendar.getTime());
		msEventUpdated.setTimeZone(timeZone);
		msEventUpdated.setRecurrence(recurrenceUpdated);
		msEventUpdated.setStartTime(secondDate);
		msEventUpdated.setExceptions(ImmutableList.of(msEventException, msEventException2));
		expect(calendarDao.getEventExtIdFor(msEventUid, user.device))
			.andReturn(eventExtId);
		expect(calendarClient.getEventFromId(user.accessToken, user.user.getEmail(), eventObmId))
			.andReturn(event);
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse initialSyncResponse = opClient.sync(decoder, initialSyncKey, new Folder(calendarCollectionIdAsString));
		SyncResponse syncResponse = opClient.sync(decoder, firstAllocatedSyncKey, new Folder(calendarCollectionIdAsString));
		
		SyncResponse updateSyncResponse = opClient.syncWithCommand(syncWithDataCommandFactory, user.device, secondAllocatedSyncKey, calendarCollectionIdAsString, SyncCommand.CHANGE, 
				serverId, clientId, msEventUpdated);
		mocksControl.verify();

		SyncCollectionResponse initialCollectionResponse = getCollectionWithId(initialSyncResponse, calendarCollectionIdAsString);
		assertThat(initialCollectionResponse.getItemChanges()).isEmpty();

		SyncCollectionResponse collectionResponse = getCollectionWithId(syncResponse, calendarCollectionIdAsString);
		assertEqualsWithoutApplicationData(collectionResponse.getItemChanges(), 
				ImmutableList.of(
					ItemChange.builder()
						.serverId(serverId)
						.isNew(true)
						.build()));
		
		assertThat(updateSyncResponse.getStatus()).isEqualTo(SyncStatus.SERVER_ERROR);
	}

	private void expectCollectionDaoPerformInitialSync(ItemSyncState itemSyncState, int collectionId) throws DaoException {
		expect(collectionDao.updateState(user.device, collectionId, itemSyncState.getSyncKey(), itemSyncState.getSyncDate()))
			.andReturn(itemSyncState);
		collectionDao.resetCollection(user.device, collectionId);
		expectLastCall();
	}
}
