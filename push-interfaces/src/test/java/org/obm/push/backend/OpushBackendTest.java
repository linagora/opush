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
package org.obm.push.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.backend.CollectionPath.Builder;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.FolderSnapshot.Folder;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.InvalidFolderSyncKeyException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.FolderSnapshotDao.FolderSnapshotNotFoundException;
import org.obm.push.service.impl.MappingService;
import org.obm.push.state.FolderSyncKey;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.inject.Provider;


public class OpushBackendTest {

	private IMocksControl mocks;
	private MappingService mappingService;
	private Provider<Builder> collectionPathBuilderProvider;
	private FolderSnapshotDao folderSnapshotDao;
	
	private User user;
	private Device device;
	private UserDataRequest udr;
	private PIMDataType type;

	@Before
	public void setUp() {
		mocks = createControl();
		mappingService = mocks.createMock(MappingService.class); 
		collectionPathBuilderProvider = mocks.createMock(Provider.class);
		folderSnapshotDao = mocks.createMock(FolderSnapshotDao.class);

		type = PIMDataType.CALENDAR;
		user = Factory.create().createUser("test@test", "test@domain", "displayName");
		device = new Device.Factory().create(null, "iPhone", "iOs 5", new DeviceId("my phone"), null);
		udr = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
	}

	private OpushBackend testee() {
		return new OpushBackend(mappingService, collectionPathBuilderProvider, folderSnapshotDao) {


			@Override
			protected CollectionChange createCollectionChange(UserDataRequest udr, OpushCollection collection)
					throws DaoException, CollectionNotFoundException {
				throw new NotImplementedException();
			}

			@Override
			protected CollectionDeletion createCollectionDeletion(UserDataRequest udr, CollectionPath collectionPath)
					throws DaoException, CollectionNotFoundException {
				throw new NotImplementedException();
			}

			@Override
			public PIMDataType getPIMDataType() {
				return type;
			}

			@Override
			protected BackendFolders currentFolders(UserDataRequest udr) {
				throw new NotImplementedException();
			}
			
		};
	}

	@Test
	public void snapshotShouldBeEmptyWhenNoFolder() {
		FolderSyncKey outgoingSyncKey = new FolderSyncKey("1234");
		BackendFolders currentFolders = testBackendFolders();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.<Folder>of());
		FolderSnapshot expectedSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.<Folder>of());
		
		folderSnapshotDao.create(user, device, type, outgoingSyncKey, expectedSnapshot);
		expectLastCall();
		
		mocks.replay();
		FolderSnapshot snapshot = testee().snapshot(udr, outgoingSyncKey, knownSnapshot, currentFolders);
		mocks.verify();

		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}

	@Test
	public void snapshotShouldContainsOneFolderAndIncrementNextIdWhenOneNewFolder() {
		FolderSyncKey outgoingSyncKey = new FolderSyncKey("1234");
		Folder newFolder = Folder.builder()
			.displayName("name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(2))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER).build();

		BackendFolders currentFolders = testBackendFolders(
			BackendFolder.builder()
				.displayName("name")
				.backendId(CollectionId.of(12))
				.parentId(Optional.<CollectionId>absent())
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.<Folder>of());
		FolderSnapshot expectedSnapshot = FolderSnapshot.nextId(3).folders(ImmutableSet.of(newFolder));
		
		folderSnapshotDao.create(user, device, type, outgoingSyncKey, expectedSnapshot);
		expectLastCall();
		
		mocks.replay();
		FolderSnapshot snapshot = testee().snapshot(udr, outgoingSyncKey, knownSnapshot, currentFolders);
		mocks.verify();

		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}

	@Test
	public void snapshotShouldContainsOneFolderAndSameNextIdWhenOneRemovedFolder() {
		FolderSyncKey outgoingSyncKey = new FolderSyncKey("1234");
		
		Folder knownFolder = Folder.builder()
			.displayName("known")
			.backendId(CollectionId.of(8))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.USER_CREATED_CALENDAR_FOLDER).build();
		Folder sameFolder = Folder.builder()
			.displayName("name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(2))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER).build();

		BackendFolders currentFolders = testBackendFolders(
			BackendFolder.builder()
				.displayName("name")
				.backendId(CollectionId.of(12))
				.parentId(Optional.<CollectionId>absent())
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(sameFolder, knownFolder));
		FolderSnapshot expectedSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(sameFolder));
		
		folderSnapshotDao.create(user, device, type, outgoingSyncKey, expectedSnapshot);
		expectLastCall();
		
		mocks.replay();
		FolderSnapshot snapshot = testee().snapshot(udr, outgoingSyncKey, knownSnapshot, currentFolders);
		mocks.verify();

		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}

	@Test
	public void snapshotShouldContainsOneFolderAndSameNextIdWhenOneChangedFolder() {
		FolderSyncKey outgoingSyncKey = new FolderSyncKey("1234");
		Folder knownFolder = Folder.builder()
			.displayName("known")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.USER_CREATED_CALENDAR_FOLDER).build();
		Folder expectedFolder = Folder.builder()
			.displayName("changed name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER).build();

		BackendFolders currentFolders = testBackendFolders(
			BackendFolder.builder()
				.displayName("changed name")
				.backendId(CollectionId.of(12))
				.parentId(Optional.<CollectionId>absent())
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownFolder));
		FolderSnapshot expectedSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(expectedFolder));
		
		folderSnapshotDao.create(user, device, type, outgoingSyncKey, expectedSnapshot);
		expectLastCall();
		
		mocks.replay();
		FolderSnapshot snapshot = testee().snapshot(udr, outgoingSyncKey, knownSnapshot, currentFolders);
		mocks.verify();

		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}

	@Test
	public void snapshotShouldContainsOneFolderAndSameNextIdWhenOneSameFolder() {
		FolderSyncKey outgoingSyncKey = new FolderSyncKey("1234");
		Folder knownFolder = Folder.builder()
			.displayName("name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER).build();

		BackendFolders currentFolders = testBackendFolders(
			BackendFolder.builder()
				.backendId(CollectionId.of(12))
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.parentId(Optional.<CollectionId>absent())
				.build());
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownFolder));
		FolderSnapshot expectedSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownFolder));
		
		folderSnapshotDao.create(user, device, type, outgoingSyncKey, expectedSnapshot);
		expectLastCall();
		
		mocks.replay();
		FolderSnapshot snapshot = testee().snapshot(udr, outgoingSyncKey, knownSnapshot, currentFolders);
		mocks.verify();

		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}
	
	private BackendFolders testBackendFolders(BackendFolder...folders) {
		final UnmodifiableIterator<BackendFolder> iterator = Iterators.forArray(folders);
		return new BackendFolders() {

			@Override
			public Iterator<BackendFolder> iterator() {
				return iterator;
			}
		};
	}

	@Test
	public void findFolderSnapshotShouldReturnEmptyWhenInitialSyncKey() {
		FolderSyncKey key = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		
		mocks.replay();
		assertThat(testee().findFolderSnapshot(udr, key)).isEqualTo(FolderSnapshot.empty());
		mocks.verify();
	}
	
	@Test
	public void findFolderSnapshotShouldReturnFromDaoWhenFound() throws Exception {
		FolderSyncKey key = new FolderSyncKey("1234");
		FolderSnapshot snapshot = FolderSnapshot.nextId(34).folders(ImmutableSet.<Folder>of());
		expect(folderSnapshotDao.get(user, device, PIMDataType.CALENDAR, key)).andReturn(snapshot);
		
		mocks.replay();
		assertThat(testee().findFolderSnapshot(udr, key)).isEqualTo(snapshot);
		mocks.verify();
	}
	
	@Test(expected=InvalidFolderSyncKeyException.class)
	public void findFolderSnapshotShouldTriggerInvalidSyncKeyWhenNotFound() throws Exception {
		FolderSyncKey key = new FolderSyncKey("1234");
		expect(folderSnapshotDao.get(user, device, PIMDataType.CALENDAR, key)).andThrow(new FolderSnapshotNotFoundException());
		
		mocks.replay();
		try {
			testee().findFolderSnapshot(udr, key);
		} finally {
			mocks.verify();
		}
	}
	
	@Test(expected=DaoException.class)
	public void findFolderSnapshotShouldPropagateWhenOtherException() throws Exception {
		FolderSyncKey key = new FolderSyncKey("1234");
		expect(folderSnapshotDao.get(user, device, PIMDataType.CALENDAR, key)).andThrow(new DaoException("message"));
		
		mocks.replay();
		try {
			testee().findFolderSnapshot(udr, key);
		} finally {
			mocks.verify();
		}
	}

	@Test(expected=NullPointerException.class)
	public void folderToCollectionDeletionShouldNPEWhenNullFolders() {
		Map<CollectionId, Folder> folders = null;
		CollectionId collection = CollectionId.of(4);
		
		mocks.replay();
		try {
			testee().folderToCollectionDeletion(folders).apply(collection);
		} finally {
			mocks.verify();
		}
	}

	@Test(expected=NullPointerException.class)
	public void folderToCollectionDeletionShouldNPEWhenNullCollectionId() {
		Map<CollectionId, Folder> folders = ImmutableMap.of();
		CollectionId collection = null;
		
		mocks.replay();
		try {
			testee().folderToCollectionDeletion(folders).apply(collection);
		} finally {
			mocks.verify();
		}
	}

	@Test(expected=NullPointerException.class)
	public void folderToCollectionDeletionShouldNPEWhenFolderNotFound() {
		Map<CollectionId, Folder> folders = ImmutableMap.of();
		CollectionId collection = CollectionId.of(5);
		
		mocks.replay();
		try {
			testee().folderToCollectionDeletion(folders).apply(collection);
		} finally {
			mocks.verify();
		}
	}
	
	@Test
	public void folderToCollectionDeletionShouldSucceedWhenFolderFound() {
		Map<CollectionId, Folder> folders = ImmutableMap.of(
			CollectionId.of(5), Folder.builder()
				.backendId(CollectionId.of(5))
				.collectionId(CollectionId.of(2))
				.parentBackendId(CollectionId.of(12))
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		CollectionId collection = CollectionId.of(5);
		
		mocks.replay();
		assertThat(testee().folderToCollectionDeletion(folders).apply(collection))
			.isEqualTo(CollectionDeletion.builder().collectionId(CollectionId.of(2)).build());
		mocks.verify();
	}

	@Test(expected=NullPointerException.class)
	public void folderToCollectionCreationShouldNPEWhenNullFolders() {
		Map<CollectionId, Folder> folders = null;
		CollectionId collection = CollectionId.of(4);
		
		mocks.replay();
		try {
			testee().folderToCollectionCreation(folders).apply(collection);
		} finally {
			mocks.verify();
		}
	}

	@Test(expected=NullPointerException.class)
	public void folderToCollectionCreationShouldNPEWhenNullCollectionId() {
		Map<CollectionId, Folder> folders = ImmutableMap.of();
		CollectionId collection = null;
		
		mocks.replay();
		try {
			testee().folderToCollectionCreation(folders).apply(collection);
		} finally {
			mocks.verify();
		}
	}

	@Test(expected=NullPointerException.class)
	public void folderToCollectionCreationShouldNPEWhenFolderNotFound() {
		Map<CollectionId, Folder> folders = ImmutableMap.of();
		CollectionId collection = CollectionId.of(5);
		
		mocks.replay();
		try {
			testee().folderToCollectionCreation(folders).apply(collection);
		} finally {
			mocks.verify();
		}
	}
	
	@Test
	public void folderToCollectionCreationShouldSucceedWhenFolderFoundAndNoParent() {
		Map<CollectionId, Folder> folders = ImmutableMap.of(
			CollectionId.of(5), Folder.builder()
				.backendId(CollectionId.of(5))
				.collectionId(CollectionId.of(2))
				.parentBackendId(CollectionId.ROOT)
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		CollectionId collection = CollectionId.of(5);
		
		mocks.replay();
		assertThat(testee().folderToCollectionCreation(folders).apply(collection))
			.isEqualTo(CollectionChange.builder()
				.isNew(true)
				.collectionId(CollectionId.of(2))
				.parentCollectionId(CollectionId.ROOT)
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		mocks.verify();
	}
	@Test
	public void folderToCollectionCreationShouldSucceedWhenFolderFoundAndParentNotFound() {
		Map<CollectionId, Folder> folders = ImmutableMap.of(
			CollectionId.of(5), Folder.builder()
				.backendId(CollectionId.of(5))
				.collectionId(CollectionId.of(2))
				.parentBackendId(CollectionId.of(12))
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		CollectionId collection = CollectionId.of(5);
		
		mocks.replay();
		assertThat(testee().folderToCollectionCreation(folders).apply(collection))
			.isEqualTo(CollectionChange.builder()
				.isNew(true)
				.collectionId(CollectionId.of(2))
				.parentCollectionId(CollectionId.ROOT)
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		mocks.verify();
	}
	
	@Test
	public void folderToCollectionCreationShouldSucceedWhenFolderFoundAndParent() {
		Map<CollectionId, Folder> folders = ImmutableMap.of(
			CollectionId.of(5), Folder.builder()
				.backendId(CollectionId.of(5))
				.collectionId(CollectionId.of(2))
				.parentBackendId(CollectionId.of(12))
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build(),
			CollectionId.of(12), Folder.builder()
				.backendId(CollectionId.of(12))
				.collectionId(CollectionId.of(4))
				.parentBackendId(CollectionId.ROOT)
				.displayName("the parent")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		CollectionId collection = CollectionId.of(5);
		
		mocks.replay();
		assertThat(testee().folderToCollectionCreation(folders).apply(collection))
			.isEqualTo(CollectionChange.builder()
				.isNew(true)
				.collectionId(CollectionId.of(2))
				.parentCollectionId(CollectionId.of(4))
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		mocks.verify();
	}

	@Test(expected=NullPointerException.class)
	public void folderToCollectionChangeShouldNPEWhenNullFolders() {
		Map<CollectionId, Folder> folders = null;
		Folder folder = Folder.builder()
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(4))
			.parentBackendId(CollectionId.ROOT)
			.displayName("the parent")
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
			.build();
		
		mocks.replay();
		try {
			testee().folderToCollectionChange(folders).apply(folder);
		} finally {
			mocks.verify();
		}
	}

	@Test(expected=NullPointerException.class)
	public void folderToCollectionChangeShouldNPEWhenNullFolder() {
		Map<CollectionId, Folder> folders = ImmutableMap.of();
		Folder folder = null;
		
		mocks.replay();
		try {
			testee().folderToCollectionChange(folders).apply(folder);
		} finally {
			mocks.verify();
		}
	}
	
	@Test
	public void folderToCollectionChangeShouldSucceedWhenNoParent() {
		Folder folder = Folder.builder()
			.backendId(CollectionId.of(5))
			.collectionId(CollectionId.of(4))
			.parentBackendId(CollectionId.ROOT)
			.displayName("name")
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
			.build();
		
		Map<CollectionId, Folder> folders = ImmutableMap.of(CollectionId.of(5), folder);
		
		mocks.replay();
		assertThat(testee().folderToCollectionChange(folders).apply(folder))
			.isEqualTo(CollectionChange.builder()
				.isNew(false)
				.collectionId(CollectionId.of(4))
				.parentCollectionId(CollectionId.ROOT)
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		mocks.verify();
	}
	
	@Test
	public void folderToCollectionChangeShouldSucceedWhenParentNotFound() {
		Folder folder = Folder.builder()
			.backendId(CollectionId.of(5))
			.collectionId(CollectionId.of(4))
			.parentBackendId(CollectionId.of(12))
			.displayName("name")
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
			.build();
		
		Map<CollectionId, Folder> folders = ImmutableMap.of(CollectionId.of(5), folder);
		
		mocks.replay();
		assertThat(testee().folderToCollectionChange(folders).apply(folder))
			.isEqualTo(CollectionChange.builder()
				.isNew(false)
				.collectionId(CollectionId.of(4))
				.parentCollectionId(CollectionId.ROOT)
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		mocks.verify();
	}
	
	@Test
	public void folderToCollectionChangeShouldSucceedWhenParentFound() {
		Folder folder = Folder.builder()
			.backendId(CollectionId.of(5))
			.collectionId(CollectionId.of(4))
			.parentBackendId(CollectionId.of(12))
			.displayName("name")
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
			.build();
			
		Folder parentFolder = Folder.builder()
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(8))
			.parentBackendId(CollectionId.ROOT)
			.displayName("the parent")
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
			.build();
		
		Map<CollectionId, Folder> folders = ImmutableMap.of(
			CollectionId.of(5), folder,
			CollectionId.of(12), parentFolder);
		
		mocks.replay();
		assertThat(testee().folderToCollectionChange(folders).apply(folder))
			.isEqualTo(CollectionChange.builder()
				.isNew(false)
				.collectionId(CollectionId.of(4))
				.parentCollectionId(CollectionId.of(8))
				.displayName("name")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.build());
		mocks.verify();
	}

	@Test
	public void buildDiffShouldReturnEmptyWhenBothEmpty() throws Exception {
		FolderSnapshot knownSnapshot = FolderSnapshot.empty();
		FolderSnapshot newSnapshot = FolderSnapshot.empty();

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = testee().buildDiff(knownSnapshot, newSnapshot);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
	}

	@Test
	public void buildDiffShouldReturnEmptyWhenBothHaveSameFolder() throws Exception {
		Folder knownSnapshotFolder = Folder.builder()
			.displayName("name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = testee().buildDiff(knownSnapshot, newSnapshot);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
	}

	@Test
	public void buildDiffShouldReturnAddWhenEmptyKnownSnapshotAndNewFolder() throws Exception {
		Folder newSnapshotFolder = Folder.builder()
			.displayName("name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.empty();
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(newSnapshotFolder));

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = testee().buildDiff(knownSnapshot, newSnapshot);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(CollectionChange.builder()
			.collectionId(newSnapshotFolder.getCollectionId())
			.parentCollectionId(newSnapshotFolder.getParentBackendId())
			.displayName(newSnapshotFolder.getDisplayName())
			.folderType(newSnapshotFolder.getFolderType())
			.isNew(true).build());
	}

	@Test
	public void buildDiffShouldReturnAddWhenKnownSnapshotAndNewFolder() throws Exception {
		Folder knownSnapshotFolder = Folder.builder()
			.displayName("name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		Folder addSnapshotFolder = Folder.builder()
			.displayName("another name")
			.backendId(CollectionId.of(15))
			.collectionId(CollectionId.of(2))
			.parentBackendId(knownSnapshotFolder.getBackendId())
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(3).folders(ImmutableSet.of(knownSnapshotFolder, addSnapshotFolder));

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = testee().buildDiff(knownSnapshot, newSnapshot);
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
	public void buildDiffShouldReturnChangeWhenKnownFolderButChanged() throws Exception {
		Folder knownSnapshotFolder = Folder.builder()
			.displayName("name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();

		Folder newSnapshotFolder = Folder.builder()
			.displayName("another display name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(newSnapshotFolder));

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = testee().buildDiff(knownSnapshot, newSnapshot);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionChanges()).containsOnly(CollectionChange.builder()
			.collectionId(newSnapshotFolder.getCollectionId())
			.parentCollectionId(newSnapshotFolder.getParentBackendId())
			.displayName(newSnapshotFolder.getDisplayName())
			.folderType(newSnapshotFolder.getFolderType())
			.isNew(false).build());
	}
	
	@Test
	public void buildDiffShouldReturnDeletionWhenKnownFolderNotInNewSnapshot() throws Exception {
		Folder knownSnapshotFolder = Folder.builder()
			.displayName("name")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(knownSnapshotFolder));
		FolderSnapshot newSnapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.<Folder>of());

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = testee().buildDiff(knownSnapshot, newSnapshot);
		mocks.verify();
		
		assertThat(hierarchyItemsChanges.getCollectionChanges()).isEmpty();
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).containsOnly(
			CollectionDeletion.builder().collectionId(knownSnapshotFolder.getCollectionId()).build());
	}
	
	@Test
	public void buildDiffWhenComplexe() throws Exception {
		Folder knownSnapshotFolder = Folder.builder()
			.displayName("not anymore folder")
			.backendId(CollectionId.of(8))
			.collectionId(CollectionId.of(2))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		Folder sameSnapshotFolder = Folder.builder()
			.displayName("unchanged folder")
			.backendId(CollectionId.of(12))
			.collectionId(CollectionId.of(1))
			.parentBackendId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		Folder priorChangeSnapshotFolder = Folder.builder()
			.displayName("previous name")
			.backendId(CollectionId.of(5))
			.collectionId(CollectionId.of(3))
			.parentBackendId(sameSnapshotFolder.getBackendId())
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		Folder changedSnapshotFolder = Folder.builder()
			.displayName("changed name")
			.backendId(CollectionId.of(5))
			.collectionId(CollectionId.of(3))
			.parentBackendId(sameSnapshotFolder.getBackendId())
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
 
		Folder newSnapshotFolder = Folder.builder()
			.displayName("yet another name")
			.backendId(CollectionId.of(980))
			.collectionId(CollectionId.of(5))
			.parentBackendId(sameSnapshotFolder.getBackendId())
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot knownSnapshot = FolderSnapshot.nextId(5)
			.folders(ImmutableSet.of(knownSnapshotFolder, sameSnapshotFolder, priorChangeSnapshotFolder));
		FolderSnapshot currentSnapshot = FolderSnapshot.nextId(6)
			.folders(ImmutableSet.of(newSnapshotFolder, sameSnapshotFolder, changedSnapshotFolder));

		mocks.replay();
		HierarchyCollectionChanges hierarchyItemsChanges = testee().buildDiff(knownSnapshot, currentSnapshot);
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
	
	@Test
	public void testOneAddedCollectionWhenNoLastKnown() {
		Set<CollectionPath> lastKnownCollections = ImmutableSet.of();
		
		OpushCollection collection = OpushCollection.builder()
				.collectionPath(new CollectionPathTest(PIMDataType.EMAIL, "name"))
				.displayName("display name")
				.build();
		PathsToCollections changedCollections = PathsToCollections.builder()
				.put(collection.collectionPath(), collection)
				.build();

		mocks.replay();
		Iterable<OpushCollection> addedCollections = testee().addedCollections(lastKnownCollections, changedCollections);
		mocks.verify();
		
		assertThat(addedCollections).containsOnly(collection);
	}
	
	@Test
	public void testTwoAddedCollectionWhenNoLastKnown() {
		Set<CollectionPath> lastKnownCollections = ImmutableSet.of();
		
		OpushCollection collection = OpushCollection.builder()
				.collectionPath(new CollectionPathTest(PIMDataType.EMAIL, "name"))
				.displayName("display name")
				.build();
		OpushCollection collection2 = OpushCollection.builder()
				.collectionPath(new CollectionPathTest(PIMDataType.EMAIL, "name 2"))
				.displayName("display name 2")
				.build();
		PathsToCollections changedCollections = PathsToCollections.builder()
				.put(collection.collectionPath(), collection)
				.put(collection2.collectionPath(), collection2)
				.build();

		mocks.replay();
		Iterable<OpushCollection> addedCollections = testee().addedCollections(lastKnownCollections, changedCollections);
		mocks.verify();
		
		assertThat(addedCollections).containsOnly(collection, collection2);
	}
	
	@Test
	public void testTwoDifferentTypeAddedWithSameNameWhenNoLastKnown() {
		Set<CollectionPath> lastKnownCollections = ImmutableSet.of();
		
		OpushCollection collection = OpushCollection.builder()
				.collectionPath(new CollectionPathTest(PIMDataType.EMAIL, "name"))
				.displayName("display name")
				.build();
		OpushCollection collection2 = OpushCollection.builder()
				.collectionPath(new CollectionPathTest(PIMDataType.CONTACTS, "name"))
				.displayName("display name")
				.build();
		PathsToCollections changedCollections = PathsToCollections.builder()
				.put(collection.collectionPath(), collection)
				.put(collection2.collectionPath(), collection2)
				.build();

		mocks.replay();
		Iterable<OpushCollection> addedCollections = testee().addedCollections(lastKnownCollections, changedCollections);
		mocks.verify();
		
		assertThat(addedCollections).containsOnly(collection, collection2);
	}
	
	@Test
	public void testOneAddedCollectionWhenSameInLastKnown() {
		Set<CollectionPath> lastKnownCollections = ImmutableSet.<CollectionPath>of(
				new CollectionPathTest(PIMDataType.EMAIL, "name"));
		
		OpushCollection collection = OpushCollection.builder()
				.collectionPath(new CollectionPathTest(PIMDataType.EMAIL, "name"))
				.displayName("display name")
				.build();
		PathsToCollections changedCollections = PathsToCollections.builder()
				.put(collection.collectionPath(), collection)
				.build();

		mocks.replay();
		Iterable<OpushCollection> addedCollections = testee().addedCollections(lastKnownCollections, changedCollections);
		mocks.verify();
		
		assertThat(addedCollections).isEmpty();
	}
	
	@Test
	public void testTwoAddedCollectionWhenOneSameInLastKnown() {
		Set<CollectionPath> lastKnownCollections = ImmutableSet.<CollectionPath>of(
				new CollectionPathTest(PIMDataType.EMAIL, "name"));
		
		OpushCollection collection = OpushCollection.builder()
				.collectionPath(new CollectionPathTest(PIMDataType.EMAIL, "name"))
				.displayName("display name")
				.build();
		OpushCollection collection2 = OpushCollection.builder()
				.collectionPath(new CollectionPathTest(PIMDataType.CONTACTS, "name"))
				.displayName("display name")
				.build();
		PathsToCollections changedCollections = PathsToCollections.builder()
				.put(collection.collectionPath(), collection)
				.put(collection2.collectionPath(), collection2)
				.build();

		mocks.replay();
		Iterable<OpushCollection> addedCollections = testee().addedCollections(lastKnownCollections, changedCollections);
		mocks.verify();
		
		assertThat(addedCollections).containsOnly(collection2);
	}
	
	@Test
	public void testNoAddedCollectionWhenOneInLastKnown() {
		Set<CollectionPath> lastKnownCollections = ImmutableSet.<CollectionPath>of(
				new CollectionPathTest(PIMDataType.EMAIL, "name"));
		
		PathsToCollections changedCollections = PathsToCollections.builder()
				.build();

		mocks.replay();
		Iterable<OpushCollection> addedCollections = testee().addedCollections(lastKnownCollections, changedCollections);
		mocks.verify();
		
		assertThat(addedCollections).isEmpty();
	}
	
	private static class CollectionPathTest extends CollectionPath {

		public CollectionPathTest(PIMDataType pimType, String displayName) {
			super("obm:\\\\test@test\\" + pimType.asCollectionPathValue() + "\\" + displayName, pimType, displayName);
		}
	}
}
