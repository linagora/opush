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
package org.obm.opush.command.moveitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;

import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Files;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.configuration.EmailConfiguration;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.ImapConnectionCounter;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.MailBackendTestModule;
import org.obm.opush.PendingQueriesLock;
import org.obm.opush.Users;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.MoveItemsStatus;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.FolderSyncKey;
import org.obm.sync.push.client.MoveItemsResponse;
import org.obm.sync.push.client.MoveItemsResponse.MoveResult;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.commands.MoveItemsCommand.Move;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;

@RunWith(GuiceRunner.class)
@GuiceModule(MailBackendTestModule.class)
public class MoveItemsHandlerTest {

	@Inject private	Users users;
	@Inject private	OpushServer opushServer;
	@Inject private GreenMail greenMail;
	@Inject private IMocksControl mocksControl;
	@Inject private PendingQueriesLock pendingQueries;
	@Inject private ImapConnectionCounter imapConnectionCounter;
	@Inject private Configuration configuration;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private FolderSnapshotDao folderSnapshotDao;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private IntegrationUserAccessUtils userAccessUtils;

	private GreenMailUser greenMailUser;
	private ImapHostManager imapHostManager;
	private OpushUser user;
	private String mailbox;
	private MailboxPath inboxPath;
	private CollectionId inboxCollectionId;
	private Folder inboxFolder;
	private MailboxPath trashPath;
	private CollectionId trashCollectionId;
	private Folder trashFolder;
	private CloseableHttpClient httpClient;

	@Before
	public void init() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
		user = users.jaures;
		greenMail.start();
		mailbox = user.user.getLoginAtDomain();
		greenMailUser = greenMail.setUser(mailbox, String.valueOf(user.password));
		imapHostManager = greenMail.getManagers().getImapHostManager();
		imapHostManager.createMailbox(greenMailUser, "Trash");

		inboxPath = MailboxPath.of(EmailConfiguration.IMAP_INBOX_NAME);
		inboxCollectionId = CollectionId.of(1234);
		inboxFolder = Folder.builder()
				.backendId(inboxPath)
				.collectionId(inboxCollectionId)
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("INBOX")
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.build();
		trashPath = MailboxPath.of(EmailConfiguration.IMAP_TRASH_NAME);
		trashCollectionId = CollectionId.of(1645);
		trashFolder = Folder.builder()
				.backendId(trashPath)
				.collectionId(trashCollectionId)
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("Trash")
				.folderType(FolderType.DEFAULT_DELETED_ITEMS_FOLDER)
				.build();

		FolderSyncKey syncKey = new FolderSyncKey("4fd6280c-cbaa-46aa-a859-c6aad00f1ef3");
		folderSnapshotDao.create(user.user, user.device, syncKey, 
				FolderSnapshot.nextId(2).folders(ImmutableSet.of(inboxFolder, trashFolder)));

		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
	}

	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		greenMail.stop();
		httpClient.close();
		Files.delete(configuration.dataDir);
	}

	@Test
	public void testMoveInboxToTrash() throws Exception {
		userAccessUtils.mockUsersAccess(user);
		
		mocksControl.replay();
		opushServer.start();
		sendEmailsToImapServer("email body data");
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		MoveItemsResponse response = opClient.moveItems(
				new Move(inboxCollectionId.serverId(1), inboxCollectionId, trashCollectionId));
		mocksControl.verify();

		MoveResult moveResult = Iterables.getOnlyElement(response.getMoveResults());
		assertThat(moveResult.status).isEqualTo(MoveItemsStatus.SUCCESS);
		assertThat(moveResult.srcMsgId).isEqualTo(inboxCollectionId.serverId(1));
		assertThat(moveResult.dstMsgId.getCollectionId()).isEqualTo(trashCollectionId);

		assertThat(pendingQueries.waitingClose(10, TimeUnit.SECONDS)).isTrue();
		assertThat(imapConnectionCounter.loginCounter.get()).isEqualTo(1);
		assertThat(imapConnectionCounter.closeCounter.get()).isEqualTo(1);
		assertThat(imapConnectionCounter.selectCounter.get()).isEqualTo(1);
		assertThat(imapConnectionCounter.listMailboxesCounter.get()).isEqualTo(1);
	}

	@Test
	public void testTwoMoveInboxToTrash() throws Exception {
		userAccessUtils.mockUsersAccess(user);
		
		mocksControl.replay();
		opushServer.start();
		sendEmailsToImapServer("email one", "email two");
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		MoveItemsResponse response = opClient.moveItems(
				new Move(inboxCollectionId.serverId(1), inboxCollectionId, trashCollectionId),
				new Move(inboxCollectionId.serverId(2), inboxCollectionId, trashCollectionId));
		mocksControl.verify();

		MoveResult moveResult = Iterables.get(response.getMoveResults(), 0);
		assertThat(moveResult.status).isEqualTo(MoveItemsStatus.SUCCESS);
		assertThat(moveResult.srcMsgId).isEqualTo(inboxCollectionId.serverId(1));
		assertThat(moveResult.dstMsgId).isEqualTo(trashCollectionId.serverId(1));
		
		MoveResult moveResult2 = Iterables.get(response.getMoveResults(), 1);
		assertThat(moveResult2.status).isEqualTo(MoveItemsStatus.SUCCESS);
		assertThat(moveResult2.srcMsgId).isEqualTo(inboxCollectionId.serverId(2));
		assertThat(moveResult2.dstMsgId).isEqualTo(trashCollectionId.serverId(2));
		
		assertThat(pendingQueries.waitingClose(10, TimeUnit.SECONDS)).isTrue();
		assertThat(imapConnectionCounter.loginCounter.get()).isEqualTo(1);
		assertThat(imapConnectionCounter.closeCounter.get()).isEqualTo(1);
		assertThat(imapConnectionCounter.selectCounter.get()).isEqualTo(1);
		assertThat(imapConnectionCounter.listMailboxesCounter.get()).isEqualTo(2);
	}

	@Ignore("Greenmail has replied that the command succeed")
	@Test
	public void testMoveUnexistingEmailFromInboxToTrash() throws Exception {
		int unexistingEmailId1 = 1561;
		
		userAccessUtils.mockUsersAccess(user);
		
		mocksControl.replay();
		opushServer.start();
		sendEmailsToImapServer("email body data");
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		MoveItemsResponse response = opClient.moveItems(
				new Move(inboxCollectionId.serverId(unexistingEmailId1), inboxCollectionId, trashCollectionId));
		mocksControl.verify();

		MoveResult moveResult = Iterables.getOnlyElement(response.getMoveResults());
		assertThat(moveResult.status).isEqualTo(MoveItemsStatus.INVALID_SOURCE_COLLECTION_ID);
		assertThat(moveResult.srcMsgId).isEqualTo(inboxCollectionId.serverId(unexistingEmailId1));
		assertThat(moveResult.dstMsgId).isNull();
	}
	
	private void sendEmailsToImapServer(String...bodies) throws InterruptedException {
		for (String body : bodies) {
			GreenMailUtil.sendTextEmail(mailbox, mailbox, "subject", body, greenMail.getSmtp().getServerSetup());
		}
		greenMail.waitForIncomingEmail(bodies.length);
	}
}
