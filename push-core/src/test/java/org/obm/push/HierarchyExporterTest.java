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
package org.obm.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.backend.PIMBackend;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.mail.MailBackend;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class HierarchyExporterTest {

	private User user;
	private Device device;
	private UserDataRequest userDataRequest;
	
	@Before
	public void setUp() {
		this.user = Factory.create().createUser("test@test", "test@domain", "displayName");
		this.device = new Device.Factory().create(null, "iPhone", "iOs 5", new DeviceId("my phone"), null);
		this.userDataRequest = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
	}
	
	@Test
	public void testHierarchyItemsChangesBuilder() {
		HierarchyCollectionChanges itemsChanges = HierarchyCollectionChanges.builder().build();
		assertThat(itemsChanges.getCollectionChanges()).isEmpty();
		assertThat(itemsChanges.getCollectionDeletions()).isEmpty();
	}
	
	@Test(expected=NullPointerException.class)
	public void testHierarchyItemsChangesBuilderChangesNPE() {
		HierarchyCollectionChanges.builder().changes(null).build();
	}

	@Test(expected=NullPointerException.class)
	public void testHierarchyItemsChangesBuilderDeletionsNPE() {
		HierarchyCollectionChanges.builder().deletions(null).build();
	}
	
	@Test
	public void testHierarchyItemsChangesBuilderMergeItems() {
		CollectionChange item1 = CollectionChange.builder()
				.collectionId(CollectionId.of(1))
				.parentCollectionId(CollectionId.ROOT)
				.displayName("1")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.isNew(true)
				.build();
		CollectionChange item2 = CollectionChange.builder()
				.collectionId(CollectionId.of(11))
				.parentCollectionId(CollectionId.ROOT)
				.displayName("2")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.isNew(true)
				.build();
		CollectionChange item3 = CollectionChange.builder()
				.collectionId(CollectionId.of(2))
				.parentCollectionId(CollectionId.ROOT)
				.displayName("3")
				.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
				.isNew(true)
				.build();
		CollectionDeletion item4 = CollectionDeletion.builder()
				.collectionId(CollectionId.of(22))
				.build();

		HierarchyCollectionChanges hierarchyItemsChanges1 = HierarchyCollectionChanges.builder()
			.changes(Lists.newArrayList(item1, item2)).build();
		
		HierarchyCollectionChanges hierarchyItemsChanges2 = HierarchyCollectionChanges.builder()
		.changes(Lists.newArrayList(item3)).deletions(Lists.newArrayList(item4)).build();
		
		HierarchyCollectionChanges hierarchyItemsChanges = HierarchyCollectionChanges.builder()
				.mergeItems(hierarchyItemsChanges1)
				.mergeItems(hierarchyItemsChanges2).build();
		
		assertThat(hierarchyItemsChanges.getCollectionChanges())
			.containsOnly(item1, item2, item3);
		
		assertThat(hierarchyItemsChanges.getCollectionDeletions()).containsOnly(item4);
	}

	@Test
	public void getBackendFoldersShouldBeEmptyWhenAllEmpty() {
		PIMBackend contactsBackend = createMock(PIMBackend.class);
		PIMBackend calendarBackend = createMock(PIMBackend.class);
		MailBackend mailBackend = createMock(MailBackend.class);

		expectGetPIMDataType(contactsBackend, calendarBackend, mailBackend);
		expect(contactsBackend.getBackendFolders(userDataRequest))
			.andReturn(BackendFolders.EMPTY.instance());
		expect(calendarBackend.getBackendFolders(userDataRequest))
			.andReturn(BackendFolders.EMPTY.instance());
		expect(mailBackend.getBackendFolders(userDataRequest))
			.andReturn(BackendFolders.EMPTY.instance());
		
		replay(mailBackend, calendarBackend, contactsBackend);

		Backends backends = buildBackends(contactsBackend, calendarBackend, mailBackend);
		BackendFolders backendFolders = new HierarchyExporter(backends).getBackendFolders(userDataRequest);
		
		verify(mailBackend, calendarBackend, contactsBackend);
		
		assertThat(backendFolders).isEmpty();
	}

	@Test
	public void getBackendFoldersShouldHaveAllElementsWhenEveryBackendHasFolder() {
		PIMBackend contactsBackend = createMock(PIMBackend.class);
		PIMBackend calendarBackend = createMock(PIMBackend.class);
		MailBackend mailBackend = createMock(MailBackend.class);

		BackendFolder addressBook = BackendFolder.builder()
			.displayName("addressBook")
			.backendId(new TestBackendId("12"))
			.parentId(Optional.<BackendId>absent())
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
			.build();
		
		BackendFolder calendar = BackendFolder.builder()
			.displayName("calendar")
			.backendId(new TestBackendId("15"))
			.parentId(Optional.<BackendId>absent())
			.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
			.build();
		
		BackendFolder mailbox = BackendFolder.builder()
			.displayName("mailbox")
			.backendId(new TestBackendId("18"))
			.parentId(Optional.<BackendId>absent())
			.folderType(FolderType.DEFAULT_INBOX_FOLDER)
			.build();
		
		expectGetPIMDataType(contactsBackend, calendarBackend, mailBackend);
		expect(contactsBackend.getBackendFolders(userDataRequest)).andReturn(folders(addressBook));
		expect(calendarBackend.getBackendFolders(userDataRequest)).andReturn(folders(calendar));
		expect(mailBackend.getBackendFolders(userDataRequest)).andReturn(folders(mailbox));
		
		replay(mailBackend, calendarBackend, contactsBackend);
		
		Backends backends = buildBackends(contactsBackend, calendarBackend, mailBackend);
		BackendFolders backendFolders = new HierarchyExporter(backends).getBackendFolders(userDataRequest);
		
		verify(mailBackend, calendarBackend, contactsBackend);
		
		assertThat(backendFolders).containsOnly(addressBook, calendar, mailbox);
	}

	private BackendFolders folders(final BackendFolder folder) {
		return new BackendFolders() {

			@Override
			public Iterator<BackendFolder> iterator() {
				return ImmutableSet.of(folder).iterator();
			}};
	}

	private void expectGetPIMDataType(PIMBackend contactsBackend,
                                      PIMBackend calendarBackend, MailBackend mailBackend) {
		
		expect(contactsBackend.getPIMDataType()).andReturn(PIMDataType.CONTACTS).anyTimes();
		expect(calendarBackend.getPIMDataType()).andReturn(PIMDataType.CALENDAR).anyTimes();
		expect(mailBackend.getPIMDataType()).andReturn(PIMDataType.EMAIL).anyTimes();
	}

	private Backends buildBackends(PIMBackend contactsBackend,
			PIMBackend calendarBackend, MailBackend mailBackend) {
		
		return new Backends(Sets.newHashSet(contactsBackend, calendarBackend, mailBackend));
	}
}
