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
import org.obm.push.backend.CollectionPath.Builder;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.resource.OpushResourcesHolder;
import org.obm.push.service.ClientIdService;
import org.obm.push.service.DateService;
import org.obm.push.service.impl.MappingService;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.WindowingDao;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.book.Folder;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.items.FolderChanges;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;


public class ContactsBackendHierarchyChangesTest {

	private static final String COLLECTION_CONTACT_PREFIX = "obm:\\\\test@test\\contacts\\";
	
	private User user;
	private Device device;
	private UserDataRequest userDataRequest;
	private AccessToken accessToken;
	private String contactParentName;
	private CollectionId contactParentId;

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

	@Before
	public void setUp() {
		user = Factory.create().createUser("test@test", "test@domain", "displayName");
		device = new Device.Factory().create(null, "iPhone", "iOs 5", new DeviceId("my phone"), null);
		userDataRequest = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		accessToken = new AccessToken(0, "OBM");
		httpClient = HttpClientBuilder.create().build();
		
		mocks = createControl();
		opushResourcesHolder = mocks.createMock(OpushResourcesHolder.class);
		expect(opushResourcesHolder.getAccessToken()).andReturn(accessToken).anyTimes();
		expect(opushResourcesHolder.getHttpClient()).andReturn(httpClient).anyTimes();
		contactParentId = CollectionId.ROOT;
		contactParentName = "contacts";

		mappingService = mocks.createMock(MappingService.class);
		bookClient = mocks.createMock(BookClient.class);
		bookClientFactory = mocks.createMock(BookClient.Factory.class);
		expect(bookClientFactory.create(anyObject(HttpClient.class)))
			.andReturn(bookClient).anyTimes();
		contactConfiguration = publicContactConfiguration();
		collectionPathBuilderProvider = mocks.createMock(Provider.class);
		windowingDao = mocks.createMock(WindowingDao.class);
		clientIdService = mocks.createMock(ClientIdService.class);
		dateService = mocks.createMock(DateService.class);
		contactConverter = new ContactConverter();
		
		contactsBackend = new ContactsBackend(mappingService, 
				bookClientFactory, 
				contactConfiguration, 
				collectionPathBuilderProvider,
				windowingDao,
				clientIdService,
				contactConverter,
				dateService,
				opushResourcesHolder);
	}

	@After
	public void teardown() throws IOException {
		httpClient.close();
	}
	
	private ContactConfiguration publicContactConfiguration() {
		return new ContactConfiguration() {
			@Override
			public String getDefaultAddressBookName() {
				return super.getDefaultAddressBookName();
			}
		};
	}

	@Test
	public void testDefaultContactChanges() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("1234567890a"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("1234567890b"))
				.build();
		
		Date lastSyncDate = DateUtils.date("2012-12-15T20:30:45Z");
		Folder change = Folder.builder().name(contactParentName).uid(contactParentId.asInt()).ownerLoginAtDomain(user.getLoginAtDomain()).build();
		expectBookClientListBooksChanged(lastSyncDate, ImmutableSet.of(change), ImmutableSet.<Folder>of());
		
		List<CollectionPath> knownCollections = ImmutableList.of(); 
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, knownCollections);
		expectMappingServiceSearchThenCreateCollection(contactParentName, contactParentId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(contactParentId));
		expectMappingServiceLookupCollection(contactParentName, contactParentId);
		
		expectBuildCollectionPath(contactParentName, contactParentId);

		mocks.replay();
		
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		
		mocks.verify();
		
		CollectionChange expectedItemChange = CollectionChange.builder()
				.collectionId(contactParentId)
				.parentCollectionId(CollectionId.ROOT)
				.displayName(contactParentName)
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.isNew(true)
				.build();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).hasSize(1);
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(expectedItemChange);
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void testNoContactsChanges() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("1234567890a"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("1234567890b"))
				.build();

		Date lastSyncDate = DateUtils.date("2012-12-15T20:30:45Z");
		expectBookClientListBooksChanged(lastSyncDate, ImmutableSet.<Folder>of(), ImmutableSet.<Folder>of());

		List<CollectionPath> knownCollections = ImmutableList.<CollectionPath>of(
				new ContactCollectionPath(contactParentName, contactParentId)); 
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, knownCollections);
		expectMappingServiceFindCollection(contactParentName, contactParentId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(contactParentId));
		
		mocks.replay();
		
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void testOnlyChanges() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("1234567890a"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("1234567890b"))
				.build();
		
		CollectionId otherCollectionMappingId = CollectionId.of(203);
		String otherCollectionDisplayName = "no default address book";
		
		Date lastSyncDate = DateUtils.date("2012-12-15T20:30:45Z");
		Folder change = Folder.builder().name(otherCollectionDisplayName).uid(otherCollectionMappingId.asInt()).ownerLoginAtDomain(user.getLoginAtDomain()).build();
		expectBookClientListBooksChanged(lastSyncDate, ImmutableSet.of(change), ImmutableSet.<Folder>of());

		List<CollectionPath> knownCollections = ImmutableList.<CollectionPath>of(
				new ContactCollectionPath(contactParentName, contactParentId)); 
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, knownCollections);
		expectMappingServiceFindCollection(contactParentName, contactParentId);
		expectMappingServiceSearchThenCreateCollection(otherCollectionDisplayName, otherCollectionMappingId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(contactParentId, otherCollectionMappingId));
		expectMappingServiceLookupCollection(otherCollectionDisplayName, otherCollectionMappingId);

		expectBuildCollectionPath(otherCollectionDisplayName, otherCollectionMappingId);
		
		mocks.replay();
		
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		
		mocks.verify();

		CollectionChange expectedItemChange = CollectionChange.builder()
				.collectionId(otherCollectionMappingId)
				.parentCollectionId(contactParentId)
				.displayName(otherCollectionDisplayName)
				.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
				.isNew(true)
				.build();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).hasSize(1);
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(expectedItemChange);
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void testOnlyDeletions() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("1234567890a"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("1234567890b"))
				.build();
		
		CollectionId otherCollectionMappingId = CollectionId.of(203);
		String otherCollectionDisplayName = "no default address book";

		Date lastSyncDate = DateUtils.date("2012-12-15T20:30:45Z");
		Folder contactDeletion = Folder.builder().name(contactParentName).uid(contactParentId.asInt()).ownerLoginAtDomain(user.getLoginAtDomain()).build();
		Folder otherCollectionDeletion = Folder.builder().name(otherCollectionDisplayName).uid(otherCollectionMappingId.asInt()).ownerLoginAtDomain(user.getLoginAtDomain()).build();
		expectBookClientListBooksChanged(lastSyncDate, ImmutableSet.<Folder>of(), ImmutableSet.of(contactDeletion, otherCollectionDeletion));

		List<CollectionPath> knownCollections = ImmutableList.<CollectionPath>of(
				new ContactCollectionPath(contactParentName, contactParentId),
				new ContactCollectionPath(otherCollectionDisplayName, otherCollectionMappingId)); 
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, knownCollections);
		expectMappingServiceLookupCollection(contactParentName, contactParentId);
		expectMappingServiceLookupCollection(otherCollectionDisplayName, otherCollectionMappingId);

		expectBuildCollectionPath(contactParentName, contactParentId);
		expectBuildCollectionPath(otherCollectionDisplayName, otherCollectionMappingId);
		
		mocks.replay();
		
		HierarchyCollectionChanges hierarchyItemsChanges = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		
		mocks.verify();

		CollectionDeletion expectedItemDeletion = CollectionDeletion.builder()
				.collectionId(contactParentId)
				.build();
		CollectionDeletion expectedItem2Deletion = CollectionDeletion.builder()
				.collectionId(otherCollectionMappingId)
				.build();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).hasSize(2);
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).containsOnly(expectedItemDeletion, expectedItem2Deletion);
	}
	
	@Test
	public void testSameAddAndDeleteDiscardDelete() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();
		CollectionId targetCollectionId = CollectionId.of(2);
			
		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of();
		Set<Folder> updated = ImmutableSet.of(
				Folder.builder().name("both").uid(targetCollectionId.asInt()).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		Set<Folder> removed = ImmutableSet.of(
				Folder.builder().name("both").uid(targetCollectionId.asInt()).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceSearchThenCreateCollection("both", targetCollectionId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(targetCollectionId));
		expectMappingServiceLookupCollection("both", targetCollectionId);

		expectBuildCollectionPath("both", targetCollectionId);
		expectBuildCollectionPath("both", targetCollectionId);
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).containsOnly(CollectionChange.builder()
				.collectionId(targetCollectionId)
				.parentCollectionId(contactParentId)
				.displayName("both")
				.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
				.isNew(true)
				.build());
		assertThat(changes.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void testSameLastKnownAndAdd() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();
		CollectionId targetCollectionId = CollectionId.of(2);
		
		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of(new ContactCollectionPath("both", targetCollectionId));
		Set<Folder> updated = ImmutableSet.of(
				Folder.builder().name("both").uid(targetCollectionId.asInt()).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		Set<Folder> removed = ImmutableSet.of();
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceFindCollection("both", targetCollectionId);
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(targetCollectionId));

		expectBuildCollectionPath("both", targetCollectionId);
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).isEmpty();
		assertThat(changes.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void testTwoSameLastKnownAndAdd() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();

		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of(
				new ContactCollectionPath("both", CollectionId.of(2)), new ContactCollectionPath("both2", CollectionId.of(3)));
		Set<Folder> updated = ImmutableSet.of(
				Folder.builder().name("both").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build(),
				Folder.builder().name("both2").uid(3).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		Set<Folder> removed = ImmutableSet.of();
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceFindCollection("both", CollectionId.of(2));
		expectMappingServiceFindCollection("both2", CollectionId.of(3));
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(CollectionId.of(2), CollectionId.of(3)));

		expectBuildCollectionPath("both", CollectionId.of(2));
		expectBuildCollectionPath("both2", CollectionId.of(3));
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).isEmpty();
		assertThat(changes.getCollectionDeletions()).isEmpty();
	}

	@Test
	public void testOneLastKnownInTwoAdd() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();

		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of(new ContactCollectionPath("known", CollectionId.of(2)));
		Set<Folder> updated = ImmutableSet.of(
				Folder.builder().name("known").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build(),
				Folder.builder().name("add").uid(3).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		Set<Folder> removed = ImmutableSet.of();
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceSearchThenCreateCollection("add", CollectionId.of(3));
		expectMappingServiceFindCollection("known", CollectionId.of(2));
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(CollectionId.of(2), CollectionId.of(3)));
		expectMappingServiceLookupCollection("add", CollectionId.of(3));

		expectBuildCollectionPath("add", CollectionId.of(3));
		expectBuildCollectionPath("known", CollectionId.of(2));
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).containsOnly(
				CollectionChange.builder()
					.collectionId(CollectionId.of("3"))
					.parentCollectionId(contactParentId)
					.displayName("add")
					.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
					.isNew(true)
					.build());
		assertThat(changes.getCollectionDeletions()).isEmpty();
	}

	@Test
	public void testOneAddOneKnownDelete() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();

		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of(new ContactCollectionPath("known", CollectionId.of(2)));
		Set<Folder> updated = ImmutableSet.of(
				Folder.builder().name("add").uid(3).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		Set<Folder> removed = ImmutableSet.of(
				Folder.builder().name("known").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceSearchThenCreateCollection("add", CollectionId.of(3));
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(CollectionId.of(3)));
		expectMappingServiceLookupCollection("add", CollectionId.of(3));
		expectMappingServiceLookupCollection("known", CollectionId.of(2));

		expectBuildCollectionPath("add", CollectionId.of(3));
		expectBuildCollectionPath("known", CollectionId.of(2));
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).containsOnly(
				CollectionChange.builder()
					.collectionId(CollectionId.of("3"))
					.parentCollectionId(contactParentId)
					.displayName("add")
					.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
					.isNew(true)
					.build());
		assertThat(changes.getCollectionDeletions()).containsOnly(
				CollectionDeletion.builder().collectionId(CollectionId.of("2")).build());
	}
	
	@Test
	public void testOneAddOneUnknownDelete() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();

		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of();
		Set<Folder> updated = ImmutableSet.of(
				Folder.builder().name("add").uid(3).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		Set<Folder> removed = ImmutableSet.of(
				Folder.builder().name("unknown").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceSearchThenCreateCollection("add", CollectionId.of(3));
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(CollectionId.of(3)));
		expectMappingServiceLookupCollection("add", CollectionId.of(3));

		expectBuildCollectionPath("add", CollectionId.of(3));
		expectBuildCollectionPath("unknown", CollectionId.of(2));
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).containsOnly(
				CollectionChange.builder()
					.collectionId(CollectionId.of("3"))
					.parentCollectionId(contactParentId)
					.displayName("add")
					.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
					.isNew(true)
					.build());
		assertThat(changes.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void testTwoAddWithSameNamesAndDifferentUidsKeepBoth() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();

		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of();
		Set<Folder> updated = ImmutableSet.of(
				Folder.builder().name("both").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build(),
				Folder.builder().name("both").uid(3).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		Set<Folder> removed = ImmutableSet.of();
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceSearchThenCreateCollection("both", CollectionId.of(2));
		expectMappingServiceSearchThenCreateCollection("both", CollectionId.of(3));
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(CollectionId.of(2), CollectionId.of(3)));
		expectMappingServiceLookupCollection("both", CollectionId.of(2));
		expectMappingServiceLookupCollection("both", CollectionId.of(3));

		expectBuildCollectionPath("both", CollectionId.of(3));
		expectBuildCollectionPath("both", CollectionId.of(2));
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).containsOnly(
				CollectionChange.builder()
					.collectionId(CollectionId.of("2"))
					.parentCollectionId(contactParentId)
					.displayName("both")
					.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
					.isNew(true)
					.build(),
				CollectionChange.builder()
					.collectionId(CollectionId.of("3"))
					.parentCollectionId(contactParentId)
					.displayName("both")
					.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
					.isNew(true)
					.build());
		assertThat(changes.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void testTwoAddWithSameNamesAndSameUidsDiscardsOne() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();

		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of();
		Set<Folder> updated = ImmutableSet.of(
				Folder.builder().name("both").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build(),
				Folder.builder().name("both").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		Set<Folder> removed = ImmutableSet.of();
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceSearchThenCreateCollection("both", CollectionId.of(2));
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(CollectionId.of(2)));
		expectMappingServiceLookupCollection("both", CollectionId.of(2));

		expectBuildCollectionPath("both", CollectionId.of(2));
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).containsOnly(
				CollectionChange.builder()
					.collectionId(CollectionId.of("2"))
					.parentCollectionId(contactParentId)
					.displayName("both")
					.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
					.isNew(true)
					.build());
		assertThat(changes.getCollectionDeletions()).isEmpty();
	}
	
	@Test
	public void testTwoDeleteWithSameNamesAndDifferentUidsKeepBoth() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();

		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of(
				new ContactCollectionPath("both", CollectionId.of(2)),
				new ContactCollectionPath("both", CollectionId.of(3)));
		
		Set<Folder> updated = ImmutableSet.of();
		Set<Folder> removed = ImmutableSet.of(
				Folder.builder().name("both").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build(),
				Folder.builder().name("both").uid(3).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceLookupCollection("both", CollectionId.of(2));
		expectMappingServiceLookupCollection("both", CollectionId.of(3));
		
		expectBuildCollectionPath("both", CollectionId.of(2));
		expectBuildCollectionPath("both", CollectionId.of(3));
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).isEmpty();
		assertThat(changes.getCollectionDeletions()).containsOnly(
				CollectionDeletion.builder().collectionId(CollectionId.of("2")).build(),
				CollectionDeletion.builder().collectionId(CollectionId.of("3")).build());
	}
	
	@Test
	public void testTwoDeleteWithSameNamesAndSameUidsDiscardsOne() throws Exception {
		FolderSyncState lastKnownState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key1"))
				.build();
		FolderSyncState outgoingSyncState = FolderSyncState.builder()
				.syncKey(new FolderSyncKey("key2"))
				.build();

		List<CollectionPath> lastKnown = ImmutableList.<CollectionPath>of(
				new ContactCollectionPath("both", CollectionId.of(2)),
				new ContactCollectionPath("both", CollectionId.of(3)));
		
		Set<Folder> updated = ImmutableSet.of();
		Set<Folder> removed = ImmutableSet.of(
				Folder.builder().name("both").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build(),
				Folder.builder().name("both").uid(2).ownerLoginAtDomain(user.getLoginAtDomain()).build());
		
		Date lastSyncDate = org.obm.DateUtils.date("2012-05-04T11:02:03");
		expectBookClientListBooksChanged(lastSyncDate, updated, removed);
		expectMappingServiceListLastKnowCollection(lastKnownState, lastSyncDate, lastKnown);
		expectMappingServiceFindCollection("both", CollectionId.of(3));
		expectMappingServiceSnapshot(outgoingSyncState, ImmutableSet.of(CollectionId.of(3)));
		expectMappingServiceLookupCollection("both", CollectionId.of(2));
		
		expectBuildCollectionPath("both", CollectionId.of(2));
		
		mocks.replay();
		HierarchyCollectionChanges changes = contactsBackend.getHierarchyChanges(userDataRequest, lastKnownState, outgoingSyncState);
		mocks.verify();
		
		assertThat(changes.getCollectionChanges()).isEmpty();
		assertThat(changes.getCollectionDeletions()).containsOnly(
				CollectionDeletion.builder().collectionId(CollectionId.of("2")).build());
	}
	
	private Builder expectBuildCollectionPath(String displayName, CollectionId contactParentId) {
		CollectionPath collectionPath = new ContactCollectionPath(displayName, contactParentId);
		CollectionPath.Builder collectionPathBuilder = expectCollectionPathBuilder(collectionPath, displayName, contactParentId);
		expectCollectionPathBuilderPovider(collectionPathBuilder);
		return collectionPathBuilder;
	}

	private CollectionPath.Builder expectCollectionPathBuilder(CollectionPath collectionPath, 
			String displayName, CollectionId folderUid) {
		
		CollectionPath.Builder collectionPathBuilder = mocks.createMock(CollectionPath.Builder.class);
		expect(collectionPathBuilder.userDataRequest(userDataRequest))
			.andReturn(collectionPathBuilder).once();
		
		expect(collectionPathBuilder.pimType(PIMDataType.CONTACTS))
			.andReturn(collectionPathBuilder).once();

		expect(collectionPathBuilder.backendName(ContactCollectionPath.backendName(displayName, folderUid)))
			.andReturn(collectionPathBuilder).once();
		
		expect(collectionPathBuilder.build())
			.andReturn(collectionPath).once();
		
		return collectionPathBuilder;
	}

	private void expectBookClientListBooksChanged(Date syncDate,
			Set<Folder> changes, Set<Folder> deletions) throws ServerFault {
		expect(bookClient.listAddressBooksChanged(accessToken, syncDate))
			.andReturn(new FolderChanges(changes, deletions, syncDate)).once();
	}

	private void expectCollectionPathBuilderPovider(CollectionPath.Builder collectionPathBuilder) {
			expect(collectionPathBuilderProvider.get())
				.andReturn(collectionPathBuilder).once();
	}

	private void expectMappingServiceSnapshot(FolderSyncState outgoingSyncState, ImmutableSet<CollectionId> immutableSet)
			throws DaoException {

		mappingService.snapshotCollections(outgoingSyncState, immutableSet);
		expectLastCall();
	}

	private void expectMappingServiceListLastKnowCollection(FolderSyncState incomingSyncState, Date syncDate,
			List<CollectionPath> collectionPaths) throws DaoException {
		
		expect(mappingService.getLastBackendMapping(PIMDataType.CONTACTS, incomingSyncState))
			.andReturn(syncDate).once();
		
		expect(mappingService.listCollections(userDataRequest, incomingSyncState))
			.andReturn(collectionPaths).once();
	}

	private void expectMappingServiceFindCollection(String collectionName, CollectionId collectionId)
		throws CollectionNotFoundException, DaoException {
		
		String collectionPath = 
				COLLECTION_CONTACT_PREFIX + ContactCollectionPath.backendName(collectionName, collectionId);
		
		expect(mappingService.getCollectionIdFor(device, collectionPath))
			.andReturn(collectionId).once();
	}
	
	private void expectMappingServiceSearchThenCreateCollection(String collectionName, CollectionId contactParentId)
		throws CollectionNotFoundException, DaoException {

		String collectionPath = 
				COLLECTION_CONTACT_PREFIX + ContactCollectionPath.backendName(collectionName, contactParentId);
		
		expect(mappingService.getCollectionIdFor(device, collectionPath))
			.andThrow(new CollectionNotFoundException()).once();
		
		expect(mappingService.createCollectionMapping(device, collectionPath))
			.andReturn(contactParentId).once();
	}
	
	private void expectMappingServiceLookupCollection(String collectionName, CollectionId contactParentId)
		throws CollectionNotFoundException, DaoException {
		
		expectMappingServiceFindCollection(collectionName, contactParentId);
	}
	
	private static class ContactCollectionPath extends CollectionPath {

		public ContactCollectionPath(String displayName, CollectionId contactParentId) {
			super(String.format("%s%s", COLLECTION_CONTACT_PREFIX, backendName(displayName, contactParentId)),
					PIMDataType.CONTACTS, backendName(displayName, contactParentId));
		}
		
		public static String backendName(String displayName, CollectionId contactParentId) {
			return String.format("%d:%s", contactParentId.asInt(), displayName);
		}
	}
}
