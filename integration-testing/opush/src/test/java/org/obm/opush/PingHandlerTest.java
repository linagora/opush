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

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.transform.TransformerException;

import org.apache.http.client.fluent.Async;
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
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.opush.env.DefaultOpushModule;
import org.obm.push.OpushServer;
import org.obm.push.bean.Device;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PingStatus;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.calendar.CalendarBackend;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.HierarchyChangedException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.protocol.PingProtocol;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.PingResponse;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.HeartbeatDao;
import org.obm.push.utils.DOMUtils;
import org.obm.push.utils.DateUtils;
import org.obm.sync.auth.AuthFault;
import org.obm.sync.push.client.OPClient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

@RunWith(GuiceRunner.class)
@GuiceModule(DefaultOpushModule.class)
public class PingHandlerTest {

	@Inject Users users;
	@Inject OpushServer opushServer;
	@Inject IMocksControl mocksControl;
	@Inject Configuration configuration;
	@Inject PolicyConfigurationProvider policyConfigurationProvider;
	@Inject PingProtocol pingProtocol;
	@Inject CassandraServer cassandraServer;
	@Inject PendingQueriesLock queriesLock;
	@Inject IntegrationTestUtils testUtils;
	@Inject IntegrationUserAccessUtils userAccessUtils;
	@Inject HeartbeatDao heartbeatDao;
	@Inject CalendarBackend calendarBackend;
	@Inject CollectionDao collectionDao;
	@Inject FolderSnapshotDao folderSnapshotDao;
	
	private CloseableHttpClient httpClient;
	private ExecutorService threadpool;
	private Async async;

	@Before
	public void init() throws Exception {
		cassandraServer.start();

		threadpool = Executors.newFixedThreadPool(4);
		async = Async.newInstance().use(threadpool);
		httpClient = HttpClientBuilder.create().build();

		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
	}
	
	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		threadpool.shutdown();
		httpClient.close();
		Files.delete(configuration.dataDir);
	}

	@Test
	@Ignore("OBMFULL-4125")
	public void testInterval() throws Exception {
		testHeartbeatInterval(5, 5, 5);
	}


	@Test
	@Ignore("OBMFULL-4125")
	public void testMinInterval() throws Exception {
		testHeartbeatInterval(1, 5, 5);
	}

	@Test
	@Ignore("OBMFULL-5442")
	public void testNoChange() throws Exception {
		prepareMockNoChange(Arrays.asList(users.jaures));

		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		Document document = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Ping>" +
				"<HeartbeatInterval>5</HeartbeatInterval>" +
					"<Folders>" +
						"<Folder>" +
							"<Id>1</Id>" +
							"<Class>Calendar</Class>" +
						"</Folder>" +
						"<Folder>" +
							"<Id>4</Id>" +
							"<Class>Contacts</Class>" +
						"</Folder>" +
					"</Folders>" +
				"</Ping>");
		
		Stopwatch stopwatch = Stopwatch.createStarted();
		Document response = opClient.postXml("Ping", document, "Ping", null, false);
		
		checkExecutionTime(2, 5, stopwatch);
		assertThat(DOMUtils.serialize(response))
			.isEqualTo(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Ping>" +
					"<Status>2</Status>" +
				"</Ping>");
		
	}
	
	@Test
	@Ignore("OBMFULL-4125")
	public void test3BlockingClient() throws Exception {
		prepareMockNoChange(Arrays.asList(users.jaures));

		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		
		ThreadPoolExecutor threadPoolExecutor = 
				new ThreadPoolExecutor(20, 20, 1,TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

		Stopwatch stopwatch = Stopwatch.createStarted();
		
		List<Future<Document>> futures = new ArrayList<Future<Document>>();
		for (int i = 0; i < 4; ++i) {
			 futures.add(queuePingCommand(opClient, users.jaures, threadPoolExecutor));	
		}
		
		for (Future<Document> f: futures) {
			Document response = f.get();
			checkNoChangeResponse(response);
		}
		
		checkExecutionTime(2, 5, stopwatch);
	}

	@Test
	@Ignore("OBMFULL-4125")
	public void testPushNotificationOnBackendChangeShort() throws Exception {
		prepareMockHasChanges(1, Arrays.asList(users.jaures));

		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		Document document = buildPingCommand(20, users.jaures.hashCode());
		Stopwatch stopwatch = Stopwatch.createStarted();
		
		Document response = opClient.postXml("Ping", document, "Ping", null, false);
		
		checkExecutionTime(5, 1, stopwatch);
		checkHasChangeResponse(response);
	}

	@Test
	@Ignore("OBMFULL-4125")
	public void testPushNotificationOnBackendChangeLong() throws Exception {
		prepareMockHasChanges(2, Arrays.asList(users.jaures));

		opushServer.start();

		Document document = buildPingCommand(20, users.jaures.hashCode());
		Stopwatch stopwatch = Stopwatch.createStarted();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		Document response = opClient.postXml("Ping", document, "Ping", null, false);
		
		checkExecutionTime(5, 6, stopwatch);
		checkHasChangeResponse(response);
	}

	@Test
	@Ignore("OBMFULL-4125")
	public void testPushNotificationOnBackendHierarchyChangedException() throws Exception {
		prepareMockHierarchyChangedException(Arrays.asList(users.jaures));

		opushServer.start();

		Document document = buildPingCommand(20, users.jaures.hashCode());

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		Document response = opClient.postXml("Ping", document, "Ping", null, false);
		
		checkFolderSyncRequiredResponse(response);
	}
	
	@Test
	public void testPingAfterPingTimeout() throws Exception {
		int heartbeat = 5;
		CollectionId collectionId = CollectionId.of(users.jaures.hashCode());
		
		prepareMockHasChanges(1, Arrays.asList(users.jaures));

		Folder folder = Folder.builder()
			.collectionId(collectionId)
			.backendId(MailboxPath.of(EmailConfiguration.IMAP_INBOX_NAME))
			.displayName(EmailConfiguration.IMAP_INBOX_NAME)
			.folderType(FolderType.DEFAULT_INBOX_FOLDER)
			.parentBackendIdOpt(Optional.<BackendId>absent())
			.build();
		
		FolderSyncKey folderSyncKey = new FolderSyncKey("c8355d6c-9325-490a-87ec-2522b2e23b99");
		folderSnapshotDao.create(users.jaures.user, users.jaures.device, folderSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder)));

		opushServer.start();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		
		Future<PingResponse> response1 = opClient.pingASync(async, pingProtocol, collectionId, heartbeat);
		PingResponse pingResponse1 = response1.get(heartbeat * 2,  TimeUnit.SECONDS);
		Future<PingResponse> response2 = opClient.pingASync(async, pingProtocol, collectionId, heartbeat);
		PingResponse pingResponse2 = response2.get(heartbeat * 2,  TimeUnit.SECONDS);
		
		assertThat(pingResponse1.getPingStatus()).isEqualTo(PingStatus.NO_CHANGES);
		assertThat(pingResponse2.getPingStatus()).isEqualTo(PingStatus.NO_CHANGES);
	}
	
	@Test(expected=TimeoutException.class)
	public void testTwoUsersPingWithSameDevice() throws Exception {
		CollectionId collectionId = CollectionId.of(users.jaures.hashCode());
		int heartbeat = 10;

		prepareMockHasChanges(1, Arrays.asList(users.jaures, users.blum));

		Folder folder = Folder.builder()
			.collectionId(collectionId)
			.backendId(MailboxPath.of(EmailConfiguration.IMAP_INBOX_NAME))
			.displayName(EmailConfiguration.IMAP_INBOX_NAME)
			.folderType(FolderType.DEFAULT_INBOX_FOLDER)
			.parentBackendIdOpt(Optional.<BackendId>absent())
			.build();
		
		FolderSyncKey folderSyncKey = new FolderSyncKey("c8355d6c-9325-490a-87ec-2522b2e23b99");
		folderSnapshotDao.create(users.jaures.user, users.jaures.device, folderSyncKey, 
			FolderSnapshot.nextId(2).folders(ImmutableSet.of(folder)));
		
		opushServer.start();

		OPClient opClientJaures = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		OPClient opClientBlum = testUtils.buildWBXMLOpushClient(users.blum, opushServer.getHttpPort(), httpClient);
		
		queriesLock.expectedQueriesCountToBeStarted(1);
		Future<PingResponse> response1 = opClientJaures.pingASync(async, pingProtocol, collectionId, 1000);
		queriesLock.waitingStart(3, TimeUnit.SECONDS);
		queriesLock.waitingClose(3, TimeUnit.SECONDS);
		opClientBlum.pingASync(async, pingProtocol, CollectionId.of(users.blum.hashCode()), heartbeat);
		response1.get(1, TimeUnit.SECONDS);
	}
	
	private void prepareMockNoChange(List<OpushUser> users) throws DaoException, CollectionNotFoundException, 
			UnexpectedObmSyncServerException, AuthFault, ConversionException, HierarchyChangedException {
		userAccessUtils.mockUsersAccess(users);
		mockForPingNeeds();
		mockForNoChangePing();
		mocksControl.replay();
	}

	private void prepareMockHasChanges(int noChangeIterationCount, List<OpushUser> users) throws DaoException, CollectionNotFoundException, 
			UnexpectedObmSyncServerException, AuthFault, ConversionException, HierarchyChangedException {
		userAccessUtils.mockUsersAccess(users);
		mockForPingNeeds();
		mockForCalendarHasChangePing(noChangeIterationCount, users);
		mocksControl.replay();
	}

	private void prepareMockHierarchyChangedException(List<OpushUser> users) throws DaoException, CollectionNotFoundException, 
			UnexpectedObmSyncServerException, AuthFault, ConversionException, HierarchyChangedException {
		userAccessUtils.mockUsersAccess(users);
		mockForPingNeeds();
		mockForCalendarHierarchyChangedException(users);
		mocksControl.replay();
	}
	
	public void testHeartbeatInterval(int heartbeatInterval, int delta, int expected) throws Exception {
		prepareMockNoChange(Arrays.asList(users.jaures));
		
		opushServer.start();
		
		Document document = buildPingCommand(heartbeatInterval, users.jaures.hashCode());
		Stopwatch stopwatch = Stopwatch.createStarted();

		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		Document response = opClient.postXml("Ping", document, "Ping", null, false);
		
		checkExecutionTime(delta, expected, stopwatch);
		checkNoChangeResponse(response);
	}

	private void checkExecutionTime(int delta, int expected,
			Stopwatch stopwatch) {
		stopwatch.stop();
		long elapsedTime = stopwatch.elapsed(TimeUnit.SECONDS);
		assertThat(elapsedTime)
			.isGreaterThanOrEqualTo(expected)
			.isLessThan(expected + delta);
	}

	private void checkNoChangeResponse(Document response)
			throws TransformerException {
		assertThat(DOMUtils.serialize(response))
			.isEqualTo(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
					"<Ping><Status>1</Status><Folders/></Ping>");
	}
	
	private void checkHasChangeResponse(Document response) throws TransformerException {
		assertThat(DOMUtils.serialize(response))
			.isEqualTo(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><Ping>" +
					"<Status>2</Status>" +
					"<Folders><Folder>1432</Folder></Folders></Ping>");
	}
	
	private void checkFolderSyncRequiredResponse(Document response) throws TransformerException {
		assertThat(DOMUtils.serialize(response))
			.isEqualTo(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><Ping>" +
					"<Status>7</Status>" +
					"</Ping>");
	}

	private void mockForPingNeeds() throws DaoException {
		mockHeartbeatDao();
	}
	
	private void mockForNoChangePing() throws DaoException, CollectionNotFoundException,
			UnexpectedObmSyncServerException, ConversionException, HierarchyChangedException {
		mockCalendarBackendHasNoChange();
		testUtils.expectUserCollectionsNeverChange();
	}

	private void mockForCalendarHasChangePing(int noChangeIterationCount, List<OpushUser> users) 
			throws DaoException, CollectionNotFoundException, UnexpectedObmSyncServerException,
			ConversionException, HierarchyChangedException {
		mockCollectionDaoHasChange(noChangeIterationCount, users);
		mockCalendarBackendHasContentChanges();
	}

	private void mockForCalendarHierarchyChangedException(List<OpushUser> users) 
			throws DaoException, CollectionNotFoundException, UnexpectedObmSyncServerException,
			ConversionException, HierarchyChangedException {
		mockCollectionDaoHasChange(1, users);
		mockCalendarBackendHierarchyChangedException();
	}

	private void mockCollectionDaoHasChange(int noChangeIterationCount, List<OpushUser> users) 
			throws DaoException, CollectionNotFoundException {
		int collectionNoChangeIterationCount = noChangeIterationCount;

		expectCollectionDaoUnchangeForXIteration(collectionNoChangeIterationCount);

		for (OpushUser user : users) {
			CollectionId collectionId = CollectionId.of(user.hashCode());
			
			ItemSyncState syncState = ItemSyncState.builder()
					.syncKey(new SyncKey("sync state"))
					.syncDate(DateUtils.getCurrentDate())
					.build();
			expect(collectionDao.lastKnownState(user.device, collectionId)).andReturn(syncState).once();
		}
	}
	
	private void expectCollectionDaoUnchangeForXIteration(int noChangeIterationCount) throws DaoException {
		ItemSyncState syncState = ItemSyncState.builder()
				.syncKey(new SyncKey("sync state"))
				.syncDate(DateUtils.getCurrentDate())
				.build();
		expect(collectionDao.lastKnownState(anyObject(Device.class), anyObject(CollectionId.class))).andReturn(syncState).times(noChangeIterationCount);
	}

	private void mockCalendarBackendHasContentChanges()
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException,
			ConversionException, HierarchyChangedException {
		
		expect(calendarBackend.getItemEstimateSize(
				anyObject(UserDataRequest.class), 
				anyObject(ItemSyncState.class),
				anyObject(CollectionId.class),
				anyObject(SyncCollectionOptions.class)))
			.andReturn(1).times(2);
	}

	private void mockCalendarBackendHierarchyChangedException()
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException,
			ConversionException, HierarchyChangedException {
		
		expect(calendarBackend.getItemEstimateSize(
				anyObject(UserDataRequest.class), 
				anyObject(ItemSyncState.class),
				anyObject(CollectionId.class),
				anyObject(SyncCollectionOptions.class)))
			.andThrow(new HierarchyChangedException(new NotAllowedException("Not allowed")));
	}

	private void mockCalendarBackendHasNoChange() 
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException,
			ConversionException, HierarchyChangedException {
		
		expect(calendarBackend.getItemEstimateSize(
				anyObject(UserDataRequest.class), 
				anyObject(ItemSyncState.class),
				anyObject(CollectionId.class),
				anyObject(SyncCollectionOptions.class)))
			.andReturn(0).anyTimes();
	}

	private Future<Document> queuePingCommand(final OPClient opClient, final OpushUser user,
			ThreadPoolExecutor threadPoolExecutor) {
		return threadPoolExecutor.submit(new Callable<Document>() {
			@Override
			public Document call() throws Exception {
				Document document = buildPingCommand(5, user.hashCode());
				return opClient.postXml("Ping", document, "Ping", null, false);
			}
		});
	}

	private void mockHeartbeatDao() throws DaoException {
		heartbeatDao.updateLastHeartbeat(anyObject(Device.class), anyLong());
		expectLastCall().anyTimes();
	}
	
	private Document buildPingCommand(int heartbeatInterval, int collectionId)
			throws SAXException, IOException {
		return DOMUtils.parse("<Ping>"
				+ "<HeartbeatInterval>"
				+ heartbeatInterval
				+ "</HeartbeatInterval>"
				+ "<Folders>"
				+ "<Folder>"
				+ "<Id>" + String.valueOf(collectionId) +"</Id>"
				+ "</Folder>"
				+ "</Folders>"
				+ "</Ping>");
	}
}
