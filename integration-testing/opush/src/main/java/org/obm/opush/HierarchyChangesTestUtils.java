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
package org.obm.opush;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.Arrays;

import org.obm.push.backend.PIMBackend;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.calendar.CalendarBackend;
import org.obm.push.calendar.CalendarPath;
import org.obm.push.contacts.AddressBookId;
import org.obm.push.contacts.ContactsBackend;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.activesync.InvalidSyncKeyException;
import org.obm.push.mail.MailBackend;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.task.TaskBackend;

import com.google.inject.Inject;

public class HierarchyChangesTestUtils {

	@Inject CalendarBackend calendarBackend;
	@Inject TaskBackend taskBackend;
	@Inject ContactsBackend contactsBackend;
	@Inject MailBackend mailBackend;

	public void mockHierarchyChangesOnlyInbox()
			throws DaoException, UnexpectedObmSyncServerException, InvalidSyncKeyException {
		
		mockHierarchyChangesForMailboxes(
			HierarchyCollectionChanges.builder()
				.changes(Arrays.asList(inboxFolder()))
				.build());
	}
	
	public CollectionChange inboxFolder() {
		return CollectionChange.builder()
				.collectionId(CollectionId.of(1))
				.parentCollectionId(CollectionId.ROOT)
				.displayName("INBOX")
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.isNew(true)
				.build();
	}

	public void mockHierarchyChangesForMailboxes(HierarchyCollectionChanges mailboxesChanges)
			throws DaoException, UnexpectedObmSyncServerException, InvalidSyncKeyException {
		
		mockCalendarHierarchyChangesReturnNoChange();
		mockTaskHierarchyChangesReturnNoChange();
		mockContactsHierarchyChangesReturnNoChange();
		mockMailBackendHierarchyChanges(mailboxesChanges);
	}

	private void mockCalendarHierarchyChangesReturnNoChange() {
		mockBackendHierarchyChangesReturnNoChange(calendarBackend);
	}
	
	private void mockTaskHierarchyChangesReturnNoChange() throws DaoException {
		mockBackendHierarchyChangesReturnNoChange(taskBackend);
	}
	
	private void mockContactsHierarchyChangesReturnNoChange() {
		mockBackendHierarchyChangesReturnNoChange(contactsBackend);
	}
	
	private void mockBackendHierarchyChangesReturnNoChange(PIMBackend backend) {
		mockBackendHierarchyChanges(backend, emptyChange());
	}

	public void mockMailBackendHierarchyChanges(HierarchyCollectionChanges hierarchyMailboxesChanges) {
		mockBackendHierarchyChanges(mailBackend, hierarchyMailboxesChanges);
	}
	
	private void mockBackendHierarchyChanges(PIMBackend backend, HierarchyCollectionChanges changes) {
		expect(backend.getHierarchyChanges(anyObject(UserDataRequest.class),
				anyObject(FolderSyncState.class), anyObject(FolderSyncState.class)))
			.andReturn(changes);
	}
	
	private HierarchyCollectionChanges emptyChange() {
		return HierarchyCollectionChanges.builder().build();
	}

	public void mockGetBackendFoldersUnchanged() {
		expect(contactsBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.<AddressBookId>instance());
		expect(calendarBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.<CalendarPath>instance());
		expect(mailBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.instance());
		expect(taskBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.instance());
	}
	

}
