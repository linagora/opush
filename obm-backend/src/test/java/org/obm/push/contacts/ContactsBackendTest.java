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
package org.obm.push.contacts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.NoPermissionException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.obm.configuration.ContactConfiguration;
import org.obm.push.backend.WindowingContact;
import org.obm.push.backend.WindowingContactChanges;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSContact;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.AddressBookId;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.impl.ObmSyncBackend.WindowingChangesDelta;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.resource.OpushResourcesHolder;
import org.obm.push.service.ClientIdService;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.WindowingDao;
import org.obm.push.utils.DateUtils;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.book.AddressBook;
import org.obm.sync.book.Contact;
import org.obm.sync.book.Folder;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.exception.ContactNotFoundException;
import org.obm.sync.items.ContactChanges;
import org.obm.sync.items.FolderChanges;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;


public class ContactsBackendTest {

	private static final String DEFAULT_PARENT_BOOK_ID = "1234";
	private static final String DEFAULT_PARENT_BOOK_NAME = "contacts";
	
	private User user;
	private Device device;
	private UserDataRequest userDataRequest;
	private Date now;
	private Date epoch;
	
	private IMocksControl mocks;
	private MappingService mappingService;
	private BookClient bookClient;
	private BookClient.Factory bookClientFactory;
	private ContactConfiguration contactConfiguration;
	private WindowingDao windowingDao;
	private ClientIdService clientIdService;
	private AccessToken token;
	private ContactsBackend contactsBackend;
	private CloseableHttpClient httpClient;
	private ContactConverter contactConverter;
	private DateService dateService;
	private OpushResourcesHolder opushResourcesHolder;
	private ContactCreationIdempotenceService creationIdempotenceService;
	private FolderSnapshotDao folderSnapshotDao;
	
	@Before
	public void setUp() {
		user = Factory.create().createUser("test@test", "test@domain", "displayName");
		device = new Device.Factory().create(null, "iPhone", "iOs 5", new DeviceId("my phone"), null);
		token = new AccessToken(0, "OBM");
		userDataRequest = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		httpClient = HttpClientBuilder.create().build();
		now = DateUtils.date("2222-11-11T11:11:11Z");
		epoch = DateUtils.date("1970-01-01T00:00:00Z");
		
		mocks = createControl();
		opushResourcesHolder = mocks.createMock(OpushResourcesHolder.class);
		expect(opushResourcesHolder.getAccessToken()).andReturn(token).anyTimes();
		expect(opushResourcesHolder.getHttpClient()).andReturn(httpClient).anyTimes();
		
		mappingService = mocks.createMock(MappingService.class);
		bookClient = mocks.createMock(BookClient.class);
		bookClientFactory = mocks.createMock(BookClient.Factory.class);
		expect(bookClientFactory.create(anyObject(HttpClient.class)))
			.andReturn(bookClient).anyTimes();
		contactConfiguration = mocks.createMock(ContactConfiguration.class);
		windowingDao = mocks.createMock(WindowingDao.class);
		clientIdService = mocks.createMock(ClientIdService.class);
		contactConverter = new ContactConverter();
		dateService = mocks.createMock(DateService.class);
		creationIdempotenceService = mocks.createMock(ContactCreationIdempotenceService.class);
		folderSnapshotDao = mocks.createMock(FolderSnapshotDao.class);
		
		contactsBackend = new ContactsBackend(mappingService, bookClientFactory, 
				contactConfiguration, windowingDao, 
				clientIdService, contactConverter, dateService, opushResourcesHolder,
				creationIdempotenceService, folderSnapshotDao);
		
		expectDefaultAddressAndParentForContactConfiguration();
	}
	
	@After
	public void teardown() throws IOException {
		httpClient.close();
	}
	
	@Test
	public void sortedByDefaultFolderName() {
		final String defaultFolderName = DEFAULT_PARENT_BOOK_NAME;
		
		Folder f1 = Folder.builder().name("users").uid(-1).ownerLoginAtDomain(user.getLoginAtDomain()).build();
		Folder f2 = Folder.builder().name("collected_contacts").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build();
		Folder f3 = Folder.builder().name(defaultFolderName).uid(3).ownerLoginAtDomain(user.getLoginAtDomain()).build();
		Folder f4 = Folder.builder().name("my address book").uid(4).ownerLoginAtDomain(user.getLoginAtDomain()).build();
		
		TreeSet<Folder> treeset = new TreeSet<Folder>(new ComparatorUsingFolderName(defaultFolderName));
		treeset.addAll(ImmutableList.of(f1, f2, f3, f4));
		
		assertThat(treeset).hasSize(4);
		assertThat(treeset).contains(f1, f2, f3, f4);
		assertThat(treeset.first().getName()).isEqualTo(defaultFolderName);
		assertThat(treeset.last().getName()).isEqualTo("users");
	}
	
	@Test
	public void testGetPIMDataType() {
		ContactsBackend contactsBackend = new ContactsBackend(null, null, null, null, null, null, null, null, null, null);
		assertThat(contactsBackend.getPIMDataType()).isEqualTo(PIMDataType.CONTACTS);
	}

	@Test
	public void testGetChanges() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		AddressBook.Id bookId = AddressBook.Id.valueOf(2);
		Date currentDate = DateUtils.getCurrentDate();
		ItemSyncState lastKnownState = ItemSyncState.builder()
				.syncDate(currentDate)
				.syncKey(new SyncKey("1234567890a"))
				.build();

		ContactChanges contactChanges = new ContactChanges(ImmutableList.<Contact> of(), ImmutableSet.<Integer> of(), currentDate);
		expect(bookClient.listContactsChanged(token, currentDate, bookId.getId())).andReturn(contactChanges).once();
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId.getId()))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder()
				.filterType(FilterType.ALL_ITEMS)
				.bodyPreferences(ImmutableList.of(BodyPreference.builder().build()))
				.build();
		
		mocks.replay();
		WindowingChangesDelta<WindowingContact> allChanges = contactsBackend.getAllChanges(userDataRequest, lastKnownState, collectionId, syncCollectionOptions);
		mocks.verify();

		assertThat(allChanges.getWindowingChanges()).isEqualTo(WindowingContactChanges.builder()
				.changes(ImmutableList.<WindowingContact>of())
				.deletions(ImmutableList.<WindowingContact>of())
				.build());
	}
	
	@Test
	public void testGetAllChangesOnFirstSync() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		Date currentDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState lastKnownState = ItemSyncState.builder()
				.syncDate(currentDate)
				.syncKey(new SyncKey("1234567890a"))
				.build();
		
		Contact contact = new Contact();
		contact.setFirstname("contact");
		contact.setUid(1);
		Contact contact2 = new Contact();
		contact2.setFirstname("contact2");
		contact2.setUid(2);

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
				org.obm.push.bean.change.hierarchy.Folder.builder()
					.displayName("folder")
					.collectionId(collectionId)
					.backendId(AddressBookId.of(bookId))
					.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
					.build());
			
		ContactChanges contactChanges = new ContactChanges(ImmutableList.of(contact, contact2), ImmutableSet.<Integer> of(), currentDate);
		expect(bookClient.firstListContactsChanged(token, currentDate, bookId)).andReturn(contactChanges).once();
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder()
				.filterType(FilterType.ALL_ITEMS)
				.bodyPreferences(ImmutableList.of(BodyPreference.builder().build()))
				.build();
		
		mocks.replay();
		WindowingChangesDelta<WindowingContact> allChanges = contactsBackend.getAllChanges(userDataRequest, lastKnownState, collectionId, syncCollectionOptions);
		mocks.verify();

		MSContact msContact = new MSContact();
		msContact.setFirstName(contact.getFirstname());
		msContact.setFileAs(contact.getFirstname());
		WindowingContact expectedWindowingContact = WindowingContact.builder()
			.uid(1)
			.applicationData(msContact)
			.build();
		MSContact msContact2 = new MSContact();
		msContact2.setFirstName(contact2.getFirstname());
		msContact2.setFileAs(contact2.getFirstname());
		WindowingContact expectedWindowingContact2 = WindowingContact.builder()
				.uid(2)
				.applicationData(msContact2)
				.build();
		
		assertThat(allChanges.getWindowingChanges()).isEqualTo(WindowingContactChanges.builder()
				.changes(ImmutableList.of(expectedWindowingContact, expectedWindowingContact2))
				.deletions(ImmutableList.<WindowingContact>of())
				.build());
	}

	@Test
	public void testGetItemEstimateSize() throws Exception {
		Date currentDate = DateUtils.getCurrentDate();
		ItemSyncState lastKnownState = ItemSyncState.builder()
				.syncDate(currentDate)
				.syncKey(new SyncKey("1234567890a"))
				.build();

		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		int contactChangedUid = 215;
		Contact contactChanged = newContactObject(contactChangedUid);
		ContactChanges contactChanges = new ContactChanges(ImmutableList.<Contact> of(contactChanged), ImmutableSet.<Integer> of(), currentDate);
		expect(bookClient.listContactsChanged(token, currentDate, bookId))
			.andReturn(contactChanges).once();
		
		mocks.replay();
		int itemEstimateSize = contactsBackend.getItemEstimateSize(userDataRequest, lastKnownState, collectionId, null);
		mocks.verify();
		
		assertThat(itemEstimateSize).isEqualTo(1);
	}
	
	@Test
	public void testCreateOrUpdate() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);
		String clientId = "1";
		String clientIdHash = "146565647814688";

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		expect(clientIdService.hash(userDataRequest, clientId)).andReturn(clientIdHash);
		
		Contact contact = newContactObject(serverId.getItemId());
		expect(bookClient.storeContact(token, bookId, contact, clientIdHash))
			.andReturn(contact).once();
		
		expect(mappingService.getServerIdFor(collectionId, String.valueOf(serverId.getItemId())))
			.andReturn(serverId);

		MSContact msContact = new MSContact();

		mocks.replay();
		ServerId newServerId = contactsBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, msContact);
		mocks.verify();
		
		assertThat(newServerId).isEqualTo(collectionId.serverId(215));
	}
	
	@Test
	public void createShouldStoreContact() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);
		String clientId = "1";
		String clientIdHash = "146565647814688";

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		expect(clientIdService.hash(userDataRequest, clientId)).andReturn(clientIdHash);
		
		MSContact msContact = new MSContact();
		
		Contact contact = newContactObject(serverId.getItemId());
		Contact convertedContact = contactConverter.contact(msContact);
		expect(bookClient.storeContact(token, bookId, convertedContact, clientIdHash))
			.andReturn(contact).once();

		expect(creationIdempotenceService.find(userDataRequest, collectionId, msContact)).
			andReturn(Optional.<ServerId>absent());
		expect(mappingService.getServerIdFor(collectionId, "215"))
			.andReturn(serverId);
		expect(creationIdempotenceService.registerCreation(userDataRequest, msContact, serverId))
			.andReturn(serverId);

		mocks.replay();
		ServerId newServerId = contactsBackend.createOrUpdate(userDataRequest, collectionId, null, clientId, msContact);
		mocks.verify();
		
		assertThat(newServerId).isEqualTo(serverId);
	}
	
	@Test
	public void createShouldOnlyReturnServerIdWhenHashAlreadyKnownAndContactExists() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);
		String clientId = "1";

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		MSContact msContact = new MSContact();
		expect(creationIdempotenceService.find(userDataRequest, collectionId, msContact))
			.andReturn(Optional.of(serverId));
		
		expect(bookClient.getContactFromId(token, bookId, serverId.getItemId())).andReturn(new Contact());

		mocks.replay();
		ServerId newServerId = contactsBackend.createOrUpdate(userDataRequest, collectionId, null, clientId, msContact);
		mocks.verify();
		
		assertThat(newServerId).isEqualTo(serverId);
	}
	
	@Test
	public void createShouldStoreContactWhenHashAlreadyKnownButContactDoesntExistAnymore() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);
		String clientId = "1";
		String clientIdHash = "146565647814688";

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		MSContact msContact = new MSContact();
		expect(creationIdempotenceService.find(userDataRequest, collectionId, msContact))
			.andReturn(Optional.of(serverId));
		
		expect(bookClient.getContactFromId(token, bookId, serverId.getItemId())).andThrow(new ContactNotFoundException(""));

		Contact contact = newContactObject(serverId.getItemId());
		Contact convertedContact = contactConverter.contact(msContact);
		expect(clientIdService.hash(userDataRequest, clientId)).andReturn(clientIdHash);
		expect(bookClient.storeContact(token, bookId, convertedContact, clientIdHash))
			.andReturn(contact).once();
		expect(mappingService.getServerIdFor(collectionId, String.valueOf(serverId.getItemId())))
			.andReturn(serverId);
		expect(creationIdempotenceService.registerCreation(userDataRequest, msContact, serverId))
		.andReturn(serverId);
		
		mocks.replay();
		ServerId newServerId = contactsBackend.createOrUpdate(userDataRequest, collectionId, null, clientId, msContact);
		mocks.verify();
		
		assertThat(newServerId).isEqualTo(serverId);
	}
	
	@Test(expected=RuntimeException.class)
	public void createShouldLetPropagateUnexpectedExceptionFromContactExistMethod() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);
		String clientId = "1";

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		MSContact msContact = new MSContact();
		expect(creationIdempotenceService.find(userDataRequest, collectionId, msContact))
			.andReturn(Optional.of(serverId));
		
		expect(bookClient.getContactFromId(token, bookId, serverId.getItemId())).andThrow(new RuntimeException(""));
		
		mocks.replay();
		try {
			contactsBackend.createOrUpdate(userDataRequest, collectionId, null, clientId, msContact);
		} finally {
			mocks.verify();
		}
	}
	
	@Test
	public void updateShouldStoreContact() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);
		String clientId = null;

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		MSContact msContact = new MSContact();
		Contact contact = contactConverter.contact(msContact);
		contact.setUid(serverId.getItemId());
		expect(bookClient.storeContact(token, bookId, contact, clientId))
			.andReturn(contact).once();
		
		expect(mappingService.getServerIdFor(collectionId, "215"))
			.andReturn(serverId);

		mocks.replay();
		ServerId newServerId = contactsBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, msContact);
		mocks.verify();
		
		assertThat(newServerId).isEqualTo(serverId);
	}
	
	@Test(expected=NoPermissionException.class)
	public void testCreateNoPermissionLetsPropagateTheException() throws Exception {
		MSContact msContact = new MSContact();
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = null;
		String clientId = "1";
		String clientIdHash = "146565647814688";

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		expect(creationIdempotenceService.find(userDataRequest, collectionId, msContact))
			.andReturn(Optional.<ServerId>absent());
		expect(clientIdService.hash(userDataRequest, clientId)).andReturn(clientIdHash);
		
		expect(bookClient.storeContact(token, bookId, new Contact(), clientIdHash)).andThrow(new NoPermissionException());

		mocks.replay();
		try {
			contactsBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, msContact);
		} catch (NoPermissionException e) {
			mocks.verify();
			throw e;
		}
	}

	@Test(expected=NoPermissionException.class)
	public void testUpdateNoPermissionLetsPropagateTheException() throws Exception {
		MSContact msContact = new MSContact();
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);
		String clientId = "1";
		String clientIdHash = "146565647814688";

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		expect(clientIdService.hash(userDataRequest, clientId)).andReturn(clientIdHash);
		
		Contact contact = newContactObject(serverId.getItemId());
		expect(bookClient.storeContact(token, bookId, contact, clientIdHash)).andThrow(new NoPermissionException());

		mocks.replay();
		try {
			contactsBackend.createOrUpdate(userDataRequest, collectionId, serverId, clientId, msContact);
		} catch (NoPermissionException e) {
			mocks.verify();
			throw e;
		}
	}
	
	@Test
	public void testDelete() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		expect(bookClient.removeContact(token, bookId, serverId.getItemId()))
			.andReturn(newContactObject(serverId.getItemId())).once();
		
		creationIdempotenceService.remove(userDataRequest, collectionId, serverId);
		expectLastCall();

		mocks.replay();
		contactsBackend.delete(userDataRequest, collectionId, serverId, true);
		mocks.verify();
	}
	
	@Test
	public void removeCreationIdempotenceShouldBeCalledEvenWhenException() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		
		expect(bookClient.removeContact(token, bookId, serverId.getItemId()))
			.andThrow(new ContactNotFoundException("not found"));
		
		creationIdempotenceService.remove(userDataRequest, collectionId, serverId);
		expectLastCall();

		mocks.replay();
		contactsBackend.delete(userDataRequest, collectionId, serverId, true);
		mocks.verify();
	}

	@Test
	public void testFetch() throws Exception {
		ItemSyncState state = null;
		SyncCollectionOptions options = null;
		SyncKey newSyncKey = new SyncKey("132");
		CollectionId collectionId = CollectionId.of(1);
		int bookId = 5;
		ServerId serverId = collectionId.serverId(215);

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(
			org.obm.push.bean.change.hierarchy.Folder.builder()
				.displayName("folder")
				.collectionId(collectionId)
				.backendId(AddressBookId.of(bookId))
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build());
		Contact contact = newContactObject(serverId.getItemId());
		expect(bookClient.getContactFromId(token, bookId, serverId.getItemId())).andReturn(contact);
		expect(mappingService.getServerIdFor(collectionId, String.valueOf(serverId.getItemId()))).andReturn(serverId);
	
		mocks.replay();
		List<ItemChange> itemChanges = contactsBackend.fetch(userDataRequest, collectionId, ImmutableList.of(serverId), options, state, newSyncKey);
		mocks.verify();
		
		ItemChange itemChange = ItemChange.builder()
				.serverId(serverId)
				.isNew(false)
				.data(contactConverter.convert(contact))
				.build();
		
		assertThat(itemChanges).hasSize(1);
		assertThat(itemChanges).containsOnly(itemChange);
	}

	private Contact newContactObject(int contactUid) {
		Contact contact = new Contact();
		contact.setUid(contactUid);
		return contact;
	}

	private void expectDefaultAddressAndParentForContactConfiguration() {
		expect(contactConfiguration.getDefaultAddressBookName())
			.andReturn(DEFAULT_PARENT_BOOK_NAME).anyTimes();
		
		expect(contactConfiguration.getDefaultParentId())
			.andReturn(DEFAULT_PARENT_BOOK_ID).anyTimes();
	}

	@Test
	public void currentFoldersShouldCallObmSync() throws Exception {
		Folder change = Folder.builder()
			.name("contacts")
			.uid(12)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();

		Set<Folder> changes = ImmutableSet.of(change);
		Set<Folder> deletions = ImmutableSet.of();
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);

		expect(bookClient.listAddressBooksChanged(token, epoch)).andReturn(folderChanges);

		mocks.replay();
		BackendFolders currentFolders = contactsBackend.getBackendFolders(userDataRequest);
		mocks.verify();

		assertThat(currentFolders).hasSize(1);
		assertThat(currentFolders.iterator().next()).isEqualTo(
			BackendFolder.builder()
				.backendId(AddressBookId.of(12))
				.displayName("contacts")
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build());
	}
	
	@Test(expected=UnexpectedObmSyncServerException.class)
	public void currentFoldersShouldWrapServerFault() throws Exception {
		expect(bookClient.listAddressBooksChanged(token, epoch)).andThrow(new ServerFault("error"));

		mocks.replay();
		try {
			contactsBackend.getBackendFolders(userDataRequest);
		} finally {
			mocks.verify();
		}
	}
	
	@Test(expected=IllegalStateException.class)
	public void currentFoldersShouldPropagateOtherException() throws Exception {
		expect(bookClient.listAddressBooksChanged(token, epoch)).andThrow(new IllegalStateException("error"));

		mocks.replay();
		try {
			contactsBackend.getBackendFolders(userDataRequest);
		} finally {
			mocks.verify();
		}
	}
}
