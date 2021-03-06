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
package org.obm.push.protocol.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Properties;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.AddressBookId;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.configuration.OpushConfiguration;
import org.obm.push.protocol.bean.AnalysedPingRequest;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.PingRequest;
import org.obm.push.protocol.bean.SyncCollection;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.StateMachine;
import org.obm.push.store.HeartbeatDao;
import org.obm.push.store.MonitoredCollectionDao;
import org.obm.push.utils.DateUtils;


public class PingAnalyserTest {
	
	private Device device;
	private UserDataRequest udr;
	private User user;
	private Credentials credentials;
	private Folder folder;
	private CollectionId collectionId;
	
	private IMocksControl mocks;
	private HeartbeatDao heartbeatDao;
	private MonitoredCollectionDao monitoredCollectionDao;
	private FolderSnapshotDao folderSnapshotDao;
	private StateMachine stateMachine;
	private OpushConfiguration configuration;
	
	private PingAnalyser pingAnalyser;
	
	@Before
	public void setup() {
		device = new Device(1, "devType", new DeviceId("devId"), new Properties(), null);
		user = Factory.create().createUser("adrien@test.tlse.lngr", "email@test.tlse.lngr", "Adrien");
		credentials = new Credentials(user, "test".toCharArray());
		udr = new UserDataRequest(credentials, "Sync", device);
		collectionId = CollectionId.of(5);
		folder = Folder.builder()
			.displayName("INBOX")
			.collectionId(collectionId)
			.backendId(MailboxPath.of("INBOX"))
			.folderType(FolderType.DEFAULT_INBOX_FOLDER)
			.build();

		mocks = createControl();
		heartbeatDao = mocks.createMock(HeartbeatDao.class);
		monitoredCollectionDao = mocks.createMock(MonitoredCollectionDao.class);
		stateMachine = mocks.createMock(StateMachine.class);
		configuration = mocks.createMock(OpushConfiguration.class);
		folderSnapshotDao = mocks.createMock(FolderSnapshotDao.class);
		
		
		pingAnalyser = new PingAnalyser(heartbeatDao, monitoredCollectionDao, stateMachine, folderSnapshotDao, configuration);
	}
	
	@Test
	public void testAnalysePingWithoutHeartbeat() throws Exception {
		SyncKey syncKey = new SyncKey("123");
		PingRequest pingRequest = PingRequest.builder()
				.add(SyncCollection.builder()
						.collectionId(collectionId)
						.syncKey(syncKey)
						.build())
				.build();
		
		long heartbeat = 100;
	
		expect(heartbeatDao.findLastHeartbeat(device)).andReturn(heartbeat);
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expect(stateMachine.lastKnownState(device, collectionId)).andReturn(null).once();
		
		expect(configuration.defaultWindowSize())
			.andReturn(50).once();
		
		mocks.replay();
		
		AnalysedPingRequest analysedPingRequest = pingAnalyser.analysePing(udr, pingRequest);
		
		mocks.verify();
		assertThat(analysedPingRequest).isEqualTo(AnalysedPingRequest.builder()
				.heartbeatInterval(heartbeat)
				.add(AnalysedSyncCollection.builder()
						.collectionId(collectionId)
						.syncKey(SyncKey.INITIAL_SYNC_KEY)
						.dataType(PIMDataType.EMAIL)
						.windowSize(50)
						.build())
				.build());
	}
	
	@Test (expected=MissingRequestParameterException.class)
	public void testAnalysePingWithoutHeartbeatAndNoneStored() throws Exception {
		SyncKey syncKey = new SyncKey("123");
		PingRequest pingRequest = PingRequest.builder()
				.add(SyncCollection.builder()
						.collectionId(collectionId)
						.syncKey(syncKey)
						.build())
				.build();
		
		expect(heartbeatDao.findLastHeartbeat(device))
			.andReturn(null).once();
		
		mocks.replay();
		
		try {
			pingAnalyser.analysePing(udr, pingRequest);
		} catch (MissingRequestParameterException e) {
			mocks.verify();
			throw e;
		}
	}
	
	@Test
	public void testAnalysePingUseMinHeartbeat() throws Exception {
		SyncKey syncKey = new SyncKey("123");
		long heartbeat = 1;
		PingRequest pingRequest = PingRequest.builder()
				.add(SyncCollection.builder()
						.collectionId(collectionId)
						.syncKey(syncKey)
						.build())
				.heartbeatInterval(heartbeat)
				.build();
		
		heartbeatDao.updateLastHeartbeat(device, PingAnalyser.MIN_SANE_HEARTBEAT_VALUE);
		expectLastCall().once();
		
		expect(stateMachine.lastKnownState(device, collectionId))
			.andReturn(null).once();
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		
		expect(configuration.defaultWindowSize())
			.andReturn(50).once();
		
		mocks.replay();
		
		AnalysedPingRequest analysedPingRequest = pingAnalyser.analysePing(udr, pingRequest);
		
		mocks.verify();
		assertThat(analysedPingRequest).isEqualTo(AnalysedPingRequest.builder()
				.heartbeatInterval(PingAnalyser.MIN_SANE_HEARTBEAT_VALUE)
				.add(AnalysedSyncCollection.builder()
						.collectionId(collectionId)
						.syncKey(SyncKey.INITIAL_SYNC_KEY)
						.dataType(PIMDataType.EMAIL)
						.windowSize(50)
						.build())
				.build());
	}
	
	@Test
	public void testAnalysePingWithKnownState() throws Exception {
		SyncKey syncKey = new SyncKey("123");
		PingRequest pingRequest = PingRequest.builder()
				.add(SyncCollection.builder()
						.collectionId(collectionId)
						.syncKey(syncKey)
						.build())
				.build();
		
		long heartbeat = 100;
		expect(heartbeatDao.findLastHeartbeat(device))
			.andReturn(heartbeat);

		SyncKey knownSyncKey = new SyncKey("456");
		ItemSyncState itemSyncState = ItemSyncState.builder()
				.syncKey(knownSyncKey)
				.syncDate(DateUtils.getCurrentDate())
				.build();
		expect(stateMachine.lastKnownState(device, collectionId))
			.andReturn(itemSyncState).once();
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		
		expect(configuration.defaultWindowSize())
			.andReturn(50).once();
		
		mocks.replay();
		
		AnalysedPingRequest analysedPingRequest = pingAnalyser.analysePing(udr, pingRequest);
		
		mocks.verify();
		assertThat(analysedPingRequest).isEqualTo(AnalysedPingRequest.builder()
				.heartbeatInterval(heartbeat)
				.add(AnalysedSyncCollection.builder()
						.collectionId(collectionId)
						.syncKey(knownSyncKey)
						.dataType(PIMDataType.EMAIL)
						.windowSize(50)
						.build())
				.build());
	}
	
	@Test
	public void testAnalysePingTwoSyncCollections() throws Exception {
		SyncKey syncKey = new SyncKey("123");
		CollectionId collectionId2 = CollectionId.of(785);
		PingRequest pingRequest = PingRequest.builder()
				.add(SyncCollection.builder()
						.collectionId(collectionId)
						.syncKey(syncKey)
						.build())
				.add(SyncCollection.builder()
						.collectionId(collectionId2)
						.syncKey(syncKey)
						.build())
				.build();
		
		long heartbeat = 100;
		expect(heartbeatDao.findLastHeartbeat(device))
			.andReturn(heartbeat).once();

		SyncKey knownSyncKey = new SyncKey("456");
		ItemSyncState itemSyncState = ItemSyncState.builder()
				.syncKey(knownSyncKey)
				.syncDate(DateUtils.getCurrentDate())
				.build();
		expect(stateMachine.lastKnownState(device, collectionId))
			.andReturn(itemSyncState).once();
		expect(stateMachine.lastKnownState(device, collectionId2))
			.andReturn(null).once();
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expect(folderSnapshotDao.get(user, device, collectionId2)).andReturn(Folder.builder()
			.displayName("address book")
			.collectionId(collectionId2)
			.backendId(AddressBookId.of(8))
			.folderType(FolderType.DEFAULT_CONTACTS_FOLDER)
			.build());
		
		expect(configuration.defaultWindowSize())
			.andReturn(50).times(2);
		
		mocks.replay();
		
		AnalysedPingRequest analysedPingRequest = pingAnalyser.analysePing(udr, pingRequest);
		
		mocks.verify();
		assertThat(analysedPingRequest).isEqualTo(AnalysedPingRequest.builder()
				.heartbeatInterval(heartbeat)
				.add(AnalysedSyncCollection.builder()
						.collectionId(collectionId)
						.syncKey(knownSyncKey)
						.dataType(PIMDataType.EMAIL)
						.windowSize(50)
						.build())
				.add(AnalysedSyncCollection.builder()
						.collectionId(collectionId2)
						.syncKey(SyncKey.INITIAL_SYNC_KEY)
						.dataType(PIMDataType.CONTACTS)
						.windowSize(50)
						.build())
				.build());
	}
}
