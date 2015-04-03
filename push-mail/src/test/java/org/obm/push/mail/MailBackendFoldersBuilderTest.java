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
package org.obm.push.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.obm.configuration.EmailConfiguration.IMAP_DRAFTS_NAME;
import static org.obm.configuration.EmailConfiguration.IMAP_INBOX_NAME;
import static org.obm.configuration.EmailConfiguration.IMAP_SENT_NAME;
import static org.obm.configuration.EmailConfiguration.IMAP_TRASH_NAME;
import static org.obm.push.bean.change.hierarchy.MailboxPath.DEFAULT_SEPARATOR;

import org.junit.Test;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.mail.bean.MailboxFolder;
import org.obm.push.mail.bean.MailboxFolders;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;


public class MailBackendFoldersBuilderTest {

	@Test
	public void buildShouldReturnEmptyWhenEmpty() {
		assertThat(new MailBackendFoldersBuilder().build()).isEmpty();
	}
	
	
	@Test
	public void buildShouldReturnOnlyElementWhenOnlyOneFolder() {
		MailboxFolders folders = new MailboxFolders(ImmutableList.of(
			new MailboxFolder("mailbox", DEFAULT_SEPARATOR))
		);
		assertThat(new MailBackendFoldersBuilder().addFolders(folders).build()).containsOnly(
			BackendFolder.builder()
				.backendId(MailboxPath.of("mailbox"))
				.displayName("mailbox")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build());
	}
	
	@Test
	public void buildShouldReturnRightFolderTypeWhenCommonMailboxes() {
		ImmutableSet<String> folders = ImmutableSet.of(
			IMAP_INBOX_NAME,
			IMAP_DRAFTS_NAME,
			IMAP_SENT_NAME,
			IMAP_TRASH_NAME
		);
		assertThat(new MailBackendFoldersBuilder().addSpecialFolders(folders).build()).containsOnly(
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_INBOX_NAME))
				.displayName(IMAP_INBOX_NAME)
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_DRAFTS_NAME))
				.displayName(IMAP_DRAFTS_NAME)
				.folderType(FolderType.DEFAULT_DRAFTS_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_SENT_NAME))
				.displayName(IMAP_SENT_NAME)
				.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_TRASH_NAME))
				.displayName(IMAP_TRASH_NAME)
				.folderType(FolderType.DEFAULT_DELETED_ITEMS_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build()
		);
	}
	
	@Test
	public void buildShouldNotDuplicateSpecialMailboxesWhenAddedTwice() {
		ImmutableSet<String> specialFolders = ImmutableSet.of(
			IMAP_INBOX_NAME,
			IMAP_DRAFTS_NAME,
			IMAP_SENT_NAME,
			IMAP_TRASH_NAME
		);

		MailboxFolders sameFoldersAsSubscribed = new MailboxFolders(ImmutableList.of(
			new MailboxFolder(IMAP_SENT_NAME, DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_INBOX_NAME, DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_DRAFTS_NAME, DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_TRASH_NAME, DEFAULT_SEPARATOR))
		);
		
		BackendFolders backendFolders = new MailBackendFoldersBuilder()
			.addSpecialFolders(specialFolders)
			.addFolders(sameFoldersAsSubscribed)
			.build();
		
		assertThat(backendFolders).containsOnly(
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_INBOX_NAME))
				.displayName(IMAP_INBOX_NAME)
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_DRAFTS_NAME))
				.displayName(IMAP_DRAFTS_NAME)
				.folderType(FolderType.DEFAULT_DRAFTS_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_SENT_NAME))
				.displayName(IMAP_SENT_NAME)
				.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_TRASH_NAME))
				.displayName(IMAP_TRASH_NAME)
				.folderType(FolderType.DEFAULT_DELETED_ITEMS_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build()
		);
	}

	@Test
	public void buildSupportsParentMatchingWhenDifferentSeparators() {
		MailboxFolders folders = new MailboxFolders(ImmutableList.of(
			new MailboxFolder("custom", DEFAULT_SEPARATOR),
			new MailboxFolder("custom/sub", DEFAULT_SEPARATOR),
			new MailboxFolder("custom.sub2", '.')
		));

		BackendFolders backendFolders = new MailBackendFoldersBuilder()
			.addFolders(folders)
			.build();
		
		assertThat(backendFolders).containsOnly(
			BackendFolder.builder()
				.backendId(MailboxPath.of("custom", DEFAULT_SEPARATOR))
				.displayName("custom")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of("custom/sub", DEFAULT_SEPARATOR))
				.displayName("custom/sub")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>of(MailboxPath.of("custom", DEFAULT_SEPARATOR)))
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of("custom.sub2", '.'))
				.displayName("custom.sub2")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>of(MailboxPath.of("custom", DEFAULT_SEPARATOR)))
				.build()
		);
	}

	@Test
	public void buildShouldReturnRightParentIsWhenSubMailboxUnordered() {
		MailboxFolders folders = new MailboxFolders(ImmutableList.of(
			new MailboxFolder("test/sub", DEFAULT_SEPARATOR),
			new MailboxFolder("test", DEFAULT_SEPARATOR),
			new MailboxFolder("test/sub2", DEFAULT_SEPARATOR)
		));

		BackendFolders backendFolders = new MailBackendFoldersBuilder()
			.addFolders(folders)
			.build();
		
		assertThat(backendFolders).containsOnly(
			BackendFolder.builder()
				.backendId(MailboxPath.of("test", DEFAULT_SEPARATOR))
				.displayName("test")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of("test/sub", DEFAULT_SEPARATOR))
				.displayName("test/sub")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>of(MailboxPath.of("test", DEFAULT_SEPARATOR)))
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of("test/sub2", DEFAULT_SEPARATOR))
				.displayName("test/sub2")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>of(MailboxPath.of("test", DEFAULT_SEPARATOR)))
				.build()
		);
	}
	
	@Test
	public void buildShouldReturnRightParentIsWhenSubMailbox() {
		MailboxFolders folders = new MailboxFolders(ImmutableList.of(
			new MailboxFolder("custom/sub", DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_INBOX_NAME + "/submailbox", DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_SENT_NAME, DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_INBOX_NAME, DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_INBOX_NAME + "/sub/mailbox", DEFAULT_SEPARATOR),
			new MailboxFolder("folder/with/hierachy/but/no/parent", DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_DRAFTS_NAME, DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_INBOX_NAME + "/sub/mailbox/withparent", DEFAULT_SEPARATOR),
			new MailboxFolder("custom", DEFAULT_SEPARATOR),
			new MailboxFolder("custom2", DEFAULT_SEPARATOR),
			new MailboxFolder("custom/sub/sub with/some space/sub", DEFAULT_SEPARATOR),
			new MailboxFolder(IMAP_TRASH_NAME, DEFAULT_SEPARATOR))
		);
		
		BackendFolders backendFolders = new MailBackendFoldersBuilder()
			.addFolders(folders)
			.addSpecialFolders(ImmutableList.of(
				IMAP_INBOX_NAME,
				IMAP_DRAFTS_NAME,
				IMAP_SENT_NAME,
				IMAP_TRASH_NAME))
			.build();
		
		assertThat(backendFolders).containsOnly(
			BackendFolder.builder()
				.backendId(MailboxPath.of("custom"))
				.displayName("custom")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of("custom/sub"))
				.displayName("custom/sub")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>of(MailboxPath.of("custom")))
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of("custom/sub/sub with/some space/sub"))
				.displayName("custom/sub/sub with/some space/sub")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>of(MailboxPath.of("custom/sub")))
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of("custom2"))
				.displayName("custom2")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of("folder/with/hierachy/but/no/parent"))
				.displayName("folder/with/hierachy/but/no/parent")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_INBOX_NAME))
				.displayName(IMAP_INBOX_NAME)
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_INBOX_NAME + "/submailbox"))
				.displayName(IMAP_INBOX_NAME + "/submailbox")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>of(MailboxPath.of(IMAP_INBOX_NAME)))
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_INBOX_NAME + "/sub/mailbox"))
				.displayName(IMAP_INBOX_NAME + "/sub/mailbox")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>of(MailboxPath.of(IMAP_INBOX_NAME)))
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_INBOX_NAME + "/sub/mailbox/withparent"))
				.displayName(IMAP_INBOX_NAME + "/sub/mailbox/withparent")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>of(MailboxPath.of(IMAP_INBOX_NAME + "/sub/mailbox")))
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_DRAFTS_NAME))
				.displayName(IMAP_DRAFTS_NAME)
				.folderType(FolderType.DEFAULT_DRAFTS_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_SENT_NAME))
				.displayName(IMAP_SENT_NAME)
				.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_TRASH_NAME))
				.displayName(IMAP_TRASH_NAME)
				.folderType(FolderType.DEFAULT_DELETED_ITEMS_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build()
		);
	}
}
