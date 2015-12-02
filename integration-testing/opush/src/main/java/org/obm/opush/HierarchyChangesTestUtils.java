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

import org.obm.configuration.EmailConfiguration;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.calendar.CalendarBackend;
import org.obm.push.contacts.ContactsBackend;
import org.obm.push.mail.MailBackend;
import org.obm.push.mail.MailBackendFoldersBuilder;
import org.obm.push.task.TaskBackend;

import com.google.inject.Inject;

public class HierarchyChangesTestUtils {

	@Inject CalendarBackend calendarBackend;
	@Inject TaskBackend taskBackend;
	@Inject ContactsBackend contactsBackend;
	@Inject MailBackend mailBackend;

	public void mockGetBackendFoldersUnchanged() {
		expectEmptyContactFolders();
		expectEmptyCalendarFolders();
		expectEmptyMailFolders();
		expectEmptyTaskFolders();
	}
	
	public void mockGetBackendFoldersWithNewMailboxes(BackendFolders mailboxes) {
		expectEmptyContactFolders();
		expectEmptyCalendarFolders();
		expectEmptyTaskFolders();
		expect(mailBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(mailboxes);
	}
	
	public void mockGetBackendFoldersWithINBOX() {
		mockGetBackendFoldersWithNewMailboxes(new MailBackendFoldersBuilder()
			.addSpecialFolder(EmailConfiguration.IMAP_INBOX_NAME, FolderType.DEFAULT_INBOX_FOLDER)
			.build());
	}

	public void expectEmptyTaskFolders() {
		expect(taskBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.instance());
	}

	public void expectEmptyMailFolders() {
		expect(mailBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.instance());
	}

	public void expectEmptyCalendarFolders() {
		expect(calendarBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.instance());
	}

	public void expectEmptyContactFolders() {
		expect(contactsBackend.getBackendFolders(anyObject(UserDataRequest.class)))
			.andReturn(BackendFolders.EMPTY.instance());
	}
}
