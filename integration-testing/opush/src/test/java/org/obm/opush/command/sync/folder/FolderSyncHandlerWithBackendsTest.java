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
package org.obm.opush.command.sync.folder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.Date;
import java.util.List;

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
import org.obm.opush.HierarchyChangesTestUtils;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.SyncKeyTestUtils;
import org.obm.opush.Users;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.command.sync.SyncHandlerWithBackendTestModule;
import org.obm.opush.command.sync.SyncTestUtils;
import org.obm.opush.env.CassandraServer;
import org.obm.opush.env.OpushStaticConfiguration;
import org.obm.push.OpushServer;
import org.obm.push.bean.FolderSyncStatus;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.FolderSyncResponse;
import org.obm.push.state.FolderSyncKey;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.items.FolderChanges;

import com.google.inject.Inject;
import com.icegreen.greenmail.util.GreenMail;

@RunWith(GuiceRunner.class)
@GuiceModule(SyncHandlerWithBackendTestModule.class)
public class FolderSyncHandlerWithBackendsTest {

	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private GreenMail greenMail;
	@Inject private SyncKeyTestUtils syncKeyTestUtils;
	@Inject private SyncTestUtils syncTestUtils;
	@Inject private BookClient bookClient;
	@Inject private HierarchyChangesTestUtils hierarchyChangesTestUtils;
	@Inject private OpushStaticConfiguration.Email emailConfiguration;
	
	private OpushUser user;
	private CloseableHttpClient httpClient;
	private String mailbox;

	@Before
	public void init() throws Exception {
		user = users.jaures;
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
		cassandraServer.start();
		greenMail.start();
		mailbox = user.user.getLoginAtDomain();
		greenMail.setUser(mailbox, String.valueOf(user.password));
		httpClient = HttpClientBuilder.create().build();
	}
	
	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		greenMail.stop();
		Files.delete(configuration.dataDir);
		httpClient.close();	
	}
	
	@Test
	public void testInitialFolderSyncWhenSpecialSettings() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		FolderSyncKey newGeneratedSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		
		emailConfiguration.configuration.imapMailboxTrash = "INBOX/Trash";
		emailConfiguration.configuration.imapMailboxSent = "INBOX/Sent";
		emailConfiguration.configuration.imapMailboxDraft = "my drafts";
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(newGeneratedSyncKey);
		hierarchyChangesTestUtils.expectEmptyTaskFolders();
		expect(bookClient.listAddressBooksChanged(anyObject(AccessToken.class), anyObject(Date.class)))
			.andReturn(FolderChanges.builder().build());
		
		mocksControl.replay();
		opushServer.start();
		FolderSyncResponse folderSyncResponse = testUtils
				.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient)
				.folderSync(initialSyncKey);
		mocksControl.verify();
		
		assertThat(folderSyncResponse.getNewSyncKey()).isEqualTo(newGeneratedSyncKey);
		assertThat(folderSyncResponse.getStatus()).isEqualTo(FolderSyncStatus.OK);
		assertThat(folderSyncResponse.getCollectionsAddedAndUpdated()).hasSize(5);
		
		List<CollectionChange> added = folderSyncResponse.getCollectionsAddedAndUpdated();
		CollectionChange inbox = syncTestUtils.lookupByType(added, FolderType.DEFAULT_INBOX_FOLDER);
		assertThat(inbox.getParentCollectionId()).isEqualTo(CollectionId.ROOT);
		assertThat(inbox.getDisplayName()).isEqualTo(EmailConfiguration.IMAP_INBOX_NAME);

		CollectionChange trash = syncTestUtils.lookupByType(added, FolderType.DEFAULT_DELETED_ITEMS_FOLDER);
		assertThat(trash.getParentCollectionId()).isEqualTo(inbox.getCollectionId());
		assertThat(trash.getDisplayName()).isEqualTo(EmailConfiguration.IMAP_TRASH_NAME);

		CollectionChange sent = syncTestUtils.lookupByType(added, FolderType.DEFAULT_SENT_EMAIL_FOLDER);
		assertThat(sent.getParentCollectionId()).isEqualTo(inbox.getCollectionId());
		assertThat(sent.getDisplayName()).isEqualTo(EmailConfiguration.IMAP_SENT_NAME);

		CollectionChange draft = syncTestUtils.lookupByType(added, FolderType.DEFAULT_DRAFTS_FOLDER);
		assertThat(draft.getParentCollectionId()).isEqualTo(CollectionId.ROOT);
		assertThat(draft.getDisplayName()).isEqualTo("my drafts");
	}
}
