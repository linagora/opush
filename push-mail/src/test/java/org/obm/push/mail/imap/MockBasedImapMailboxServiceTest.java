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
package org.obm.push.mail.imap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.User;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.configuration.OpushEmailConfiguration;
import org.obm.push.minig.imap.StoreClient;


public class MockBasedImapMailboxServiceTest {

	private UserDataRequest udr;
	private OpushEmailConfiguration emailConfiguration;
	private LinagoraImapClientProvider imapClientProvider;
	private StoreClient storeClient;
	
	@Before
	public void setUp() {
		String mailbox = "user@domain";
		char[] password = "password".toCharArray();
	    udr = new UserDataRequest(
				new Credentials(User.Factory.create()
						.createUser(mailbox, mailbox, null), password), null, null);

		emailConfiguration = createMock(OpushEmailConfiguration.class);
		imapClientProvider = createMock(LinagoraImapClientProvider.class);
		storeClient = createMock(StoreClient.class);
	}
	
 	@Test
	public void testParseSpecificINBOXCase() throws Exception {
		String userINBOXFolder = "INBOX";

		expect(imapClientProvider.getImapClient(udr)).andReturn(storeClient);
		expect(storeClient.findMailboxNameWithServerCase(userINBOXFolder))
			.andReturn(userINBOXFolder);
		
		replay(emailConfiguration, imapClientProvider, storeClient);
		LinagoraMailboxService emailManager = new LinagoraMailboxService(
				emailConfiguration, imapClientProvider);

		String parsedMailbox = emailManager.parseMailBoxName(udr, MailboxPath.of(userINBOXFolder));
		verify(emailConfiguration, imapClientProvider, storeClient);
		
		assertThat(parsedMailbox).isEqualTo(OpushEmailConfiguration.IMAP_INBOX_NAME);
	}

	@Test
	public void testParseSpecificINBOXCaseIsntCaseSensitive() throws Exception {
		String userINBOXFolder = "InBoX";
		String serverINBOXFolder = "INBOX";

		expect(imapClientProvider.getImapClient(udr)).andReturn(storeClient);
		expect(storeClient.findMailboxNameWithServerCase(userINBOXFolder)).andReturn(serverINBOXFolder);

		replay(emailConfiguration, imapClientProvider, storeClient);
		LinagoraMailboxService emailManager = new LinagoraMailboxService(
				emailConfiguration, imapClientProvider);

		String parsedMailbox = emailManager.parseMailBoxName(udr, MailboxPath.of(userINBOXFolder));
		verify(emailConfiguration, imapClientProvider, storeClient);
		
		assertThat(parsedMailbox).isEqualTo(OpushEmailConfiguration.IMAP_INBOX_NAME);
	}

	@Test
	public void testParseINBOXWithOtherFolderEndingByINBOX() throws Exception {
		String folderEndingByINBOX = "userFolder" + OpushEmailConfiguration.IMAP_INBOX_NAME;
		
		expect(imapClientProvider.getImapClient(udr)).andReturn(storeClient);
		expect(storeClient.findMailboxNameWithServerCase(folderEndingByINBOX)).andReturn(folderEndingByINBOX);

		replay(emailConfiguration, imapClientProvider, storeClient);
		LinagoraMailboxService emailManager = new LinagoraMailboxService(
				emailConfiguration, imapClientProvider);

		String parsedMailbox = emailManager.parseMailBoxName(udr, MailboxPath.of(folderEndingByINBOX));
		verify(emailConfiguration, imapClientProvider, storeClient);
		
		assertThat(parsedMailbox).isEqualTo(folderEndingByINBOX);
	}
}
