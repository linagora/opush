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
package org.obm.push.bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.obm.push.bean.PIMDataType.CALENDAR;
import static org.obm.push.bean.PIMDataType.CONTACTS;
import static org.obm.push.bean.PIMDataType.EMAIL;
import static org.obm.push.bean.PIMDataType.TASKS;
import static org.obm.push.bean.PIMDataType.UNKNOWN;

import org.junit.Test;

public class FolderTypeTest {

	@Test
	public void testFolderTypeAttributes() {
		assertThat(FolderType.USER_FOLDER_GENERIC.asSpecificationValue()).isEqualTo("1");
		assertThat(FolderType.USER_FOLDER_GENERIC.getPIMDataType()).isEqualTo(UNKNOWN);
		assertThat(FolderType.DEFAULT_INBOX_FOLDER.asSpecificationValue()).isEqualTo("2");
		assertThat(FolderType.DEFAULT_INBOX_FOLDER.getPIMDataType()).isEqualTo(EMAIL);
		assertThat(FolderType.DEFAULT_DRAFTS_FOLDER.asSpecificationValue()).isEqualTo("3");
		assertThat(FolderType.DEFAULT_DRAFTS_FOLDER.getPIMDataType()).isEqualTo(EMAIL);
		assertThat(FolderType.DEFAULT_DELETED_ITEMS_FOLDER.asSpecificationValue()).isEqualTo("4");
		assertThat(FolderType.DEFAULT_DELETED_ITEMS_FOLDER.getPIMDataType()).isEqualTo(EMAIL);
		assertThat(FolderType.DEFAULT_SENT_EMAIL_FOLDER.asSpecificationValue()).isEqualTo("5");
		assertThat(FolderType.DEFAULT_SENT_EMAIL_FOLDER.getPIMDataType()).isEqualTo(EMAIL);
		assertThat(FolderType.DEFAULT_OUTBOX_FOLDER.asSpecificationValue()).isEqualTo("6");
		assertThat(FolderType.DEFAULT_OUTBOX_FOLDER.getPIMDataType()).isEqualTo(EMAIL);
		assertThat(FolderType.DEFAULT_TASKS_FOLDER.asSpecificationValue()).isEqualTo("7");
		assertThat(FolderType.DEFAULT_TASKS_FOLDER.getPIMDataType()).isEqualTo(TASKS);
		assertThat(FolderType.DEFAULT_CALENDAR_FOLDER.asSpecificationValue()).isEqualTo("8");
		assertThat(FolderType.DEFAULT_CALENDAR_FOLDER.getPIMDataType()).isEqualTo(CALENDAR);
		assertThat(FolderType.DEFAULT_CONTACTS_FOLDER.asSpecificationValue()).isEqualTo("9");
		assertThat(FolderType.DEFAULT_CONTACTS_FOLDER.getPIMDataType()).isEqualTo(CONTACTS);
		assertThat(FolderType.DEFAULT_NOTES_FOLDER.asSpecificationValue()).isEqualTo("10");
		assertThat(FolderType.DEFAULT_NOTES_FOLDER.getPIMDataType()).isEqualTo(UNKNOWN);
		assertThat(FolderType.DEFAULT_JOURNAL_FOLDER.asSpecificationValue()).isEqualTo("11");
		assertThat(FolderType.DEFAULT_JOURNAL_FOLDER.getPIMDataType()).isEqualTo(UNKNOWN);
		assertThat(FolderType.USER_CREATED_EMAIL_FOLDER.asSpecificationValue()).isEqualTo("12");
		assertThat(FolderType.USER_CREATED_EMAIL_FOLDER.getPIMDataType()).isEqualTo(EMAIL);
		assertThat(FolderType.USER_CREATED_CALENDAR_FOLDER.asSpecificationValue()).isEqualTo("13");
		assertThat(FolderType.USER_CREATED_CALENDAR_FOLDER.getPIMDataType()).isEqualTo(CALENDAR);
		assertThat(FolderType.USER_CREATED_CONTACTS_FOLDER.asSpecificationValue()).isEqualTo("14");
		assertThat(FolderType.USER_CREATED_CONTACTS_FOLDER.getPIMDataType()).isEqualTo(CONTACTS);
		assertThat(FolderType.USER_CREATED_TASKS_FOLDER.asSpecificationValue()).isEqualTo("15");
		assertThat(FolderType.USER_CREATED_TASKS_FOLDER.getPIMDataType()).isEqualTo(TASKS);
		assertThat(FolderType.USER_CREATED_JOURNAL_FOLDER.asSpecificationValue()).isEqualTo("16");
		assertThat(FolderType.USER_CREATED_JOURNAL_FOLDER.getPIMDataType()).isEqualTo(UNKNOWN);
		assertThat(FolderType.USER_CREATED_NOTES_FOLDER.asSpecificationValue()).isEqualTo("17");
		assertThat(FolderType.USER_CREATED_NOTES_FOLDER.getPIMDataType()).isEqualTo(UNKNOWN);
		assertThat(FolderType.UNKNOWN_FOLDER_TYPE.asSpecificationValue()).isEqualTo("18");
		assertThat(FolderType.UNKNOWN_FOLDER_TYPE.getPIMDataType()).isEqualTo(UNKNOWN);
	}
}
