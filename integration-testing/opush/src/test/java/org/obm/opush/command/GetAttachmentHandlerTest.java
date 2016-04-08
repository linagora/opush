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
package org.obm.opush.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Files;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.configuration.EmailConfiguration;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.MailBackendTestModule;
import org.obm.opush.Users;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.FolderSyncKey;
import org.obm.sync.push.client.Exceptions.UnexpectedHttpStatusException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;

@RunWith(GuiceRunner.class)
@GuiceModule(MailBackendTestModule.class)
public class GetAttachmentHandlerTest {

	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private FolderSnapshotDao folderSnapshotDao;
	@Inject private IntegrationUserAccessUtils userAccess;
	@Inject private GreenMail greenMail;
	
	private CloseableHttpClient httpClient;
	private MailboxPath inboxPath;
	private CollectionId collectionId;
	private Folder inboxFolder;
	private OpushUser user;
	private GreenMailUser greenMailUser;

	@Before
	public void init() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		user = users.jaures;
		greenMail.start();
		greenMailUser = greenMail.setUser(user.user.getLoginAtDomain(), String.valueOf(user.password));
		cassandraServer.start();

		inboxPath = MailboxPath.of(EmailConfiguration.IMAP_INBOX_NAME);
		collectionId = CollectionId.of(15105);
		inboxFolder = Folder.builder()
				.backendId(inboxPath)
				.collectionId(collectionId)
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("INBOX")
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.build();

		FolderSyncKey syncKey = new FolderSyncKey("4fd6280c-cbaa-46aa-a859-c6aad00f1ef3");
		folderSnapshotDao.create(users.jaures.user, users.jaures.device, syncKey, 
				FolderSnapshot.nextId(2).folders(ImmutableSet.of(inboxFolder)));

		userAccess.mockUsersAccess(user);
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration").anyTimes();
		
		mocksControl.replay();
		opushServer.start();

	}
	
	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		httpClient.close();
		Files.delete(configuration.dataDir);
	}

	@Test
	public void getAttachmentShouldFindExpectedMimePart() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/textAndAttachments.eml");
		
		byte[] postGetAttachment = testUtils
			.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient)
			.postGetAttachment(collectionId.asString() + "_1_3_aW1hZ2UvanBlZw==");
		
		mocksControl.verify();
		assertThat(postGetAttachment).hasSize(807);
	}
	
	@Test
	public void getAttachmentShouldFindExpectedMimePartAndTransfertEncodingBASE64() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/textAndAttachments.eml");
		
		byte[] postGetAttachment = testUtils
				.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient)
				.postGetAttachment(collectionId.asString() + "_1_3_aW1hZ2UvanBlZw==_QkFTRTY0");
		
		mocksControl.verify();
		assertThat(postGetAttachment).hasSize(597);
	}
	
	@Test
	public void getAttachmentShouldFindExpectedMimePartAndTransfertEncodingQuotedPrintable() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/textAndAttachments.eml");
		
		byte[] postGetAttachment = testUtils
				.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient)
				.postGetAttachment(collectionId.asString() + "_1_3_aW1hZ2UvanBlZw==_UVVPVEVELVBSSU5UQUJMRQ==");
		
		mocksControl.verify();
		assertThat(postGetAttachment).hasSize(807);
	}

	@Test(expected=UnexpectedHttpStatusException.class)
	public void getAttachmentShouldReturnError500WhenUnexistingCollection() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/textAndAttachments.eml");
		
		try {
		testUtils
			.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient)
			.postGetAttachment("999999_1_3_aW1hZ2UvanBlZw==");
		} catch(UnexpectedHttpStatusException e) {
			mocksControl.verify();
			assertThat(e.getStatus()).isEqualTo(500);
			throw e;
		}
	}

	@Test(expected=UnexpectedHttpStatusException.class)
	public void getAttachmentShouldReturnError500WhenBadlyFormattedAttachmenName() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/textAndAttachments.eml");
		
		try {
		testUtils
			.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient)
			.postGetAttachment("BAD-FORMAT");
		} catch(UnexpectedHttpStatusException e) {
			mocksControl.verify();
			assertThat(e.getStatus()).isEqualTo(500);
			throw e;
		}
	}

	@Test(expected=UnexpectedHttpStatusException.class)
	public void getAttachmentShouldReturnError500WhenUnfoundAttachment() throws Exception {
		testUtils.appendToINBOX(greenMailUser, "eml/textAndAttachments.eml");
		
		try {
		testUtils
			.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient)
			.postGetAttachment(collectionId.asString() + "_1_9999999999999_aW1hZ2UvanBlZw==");
		} catch(UnexpectedHttpStatusException e) {
			mocksControl.verify();
			assertThat(e.getStatus()).isEqualTo(500);
			throw e;
		}
	}
}
