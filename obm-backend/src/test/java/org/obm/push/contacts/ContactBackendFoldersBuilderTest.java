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

import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.AddressBookId;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.sync.book.Folder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class ContactBackendFoldersBuilderTest {

	private User user;
	private Device device;
	private UserDataRequest udr;

	@Before
	public void setUp() {
		user = Factory.create().createUser("test@test", "test@domain", "displayName");
		device = new Device.Factory().create(null, "iPhone", "iOs 5", new DeviceId("my phone"), null);
		udr = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
	}

	
	@Test
	public void findFolderTypeShouldReturnUserCreatedWhenRightNameAndWrongUser() {
		FolderType folderType = new ContactBackendFoldersBuilder()
			.defaultAddressBookName("default")
			.findFolderType(udr, Folder.builder()
				.name("default")
				.uid(12)
				.ownerLoginAtDomain("belongs to another")
				.build());
		
		assertThat(folderType).isEqualTo(FolderType.USER_CREATED_CONTACTS_FOLDER);
	}
	
	@Test
	public void findFolderTypeShouldReturnUserCreatedWhenWrongNameAndRightUser() {
		FolderType folderType = new ContactBackendFoldersBuilder()
			.defaultAddressBookName("default")
			.findFolderType(udr, Folder.builder()
				.name("not matching")
				.uid(12)
				.ownerLoginAtDomain(user.getLoginAtDomain())
				.build());
		
		assertThat(folderType).isEqualTo(FolderType.USER_CREATED_CONTACTS_FOLDER);
	}
	
	@Test
	public void findFolderTypeShouldReturnUserCreatedWhenNameAndUserMatch() {
		FolderType folderType = new ContactBackendFoldersBuilder()
			.defaultAddressBookName("default")
			.findFolderType(udr, Folder.builder()
				.name("default")
				.uid(12)
				.ownerLoginAtDomain(user.getLoginAtDomain())
				.build());
		
		assertThat(folderType).isEqualTo(FolderType.DEFAULT_CONTACTS_FOLDER);
	}
	
	@Test
	public void buildEmptyShouldReturnEmpty() {
		BackendFolders backendFolders = new ContactBackendFoldersBuilder()
			.userDataRequest(udr)
			.defaultAddressBookName("default")
			.folders(ImmutableSet.<Folder>of())
			.build();
		
		assertThat(backendFolders).isEmpty();
	}

	@Test
	public void buildOneFolderShouldReturnDefaultWhenMatchingNameAndOwner() {
		BackendFolders backendFolders = new ContactBackendFoldersBuilder()
			.userDataRequest(udr)
			.defaultAddressBookName("default")
			.folders(ImmutableSet.of( 
				Folder.builder()
					.name("default")
					.uid(12)
					.ownerLoginAtDomain(user.getLoginAtDomain()).build()))
			.build();
		
		assertThat(backendFolders).containsOnly(BackendFolder.builder()
			.backendId(AddressBookId.of(12))
			.parentId(Optional.<BackendId>absent())
			.displayName("default")
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
			.build());
	}
	
	@Test
	public void buildOneFolderShouldReturnUserFolderWhenMatchingNameButNotOwner() {
		BackendFolders backendFolders = new ContactBackendFoldersBuilder()
			.userDataRequest(udr)
			.defaultAddressBookName("default")
			.folders(ImmutableSet.of( 
				Folder.builder()
					.name("default")
					.uid(12)
					.ownerLoginAtDomain("belongs to another user").build()))
			.build();
		
		assertThat(backendFolders).containsOnly(BackendFolder.builder()
			.backendId(AddressBookId.of(12))
			.parentId(Optional.<BackendId>absent())
			.displayName("default")
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
			.build());
	}
	
	@Test
	public void buildOneFolderShouldReturnUserFolderWhenMatchingOwnerButNotName() {
		BackendFolders backendFolders = new ContactBackendFoldersBuilder()
			.userDataRequest(udr)
			.defaultAddressBookName("default")
			.folders(ImmutableSet.of( 
				Folder.builder()
					.name("the name")
					.uid(12)
					.ownerLoginAtDomain(user.getLoginAtDomain()).build()))
			.build();
		
		assertThat(backendFolders).containsOnly(BackendFolder.builder()
			.backendId(AddressBookId.of(12))
			.parentId(Optional.<BackendId>absent())
			.displayName("the name")
			.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
			.build());
	}
	
	@Test
	public void buildTwoFoldersShouldReturnHierarchyWhenDefaultAsFirst() {
		BackendFolders backendFolders = new ContactBackendFoldersBuilder()
			.userDataRequest(udr)
			.defaultAddressBookName("default")
			.folders(ImmutableList.of( 
				Folder.builder()
					.name("default")
					.uid(15)
					.ownerLoginAtDomain(user.getLoginAtDomain()).build(),
				Folder.builder()
					.name("the name")
					.uid(12)
					.ownerLoginAtDomain(user.getLoginAtDomain()).build()))
			.build();
		
		assertThat(backendFolders).containsOnly(
			BackendFolder.builder()
				.backendId(AddressBookId.of(15))
				.parentId(Optional.<BackendId>absent())
				.displayName("default")
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build(),
			BackendFolder.builder()
				.backendId(AddressBookId.of(12))
				.parentId(Optional.<BackendId>of(AddressBookId.of(15)))
				.displayName("the name")
				.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
				.build());
	}
	
	@Test
	public void buildTwoFoldersShouldReturnHierarchyWhenDefaultAsLast() {
		BackendFolders backendFolders = new ContactBackendFoldersBuilder()
			.userDataRequest(udr)
			.defaultAddressBookName("default")
			.folders(ImmutableList.of( 
				Folder.builder()
					.name("the name")
					.uid(15)
					.ownerLoginAtDomain(user.getLoginAtDomain()).build(),
				Folder.builder()
					.name("default")
					.uid(12)
					.ownerLoginAtDomain(user.getLoginAtDomain()).build()))
			.build();

		assertThat(backendFolders).containsOnly(
			BackendFolder.builder()
				.backendId(AddressBookId.of(12))
				.parentId(Optional.<BackendId>absent())
				.displayName("default")
				.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
				.build(),
			BackendFolder.builder()
				.backendId(AddressBookId.of(15))
				.parentId(Optional.<BackendId>of(AddressBookId.of(12)))
				.displayName("the name")
				.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
				.build());
	}
	
	@Test
	public void buildTwoFoldersShouldReturnOnlyUserCreatedWhenNoDefault() {
		BackendFolders backendFolders = new ContactBackendFoldersBuilder()
			.userDataRequest(udr)
			.defaultAddressBookName("default")
			.folders(ImmutableList.of( 
				Folder.builder()
					.name("the name")
					.uid(15)
					.ownerLoginAtDomain(user.getLoginAtDomain()).build(),
				Folder.builder()
					.name("not default")
					.uid(12)
					.ownerLoginAtDomain(user.getLoginAtDomain()).build()))
			.build();
	
		assertThat(backendFolders).containsOnly(
			BackendFolder.builder()
				.backendId(AddressBookId.of(12))
				.parentId(Optional.<BackendId>absent())
				.displayName("not default")
				.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
				.build(),
			BackendFolder.builder()
				.backendId(AddressBookId.of(15))
				.parentId(Optional.<BackendId>absent())
				.displayName("the name")
				.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
				.build());
	}

}
