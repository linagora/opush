/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013-2014  Linagora
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
package org.obm.push.dao.testsuite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.guice.GuiceRunner;
import org.obm.push.ProtocolVersion;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.FolderSnapshotDao.FolderSnapshotNotFoundException;
import org.obm.push.state.FolderSyncKey;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

@RunWith(GuiceRunner.class)
public abstract class NewFolderSnapshotDaoTest {

	@Inject protected FolderSnapshotDao folderDao;
	
	private Device device;
	private User user;

	@Before
	public void setUp() {
		device = new Device(1, "devType", new DeviceId("devId"), new Properties(), ProtocolVersion.V121);
		user = Factory.create().createUser("login@domain", "email@domain", "displayName");
	}
	
	@Test(expected=FolderSnapshotNotFoundException.class)
	public void getShouldThrowNotFoundWhenNothingFound() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		folderDao.get(user, device, syncKey);
	}
	
	@Test
	public void createShouldMakeSnapshotFindableWhenEmptyAndCalendarType() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		FolderSnapshot snapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.<Folder>of());
		
		folderDao.create(user, device, syncKey, snapshot);
		FolderSnapshot result = folderDao.get(user, device, syncKey);
		
		assertThat(result).isEqualTo(snapshot);
	}
	
	@Test
	public void createShouldMakeSnapshotFindableWhenEmptyAndEmailType() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		FolderSnapshot snapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.<Folder>of());
		
		folderDao.create(user, device, syncKey, snapshot);
		FolderSnapshot result = folderDao.get(user, device, syncKey);
		
		assertThat(result).isEqualTo(snapshot);
	}

	@Test
	public void createShouldMakeSnapshotFindableWhenOnlyOneAndParent() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		Folder folder = Folder.builder()
			.displayName("name")
			.backendId("15")
			.collectionId(CollectionId.of(2))
			.parentBackendId(Optional.of("12"))
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot snapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder));
		
		folderDao.create(user, device, syncKey, snapshot);
		FolderSnapshot result = folderDao.get(user, device, syncKey);
		
		assertThat(result).isEqualTo(snapshot);
	}
	
	@Test
	public void createShouldMakeSnapshotFindableWhenOnlyOneAndNoParent() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		Folder folder = Folder.builder()
			.displayName("name")
			.backendId("15")
			.collectionId(CollectionId.of(2))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot snapshot = FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder));
		
		folderDao.create(user, device, syncKey, snapshot);
		FolderSnapshot result = folderDao.get(user, device, syncKey);
		
		assertThat(result).isEqualTo(snapshot);
	}

	@Test
	public void createShouldMakeSnapshotFindableWhenManySameTypeFolders() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");

		Folder folder1 = Folder.builder()
			.displayName("calendar")
			.backendId("8")
			.collectionId(CollectionId.of(2))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.USER_CREATED_CALENDAR_FOLDER).build();
		
		Folder folder2 = Folder.builder()
			.displayName("calendar 2")
			.backendId("88")
			.collectionId(CollectionId.of(22))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER).build();
		
		Folder folder3 = Folder.builder()
			.displayName("calendar 3")
			.backendId("888")
			.collectionId(CollectionId.of(2222))
			.parentBackendId(Optional.of("a parent"))
			.folderType(FolderType.USER_CREATED_CALENDAR_FOLDER).build();
		
		FolderSnapshot snapshot = FolderSnapshot.nextId(8).folders(ImmutableSet.of(folder1, folder2, folder3));
		folderDao.create(user, device, syncKey, snapshot);
		FolderSnapshot result = folderDao.get(user, device, syncKey);
		
		assertThat(result).isEqualTo(snapshot);
	}
	
	@Test
	public void createShouldMakeSnapshotFindableWhenManyDifferentTypeFolders() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");

		Folder calendarFolder = Folder.builder()
			.displayName("calendar")
			.backendId("8")
			.collectionId(CollectionId.of(2))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.USER_CREATED_CALENDAR_FOLDER).build();
		
		Folder contactFolder = Folder.builder()
			.displayName("address book")
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		Folder mailFolder = Folder.builder()
			.displayName("INBOX")
			.backendId("INBOX")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot snapshot = FolderSnapshot.nextId(2).folders(
				ImmutableSet.of(calendarFolder, contactFolder, mailFolder));
		folderDao.create(user, device, syncKey, snapshot);

		FolderSnapshot result = folderDao.get(user, device, syncKey);
		
		assertThat(result).isEqualTo(snapshot);
	}
	
	@Test
	public void createShouldOverrideSnapshotWhenSameUserDeviceIdAndSyncKey() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");

		Folder folder1 = Folder.builder()
			.displayName("calendar")
			.backendId("8")
			.collectionId(CollectionId.of(2))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.USER_CREATED_CALENDAR_FOLDER).build();
		
		Folder folder2 = Folder.builder()
			.displayName("address book")
			.backendId("12")
			.collectionId(CollectionId.of(1))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER).build();
		
		Folder folder3 = Folder.builder()
			.displayName("INBOX")
			.backendId("INBOX")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build();
		
		FolderSnapshot snapshot1 = FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder1));
		folderDao.create(user, device, syncKey, snapshot1);

		FolderSnapshot snapshot2 = FolderSnapshot.nextId(4).folders(ImmutableSet.of(folder2));
		folderDao.create(user, device, syncKey, snapshot2);
		
		FolderSnapshot snapshot3 = FolderSnapshot.nextId(5).folders(ImmutableSet.of(folder3));
		folderDao.create(user, device, syncKey, snapshot3);

		FolderSnapshot result = folderDao.get(user, device, syncKey);
		
		assertThat(result).isEqualTo(snapshot3);
	}
	
	@Test(expected=CollectionNotFoundException.class)
	public void createShouldThrowNotFoundWhenWrongCollectionId() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		
		Folder folder = Folder.builder()
			.displayName("INBOX")
			.backendId("INBOX")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER).build();
		
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder)));
		
		folderDao.get(user, device, CollectionId.of(4));
	}
	
	@Test
	public void createShouldMakeFolderFindableWhenRightCollectionId() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		
		Folder folder = Folder.builder()
			.displayName("INBOX")
			.backendId("INBOX")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER).build();
		
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder)));
		
		Folder result = folderDao.get(user, device, folder.getCollectionId());
		
		assertThat(result).isEqualTo(folder);
	}
	
	@Test
	public void createShouldMakeTheLastFolderFindableWhenCreatedManyTimes() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		
		Folder folderV1 = Folder.builder()
			.displayName("INBOX")
			.backendId("INBOX")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER).build();
		
		Folder folderV2 = Folder.builder()
			.displayName("INBOX V2")
			.backendId("INBOX V2")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER).build();
		
		Folder folderV3 = Folder.builder()
			.displayName("INBOX V3")
			.backendId("INBOX V3")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.of("the parent"))
			.folderType(FolderType.USER_CREATED_EMAIL_FOLDER).build();
		
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folderV1)));
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folderV2)));
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folderV3)));
		
		Folder result = folderDao.get(user, device, folderV1.getCollectionId());
		
		assertThat(result).isEqualTo(folderV3);
	}

	@Test(expected=CollectionNotFoundException.class)
	public void createShouldThrowNotFoundWhenWrongBackendId() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		
		Folder folder = Folder.builder()
			.displayName("INBOX")
			.backendId("INBOX")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER).build();
		
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder)));
		
		folderDao.get(user, device, PIMDataType.EMAIL, "unknown backend id");
	}

	@Test(expected=CollectionNotFoundException.class)
	public void createShouldThrowNotFoundWhenWrongFolderType() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		
		Folder folder = Folder.builder()
			.displayName("INBOX")
			.backendId("INBOX")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER).build();
		
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder)));
		
		folderDao.get(user, device, PIMDataType.CALENDAR, folder.getBackendId());
	}
	
	@Test
	public void createShouldMakeFolderFindableWhenRightBackendIdAndFolderType() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		
		Folder folder = Folder.builder()
			.displayName("INBOX")
			.backendId("INBOX")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER).build();
		
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder)));
		
		Folder result = folderDao.get(user, device, PIMDataType.EMAIL, folder.getBackendId());
		
		assertThat(result).isEqualTo(folder);
	}
	
	@Test
	public void createShouldMakeTheLastFolderFindableWhenCreatedManyTimesByBackendId() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		
		Folder folderV1 = Folder.builder()
			.displayName("INBOX")
			.backendId("INBOX")
			.collectionId(CollectionId.of(3))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER).build();
		
		Folder folderV2 = Folder.builder()
			.displayName("INBOX V2")
			.backendId("INBOX")
			.collectionId(CollectionId.of(4))
			.parentBackendId(Optional.<String>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER).build();
		
		Folder folderV3 = Folder.builder()
			.displayName("INBOX V3")
			.backendId("INBOX")
			.collectionId(CollectionId.of(5))
			.parentBackendId(Optional.of("the parent"))
			.folderType(FolderType.USER_CREATED_EMAIL_FOLDER).build();
		
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folderV1)));
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folderV2)));
		folderDao.create(user, device, syncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folderV3)));
		
		Folder result = folderDao.get(user, device, PIMDataType.EMAIL, folderV1.getBackendId());
		
		assertThat(result).isEqualTo(folderV3);
	}
}
