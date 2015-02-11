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
package org.obm.push.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.util.Strings;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.obm.icalendar.ICalendar;
import org.obm.icalendar.Ical4jHelper;
import org.obm.icalendar.Ical4jUser;
import org.obm.icalendar.ical4jwrapper.ICalendarEvent;
import org.obm.push.backend.CollectionPath;
import org.obm.push.backend.CollectionPath.Builder;
import org.obm.push.backend.WindowingEvent;
import org.obm.push.backend.WindowingEventChanges;
import org.obm.push.bean.AttendeeStatus;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.MSEventUid;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.ICalendarConverterException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.impl.ObmSyncBackend.WindowingChangesDelta;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.resource.OpushResourcesHolder;
import org.obm.push.service.ClientIdService;
import org.obm.push.service.DateService;
import org.obm.push.service.EventService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.impl.MappingService;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.WindowingDao;
import org.obm.push.utils.DateUtils;
import org.obm.sync.NotAllowedException;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.EventAlreadyExistException;
import org.obm.sync.auth.EventNotFoundException;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.calendar.Attendee;
import org.obm.sync.calendar.CalendarInfo;
import org.obm.sync.calendar.ContactAttendee;
import org.obm.sync.calendar.DeletedEvent;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventObmId;
import org.obm.sync.calendar.Participation;
import org.obm.sync.calendar.UserAttendee;
import org.obm.sync.client.calendar.CalendarClient;
import org.obm.sync.items.EventChanges;
import org.obm.sync.items.ParticipationChanges;
import org.slf4j.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Provider;


public class CalendarBackendTest {

	private User user;
	private Device device;
	private UserDataRequest userDataRequest;
	private AccessToken token;
	private FolderSyncState lastKnownState;
	private FolderSyncState outgoingSyncState;
	private String rootCalendarPath;
	private CalendarCollectionPath userCalendarCollectionPath;
	
	private MappingService mappingService;
	private CalendarClient calendarClient;
	private CalendarClient.Factory calendarClientFactory;
	private EventConverter eventConverter;
	private EventService eventService;
	private Provider<CollectionPath.Builder> collectionPathBuilderProvider;
	private ConsistencyEventChangesLogger consistencyLogger;
	private EventExtId.Factory eventExtIdFactory;
	private WindowingDao windowingDao;
	private ClientIdService clientIdService;
	private Ical4jHelper ical4jHelper;
	private Ical4jUser.Factory ical4jUserFactory;
	private DateService dateService;
	private OpushResourcesHolder opushResourcesHolder;
	private FolderSnapshotDao folderSnapshotDao;
	
	private CalendarBackend calendarBackend;
	private IMocksControl mockControl;
	private Builder collectionPathBuilder;
	private CloseableHttpClient httpClient;
	
	private static class CalendarCollectionPath extends CollectionPath {

		public CalendarCollectionPath(String rootCalendarPath, String displayName) {
			super(String.format("%s%s", rootCalendarPath, displayName), 
					PIMDataType.CALENDAR, displayName);
		}
	}
	
	@Before
	public void setUp() {
		this.user = Factory.create().createUser("test@test", "test@domain", "displayName");
		this.device = new Device.Factory().create(null, "iPhone", "iOs 5", new DeviceId("my phone"), null);
		this.userDataRequest = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		this.token = new AccessToken(0, "OBM");
		this.lastKnownState = buildFolderSyncState(new FolderSyncKey("1234567890a"));
		this.outgoingSyncState = buildFolderSyncState(new FolderSyncKey("1234567890b"));
		this.rootCalendarPath = "obm:\\\\test@test\\calendar\\";
		this.userCalendarCollectionPath = new CalendarCollectionPath(rootCalendarPath, "test");
		this.httpClient = HttpClientBuilder.create().build();

		mockControl = createControl();
		
		this.mappingService = mockControl.createMock(MappingService.class);
		this.calendarClient = mockControl.createMock(CalendarClient.class);
		this.calendarClientFactory = mockControl.createMock(CalendarClient.Factory.class);
		this.eventConverter = mockControl.createMock(EventConverter.class);
		this.eventService = mockControl.createMock(EventService.class);
		this.collectionPathBuilderProvider = mockControl.createMock(Provider.class);
		this.collectionPathBuilder = mockControl.createMock(CollectionPath.Builder.class);
		expect(collectionPathBuilderProvider.get()).andReturn(collectionPathBuilder).anyTimes();
		this.consistencyLogger = mockControl.createMock(ConsistencyEventChangesLogger.class);
		this.eventExtIdFactory = mockControl.createMock(EventExtId.Factory.class);
		this.windowingDao = mockControl.createMock(WindowingDao.class);
		this.clientIdService = mockControl.createMock(ClientIdService.class);
		this.ical4jHelper = mockControl.createMock(Ical4jHelper.class);
		this.ical4jUserFactory = mockControl.createMock(Ical4jUser.Factory.class);
		this.dateService = mockControl.createMock(DateService.class);
		this.opushResourcesHolder = mockControl.createMock(OpushResourcesHolder.class);
		this.folderSnapshotDao = mockControl.createMock(FolderSnapshotDao.class);
		expect(opushResourcesHolder.getAccessToken()).andReturn(token).anyTimes();
		expect(opushResourcesHolder.getHttpClient()).andReturn(httpClient).anyTimes();
		
		consistencyLogger.log(anyObject(Logger.class), anyObject(EventChanges.class));
		expectLastCall().anyTimes();
		
		expect(calendarClientFactory.create(anyObject(HttpClient.class)))
			.andReturn(calendarClient).anyTimes();
		
		this.calendarBackend = new CalendarBackend(mappingService, 
				calendarClientFactory, 
				eventConverter, 
				eventService, 
				collectionPathBuilderProvider, consistencyLogger, eventExtIdFactory, 
				windowingDao,
				clientIdService,
				ical4jHelper,
				ical4jUserFactory,
				dateService,
				opushResourcesHolder,
				folderSnapshotDao);
	}
	
	@After
	public void teardown() throws IOException {
		httpClient.close();
	}
	
	@Test
	public void testGetPIMDataType() {
		assertThat(calendarBackend.getPIMDataType()).isEqualTo(PIMDataType.CALENDAR);
	}
	
	@Test
	public void testInitialCalendarChanges() throws Exception {
		String calendarDisplayName = user.getLogin() + " calendar";
		String defaultCalendarName = rootCalendarPath + user.getLogin();
		
		CollectionId collectionMappingId = CollectionId.of(1);
		List<CollectionPath> knownCollections = ImmutableList.of(); 
		expectMappingServiceListLastKnowCollection(lastKnownState, knownCollections);
		expectMappingServiceSearchThenCreateCollection(defaultCalendarName, collectionMappingId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(collectionMappingId));
		expectMappingServiceLookupCollection(defaultCalendarName, collectionMappingId);
		
		expectBuildCollectionPath(user.getLogin());

		mockControl.replay();
		
		HierarchyCollectionChanges hierarchyItemsChanges = calendarBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		
		mockControl.verify();
		
		CollectionChange expectedItemChange = CollectionChange.builder()
				.collectionId(collectionMappingId)
				.parentCollectionId(CollectionId.ROOT)
				.displayName(calendarDisplayName)
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.isNew(true)
				.build();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).hasSize(1);
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(expectedItemChange);
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void testNoCalendarChanges() throws Exception {
		String calendarDisplayName = user.getLogin();
		String defaultCalendarName = rootCalendarPath + user.getLogin();
		
		CollectionId collectionMappingId = CollectionId.of(1);
		List<CollectionPath> knownCollections = ImmutableList.<CollectionPath>of(
				new CollectionPathTest(defaultCalendarName, PIMDataType.CALENDAR, calendarDisplayName));
		expectMappingServiceListLastKnowCollection(lastKnownState, knownCollections);
		expectMappingServiceFindCollection(defaultCalendarName, collectionMappingId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(collectionMappingId));
		
		expectBuildCollectionPath(user.getLogin());

		mockControl.replay();
		
		HierarchyCollectionChanges hierarchyItemsChanges = calendarBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		
		mockControl.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
	}

	@Test
	public void testInitialCalendarChangesWhenMultipleCalendar() throws Exception {
		FolderSyncState lastKnownState = buildFolderSyncState(new FolderSyncKey("1234567890a"));
		FolderSyncState outgoingSyncState = buildFolderSyncState(new FolderSyncKey("1234567890b"));

		device = new Device.Factory().create(null, "MultipleCalendarsDevice", "iOs 5", new DeviceId("my phone"), null);
		userDataRequest = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		
		CollectionId calendar1MappingId = CollectionId.of(1);
		String calendar1DisplayName = "test calendar";
		String calendar1BackendName = "test";
		String calendar1CollectionPath = rootCalendarPath + calendar1BackendName;
		CollectionId calendar2MappingId = CollectionId.of(2);
		String calendar2DisplayName = "alias calendar";
		String calendar2BackendName = "alias";
		String calendar2CollectionPath = rootCalendarPath + calendar2BackendName;

		expectObmSyncCalendarChanges(
				newCalendarInfo("test", "test@test"),
				newCalendarInfo("alias", "superman@test"));
		
		expectBuildCollectionPath(calendar1BackendName);
		expectBuildCollectionPath(calendar2BackendName);
		
		List<CollectionPath> knownCollections = ImmutableList.of(); 
		expectMappingServiceListLastKnowCollection(lastKnownState, knownCollections);
		expectMappingServiceSearchThenCreateCollection(calendar1CollectionPath, calendar1MappingId);
		expectMappingServiceSearchThenCreateCollection(calendar2CollectionPath, calendar2MappingId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(calendar1MappingId, calendar2MappingId));
		expectMappingServiceLookupCollection(calendar1CollectionPath, calendar1MappingId);
		expectMappingServiceLookupCollection(calendar2CollectionPath, calendar2MappingId);
		
		mockControl.replay();
		
		HierarchyCollectionChanges hierarchyItemsChanges = calendarBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		
		mockControl.verify();

		CollectionChange expectedItemChange1 = CollectionChange.builder()
				.collectionId(calendar1MappingId)
				.parentCollectionId(CollectionId.ROOT)
				.displayName(calendar1DisplayName)
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.isNew(true)
				.build();
		CollectionChange expectedItemChange2 = CollectionChange.builder()
				.collectionId(calendar2MappingId)
				.parentCollectionId(CollectionId.ROOT)
				.displayName(calendar2DisplayName)
				.folderType(FolderType.USER_CREATED_CALENDAR_FOLDER)
				.isNew(true)
				.build();
		
		assertThat(hierarchyItemsChanges.getCollectionChanges()).hasSize(2);
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(expectedItemChange1, expectedItemChange2);
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
	}

	@Test
	public void testDeletionWhenMultipleCalendar() throws Exception {
		FolderSyncState lastKnownState = buildFolderSyncState(new FolderSyncKey("1234567890a"));
		FolderSyncState outgoingSyncState = buildFolderSyncState(new FolderSyncKey("1234567890b"));

		device = new Device.Factory().create(null, "MultipleCalendarsDevice", "iOs 5", new DeviceId("my phone"), null);
		userDataRequest = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		
		CollectionId calendar1MappingId = CollectionId.of(1);
		String calendar1DisplayName = "added calendar";
		String calendar1CollectionPath = rootCalendarPath + "added";
		CollectionId calendar2MappingId = CollectionId.of(2);
		String calendar2DisplayName = "deleted calendar";
		String calendar2CollectionPath = rootCalendarPath + "deleted";

		expectObmSyncCalendarChanges(newCalendarInfo("added", calendar1DisplayName));
		
		expectBuildCollectionPath("added");

		List<CollectionPath> knownCollections = ImmutableList.<CollectionPath>of(
				new CollectionPathTest(calendar2CollectionPath, PIMDataType.CALENDAR, calendar2DisplayName));
		expectMappingServiceListLastKnowCollection(lastKnownState, knownCollections);
		expectMappingServiceSearchThenCreateCollection(calendar1CollectionPath, calendar1MappingId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(calendar1MappingId));
		expectMappingServiceLookupCollection(calendar1CollectionPath, calendar1MappingId);
		expectMappingServiceLookupCollection(calendar2CollectionPath, calendar2MappingId);
		
		mockControl.replay();
		
		HierarchyCollectionChanges hierarchyItemsChanges = calendarBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		
		mockControl.verify();

		CollectionChange expectedItemChange1 = CollectionChange.builder()
				.collectionId(calendar1MappingId)
				.parentCollectionId(CollectionId.ROOT)
				.displayName(calendar1DisplayName)
				.folderType(FolderType.USER_CREATED_CALENDAR_FOLDER)
				.isNew(true)
				.build();
		CollectionDeletion expectedItemChange2 = CollectionDeletion.builder()
				.collectionId(calendar2MappingId)
				.build();
		
		assertThat(hierarchyItemsChanges.getCollectionChanges()).hasSize(1);
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(expectedItemChange1);
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).hasSize(1);
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).containsOnly(expectedItemChange2);
	}

	@Test
	public void collectionDisplayNameForCalendar() throws Exception {
		CollectionId calendarMappingId = CollectionId.of(1);
		String calendarBackendName = "test";
		String calendarDisplayName = calendarBackendName + " calendar";
		String calendarCollectionPath = rootCalendarPath + calendarBackendName;

		expectBuildCollectionPath(calendarBackendName);

		expectMappingServiceListLastKnowCollection(lastKnownState, ImmutableList.<CollectionPath>of());
		expectMappingServiceSearchThenCreateCollection(calendarCollectionPath, calendarMappingId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(calendarMappingId));
		expectMappingServiceLookupCollection(calendarCollectionPath, calendarMappingId);
		
		mockControl.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = calendarBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mockControl.verify();

		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(CollectionChange.builder()
				.displayName(calendarDisplayName)
				.collectionId(CollectionId.of(1))
				.parentCollectionId(CollectionId.ROOT)
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.isNew(true)
				.build());
	}

	@Test
	public void collectionDisplayNameForMultipleCalendar() throws Exception {
		CollectionId calendarMappingId = CollectionId.of(1);
		String calendarBackendName = "test";
		String calendarDisplayName = calendarBackendName + " calendar";
		String calendarCollectionPath = rootCalendarPath + calendarBackendName;

		CollectionId calendar2MappingId = CollectionId.of(2);
		String calendar2BackendName = "test2";
		String calendar2DisplayName = calendar2BackendName + " calendar";
		String calendar2CollectionPath = rootCalendarPath + calendar2BackendName;

		device = new Device.Factory().create(null, "MultipleCalendarsDevice", "iOs 5", new DeviceId("my phone"), null);
		userDataRequest = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		
		expectObmSyncCalendarChanges(
				newCalendarInfo("test", calendarBackendName),
				newCalendarInfo("test2", calendar2BackendName));

		expectBuildCollectionPath(calendarBackendName);
		expectBuildCollectionPath(calendar2BackendName);

		expectMappingServiceListLastKnowCollection(lastKnownState, ImmutableList.<CollectionPath>of());
		expectMappingServiceSearchThenCreateCollection(calendarCollectionPath, calendarMappingId);
		expectMappingServiceSearchThenCreateCollection(calendar2CollectionPath, calendar2MappingId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(calendarMappingId, calendar2MappingId));
		expectMappingServiceLookupCollection(calendarCollectionPath, calendarMappingId);
		expectMappingServiceLookupCollection(calendar2CollectionPath, calendar2MappingId);
		
		mockControl.replay();

		HierarchyCollectionChanges hierarchyItemsChanges = calendarBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		
		mockControl.verify();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(
				CollectionChange.builder()
					.displayName(calendarDisplayName)
					.collectionId(CollectionId.of(1))
					.parentCollectionId(CollectionId.ROOT)
					.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
					.isNew(true)
					.build(),
				CollectionChange.builder()
					.displayName(calendar2DisplayName)
					.collectionId(CollectionId.of(2))
					.parentCollectionId(CollectionId.ROOT)
					.folderType(FolderType.USER_CREATED_CALENDAR_FOLDER)
					.isNew(true)
					.build()
				);
	}

	private void expectBuildCollectionPath(String backendName) {
		CollectionPath collectionPath = new CalendarCollectionPath(rootCalendarPath, backendName);
		
		expect(collectionPathBuilder.userDataRequest(userDataRequest))
			.andReturn(collectionPathBuilder).once();
		
		expect(collectionPathBuilder.pimType(PIMDataType.CALENDAR))
			.andReturn(collectionPathBuilder).once();
		
		expect(collectionPathBuilder.backendName(backendName))
			.andReturn(collectionPathBuilder).once();
		
		expect(collectionPathBuilder.build()).andReturn(collectionPath).once();
	}

	private void expectObmSyncCalendarChanges(CalendarInfo...calendarInfos) throws ServerFault {
		expect(calendarClient.listCalendars(token, null, null, null))
			.andReturn(Arrays.asList(calendarInfos)).once();
	}

	private CalendarInfo newCalendarInfo(String uid, String mail) {
		CalendarInfo calendarInfo = new CalendarInfo();
		calendarInfo.setUid(uid);
		calendarInfo.setMail(mail);
		return calendarInfo;
	}

	private void expectMappingServiceSnapshot(FolderSyncState outgoingSyncState, ImmutableSet<CollectionId> immutableSet)
			throws DaoException {

		mappingService.snapshotCollections(outgoingSyncState, immutableSet);
		expectLastCall();
	}

	private void expectMappingServiceListLastKnowCollection(FolderSyncState incomingSyncState,
			List<CollectionPath> collectionPaths) throws DaoException {
		
		expect(mappingService.listCollections(userDataRequest, incomingSyncState))
			.andReturn(collectionPaths).once();
	}

	private void expectMappingServiceFindCollection(String collectionPath, CollectionId collectionMappingId)
		throws CollectionNotFoundException, DaoException {
		
		expect(mappingService.getCollectionIdFor(device, collectionPath))
			.andReturn(collectionMappingId).once();
	}
	
	private void expectMappingServiceSearchThenCreateCollection(String collectionPath, CollectionId collectionMappingId)
		throws CollectionNotFoundException, DaoException {
		
		expect(mappingService.getCollectionIdFor(device, collectionPath))
			.andThrow(new CollectionNotFoundException()).once();
		
		expect(mappingService.createCollectionMapping(device, collectionPath))
			.andReturn(collectionMappingId).once();
	}
	
	private void expectMappingServiceLookupCollection(String collectionPath, CollectionId collectionMappingId)
		throws CollectionNotFoundException, DaoException {
		
		expectMappingServiceFindCollection(collectionPath, collectionMappingId);
	}

	@Test
	public void testGetEstimateSize() throws Exception {
		Date currentDate = DateUtils.getCurrentDate();
		ItemSyncState lastKnownState = ItemSyncState.builder()
				.syncDate(currentDate)
				.syncKey(new SyncKey("1234567890a"))
				.build();
		CollectionId collectionId = CollectionId.of(1);

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		EventChanges eventChanges = expectTwoDeletedAndTwoUpdatedEventChanges(currentDate, 11, 12, 21, 22);
		
		expect(calendarClient.getSync(token, "test", currentDate))
			.andReturn(eventChanges).once();
		
		expect(calendarClient.getUserEmail(token))
			.andReturn("test").anyTimes();

		expectConvertUpdatedEventsToMSEvents(eventChanges);
		
		mockControl.replay();
		
		BodyPreference.Builder bodyPreferenceBuilder = BodyPreference.builder();
		BodyPreference bodyPreference = bodyPreferenceBuilder.build();
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder()
				.filterType(FilterType.ALL_ITEMS)
				.bodyPreferences(ImmutableList.of(bodyPreference))
				.build();
		
		int itemEstimateSize = calendarBackend.getItemEstimateSize(userDataRequest, lastKnownState, collectionId, syncCollectionOptions);
		
		mockControl.verify();
		
		assertThat(itemEstimateSize).isEqualTo(4);
	}
	
	@Test 
	public void testGetChanged() throws Exception {
		Date currentDate = DateUtils.getCurrentDate();
		SyncKey syncKey = new SyncKey("1234567890a");
		ItemSyncState lastKnownState = ItemSyncState.builder()
				.syncDate(currentDate)
				.syncKey(syncKey)
				.build();

		CollectionId collectionId = CollectionId.of(1);

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		EventChanges eventChanges = expectTwoDeletedAndTwoUpdatedEventChanges(currentDate, 11, 12, 21, 22);
		expect(calendarClient.getSync(token, "test", currentDate)).andReturn(eventChanges).once();
		expect(calendarClient.getUserEmail(token)).andReturn("test@test").anyTimes();
		expectConvertUpdatedEventsToMSEvents(eventChanges);
		
		mockControl.replay();
		
		BodyPreference.Builder bodyPreferenceBuilder = BodyPreference.builder();
		BodyPreference bodyPreference = bodyPreferenceBuilder.build();
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder()
				.filterType(FilterType.ALL_ITEMS)
				.bodyPreferences(ImmutableList.of(bodyPreference))
				.build();
		
		WindowingChangesDelta<WindowingEvent> allChanges = calendarBackend.getAllChanges(userDataRequest, lastKnownState, collectionId, syncCollectionOptions);
		
		mockControl.verify();
		
		assertThat(allChanges.getDeltaDate()).isEqualTo(currentDate);
		assertThat(allChanges.getWindowingChanges()).isEqualTo(WindowingEventChanges.builder()
				.changes(ImmutableList.of(
						WindowingEvent.builder().uid(21).build(), 
						WindowingEvent.builder().uid(22).build()))
				.deletions(ImmutableList.of(
						WindowingEvent.builder().uid(11).build(),
						WindowingEvent.builder().uid(12).build()))
				.build());
	}
	
	@Test 
	public void testGetAllChangesOnFirstSync() throws Exception {
		Date currentDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		SyncKey syncKey = new SyncKey("1234567890a");
		ItemSyncState lastKnownState = ItemSyncState.builder()
				.syncDate(currentDate)
				.syncKey(syncKey)
				.build();

		CollectionId collectionId = CollectionId.of(1);

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		EventChanges eventChanges = expectTwoUpdatedEventChanges(currentDate, 21, 22);
		expect(calendarClient.getFirstSync(token, "test", currentDate)).andReturn(eventChanges).once();
		expect(calendarClient.getUserEmail(token)).andReturn("test@test").anyTimes();
		expectConvertUpdatedEventsToMSEvents(eventChanges);
		
		mockControl.replay();
		
		BodyPreference.Builder bodyPreferenceBuilder = BodyPreference.builder();
		BodyPreference bodyPreference = bodyPreferenceBuilder.build();
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder()
				.filterType(FilterType.ALL_ITEMS)
				.bodyPreferences(ImmutableList.of(bodyPreference))
				.build();
		
		WindowingChangesDelta<WindowingEvent> allChanges = calendarBackend.getAllChanges(userDataRequest, lastKnownState, collectionId, syncCollectionOptions);
		
		mockControl.verify();
		
		assertThat(allChanges.getDeltaDate()).isEqualTo(currentDate);
		assertThat(allChanges.getWindowingChanges()).isEqualTo(WindowingEventChanges.builder()
				.changes(ImmutableList.of(
						WindowingEvent.builder().uid(21).build(), 
						WindowingEvent.builder().uid(22).build()))
				.deletions(ImmutableList.<WindowingEvent> of())
				.build());
	}

	private EventChanges expectTwoDeletedAndTwoUpdatedEventChanges(Date currentDate, int deletedObmId, int deletedObmId2,
			int createdObmId, int createdObmId2) {
		Set<DeletedEvent> deletedEvents = ImmutableSet.of(
				createDeletedEvent(new EventObmId(deletedObmId), new EventExtId(Strings.valueOf(deletedObmId))),
				createDeletedEvent(new EventObmId(deletedObmId2), new EventExtId(Strings.valueOf(deletedObmId2))));		
		List<Event> updated = new ArrayList<Event>();
		updated.add(createEvent(createdObmId));
		updated.add(createEvent(createdObmId2));
		
		return EventChanges.builder()
					.lastSync(currentDate)
					.deletes(deletedEvents)
					.updates(updated)
					.participationChanges(ImmutableList.<ParticipationChanges> of())
					.build();
	}

	private EventChanges expectTwoUpdatedEventChanges(Date currentDate, int createdObmId, int createdObmId2) {
		List<Event> updated = new ArrayList<Event>();
		updated.add(createEvent(createdObmId));
		updated.add(createEvent(createdObmId2));
		
		return EventChanges.builder()
					.lastSync(currentDate)
					.deletes(ImmutableList.<DeletedEvent> of())
					.updates(updated)
					.participationChanges(ImmutableList.<ParticipationChanges> of())
					.build();
	}

	private Event createEvent(int obmId) {
		Event event = new Event();
		event.setUid(new EventObmId(obmId));
		return event;
	}

	private DeletedEvent createDeletedEvent(EventObmId eventObmId, EventExtId eventExtId) {
		return DeletedEvent.builder()
					.eventObmId(eventObmId.getObmId())
					.eventExtId(eventExtId.getExtId())
					.build();
	}
	
	private void expectConvertUpdatedEventsToMSEvents(EventChanges eventChanges) throws DaoException, ConversionException {
		for (Event event : eventChanges.getUpdated()) {
			expect(eventService.convertEventToMSEvent(userDataRequest, event))
				.andReturn(null).once();
		}
	}

	@Test
	public void testCreateExternalEventIsAccept() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		ServerId serverId = null;
		String clientId = "3";
		String clientIdHash = "54661110";

		Event oldEvent = null;
		Event creatingEvent = new Event();
		String eventExtIdString = "00000123-0456-0789-0012-000000000345";
		EventExtId eventExtId = new EventExtId(eventExtIdString);
		creatingEvent.setExtId(eventExtId);
		boolean eventIsResolvedAsInternal = false;
		
		MSEvent creatingMSEvent = new MSEvent();
		creatingMSEvent.setUid(new MSEventUid("abc0123"));
		int createdObmId = 12315648;

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		expect(clientIdService.hash(userDataRequest, clientId)).andReturn(clientIdHash);
		
		expect(eventService.getEventExtIdFor(creatingMSEvent.getUid(), device)).andReturn(eventExtIdString);
		expect(calendarClient.getEventFromExtId(token, "test", eventExtId))
			.andReturn(oldEvent).once();
		
		expect(eventConverter.isInternalEvent(oldEvent, eventExtId)).andReturn(eventIsResolvedAsInternal);
		expect(eventConverter.convert(user, oldEvent, creatingMSEvent, eventIsResolvedAsInternal))
			.andReturn(creatingEvent).once();

		expect(calendarClient.createEvent(eq(token), eq("test"), eq(creatingEvent), eq(true), anyObject(String.class)))
			.andReturn(new EventObmId(createdObmId));
		
		mockControl.replay();
		ServerId serverIdFor = calendarBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, creatingMSEvent);
		mockControl.verify();
		
		assertThat(serverIdFor).isEqualTo(collectionId.serverId(createdObmId));
	}
	
	@Test
	public void testCreateInternalEvent() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		String clientId = "13";
		String clientIdHash = "135660464";
		ServerId serverId = null;

		EventExtId eventExtId = new EventExtId(null);
		Event oldEvent = null;
		Event creatingEvent = new Event();
		String generatedEventExtIdString = "00000123-0456-0789-0012-000000000345";
		EventExtId generatedEventExtID = new EventExtId(generatedEventExtIdString);
		boolean eventIsResolvedAsInternal = true;
		
		MSEvent creatingMSEvent = new MSEvent();
		creatingMSEvent.setUid(new MSEventUid("abc0123"));
		creatingMSEvent.setObmSequence(4);
		int createdObmId = 12315648;
		
		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		expect(clientIdService.hash(userDataRequest, clientId)).andReturn(clientIdHash);
		
		expect(eventService.getEventExtIdFor(creatingMSEvent.getUid(), device)).andReturn(null);
		expect(eventConverter.isInternalEvent(oldEvent, eventExtId)).andReturn(eventIsResolvedAsInternal);
		expect(eventConverter.convert(user, oldEvent, creatingMSEvent, eventIsResolvedAsInternal))
			.andReturn(creatingEvent).once();

		expect(eventExtIdFactory.generate()).andReturn(generatedEventExtID);
		eventService.trackEventExtIdMSEventUidTranslation(generatedEventExtIdString, creatingMSEvent.getUid(), device);
		expectLastCall();
		
		expect(calendarClient.createEvent(eq(token), eq("test"), eq(creatingEvent), eq(true), anyObject(String.class)))
			.andReturn(new EventObmId(createdObmId));
		
		mockControl.replay();
		ServerId serverIdFor = calendarBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, creatingMSEvent);
		mockControl.verify();
		
		assertThat(serverIdFor).isEqualTo(collectionId.serverId(createdObmId));
	}

	@Test
	public void testUpdateOwnInternalEvent() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int itemId = 123;
		ServerId serverId = collectionId.serverId(itemId);
		String clientId = null;

		String eventExtIdString = "00000123-0456-0789-0012-000000000345";
		EventExtId eventExtId = new EventExtId(eventExtIdString);
		
		Event oldEvent = new Event();
		oldEvent.setUid(new EventObmId(itemId));
		oldEvent.setInternalEvent(true);
		oldEvent.setOwner("test");
		oldEvent.setOwnerEmail("test@test");
		Event updatingEvent = new Event();
		updatingEvent.setUid(new EventObmId(itemId));
		Event updatedEvent = new Event();
		updatedEvent.setUid(new EventObmId(itemId));
		boolean eventIsResolvedAsInternal = true;
		
		MSEvent updatingMSEvent = new MSEvent();
		updatingMSEvent.setUid(new MSEventUid("abc0123"));

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		expect(eventService.getEventExtIdFor(updatingMSEvent.getUid(), device)).andReturn(eventExtIdString);
		expect(calendarClient.getEventFromId(token, "test", new EventObmId(itemId)))
			.andReturn(oldEvent).once();
		
		expect(eventConverter.isInternalEvent(oldEvent, eventExtId)).andReturn(eventIsResolvedAsInternal);
		expect(eventConverter.convert(user, oldEvent, updatingMSEvent, eventIsResolvedAsInternal))
			.andReturn(updatingEvent).once();

		expect(calendarClient.modifyEvent(token, "test", updatingEvent, true, true))
			.andReturn(updatedEvent);
		
		mockControl.replay();
		ServerId serverIdFor = calendarBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, updatingMSEvent);
		mockControl.verify();
		
		assertThat(serverIdFor).isEqualTo(collectionId.serverId(itemId));
	}
	
	@Test
	public void testUpdateNotOwnInternalEventWithServerId() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int itemId = 123;
		ServerId serverId = collectionId.serverId(itemId);
		String clientId = null;

		String eventExtIdString = "00000123-0456-0789-0012-000000000345";
		EventExtId eventExtId = new EventExtId(eventExtIdString);
		
		Event oldEvent = new Event();
		oldEvent.setUid(new EventObmId(itemId));
		oldEvent.setExtId(eventExtId);
		oldEvent.setInternalEvent(true);
		oldEvent.setSequence(4);
		
		MSEvent updatingMSEvent = new MSEvent();
		updatingMSEvent.setUid(new MSEventUid("abc0123"));
		
		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		expect(eventService.getEventExtIdFor(updatingMSEvent.getUid(), device)).andReturn(eventExtIdString);
		expect(calendarClient.getEventFromId(token, "test", new EventObmId(itemId)))
			.andReturn(oldEvent).once();
		
		expect(eventConverter.getParticipation(AttendeeStatus.ACCEPT)).andReturn(Participation.accepted());
		expect(calendarClient.changeParticipationState(token, "test", eventExtId, Participation.accepted(), 4, true))
			.andReturn(true);
		
		mockControl.replay();
		ServerId serverIdFor = calendarBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, updatingMSEvent);
		mockControl.verify();
		
		assertThat(serverIdFor).isEqualTo(collectionId.serverId(itemId));
	}
	
	@Test
	public void testUpdateNotOwnInternalEventWithClientId() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		String clientId = "13";
		ServerId serverId = null;

		String eventExtIdString = "00000123-0456-0789-0012-000000000345";
		EventExtId eventExtId = new EventExtId(eventExtIdString);
		
		Event oldEvent = new Event();
		oldEvent.setUid(new EventObmId(123));
		oldEvent.setExtId(eventExtId);
		oldEvent.setInternalEvent(true);
		oldEvent.setSequence(4);
		
		MSEvent updatingMSEvent = new MSEvent();
		updatingMSEvent.setUid(new MSEventUid("abc0123"));
		
		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		expect(eventService.getEventExtIdFor(updatingMSEvent.getUid(), device)).andReturn(eventExtIdString);
		expect(calendarClient.getEventFromExtId(token, "test", eventExtId))
			.andReturn(oldEvent).once();
		
		expect(eventConverter.getParticipation(AttendeeStatus.ACCEPT)).andReturn(Participation.accepted());
		expect(calendarClient.changeParticipationState(token, "test", eventExtId, Participation.accepted(), 4, true))
			.andReturn(true);
		
		mockControl.replay();
		ServerId serverIdFor = calendarBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, updatingMSEvent);
		mockControl.verify();
		
		assertThat(serverIdFor).isEqualTo(collectionId.serverId(oldEvent.getObmId().getObmId()));
	}
	
	@Test
	public void testUpdateNotOwnExternalEvent() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int itemId = 123;
		ServerId serverId = collectionId.serverId(itemId);
		String clientId = null;

		String eventExtIdString = "00000123-0456-0789-0012-000000000345";
		EventExtId eventExtId = new EventExtId(eventExtIdString);
		
		Event oldEvent = new Event();
		oldEvent.setUid(new EventObmId(itemId));
		oldEvent.setInternalEvent(true);
		oldEvent.setOwner("test");
		oldEvent.setOwnerEmail("test@test");
		boolean eventIsResolvedAsInternal = false;

		Event updatingEvent = new Event();
		
		MSEvent updatingMSEvent = new MSEvent();
		updatingMSEvent.setUid(new MSEventUid("abc0123"));
		updatingMSEvent.setObmSequence(4);
		
		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		expect(eventService.getEventExtIdFor(updatingMSEvent.getUid(), device)).andReturn(eventExtIdString);
		expect(calendarClient.getEventFromId(token, "test", new EventObmId(itemId)))
			.andReturn(oldEvent).once();
		
		expect(eventConverter.isInternalEvent(oldEvent, eventExtId)).andReturn(eventIsResolvedAsInternal);

		expect(eventConverter.convert(user, oldEvent, updatingMSEvent, eventIsResolvedAsInternal)).andReturn(updatingEvent);
		expect(calendarClient.modifyEvent(token, "test", updatingEvent, true, true)).andReturn(updatingEvent);
		
		mockControl.replay();
		ServerId serverIdFor = calendarBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, updatingMSEvent);
		mockControl.verify();
		
		assertThat(serverIdFor).isEqualTo(collectionId.serverId(itemId));
	}

	@Test
	public void testDelete() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int itemId = 3;
		ServerId serverId = collectionId.serverId(itemId);

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		expectGetAndRemoveEventFromId(itemId);
		
		mockControl.replay();
		
		calendarBackend.delete(userDataRequest, collectionId, serverId, true);
		
		mockControl.verify();
	}

	private void expectGetAndRemoveEventFromId(int itemId)
			throws ServerFault, EventNotFoundException, NotAllowedException {
		
		EventObmId eventObmId = new EventObmId(itemId);
		Event event = new Event();
		event.setUid(eventObmId);
		expect(calendarClient.getEventFromId(token, "test", eventObmId))
			.andReturn(event).once();
		
		calendarClient.removeEventById(token, "test", event.getObmId(), event.getSequence(), true);
		expectLastCall();
	}

	
	@Test
	public void testHandleMettingResponseExternalCreation() throws Exception {
		String calendar = user.getLogin();
		String calendarPath = rootCalendarPath + calendar;
		expectBuildCollectionPath(calendar);
		expect(mappingService.getCollectionIdFor(device, calendarPath)).andReturn(CollectionId.of(1));

		boolean isInternal = false;
		String clientId = null;
		MSEventUid msEventUid = new MSEventUid("1");
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(msEventUid);
		
		EventExtId eventExtId = new EventExtId("1564");
		Event eventFromICS = new Event();
		eventFromICS.setUid(new EventObmId(150));
		eventFromICS.setInternalEvent(isInternal);
		eventFromICS.setExtId(eventExtId);
		eventFromICS.setSequence(12);
		
		ICalendar iCalendar = icalendar("simpleEvent.ics");
		expect(ical4jUserFactory.createIcal4jUser(user.getEmail(), token.getDomain()))
			.andReturn(null).once();
		expect(ical4jHelper.parseICSEvent(iCalendar.getICalendar(), null, token.getObmId()))
			.andReturn(ImmutableList.of(eventFromICS)).once();
		
		EventObmId eventCreationDbId = new EventObmId(9);
		expect(calendarClient.getEventFromExtId(token, calendar, eventExtId))
			.andThrow(new EventNotFoundException("Replying to an external invitation"));
		
		expect(eventConverter.getParticipation(AttendeeStatus.ACCEPT)).andReturn(Participation.accepted());
		expect(calendarClient.createEvent(token, calendar, eventFromICS, isInternal, clientId))
			.andReturn(eventCreationDbId).once();
		expect(calendarClient.getEventFromId(token, calendar, eventCreationDbId))
			.andReturn(eventFromICS);
		
		expect(calendarClient.changeParticipationState(token, calendar,
				eventExtId, Participation.accepted(), eventFromICS.getSequence(), true))
			.andReturn(true);
		
		mockControl.replay();
		ServerId serverIdResponse = calendarBackend.handleMeetingResponse(userDataRequest, iCalendar, AttendeeStatus.ACCEPT);
		mockControl.verify();
		
		assertThat(serverIdResponse).isEqualTo(CollectionId.of(1).serverId(150));
	}
	
	@Test
	public void testHandleMettingResponseExternalUpdate() throws Exception {
		String calendar = user.getLogin();
		String calendarPath = rootCalendarPath + calendar;
		expectBuildCollectionPath(calendar);
		expect(mappingService.getCollectionIdFor(device, calendarPath)).andReturn(CollectionId.of(1));

		boolean isInternal = false;
		MSEventUid msEventUid = new MSEventUid("1");
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(msEventUid);
		
		EventExtId eventExtId = new EventExtId("1564");
		Event eventFromDB = new Event();
		eventFromDB.setUid(new EventObmId(100));
		eventFromDB.setSequence(5);
		Event eventFromICS = new Event();
		eventFromICS.setInternalEvent(isInternal);
		eventFromICS.setExtId(eventExtId);
		Event eventWithMergedInfos = new Event();
		eventWithMergedInfos.setUid(eventFromDB.getUid());
		eventWithMergedInfos.setSequence(eventFromDB.getSequence());
		eventWithMergedInfos.setInternalEvent(eventFromICS.isInternalEvent());
		eventWithMergedInfos.setExtId(eventFromICS.getExtId());

		ICalendar iCalendar = icalendar("simpleEvent.ics");
		expect(ical4jUserFactory.createIcal4jUser(user.getEmail(), token.getDomain()))
			.andReturn(null).once();
		expect(ical4jHelper.parseICSEvent(iCalendar.getICalendar(), null, token.getObmId()))
			.andReturn(ImmutableList.of(eventFromICS)).once();
		
		expect(calendarClient.getEventFromExtId(token, calendar, eventExtId)).andReturn(eventFromDB);
		expect(eventConverter.getParticipation(AttendeeStatus.ACCEPT)).andReturn(Participation.accepted());
		expect(calendarClient.modifyEvent(token, calendar, eventFromICS, true, false))
			.andReturn(eventWithMergedInfos);
		
		expect(calendarClient.changeParticipationState(token, calendar, 
				eventExtId, Participation.accepted(), eventFromDB.getSequence(), true))
			.andReturn(true);
		
		mockControl.replay();
		ServerId serverIdResponse = calendarBackend.handleMeetingResponse(userDataRequest, iCalendar, AttendeeStatus.ACCEPT);
		mockControl.verify();
		
		assertThat(serverIdResponse).isEqualTo(CollectionId.of(1).serverId(100));
	}
	
	@Test
	public void testHandleMettingResponseOnInternalEvent() throws Exception {
		String calendar = user.getLogin();
		String calendarPath = rootCalendarPath + calendar;
		expectBuildCollectionPath(calendar);
		expect(mappingService.getCollectionIdFor(device, calendarPath)).andReturn(CollectionId.of(1));
		
		EventExtId eventExtId = new EventExtId("145");
		Event iCSEvent = new Event();
		iCSEvent.setInternalEvent(true);
		iCSEvent.setExtId(eventExtId);

		ICalendar iCalendar = icalendar("simpleEvent.ics");
		
		expect(ical4jUserFactory.createIcal4jUser(user.getEmail(), token.getDomain()))
			.andReturn(null).once();
		expect(ical4jHelper.parseICSEvent(iCalendar.getICalendar(), null, token.getObmId()))
			.andReturn(ImmutableList.of(iCSEvent)).once();
		
		Event oBMEvent = new Event();
		oBMEvent.setUid(new EventObmId(180));
		oBMEvent.setInternalEvent(true);
		oBMEvent.setExtId(eventExtId);
		
		expect(calendarClient.getEventFromExtId(token, calendar, eventExtId))
			.andReturn(oBMEvent).once();
		
		expect(eventConverter.getParticipation(AttendeeStatus.ACCEPT)).andReturn(Participation.accepted());
		expect(calendarClient.changeParticipationState(token, calendar,
				eventExtId, Participation.accepted(), 0, true))
			.andReturn(true);
		
		mockControl.replay();

		ServerId serverIdResponse = calendarBackend.handleMeetingResponse(userDataRequest, iCalendar, AttendeeStatus.ACCEPT);
		
		mockControl.verify();
		assertThat(serverIdResponse).isEqualTo(CollectionId.of(1).serverId(180));
	}
	
	private void expectGetAndModifyEvent(EventExtId eventExtId, Event event) 
			throws ServerFault, EventNotFoundException, NotAllowedException {
		
		expect(calendarClient.getEventFromExtId(token, user.getLogin(), eventExtId))
			.andReturn(event).once();
		
		expect(calendarClient.modifyEvent(token, user.getLogin(), event, true, false))
			.andReturn(event).once();
	}
	
	private void expectEventConvertion(Event event) throws ConversionException {
		expect(eventConverter.convert(eq(user), eq(event), anyObject(MSEvent.class), eq(false)))
			.andReturn(event).once();
	}
	
	@Test
	public void testFetch() throws Exception {
		SyncCollectionOptions options = null;
		ItemSyncState state = null;
		SyncKey newSyncKey = new SyncKey("132");
		CollectionId collectionId = CollectionId.of(1);
		Integer itemId1 = 1;
		Integer itemId2 = 2;
		ServerId serverId1 = collectionId.serverId(itemId1);
		ServerId serverId2 = collectionId.serverId(itemId2);
		
		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);

		Event event1 = expectGetEventFromId(itemId1);
		Event event2 = expectGetEventFromId(itemId2);

		expectConvertEventToMSEvent(serverId1, event1);
		expectConvertEventToMSEvent(serverId2, event2);
		
		mockControl.replay();
		
		List<ServerId> itemIds = ImmutableList.of(serverId1, serverId2);

		List<ItemChange> itemChanges = calendarBackend.fetch(userDataRequest, collectionId, itemIds, options, state, newSyncKey);
		
		mockControl.verify();
		
		assertThat(itemChanges).hasSize(2);
	}

	private Event expectGetEventFromId(Integer itemId) 
			throws ServerFault, EventNotFoundException, NotAllowedException {
		
		EventObmId eventObmId = new EventObmId(itemId);
		Event event = new Event();
		event.setUid(eventObmId);
		expect(calendarClient.getEventFromId(token, "test", eventObmId))
			.andReturn(event).once();
		
		return event;
	}

	private void expectConvertEventToMSEvent(ServerId serverId1, Event event) 
			throws DaoException, ConversionException {
		
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(new MSEventUid(String.valueOf(serverId1.getItemId())));
		expect(eventService.convertEventToMSEvent(userDataRequest, event))
			.andReturn(msEvent).once();
	}
	
	private static class CollectionPathTest extends CollectionPath {

		public CollectionPathTest(String collectionPath, PIMDataType pimType, String displayName) {
			super(collectionPath, pimType, displayName);
		}
		
	}
	
	@Test
	public void appendChangesToBuilderMustNotTransformDeclinedEventIntoRemoved() throws ServerFault, DaoException, ConversionException {
		EventObmId eventObmId = new EventObmId(132453);
		EventExtId eventExtId = new EventExtId("event-ext-id-bla-bla");
		Attendee attendee = UserAttendee.builder().email(user.getLoginAtDomain()).participation(Participation.declined()).build();
		Event event = new Event();
		
		event.setExtId(eventExtId);
		event.setUid(eventObmId);
		event.addAttendee(attendee);
		
		DeletedEvent deletedEvent = createDeletedEvent(eventObmId, eventExtId);
		EventChanges eventChanges = EventChanges.builder()
			.deletes(Arrays.asList(deletedEvent))
			.updates(Arrays.asList(event))
			.lastSync(org.obm.DateUtils.date("2012-10-10"))
			.build();
		
		expect(calendarClient.getUserEmail(token)).andReturn("test@test").anyTimes();
		
		
		mockControl.replay();
		
		WindowingEventChanges.Builder builder = WindowingEventChanges.builder();
		calendarBackend.appendChangesToBuilder(userDataRequest, token, eventChanges, builder);
		
		mockControl.verify();
		
		assertThat(builder.build().deletions()).hasSize(1).containsOnly(
				WindowingEvent.builder()
				.uid(132453)
				.build());
	}

	private FolderSyncState buildFolderSyncState(FolderSyncKey syncKey) {
		return FolderSyncState.builder()
				.syncKey(syncKey)
				.build();
	}
	
	@Test 
	public void testGetAllChangesThrowsHierarchyChangedException() throws Exception {
		Date currentDate = DateUtils.getCurrentDate();
		SyncKey syncKey = new SyncKey("1234567890a");
		ItemSyncState lastKnownState = ItemSyncState.builder()
				.syncDate(currentDate)
				.syncKey(syncKey)
				.build();

		CollectionId collectionId = CollectionId.of(1);

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		expect(calendarClient.getSync(token, "test", currentDate))
			.andThrow(new NotAllowedException("Not Allowed")).once();
		
		mockControl.replay();
		
		BodyPreference.Builder bodyPreferenceBuilder = BodyPreference.builder();
		BodyPreference bodyPreference = bodyPreferenceBuilder.build();
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder()
				.filterType(FilterType.ALL_ITEMS)
				.bodyPreferences(ImmutableList.of(bodyPreference))
				.build();
		
		WindowingChangesDelta<WindowingEvent> allChanges = calendarBackend.getAllChanges(userDataRequest, lastKnownState, collectionId, syncCollectionOptions);
		
		mockControl.verify();
		
		assertThat(allChanges.getDeltaDate()).isEqualTo(currentDate);
		assertThat(allChanges.getWindowingChanges()).isEqualTo(WindowingEventChanges.builder()
				.changes(ImmutableList.<WindowingEvent> of())
				.deletions(ImmutableList.<WindowingEvent> of())
				.build());
	}
	
	@Test (expected=ItemNotFoundException.class)
	public void testCreateOrUpdateThrowsHierarchyChangedException() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		String itemId = "2";
		ServerId serverId = collectionId.serverId(2);
		String clientId = "3";
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(new MSEventUid("abc0123"));
		String eventExtIdString = "00000123-0456-0789-0012-000000000345";

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		expect(eventService.getEventExtIdFor(msEvent.getUid(), device)).andReturn(eventExtIdString);
		
		expect(mappingService.getServerIdFor(collectionId, itemId)).andReturn(serverId).once();
		
		expect(calendarClient.getEventFromId(token, user.getLogin(), new EventObmId(itemId)))
			.andThrow(new NotAllowedException("Not allowed")).once();

		mockControl.replay();
		
		calendarBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, msEvent);
	}
	
	@Test (expected=ItemNotFoundException.class)
	public void testCreateOrUpdateThrowsHierarchyChangedExceptionOnUpdateCalendarEntity() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int itemId = 2;
		ServerId serverId = collectionId.serverId(itemId);
		String clientId = "3";
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(new MSEventUid("abc0123"));
		String eventExtIdString = "00000123-0456-0789-0012-000000000345";
		EventExtId eventExtId = new EventExtId(eventExtIdString);

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		expect(eventService.getEventExtIdFor(msEvent.getUid(), device)).andReturn(eventExtIdString);
		expect(mappingService.getServerIdFor(collectionId, String.valueOf(itemId))).andReturn(serverId).once();
		
		Event event = new Event();
		event.setUid(new EventObmId(itemId));
		event.setOwner("test");
		event.setOwnerEmail("test@test");
		expect(calendarClient.getEventFromId(token, user.getLogin(), new EventObmId(itemId)))
			.andReturn(event).once();

		expect(eventConverter.isInternalEvent(event, eventExtId)).andReturn(false);
		
		expect(calendarClient.modifyEvent(token, "test", event, true, true))
			.andThrow(new NotAllowedException("Not allowed")).once();

		expectEventConvertion(event);
		
		mockControl.replay();
		
		calendarBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, msEvent);
	}
	
	@Test (expected=ItemNotFoundException.class)
	public void testCreateOrUpdateThrowsHierarchyChangedExceptionOnCreateCalendarEntity() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		String clientId = "3";
		String clientIdHash = "35466464106456405";
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(new MSEventUid("abc0123"));
		String eventExtIdString = "00000123-0456-0789-0012-000000000345";
		EventExtId eventExtId = new EventExtId(eventExtIdString);
		Event event = new Event();

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		expect(clientIdService.hash(userDataRequest, clientId)).andReturn(clientIdHash);
		
		expect(eventService.getEventExtIdFor(msEvent.getUid(), device)).andReturn(eventExtIdString).once();
		expect(calendarClient.getEventFromExtId(token, user.getLogin(), eventExtId))
			.andReturn(null).once();
	
		expect(calendarClient.createEvent(eq(token), eq("test"), eq(event), eq(true), anyObject(String.class)))
			.andThrow(new NotAllowedException("Not allowed")).once();

		expect(eventConverter.isInternalEvent(null, eventExtId)).andReturn(false).once();
		
		expect(eventConverter.convert(user, null, msEvent, false))
			.andReturn(event).once();
		
		eventService.trackEventExtIdMSEventUidTranslation(eventExtIdString, msEvent.getUid(), device);
		expectLastCall().once();
		
		mockControl.replay();
		
		calendarBackend.createOrUpdate(userDataRequest, collectionId, null, clientId, msEvent);
	}
	
	@Test (expected=ItemNotFoundException.class)
	public void testCreateOrUpdateThrowsHierarchyChangedExceptionOnEventAlreadyExist() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		String clientId = "3";
		String clientIdHash = "35466464106456405";
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(new MSEventUid("abc0123"));
		String eventExtIdString = "00000123-0456-0789-0012-000000000345";
		EventExtId eventExtId = new EventExtId(eventExtIdString);
		Event event = new Event();
		
		expect(eventService.getEventExtIdFor(msEvent.getUid(), device)).andReturn(eventExtIdString).once();
		expect(clientIdService.hash(userDataRequest, clientId)).andReturn(clientIdHash);

		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		expect(eventService.getEventExtIdFor(msEvent.getUid(), device))
			.andReturn(eventExtIdString).once();

		expect(calendarClient.getEventFromExtId(token, user.getLogin(), eventExtId))
			.andReturn(null).once();
	
		expect(calendarClient.createEvent(eq(token), eq("test"), eq(event), eq(true), anyObject(String.class)))
			.andThrow(new EventAlreadyExistException("Already exist")).once();

		expect(calendarClient.getEventObmIdFromExtId(token, "test", eventExtId))
			.andThrow(new NotAllowedException("Not allowed")).once();
		
		expect(eventConverter.isInternalEvent(null, eventExtId))
			.andReturn(false).once();
		
		expect(eventConverter.convert(user, null, msEvent, false))
			.andReturn(event).once();
		
		eventService.trackEventExtIdMSEventUidTranslation(eventExtIdString, msEvent.getUid(), device);
		expectLastCall().once();
		
		mockControl.replay();
		
		calendarBackend.createOrUpdate(userDataRequest, collectionId, null, clientId, msEvent);
	}
	
	@Test (expected=ItemNotFoundException.class)
	public void testHandleMettingResponseThrowsHierarchyChangedException() throws Exception {
		String calendar = user.getLogin();
		String defaultCalendarName = rootCalendarPath + calendar;
		
		String eventExtIdString = "1";
		MSEventUid msEventUid = new MSEventUid(eventExtIdString);
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(msEventUid);

		EventExtId eventExtId = new EventExtId(eventExtIdString);
		expect(eventService.getEventExtIdFor(msEventUid, device))
			.andReturn(eventExtIdString).once();

		Event event = new Event();
		event.setUid(new EventObmId(1));
		event.setExtId(eventExtId);

		ICalendar iCalendar = icalendar("simpleEvent.ics");
		
		expect(ical4jUserFactory.createIcal4jUser(user.getEmail(), token.getDomain()))
			.andReturn(null).once();
		expect(ical4jHelper.parseICSEvent(iCalendar.getICalendar(), null, token.getObmId()))
			.andReturn(ImmutableList.of(event)).once();
		
		expect(calendarClient.getEventFromExtId(token, calendar, eventExtId))
			.andReturn(event).once();

		expect(eventConverter.isInternalEvent(event, false)).andReturn(false).once();
		expectGetAndModifyEvent(eventExtId, event);
		expect(calendarClient.changeParticipationState(token, calendar, eventExtId, null, 0, true))
			.andThrow(new NotAllowedException("Not allowed")).once();
		
		expectEventConvertion(event);
		expect(eventConverter.getParticipation(AttendeeStatus.ACCEPT))
			.andReturn(null).once();
		
		ServerId serverId = CollectionId.of(1).serverId(123);
		expect(mappingService.getCollectionIdFor(device, defaultCalendarName))
			.andReturn(CollectionId.of(1)).once();
		expect(mappingService.getServerIdFor(CollectionId.of(1), eventExtIdString))
			.andReturn(serverId);
		
		expectBuildCollectionPath(user.getLogin());
		
		mockControl.replay();

		calendarBackend.handleMeetingResponse(userDataRequest, iCalendar, AttendeeStatus.ACCEPT);
	}
	
	@Test (expected=ICalendarConverterException.class)
	public void testHandleMettingResponseErrorWhenParsingICS() throws Exception {
		String calendar = user.getLogin();
		
		MSEventUid msEventUid = new MSEventUid("1");
		MSEvent msEvent = new MSEvent();
		msEvent.setUid(msEventUid);

		EventExtId eventExtId = new EventExtId("1");

		Event event = new Event();
		event.setUid(new EventObmId(1));
		event.setExtId(eventExtId);

		ICalendar iCalendar = icalendar("simpleEvent.ics");
		
		expect(ical4jUserFactory.createIcal4jUser(user.getEmail(), token.getDomain()))
			.andReturn(null).once();
		expect(ical4jHelper.parseICSEvent(iCalendar.getICalendar(), null, token.getObmId()))
			.andThrow(new ParserException(1));
		
		expectBuildCollectionPath(calendar);
		
		mockControl.replay();

		try {
			calendarBackend.handleMeetingResponse(userDataRequest, iCalendar, AttendeeStatus.ACCEPT);
		} catch (ICalendarConverterException e) {
			mockControl.verify();
			throw e;
		}
	}
	
	@Test 
	public void testFetchThrowsHierarchyChangedException() throws Exception {
		SyncCollectionOptions options = null;
		ItemSyncState state = null;
		SyncKey newSyncKey = new SyncKey("132");
		CollectionId collectionId = CollectionId.of(1);
		Integer itemId = 1;
		ServerId serverId = collectionId.serverId(itemId);
		
		expect(mappingService.getCollectionPathFor(collectionId)).andReturn(userCalendarCollectionPath.collectionPath());
		expect(collectionPathBuilder.userDataRequest(userDataRequest)).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.fullyQualifiedCollectionPath(userCalendarCollectionPath.collectionPath())).andReturn(collectionPathBuilder);
		expect(collectionPathBuilder.build()).andReturn(userCalendarCollectionPath);
		
		EventObmId eventObmId = new EventObmId(itemId);
		Event event = new Event();
		event.setUid(eventObmId);
		expect(calendarClient.getEventFromId(token, "test", eventObmId))
			.andThrow(new NotAllowedException("Not allowed")).once();
		
		mockControl.replay();
		
		List<ServerId> itemIds = ImmutableList.of(serverId);

		calendarBackend.fetch(userDataRequest, collectionId, itemIds, options, state, newSyncKey);
		mockControl.verify();
	}
	
	@Test
	public void testIsParticipationChangeUpdateWhenOldEventIsNull() {
		CalendarCollectionPath collectionPath = new CalendarCollectionPath(rootCalendarPath, "calendarName");
		Event oldEvent = null;
		assertThat(calendarBackend.isParticipationChangeUpdate(collectionPath, oldEvent)).isFalse();
	}

	@Test
	public void testIsParticipationChangeUpdateWhenOldEventNotBelongsToCalendar() {
		CalendarCollectionPath collectionPath = new CalendarCollectionPath(rootCalendarPath, "calendarName");
		Event oldEvent = new Event();
		oldEvent.setOwner("otherCalendarName");
		
		assertThat(calendarBackend.isParticipationChangeUpdate(collectionPath, oldEvent)).isTrue();
	}

	@Test
	public void testIsParticipationChangeUpdateWhenOldEventBelongsToCalendar() {
		CalendarCollectionPath collectionPath = new CalendarCollectionPath(rootCalendarPath, "calendarName");
		Event oldEvent = new Event();
		oldEvent.setOwner("calendarName");

		assertThat(calendarBackend.isParticipationChangeUpdate(collectionPath, oldEvent)).isFalse();
	}
	
	
	private ICalendar icalendar(String filename) throws IOException, ParserException {
		InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream("icsFiles/" + filename);
		if (in == null) {
			Assert.fail("Cannot load " + filename);
		}
		return ICalendar.builder().inputStream(in).build();	
	}
	
	@Test(expected=NullPointerException.class)
	public void testAppendOrganizerIfNoneOnNullList() {
		Iterable<Event> events = null;
		ICalendarEvent iCalendarEvent = mockControl.createMock(ICalendarEvent.class);
		expect(iCalendarEvent.organizer()).andReturn("organizer@domain");
		
		mockControl.replay();
		try {
			calendarBackend.appendOrganizerIfNone(events, iCalendarEvent);
		} catch (Exception e) {
			mockControl.verify();
			throw e;
		}
	}
	
	@Test
	public void testAppendOrganizerIfNoneOnEmptyList() {
		Iterable<Event> events = ImmutableList.of();
		ICalendarEvent iCalendarEvent = mockControl.createMock(ICalendarEvent.class);
		expect(iCalendarEvent.organizer()).andReturn("organizer@domain");
		
		mockControl.replay();
		Iterable<Event> changedEvents = calendarBackend.appendOrganizerIfNone(events, iCalendarEvent);
		mockControl.verify();
		
		assertThat(changedEvents).isEmpty();
	}
	
	@Test(expected=NullPointerException.class)
	public void testAppendOrganizerIfNoneOnNullICalendarEvent() {
		Iterable<Event> events = ImmutableList.of();
		ICalendarEvent iCalendarEvent = null;
		
		mockControl.replay();
		try {
			calendarBackend.appendOrganizerIfNone(events, iCalendarEvent);
		} catch (Exception e) {
			mockControl.verify();
			throw e;
		}
	}
	
	@Test
	public void testAppendOrganizerIfNoneOnOneEventWithoutAttendee() {
		Event event = new Event();
		Iterable<Event> events = ImmutableList.of(event);
		ICalendarEvent iCalendarEvent = mockControl.createMock(ICalendarEvent.class);
		expect(iCalendarEvent.organizer()).andReturn("organizer@domain");
		
		mockControl.replay();
		Iterable<Event> changedEvents = calendarBackend.appendOrganizerIfNone(events, iCalendarEvent);
		mockControl.verify();
		
		assertThat(changedEvents).hasSize(1);
		assertThat(Iterables.getOnlyElement(changedEvents).getAttendees())
			.containsOnly(ContactAttendee.builder().asOrganizer().email("organizer@domain").build());
	}
	
	@Test
	public void testAppendOrganizerIfNoneOnOneEventWithoutOrganizer() {
		Event event = new Event();
		event.addAttendee(ContactAttendee.builder().asAttendee().email("attendee@domain").build());
		Iterable<Event> events = ImmutableList.of(event);
		ICalendarEvent iCalendarEvent = mockControl.createMock(ICalendarEvent.class);
		expect(iCalendarEvent.organizer()).andReturn("organizer@domain");
		
		mockControl.replay();
		Iterable<Event> changedEvents = calendarBackend.appendOrganizerIfNone(events, iCalendarEvent);
		mockControl.verify();
		
		assertThat(changedEvents).hasSize(1);
		assertThat(Iterables.getOnlyElement(changedEvents).getAttendees())
			.containsOnly(
					ContactAttendee.builder().asAttendee().email("attendee@domain").build(),
					ContactAttendee.builder().asOrganizer().email("organizer@domain").build());
	}
	
	@Test
	public void testAppendOrganizerIfNoneOnOneEventWithOrganizer() {
		Event event = new Event();
		event.addAttendee(ContactAttendee.builder().asAttendee().email("attendee@domain").build());
		event.addAttendee(ContactAttendee.builder().asOrganizer().email("initialorganizer@domain").build());
		Iterable<Event> events = ImmutableList.of(event);
		ICalendarEvent iCalendarEvent = mockControl.createMock(ICalendarEvent.class);
		expect(iCalendarEvent.organizer()).andReturn("organizer@domain");
		
		mockControl.replay();
		Iterable<Event> changedEvents = calendarBackend.appendOrganizerIfNone(events, iCalendarEvent);
		mockControl.verify();
		
		assertThat(changedEvents).hasSize(1);
		assertThat(Iterables.getOnlyElement(changedEvents).getAttendees())
			.containsOnly(
					ContactAttendee.builder().asAttendee().email("attendee@domain").build(),
					ContactAttendee.builder().asOrganizer().email("initialorganizer@domain").build());
	}
	
	@Test
	public void testAppendOrganizerIfNoneOnTwoDifferentEvent() {
		Event event = new Event();
		event.setTitle("one");
		event.addAttendee(ContactAttendee.builder().asAttendee().email("attendee@domain").build());
		Event event2 = new Event();
		event2.setTitle("two");
		event2.addAttendee(ContactAttendee.builder().asOrganizer().email("initialorganizer@domain").build());
		
		Iterable<Event> events = ImmutableList.of(event, event2);
		ICalendarEvent iCalendarEvent = mockControl.createMock(ICalendarEvent.class);
		expect(iCalendarEvent.organizer()).andReturn("organizer@domain");
		
		mockControl.replay();
		Iterable<Event> changedEvents = calendarBackend.appendOrganizerIfNone(events, iCalendarEvent);
		mockControl.verify();
		
		assertThat(changedEvents).hasSize(2);
		assertThat(findEventByTitle(changedEvents, "one").getAttendees()).containsOnly(
				ContactAttendee.builder().asAttendee().email("attendee@domain").build(),
				ContactAttendee.builder().asOrganizer().email("organizer@domain").build());
		assertThat(findEventByTitle(changedEvents, "two").getAttendees()).containsOnly(
				ContactAttendee.builder().asOrganizer().email("initialorganizer@domain").build());
	}

	private Event findEventByTitle(Iterable<Event> changedEvents, final String title) {
		return Iterables.find(changedEvents, new Predicate<Event>() {
			@Override
			public boolean apply(Event input) {
				return input.getTitle().equals(title);
			}
		});
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBelongsToCalendarWhenNullCalendar() {
		Event event = new Event();
		calendarBackend.belongsToCalendar(event, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBelongsToCalendarWhenEmptyCalendar() {
		Event event = new Event();
		calendarBackend.belongsToCalendar(event, "");
	}

	@Test
	public void testBelongsToCalendarWhenOwnerIsNull() {
		Event event = new Event();
		event.setOwner(null);
		
		assertThat(calendarBackend.belongsToCalendar(event, "owner")).isFalse();
	}
	
	@Test
	public void testBelongsToCalendarWhenOwnerIsEmpty() {
		Event event = new Event();
		event.setOwner("");
		
		assertThat(calendarBackend.belongsToCalendar(event, "owner")).isFalse();
	}
	
	@Test
	public void testBelongsToCalendarWhenOwnerIsDifferent() {
		Event event = new Event();
		event.setOwner("owner");
		
		assertThat(calendarBackend.belongsToCalendar(event, "user@email.com")).isFalse();
	}

	@Test
	public void testBelongsToCalendarWhenCalendarEqualsOwner() {
		Event event = new Event();
		event.setOwner("owner");
		
		assertThat(calendarBackend.belongsToCalendar(event, "owner")).isTrue();
	}

	@Test
	public void testBelongsToCalendarWhenCalendarEqualsOwnerDifferentCase() {
		Event event = new Event();
		event.setOwner("OWNER");
		
		assertThat(calendarBackend.belongsToCalendar(event, "owner")).isTrue();
	}
	
	@Test
	public void testBelongsToCalendarWhenCalendarNotEqualsOwnerButCreator() {
		Event event = new Event();
		event.setOwner("owner");
		event.setCreatorEmail("creator@email.com");
		
		assertThat(calendarBackend.belongsToCalendar(event, "creator@email.com")).isFalse();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void currentFoldersShouldReturnOnlyDefaultCalendar() {
		mockControl.replay();
		BackendFolders<CalendarPath> currentFolders = calendarBackend.currentFolders(userDataRequest);
		mockControl.verify();
		
		assertThat(currentFolders).containsOnly(
			BackendFolder.<CalendarPath>builder()
				.backendId(CalendarPath.of("test"))
				.displayName("test calendar")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.parentId(Optional.<CalendarPath>absent())
				.build());
	}
}
