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
package org.obm.push.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Date;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.ProtocolVersion;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncCollectionCommand;
import org.obm.push.bean.SyncCollectionCommandsResponse;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncCollectionResponsesResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.exception.activesync.InvalidSyncKeyException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.ItemTrackingDao;
import org.obm.push.utils.DateUtils;
import org.obm.push.utils.UUIDFactory;

import com.google.common.collect.ImmutableSet;


public class StateMachineTest {
	
	SyncKeyFactory syncKeyFactory;
	User user;
	Device device;
	UserDataRequest udr;
	IMocksControl control;

	@Before
	public void setUp() {
		UUIDFactory uuidFactory = new UUIDFactory() {};
		syncKeyFactory = new SyncKeyFactory(uuidFactory);

		user = Factory.create().createUser("test@test", "test@domain", "displayName");
		device = new Device.Factory().create(3, "type", "agent", new DeviceId("my phone"), ProtocolVersion.V121);
		udr = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		
		control = createControl();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetFolderSyncStateWithNullKey() throws Exception {
		StateMachine stateMachine = new StateMachine(null , null, syncKeyFactory);
		
		stateMachine.getFolderSyncState(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetFolderSyncStateWithEmptyKey() throws Exception {
		StateMachine stateMachine = new StateMachine(null , null, syncKeyFactory);
		
		stateMachine.getFolderSyncState(new SyncKey(""));
	}

	@Test
	public void testGetFolderSyncStateWithInitialKey() throws Exception {
		SyncKey initialSyncKey = SyncKey.INITIAL_FOLDER_SYNC_KEY;

		StateMachine stateMachine = new StateMachine(null , null, syncKeyFactory);
		FolderSyncState folderSyncState = stateMachine.getFolderSyncState(initialSyncKey);
		
		assertThat(folderSyncState.getSyncKey()).isEqualTo(initialSyncKey);
		assertThat(folderSyncState.isInitialFolderSync()).isTrue();
	}

	@Test
	public void testGetFolderSyncStateWithKnownKey() throws Exception {
		SyncKey knownSyncKey = new SyncKey("1234");
		int knownSyncStateId = 156;
		FolderSyncState knownFolderSyncState = FolderSyncState.builder()
				.syncKey(knownSyncKey)
				.id(knownSyncStateId)
				.build();
		
		CollectionDao collectionDao = control.createMock(CollectionDao.class);
		expect(collectionDao.findFolderStateForKey(knownSyncKey)).andReturn(knownFolderSyncState).once();
		
		control.replay();
		StateMachine stateMachine = new StateMachine(collectionDao , null, syncKeyFactory);
		FolderSyncState folderSyncState = stateMachine.getFolderSyncState(knownSyncKey);
		control.verify();

		assertThat(folderSyncState.getId()).isEqualTo(knownSyncStateId);
		assertThat(folderSyncState.getSyncKey()).isEqualTo(knownSyncKey);
		assertThat(folderSyncState.isInitialFolderSync()).isFalse();
	}

	@Test(expected=InvalidSyncKeyException.class)
	public void testGetFolderSyncStateWithUnknownKey() throws Exception {
		SyncKey unknownSyncKey = new SyncKey("1234");
		
		CollectionDao collectionDao = control.createMock(CollectionDao.class);
		expect(collectionDao.findFolderStateForKey(unknownSyncKey)).andReturn(null).once();

		try {
			control.replay();
			StateMachine stateMachine = new StateMachine(collectionDao , null, syncKeyFactory);
			stateMachine.getFolderSyncState(unknownSyncKey);
		} catch (Exception e) {
			control.verify();
			throw e;
		}
	}

	@Test
	public void allocateNewSyncStateShouldTrackAllCommandAddsAndDeletions() throws Exception {
		CollectionId collectionId = CollectionId.of(12);
		Date lastSync = DateUtils.getCurrentDate();
		SyncKey newSyncKey = new SyncKey("a1fa04d6-ba8b-4a9a-9529-1d0bb7a359b1");
		ItemSyncState allocatedState = ItemSyncState.builder().syncKey(newSyncKey).syncDate(lastSync).build();

		SyncCollectionResponse response = SyncCollectionResponse.builder()
			.collectionId(collectionId)
			.dataType(PIMDataType.CALENDAR)
			.moreAvailable(false)
			.status(SyncStatus.OK)
			.syncKey(newSyncKey)
			.responses(SyncCollectionResponsesResponse.empty())
			.commands(SyncCollectionCommandsResponse.builder()
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.OK)
					.type(SyncCommand.ADD)
					.serverId(collectionId.serverId(5))
					.build())
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.SERVER_ERROR)
					.type(SyncCommand.ADD)
					.serverId(collectionId.serverId(6))
					.build())
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.OK)
					.type(SyncCommand.DELETE)
					.serverId(collectionId.serverId(1))
					.build())
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.SERVER_ERROR)
					.type(SyncCommand.DELETE)
					.serverId(collectionId.serverId(2))
					.build())
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.CONFLICT)
					.type(SyncCommand.DELETE)
					.serverId(collectionId.serverId(3))
					.build())
				.build())
			.build();
		
		CollectionDao collectionDao = control.createMock(CollectionDao.class);
		expect(collectionDao.updateState(device, CollectionId.of(12), newSyncKey, lastSync)).andReturn(allocatedState);
		
		ItemTrackingDao itemTrackingDao = control.createMock(ItemTrackingDao.class);
		itemTrackingDao.markAsSynced(allocatedState, ImmutableSet.of(collectionId.serverId(5), collectionId.serverId(6)));
		expectLastCall();
		itemTrackingDao.markAsDeleted(allocatedState, ImmutableSet.of(collectionId.serverId(1), collectionId.serverId(2), collectionId.serverId(3)));
		expectLastCall();
		
		control.replay();
		StateMachine stateMachine = new StateMachine(collectionDao , itemTrackingDao, syncKeyFactory);
		stateMachine.allocateNewSyncState(udr, collectionId, lastSync, response, newSyncKey);
		control.verify();
	}

	@Test
	public void allocateNewSyncStateShouldTrackOnlyOKResponses() throws Exception {
		CollectionId collectionId = CollectionId.of(12);
		Date lastSync = DateUtils.getCurrentDate();
		SyncKey newSyncKey = new SyncKey("a1fa04d6-ba8b-4a9a-9529-1d0bb7a359b1");
		ItemSyncState allocatedState = ItemSyncState.builder().syncKey(newSyncKey).syncDate(lastSync).build();

		SyncCollectionResponse response = SyncCollectionResponse.builder()
			.collectionId(collectionId)
			.dataType(PIMDataType.CALENDAR)
			.moreAvailable(false)
			.status(SyncStatus.OK)
			.syncKey(newSyncKey)
			.commands(SyncCollectionCommandsResponse.empty())
			.responses(SyncCollectionResponsesResponse.builder()
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.OK)
					.type(SyncCommand.ADD)
					.serverId(collectionId.serverId(5))
					.build())
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.SERVER_ERROR)
					.type(SyncCommand.ADD)
					.serverId(collectionId.serverId(6))
					.build())
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.OK)
					.type(SyncCommand.DELETE)
					.serverId(collectionId.serverId(1))
					.build())
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.OBJECT_NOT_FOUND)
					.type(SyncCommand.DELETE)
					.serverId(collectionId.serverId(2))
					.build())
				.addCommand(SyncCollectionCommand.builder()
					.status(SyncStatus.CONFLICT)
					.type(SyncCommand.DELETE)
					.serverId(collectionId.serverId(3))
					.build())
				.build())
			.build();
		
		CollectionDao collectionDao = control.createMock(CollectionDao.class);
		expect(collectionDao.updateState(device, CollectionId.of(12), newSyncKey, lastSync)).andReturn(allocatedState);
		
		ItemTrackingDao itemTrackingDao = control.createMock(ItemTrackingDao.class);
		itemTrackingDao.markAsSynced(allocatedState, ImmutableSet.of(collectionId.serverId(5)));
		expectLastCall();
		itemTrackingDao.markAsDeleted(allocatedState, ImmutableSet.of(collectionId.serverId(1)));
		expectLastCall();
		
		control.replay();
		StateMachine stateMachine = new StateMachine(collectionDao , itemTrackingDao, syncKeyFactory);
		stateMachine.allocateNewSyncState(udr, collectionId, lastSync, response, newSyncKey);
		control.verify();
	}

	@Test
	public void allocateNewSyncStateWithoutTrackingShouldStoreUpdatedState() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		Date lastSync = DateUtils.getCurrentDate();
		SyncKey newSyncKey = new SyncKey("a1fa04d6-ba8b-4a9a-9529-1d0bb7a359b1");
		
		CollectionDao collectionDao = control.createMock(CollectionDao.class);
		expect(collectionDao.updateState(device, CollectionId.of(1), newSyncKey, lastSync))
			.andReturn(ItemSyncState.builder()
					.syncKey(newSyncKey)
					.syncDate(lastSync)
					.build());
		
		control.replay();
		StateMachine stateMachine = new StateMachine(collectionDao , null, syncKeyFactory);
		stateMachine.allocateNewSyncStateWithoutTracking(udr, collectionId, lastSync, newSyncKey);
		control.verify();
	}
}
