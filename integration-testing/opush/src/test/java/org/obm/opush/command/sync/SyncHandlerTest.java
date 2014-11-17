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
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.obm.DateUtils.date;
import static org.obm.push.bean.FilterType.THREE_DAYS_BACK;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
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
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.Device;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSEmailBodyType;
import org.obm.push.bean.MSEmailHeader;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionCommand;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemChangesBuilder;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.bean.ms.MSEmailBody;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.HierarchyChangedException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.mail.exception.FilterTypeChangedException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.FolderSyncResponse;
import org.obm.push.protocol.bean.SyncCollection;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.EncoderFactory;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.CollectionDao;
import org.obm.push.utils.DateUtils;
import org.obm.push.utils.SerializableInputStream;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.commands.Sync;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

@GuiceModule(SyncHandlerTestModule.class)
@RunWith(GuiceRunner.class)
public class SyncHandlerTest {

	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private IMocksControl mocksControl;
	@Inject private SyncDecoder decoder;
	@Inject private IContentsExporter contentsExporter;
	@Inject private IContentsImporter contentsImporter;
	@Inject private EncoderFactory encoderFactory;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private HierarchyChangesTestUtils hierarchyChangesTestUtils;
	@Inject private SyncKeyTestUtils syncKeyTestUtils;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private SyncTestUtils syncTestUtils;
	@Inject private CollectionDao collectionDao;

	private List<OpushUser> userAsList;
	private CloseableHttpClient httpClient;

	@Before
	public void init() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
		userAsList = Arrays.asList(users.jaures);
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
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
		CollectionId syncEmailCollectionId = CollectionId.of(4);
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();

		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("234");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, Sets.newHashSet(syncEmailCollectionId), delta, userAsList);
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
		FolderSyncKey firstSyncKey = new FolderSyncKey("345");
		SyncKey syncEmailSyncKey = new SyncKey("1");
		CollectionId syncEmailCollectionId = CollectionId.of(4);
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstSyncKey);
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, Sets.newHashSet(syncEmailCollectionId), delta, userAsList);
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
		FolderSyncKey firstSyncKey = new FolderSyncKey("345");
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
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, Sets.newHashSet(syncEmailCollectionId), delta, userAsList);
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
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("F2342");
		SyncKey syncEmailSyncKey = new SyncKey("13424");
		CollectionId syncEmailCollectionId = CollectionId.of(432);

		testUtils.expectUserCollectionsNeverChange(users.jaures, ImmutableList.of(syncEmailCollectionId));
		syncTestUtils.mockCollectionDaoForEmailSync(syncEmailSyncKey, ImmutableList.of(syncEmailCollectionId));
		
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2342"));
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
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
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("F2342");
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
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, Sets.newHashSet(syncEmailCollectionId), delta, userAsList);
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

		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("234");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, Sets.newHashSet(syncEmailCollectionId), delta, userAsList);
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
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("234");
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
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, Sets.newHashSet(syncEmailCollectionId), delta, userAsList);
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
		CollectionId syncEmailCollectionId = CollectionId.of(432);
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
		
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("234");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		testUtils.expectContentExporterFetching(userDataRequest, itemChange);
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, ImmutableList.of(syncEmailCollectionId), delta, userAsList);
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		FolderSyncResponse folderSyncResponse = opClient.folderSync(initialSyncKey);
		
		CollectionChange inbox = syncTestUtils.lookupInbox(folderSyncResponse.getCollectionsAddedAndUpdated());
		SyncResponse syncEmailResponse = opClient.run(Sync.builder(decoder)
				.collection(SyncCollection.builder().collectionId(inbox.getCollectionId())
								.syncKey(syncEmailSyncKey).dataType(PIMDataType.EMAIL)
								.command(SyncCollectionCommand.builder().type(SyncCommand.FETCH).serverId(serverId).build())
							.build())
				.build());

		syncTestUtils.checkMailFolderHasFetchItems(syncEmailResponse, inbox.getCollectionId(), syncEmailCollectionId.serverId(123));
		SyncCollectionResponse collection = syncTestUtils.getCollectionWithId(syncEmailResponse, inbox.getCollectionId());
		assertThat(collection.getItemDeletions()).isEmpty();
	}
	
	@Test
	public void testSyncWithUnknownSyncKeyReturnsInvalidSyncKeyStatus() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		String collectionPath = testUtils.buildEmailInboxCollectionPath(users.jaures); 
		
		SyncKey initialSyncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey secondSyncKey = new SyncKey("456");
		Date initialUpdateStateDate = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		ItemSyncState firstItemSyncState = ItemSyncState.builder().syncKey(initialSyncKey).syncDate(initialUpdateStateDate).build();
		
		userAccessUtils.mockUsersAccess(userAsList);

		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2342"));
		expect(collectionDao.getCollectionPath(collectionId)).andReturn(collectionPath).times(2);
		expect(collectionDao.findItemStateForKey(secondSyncKey)).andReturn(null);
		expect(collectionDao.updateState(anyObject(Device.class), anyObject(CollectionId.class), anyObject(SyncKey.class), anyObject(Date.class)))
			.andReturn(firstItemSyncState)
			.anyTimes();
		collectionDao.resetCollection(users.jaures.device, collectionId);
		expectLastCall();
		contentsExporter.initialize(users.jaures.deviceId, collectionId, PIMDataType.EMAIL, THREE_DAYS_BACK, new SyncKey("2342"));
		expectLastCall();
		
		mocksControl.replay();
		opushServer.start();
		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		opClient.syncEmail(decoder, initialSyncKey, collectionId, THREE_DAYS_BACK, 100);
		SyncResponse syncResponse = opClient.syncEmail(decoder, secondSyncKey, collectionId, THREE_DAYS_BACK, 100);
		mocksControl.verify();

		SyncCollectionResponse inboxResponse = syncTestUtils.getCollectionWithId(syncResponse, collectionId);
		assertThat(inboxResponse.getStatus()).isEqualTo(SyncStatus.INVALID_SYNC_KEY);
	}

	@Test
	public void testSyncWithoutOptionsAndNoOptionsInCacheTakeThePreviousOne() throws Exception {
		OpushUser user = users.jaures;
		CollectionId collectionId = CollectionId.of(1);
		String collectionPath = testUtils.buildEmailInboxCollectionPath(user);
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

		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("234");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"), new SyncKey("3345"));
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		expect(contentsExporter.getChanged(
				anyObject(UserDataRequest.class),
				anyObject(ItemSyncState.class),
				anyObject(AnalysedSyncCollection.class),
				anyObject(SyncKey.class)))
			.andReturn(DataDelta.newEmptyDelta(secondRequestSyncState.getSyncDate(), secondRequestSyncState.getSyncKey()));
		contentsExporter.initialize(users.jaures.deviceId, collectionId, PIMDataType.EMAIL, THREE_DAYS_BACK, new SyncKey("2345"));
		expectLastCall();
		
		expect(collectionDao.getCollectionPath(collectionId)).andReturn(collectionPath).anyTimes();
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

	private FolderSyncState newSyncState(FolderSyncKey folderSyncKey) {
		return FolderSyncState.builder()
				.syncKey(folderSyncKey)
				.build();
	}
	
	public void testPartialSyncWhenNoPreviousSendError13() throws Exception {
		FolderSyncKey initialFolderSyncKey = new FolderSyncKey("0");
		FolderSyncKey nextFolderSyncKey = new FolderSyncKey("1234");
		
		userAccessUtils.mockUsersAccess(userAsList);
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(nextFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
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
		FolderSyncKey initialFolderSyncKey = new FolderSyncKey("0");
		FolderSyncKey nextFolderSyncKey = new FolderSyncKey("56789");

		SyncKey initialSyncKey = new SyncKey("1234");
		CollectionId syncEmailCollectionId = CollectionId.of(12);
		DataDelta emptyDelta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(nextFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(initialSyncKey, ImmutableSet.of(syncEmailCollectionId), emptyDelta, userAsList);
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
		java.util.Collection<CollectionId> existingCollections = Collections.emptySet();
		CollectionId syncEmailUnexistingCollectionId = CollectionId.of(15105);
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncEmailSyncKey, existingCollections, delta, userAsList);
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
		CollectionId collectionId = CollectionId.of(15105);
		List<CollectionId> existingCollections = ImmutableList.of(collectionId);
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncKey, existingCollections, delta, userAsList);
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.sync(decoder, syncKey, collectionId, PIMDataType.CALENDAR);

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.SERVER_ERROR);
	}

	@Test
	public void testSyncOnHierarchyChangedException() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey syncEmailSyncKey = new SyncKey("1");
		CollectionId syncEmailCollectionId = CollectionId.of(4);
		
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("234");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		userAccessUtils.mockUsersAccess(userAsList);
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		mockEmailSyncThrowsException(syncEmailSyncKey, Sets.newHashSet(syncEmailCollectionId), 
				new HierarchyChangedException(new NotAllowedException("Not allowed")));
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		opClient.folderSync(initialSyncKey);
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, syncEmailCollectionId, FilterType.THREE_DAYS_BACK, 100);

		assertThat(syncEmailResponse).isNotNull();
		assertThat(syncEmailResponse.getStatus()).isEqualTo(SyncStatus.HIERARCHY_CHANGED);
	}

	@Test
	public void testSyncOnIllegalArgumentException() throws Exception {
		FolderSyncKey initialSyncKey = FolderSyncKey.INITIAL_FOLDER_SYNC_KEY;
		SyncKey syncEmailSyncKey = new SyncKey("1");
		CollectionId syncEmailCollectionId = CollectionId.of(4);
		
		FolderSyncKey firstFolderSyncKey = new FolderSyncKey("234");
		syncKeyTestUtils.mockNextGeneratedSyncKey(firstFolderSyncKey);
		syncKeyTestUtils.mockNextGeneratedSyncKey(new SyncKey("2345"));
		testUtils.expectAllocateFolderState(users.jaures.device, newSyncState(firstFolderSyncKey));
		testUtils.expectCreateFolderMappingState();
		userAccessUtils.mockUsersAccess(userAsList);
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		mockEmailSyncThrowsException(syncEmailSyncKey, Sets.newHashSet(syncEmailCollectionId), 
				new IllegalArgumentException("Illegal"));
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		opClient.folderSync(initialSyncKey);
		SyncResponse syncEmailResponse = opClient.syncEmail(decoder, syncEmailSyncKey, syncEmailCollectionId, FilterType.THREE_DAYS_BACK, 100);

		assertThat(syncEmailResponse).isNotNull();
		assertThat(syncEmailResponse.getStatus()).isEqualTo(SyncStatus.SERVER_ERROR);
	}

	private void mockEmailSyncThrowsException(SyncKey syncKey, HashSet<CollectionId> hashSet, Throwable throwable)
			throws DaoException, ConversionException, FilterTypeChangedException {

		testUtils.expectUserCollectionsNeverChange(users.jaures, hashSet);
		syncTestUtils.mockCollectionDaoForEmailSync(syncKey, hashSet);
		
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
		CollectionId collectionId = CollectionId.of(15);
		List<CollectionId> existingCollections = ImmutableList.of(collectionId);
		DataDelta delta = DataDelta.builder()
			.syncDate(new Date())
			.syncKey(new SyncKey("123"))
			.build();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncKey, existingCollections, delta, userAsList);

		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.run(Sync.builder(decoder)
				.collection(SyncCollection.builder().collectionId(collectionId).syncKey(syncKey)
							.dataType(PIMDataType.EMAIL)
							.command(SyncCollectionCommand.builder().type(command).serverId(collectionId.serverId(51)).build())
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
		List<CollectionId> existingCollections = ImmutableList.of(collectionId);
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
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncKey, existingCollections, serverDataDelta, userAsList);
		
		UserDataRequest udr = new UserDataRequest(users.jaures.credentials, "Sync", users.jaures.device);
		expect(contentsImporter.importMessageChange(udr, collectionId, serverId, clientId, clientData))
			.andThrow(new NoPermissionException());
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.run(
				Sync.builder(decoder).encoder(encoderFactory).device(users.jaures.device) 
					.collection(
							SyncCollection.builder().collectionId(collectionId)
								.syncKey(syncKey).dataType(PIMDataType.EMAIL)
								.command(SyncCollectionCommand.builder().type(SyncCommand.ADD)
										.serverId(serverId).clientId(clientId).applicationData(clientData).build())
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
		List<CollectionId> existingCollections = ImmutableList.of(collectionId);
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
		testUtils.expectCreateFolderMappingState();
		hierarchyChangesTestUtils.mockHierarchyChangesOnlyInbox();
		syncTestUtils.mockEmailSyncClasses(syncKey, existingCollections, serverDataDelta, userAsList);
		
		UserDataRequest udr = new UserDataRequest(users.jaures.credentials, "Sync", users.jaures.device);
		expect(contentsImporter.importMessageChange(udr, collectionId, serverId, clientId, clientData))
			.andThrow(new NoPermissionException());
		
		mocksControl.replay();
		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opClient.run(
				Sync.builder(decoder).encoder(encoderFactory).device(users.jaures.device) 
					.collection(SyncCollection.builder().collectionId(collectionId)
							.syncKey(syncKey).dataType(PIMDataType.EMAIL)
							.command(SyncCollectionCommand.builder().type(SyncCommand.ADD)
										.serverId(serverId).clientId(clientId).applicationData(clientData).build())
							.build())
					.build());

		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		syncTestUtils.checkMailFolderHasNoChange(syncResponse, collectionId);
		TimeZone.setDefault(defaultTimeZone);
	}
}
