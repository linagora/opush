/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015 Linagora
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
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.obm.DateUtils;
import org.obm.configuration.ContactConfiguration;
import org.obm.push.backend.CollectionPath;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.resource.OpushResourcesHolder;
import org.obm.push.service.ClientIdService;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.impl.MappingService;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.WindowingDao;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.items.FolderChanges;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;

public class ContactBackendGetFoldersTest {

	private User user;
	private Device device;
	private UserDataRequest udr;
	private AccessToken accessToken;

	private IMocksControl mocks;
	private MappingService mappingService;
	private BookClient bookClient;
	private BookClient.Factory bookClientFactory;
	private ContactConfiguration contactConfiguration;
	private Provider<CollectionPath.Builder> collectionPathBuilderProvider;
	private WindowingDao windowingDao;
	private ClientIdService clientIdService;
	private ContactsBackend contactsBackend;
	private CloseableHttpClient httpClient;
	private ContactConverter contactConverter;
	private DateService dateService;
	private OpushResourcesHolder opushResourcesHolder;
	private FolderSnapshotDao folderSnapshotDao;
	
	private String contactDisplayName;
	private Date epoch;
	private Date now;
	private FolderSyncKey incomingSK;
	private FolderSyncKey outgoingSK;

	@Before
	public void setUp() {
		user = Factory.create().createUser("test@test", "test@domain", "displayName");
		device = new Device.Factory().create(null, "iPhone", "iOs 5", new DeviceId("my phone"), null);
		udr = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		accessToken = new AccessToken(0, "OBM");
		httpClient = HttpClientBuilder.create().build();
		
		contactDisplayName = "contacts";
		epoch = DateUtils.date("1970-01-01T00:00:00Z");
		now = DateUtils.date("2222-11-11T11:11:11Z");
		incomingSK = new FolderSyncKey("1234");
		outgoingSK = new FolderSyncKey("4567");

		mocks = createControl();
		opushResourcesHolder = mocks.createMock(OpushResourcesHolder.class);
		mappingService = mocks.createMock(MappingService.class);
		bookClient = mocks.createMock(BookClient.class);
		bookClientFactory = mocks.createMock(BookClient.Factory.class);
		contactConfiguration = mocks.createMock(ContactConfiguration.class);
		collectionPathBuilderProvider = mocks.createMock(Provider.class);
		windowingDao = mocks.createMock(WindowingDao.class);
		clientIdService = mocks.createMock(ClientIdService.class);
		dateService = mocks.createMock(DateService.class);
		folderSnapshotDao = mocks.createMock(FolderSnapshotDao.class);
		contactConverter = new ContactConverter();
		
		expect(opushResourcesHolder.getAccessToken()).andReturn(accessToken).anyTimes();
		expect(opushResourcesHolder.getHttpClient()).andReturn(httpClient).anyTimes();
		expect(bookClientFactory.create(anyObject(HttpClient.class))).andReturn(bookClient).anyTimes();
		expect(contactConfiguration.getDefaultAddressBookName()).andReturn("contacts").anyTimes();
		
		contactsBackend = new ContactsBackend(mappingService, 
				bookClientFactory, 
				contactConfiguration, 
				collectionPathBuilderProvider,
				windowingDao,
				clientIdService,
				contactConverter,
				dateService,
				opushResourcesHolder,
				folderSnapshotDao);
	}

	@After
	public void teardown() throws IOException {
		httpClient.close();
	}

	@Test
	public void getFoldersShouldReturnOneAddWhenEmptyKnownSnapshotAndOneChange() throws Exception {
		org.obm.sync.book.Folder change = org.obm.sync.book.Folder.builder()
			.name(contactDisplayName)
			.uid(12)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();

		Set<org.obm.sync.book.Folder> changes = ImmutableSet.of(change);
		Set<org.obm.sync.book.Folder> deletions = ImmutableSet.of();
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);
		expect(bookClient.listAddressBooksChanged(accessToken, epoch)).andReturn(folderChanges);
		
		Folder newSnapshotFolder = Folder.builder()
			.displayName(contactDisplayName)
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.empty();
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(newSnapshotFolder));
		expect(folderSnapshotDao.get(user, device, PIMDataType.CONTACTS, incomingSK)).andReturn(knownSnapshot);
		folderSnapshotDao.create(user, device, PIMDataType.CONTACTS, outgoingSK, newSnapshot);
		expectLastCall();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getFolders(udr, incomingSK, outgoingSK);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(CollectionChange.builder()
			.collectionId(newSnapshotFolder.getCollectionId())
			.parentCollectionId(CollectionId.ROOT)
			.displayName(newSnapshotFolder.getDisplayName())
			.folderType(newSnapshotFolder.getFolderType())
			.isNew(true).build());
	}

	@Test
	public void getFoldersShouldReturnOneAddWhenKnownSnapshotAndOneMoreChange() throws Exception {
		org.obm.sync.book.Folder knownChange = org.obm.sync.book.Folder.builder()
			.name(contactDisplayName)
			.uid(12)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();
		org.obm.sync.book.Folder addFolder = org.obm.sync.book.Folder.builder()
			.name("another name")
			.uid(15)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();

		Set<org.obm.sync.book.Folder> changes = ImmutableSet.of(knownChange, addFolder);
		Set<org.obm.sync.book.Folder> deletions = ImmutableSet.of();
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);
		expect(bookClient.listAddressBooksChanged(accessToken, epoch)).andReturn(folderChanges);

		Folder knownSnapshotFolder = Folder.builder()
			.displayName(contactDisplayName)
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		Folder addSnapshotFolder = Folder.builder()
			.displayName("another name")
			.backendId("15")
			.collectionId(CollectionId.of(2))
			.parentBackendId(Optional.of(knownSnapshotFolder.getBackendId()))
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(3).folders(ImmutableSet.of(knownSnapshotFolder, addSnapshotFolder));
		expect(folderSnapshotDao.get(user, device, PIMDataType.CONTACTS, incomingSK)).andReturn(knownSnapshot);
		folderSnapshotDao.create(user, device, PIMDataType.CONTACTS, outgoingSK, newSnapshot);
		expectLastCall();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getFolders(udr, incomingSK, outgoingSK);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(CollectionChange.builder()
			.collectionId(addSnapshotFolder.getCollectionId())
			.parentCollectionId(knownSnapshotFolder.getCollectionId())
			.displayName(addSnapshotFolder.getDisplayName())
			.folderType(addSnapshotFolder.getFolderType())
			.isNew(true).build());
	}

	@Test
	public void getFoldersShouldReturnEmptyWhenEmptyKnownSnapshotAndOneDeletion() throws Exception {
		org.obm.sync.book.Folder deleted = org.obm.sync.book.Folder.builder()
			.name(contactDisplayName)
			.uid(12)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();

		Set<org.obm.sync.book.Folder> changes = ImmutableSet.of();
		Set<org.obm.sync.book.Folder> deletions = ImmutableSet.of(deleted);
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);
		expect(bookClient.listAddressBooksChanged(accessToken, epoch)).andReturn(folderChanges);
		
		FolderSnapshot knownSnapshot = FolderSnapshot.empty();
		FolderSnapshot newSnapshot = FolderSnapshot.empty();
		expect(folderSnapshotDao.get(user, device, PIMDataType.CONTACTS, incomingSK)).andReturn(knownSnapshot);
		folderSnapshotDao.create(user, device, PIMDataType.CONTACTS, outgoingSK, newSnapshot);
		expectLastCall();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getFolders(udr, incomingSK, outgoingSK);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
	}

	@Test
	public void getFoldersShouldReturnEmptyWhenKnownSnapshotAndSameChangeAndDeletion() throws Exception {
		// The "deletions" are always discarded as snapshot behavior
		// manage the diff itself, so deletions. The deletion should be discarded.
		org.obm.sync.book.Folder sameAddAndDelete = org.obm.sync.book.Folder.builder()
			.name(contactDisplayName)
			.uid(12)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();

		Set<org.obm.sync.book.Folder> changes = ImmutableSet.of(sameAddAndDelete);
		Set<org.obm.sync.book.Folder> deletions = ImmutableSet.of(sameAddAndDelete);
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);
		expect(bookClient.listAddressBooksChanged(accessToken, epoch)).andReturn(folderChanges);

		Folder snapshotFolder = Folder.builder()
			.displayName(contactDisplayName)
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(snapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(snapshotFolder));
		expect(folderSnapshotDao.get(user, device, PIMDataType.CONTACTS, incomingSK)).andReturn(knownSnapshot);
		folderSnapshotDao.create(user, device, PIMDataType.CONTACTS, outgoingSK, newSnapshot);
		expectLastCall();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getFolders(udr, incomingSK, outgoingSK);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
	}

	@Test
	public void getFoldersShouldReturnEmptyWhenEmptyKnownSnapshotAndNoChange() throws Exception {
		Set<org.obm.sync.book.Folder> changes = ImmutableSet.of();
		Set<org.obm.sync.book.Folder> deletions = ImmutableSet.of();
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);
		expect(bookClient.listAddressBooksChanged(accessToken, epoch)).andReturn(folderChanges);
		
		FolderSnapshot knownSnapshot = FolderSnapshot.empty();
		FolderSnapshot newSnapshot = FolderSnapshot.empty();
		expect(folderSnapshotDao.get(user, device, PIMDataType.CONTACTS, incomingSK)).andReturn(knownSnapshot);
		folderSnapshotDao.create(user, device, PIMDataType.CONTACTS, outgoingSK, newSnapshot);
		expectLastCall();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getFolders(udr, incomingSK, outgoingSK);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
	}
	
	@Test
	public void getFoldersShouldReturnOneDeletionWhenKnownSnapshotAndNoChange() throws Exception {
		Set<org.obm.sync.book.Folder> changes = ImmutableSet.of();
		Set<org.obm.sync.book.Folder> deletions = ImmutableSet.of();
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);
		expect(bookClient.listAddressBooksChanged(accessToken, epoch)).andReturn(folderChanges);

		Folder knownSnapshotFolder = Folder.builder()
			.displayName(contactDisplayName)
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.<Folder>of());
		expect(folderSnapshotDao.get(user, device, PIMDataType.CONTACTS, incomingSK)).andReturn(knownSnapshot);
		folderSnapshotDao.create(user, device, PIMDataType.CONTACTS, outgoingSK, newSnapshot);
		expectLastCall();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getFolders(udr, incomingSK, outgoingSK);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).containsOnly(
			CollectionDeletion.builder().collectionId(knownSnapshotFolder.getCollectionId()).build());
	}
	
	@Test
	public void getFoldersShouldReturnEmptyWhenKnownSnapshotAndChange() throws Exception {
		org.obm.sync.book.Folder knownChange = org.obm.sync.book.Folder.builder()
			.name(contactDisplayName)
			.uid(12)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();

		Set<org.obm.sync.book.Folder> changes = ImmutableSet.of(knownChange);
		Set<org.obm.sync.book.Folder> deletions = ImmutableSet.of();
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);
		expect(bookClient.listAddressBooksChanged(accessToken, epoch)).andReturn(folderChanges);

		Folder knownSnapshotFolder = Folder.builder()
			.displayName(contactDisplayName)
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));
		expect(folderSnapshotDao.get(user, device, PIMDataType.CONTACTS, incomingSK)).andReturn(knownSnapshot);
		folderSnapshotDao.create(user, device, PIMDataType.CONTACTS, outgoingSK, newSnapshot);
		expectLastCall();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getFolders(udr, incomingSK, outgoingSK);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void getFoldersShouldReturnOneChangeWhenKnownSnapshotAndChange() throws Exception {
		org.obm.sync.book.Folder changed = org.obm.sync.book.Folder.builder()
			.name("another display name")
			.uid(12)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();
		
		Set<org.obm.sync.book.Folder> changes = ImmutableSet.of(changed);
		Set<org.obm.sync.book.Folder> deletions = ImmutableSet.of();
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);
		expect(bookClient.listAddressBooksChanged(accessToken, epoch)).andReturn(folderChanges);

		Folder knownSnapshotFolder = Folder.builder()
			.displayName(contactDisplayName)
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();

		Folder newSnapshotFolder = Folder.builder()
			.displayName("another display name")
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(newSnapshotFolder));
		expect(folderSnapshotDao.get(user, device, PIMDataType.CONTACTS, incomingSK)).andReturn(knownSnapshot);
		folderSnapshotDao.create(user, device, PIMDataType.CONTACTS, outgoingSK, newSnapshot);
		expectLastCall();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getFolders(udr, incomingSK, outgoingSK);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(CollectionChange.builder()
			.collectionId(newSnapshotFolder.getCollectionId())
			.parentCollectionId(CollectionId.ROOT)
			.displayName(newSnapshotFolder.getDisplayName())
			.folderType(newSnapshotFolder.getFolderType())
			.isNew(false).build());
	}
	
	@Test
	public void getFoldersShouldReturnOneOfEachWhenKnownSnapshotAndManyChanges() throws Exception {
		org.obm.sync.book.Folder same = org.obm.sync.book.Folder.builder()
			.name(contactDisplayName)
			.uid(12)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();
		org.obm.sync.book.Folder changed = org.obm.sync.book.Folder.builder()
			.name("changed name")
			.uid(5)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();
		org.obm.sync.book.Folder newer = org.obm.sync.book.Folder.builder()
			.name("yet another name")
			.uid(980)
			.ownerLoginAtDomain(user.getLoginAtDomain()).build();
		
		Set<org.obm.sync.book.Folder> changes = ImmutableSet.of(same, changed, newer);
		Set<org.obm.sync.book.Folder> deletions = ImmutableSet.of();
		FolderChanges folderChanges = new FolderChanges(changes, deletions, now);
		expect(bookClient.listAddressBooksChanged(accessToken, epoch)).andReturn(folderChanges);

		Folder knownSnapshotFolder = Folder.builder()
			.displayName("not anymore folder")
			.backendId("8")
			.collectionId(CollectionId.of(2))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		Folder sameSnapshotFolder = Folder.builder()
			.displayName(contactDisplayName)
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		Folder priorChangeSnapshotFolder = Folder.builder()
			.displayName("previous name")
			.backendId("5")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.of(sameSnapshotFolder.getBackendId()))
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		Folder changedSnapshotFolder = Folder.builder()
			.displayName("changed name")
			.backendId("5")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.of(sameSnapshotFolder.getBackendId()))
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
 
		Folder newSnapshotFolder = Folder.builder()
			.displayName("yet another name")
			.backendId("980")
			.collectionId(CollectionId.of(5))
			.parentBackendId(Optional.of(sameSnapshotFolder.getBackendId()))
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(5)
			.folders(ImmutableSet.of(knownSnapshotFolder, sameSnapshotFolder, priorChangeSnapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(6)
			.folders(ImmutableSet.of(newSnapshotFolder, sameSnapshotFolder, changedSnapshotFolder));
		expect(folderSnapshotDao.get(user, device, PIMDataType.CONTACTS, incomingSK)).andReturn(knownSnapshot);
		folderSnapshotDao.create(user, device, PIMDataType.CONTACTS, outgoingSK, newSnapshot);
		expectLastCall();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getFolders(udr, incomingSK, outgoingSK);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).containsOnly(
			CollectionDeletion.builder().collectionId(knownSnapshotFolder.getCollectionId()).build());
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(
			CollectionChange.builder()
				.collectionId(newSnapshotFolder.getCollectionId())
				.parentCollectionId(sameSnapshotFolder.getCollectionId())
				.displayName(newSnapshotFolder.getDisplayName())
				.folderType(newSnapshotFolder.getFolderType())
				.isNew(true).build(),
			CollectionChange.builder()
				.collectionId(changedSnapshotFolder.getCollectionId())
				.parentCollectionId(sameSnapshotFolder.getCollectionId())
				.displayName(changedSnapshotFolder.getDisplayName())
				.folderType(changedSnapshotFolder.getFolderType())
				.isNew(false).build()
			);
	}
}
