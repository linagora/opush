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

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import org.obm.push.bean.FolderType;
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
import org.obm.push.bean.SyncCollectionCommandRequest;
import org.obm.push.bean.SyncCollectionCommandResponse;
import org.obm.push.bean.SyncCollectionCommandsResponse;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.hierarchy.Folder;
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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class JSONServiceTest {
	
	@Test
	public void testSerializeFolderWithParent() {
		Folder folder = Folder.builder()
			.backendId("the backendId")
			.parentBackendId(Optional.of("parent"))
			.displayName("the displayName")
			.collectionId(CollectionId.of(5))
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
			.build();
		
		assertThat(new JSONService().serialize(folder)).isEqualTo(
			"{" +
				"\"backendId\":\"the backendId\"," + 
				"\"collectionId\":5," + 
				"\"displayName\":\"the displayName\"," + 
				"\"folderType\":\"8\"," + 
				"\"parentBackendId\":\"parent\"" + 
			"}");
	}
	
	@Test
	public void testDeserializeFolderWithParent() {
		Folder folder = new JSONService().deserialize(Folder.class,
			"{" +
				"\"backendId\":\"the backendId\"," + 
				"\"collectionId\":5," + 
				"\"displayName\":\"the displayName\"," + 
				"\"folderType\":\"8\"," + 
				"\"parentBackendId\":\"parent\"" + 
			"}");
		
		assertThat(folder).isEqualTo(Folder.builder()
			.backendId("the backendId")
			.parentBackendId(Optional.of("parent"))
			.displayName("the displayName")
			.collectionId(CollectionId.of(5))
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
			.build());
	}

	@Test
	public void testSerializeEmailChanges() {
		EmailChanges emailChanges = EmailChanges.builder()
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
				.build();
		
		String serialized = new JSONService().serialize(emailChanges);
		assertThat(serialized).isEqualTo(
				"{\"additions\":" + 
						"[{" +
							"\"answered\":true," +
							"\"date\":1355353140000," +  
							"\"read\":false," + 
							"\"uid\":15" +
						"}]," + 
					"\"changes\":" + 
						"[{" +
							"\"answered\":true," + 
							"\"date\":1355353140000," + 
							"\"read\":false," + 
							"\"uid\":16" + 
						"}]," + 
					"\"deletions\":" + 
						"[{" +
							"\"answered\":true," + 
							"\"date\":1355353140000," + 
							"\"read\":false," + 
							"\"uid\":10" + 
						"}]" + 
				"}");
	}

	@Test
	public void testDeserializeEmailChanges() {
		EmailChanges emailChanges = new JSONService().deserialize(EmailChanges.class,
				"{\"deletions\":" + 
						"[{\"uid\":10," + 
							"\"read\":false," + 
							"\"date\":1355353140000," + 
							"\"answered\":true" + 
						"}]," + 
					"\"changes\":" + 
						"[{\"uid\":16," + 
							"\"read\":false," + 
							"\"date\":1355353140000," + 
							"\"answered\":true" + 
						"}]," + 
					"\"additions\":" + 
						"[{\"uid\":15," + 
							"\"read\":false," + 
							"\"date\":1355353140000," + 
							"\"answered\":true" + 
						"}]" + 
				"}");
		
		assertThat(emailChanges).isEqualTo(
			EmailChanges.builder()
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
				.build());
	}
	
	@Test
	public void testSerializeSyncCollectionOptions() {
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
		
		String serialized = new JSONService().serialize(syncCollectionOptions);
		assertThat(serialized).isEqualTo(
				"{" +
					"\"bodyPreferences\":[{" +
						"\"allOrNone\":true," + 
						"\"truncationSize\":5," + 
						"\"type\":\"MIME\"" + 
					"}]," + 
					"\"conflict\":5," + 
					"\"deletesAsMoves\":false," + 
					"\"filterType\":\"ONE_DAY_BACK\"," + 
					"\"mimeSupport\":6," + 
					"\"mimeTruncation\":400," + 
					"\"truncation\":420" + 
				"}");
	}
	
	@Test
	public void testDeserializeSyncCollectionOptions() {
		SyncCollectionOptions syncCollectionOptions = new JSONService().deserialize(SyncCollectionOptions.class, 
				"{" +
					"\"bodyPreferences\":" + 
						"[{" +
							"\"allOrNone\":true," + 
							"\"truncationSize\":5," + 
							"\"type\":\"MIME\"" + 
						"}]," + 
					"\"conflict\":5," + 
					"\"deletesAsMoves\":false," + 
					"\"filterType\":\"ONE_DAY_BACK\"," + 
					"\"mimeSupport\":6," + 
					"\"mimeTruncation\":400," + 
					"\"truncation\":420" + 
				"}");
		
		SyncCollectionOptions expectedSyncCollectionOptions = SyncCollectionOptions.builder()
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
		assertThat(syncCollectionOptions).isEqualTo(expectedSyncCollectionOptions);
	}
	
	@Test
	public void testSerializeSyncCollectionCommandsResponse() {
		SyncCollectionCommandsResponse request = SyncCollectionCommandsResponse.builder()
				.addCommand(SyncCollectionCommandResponse.builder()
						.applicationData(null)
						.clientId("1")
						.type(SyncCommand.ADD)
						.serverId(CollectionId.of(1).serverId(2))
						.build())
				.build();
		
		String serialized = new JSONService().serialize(request);
		assertThat(serialized).isEqualTo(
				"{}");
	}
	
	@Test
	public void testDeserializeSyncCollectionCommandsResponse() {
		SyncCollectionCommandsResponse request = new JSONService().deserialize(SyncCollectionCommandsResponse.class,
				"{}");
		
		assertThat(request).isEqualTo(
				SyncCollectionCommandsResponse.builder()
				.build());
	}
	@Test
	public void testSerializeMSContact() {
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
		contact.setAnniversary(date("2008-10-15T11:15:10Z"));
		contact.setBirthday(date("2007-10-15T11:15:10Z"));
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
		
		String serialized = new JSONService().serialize(contact);
		assertThat(serialized).isEqualTo(
				"{" + 
					"\"type\":\"CONTACTS\"," + 
					"\"accountName\":\"AccountName\"," + 
					"\"anniversary\":1224069310000," + 
					"\"assistantName\":\"AssistantName\"," + 
					"\"assistantPhoneNumber\":\"AssistantTelephoneNumber\"," + 
					"\"assistnamePhoneNumber\":\"AssistnameTelephoneNumber\"," + 
					"\"birthday\":1192446910000," + 
					"\"business2PhoneNumber\":\"Business2TelephoneNumber\"," + 
					"\"businessAddressCity\":\"BusinessAddressCity\"," + 
					"\"businessAddressCountry\":\"BusinessAddressCountry\"," + 
					"\"businessFaxNumber\":\"BusinessFaxNumber\"," + 
					"\"businessPhoneNumber\":\"BusinessTelephoneNumber\"," + 
					"\"businessPostalCode\":\"BusinessAddressPostalCode\"," + 
					"\"businessState\":\"BusinessAddressState\"," + 
					"\"businessStreet\":\"BusinessAddressStreet\"," + 
					"\"carPhoneNumber\":\"CarTelephoneNumber\"," + 
					"\"categories\":[\"category\"]," + 
					"\"children\":[\"children\"]," + 
					"\"companyMainPhone\":\"CompanyMainPhone\"," + 
					"\"companyName\":\"CompanyName\"," + 
					"\"customerId\":\"CustomerId\"," + 
					"\"data\":\"Data\"," + 
					"\"department\":\"Department\"," + 
					"\"email1Address\":\"Email1Address\"," + 
					"\"email2Address\":\"Email2Address\"," + 
					"\"email3Address\":\"Email3Address\"," + 
					"\"fileAs\":\"FileAs\"," + 
					"\"firstName\":\"FirstName\"," + 
					"\"governmentId\":\"GovernmentId\"," + 
					"\"home2PhoneNumber\":\"Home2TelephoneNumber\"," + 
					"\"homeAddressCity\":\"HomeAddressCity\"," + 
					"\"homeAddressCountry\":\"HomeAddressCountry\"," + 
					"\"homeAddressPostalCode\":\"HomeAddressPostalCode\"," + 
					"\"homeAddressState\":\"HomeAddressState\"," + 
					"\"homeAddressStreet\":\"HomeAddressStreet\"," + 
					"\"homeFaxNumber\":\"HomeFaxNumber\"," + 
					"\"homePhoneNumber\":\"HomeTelephoneNumber\"," + 
					"\"iMAddress\":\"IMAddress\"," + 
					"\"iMAddress2\":\"IMAddress2\"," + 
					"\"iMAddress3\":\"IMAddress3\"," + 
					"\"imaddress\":\"IMAddress\"," + 
					"\"imaddress2\":\"IMAddress2\"," + 
					"\"imaddress3\":\"IMAddress3\"," + 
					"\"jobTitle\":\"JobTitle\"," + 
					"\"lastName\":\"LastName\"," + 
					"\"mMS\":\"MMS\"," + 
					"\"managerName\":\"ManagerName\"," + 
					"\"middleName\":\"MiddleName\"," + 
					"\"mms\":\"MMS\"," + 
					"\"mobilePhoneNumber\":\"MobileTelephoneNumber\"," + 
					"\"nickName\":\"NickName\"," + 
					"\"officeLocation\":\"OfficeLocation\"," + 
					"\"otherAddressCity\":\"OtherAddressCity\"," + 
					"\"otherAddressCountry\":\"OtherAddressCountry\"," + 
					"\"otherAddressPostalCode\":\"OtherAddressPostalCode\"," + 
					"\"otherAddressState\":\"OtherAddressState\"," + 
					"\"otherAddressStreet\":\"OtherAddressStreet\"," + 
					"\"pagerNumber\":\"PagerNumber\"," + 
					"\"picture\":\"Picture\"," + 
					"\"radioPhoneNumber\":\"RadioTelephoneNumber\"," + 
					"\"spouse\":\"Spouse\"," + 
					"\"suffix\":\"Suffix\"," + 
					"\"title\":\"Title\"," + 
					"\"webPage\":\"Webpage\"," + 
					"\"yomiCompanyName\":\"YomiCompanyName\"," + 
					"\"yomiFirstName\":\"YomiFirstName\"," + 
					"\"yomiLastName\":\"YomiLastName\"" + 
				"}");
	}
	
	@Test
	public void testDeserializeMSContact() {
		MSContact contact = new JSONService().deserialize(MSContact.class,
				"{" + 
					"\"accountName\":\"AccountName\"," + 
					"\"anniversary\":1224069310000," + 
					"\"assistantName\":\"AssistantName\"," + 
					"\"assistantPhoneNumber\":\"AssistantTelephoneNumber\"," + 
					"\"assistnamePhoneNumber\":\"AssistnameTelephoneNumber\"," + 
					"\"birthday\":1192446910000," + 
					"\"business2PhoneNumber\":\"Business2TelephoneNumber\"," + 
					"\"businessAddressCity\":\"BusinessAddressCity\"," + 
					"\"businessAddressCountry\":\"BusinessAddressCountry\"," + 
					"\"businessFaxNumber\":\"BusinessFaxNumber\"," + 
					"\"businessPhoneNumber\":\"BusinessTelephoneNumber\"," + 
					"\"businessPostalCode\":\"BusinessAddressPostalCode\"," + 
					"\"businessState\":\"BusinessAddressState\"," + 
					"\"businessStreet\":\"BusinessAddressStreet\"," + 
					"\"carPhoneNumber\":\"CarTelephoneNumber\"," + 
					"\"categories\":[\"category\"]," + 
					"\"children\":[\"children\"]," + 
					"\"companyMainPhone\":\"CompanyMainPhone\"," + 
					"\"companyName\":\"CompanyName\"," + 
					"\"customerId\":\"CustomerId\"," + 
					"\"data\":\"Data\"," + 
					"\"department\":\"Department\"," + 
					"\"email1Address\":\"Email1Address\"," + 
					"\"email2Address\":\"Email2Address\"," + 
					"\"email3Address\":\"Email3Address\"," + 
					"\"fileAs\":\"FileAs\"," + 
					"\"firstName\":\"FirstName\"," + 
					"\"governmentId\":\"GovernmentId\"," + 
					"\"home2PhoneNumber\":\"Home2TelephoneNumber\"," + 
					"\"homeAddressCity\":\"HomeAddressCity\"," + 
					"\"homeAddressCountry\":\"HomeAddressCountry\"," + 
					"\"homeAddressPostalCode\":\"HomeAddressPostalCode\"," + 
					"\"homeAddressState\":\"HomeAddressState\"," + 
					"\"homeAddressStreet\":\"HomeAddressStreet\"," + 
					"\"homeFaxNumber\":\"HomeFaxNumber\"," + 
					"\"homePhoneNumber\":\"HomeTelephoneNumber\"," + 
					"\"iMAddress\":\"IMAddress\"," + 
					"\"iMAddress2\":\"IMAddress2\"," + 
					"\"iMAddress3\":\"IMAddress3\"," + 
					"\"imaddress\":\"IMAddress\"," + 
					"\"imaddress2\":\"IMAddress2\"," + 
					"\"imaddress3\":\"IMAddress3\"," + 
					"\"jobTitle\":\"JobTitle\"," + 
					"\"lastName\":\"LastName\"," + 
					"\"mMS\":\"MMS\"," + 
					"\"managerName\":\"ManagerName\"," + 
					"\"middleName\":\"MiddleName\"," + 
					"\"mms\":\"MMS\"," + 
					"\"mobilePhoneNumber\":\"MobileTelephoneNumber\"," + 
					"\"nickName\":\"NickName\"," + 
					"\"officeLocation\":\"OfficeLocation\"," + 
					"\"otherAddressCity\":\"OtherAddressCity\"," + 
					"\"otherAddressCountry\":\"OtherAddressCountry\"," + 
					"\"otherAddressPostalCode\":\"OtherAddressPostalCode\"," + 
					"\"otherAddressState\":\"OtherAddressState\"," + 
					"\"otherAddressStreet\":\"OtherAddressStreet\"," + 
					"\"pagerNumber\":\"PagerNumber\"," + 
					"\"picture\":\"Picture\"," + 
					"\"radioPhoneNumber\":\"RadioTelephoneNumber\"," + 
					"\"spouse\":\"Spouse\"," + 
					"\"suffix\":\"Suffix\"," + 
					"\"title\":\"Title\"," + 
					"\"type\":\"CONTACTS\"," + 
					"\"webPage\":\"Webpage\"," + 
					"\"yomiCompanyName\":\"YomiCompanyName\"," + 
					"\"yomiFirstName\":\"YomiFirstName\"," + 
					"\"yomiLastName\":\"YomiLastName\"" + 
				"}");
		
		MSContact expectedContact = new MSContact();
		expectedContact.setAssistantName("AssistantName");
		expectedContact.setAssistantPhoneNumber("AssistantTelephoneNumber");
		expectedContact.setAssistnamePhoneNumber("AssistnameTelephoneNumber");
		expectedContact.setBusiness2PhoneNumber("Business2TelephoneNumber");
		expectedContact.setBusinessPhoneNumber("BusinessTelephoneNumber");
		expectedContact.setWebPage("Webpage");
		expectedContact.setDepartment("Department");
		expectedContact.setEmail1Address("Email1Address");
		expectedContact.setEmail2Address("Email2Address");
		expectedContact.setEmail3Address("Email3Address");
		expectedContact.setBusinessFaxNumber("BusinessFaxNumber");
		expectedContact.setFileAs("FileAs");
		expectedContact.setFirstName("FirstName");
		expectedContact.setMiddleName("MiddleName");
		expectedContact.setHomeAddressCity("HomeAddressCity");
		expectedContact.setHomeAddressCountry("HomeAddressCountry");
		expectedContact.setHomeFaxNumber("HomeFaxNumber");
		expectedContact.setHomePhoneNumber("HomeTelephoneNumber");
		expectedContact.setHome2PhoneNumber("Home2TelephoneNumber");
		expectedContact.setHomeAddressPostalCode("HomeAddressPostalCode");
		expectedContact.setHomeAddressState("HomeAddressState");
		expectedContact.setHomeAddressStreet("HomeAddressStreet");
		expectedContact.setMobilePhoneNumber("MobileTelephoneNumber");
		expectedContact.setSuffix("Suffix");
		expectedContact.setCompanyName("CompanyName");
		expectedContact.setOtherAddressCity("OtherAddressCity");
		expectedContact.setOtherAddressCountry("OtherAddressCountry");
		expectedContact.setCarPhoneNumber("CarTelephoneNumber");
		expectedContact.setOtherAddressPostalCode("OtherAddressPostalCode");
		expectedContact.setOtherAddressState("OtherAddressState");
		expectedContact.setOtherAddressStreet("OtherAddressStreet");
		expectedContact.setPagerNumber("PagerNumber");
		expectedContact.setTitle("Title");
		expectedContact.setBusinessPostalCode("BusinessAddressPostalCode");
		expectedContact.setBusinessState("BusinessAddressState");
		expectedContact.setBusinessStreet("BusinessAddressStreet");
		expectedContact.setBusinessAddressCountry("BusinessAddressCountry");
		expectedContact.setBusinessAddressCity("BusinessAddressCity");
		expectedContact.setLastName("LastName");
		expectedContact.setSpouse("Spouse");
		expectedContact.setJobTitle("JobTitle");
		expectedContact.setYomiFirstName("YomiFirstName");
		expectedContact.setYomiLastName("YomiLastName");
		expectedContact.setYomiCompanyName("YomiCompanyName");
		expectedContact.setOfficeLocation("OfficeLocation");
		expectedContact.setRadioPhoneNumber("RadioTelephoneNumber");
		expectedContact.setPicture("Picture");
		expectedContact.setAnniversary(date("2008-10-15T11:15:10Z"));
		expectedContact.setBirthday(date("2007-10-15T11:15:10Z"));
		expectedContact.setCategories(Lists.newArrayList("category"));
		expectedContact.setChildren(Lists.newArrayList("children"));
		expectedContact.setCustomerId("CustomerId");
		expectedContact.setGovernmentId("GovernmentId");
		expectedContact.setIMAddress("IMAddress");
		expectedContact.setIMAddress2("IMAddress2");
		expectedContact.setIMAddress3("IMAddress3");
		expectedContact.setManagerName("ManagerName");
		expectedContact.setCompanyMainPhone("CompanyMainPhone");
		expectedContact.setAccountName("AccountName");
		expectedContact.setNickName("NickName");
		expectedContact.setMMS("MMS");
		expectedContact.setData("Data");
		assertThat(contact).isEqualTo(expectedContact);
	}
	
	@Test
	public void testSerializeWindowingContact() {
		MSContact contact = new MSContact();
		contact.setAssistantName("AssistantName");
		contact.setFirstName("FirstName");
		contact.setMiddleName("MiddleName");
		contact.setCompanyName("CompanyName");
		contact.setTitle("Title");
		
		String serialized = new JSONService().serialize(WindowingContact.builder().uid(56).applicationData(contact).build());
		assertThat(serialized).isEqualTo(
			"{" +
				"\"applicationData\":{" +
					"\"type\":\"CONTACTS\"," +
					"\"accountName\":null," +
					"\"anniversary\":null," +
					"\"assistantName\":\"AssistantName\"," +
					"\"assistantPhoneNumber\":null," +
					"\"assistnamePhoneNumber\":null," +
					"\"birthday\":null," +
					"\"business2PhoneNumber\":null," +
					"\"businessAddressCity\":null," +
					"\"businessAddressCountry\":null," +
					"\"businessFaxNumber\":null," +
					"\"businessPhoneNumber\":null," +
					"\"businessPostalCode\":null," +
					"\"businessState\":null," +
					"\"businessStreet\":null," +
					"\"carPhoneNumber\":null," +
					"\"categories\":null," +
					"\"children\":null," +
					"\"companyMainPhone\":null," +
					"\"companyName\":\"CompanyName\"," +
					"\"customerId\":null," +
					"\"data\":null," +
					"\"department\":null," +
					"\"email1Address\":null," +
					"\"email2Address\":null," +
					"\"email3Address\":null," +
					"\"fileAs\":null," +
					"\"firstName\":\"FirstName\"," +
					"\"governmentId\":null," +
					"\"home2PhoneNumber\":null," +
					"\"homeAddressCity\":null," +
					"\"homeAddressCountry\":null," +
					"\"homeAddressPostalCode\":null," +
					"\"homeAddressState\":null," +
					"\"homeAddressStreet\":null," +
					"\"homeFaxNumber\":null," +
					"\"homePhoneNumber\":null," +
					"\"iMAddress\":null," +
					"\"iMAddress2\":null," +
					"\"iMAddress3\":null," +
					"\"imaddress\":null," +
					"\"imaddress2\":null," +
					"\"imaddress3\":null," +
					"\"jobTitle\":null," +
					"\"lastName\":null," +
					"\"mMS\":null," +
					"\"managerName\":null," +
					"\"middleName\":\"MiddleName\"," +
					"\"mms\":null," +
					"\"mobilePhoneNumber\":null," +
					"\"nickName\":null," +
					"\"officeLocation\":null," +
					"\"otherAddressCity\":null," +
					"\"otherAddressCountry\":null," +
					"\"otherAddressPostalCode\":null," +
					"\"otherAddressState\":null," +
					"\"otherAddressStreet\":null," +
					"\"pagerNumber\":null," +
					"\"picture\":null," +
					"\"radioPhoneNumber\":null," +
					"\"spouse\":null," +
					"\"suffix\":null," +
					"\"title\":\"Title\"," +
					"\"webPage\":null," +
					"\"yomiCompanyName\":null," +
					"\"yomiFirstName\":null," +
					"\"yomiLastName\":null" +
				"}," +
				"\"uid\":56" +
			"}");
	}
	
	@Test
	public void testDeserializeWindowingContact() {
		WindowingContact contact = new JSONService().deserialize(WindowingContact.class,
			"{" +
				"\"applicationData\":{" +
					"\"type\":\"CONTACTS\"," +
					"\"accountName\":null," +
					"\"anniversary\":null," +
					"\"assistantName\":\"AssistantName\"," +
					"\"assistantPhoneNumber\":null," +
					"\"assistnamePhoneNumber\":null," +
					"\"birthday\":null," +
					"\"business2PhoneNumber\":null," +
					"\"businessAddressCity\":null," +
					"\"businessAddressCountry\":null," +
					"\"businessFaxNumber\":null," +
					"\"businessPhoneNumber\":null," +
					"\"businessPostalCode\":null," +
					"\"businessState\":null," +
					"\"businessStreet\":null," +
					"\"carPhoneNumber\":null," +
					"\"categories\":null," +
					"\"children\":null," +
					"\"companyMainPhone\":null," +
					"\"companyName\":\"CompanyName\"," +
					"\"customerId\":null," +
					"\"data\":null," +
					"\"department\":null," +
					"\"email1Address\":null," +
					"\"email2Address\":null," +
					"\"email3Address\":null," +
					"\"fileAs\":null," +
					"\"firstName\":\"FirstName\"," +
					"\"governmentId\":null," +
					"\"home2PhoneNumber\":null," +
					"\"homeAddressCity\":null," +
					"\"homeAddressCountry\":null," +
					"\"homeAddressPostalCode\":null," +
					"\"homeAddressState\":null," +
					"\"homeAddressStreet\":null," +
					"\"homeFaxNumber\":null," +
					"\"homePhoneNumber\":null," +
					"\"iMAddress\":null," +
					"\"iMAddress2\":null," +
					"\"iMAddress3\":null," +
					"\"imaddress\":null," +
					"\"imaddress2\":null," +
					"\"imaddress3\":null," +
					"\"jobTitle\":null," +
					"\"lastName\":null," +
					"\"mMS\":null," +
					"\"managerName\":null," +
					"\"middleName\":\"MiddleName\"," +
					"\"mms\":null," +
					"\"mobilePhoneNumber\":null," +
					"\"nickName\":null," +
					"\"officeLocation\":null," +
					"\"otherAddressCity\":null," +
					"\"otherAddressCountry\":null," +
					"\"otherAddressPostalCode\":null," +
					"\"otherAddressState\":null," +
					"\"otherAddressStreet\":null," +
					"\"pagerNumber\":null," +
					"\"picture\":null," +
					"\"radioPhoneNumber\":null," +
					"\"spouse\":null," +
					"\"suffix\":null," +
					"\"title\":\"Title\"," +
					"\"webPage\":null," +
					"\"yomiCompanyName\":null," +
					"\"yomiFirstName\":null," +
					"\"yomiLastName\":null" +
				"}," +
				"\"uid\":56" +
			"}");

		MSContact expectedContact = new MSContact();
		expectedContact.setAssistantName("AssistantName");
		expectedContact.setFirstName("FirstName");
		expectedContact.setMiddleName("MiddleName");
		expectedContact.setCompanyName("CompanyName");
		expectedContact.setTitle("Title");
		
		WindowingContact expectedWindowingContact = WindowingContact.builder().applicationData(expectedContact).uid(56).build();
		
		assertThat(contact).isEqualTo(expectedWindowingContact);
	}

	@Test(expected=RuntimeException.class)
	public void testDeserializeBadType() {
		new JSONService().deserialize(WindowingEvent.class,
			"{" +
				"\"applicationData\":{" +
					"\"type\":\"CONTACTS\"," +
					"\"accountName\":null," +
					"\"anniversary\":null," +
					"\"assistantName\":\"AssistantName\"," +
					"\"assistantPhoneNumber\":null," +
					"\"assistnamePhoneNumber\":null," +
					"\"birthday\":null," +
					"\"business2PhoneNumber\":null," +
					"\"businessAddressCity\":null," +
					"\"businessAddressCountry\":null," +
					"\"businessFaxNumber\":null," +
					"\"businessPhoneNumber\":null," +
					"\"businessPostalCode\":null," +
					"\"businessState\":null," +
					"\"businessStreet\":null," +
					"\"carPhoneNumber\":null," +
					"\"categories\":null," +
					"\"children\":null," +
					"\"companyMainPhone\":null," +
					"\"companyName\":\"CompanyName\"," +
					"\"customerId\":null," +
					"\"data\":null," +
					"\"department\":null," +
					"\"email1Address\":null," +
					"\"email2Address\":null," +
					"\"email3Address\":null," +
					"\"fileAs\":null," +
					"\"firstName\":\"FirstName\"," +
					"\"governmentId\":null," +
					"\"home2PhoneNumber\":null," +
					"\"homeAddressCity\":null," +
					"\"homeAddressCountry\":null," +
					"\"homeAddressPostalCode\":null," +
					"\"homeAddressState\":null," +
					"\"homeAddressStreet\":null," +
					"\"homeFaxNumber\":null," +
					"\"homePhoneNumber\":null," +
					"\"iMAddress\":null," +
					"\"iMAddress2\":null," +
					"\"iMAddress3\":null," +
					"\"imaddress\":null," +
					"\"imaddress2\":null," +
					"\"imaddress3\":null," +
					"\"jobTitle\":null," +
					"\"lastName\":null," +
					"\"mMS\":null," +
					"\"managerName\":null," +
					"\"middleName\":\"MiddleName\"," +
					"\"mms\":null," +
					"\"mobilePhoneNumber\":null," +
					"\"nickName\":null," +
					"\"officeLocation\":null," +
					"\"otherAddressCity\":null," +
					"\"otherAddressCountry\":null," +
					"\"otherAddressPostalCode\":null," +
					"\"otherAddressState\":null," +
					"\"otherAddressStreet\":null," +
					"\"pagerNumber\":null," +
					"\"picture\":null," +
					"\"radioPhoneNumber\":null," +
					"\"spouse\":null," +
					"\"suffix\":null," +
					"\"title\":\"Title\"," +
					"\"webPage\":null," +
					"\"yomiCompanyName\":null," +
					"\"yomiFirstName\":null," +
					"\"yomiLastName\":null" +
				"}," +
				"\"uid\":56" +
			"}");
	}
	
	@Test
	public void testSerializeMSEmail() {
		MSEmail msEmail = MSEmail.builder()
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
				.build();
		
		String serialized = new JSONService().serialize(msEmail);
		assertThat(serialized).isEqualTo(
			"{" +
				"\"type\":\"EMAIL\"," + 
				"\"answered\":false," + 
				"\"attachments\":[]," + 
				"\"body\":" + 
					"{\"bodyType\":\"PlainText\"," + 
						"\"charset\":\"UTF-8\"," + 
						"\"estimatedDataSize\":0," + 
						"\"mimeData\":\"content\"," + 
						"\"truncated\":false" + 
					"}," + 
				"\"header\":" + 
					"{\"cc\":" + 
						"[{\"displayName\":\"second\"," + 
							"\"mail\":\"second@obm.org\"" + 
						"}]," + 
						"\"date\":1224069310000," + 
						"\"from\":" + 
							"[{\"displayName\":\"first\"," + 
								"\"mail\":\"first@obm.org\"" + 
							"}]," + 
						"\"replyTo\":" + 
							"[{\"displayName\":null," + 
								"\"mail\":\"fourth@obm.org\"" + 
							"}]," + 
						"\"subject\":\"headersubject\"," + 
						"\"to\":" + 
							"[{\"displayName\":null," + 
								"\"mail\":\"third@obm.org\"" + 
							"}]" + 
					"}," + 
				"\"importance\":\"NORMAL\"," + 
				"\"meetingRequest\":" + 
					"{\"allDayEvent\":true," + 
						"\"categories\":[{\"category\":\"category\"}]," + 
						"\"dtStamp\":1224069314000," + 
						"\"endTime\":1224069318000," + 
						"\"instanceType\":\"SINGLE\"," + 
						"\"intDBusyStatus\":\"TENTATIVE\"," + 
						"\"location\":\"location\"," + 
						"\"msEventExtId\":{\"uid\":\"uid\"}," + 
						"\"msEventUid\":{\"uid\":\"euid\"}," + 
						"\"organizer\":\"organizer@obm.org\"," + 
						"\"recurrenceId\":1224069317000," + 
						"\"recurrences\":" + 
							"[{\"dayOfMonth\":3," + 
								"\"dayOfWeek\":[\"FRIDAY\"]," + 
								"\"interval\":6," + 
								"\"monthOfYear\":5," + 
								"\"occurrences\":2," + 
								"\"type\":\"MONTHLY\"," + 
								"\"until\":1224069377000," + 
								"\"weekOfMonth\":4" + 
							"}]," + 
						"\"reminder\":12," + 
						"\"responseRequested\":true," + 
						"\"sensitivity\":\"CONFIDENTIAL\"," + 
						"\"startTime\":1224069137000," + 
						"\"timeZone\":\"GMT\"" + 
					"}," + 
				"\"messageClass\":\"SCHEDULE_MEETING_REQUEST\"," + 
				"\"read\":true," + 
				"\"starred\":true," + 
				"\"subject\":\"subject\"" + 
			"}");
	}
	
	@Test
	public void testDeserializeMSEmail() {
		MSEmail msEmail = new JSONService().deserialize(MSEmail.class,
			"{" +
				"\"answered\":false," + 
				"\"attachments\":[]," + 
				"\"body\":{" + 
					"\"bodyType\":\"PlainText\"," + 
					"\"charset\":\"UTF-8\"," + 
					"\"estimatedDataSize\":0," + 
					"\"mimeData\":\"content\"," + 
					"\"truncated\":false" + 
				"}," + 
				"\"header\":{" + 
					"\"cc\":" + 
						"[{\"displayName\":\"second\"," + 
							"\"mail\":\"second@obm.org\"" + 
						"}]," + 
					"\"date\":1224069310000," + 
					"\"from\":" + 
						"[{\"displayName\":\"first\"," + 
							"\"mail\":\"first@obm.org\"" + 
						"}]," + 
					"\"replyTo\":" + 
						"[{\"displayName\":null," + 
							"\"mail\":\"fourth@obm.org\"" + 
						"}]," + 
					"\"subject\":\"headersubject\"," + 
					"\"to\":" + 
						"[{\"displayName\":null," + 
							"\"mail\":\"third@obm.org\"" + 
						"}]" + 
					"}," + 
				"\"importance\":\"NORMAL\"," + 
				"\"meetingRequest\":{" + 
					"\"allDayEvent\":true," + 
					"\"categories\":[{\"category\":\"category\"}]," + 
					"\"dtStamp\":1224069314000," + 
					"\"endTime\":1224069318000," + 
					"\"instanceType\":\"SINGLE\"," + 
					"\"intDBusyStatus\":\"TENTATIVE\"," + 
					"\"location\":\"location\"," + 
					"\"msEventExtId\":{\"uid\":\"uid\"}," + 
					"\"msEventUid\":{\"uid\":\"euid\"}," + 
					"\"organizer\":\"organizer@obm.org\"," + 
					"\"recurrenceId\":1224069317000," + 
					"\"recurrences\":" + 
						"[{\"dayOfMonth\":3," + 
							"\"dayOfWeek\":[\"FRIDAY\"]," + 
							"\"interval\":6," + 
							"\"monthOfYear\":5," + 
							"\"occurrences\":2," + 
							"\"type\":\"MONTHLY\"," + 
							"\"until\":1224069377000," + 
							"\"weekOfMonth\":4" + 
						"}]," + 
					"\"reminder\":12," + 
					"\"responseRequested\":true," + 
					"\"sensitivity\":\"CONFIDENTIAL\"," + 
					"\"startTime\":1224069137000," + 
					"\"timeZone\":\"GMT\"" + 
				"}," + 
				"\"messageClass\":\"SCHEDULE_MEETING_REQUEST\"," + 
				"\"type\":\"EMAIL\"," + 
				"\"read\":true," + 
				"\"starred\":true," + 
				"\"subject\":\"subject\"" + 
			"}");
		
		assertThat(msEmail).isEqualTo(
			MSEmail.builder()
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
				.build());
	}
	
	@Test
	public void testSerializeMSTask() {
		MSTask msTask = new MSTask();
		msTask.setCategories(ImmutableList.of("categories"));
		msTask.setComplete(true);
		msTask.setDateCompleted(date("2008-10-15T11:12:17Z"));
		msTask.setDescription("description");
		msTask.setDueDate(date("2008-10-15T11:12:19Z"));
		msTask.setImportance(5);
		msTask.setReminderSet(true);
		msTask.setReminderTime(date("2008-10-15T11:12:22Z"));
		msTask.setSensitivity(CalendarSensitivity.PERSONAL);
		msTask.setStartDate(date("2008-10-15T11:10:19Z"));
		msTask.setSubject("subject");
		msTask.setUtcDueDate(date("2008-10-15T11:02:19Z"));
		msTask.setUtcStartDate(date("2008-10-15T10:12:19Z"));
		
		String serialized = new JSONService().serialize(msTask);
		assertThat(serialized).isEqualTo("{" +
				"\"type\":\"TASKS\"," + 
				"\"UtcDueDate\":1224068539000," + 
				"\"categories\":[\"categories\"]," + 
				"\"complete\":true," + 
				"\"dateCompleted\":1224069137000," + 
				"\"description\":\"description\"," + 
				"\"dueDate\":1224069139000," + 
				"\"importance\":5," + 
				"\"recurrence\":null," + 
				"\"reminderSet\":true," + 
				"\"reminderTime\":1224069142000," + 
				"\"sensitivity\":\"PERSONAL\"," + 
				"\"startDate\":1224069019000," + 
				"\"subject\":\"subject\"," + 
				"\"utcDueDate\":1224068539000," + 
				"\"utcStartDate\":1224065539000" + 
			"}");
	}
	
	@Test
	public void testDeserializeMSTask() {
		MSTask msTask = new JSONService().deserialize(MSTask.class,
				"{\"UtcDueDate\":1224068539000," + 
					"\"categories\":[\"categories\"]," + 
					"\"complete\":true," + 
					"\"dateCompleted\":1224069137000," + 
					"\"description\":\"description\"," + 
					"\"dueDate\":1224069139000," + 
					"\"importance\":5," + 
					"\"recurrence\":null," + 
					"\"reminderSet\":true," + 
					"\"reminderTime\":1224069142000," + 
					"\"sensitivity\":\"PERSONAL\"," + 
					"\"startDate\":1224069019000," + 
					"\"subject\":\"subject\"," + 
					"\"type\":\"TASKS\"," + 
					"\"utcDueDate\":1224068539000," + 
					"\"utcStartDate\":1224065539000" + 
				"}");
		
		MSTask expectedMSTask = new MSTask();
		expectedMSTask.setCategories(ImmutableList.of("categories"));
		expectedMSTask.setComplete(true);
		expectedMSTask.setDateCompleted(date("2008-10-15T11:12:17Z"));
		expectedMSTask.setDescription("description");
		expectedMSTask.setDueDate(date("2008-10-15T11:12:19Z"));
		expectedMSTask.setImportance(5);
		expectedMSTask.setReminderSet(true);
		expectedMSTask.setReminderTime(date("2008-10-15T11:12:22Z"));
		expectedMSTask.setSensitivity(CalendarSensitivity.PERSONAL);
		expectedMSTask.setStartDate(date("2008-10-15T11:10:19Z"));
		expectedMSTask.setSubject("subject");
		expectedMSTask.setUtcDueDate(date("2008-10-15T11:02:19Z"));
		expectedMSTask.setUtcStartDate(date("2008-10-15T10:12:19Z"));
		
		assertThat(msTask).isEqualTo(expectedMSTask);
	}
	
	@Test
	public void testSerializeDevice() {
		Properties hints = new Properties();
		hints.put("prop", "propValue");
		Device device = new Device(1, "devType", new DeviceId("devId"), hints, ProtocolVersion.V121);
		
		String serialized = new JSONService().serialize(device);
		assertThat(serialized).isEqualTo(
				"{\"databaseId\":1," +
					"\"devId\":{\"deviceId\":\"devId\"}," + 
					"\"devType\":\"devType\"," + 
					"\"hints\":{\"prop\":\"propValue\"}," + 
					"\"protocolVersion\":\"V121\"" + 
				"}");
	}
	
	@Test
	public void testDeserializeDevice() {
		Device device = new JSONService().deserialize(Device.class,
				"{\"databaseId\":1," +
					"\"devId\":{\"deviceId\":\"devId\"}," + 
					"\"devType\":\"devType\"," + 
					"\"hints\":{\"prop\":\"propValue\"}," + 
					"\"protocolVersion\":\"V121\"" + 
				"}");
		
		Properties hints = new Properties();
		hints.put("prop", "propValue");
		Device expectedDevice = new Device(1, "devType", new DeviceId("devId"), hints, ProtocolVersion.V121);
		
		assertThat(device).isEqualTo(expectedDevice);
	}
	
	@Test
	public void testSerializeCredentials() {
		User user = Factory.create().createUser("login@titi", "email", "displayName");
		Credentials credentials = new Credentials(user, "tata".toCharArray());
		
		String serialized = new JSONService().serialize(credentials);
		assertThat(serialized).isEqualTo(
				"{\"password\":\"tata\"," + 
					"\"user\":" + 
						"{\"displayName\":\"displayName\"," + 
							"\"domain\":\"titi\"," + 
							"\"email\":\"email\"," + 
							"\"login\":\"login\"" + 
						"}" + 
				"}");
	}
	
	@Test
	public void testDeserializeCredentials() {
		Credentials credentials = new JSONService().deserialize(Credentials.class,
				"{\"password\":\"tata\"," + 
					"\"user\":" + 
						"{\"displayName\":\"displayName\"," + 
							"\"domain\":\"titi\"," + 
							"\"email\":\"email\"," + 
							"\"login\":\"login\"" + 
						"}" + 
				"}");
		
		User user = Factory.create().createUser("login@titi", "email", "displayName");
		Credentials expectedCredentials = new Credentials(user, "tata".toCharArray());
		assertThat(credentials).isEqualTo(expectedCredentials);
	}
	
	@Test
	public void testSerializeMSEventUid() {
		MSEventUid msEventUid = new MSEventUid("uid");
		
		String serialized = new JSONService().serialize(msEventUid);
		assertThat(serialized).isEqualTo("{\"uid\":\"uid\"}");
	}
	
	@Test
	public void testDeserializeMSEventUid() {
		MSEventUid msEventUid = new JSONService().deserialize(MSEventUid.class, "{\"uid\":\"uid\"}");
		
		MSEventUid expectedMSEventUid = new MSEventUid("uid");
		assertThat(msEventUid).isEqualTo(expectedMSEventUid);
	}
	
	@Test
	public void testSerializeDeviceId() {
		DeviceId deviceId = new DeviceId("devId");
		
		String serialized = new JSONService().serialize(deviceId);
		assertThat(serialized).isEqualTo("{\"deviceId\":\"devId\"}");
	}
	
	@Test
	public void testDeserializeDeviceId() {
		DeviceId deviceId = new JSONService().deserialize(DeviceId.class, "{\"deviceId\":\"devId\"}");
		
		DeviceId expectedDeviceId = new DeviceId("devId");
		assertThat(deviceId).isEqualTo(expectedDeviceId);
	}
	
	@Test
	public void testSerializeSyncKey() {
		SyncKey syncKey = new SyncKey("123");
		
		String serialized = new JSONService().serialize(syncKey);
		assertThat(serialized).isEqualTo("{\"syncKey\":\"123\"}");
	}
	
	@Test
	public void testDeserializeSyncKey() {
		SyncKey syncKey = new JSONService().deserialize(SyncKey.class, "{\"syncKey\":\"123\"}");
		
		assertThat(syncKey).isEqualTo(new SyncKey("123"));
	}
	
	@Test
	public void testSerializeEmail() {
		Email email = Email.builder()
				.uid(1)
				.read(true)
				.date(date("2008-10-15T11:10:19Z"))
				.answered(true)
				.build();
		
		String serialized = new JSONService().serialize(email);
		assertThat(serialized).isEqualTo("{\"answered\":true,\"date\":1224069019000,\"read\":true,\"uid\":1}");
	}
	
	@Test
	public void testDeserializeEmail() {
		Email email = new JSONService().deserialize(Email.class, "{\"answered\":true,\"date\":1224069019000,\"read\":true,\"uid\":1}");
		
		Email expectedEmail = Email.builder()
				.uid(1)
				.read(true)
				.date(date("2008-10-15T11:10:19Z"))
				.answered(true)
				.build();
		assertThat(email).isEqualTo(expectedEmail);
	}
	
	@Test
	public void testSerializeSnapshot() {
		Snapshot snapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(2)
				.addEmail(Email.builder()
						.uid(1)
						.read(true)
						.date(date("2008-10-15T11:10:19Z"))
						.answered(true)
						.build())
				.build();
		
		String serialized = new JSONService().serialize(snapshot);
		assertThat(serialized).isEqualTo(
				"{" + 
					"\"emails\":" + 
						"[" + 
							"{\"answered\":true," + 
								"\"date\":1224069019000," + 
								"\"read\":true," + 
								"\"uid\":1" + 
							"}" + 
						"]," + 
					"\"filterType\":\"THREE_DAYS_BACK\"," + 
					"\"uidNext\":2}");
	}
	
	@Test
	public void testDeserializeSnapshot() {
		Snapshot snapshot = new JSONService().deserialize(Snapshot.class, 
				"{" + 
					"\"emails\":" + 
						"[" + 
							"{\"answered\":true," + 
								"\"date\":1224069019000," + 
								"\"read\":true," + 
								"\"uid\":1" + 
							"}" + 
						"]," + 
					"\"filterType\":\"THREE_DAYS_BACK\"," + 
					"\"uidNext\":2}");
		
		Snapshot expectedSnapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(2)
				.addEmail(Email.builder()
						.uid(1)
						.read(true)
						.date(date("2008-10-15T11:10:19Z"))
						.answered(true)
						.build())
				.build();
		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}
	
	@Test
	public void testSerializeAnalysedSyncCollection() {
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
		
		MSEmail msEmail = MSEmail.builder()
				.answered(true)
				.attachements(ImmutableSet.of(msAttachement))
				.header(MSEmailHeader.builder()
						.subject("a subject")
						.from(new MSAddress("from@domain.org"))
						.to(new MSAddress("to@domain.org"))
						.cc(new MSAddress("cc@domain.org"))
						.replyTo(new MSAddress("replyto@domain.org"))
						.date(date("2008-02-03T20:37:05Z"))
						.build())
				.body(org.obm.push.bean.ms.MSEmailBody.builder()
						.mimeData(new SerializableInputStream(new ByteArrayInputStream("message".getBytes())))
						.bodyType(MSEmailBodyType.PlainText)
						.estimatedDataSize(0)
						.charset(Charsets.UTF_8)
						.truncated(false)
						.build())
				.meetingRequest(
						MSMeetingRequest.builder()
							.startTime(date("2012-02-03T11:22:33"))
							.endTime(date("2012-02-03T12:22:33"))
							.dtStamp(date("2012-02-02T11:22:33"))
							.instanceType(MSMeetingRequestInstanceType.MASTER_RECURRING)
							.msEventExtId(new MSEventExtId("ext-id-123-536"))
							.recurrences(Arrays.asList(
									MSMeetingRequestRecurrence.builder()
									.type(MSMeetingRequestRecurrenceType.DAILY)
									.interval(1)
									.build()))
							.recurrenceId(date("2012-02-02T11:22:33"))
							.categories(Arrays.asList(
									new MSMeetingRequestCategory("category")
									))
							.build())
				.build();
		
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
				.command(SyncCollectionCommandRequest.builder()
						.serverId(CollectionId.of(1).serverId(3))
						.clientId("clientId")
						.type(SyncCommand.ADD)
						.applicationData(msEmail)
						.build())
				.build();
		
		String serialized = new JSONService().serialize(analysedSyncCollection);
		assertThat(serialized).isEqualTo(
				"{\"changes\":true," + 
					"\"collectionId\":1," + 
					"\"collectionPath\":\"path\"," + 
					"\"dataType\":\"EMAIL\"," + 
					"\"deletesAsMoves\":true," + 
					"\"options\":" + 
						"{\"bodyPreferences\":" + 
							"[" + 
								"{\"allOrNone\":true," + 
									"\"truncationSize\":5," + 
									"\"type\":\"MIME\"" + 
								"}" + 
							"]," + 
							"\"conflict\":5," + 
							"\"deletesAsMoves\":false," + 
							"\"filterType\":\"ONE_DAY_BACK\"," + 
							"\"mimeSupport\":6," + 
							"\"mimeTruncation\":400," + 
							"\"truncation\":420" + 
						"}," + 
					"\"status\":\"OK\"," + 
					"\"syncKey\":{\"syncKey\":\"123\"}," + 
					"\"windowSize\":2" + 
				"}");
	}
	
	@Test
	public void testDeserializeAnalysedSyncCollection() {
		AnalysedSyncCollection analysedSyncCollection = new JSONService().deserialize(AnalysedSyncCollection.class, 
				"{\"changes\":true," + 
					"\"collectionId\":1," + 
					"\"collectionPath\":\"path\"," + 
					"\"dataType\":\"EMAIL\"," + 
					"\"deletesAsMoves\":true," + 
					"\"options\":" + 
						"{\"bodyPreferences\":" + 
							"[" + 
								"{\"allOrNone\":true," + 
									"\"truncationSize\":5," + 
									"\"type\":\"MIME\"" + 
								"}" + 
							"]," + 
							"\"conflict\":5," + 
							"\"deletesAsMoves\":false," + 
							"\"filterType\":\"ONE_DAY_BACK\"," + 
							"\"mimeSupport\":6," + 
							"\"mimeTruncation\":400," + 
							"\"truncation\":420" + 
						"}," + 
					"\"status\":\"OK\"," + 
					"\"syncKey\":{\"syncKey\":\"123\"}," + 
					"\"windowSize\":2" + 
				"}");
		
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
		
		AnalysedSyncCollection expectedAnalysedSyncCollection = AnalysedSyncCollection.builder()
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
		assertThat(analysedSyncCollection).isEqualTo(expectedAnalysedSyncCollection);
	}
	
	@Test
	public void testDeserializeOldAnalysedSyncCollection() {
		AnalysedSyncCollection analysedSyncCollection = new JSONService().deserialize(AnalysedSyncCollection.class, 
				"{\"changes\":true," + 
					"\"collectionId\":1," + 
					"\"collectionPath\":\"path\"," + 
					"\"commands\":{}," + 
					"\"dataType\":\"EMAIL\"," + 
					"\"deletesAsMoves\":true," + 
					"\"options\":" + 
						"{\"bodyPreferences\":" + 
							"[" + 
								"{\"allOrNone\":true," + 
									"\"truncationSize\":5," + 
									"\"type\":\"MIME\"" + 
								"}" + 
							"]," + 
							"\"conflict\":5," + 
							"\"deletesAsMoves\":false," + 
							"\"filterType\":\"ONE_DAY_BACK\"," + 
							"\"mimeSupport\":6," + 
							"\"mimeTruncation\":400," + 
							"\"truncation\":420" + 
						"}," + 
					"\"status\":\"OK\"," + 
					"\"syncKey\":{\"syncKey\":\"123\"}," + 
					"\"windowSize\":2" + 
				"}");
		
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
		
		AnalysedSyncCollection expectedAnalysedSyncCollection = AnalysedSyncCollection.builder()
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
		assertThat(analysedSyncCollection).isEqualTo(expectedAnalysedSyncCollection);
	}
	
	@Test
	public void testSerializeMSEvent() {
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
		
		MSRecurrence msRecurrence = new MSRecurrence();
		msRecurrence.setDayOfMonth(1);
		msRecurrence.setDayOfWeek(ImmutableSet.of(RecurrenceDayOfWeek.SATURDAY));
		msRecurrence.setDeadOccur(true);
		msRecurrence.setInterval(2);
		msRecurrence.setMonthOfYear(3);
		msRecurrence.setOccurrences(4);
		msRecurrence.setRegenerate(true);
		msRecurrence.setStart(date("2012-02-02T11:12:33"));
		msRecurrence.setType(RecurrenceType.WEEKLY);
		msRecurrence.setUntil(date("2012-02-02T09:22:33"));
		msRecurrence.setWeekOfMonth(5);
		
		MSEvent msEvent = new MSEvent();
		msEvent.setAllDayEvent(true);
		msEvent.setAttendeeEmails(ImmutableSet.of("attendee@obm.org"));
		msEvent.setBusyStatus(CalendarBusyStatus.BUSY);
		msEvent.setCategories(ImmutableList.of("category"));
		msEvent.setCreated(date("2012-02-02T11:22:33"));
		msEvent.setDescription("description");
		msEvent.setDtStamp(date("2012-02-02T11:22:30"));
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
		
		String serialized = new JSONService().serialize(msEvent);
		assertThat(serialized).isEqualTo("{" +
				"\"type\":\"CALENDAR\"," + 
				"\"allDayEvent\":true," + 
				"\"attendeeEmails\":[\"attendee@obm.org\"]," + 
				"\"attendees\":[]," + 
				"\"busyStatus\":\"BUSY\"," + 
				"\"categories\":[\"category\"]," + 
				"\"created\":1328178153000," + 
				"\"description\":\"description\"," + 
				"\"dtStamp\":1328178150000," + 
				"\"endTime\":1328178030000," + 
				"\"exceptions\":" + 
					"[{" + 
						"\"allDayEvent\":true," + 
						"\"busyStatus\":\"BUSY\"," + 
						"\"categories\":[\"category\"]," + 
						"\"deleted\":false," + 
						"\"description\":\"description\"," + 
						"\"dtStamp\":1328178150000," + 
						"\"endTime\":1328178030000," + 
						"\"exceptionStartTime\":null," + 
						"\"location\":\"location\"," + 
						"\"meetingStatus\":\"IS_A_MEETING\"," + 
						"\"reminder\":2," + 
						"\"sensitivity\":\"PERSONAL\"," + 
						"\"startTime\":1328091630000," + 
						"\"subject\":\"subject\"" + 
					"}]," + 
				"\"lastUpdate\":1330683630000," + 
				"\"location\":\"location\"," + 
				"\"meetingStatus\":\"IS_A_MEETING\"," + 
				"\"obmSequence\":1," + 
				"\"organizerEmail\":\"organizer@obm.org\"," + 
				"\"organizerName\":\"organizer\"," + 
				"\"recurrence\":{" + 
					"\"dayOfMonth\":1," + 
					"\"dayOfWeek\":[\"SATURDAY\"]," + 
					"\"deadOccur\":true," + 
					"\"interval\":2," + 
					"\"monthOfYear\":3," + 
					"\"occurrences\":4," + 
					"\"regenerate\":true," + 
					"\"start\":1328177553000," + 
					"\"type\":\"WEEKLY\"," + 
					"\"until\":1328170953000," + 
					"\"weekOfMonth\":5" + 
				"}," + 
				"\"reminder\":2," + 
				"\"sensitivity\":\"PERSONAL\"," + 
				"\"startTime\":1328091630000," + 
				"\"subject\":\"subject\"," + 
				"\"timeZone\":\"GMT\"," + 
				"\"uid\":{\"uid\":\"123\"}" + 
			"}");
	}
	
	@Test
	public void testDeserializeMSEvent() {
		MSEvent msEvent = new JSONService().deserialize(MSEvent.class, "{" +
				"\"type\":\"CALENDAR\"," + 
				"\"allDayEvent\":true," + 
				"\"attendeeEmails\":[\"attendee@obm.org\"]," + 
				"\"attendees\":[]," + 
				"\"busyStatus\":\"BUSY\"," + 
				"\"categories\":[\"category\"]," + 
				"\"created\":1328178153000," + 
				"\"description\":\"description\"," + 
				"\"dtStamp\":1328178150000," + 
				"\"endTime\":1328178030000," + 
				"\"exceptions\":" + 
					"[{" + 
						"\"allDayEvent\":true," + 
						"\"busyStatus\":\"BUSY\"," + 
						"\"categories\":[\"category\"]," + 
						"\"deleted\":false," + 
						"\"description\":\"description\"," + 
						"\"dtStamp\":1328178150000," + 
						"\"endTime\":1328178030000," + 
						"\"exceptionStartTime\":null," + 
						"\"location\":\"location\"," + 
						"\"meetingStatus\":\"IS_A_MEETING\"," + 
						"\"reminder\":2," + 
						"\"sensitivity\":\"PERSONAL\"," + 
						"\"startTime\":1328091630000," + 
						"\"subject\":\"subject\"" + 
					"}]," + 
				"\"lastUpdate\":1330683630000," + 
				"\"location\":\"location\"," + 
				"\"meetingStatus\":\"IS_A_MEETING\"," + 
				"\"obmSequence\":1," + 
				"\"organizerEmail\":\"organizer@obm.org\"," + 
				"\"organizerName\":\"organizer\"," + 
				"\"recurrence\":{" + 
					"\"dayOfMonth\":1," + 
					"\"dayOfWeek\":[\"SATURDAY\"]," + 
					"\"deadOccur\":true," + 
					"\"interval\":2," + 
					"\"monthOfYear\":3," + 
					"\"occurrences\":4," + 
					"\"regenerate\":true," + 
					"\"start\":1328177553000," + 
					"\"type\":\"WEEKLY\"," + 
					"\"until\":1328170953000," + 
					"\"weekOfMonth\":5" + 
				"}," + 
				"\"reminder\":2," + 
				"\"sensitivity\":\"PERSONAL\"," + 
				"\"startTime\":1328091630000," + 
				"\"subject\":\"subject\"," + 
				"\"timeZone\":\"GMT\"," + 
				"\"uid\":{\"uid\":\"123\"}" + 
			"}");
		
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
		
		MSRecurrence msRecurrence = new MSRecurrence();
		msRecurrence.setDayOfMonth(1);
		msRecurrence.setDayOfWeek(ImmutableSet.of(RecurrenceDayOfWeek.SATURDAY));
		msRecurrence.setDeadOccur(true);
		msRecurrence.setInterval(2);
		msRecurrence.setMonthOfYear(3);
		msRecurrence.setOccurrences(4);
		msRecurrence.setRegenerate(true);
		msRecurrence.setStart(date("2012-02-02T11:12:33"));
		msRecurrence.setType(RecurrenceType.WEEKLY);
		msRecurrence.setUntil(date("2012-02-02T09:22:33"));
		msRecurrence.setWeekOfMonth(5);
		
		MSEvent expectedMSEvent = new MSEvent();
		expectedMSEvent.setAllDayEvent(true);
		expectedMSEvent.setAttendeeEmails(ImmutableSet.of("attendee@obm.org"));
		expectedMSEvent.setBusyStatus(CalendarBusyStatus.BUSY);
		expectedMSEvent.setCategories(ImmutableList.of("category"));
		expectedMSEvent.setCreated(date("2012-02-02T11:22:33"));
		expectedMSEvent.setDescription("description");
		expectedMSEvent.setDtStamp(date("2012-02-02T11:22:30"));
		expectedMSEvent.setEndTime(date("2012-02-02T11:20:30"));
		expectedMSEvent.setExceptions(ImmutableList.of(msEventException));
		expectedMSEvent.setLastUpdate(date("2012-03-02T11:20:30"));
		expectedMSEvent.setLocation("location");
		expectedMSEvent.setMeetingStatus(CalendarMeetingStatus.IS_A_MEETING);
		expectedMSEvent.setObmSequence(1);
		expectedMSEvent.setOrganizerEmail("organizer@obm.org");
		expectedMSEvent.setOrganizerName("organizer");
		expectedMSEvent.setRecurrence(msRecurrence);
		expectedMSEvent.setReminder(2);
		expectedMSEvent.setSensitivity(CalendarSensitivity.PERSONAL);
		expectedMSEvent.setStartTime(date("2012-02-01T11:20:30"));
		expectedMSEvent.setSubject("subject");
		expectedMSEvent.setTimeZone(TimeZone.getTimeZone("GMT"));
		expectedMSEvent.setUid(new MSEventUid("123"));

		assertThat(msEvent).isEqualTo(expectedMSEvent);
	}

	
	@Test
	public void testSerializeWindowingEvent() {
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
		
		MSRecurrence msRecurrence = new MSRecurrence();
		msRecurrence.setDayOfMonth(1);
		msRecurrence.setDayOfWeek(ImmutableSet.of(RecurrenceDayOfWeek.SATURDAY));
		msRecurrence.setDeadOccur(true);
		msRecurrence.setInterval(2);
		msRecurrence.setMonthOfYear(3);
		msRecurrence.setOccurrences(4);
		msRecurrence.setRegenerate(true);
		msRecurrence.setStart(date("2012-02-02T11:12:33"));
		msRecurrence.setType(RecurrenceType.WEEKLY);
		msRecurrence.setUntil(date("2012-02-02T09:22:33"));
		msRecurrence.setWeekOfMonth(5);
		
		MSEvent msEvent = new MSEvent();
		msEvent.setAllDayEvent(true);
		msEvent.setAttendeeEmails(ImmutableSet.of("attendee@obm.org"));
		msEvent.setBusyStatus(CalendarBusyStatus.BUSY);
		msEvent.setCategories(ImmutableList.of("category"));
		msEvent.setCreated(date("2012-02-02T11:22:33"));
		msEvent.setDescription("description");
		msEvent.setDtStamp(date("2012-02-02T11:22:30"));
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
		
		String serialized = new JSONService().serialize(WindowingEvent.builder().uid(7).applicationData(msEvent).build());
		assertThat(serialized).isEqualTo(
			"{\"applicationData\":{" +
				"\"type\":\"CALENDAR\"," + 
				"\"allDayEvent\":true," + 
				"\"attendeeEmails\":[\"attendee@obm.org\"]," + 
				"\"attendees\":[]," + 
				"\"busyStatus\":\"BUSY\"," + 
				"\"categories\":[\"category\"]," + 
				"\"created\":1328178153000," + 
				"\"description\":\"description\"," + 
				"\"dtStamp\":1328178150000," + 
				"\"endTime\":1328178030000," + 
				"\"exceptions\":" + 
					"[{" + 
						"\"allDayEvent\":true," + 
						"\"busyStatus\":\"BUSY\"," + 
						"\"categories\":[\"category\"]," + 
						"\"deleted\":false," + 
						"\"description\":\"description\"," + 
						"\"dtStamp\":1328178150000," + 
						"\"endTime\":1328178030000," + 
						"\"exceptionStartTime\":null," + 
						"\"location\":\"location\"," + 
						"\"meetingStatus\":\"IS_A_MEETING\"," + 
						"\"reminder\":2," + 
						"\"sensitivity\":\"PERSONAL\"," + 
						"\"startTime\":1328091630000," + 
						"\"subject\":\"subject\"" + 
					"}]," + 
				"\"lastUpdate\":1330683630000," + 
				"\"location\":\"location\"," + 
				"\"meetingStatus\":\"IS_A_MEETING\"," + 
				"\"obmSequence\":1," + 
				"\"organizerEmail\":\"organizer@obm.org\"," + 
				"\"organizerName\":\"organizer\"," + 
				"\"recurrence\":{" + 
					"\"dayOfMonth\":1," + 
					"\"dayOfWeek\":[\"SATURDAY\"]," + 
					"\"deadOccur\":true," + 
					"\"interval\":2," + 
					"\"monthOfYear\":3," + 
					"\"occurrences\":4," + 
					"\"regenerate\":true," + 
					"\"start\":1328177553000," + 
					"\"type\":\"WEEKLY\"," + 
					"\"until\":1328170953000," + 
					"\"weekOfMonth\":5" + 
				"}," + 
				"\"reminder\":2," + 
				"\"sensitivity\":\"PERSONAL\"," + 
				"\"startTime\":1328091630000," + 
				"\"subject\":\"subject\"," + 
				"\"timeZone\":\"GMT\"," + 
				"\"uid\":{\"uid\":\"123\"}" + 
			"}," +
			"\"uid\":7}");
	}
	
	@Test
	public void testDeserializeWindowingEvent() {
		WindowingEvent event = new JSONService().deserialize(WindowingEvent.class, 
			"{\"applicationData\":{" +
					"\"type\":\"CALENDAR\"," + 
					"\"allDayEvent\":true," + 
					"\"attendeeEmails\":[\"attendee@obm.org\"]," + 
					"\"attendees\":[]," + 
					"\"busyStatus\":\"BUSY\"," + 
					"\"categories\":[\"category\"]," + 
					"\"created\":1328178153000," + 
					"\"description\":\"description\"," + 
					"\"dtStamp\":1328178150000," + 
					"\"endTime\":1328178030000," + 
					"\"exceptions\":" + 
						"[{" + 
							"\"allDayEvent\":true," + 
							"\"busyStatus\":\"BUSY\"," + 
							"\"categories\":[\"category\"]," + 
							"\"deleted\":false," + 
							"\"description\":\"description\"," + 
							"\"dtStamp\":1328178150000," + 
							"\"endTime\":1328178030000," + 
							"\"exceptionStartTime\":null," + 
							"\"location\":\"location\"," + 
							"\"meetingStatus\":\"IS_A_MEETING\"," + 
							"\"reminder\":2," + 
							"\"sensitivity\":\"PERSONAL\"," + 
							"\"startTime\":1328091630000," + 
							"\"subject\":\"subject\"" + 
						"}]," + 
					"\"lastUpdate\":1330683630000," + 
					"\"location\":\"location\"," + 
					"\"meetingStatus\":\"IS_A_MEETING\"," + 
					"\"obmSequence\":1," + 
					"\"organizerEmail\":\"organizer@obm.org\"," + 
					"\"organizerName\":\"organizer\"," + 
					"\"recurrence\":{" + 
						"\"dayOfMonth\":1," + 
						"\"dayOfWeek\":[\"SATURDAY\"]," + 
						"\"deadOccur\":true," + 
						"\"interval\":2," + 
						"\"monthOfYear\":3," + 
						"\"occurrences\":4," + 
						"\"regenerate\":true," + 
						"\"start\":1328177553000," + 
						"\"type\":\"WEEKLY\"," + 
						"\"until\":1328170953000," + 
						"\"weekOfMonth\":5" + 
					"}," + 
					"\"reminder\":2," + 
					"\"sensitivity\":\"PERSONAL\"," + 
					"\"startTime\":1328091630000," + 
					"\"subject\":\"subject\"," + 
					"\"timeZone\":\"GMT\"," + 
					"\"uid\":{\"uid\":\"123\"}" + 
				"}," +
				"\"uid\":7}");
		
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
		
		MSRecurrence msRecurrence = new MSRecurrence();
		msRecurrence.setDayOfMonth(1);
		msRecurrence.setDayOfWeek(ImmutableSet.of(RecurrenceDayOfWeek.SATURDAY));
		msRecurrence.setDeadOccur(true);
		msRecurrence.setInterval(2);
		msRecurrence.setMonthOfYear(3);
		msRecurrence.setOccurrences(4);
		msRecurrence.setRegenerate(true);
		msRecurrence.setStart(date("2012-02-02T11:12:33"));
		msRecurrence.setType(RecurrenceType.WEEKLY);
		msRecurrence.setUntil(date("2012-02-02T09:22:33"));
		msRecurrence.setWeekOfMonth(5);
		
		MSEvent expectedMSEvent = new MSEvent();
		expectedMSEvent.setAllDayEvent(true);
		expectedMSEvent.setAttendeeEmails(ImmutableSet.of("attendee@obm.org"));
		expectedMSEvent.setBusyStatus(CalendarBusyStatus.BUSY);
		expectedMSEvent.setCategories(ImmutableList.of("category"));
		expectedMSEvent.setCreated(date("2012-02-02T11:22:33"));
		expectedMSEvent.setDescription("description");
		expectedMSEvent.setDtStamp(date("2012-02-02T11:22:30"));
		expectedMSEvent.setEndTime(date("2012-02-02T11:20:30"));
		expectedMSEvent.setExceptions(ImmutableList.of(msEventException));
		expectedMSEvent.setLastUpdate(date("2012-03-02T11:20:30"));
		expectedMSEvent.setLocation("location");
		expectedMSEvent.setMeetingStatus(CalendarMeetingStatus.IS_A_MEETING);
		expectedMSEvent.setObmSequence(1);
		expectedMSEvent.setOrganizerEmail("organizer@obm.org");
		expectedMSEvent.setOrganizerName("organizer");
		expectedMSEvent.setRecurrence(msRecurrence);
		expectedMSEvent.setReminder(2);
		expectedMSEvent.setSensitivity(CalendarSensitivity.PERSONAL);
		expectedMSEvent.setStartTime(date("2012-02-01T11:20:30"));
		expectedMSEvent.setSubject("subject");
		expectedMSEvent.setTimeZone(TimeZone.getTimeZone("GMT"));
		expectedMSEvent.setUid(new MSEventUid("123"));

		assertThat(event).isEqualTo(WindowingEvent.builder().uid(7).applicationData(expectedMSEvent).build());
	}
	
	@Test
	public void testSerializeItemDeletion() {
		ItemDeletion itemDeletion = ItemDeletion.builder()
				.serverId(CollectionId.of(12).serverId(3))
				.build();
		
		String serialized = new JSONService().serialize(itemDeletion);
		assertThat(serialized).isEqualTo("{\"serverId\":\"12:3\"}");
	}
	
	@Test
	public void testDeserializeItemDeletion() {
		ItemDeletion itemDeletion = new JSONService().deserialize(ItemDeletion.class, "{\"serverId\":\"1:123\"}");
		
		assertThat(itemDeletion).isEqualTo(
			ItemDeletion.builder()
				.serverId(CollectionId.of(1).serverId(123))
				.build());
	}
	
	@Test
	public void testSerializeItemChange() {
		MSRecurrence msRecurrence = new MSRecurrence();
		msRecurrence.setType(RecurrenceType.DAILY);
		msRecurrence.setInterval(7);
		msRecurrence.setUntil(date("2004-12-11T11:15:10Z"));
		msRecurrence.setOccurrences(4);
		msRecurrence.setDayOfMonth(2);
		msRecurrence.setDayOfWeek(ImmutableSet.of(RecurrenceDayOfWeek.FRIDAY, RecurrenceDayOfWeek.SUNDAY));
		msRecurrence.setWeekOfMonth(4);
		msRecurrence.setMonthOfYear(2);
		msRecurrence.setDeadOccur(true);
		msRecurrence.setRegenerate(true);
		msRecurrence.setStart(date("2004-12-11T12:15:10Z"));
		
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
		
		ItemChange itemChange = ItemChange.builder()
			.serverId(CollectionId.of(12).serverId(3))
			.isNew(true)
			.data(msTask)
			.build();
		String serialized = new JSONService().serialize(itemChange);
		assertThat(serialized).isEqualTo(
				"{\"data\":{" + 
					"\"type\":\"TASKS\"," + 
					"\"UtcDueDate\":1328264673000," + 
					"\"categories\":[\"category\"]," + 
					"\"complete\":true," + 
					"\"dateCompleted\":1329042153000," + 
					"\"description\":\"description\"," + 
					"\"dueDate\":1328178453000," + 
					"\"importance\":2," + 
					"\"recurrence\":{" + 
						"\"dayOfMonth\":2," + 
						"\"dayOfWeek\":[\"FRIDAY\",\"SUNDAY\"]," + 
						"\"deadOccur\":true," + 
						"\"interval\":7," + 
						"\"monthOfYear\":2," + 
						"\"occurrences\":4," + 
						"\"regenerate\":true," + 
						"\"start\":1102767310000," + 
						"\"type\":\"DAILY\"," + 
						"\"until\":1102763710000," + 
						"\"weekOfMonth\":4" + 
					"}," + 
					"\"reminderSet\":true," + 
					"\"reminderTime\":1328199753000," + 
					"\"sensitivity\":\"PRIVATE\"," + 
					"\"startDate\":1328163753000," + 
					"\"subject\":\"subject\"," + 
					"\"utcDueDate\":1328264673000," + 
					"\"utcStartDate\":1328350953000" + 
				"}," + 
				"\"isNew\":true," + 
				"\"serverId\":\"12:3\"" + 
			"}");
	}
	
	@Test
	public void testDeserializeItemChange() {
		ItemChange itemChange = new JSONService().deserialize(ItemChange.class, 
			"{\"data\":{" + 
					"\"type\":\"TASKS\"," + 
					"\"UtcDueDate\":1328264673000," + 
					"\"categories\":[\"category\"]," + 
					"\"complete\":true," + 
					"\"dateCompleted\":1329042153000," + 
					"\"description\":\"description\"," + 
					"\"dueDate\":1328178453000," + 
					"\"importance\":2," + 
					"\"recurrence\":{" + 
						"\"dayOfMonth\":2," + 
						"\"dayOfWeek\":[\"FRIDAY\",\"SUNDAY\"]," + 
						"\"deadOccur\":true," + 
						"\"interval\":7," + 
						"\"monthOfYear\":2," + 
						"\"occurrences\":4," + 
						"\"regenerate\":true," + 
						"\"start\":1102767310000," + 
						"\"type\":\"DAILY\"," + 
						"\"until\":1102763710000," + 
						"\"weekOfMonth\":4" + 
					"}," + 
					"\"reminderSet\":true," + 
					"\"reminderTime\":1328199753000," + 
					"\"sensitivity\":\"PRIVATE\"," + 
					"\"startDate\":1328163753000," + 
					"\"subject\":\"subject\"," + 
					"\"utcDueDate\":1328264673000," + 
					"\"utcStartDate\":1328350953000" + 
				"}," + 
				"\"isNew\":true," + 
				"\"serverId\":\"12:3\"" + 
			"}");
		
		MSRecurrence msRecurrence = new MSRecurrence();
		msRecurrence.setType(RecurrenceType.DAILY);
		msRecurrence.setInterval(7);
		msRecurrence.setUntil(date("2004-12-11T11:15:10Z"));
		msRecurrence.setOccurrences(4);
		msRecurrence.setDayOfMonth(2);
		msRecurrence.setDayOfWeek(ImmutableSet.of(RecurrenceDayOfWeek.FRIDAY, RecurrenceDayOfWeek.SUNDAY));
		msRecurrence.setWeekOfMonth(4);
		msRecurrence.setMonthOfYear(2);
		msRecurrence.setDeadOccur(true);
		msRecurrence.setRegenerate(true);
		msRecurrence.setStart(date("2004-12-11T12:15:10Z"));
		
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
		
		ItemChange expectedItemChange = ItemChange.builder()
			.serverId(CollectionId.of(12).serverId(3))
			.isNew(true)
			.data(msTask)
			.build();
		assertThat(itemChange).isEqualTo(expectedItemChange);
	}
	
	@Test
	public void testSerializeSetSingleValue() {
		ImmutableSet<MSEventUid> msEventUids = ImmutableSet.<MSEventUid> of(new MSEventUid("uid"));
		
		Set<String> serialized = new JSONService().serializeSet(msEventUids);
		assertThat(serialized).containsOnly("{\"uid\":\"uid\"}");
	}
	
	@Test
	public void testSerializeSet() {
		ImmutableSet<MSEventUid> msEventUids = ImmutableSet.<MSEventUid> of(new MSEventUid("uid1"), new MSEventUid("uid2"));
		
		Set<String> serialized = new JSONService().serializeSet(msEventUids);
		assertThat(serialized).containsOnly("{\"uid\":\"uid1\"}", "{\"uid\":\"uid2\"}");
	}
	
	@Test
	public void testSerializeEmptySet() {
		ImmutableSet<MSEventUid> msEventUids = ImmutableSet.<MSEventUid> of();
		
		Set<String> serialized = new JSONService().serializeSet(msEventUids);
		assertThat(serialized).isEmpty();
	}
	
	@Test
	public void testDeserializeSetSingleValue() {
		Set<MSEventUid> deserializedSet = new JSONService().deserializeSet(MSEventUid.class, ImmutableSet.<String> of("{\"uid\":\"uid1\"}"));
		
		MSEventUid expectedMSEventUid = new MSEventUid("uid1");
		assertThat(deserializedSet).containsOnly(expectedMSEventUid);
	}
	
	@Test
	public void testDeserializeSet() {
		Set<MSEventUid> deserializedSet = new JSONService().deserializeSet(MSEventUid.class, ImmutableSet.<String> of("{\"uid\":\"uid1\"}", "{\"uid\":\"uid2\"}"));
		
		MSEventUid expectedMSEventUid = new MSEventUid("uid1");
		MSEventUid expectedMSEventUid2 = new MSEventUid("uid2");
		assertThat(deserializedSet).containsOnly(expectedMSEventUid, expectedMSEventUid2);
	}
	
	@Test
	public void testEmptyDeserializeSet() {
		Set<MSEventUid> deserializedSet = new JSONService().deserializeSet(MSEventUid.class, ImmutableSet.<String> of());
		
		assertThat(deserializedSet).isEmpty();
	}
	
	private Date date(String date) {
		return new DateTime(date, DateTimeZone.forID("Europe/Paris")).toDate();
	}

}
