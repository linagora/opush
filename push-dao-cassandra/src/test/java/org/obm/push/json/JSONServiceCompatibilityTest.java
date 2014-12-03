/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.push.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.ProtocolVersion;
import org.obm.push.backend.WindowingContact;
import org.obm.push.backend.WindowingEvent;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.CalendarBusyStatus;
import org.obm.push.bean.CalendarMeetingStatus;
import org.obm.push.bean.CalendarSensitivity;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.MSAddress;
import org.obm.push.bean.MSAttachement;
import org.obm.push.bean.MSContact;
import org.obm.push.bean.MSEmailBodyType;
import org.obm.push.bean.MSEmailHeader;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.MSEventException;
import org.obm.push.bean.MSEventExtId;
import org.obm.push.bean.MSEventUid;
import org.obm.push.bean.MSRecurrence;
import org.obm.push.bean.MSTask;
import org.obm.push.bean.MethodAttachment;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.RecurrenceDayOfWeek;
import org.obm.push.bean.RecurrenceType;
import org.obm.push.bean.SyncCollectionCommandResponse;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.bean.ms.MSEmailBody;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequest;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequestCategory;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequestInstanceType;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequestIntDBusyStatus;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequestRecurrence;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequestRecurrenceDayOfWeek;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequestRecurrenceType;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequestSensitivity;
import org.obm.push.mail.EmailChanges;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.utils.SerializableInputStream;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class JSONServiceCompatibilityTest {

	private JSONService jsonService;
	private Map<String, Object> serializedClasses;

	@Before
	public void setUp() {
		jsonService = new JSONService();
		
		MSContact contact = new MSContact();
		contact.setAssistantName("AssistantName");
		contact.setAssistantPhoneNumber("AssistantTelephoneNumber");
		contact.setAssistnamePhoneNumber("AssistnameTelephoneNumber");
		contact.setBusiness2PhoneNumber("Business2TelephoneNumber");
		contact.setBusinessPhoneNumber("BusinessTelephoneNumber");
		contact.setWebPage("Webpage");
		contact.setDepartment("Department");
		contact.setEmail1Address("Email1Address");
		contact.setEmail2Address("Email2Address");
		contact.setEmail3Address("Email3Address");
		contact.setBusinessFaxNumber("BusinessFaxNumber");
		contact.setFileAs("FileAs");
		contact.setFirstName("FirstName");
		contact.setMiddleName("MiddleName");
		contact.setHomeAddressCity("HomeAddressCity");
		contact.setHomeAddressCountry("HomeAddressCountry");
		contact.setHomeFaxNumber("HomeFaxNumber");
		contact.setHomePhoneNumber("HomeTelephoneNumber");
		contact.setHome2PhoneNumber("Home2TelephoneNumber");
		contact.setHomeAddressPostalCode("HomeAddressPostalCode");
		contact.setHomeAddressState("HomeAddressState");
		contact.setHomeAddressStreet("HomeAddressStreet");
		contact.setMobilePhoneNumber("MobileTelephoneNumber");
		contact.setSuffix("Suffix");
		contact.setCompanyName("CompanyName");
		contact.setOtherAddressCity("OtherAddressCity");
		contact.setOtherAddressCountry("OtherAddressCountry");
		contact.setCarPhoneNumber("CarTelephoneNumber");
		contact.setOtherAddressPostalCode("OtherAddressPostalCode");
		contact.setOtherAddressState("OtherAddressState");
		contact.setOtherAddressStreet("OtherAddressStreet");
		contact.setPagerNumber("PagerNumber");
		contact.setTitle("Title");
		contact.setBusinessPostalCode("BusinessAddressPostalCode");
		contact.setBusinessState("BusinessAddressState");
		contact.setBusinessStreet("BusinessAddressStreet");
		contact.setBusinessAddressCountry("BusinessAddressCountry");
		contact.setBusinessAddressCity("BusinessAddressCity");
		contact.setLastName("LastName");
		contact.setSpouse("Spouse");
		contact.setJobTitle("JobTitle");
		contact.setYomiFirstName("YomiFirstName");
		contact.setYomiLastName("YomiLastName");
		contact.setYomiCompanyName("YomiCompanyName");
		contact.setOfficeLocation("OfficeLocation");
		contact.setRadioPhoneNumber("RadioTelephoneNumber");
		contact.setPicture("Picture");
		contact.setAnniversary(new DateTime("2008-10-15T11:15:10Z").toDate());
		contact.setBirthday(new DateTime("2007-10-15T11:15:10Z").toDate());
		contact.setCategories(Lists.newArrayList("category"));
		contact.setChildren(Lists.newArrayList("children"));
		contact.setCustomerId("CustomerId");
		contact.setGovernmentId("GovernmentId");
		contact.setIMAddress("IMAddress");
		contact.setIMAddress2("IMAddress2");
		contact.setIMAddress3("IMAddress3");
		contact.setManagerName("ManagerName");
		contact.setCompanyMainPhone("CompanyMainPhone");
		contact.setAccountName("AccountName");
		contact.setNickName("NickName");
		contact.setMMS("MMS");
		contact.setData("Data");
		
		MSRecurrence msRecurrence = new MSRecurrence();
		msRecurrence.setDayOfMonth(2);
		msRecurrence.setDayOfWeek(ImmutableSet.of(RecurrenceDayOfWeek.FRIDAY, RecurrenceDayOfWeek.SUNDAY));
		msRecurrence.setDeadOccur(true);
		msRecurrence.setInterval(7);
		msRecurrence.setMonthOfYear(2);
		msRecurrence.setOccurrences(4);
		msRecurrence.setRegenerate(true);
		msRecurrence.setStart(date("2004-12-11T13:15:10"));
		msRecurrence.setType(RecurrenceType.DAILY);
		msRecurrence.setUntil(date("2004-12-11T12:15:10"));
		msRecurrence.setWeekOfMonth(4);
		
		MSTask msTask = new MSTask();
		msTask.setCategories(ImmutableList.of("category"));
		msTask.setComplete(true);
		msTask.setDateCompleted(date("2012-02-12T11:22:33"));
		msTask.setDescription("description");
		msTask.setDueDate(date("2012-02-02T11:27:33"));
		msTask.setImportance(2);
		msTask.setRecurrence(msRecurrence);
		msTask.setReminderSet(true);
		msTask.setReminderTime(date("2012-02-02T17:22:33"));
		msTask.setSensitivity(CalendarSensitivity.PRIVATE);
		msTask.setStartDate(date("2012-02-02T07:22:33"));
		msTask.setSubject("subject");
		msTask.setUtcDueDate(date("2012-02-03T11:24:33"));
		msTask.setUtcStartDate(date("2012-02-04T11:22:33"));
		
		Properties hints = new Properties();
		hints.put("prop", "propValue");
		Device device = new Device(1, "devType", new DeviceId("devId"), hints, ProtocolVersion.V121);

		User user = Factory.create().createUser("login@titi", "email", "displayName");
		Credentials credentials = new Credentials(user, "tata".toCharArray());
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder()
				.conflict(5)
				.deletesAsMoves(false)
				.filterType(FilterType.ONE_DAY_BACK)
				.mimeSupport(6)
				.mimeTruncation(400)
				.truncation(420)
				.bodyPreferences(ImmutableList.of(BodyPreference.builder()
						.allOrNone(true)
						.bodyType(MSEmailBodyType.MIME)
						.truncationSize(5).build()))
				.build();
		
		MSAttachement msAttachement = new MSAttachement();
		msAttachement.setDisplayName("displayName");
		msAttachement.setEstimatedDataSize(156);
		msAttachement.setFileReference("file reference");
		msAttachement.setMethod(MethodAttachment.EmbeddedMessage);
		
		
		AnalysedSyncCollection analysedSyncCollection = AnalysedSyncCollection.builder()
				.dataType(PIMDataType.EMAIL)
				.syncKey(new SyncKey("123"))
				.deletesAsMoves(true)
				.changes(true)
				.collectionId(CollectionId.of(1))
				.collectionPath("path")
				.windowSize(2)
				.options(syncCollectionOptions)
				.status(SyncStatus.OK)
				.build();
		
		MSEventException msEventException = new MSEventException();
		msEventException.setAllDayEvent(true);
		msEventException.setBusyStatus(CalendarBusyStatus.BUSY);
		msEventException.setCategories(ImmutableList.of("category"));
		msEventException.setDescription("description");
		msEventException.setDtStamp(date("2012-02-02T11:22:30"));
		msEventException.setEndTime(date("2012-02-02T11:20:30"));
		msEventException.setLocation("location");
		msEventException.setMeetingStatus(CalendarMeetingStatus.IS_A_MEETING);
		msEventException.setReminder(2);
		msEventException.setSensitivity(CalendarSensitivity.PERSONAL);
		msEventException.setStartTime(date("2012-02-01T11:20:30"));
		msEventException.setSubject("subject");
		
		MSEvent msEvent = new MSEvent();
		msEvent.setAllDayEvent(true);
		msEvent.setAttendeeEmails(ImmutableSet.of("attendee@obm.org"));
		msEvent.setBusyStatus(CalendarBusyStatus.BUSY);
		msEvent.setCategories(ImmutableList.of("category"));
		msEvent.setCreated(date("2012-02-02T11:22:33"));
		msEvent.setDescription("description");
		msEvent.setDtStamp(new DateTime("2012-02-02T10:22:30Z").toDate());
		msEvent.setEndTime(date("2012-02-02T11:20:30"));
		msEvent.setExceptions(ImmutableList.of(msEventException));
		msEvent.setLastUpdate(date("2012-03-02T11:20:30"));
		msEvent.setLocation("location");
		msEvent.setMeetingStatus(CalendarMeetingStatus.IS_A_MEETING);
		msEvent.setObmSequence(1);
		msEvent.setOrganizerEmail("organizer@obm.org");
		msEvent.setOrganizerName("organizer");
		msEvent.setRecurrence(msRecurrence);
		msEvent.setReminder(2);
		msEvent.setSensitivity(CalendarSensitivity.PERSONAL);
		msEvent.setStartTime(date("2012-02-01T11:20:30"));
		msEvent.setSubject("subject");
		msEvent.setTimeZone(TimeZone.getTimeZone("GMT"));
		msEvent.setUid(new MSEventUid("123"));
		
		ItemChange itemChange = ItemChange.builder()
			.serverId(CollectionId.of(1).serverId(33))
			.isNew(true)
			.data(msTask)
			.build();
		
		serializedClasses = ImmutableMap.<String, Object>builder()
			.put(filename(EmailChanges.class), EmailChanges.builder()
				.deletions(ImmutableSet.of(
						Email.builder()
						.date(date("2012-12-12T23:59:00"))
						.answered(true)
						.read(false)
						.uid(10l)
						.build()))
				.additions(ImmutableSet.of(
						Email.builder()
						.date(date("2012-12-12T23:59:00"))
						.answered(true)
						.read(false)
						.uid(15l)
						.build()))
				.changes(ImmutableSet.of(
						Email.builder()
							.date(date("2012-12-12T23:59:00"))
							.answered(true)
							.read(false)
							.uid(16l)
							.build()))
				.build())
			.put(filename(SyncCollectionOptions.class), syncCollectionOptions)
			.put(filename(SyncKey.class), new SyncKey("123"))
			.put("org.obm.push.bean.SyncCollectionCommand.json", SyncCollectionCommandResponse.builder()
						.applicationData(null)
						.status(SyncStatus.OBJECT_NOT_FOUND)
						.clientId("1")
						.type(SyncCommand.ADD)
						.serverId(CollectionId.of(1).serverId(2))
						.build())
			.put(filename(MSContact.class), contact)
			.put(filename(MSEmail.class), MSEmail.builder()
						.header(MSEmailHeader.builder()
								.from(new MSAddress("first", "first@obm.org"))
								.cc(new MSAddress("second", "second@obm.org"))
								.to(new MSAddress("third@obm.org"))
								.replyTo(new MSAddress("fourth@obm.org"))
								.subject("headersubject")
								.date(date("2008-10-15T11:15:10Z"))
								.build())
						.body(MSEmailBody.builder()
								.charset(Charsets.UTF_8)
								.bodyType(MSEmailBodyType.PlainText)
								.mimeData(new SerializableInputStream("content"))
								.build())
						.meetingRequest(MSMeetingRequest.builder()
								.allDayEvent(true)
								.categories(ImmutableList.of(new MSMeetingRequestCategory("category")))
								.dtStamp(date("2008-10-15T11:15:14Z"))
								.endTime(date("2008-10-15T11:15:18Z"))
								.instanceType(MSMeetingRequestInstanceType.SINGLE)
								.intDBusyStatus(MSMeetingRequestIntDBusyStatus.TENTATIVE)
								.location("location")
								.msEventExtId(new MSEventExtId("uid"))
								.msEventUid(new MSEventUid("euid"))
								.organizer("organizer@obm.org")
								.recurrenceId(date("2008-10-15T11:15:17Z"))
								.recurrences(ImmutableList.of(MSMeetingRequestRecurrence.builder()
										.dayOfMonth(3)
										.dayOfWeek(ImmutableList.of(MSMeetingRequestRecurrenceDayOfWeek.FRIDAY))
										.interval(6)
										.monthOfYear(5)
										.occurrences(2)
										.type(MSMeetingRequestRecurrenceType.MONTHLY)
										.until(date("2008-10-15T11:16:17Z"))
										.weekOfMonth(4)
										.build()))
								.reminder(12L)
								.responseRequested(true)
								.sensitivity(MSMeetingRequestSensitivity.CONFIDENTIAL)
								.startTime(date("2008-10-15T11:12:17Z"))
								.timeZone(TimeZone.getTimeZone("GMT"))
								.build())
						.read(true)
						.starred(true)
						.subject("subject")
						.build())
			.put(filename(MSTask.class), msTask)
			.put(filename(Device.class), device)
			.put(filename(Credentials.class), credentials)
			.put(filename(MSEventUid.class), new MSEventUid("uid"))
			.put(filename(DeviceId.class), new DeviceId("devId"))
			.put(filename(Email.class), Email.builder()
					.uid(1)
					.read(true)
					.date(date("2008-10-15T11:10:19Z"))
					.answered(true)
					.build())
			.put(filename(Snapshot.class), 
				Snapshot.builder()
					.filterType(FilterType.THREE_DAYS_BACK)
					.uidNext(2)
					.addEmail(Email.builder()
							.uid(1)
							.read(true)
							.date(date("2008-10-15T11:10:19Z"))
							.answered(true)
							.build())
					.build())
			.put(filename(AnalysedSyncCollection.class), analysedSyncCollection)
			.put(filename(MSEvent.class), msEvent)
			.put(filename(ItemDeletion.class), ItemDeletion.builder()
					.serverId(CollectionId.of(1).serverId(3))
					.build())
			.put(filename(ItemChange.class), itemChange)
			.put(filename(WindowingContact.class), WindowingContact.builder().uid(102).applicationData(contact).build())
			.put(filename(WindowingEvent.class), WindowingEvent.builder().uid(201).applicationData(msEvent).build())
			.build();
	}

	@Test
	public void deserialize() throws Exception {
		for (File jsonFile: findJsonFiles()) {
			assertThat(serializedClasses.get(jsonFile.getName()))
				.as("File " + jsonFile.getName())
				.isEqualTo(jsonService.deserialize(filenameToClass(jsonFile), FileUtils.readFileToString(jsonFile)));
		}
	}
	
	private String filename(Class<?> clazz) {
		return clazz.getCanonicalName() + ".json";
	}

	private Class<?> filenameToClass(File file) throws ClassNotFoundException {
		return Class.forName(file.getName().split(".json")[0]);
	}

	private File[] findJsonFiles() throws URISyntaxException {
		URL folderSource = ClassLoader.getSystemResource("compatibility");
		File[] jsonFiles = new File(folderSource.toURI()).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".json");
			}
		});
		return jsonFiles;
	}
	
	private Date date(String date) {
		return new DateTime(date, DateTimeZone.forID("Europe/Paris")).toDate();
	}
}
