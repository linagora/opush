/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013-2014  Linagora
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
package org.obm.push.dao.testsuite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.guice.GuiceRunner;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.SyncKey;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.store.SnapshotDao;
import org.obm.push.utils.DateUtils;

@RunWith(GuiceRunner.class)
public abstract class SnapshotDaoTest {

	protected SnapshotDao snapshotDao;
	
	@Test
	public void getWhenSyncKeyDoesNotMatch() {
		SyncKey syncKey = new SyncKey("8b7d5982-cbb3-4cd0-8151-0d20f9118fe7");
		DeviceId deviceId = new DeviceId("deviceId");
		Integer collectionId = 1;
		int uidNext = 2;
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();

		Snapshot storedSnapshot = Snapshot.builder()
				.deviceId(deviceId)
				.filterType(FilterType.THREE_DAYS_BACK)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.uidNext(uidNext)
				.addEmail(email)
				.build();
		
		snapshotDao.put(storedSnapshot);

		SyncKey otherSyncKey = new SyncKey("c1c98fd7-5692-44fe-a73f-85b6ef0dcaa6");
		Snapshot snapshot = snapshotDao.get(deviceId, otherSyncKey, collectionId);
		
		assertThat(snapshot).isNull();
	}
	
	@Test
	public void getWhenCollectionDoesNotMatch() {
		SyncKey syncKey = new SyncKey("b2704aef-26a7-49eb-baf8-e1a3efbccf8b");
		DeviceId deviceId = new DeviceId("deviceId");
		Integer collectionId = 1;
		int uidNext = 2;
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();

		Snapshot storedSnapshot = Snapshot.builder()
				.deviceId(deviceId)
				.filterType(FilterType.THREE_DAYS_BACK)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.uidNext(uidNext)
				.addEmail(email)
				.build();
		
		snapshotDao.put(storedSnapshot);

		Integer otherCollection = 15;
		Snapshot snapshot = snapshotDao.get(deviceId, syncKey, otherCollection);
		
		assertThat(snapshot).isNull();
	}
	
	@Test
	public void getWhenDeviceDoesNotMatch() {
		SyncKey syncKey = new SyncKey("2ae02b70-3de8-4da0-8241-3cb7e948ab24");
		DeviceId deviceId = new DeviceId("deviceId");
		Integer collectionId = 1;
		int uidNext = 2;
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();

		Snapshot storedSnapshot = Snapshot.builder()
				.deviceId(deviceId)
				.filterType(FilterType.THREE_DAYS_BACK)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.uidNext(uidNext)
				.addEmail(email)
				.build();
		
		snapshotDao.put(storedSnapshot);

		DeviceId otherDeviceId = new DeviceId("otherDeviceId");
		Snapshot snapshot = snapshotDao.get(otherDeviceId, syncKey, collectionId);
		
		assertThat(snapshot).isNull();
	}
	
	@Test
	public void getWhenParamsMatch() {
		SyncKey syncKey = new SyncKey("8b5dd1d5-9fd7-423f-81c7-89ffe4e5cfb6");
		DeviceId deviceId = new DeviceId("deviceId");
		Integer collectionId = 1;
		int uidNext = 2;
		Email email = Email.builder()
				.uid(3)
				.read(false)
				.date(DateUtils.getCurrentDate())
				.build();
		
		Snapshot expectedSnapshot = Snapshot.builder()
				.deviceId(deviceId)
				.filterType(FilterType.THREE_DAYS_BACK)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.uidNext(uidNext)
				.addEmail(email)
				.build();
		
		snapshotDao.put(expectedSnapshot);
		Snapshot snapshot = snapshotDao.get(deviceId, syncKey, collectionId);
		
		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}
	
	@Test(expected=NullPointerException.class)
	public void putWhenSnapshotIsNull() {
		snapshotDao.put(null);
	}
}
