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
package org.obm.push.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.SyncKey;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.store.SnapshotDao;

public class SnapshotServiceImplTest {

	@Test
	public void testGetSnapshotEmpty() {
		DeviceId deviceId = new DeviceId("deviceId");
		SyncKey syncKey = new SyncKey("123");
		int collectionId = 1;
		
		SnapshotDao snapshotDao = createStrictMock(SnapshotDao.class);
		expect(snapshotDao.get(deviceId, syncKey, collectionId))
			.andReturn(null).once();
		
		replay(snapshotDao);
		
		SnapshotService snapshotService = new SnapshotServiceImpl(snapshotDao);
		Snapshot snapshot = snapshotService.getSnapshot(deviceId, syncKey, collectionId);
		
		verify(snapshotDao);
		assertThat(snapshot).isNull();
	}

	@Test
	public void testGetSnapshot() {
		DeviceId deviceId = new DeviceId("deviceId");
		SyncKey syncKey = new SyncKey("123");
		int collectionId = 1;
		
		Snapshot expectedSnapshot = Snapshot.builder()
				.deviceId(deviceId)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.filterType(FilterType.THREE_DAYS_BACK)
				.build();
		
		SnapshotDao snapshotDao = createStrictMock(SnapshotDao.class);
		expect(snapshotDao.get(deviceId, syncKey, collectionId))
			.andReturn(expectedSnapshot).once();
		
		replay(snapshotDao);
		
		SnapshotService snapshotService = new SnapshotServiceImpl(snapshotDao);
		Snapshot snapshot = snapshotService.getSnapshot(deviceId, syncKey, collectionId);
		
		verify(snapshotDao);
		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}

	@Test
	public void testStoreSnapshot() {
		DeviceId deviceId = new DeviceId("deviceId");
		SyncKey syncKey = new SyncKey("123");
		int collectionId = 1;
		
		Snapshot snapshot = Snapshot.builder()
				.deviceId(deviceId)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.filterType(FilterType.THREE_DAYS_BACK)
				.build();
		
		SnapshotDao snapshotDao = createStrictMock(SnapshotDao.class);
		snapshotDao.put(snapshot);
		expectLastCall().once();
		
		replay(snapshotDao);
		
		SnapshotService snapshotService = new SnapshotServiceImpl(snapshotDao);
		snapshotService.storeSnapshot(snapshot);
		
		verify(snapshotDao);
	}
}
