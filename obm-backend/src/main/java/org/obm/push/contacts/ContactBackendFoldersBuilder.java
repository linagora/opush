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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.obm.push.bean.FolderType;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.AddressBookId;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.sync.book.Folder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Sets;

public class ContactBackendFoldersBuilder {

	private Set<Folder> folders;
	private Folder parentFolder;
	private UserDataRequest udr;
	private String defaultAddressBookName;
	
	public ContactBackendFoldersBuilder() {
		folders = Sets.newHashSet();
	}

	public ContactBackendFoldersBuilder userDataRequest(UserDataRequest udr) {
		this.udr = udr;
		return this;
	}
	
	public ContactBackendFoldersBuilder defaultAddressBookName(String defaultAddressBookName) {
		this.defaultAddressBookName = defaultAddressBookName;
		return this;
	}
	
	public ContactBackendFoldersBuilder folders(Collection<Folder> allFolders) {
		for (Folder folder : allFolders) {
			add(folder);
		}
		return this;
	}
	
	private void add(Folder folder) {
		if (FolderType.DEFAULT_CONTACTS_FOLDER == findFolderType(udr, folder)) {
			parentFolder = folder;
		} else {
			folders.add(folder);
		}
	}

	@VisibleForTesting FolderType findFolderType(UserDataRequest udr, Folder folder) {
		if (isDefaultFolder(udr, folder)) {
			return FolderType.DEFAULT_CONTACTS_FOLDER;
		} else {
			return FolderType.USER_CREATED_CONTACTS_FOLDER;
		}
	}

	private boolean isDefaultFolder(UserDataRequest udr, Folder folder) {
		boolean isOwner = udr.getUser().getLoginAtDomain().equalsIgnoreCase(folder.getOwnerLoginAtDomain());
		boolean isDefaultAddressBookName = folder.getName().equalsIgnoreCase(defaultAddressBookName);
		return isOwner && isDefaultAddressBookName;
	}
	
	public BackendFolders build() {
		final Builder<BackendFolder> backendFoldersBuilder = ImmutableSet.builder();
		Optional<BackendId> parentId = parentFolder != null ? 
				Optional.<BackendId>of(AddressBookId.of(parentFolder.getUid())) :
				Optional.<BackendId>absent();
		
		if (parentFolder != null) {
			backendFoldersBuilder.add(BackendFolder.builder()
				.displayName(parentFolder.getName())
				.folderType(findFolderType(udr, parentFolder))
				.backendId(AddressBookId.of(parentFolder.getUid()))
				.parentId(Optional.<BackendId>absent())
				.build());
		}
		
		for (Folder folder : folders) {
			backendFoldersBuilder.add(BackendFolder.builder()
				.displayName(folder.getName())
				.folderType(findFolderType(udr, folder))
				.backendId(AddressBookId.of(folder.getUid()))
				.parentId(parentId)
				.build());
		}
		return new BackendFolders() {
			
			Set<BackendFolder> backendFolders = backendFoldersBuilder.build();
			
			@Override
			public Iterator<BackendFolder> iterator() {
				return backendFolders.iterator();
			}
		};
	}
}