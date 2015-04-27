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
package org.obm.opush.command.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.obm.DateUtils.date;
import static org.obm.push.bean.FilterType.THREE_DAYS_BACK;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.naming.NoPermissionException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.easymock.IMocksControl;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.ProtocolVersion;
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.Device;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSEmailBodyType;
import org.obm.push.bean.MSEmailHeader;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionCommandRequest;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemChangesBuilder;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.bean.ms.MSEmailBody;
import org.obm.push.calendar.CalendarBackend;
import org.obm.push.contacts.ContactsBackend;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.HierarchyChangedException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.mail.MailBackend;
import org.obm.push.mail.exception.FilterTypeChangedException;
import org.obm.push.protocol.bean.ClientSyncRequest;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.FolderSyncResponse;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.FolderSnapshotDao.FolderSnapshotNotFoundException;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.DeviceDao;
import org.obm.push.store.DeviceDao.PolicyStatus;
import org.obm.push.task.TaskBackend;
import org.obm.push.utils.DateUtils;
import org.obm.push.utils.SerializableInputStream;
import org.obm.sync.push.client.HttpRequestException;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.commands.Sync;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

@GuiceModule(SyncHandlerTestModule.class)
@RunWith(GuiceRunner.class)
public class SyncHandlerTest {

	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private IMocksControl mocksControl;
	@Inject private SyncDecoder decoder;
	@Inject private Sync.Builder syncBuilder;
	@Inject private IContentsExporter contentsExporter;
	@Inject private IContentsImporter contentsImporter;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private HierarchyChangesTestUtils hierarchyChangesTestUtils;
	@Inject private SyncKeyTestUtils syncKeyTestUtils;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private SyncTestUtils syncTestUtils;
	@Inject private CollectionDao collectionDao;
	@Inject private FolderSnapshotDao folderSnapshotDao;
	@Inject private DeviceDao deviceDao;

	@Inject CalendarBackend calendarBackend;
	@Inject TaskBackend taskBackend;
	@Inject ContactsBackend contactsBackend;
	@Inject MailBackend mailBackend;
	
	private List<OpushUser> userAsList;
	private CloseableHttpClient httpClient;
	private Folder inboxFolder;
	private CollectionId inboxCollectionId;
	private FolderSyncKey knownFolderSyncKey;

	@Before
	public void init() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
		userAsList = Arrays.asList(users.jaures);
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");

		inboxCollectionId = CollectionId.of(1);
		inboxFolder = Folder.builder()
			.collectionId(inboxCollectionId)
			.backendId(MailboxPath.of(EmailConfiguration.IMAP_INBOX_NAME))
			.displayName(EmailConfiguration.IMAP_INBOX_NAME)
			.folderType(FolderType.DEFAULT_INBOX_FOLDER)
			.parentBackendIdOpt(Optional.<BackendId>absent())
			.build();
		
		knownFolderSyncKey = new FolderSyncKey("c8355d6c-9325-490a-87ec-2522b2e23b99");
		folderSnapshotDao.create(users.jaures.user, users.jaures.device, knownFolderSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(inboxFolder)));
	}
	
	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		httpClient.close();
	}

	@Test
	public void testSyncDefaultMailFolderUnchange() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey syncEmailSyncKey = new SyncKey("1");
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();

		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, delta, userAsList);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);

		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, inbox.getCollectionId(), THREE_DAYS_BACK, 150);

		syncTestUtils.checkMailFolderHasNoChange(syncEmailResponse, inbox.getCollectionId());
	}
	
	@Test
	public void testSyncWithWaitReturnsServerError() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		FolderSyncKey firstSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		SyncKey syncEmailSyncKey = new SyncKey("1");
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstSyncKey);
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, delta, userAsList);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);
		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		SyncResponse syncEmailResponse = opClient.syncEmailWithWait(decoder, syncEmailSyncKey, inbox.getCollectionId(), THREE_DAYS_BACK, 150);

		assertThat(syncEmailResponse.getStatus()).isEqualTo(SyncStatus.SERVER_ERROR);
	}
	
	@Test
	public void testSyncOneInboxMail() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		FolderSyncKey firstSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		SyncKey syncEmailSyncKey = new SyncKey("13424");
		CollectionId syncEmailCollectionId = CollectionId.of(432);

		MSEmail applicationData = applicationData("text", MSEmailBodyType.PlainText);
		DataDelta delta = DataDelta.builder()
			.changes(new ItemChangesBuilder()
					.addItemChange(
						ItemChange.builder().serverId(syncEmailCollectionId.serverId(0))
							.data(applicationData))
					.build())
			.syncDate(new Date())
			.syncKey(syncEmailSyncKey)
			.build();

		syncKeyTestUtils.mockNextGeneratedSyncKey(firstSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2342"));
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, delta, userAsList);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);
		
		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, inbox.getCollectionId(), THREE_DAYS_BACK, 150);

		syncTestUtils.checkMailFolderHasAddItems(syncEmailResponse, inbox.getCollectionId(),
				ItemChange.builder()
					.serverId(syncEmailCollectionId.serverId(0))
					.isNew(true)
					.data(applicationData)
					.build());
	}

	@Test
	public void testSyncTwoMailButOneDisappearing() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		SyncKey syncEmailSyncKey = new SyncKey("13424");

		testUtils.expectUserCollectionsNeverChange();
		syncTestUtils.mockCollectionDaoForEmailSync(syncEmailSyncKey);
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2342"));
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		userAccessUtils.mockUsersAccess(userAsList);
		
		expect(contentsExporter.getChanged(
				anyObject(UserDataRequest.class),
				anyObject(ItemSyncState.class),
				anyObject(AnalysedSyncCollection.class),
				anyObject(SyncKey.class)))
				.andThrow(new ItemNotFoundException());
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);
		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, inbox.getCollectionId(), FilterType.THREE_DAYS_BACK, 100);
		
		assertThat(syncEmailResponse).isNotNull();
		assertThat(syncEmailResponse.getStatus()).isEqualTo(SyncStatus.CONVERSATION_ERROR_OR_INVALID_ITEM);
	}
	
	@Test
	public void testSyncTwoInboxMails() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		SyncKey syncEmailSyncKey = new SyncKey("13424");
		CollectionId syncEmailCollectionId = CollectionId.of(432);
		
		MSEmail applicationData = applicationData("text", MSEmailBodyType.PlainText);
		DataDelta delta = DataDelta.builder()
			.changes(new ItemChangesBuilder()
					.addItemChange(
						ItemChange.builder().serverId(syncEmailCollectionId.serverId(0))
							.data(applicationData))
					.addItemChange(
						ItemChange.builder().serverId(syncEmailCollectionId.serverId(1))
							.data(applicationData))
					.build())
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();

		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2342"));
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, delta, userAsList);

		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);
		
		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, inbox.getCollectionId(), THREE_DAYS_BACK, 150);

		syncTestUtils.checkMailFolderHasAddItems(syncEmailResponse, inbox.getCollectionId(), 
				ItemChange.builder()
					.serverId(syncEmailCollectionId.serverId(0))
					.isNew(true)
					.data(applicationData)
					.build(),
				ItemChange.builder().serverId(syncEmailCollectionId.serverId(1))
					.isNew(true)
					.data(applicationData)
					.build()); 
	}

	@Test
	public void testSyncOneInboxDeletedMail() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey syncEmailSyncKey = new SyncKey("13424");
		CollectionId syncEmailCollectionId = CollectionId.of(432);
		
		DataDelta delta = DataDelta.builder()
			.deletions(ImmutableList.of(
					ItemDeletion.builder().serverId(syncEmailCollectionId.serverId(0)).build()))
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();

		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, delta, userAsList);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);
		
		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, inbox.getCollectionId(), THREE_DAYS_BACK, 150);

		syncTestUtils.checkMailFolderHasDeleteItems(syncEmailResponse, inbox.getCollectionId(),
				ItemDeletion.builder().serverId(syncEmailCollectionId.serverId(0)).build());
	}

	@Test
	public void testSyncInboxOneNewOneDeletedMail() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		SyncKey syncEmailSyncKey = new SyncKey("13424");
		CollectionId syncEmailCollectionId = CollectionId.of(432);
		MSEmail applicationData = applicationData("text", MSEmailBodyType.PlainText);
		DataDelta delta = DataDelta.builder()
			.changes(new ItemChangesBuilder()
					.addItemChange(
						ItemChange.builder().serverId(syncEmailCollectionId.serverId(123))
							.data(applicationData))
					.build())
			.deletions(ImmutableList.of(
					ItemDeletion.builder().serverId(syncEmailCollectionId.serverId(122)).build()))
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();

		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("23455"));
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, delta, userAsList);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);

		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, inbox.getCollectionId(), THREE_DAYS_BACK, 150);

		syncTestUtils.checkMailFolderHasItems(syncEmailResponse, inbox.getCollectionId(), 
				ImmutableSet.of(ItemChange.builder()
					.serverId(syncEmailCollectionId.serverId(123))
					.isNew(true)
					.data(applicationData)
					.build()),
				ImmutableSet.of(ItemDeletion.builder().serverId(syncEmailCollectionId.serverId(122)).build()));
	}

	@Test
	public void testSyncInboxFetchIdsNotEmpty() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey syncEmailSyncKey = new SyncKey("13424");
		CollectionId syncEmailCollectionId = CollectionId.of(1);
		ServerId serverId = syncEmailCollectionId.serverId(123);
		MSEmail applicationData = applicationData("text", MSEmailBodyType.PlainText);
		ItemChange itemChange = ItemChange.builder().serverId(serverId)
			.data(applicationData)
			.build();
		List<ItemChange> itemChanges = ImmutableList.of(itemChange);
		DataDelta delta = DataDelta.builder()
			.changes(itemChanges)
			.deletions(ImmutableList.of(
					ItemDeletion.builder().serverId(syncEmailCollectionId.serverId(122)).build()))
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();

		UserDataRequest userDataRequest = new UserDataRequest(users.jaures.credentials, 
				"Sync", 
				users.jaures.device);
		
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		testUtils.expectContentExporterFetching(userDataRequest, itemChange);
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, delta, userAsList);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);
		
		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		SyncResponse syncEmailResponse = opClient.run(syncBuilder
			.request(ClientSyncRequest.builder()
				.addCollection(AnalysedSyncCollection.builder().collectionId(inbox.getCollectionId())
					.syncKey(syncEmailSyncKey).dataType(PIMDataType.EMAIL)
					.command(SyncCollectionCommandRequest.builder().type(SyncCommand.FETCH).serverId(serverId).build())
					.build())
				.build())
			.build());

		syncTestUtils.checkMailFolderHasFetchItems(syncEmailResponse, inbox.getCollectionId(), syncEmailCollectionId.serverId(123));
		SyncCollectionResponse collection = syncTestUtils.getCollectionWithId(syncEmailResponse, inbox.getCollectionId());
		assertThat(collection.getItemDeletions()).isEmpty();
	}
	
	@Test
	public void testSyncWithUnknownSyncKeyReturnsInvalidSyncKeyStatus() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey secondSyncKey = new SyncKey("456");
		Date initialUpdateStateDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstItemSyncState = ItemSyncState.builder().syncKey(initialSyncKey).syncDate(initialUpdateStateDate).build();
		
		userAccessUtils.mockUsersAccess(userAsList);

		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2342"));
		expect(collectionDao.findItemStateForKey(secondSyncKey)).andReturn(null);
		expect(collectionDao.updateState(anyObject(Device.class), anyObject(CollectionId.class), anyObject(SyncKey.class), anyObject(Date.class)))
			.andReturn(firstItemSyncState)
			.anyTimes();
		collectionDao.resetCollection(users.jaures.device, inboxCollectionId);
		expectLastCall();
		contentsExporter.initialize(users.jaures.deviceId, inboxCollectionId, PIMDataType.EMAIL, THREE_DAYS_BACK, new SyncKey("2342"));
		expectLastCall();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		opClient.syncEmail(decoder, initialSyncKey, inboxCollectionId, THREE_DAYS_BACK, 100);
		SyncResponse syncResponse = opClient.syncEmail(decoder, secondSyncKey, inboxCollectionId, THREE_DAYS_BACK, 100);
		mocksControl.verify();

		SyncCollectionResponse inboxResponse = syncTestUtils.getCollectionWithId(syncResponse, inboxCollectionId);
		assertThat(inboxResponse.getStatus()).isEqualTo(SyncStatus.INVALID_SYNC_KEY);
	}

	@Test
	public void testSyncWithoutOptionsAndNoOptionsInCacheTakeThePreviousOne() throws Exception {
		OpushUser user = users.jaures;
		CollectionId collectionId = CollectionId.of(1);
		FolderSyncKey initialFolderSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey secondSyncKey = new SyncKey("13424");

		SyncCollectionOptions toStoreOptions = SyncCollectionOptions.builder()
				.filterType(THREE_DAYS_BACK)
				.conflict(1)
				.build();
		ItemSyncState secondRequestSyncState = ItemSyncState.builder()
				.id(4)
				.syncKey(secondSyncKey)
				.syncDate(date("2012-10-10T16:22:53"))
				.build();

		userAccessUtils.mockUsersAccess(userAsList);

		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"), new SyncKey("3345"));
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		expect(contentsExporter.getChanged(
				anyObject(UserDataRequest.class),
				anyObject(ItemSyncState.class),
				anyObject(AnalysedSyncCollection.class),
				anyObject(SyncKey.class)))
			.andReturn(DataDelta.newEmptyDelta(secondRequestSyncState.getSyncDate(), secondRequestSyncState.getSyncKey()));
		contentsExporter.initialize(users.jaures.deviceId, collectionId, PIMDataType.EMAIL, THREE_DAYS_BACK, new SyncKey("2345"));
		expectLastCall();
		
		expect(collectionDao.findItemStateForKey(secondSyncKey)).andReturn(secondRequestSyncState);
		expect(collectionDao.updateState(anyObject(Device.class), anyObject(CollectionId.class),
				anyObject(SyncKey.class), anyObject(Date.class))).andReturn(secondRequestSyncState).times(2);
		collectionDao.resetCollection(user.device, collectionId);
		expectLastCall();

		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialFolderSyncKey);
		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		
		opClient.syncEmail(decoder, initialSyncKey, inbox.getCollectionId(), toStoreOptions.getFilterType(), 25);
		SyncResponse syncWithoutOptions = opClient.syncWithoutOptions(decoder, secondSyncKey, inbox.getCollectionId());
		mocksControl.verify();

		syncTestUtils.checkMailFolderHasNoChange(syncWithoutOptions, inbox.getCollectionId());
	}

	public void testPartialSyncWhenNoPreviousSendError13() throws Exception {
		FolderSyncKey initialFolderSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		
		userAccessUtils.mockUsersAccess(userAsList);
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		opClient.folderSync(initialFolderSyncKey);
		SyncResponse partialSyncResponse = opClient.partialSync(decoder);
		
		assertThat(partialSyncResponse.getStatus()).isEqualTo(SyncStatus.PARTIAL_REQUEST);
	}
	
	@Ignore("We don't support partial request yet")
	@Test
	public void testPartialSyncWhenValidPreviousSync() throws Exception {
		FolderSyncKey initialFolderSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;

		SyncKey initialSyncKey = new SyncKey("1234");
		CollectionId syncEmailCollectionId = CollectionId.of(12);
		DataDelta emptyDelta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		
		syncTestUtils.mockEmailSyncClasses(initialSyncKey, emptyDelta, userAsList);
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		opClient.folderSync(initialFolderSyncKey);
		opClient.syncEmail(decoder, initialSyncKey, syncEmailCollectionId, THREE_DAYS_BACK, 150);
		SyncResponse partialSyncResponse = opClient.partialSync(decoder);
		
		assertThat(partialSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
	}
	
	private MSEmail applicationData(String message, MSEmailBodyType emailBodyType) {
		return MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(new SerializableInputStream(new ByteArrayInputStream(message.getBytes())))
					.bodyType(emailBodyType)
					.estimatedDataSize(0)
					.charset(Charsets.UTF_8)
					.truncated(false)
					.build())
			.build();
	}

	@Test
	public void testSyncOnUnexistingCollection() throws Exception {
		SyncKey syncEmailSyncKey = new SyncKey("1");
		CollectionId syncEmailUnexistingCollectionId = CollectionId.of(15105);
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, delta, userAsList);
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, syncEmailUnexistingCollectionId, THREE_DAYS_BACK, 25);

		SyncCollectionResponse mailboxResponse = syncTestUtils.getCollectionWithId(syncEmailResponse, syncEmailUnexistingCollectionId);
		assertThat(mailboxResponse.getStatus()).isEqualTo(SyncStatus.OBJECT_NOT_FOUND);
	}

	@Test
	public void testSyncDataClassAtCalendarButRecognizedAsEmail() throws Exception {
		SyncKey syncKey = new SyncKey("1");
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		syncTestUtils.mockEmailSyncClasses(syncKey, delta, userAsList);
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.sync(decoder, syncKey, inboxCollectionId, PIMDataType.CALENDAR);

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.SERVER_ERROR);
	}

	@Test
	public void testSyncOnHierarchyChangedException() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey syncEmailSyncKey = new SyncKey("1");
		
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		userAccessUtils.mockUsersAccess(userAsList);
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		mockEmailSyncThrowsException(syncEmailSyncKey, 
				new HierarchyChangedException(new NotAllowedException("Not allowed")));

		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		opClient.folderSync(initialSyncKey);
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 100);

		assertThat(syncEmailResponse).isNotNull();
		assertThat(syncEmailResponse.getStatus()).isEqualTo(SyncStatus.HIERARCHY_CHANGED);
	}

	@Test
	public void testSyncOnIllegalArgumentException() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey syncEmailSyncKey = new SyncKey("1");
		
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		userAccessUtils.mockUsersAccess(userAsList);
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		mockEmailSyncThrowsException(syncEmailSyncKey, new IllegalArgumentException("Illegal"));

		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		opClient.folderSync(initialSyncKey);
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, inboxCollectionId, FilterType.THREE_DAYS_BACK, 100);

		assertThat(syncEmailResponse).isNotNull();
		assertThat(syncEmailResponse.getStatus()).isEqualTo(SyncStatus.SERVER_ERROR);
	}
	
	@Test(expected=HttpRequestException.class)
	public void syncWhenDeviceNotFoundShouldCleanUserRelatedData() throws Exception {
		folderSnapshotDao.create(users.blum.user, users.blum.device, knownFolderSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(inboxFolder)));
		
		SyncKey incomingSK = new SyncKey("888a5e46-3fe9-4684-879f-d935c5788888");
		SyncKey outgoingSK = new SyncKey("770a5e46-3fe9-4684-879f-d935c5721e1f");
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(outgoingSK);
		userAccessUtils.expectUserLoginFromOpush(userAsList);
		
		expect(deviceDao.getDevice(users.jaures.user, users.jaures.deviceId, users.jaures.userAgent, ProtocolVersion.V121))
			.andReturn(null);
		deviceDao.registerNewDevice(users.jaures.user, users.jaures.deviceId, users.jaures.deviceType);
		expectLastCall();
		expect(deviceDao.getPolicyKey(users.jaures.user, users.jaures.deviceId, PolicyStatus.ACCEPTED))
			.andReturn(null);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		try {
			opClient.syncEmail(decoder, incomingSK, inboxCollectionId, FilterType.THREE_DAYS_BACK, 100);
		} catch (Exception e) {
			assertThat(e).isInstanceOf(HttpRequestException.class);
			assertThat(((HttpRequestException)e).getStatusCode()).isEqualTo(449);
			assertThatUserHasNoFolderSnapshot(users.jaures);
			assertThatUserHasNoFolderMapping(users.jaures);
			assertThatUserHasFolderSnapshot(users.blum);
			assertThatUserHasFolderMapping(users.blum);
			throw e;
		}
	}

	private void assertThatUserHasFolderSnapshot(OpushUser user) {
		try {
			assertThat(folderSnapshotDao.get(user.user, user.device, knownFolderSyncKey)).isNotNull();
		} catch (FolderSnapshotNotFoundException e) {
			fail("FolderSnapshotNotFoundException was not expected");
		}
	}
	
	private void assertThatUserHasFolderMapping(OpushUser user) {
		try {
			assertThat(folderSnapshotDao.get(user.user, user.device, inboxCollectionId)).isNotNull();
		} catch (CollectionNotFoundException e) {
			fail("CollectionNotFoundException was not expected");
		}
	}

	private void assertThatUserHasNoFolderSnapshot(OpushUser user) {
		try {
			folderSnapshotDao.get(user.user, user.device, knownFolderSyncKey);
			fail("FolderSnapshotNotFoundException was expected");
		} catch (FolderSnapshotNotFoundException e) {
			// expected
		}
	}
	
	private void assertThatUserHasNoFolderMapping(OpushUser user) {
		try {
			folderSnapshotDao.get(user.user, user.device, inboxCollectionId);
			fail("CollectionNotFoundException was expected");
		} catch (CollectionNotFoundException e) {
			// expected
		}
	}

	private void mockEmailSyncThrowsException(SyncKey syncKey, Throwable throwable)
			throws DaoException, ConversionException, FilterTypeChangedException {

		testUtils.expectUserCollectionsNeverChange();
		syncTestUtils.mockCollectionDaoForEmailSync(syncKey);
		
		syncTestUtils.mockItemTrackingDao();
		
		expect(contentsExporter.getChanged(
				anyObject(UserDataRequest.class), 
				anyObject(ItemSyncState.class),
				anyObject(AnalysedSyncCollection.class),
				anyObject(SyncKey.class)))
				.andThrow(throwable);
	}

	@Test
	public void testSyncWithAddCommandButWithoutApplicationDataGetsProtocolError() throws Exception {
		testSyncWithGivenCommandButWithoutApplicationDataGetsProtocolError(SyncCommand.ADD);
	}

	@Test
	public void testSyncWithChangeCommandButWithoutApplicationDataGetsProtocolError() throws Exception {
		testSyncWithGivenCommandButWithoutApplicationDataGetsProtocolError(SyncCommand.CHANGE);
	}

	private void testSyncWithGivenCommandButWithoutApplicationDataGetsProtocolError(SyncCommand command) throws Exception {
		SyncKey syncKey = new SyncKey("1");
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		syncTestUtils.mockEmailSyncClasses(syncKey, delta, userAsList);

		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.run(syncBuilder
			.request(ClientSyncRequest.builder()
				.addCollection(AnalysedSyncCollection.builder().collectionId(inboxCollectionId).syncKey(syncKey)
					.dataType(PIMDataType.EMAIL)
					.command(SyncCollectionCommandRequest.builder().type(command).serverId(inboxCollectionId.serverId(51)).build())
					.build())
				.build())
			.build());

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.PROTOCOL_ERROR);
	}

	@Test
	public void testAddLeadingToNoPermissionExceptionReplyNothing() throws Exception {
		TimeZone defaultTimeZone = TimeZone.getDefault();
		TimeZone.setDefault(DateTimeZone.UTC.toTimeZone());
		
		SyncKey syncKey = new SyncKey("13424");
		CollectionId collectionId = CollectionId.of(1);
		ServerId serverId = collectionId.serverId(1);
		String clientId = "156";

		DataDelta serverDataDelta = DataDelta.newEmptyDelta(date("2012-10-10T16:22:53"), syncKey);
		
		MSEmail clientData = MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(new SerializableInputStream(new ByteArrayInputStream("obm".getBytes())))
					.bodyType(MSEmailBodyType.PlainText)
					.estimatedDataSize(0)
					.charset(Charsets.UTF_8)
					.truncated(false)
					.build())
			.build();
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2342"));
		syncTestUtils.mockEmailSyncClasses(syncKey, serverDataDelta, userAsList);
		
		UserDataRequest udr = new UserDataRequest(users.jaures.credentials, "Sync", users.jaures.device);
		expect(contentsImporter.importMessageChange(udr, collectionId, serverId, clientId, clientData))
			.andThrow(new NoPermissionException());
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.run(syncBuilder
			.device(users.jaures.device)
			.request(ClientSyncRequest.builder()
				.addCollection(AnalysedSyncCollection.builder().collectionId(collectionId)
					.syncKey(syncKey).dataType(PIMDataType.EMAIL)
					.command(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD)
							.serverId(serverId).clientId(clientId).applicationData(clientData).build())
					.build())
				.build())
			.build());

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		syncTestUtils.checkMailFolderHasNoChange(syncResponse, collectionId);
		TimeZone.setDefault(defaultTimeZone);
	}

	@Test
	public void testChangeLeadingToNoPermissionExceptionReplyNothing() throws Exception {
		TimeZone defaultTimeZone = TimeZone.getDefault();
		TimeZone.setDefault(DateTimeZone.UTC.toTimeZone());
		
		SyncKey syncKey = new SyncKey("13424");
		CollectionId collectionId = CollectionId.of(1);
		ServerId serverId = CollectionId.of(432).serverId(1456);
		String clientId = null;

		DataDelta serverDataDelta = DataDelta.newEmptyDelta(date("2012-10-10T16:22:53"), syncKey);

		MSEmail clientData = MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(new SerializableInputStream(new ByteArrayInputStream("obm".getBytes())))
					.bodyType(MSEmailBodyType.PlainText)
					.estimatedDataSize(0)
					.charset(Charsets.UTF_8)
					.truncated(false)
					.build())
			.build();
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		syncTestUtils.mockEmailSyncClasses(syncKey, serverDataDelta, userAsList);
		
		UserDataRequest udr = new UserDataRequest(users.jaures.credentials, "Sync", users.jaures.device);
		expect(contentsImporter.importMessageChange(udr, collectionId, serverId, clientId, clientData))
			.andThrow(new NoPermissionException());
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.run(syncBuilder
			.device(users.jaures.device)
			.request(ClientSyncRequest.builder()
				.addCollection(AnalysedSyncCollection.builder().collectionId(collectionId)
					.syncKey(syncKey).dataType(PIMDataType.EMAIL)
					.command(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD)
								.serverId(serverId).clientId(clientId).applicationData(clientData).build())
					.build())
				.build())
			.build());

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		syncTestUtils.checkMailFolderHasNoChange(syncResponse, collectionId);
		TimeZone.setDefault(defaultTimeZone);
	}
	
	@Test
	public void deleteWithZeroServerIdShouldBeIgnored() throws Exception {
		SyncKey syncKey = new SyncKey("13424");
		CollectionId collectionId = CollectionId.of(1);
		ServerId serverId = collectionId.serverId(1456);
		String clientId = "94953";

		DataDelta serverDataDelta = DataDelta.newEmptyDelta(date("2012-10-10T16:22:53"), syncKey);
		
		MSEmail clientData = MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(new SerializableInputStream(new ByteArrayInputStream("obm".getBytes())))
					.bodyType(MSEmailBodyType.PlainText)
					.estimatedDataSize(0)
					.charset(Charsets.UTF_8)
					.truncated(false)
					.build())
			.build();
		
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		syncTestUtils.mockEmailSyncClasses(syncKey, serverDataDelta, userAsList);
		
		UserDataRequest udr = new UserDataRequest(users.jaures.credentials, "Sync", users.jaures.device);
		expect(contentsImporter.importMessageChange(udr, collectionId, null, clientId, clientData))
			.andReturn(serverId);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.run(syncBuilder
			.request(ClientSyncRequest.builder()
				.addCollection(AnalysedSyncCollection.builder()
					.dataType(PIMDataType.EMAIL).collectionId(collectionId).syncKey(syncKey)
					.command(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD).clientId(clientId).applicationData(clientData).build())
					.command(SyncCollectionCommandRequest.builder().type(SyncCommand.DELETE).serverId(ServerId.of("0")).build())
					.build())
				.build())
			.build());

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(syncResponse, collectionId);
		assertThat(collectionResponse.getResponses().adds()).containsExactly(serverId);
	}
	
	@Test
	public void deleteWithItemBelongingToAnotherCollectionShouldBeIgnored() throws Exception {
		SyncKey syncKey = new SyncKey("13424");
		CollectionId collectionId = CollectionId.of(1);
		ServerId serverId = collectionId.serverId(1456);
		String clientId = "94953";

		DataDelta serverDataDelta = DataDelta.newEmptyDelta(date("2012-10-10T16:22:53"), syncKey);
		
		MSEmail clientData = MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(new SerializableInputStream(new ByteArrayInputStream("obm".getBytes())))
					.bodyType(MSEmailBodyType.PlainText)
					.estimatedDataSize(0)
					.charset(Charsets.UTF_8)
					.truncated(false)
					.build())
			.build();
		
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		syncTestUtils.mockEmailSyncClasses(syncKey, serverDataDelta, userAsList);
		
		UserDataRequest udr = new UserDataRequest(users.jaures.credentials, "Sync", users.jaures.device);
		expect(contentsImporter.importMessageChange(udr, collectionId, null, clientId, clientData))
			.andReturn(serverId);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.run(syncBuilder
			.request(ClientSyncRequest.builder()
				.addCollection(AnalysedSyncCollection.builder()
					.dataType(PIMDataType.EMAIL).collectionId(collectionId).syncKey(syncKey)
					.command(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD).clientId(clientId).applicationData(clientData).build())
					.command(SyncCollectionCommandRequest.builder().type(SyncCommand.DELETE).serverId(CollectionId.of(2).serverId(2)).build())
					.build())
				.build())
			.build());

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(syncResponse, collectionId);
		assertThat(collectionResponse.getResponses().adds()).containsExactly(serverId);
	}
	
	@Test
	public void syncShouldAcceptSyncRequestWithoutDataClass() throws Exception {
		SyncKey syncKey = new SyncKey("13424");
		CollectionId collectionId = CollectionId.of(1);
		ServerId serverId = collectionId.serverId(1456);
		String clientId = "156";

		DataDelta serverDataDelta = DataDelta.newEmptyDelta(date("2012-10-10T16:22:53"), syncKey);
		
		MSEmail clientData = MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(new SerializableInputStream(new ByteArrayInputStream("obm".getBytes())))
					.bodyType(MSEmailBodyType.PlainText)
					.estimatedDataSize(0)
					.charset(Charsets.UTF_8)
					.truncated(false)
					.build())
			.build();
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		hierarchyChangesTestUtils.mockGetBackendFoldersWithINBOX();
		syncTestUtils.mockEmailSyncClasses(syncKey, serverDataDelta, userAsList);
		
		UserDataRequest udr = new UserDataRequest(users.jaures.credentials, "Sync", users.jaures.device);
		expect(contentsImporter.importMessageChange(udr, collectionId, null, clientId, clientData))
			.andReturn(serverId);
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.run(syncBuilder
			.request(ClientSyncRequest.builder()
				.addCollection(AnalysedSyncCollection.builder()
					.collectionId(collectionId).syncKey(syncKey)
					.command(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD).clientId(clientId).applicationData(clientData).build())
					.build())
				.build())
			.build());

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		SyncCollectionResponse collectionResponse = syncTestUtils.getCollectionWithId(syncResponse, collectionId);
		assertThat(collectionResponse.getResponses().adds()).containsExactly(serverId);
	}
}
