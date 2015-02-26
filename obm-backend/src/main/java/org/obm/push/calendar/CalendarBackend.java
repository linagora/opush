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

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.fortuna.ical4j.data.ParserException;

import org.obm.breakdownduration.bean.Watch;
import org.obm.icalendar.Ical4jHelper;
import org.obm.icalendar.Ical4jUser;
import org.obm.icalendar.Ical4jUser.Factory;
import org.obm.icalendar.ical4jwrapper.ICalendarEvent;
import org.obm.push.backend.WindowingEvent;
import org.obm.push.backend.WindowingEventChanges;
import org.obm.push.bean.AttendeeStatus;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.CalendarPath;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.ICalendarConverterException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.HierarchyChangedException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.impl.ObmSyncBackend;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.resource.OpushResourcesHolder;
import org.obm.push.service.ClientIdService;
import org.obm.push.service.DateService;
import org.obm.push.service.EventService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.WindowingDao;
import org.obm.sync.PermissionException;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.EventAlreadyExistException;
import org.obm.sync.auth.EventNotFoundException;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.calendar.Attendee;
import org.obm.sync.calendar.ContactAttendee;
import org.obm.sync.calendar.DeletedEvent;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventExtId;
import org.obm.sync.calendar.EventObmId;
import org.obm.sync.calendar.Participation;
import org.obm.sync.client.calendar.CalendarClient;
import org.obm.sync.items.EventChanges;
import org.obm.sync.services.ICalendar;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@Watch(BreakdownGroups.EVENT)
public class CalendarBackend extends ObmSyncBackend<WindowingEvent> implements org.obm.push.ICalendarBackend {

	private static final String DEFAULT_CALENDAR_DISPLAYNAME_SUFFIX = " calendar";
	
	private final EventConverter eventConverter;
	private final EventService eventService;
	private final CalendarClient.Factory calendarClientFactory;
	private final ConsistencyEventChangesLogger consistencyLogger;
	private final EventExtId.Factory eventExtIdFactory;
	private final ClientIdService clientIdService;
	private final Ical4jHelper ical4jHelper;
	private final Factory ical4jUserFactory;
	private final FolderSnapshotDao folderSnapshotDao;
	
	@Inject
	@VisibleForTesting CalendarBackend(MappingService mappingService, 
			CalendarClient.Factory calendarClientFactory, 
			EventConverter eventConverter, 
			EventService eventService,
			ConsistencyEventChangesLogger consistencyLogger,
			EventExtId.Factory eventExtIdFactory,
			WindowingDao windowingDao,
			ClientIdService clientIdService,
			Ical4jHelper ical4jHelper, 
			Ical4jUser.Factory ical4jUserFactory,
			DateService dateService,
			OpushResourcesHolder opushResourcesHolder,
			FolderSnapshotDao folderSnapshotDao) {
		
		super(mappingService, windowingDao, dateService, opushResourcesHolder);
		this.calendarClientFactory = calendarClientFactory;
		this.eventConverter = eventConverter;
		this.eventService = eventService;
		this.consistencyLogger = consistencyLogger;
		this.eventExtIdFactory = eventExtIdFactory;
		this.clientIdService = clientIdService;
		this.ical4jHelper = ical4jHelper;
		this.ical4jUserFactory = ical4jUserFactory;
		this.folderSnapshotDao = folderSnapshotDao;
	}
	
	@Override
	public PIMDataType getPIMDataType() {
		return PIMDataType.CALENDAR;
	}
	
	@Override
	public BackendFolders getBackendFolders(final UserDataRequest udr) {
		return new BackendFolders() {

			@Override
			public Iterator<BackendFolder> iterator() {
				return Iterators.singletonIterator(defaultCalendar(udr));
			}
		};
	}

	private BackendFolder defaultCalendar(UserDataRequest udr) {
		return BackendFolder.builder()
				.parentId(Optional.<BackendId>absent())
				.backendId(defaultCalendarPath(udr))
				.displayName(udr.getUser().getLogin() + DEFAULT_CALENDAR_DISPLAYNAME_SUFFIX)
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build();
	}

	private CalendarPath defaultCalendarPath(UserDataRequest udr) {
		return CalendarPath.of(udr.getUser().getLogin());
	}

	@Override
	public int getItemEstimateSize(UserDataRequest udr, ItemSyncState state, CollectionId collectionId, SyncCollectionOptions collectionOptions) 
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException, ConversionException, HierarchyChangedException {
		
		WindowingChangesDelta<WindowingEvent> allChanges = getAllChanges(udr, state, collectionId, collectionOptions);
		return allChanges.getWindowingChanges().sumOfChanges();
	}
	
	@Override
	protected WindowingEventChanges.Builder windowingChangesBuilder() {
		return WindowingEventChanges.builder();
	}

	@Override
	protected WindowingChangesDelta<WindowingEvent> getAllChanges(UserDataRequest udr, ItemSyncState state, CollectionId collectionId, SyncCollectionOptions collectionOptions) {
		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		CalendarPath calendar = folder.getTypedBackendId();
		AccessToken token = getAccessToken();
		
		try {
			EventChanges changes = null;
			Date filteredSyncDate = state.getFilteredSyncDate(collectionOptions.getFilterType());
			boolean syncFiltered = filteredSyncDate != state.getSyncDate();
			if (state.isInitial()) {
				changes = initialSync(calendar, token, filteredSyncDate, syncFiltered);
			} else {
				changes = sync(calendar, token, filteredSyncDate, syncFiltered);
			}
			
			consistencyLogger.log(logger, changes);
			logger.info("Event changes [ {} ]", changes.getUpdated().size());
			logger.info("Event changes LastSync [ {} ]", changes.getLastSync().toString());
			
			
			WindowingEventChanges.Builder builder = WindowingEventChanges.builder();
			appendChangesToBuilder(udr, token, changes, builder);
			
			return WindowingChangesDelta.<WindowingEvent>builder()
					.deltaDate(changes.getLastSync())
					.windowingChanges(builder.build())
					.build();
		} catch (org.obm.sync.NotAllowedException e) {
			logger.warn(e.getMessage(), e);
			return WindowingChangesDelta.<WindowingEvent>builder()
					.deltaDate(state.getSyncDate())
					.windowingChanges(WindowingEventChanges.empty())
					.build();
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		}
	}
	
	private EventChanges initialSync(CalendarPath calendar, AccessToken token, Date filteredSyncDate, boolean syncFiltered)
			throws ServerFault, org.obm.sync.NotAllowedException {
		
		if (syncFiltered) {
			return getCalendarClient().getFirstSyncEventDate(token, calendar.getPath(), filteredSyncDate);
		} 
		return getCalendarClient().getFirstSync(token, calendar.getPath(), filteredSyncDate);
	}

	private EventChanges sync(CalendarPath calendar, AccessToken token, Date filteredSyncDate, boolean syncFiltered) 
			throws ServerFault, org.obm.sync.NotAllowedException {
		
		if (syncFiltered) {
			return getCalendarClient().getSyncEventDate(token, calendar.getPath(), filteredSyncDate);
		} 
		return getCalendarClient().getSync(token, calendar.getPath(), filteredSyncDate);
	}
	
	@VisibleForTesting <B extends WindowingEventChanges.Builder> void appendChangesToBuilder(UserDataRequest udr, AccessToken token, EventChanges changes, B builder) 
			throws ServerFault, DaoException, ConversionException {
		
		String userEmail = getCalendarClient().getUserEmail(token);
		Preconditions.checkNotNull(userEmail, "User has no email address");

		appendUpdatesEventFilter(changes.getUpdated(), userEmail, udr, builder);
		appendDeletions(changes.getDeletedEvents(), builder);
	}

	private <B extends WindowingEventChanges.Builder> void appendUpdatesEventFilter(Set<Event> events, String userEmail, UserDataRequest udr, B builder) 
			throws DaoException, ConversionException {
		
		for (Event event : events) {
			if (checkIfEventCanBeAdded(event, userEmail) && event.getRecurrenceId() == null) {
				builder.change(WindowingEvent.builder()
						.uid(event.getObmId().getObmId())
						.applicationData(eventService.convertEventToMSEvent(udr, event))
						.build());
			}	
		}
	}
	
	private <B extends WindowingEventChanges.Builder> void appendDeletions(Iterable<DeletedEvent> eventsRemoved, B builder) {
		for (DeletedEvent eventRemove : eventsRemoved) {
			builder.deletion(WindowingEvent.builder()
					.uid(eventRemove.getId().getObmId())
					.build());
		}
	}

	private boolean checkIfEventCanBeAdded(Event event, String userEmail) {
		for (final Attendee attendee : event.getAttendees()) {
			if (userEmail.equals(attendee.getEmail()) && 
					Participation.declined().equals(attendee.getParticipation())) {
				return false;
			}
		}
		return true;
	}

	private ItemChange createItemChangeToAddFromEvent(final UserDataRequest udr, final Event event, ServerId serverId)
			throws DaoException, ConversionException {
		
		IApplicationData ev = eventService.convertEventToMSEvent(udr, event);
		return ItemChange.builder()
			.serverId(serverId)
			.data(ev)
			.build();
	}

	private ServerId getServerIdFor(CollectionId collectionId, EventObmId uid) {
		return collectionId.serverId(uid.getObmId());
	}

	@Override
	public ServerId createOrUpdate(UserDataRequest udr, CollectionId collectionId,
			ServerId serverId, String clientId, IApplicationData data)
			throws CollectionNotFoundException, ProcessingEmailException, 
			DaoException, UnexpectedObmSyncServerException, ItemNotFoundException, ConversionException, HierarchyChangedException {

		MSEvent msEvent = (MSEvent) data;

		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		CalendarPath calendar = folder.getTypedBackendId();
		AccessToken token = getAccessToken();
		
		logger.info("createOrUpdate( calendar = {}, serverId = {} )", folder.getBackendId(), serverId);
		
		try {
			EventExtId eventExtId = getEventExtId(udr, msEvent);
			Event oldEvent = fetchReferenceEvent(token, serverId, eventExtId, calendar);
			EventObmId eventId = getEventId(oldEvent);
			
			EventObmId newEventId = chooseBackendChange(udr, msEvent, calendar, token, eventExtId, oldEvent, eventId, clientId);
			
			return getServerIdFor(collectionId, newEventId);
		} catch (org.obm.sync.NotAllowedException | PermissionException e) {
			logger.warn(e.getMessage(), e);
			throw new ItemNotFoundException(e);
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		} catch (EventNotFoundException e) {
			throw new ItemNotFoundException(e);
		}
	}

	private EventExtId getEventExtId(UserDataRequest udr, MSEvent msEvent) {
		try {
			return new EventExtId(eventService.getEventExtIdFor(msEvent.getUid(), udr.getDevice()));
		} catch (org.obm.push.exception.EventNotFoundException e) {
			return null;
		}
	}

	private EventObmId chooseBackendChange(UserDataRequest udr, MSEvent msEvent,
			CalendarPath calendar, AccessToken token,
			EventExtId eventExtId, Event oldEvent, final EventObmId eventId, String clientId)
			throws org.obm.sync.NotAllowedException, ServerFault, PermissionException {
		
		if (isParticipationChangeUpdate(calendar, oldEvent)) {
			updateUserStatus(oldEvent, AttendeeStatus.ACCEPT, token, calendar);
			return eventId;
		} else if (isEventModification(eventId)){
			updateEvent(token, udr, calendar, oldEvent, eventExtId, msEvent);
			return eventId;
		} else {
			return createEvent(udr, token, calendar, oldEvent, msEvent, eventExtId, clientId);
		}
	}

	private boolean isEventModification(EventObmId eventId) {
		return eventId != null;
	}

	private EventObmId getEventId(Event oldEvent) {
		if (oldEvent != null) {
			return oldEvent.getObmId();
		}
		return null;
	}

	private Event fetchReferenceEvent(AccessToken token, ServerId serverId, EventExtId eventExtId, CalendarPath calendar)
					throws ServerFault, EventNotFoundException, org.obm.sync.NotAllowedException {
		if (serverId != null) {
			EventObmId id = convertServerIdToEventObmId(serverId);
			return getCalendarClient().getEventFromId(token, calendar.getPath(), id);	
		} else if (eventExtId != null && !Strings.isNullOrEmpty(eventExtId.getExtId())) {
			return getEventFromExtId(token, eventExtId, calendar);
		}
		return null;
	}

	@VisibleForTesting boolean isParticipationChangeUpdate(CalendarPath calendar, Event oldEvent) {
		return oldEvent != null && !belongsToCalendar(oldEvent, calendar.getPath());
	}

	@VisibleForTesting boolean belongsToCalendar(Event oldEvent, String calendarName) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(calendarName));
		return calendarName.equalsIgnoreCase(oldEvent.getOwner());
	}

	private void updateEvent(AccessToken token, UserDataRequest udr, 
			CalendarPath calendar, Event oldEvent, 
			EventExtId eventExtId, MSEvent msEvent) throws ServerFault, org.obm.sync.NotAllowedException, PermissionException {
		
		boolean isInternal = eventConverter.isInternalEvent(oldEvent, eventExtId);
		Event event = convertMSObjectToObmObject(udr, msEvent, oldEvent, isInternal);
		event.setUid(oldEvent.getObmId());
		setSequence(oldEvent, event);
		if (event.getExtId() == null || event.getExtId().getExtId() == null) {
			event.setExtId(oldEvent.getExtId());
		}
		getCalendarClient().modifyEvent(token, calendar.getPath(), event, true, true);
	}

	private void setSequence(Event oldEvent, Event event) {
		if (event.hasImportantChanges(oldEvent)) {
			event.setSequence(oldEvent.getSequence() + 1);
		} else {
			event.setSequence(oldEvent.getSequence());
		}
	}

	private EventObmId createEvent(UserDataRequest udr, AccessToken token,
			CalendarPath calendar, Event oldEvent, MSEvent msEvent, EventExtId eventExtId, String clientId)
			throws ServerFault, DaoException, org.obm.sync.NotAllowedException, PermissionException {
		
		boolean isInternal = eventConverter.isInternalEvent(oldEvent, eventExtId);
		Event event = convertMSObjectToObmObject(udr, msEvent, oldEvent, isInternal);
		assignExtId(udr, msEvent, eventExtId, event);
		try { 
			return getCalendarClient().createEvent(token, calendar.getPath(), event, true, clientIdService.hash(udr, clientId));
		} catch (EventAlreadyExistException e) {
			return getEventIdFromExtId(token, calendar, event);
		}
	}

	private void assignExtId(UserDataRequest udr, MSEvent msEvent, EventExtId eventExtId, Event event) {
		if (eventExtId == null || Strings.isNullOrEmpty(eventExtId.getExtId())) {
			EventExtId newEventExtId = eventExtIdFactory.generate();
			eventService.trackEventExtIdMSEventUidTranslation(newEventExtId.getExtId(), msEvent.getUid(), udr.getDevice());
			event.setExtId(newEventExtId);
		} else {
			event.setExtId(eventExtId);
		}
	}
	
	private EventObmId convertServerIdToEventObmId(ServerId serverId) {
		return new EventObmId(serverId.getItemId());
	}

	private Event convertMSObjectToObmObject(UserDataRequest udr,
			MSEvent data, Event oldEvent, boolean isInternal) throws ConversionException {
		return eventConverter.convert(udr.getUser(), oldEvent, data, isInternal);
	}
	
	private EventObmId getEventIdFromExtId(AccessToken token, CalendarPath calendar, Event event)
			throws UnexpectedObmSyncServerException, org.obm.sync.NotAllowedException {
		
		try {
			return getCalendarClient().getEventObmIdFromExtId(token, calendar.getPath(), event.getExtId());
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		} catch (EventNotFoundException e) {
			logger.info(e.getMessage());
		}
		return null;
	}

	@Override
	public void delete(UserDataRequest udr, CollectionId collectionId, ServerId serverId, Boolean moveToTrash) 
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException, ItemNotFoundException {

		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		CalendarPath calendar = folder.getTypedBackendId();
		if (serverId != null) {

			AccessToken token = getAccessToken();
			try {
				logger.info("Delete event serverId {} in calendar {}", serverId, folder.getBackendId());
				//FIXME: not transactional
				Event evr = getEventFromServerId(token, calendar, serverId);
				getCalendarClient().removeEventById(token, calendar.getPath(), evr.getObmId(), evr.getSequence(), true);
			} catch (ServerFault e) {
				throw new UnexpectedObmSyncServerException(e);
			} catch (EventNotFoundException e) {
				throw new ItemNotFoundException(e);
			} catch (org.obm.sync.NotAllowedException e) {
				logger.warn(e.getMessage(), e);
				throw new ItemNotFoundException(e);
			}
		}
	}

	@Override
	public ServerId handleMeetingResponse(UserDataRequest udr, Object iCalendar, AttendeeStatus status) 
			throws UnexpectedObmSyncServerException, CollectionNotFoundException, DaoException,
			ItemNotFoundException, ConversionException, HierarchyChangedException, ICalendarConverterException {
		
		CalendarPath calendar = defaultCalendarPath(udr);
		Folder folder = folderSnapshotDao.get(
			udr.getUser(), udr.getDevice(), getPIMDataType(), calendar);
		
		AccessToken at = getAccessToken();
		try {
			Event event = convertICalendarToEvent(udr, at, (org.obm.icalendar.ICalendar) iCalendar);
			logger.info("handleMeetingResponse = {}", event.getExtId());
			Event obmEvent = createOrModifyInvitationEvent(at, event, calendar);
			updateUserStatus(obmEvent, status, at, calendar);
			return getServerIdFor(folder.getCollectionId(), obmEvent.getObmId());
		} catch (org.obm.sync.NotAllowedException | PermissionException e) {
			logger.warn(e.getMessage(), e);
			throw new ItemNotFoundException(e);
		} catch (UnexpectedObmSyncServerException e) {
			throw e;
		} catch (EventNotFoundException e) {
			throw new ItemNotFoundException(e);
		}
	}

	private Event createOrModifyInvitationEvent(AccessToken at, Event event, CalendarPath calendar) 
		throws UnexpectedObmSyncServerException, EventNotFoundException, 
			ConversionException, DaoException, org.obm.sync.NotAllowedException, PermissionException {
		
		try {
			boolean internalEvent = event.isInternalEvent();
			if (internalEvent) {
				return getCalendarClient().getEventFromExtId(at, calendar.getPath(), event.getExtId());
			}
			
			Event previousEvent = getEventFromExtId(at, event.getExtId(), calendar);
			if (previousEvent == null) {
				try {
					logger.info("createOrModifyInvitationEvent : create new event {}", event.getObmId());
					EventObmId id = getCalendarClient().createEvent(at, calendar.getPath(), event, internalEvent, null);
					return getCalendarClient().getEventFromId(at, calendar.getPath(), id);
				} catch (EventAlreadyExistException e) {
					throw new UnexpectedObmSyncServerException("it's not possible because getEventFromExtId == null");
				}
				
			} else {
				event.setUid(previousEvent.getObmId());
				event.setSequence(previousEvent.getSequence());
				if (!previousEvent.isInternalEvent()) {
					logger.info("createOrModifyInvitationEvent : update event {}", event.getObmId());
					previousEvent = getCalendarClient().modifyEvent(at, calendar.getPath(), event, true, false);
				}
				return previousEvent;
			}	
			
		} catch (ServerFault fault) {
			throw new UnexpectedObmSyncServerException(fault);
		}		
	}

	private Event convertICalendarToEvent(UserDataRequest udr, AccessToken accessToken, org.obm.icalendar.ICalendar iCalendar) throws ICalendarConverterException {
		if (iCalendar == null) {
			return null;
		}
		try {
			Iterable<Event> obmEvents = convertICalendarToEvents(udr, accessToken, iCalendar);
			if (!Iterables.isEmpty(obmEvents)) {
				return Iterables.getFirst(obmEvents, null);
			}
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			throw new ICalendarConverterException("ICS can't be converted to Event", e);
		} catch (ParserException e) {
			logger.warn(e.getMessage(), e);
			throw new ICalendarConverterException("ICS can't be converted to Event", e);
		}
		throw new ICalendarConverterException("ICS can't be converted to Event");
	}

	private Iterable<Event> convertICalendarToEvents(UserDataRequest udr, AccessToken accessToken, org.obm.icalendar.ICalendar iCalendar)
			throws IOException, ParserException {
		
		Ical4jUser ical4jUser = ical4jUserFactory.createIcal4jUser(udr.getUser().getEmail(), accessToken.getDomain());
		List<Event> parsedEvents = ical4jHelper.parseICSEvent(iCalendar.getICalendar(), ical4jUser, accessToken.getObmId());
		return appendOrganizerIfNone(parsedEvents, iCalendar.getICalendarEvent());
	}

	@VisibleForTesting Iterable<Event> appendOrganizerIfNone(Iterable<Event> events, ICalendarEvent iCalendarEvent) {
		String organizerEmail = iCalendarEvent.organizer();
		if (Strings.isNullOrEmpty(organizerEmail)) {
			return events;
		}

		final ContactAttendee organizerFallback = ContactAttendee.builder().asOrganizer().email(organizerEmail).build();
		return FluentIterable.from(events)
				.transform(new Function<Event, Event>() {
					@Override
					public Event apply(Event input) {
						return input.withOrganizerIfNone(organizerFallback);
					}
				});
	}

	private Event getEventFromExtId(AccessToken at, EventExtId eventExtId, CalendarPath calendar) 
		throws ServerFault, org.obm.sync.NotAllowedException {
		
		try {
			return getCalendarClient().getEventFromExtId(at, calendar.getPath(), eventExtId);
		} catch (EventNotFoundException e) {
			logger.info(e.getMessage());
		}
		return null;
	}
	
	private void updateUserStatus(Event event, AttendeeStatus status, AccessToken at, CalendarPath calendar)
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException, org.obm.sync.NotAllowedException {
		
		logger.info("update user status {} in calendar {}", status, calendar.getPath());
		Participation participationStatus = eventConverter.getParticipation(status);
		try {
			getCalendarClient().changeParticipationState(at, calendar.getPath(), event.getExtId(), 
					participationStatus, event.getSequence(), true);
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		}
	}

	@Override
	public List<ItemChange> fetch(UserDataRequest udr, CollectionId collectionId, List<ServerId> fetchServerIds, SyncCollectionOptions syncCollectionOptions,
				ItemSyncState previousItemSyncState, SyncKey newSyncKey)
			throws DaoException, UnexpectedObmSyncServerException, ConversionException, HierarchyChangedException {
	
		return fetch(udr, collectionId, fetchServerIds, syncCollectionOptions);
	}
	
	@Override
	public List<ItemChange> fetch(UserDataRequest udr, CollectionId collectionId, List<ServerId> fetchServerIds, SyncCollectionOptions syncCollectionOptions)
			throws DaoException, UnexpectedObmSyncServerException, ConversionException, HierarchyChangedException {
	
		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		CalendarPath calendar = folder.getTypedBackendId();
		
		List<ItemChange> ret = new LinkedList<ItemChange>();
		AccessToken token = getAccessToken();
		for (ServerId serverId : fetchServerIds) {
			try {
				Event event = getEventFromServerId(token, calendar, serverId);
				if (event != null) {
					ItemChange ic = createItemChangeToAddFromEvent(udr, event, serverId);
					ret.add(ic);
				}
			} catch (org.obm.sync.NotAllowedException e) {
				logger.warn(e.getMessage(), e);
			} catch (EventNotFoundException e) {
				logger.error("event from serverId {} not found.", serverId);
			} catch (ServerFault e1) {
				logger.error(e1.getMessage(), e1);
			}
		}
		return ret;
	}
	
	private Event getEventFromServerId(AccessToken token, CalendarPath calendar, ServerId serverId) throws ServerFault, EventNotFoundException, org.obm.sync.NotAllowedException {
		Integer itemId = serverId.getItemId();
		if (itemId == null) {
			return null;
		}
		return getCalendarClient().getEventFromId(token, calendar.getPath(), new EventObmId(itemId));
	}

	@Override
	public ServerId move(UserDataRequest udr, Folder srcFolder, Folder dstFolder,
			ServerId serverId) throws CollectionNotFoundException,
			ProcessingEmailException {
		return null;
	}

	@Override
	public void emptyFolderContent(UserDataRequest udr, Folder folder,
			boolean deleteSubFolder) throws NotAllowedException {
		throw new NotAllowedException(
				"emptyFolderContent is only supported for emails, collection was "
						+ folder.getBackendId());
	}

	private ICalendar getCalendarClient() {
		return calendarClientFactory.create(opushResourcesHolder.getHttpClient());
	}
	
	@Override
	public void initialize(DeviceId deviceId, CollectionId collectionId, FilterType filterType, SyncKey newSyncKey) {
		// nothing to do
	}
}
