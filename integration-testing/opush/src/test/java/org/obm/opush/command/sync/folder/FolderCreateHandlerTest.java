/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015  Linagora
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

import java.util.Collection;
import java.util.Date;

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
import org.obm.push.OpushServer;
import org.obm.push.bean.FolderCreateStatus;
import org.obm.push.bean.FolderSyncStatus;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderCreateRequest;
import org.obm.push.bean.change.hierarchy.FolderCreateResponse;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.calendar.CalendarBackend;
import org.obm.push.configuration.OpushEmailConfiguration;
import org.obm.push.contacts.ContactsBackend;
import org.obm.push.mail.MailBackend;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.FolderSyncResponse;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.state.FolderSyncKeyFactory;
import org.obm.push.task.TaskBackend;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.items.FolderChanges;
import org.obm.sync.push.client.OPClient;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;

@RunWith(GuiceRunner.class)
@GuiceModule(SyncHandlerWithBackendTestModule.class)
public class FolderCreateHandlerTest {
	
	private static final int NON_EXISTING_PARENT = 89796654;
	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private GreenMail greenMail;
	@Inject private FolderSnapshotDao folderSnapshotDao;
	@Inject private SyncKeyTestUtils syncKeyTestUtils;
	@Inject private FolderSyncKeyFactory folderSyncKeyFactory;
	@Inject private BookClient bookClient;
	@Inject private HierarchyChangesTestUtils hierarchyChangesTestUtils;
	
	@Inject CalendarBackend calendarBackend;
	@Inject TaskBackend taskBackend;
	@Inject ContactsBackend contactsBackend;
	@Inject MailBackend mailBackend;
	
	private OpushUser user;
	private CloseableHttpClient httpClient;
	private String mailbox;
	private GreenMailUser greenmailUser;
	private ImapHostManager imapHostManager;

	@Before
	public void init() throws Exception {
		user = users.jaures;
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
		cassandraServer.start();
		greenMail.start();
		mailbox = user.user.getLoginAtDomain();
		greenmailUser = greenMail.setUser(mailbox, String.valueOf(user.password));
		httpClient = HttpClientBuilder.create().build();
		imapHostManager = greenMail.getManagers().getImapHostManager();
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
	public void createContactFolderShouldReturnError() throws Exception {
		FolderSyncKey requestSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey nextSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");

		folderSnapshotDao.create(user.user, user.device, requestSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of("folder"))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("folder")
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build())));
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(nextSyncKey);
		
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderCreateResponse folderCreateResponse = 
				opClient.folderCreate(FolderCreateRequest.builder()
						.folderSyncKey(requestSyncKey)
						.folderDisplayName("contact")
						.folderParentId(CollectionId.ROOT)
						.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER)
						.build());
		
		mocksControl.verify();
		
		assertThat(folderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.ALREADY_EXISTS);
	}
	
	@Test
	public void createCalendarFolderShouldReturnError() throws Exception {
		FolderSyncKey requestSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey nextSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");
		

		folderSnapshotDao.create(user.user, user.device, requestSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of("folder"))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("same folder")
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build())));
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(nextSyncKey);
		
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderCreateResponse folderCreateResponse = 
				opClient.folderCreate(FolderCreateRequest.builder()
						.folderSyncKey(requestSyncKey)
						.folderDisplayName("calendar")
						.folderParentId(CollectionId.ROOT)
						.folderType(FolderType.DEFAULT_CALENDAR_FOLDER)
						.build());
		
		assertThat(folderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.ALREADY_EXISTS);
	}
	
	@Test
	public void createFolderShouldReturnStatusUnkownSyncKeyWhenNoSnapshotFound() throws Exception {
		FolderSyncKey requestSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey nextSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");

		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(nextSyncKey);
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderCreateResponse folderCreateResponse = 
				opClient.folderCreate(FolderCreateRequest.builder()
						.folderSyncKey(requestSyncKey)
						.folderDisplayName("email")
						.folderParentId(CollectionId.ROOT)
						.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
						.build());
		
		mocksControl.verify();
		
		assertThat(folderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.INVALID_SYNC_KEY);
	}
	
	@Test
	public void createFolderShouldReturnStatusServerErrorWhenUnexpectedError() throws Exception {
		FolderSyncKey newSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");

		folderSnapshotDao.create(user.user, user.device, newSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of("folder"))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("folder")
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build())));
		
		userAccessUtils.mockUsersAccess(user);
		expect(folderSyncKeyFactory.randomSyncKey()).andThrow(new RuntimeException());
		
		
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderCreateResponse folderCreateResponse = 
				opClient.folderCreate(FolderCreateRequest.builder()
						.folderSyncKey(newSyncKey)
						.folderDisplayName("email")
						.folderParentId(CollectionId.ROOT)
						.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
						.build());
		
		mocksControl.verify();
		
		assertThat(folderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.SERVER_ERROR);
	}
	
	@Test
	public void createRootEmailFolderShouldReturnStatusOK() throws Exception {
		FolderSyncKey requestSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey nextSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");

		folderSnapshotDao.create(user.user, user.device, requestSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of("folder"))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("folder")
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build())));
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(nextSyncKey);
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderCreateResponse folderCreateResponse = 
				opClient.folderCreate(FolderCreateRequest.builder()
						.folderSyncKey(requestSyncKey)
						.folderDisplayName("email")
						.folderParentId(CollectionId.ROOT)
						.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
						.build());
		
		mocksControl.verify();
		
		Collection<MailFolder> mailfolders = imapHostManager.listMailboxes(greenmailUser, "*");
		
		assertThat(mailfolders).extracting("name").contains("email");
		assertThat(folderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.OK);
	}
	
	@Test
	public void folderShouldBeCreatedAsSubscribed() throws Exception {
		FolderSyncKey firstSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey secondSyncKey = new FolderSyncKey("7dd9234e-ce66-4e39-9c38-6f95c4602bb5");
		FolderSyncKey thirdSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");
		
		imapHostManager.createMailbox(greenmailUser, "folder");
		imapHostManager.subscribe(greenmailUser, "folder");

		folderSnapshotDao.create(user.user, user.device, firstSyncKey, 
			FolderSnapshot.nextId(5).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of("folder"))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("folder")
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build())));

		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondSyncKey, thirdSyncKey);
		
		hierarchyChangesTestUtils.expectEmptyTaskFolders();
		expect(bookClient.listAddressBooksChanged(anyObject(AccessToken.class), anyObject(Date.class)))
			.andReturn(FolderChanges.builder().build());
		
		mocksControl.replay();
		
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		opClient.folderCreate(FolderCreateRequest.builder()
			.folderSyncKey(firstSyncKey)
			.folderDisplayName("email")
			.folderParentId(CollectionId.ROOT)
			.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
			.build());
		
		FolderSyncResponse folderSyncResponse = opClient.folderSync(secondSyncKey);
		
		mocksControl.verify();
		
		assertThat(folderSyncResponse.getNewSyncKey()).isEqualTo(thirdSyncKey);
		assertThat(folderSyncResponse.getStatus()).isEqualTo(FolderSyncStatus.OK);
		assertThat(folderSyncResponse.getCollectionsDeleted()).isEmpty();
	}
	
	@Test
	public void createEmailFolderWithParentThatExistsShouldReturnStatusOK() throws Exception {
		FolderSyncKey requestSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey nextSyncKey = new FolderSyncKey("12342234-1234-1234-1234-123456123456");

		imapHostManager.createMailbox(greenmailUser, "folder");
		imapHostManager.subscribe(greenmailUser, "folder");
		
		folderSnapshotDao.create(user.user, user.device, requestSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of("folder"))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("folder")
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build())));
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(nextSyncKey);
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		
		FolderCreateResponse folderCreateResponse = 
				opClient.folderCreate(FolderCreateRequest.builder()
						.folderSyncKey(requestSyncKey)
						.folderDisplayName("email")
						.folderParentId(CollectionId.of(4))
						.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
						.build());
		
		mocksControl.verify();
		
		assertThat(folderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.OK);
	}
	
	@Test
	public void createEmailFolderWithParentThatDoesntExistShouldReturnStatusParentNotFound() 
			throws Exception {
		FolderSyncKey requestSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey nextSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");

		folderSnapshotDao.create(user.user, user.device, requestSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of("folder"))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("same folder")
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build())));
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(nextSyncKey);
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderCreateResponse folderCreateResponse = 
				opClient.folderCreate(FolderCreateRequest.builder()
						.folderSyncKey(requestSyncKey)
						.folderDisplayName("email")
						.folderParentId(CollectionId.of(NON_EXISTING_PARENT))
						.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
						.build());
		
		mocksControl.verify();
		
		assertThat(folderCreateResponse.getStatus())
			.isEqualTo(FolderCreateStatus.PARENT_FOLDER_NOT_FOUND);
	}
	
	@Test
	public void createTwiceAFolderShouldReturnAStatusAlreadyExist() throws Exception {
		FolderSyncKey firstSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey secondSyncKey = new FolderSyncKey("12342234-1234-1234-1234-123456123456");
		FolderSyncKey thirdSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");

		folderSnapshotDao.create(user.user, user.device, firstSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of("folder"))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("folder")
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build())));
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondSyncKey, thirdSyncKey);
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		
		opClient.folderCreate(FolderCreateRequest.builder()
				.folderSyncKey(firstSyncKey)
				.folderDisplayName("email")
				.folderParentId(CollectionId.ROOT)
				.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
				.build());

		FolderCreateResponse sameFolderCreateResponse = 
				opClient.folderCreate(FolderCreateRequest.builder()
						.folderSyncKey(secondSyncKey)
						.folderDisplayName("email")
						.folderParentId(CollectionId.ROOT)
						.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
						.build());

		
		mocksControl.verify();
	
		assertThat(sameFolderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.ALREADY_EXISTS);
	}

	@Test
	public void createFolderWithNameAlreadyUsedButWithAnOtherParentFolderShouldNotReturnStatusAlreadyExist() 
			throws Exception {
		FolderSyncKey firstSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey secondSyncKey = new FolderSyncKey("192f9742-259f-4f90-b0da-88e5d1a6cd0a");
		FolderSyncKey thirdSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");
		
		imapHostManager.createMailbox(greenmailUser, "folder");
		imapHostManager.subscribe(greenmailUser, "folder");

		folderSnapshotDao.create(user.user, user.device, firstSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of("folder"))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("folder")
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build())));
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondSyncKey, thirdSyncKey);
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		
		opClient.folderCreate(FolderCreateRequest.builder()
				.folderSyncKey(firstSyncKey)
				.folderDisplayName("email")
				.folderParentId(CollectionId.ROOT)
				.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
				.build());
		

		FolderCreateResponse sameFolderNameWithAnOtherParentFolderCreateResponse = 
				opClient.folderCreate(FolderCreateRequest.builder()
						.folderSyncKey(secondSyncKey)
						.folderDisplayName("email")
						.folderParentId(CollectionId.of(4))
						.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
						.build());

		
		mocksControl.verify();
	
		assertThat(sameFolderNameWithAnOtherParentFolderCreateResponse.getStatus())
			.isEqualTo(FolderCreateStatus.OK);
	}
	
	@Test
	public void createFolderShouldSucceedWhenItAlreadyExistAsNotSubscribed() throws Exception {
		FolderSyncKey firstSyncKey = new FolderSyncKey("12341234-1234-1234-1234-123456123456");
		FolderSyncKey secondSyncKey = new FolderSyncKey("12342234-1234-1234-1234-123456123456");
		FolderSyncKey thirdSyncKey = new FolderSyncKey("d58ea559-d1b8-4091-8ba5-860e6fa54875");
		String notSubscribedFolder = "unsubscribed-folder";
		int nextFolderId = 2;
		
		imapHostManager.createMailbox(greenmailUser, notSubscribedFolder);
		imapHostManager.unsubscribe(greenmailUser, notSubscribedFolder);
		
		folderSnapshotDao.create(user.user, user.device, firstSyncKey, 
			FolderSnapshot.nextId(nextFolderId).folders(ImmutableSet.of(Folder.builder()
				.backendId(MailboxPath.of(OpushEmailConfiguration.IMAP_INBOX_NAME))
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName(OpushEmailConfiguration.IMAP_INBOX_NAME)
				.collectionId(CollectionId.of(4))
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.build())));
		
		hierarchyChangesTestUtils.expectEmptyTaskFolders();
		expect(bookClient.listAddressBooksChanged(anyObject(AccessToken.class), anyObject(Date.class)))
			.andReturn(FolderChanges.builder().build());
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondSyncKey, thirdSyncKey);
		mocksControl.replay();
		opushServer.start();
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		FolderCreateResponse folderCreateResponse = opClient.folderCreate(FolderCreateRequest.builder()
				.folderSyncKey(firstSyncKey)
				.folderDisplayName(notSubscribedFolder)
				.folderParentId(CollectionId.ROOT)
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.build());
		
		FolderSyncResponse folderSyncResponse = opClient.folderSync(secondSyncKey);
		
		mocksControl.verify();
		
		assertThat(folderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.OK);
		assertThat(folderCreateResponse.getCollectionId()).isEqualTo(CollectionId.of(nextFolderId));
		assertThat(folderSyncResponse.getNewSyncKey()).isEqualTo(thirdSyncKey);
		assertThat(folderSyncResponse.getStatus()).isEqualTo(FolderSyncStatus.OK);
		assertThat(SyncTestUtils.getCollectionWithId(folderSyncResponse, CollectionId.of(nextFolderId)).isPresent()).isFalse();
		
	}
}
