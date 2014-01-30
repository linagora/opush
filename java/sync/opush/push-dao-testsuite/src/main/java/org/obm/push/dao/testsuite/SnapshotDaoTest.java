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
import org.obm.push.bean.SnapshotKey;
import org.obm.push.bean.SyncKey;
import org.obm.push.exception.DaoException;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.store.SnapshotDao;
import org.obm.push.utils.DateUtils;

@RunWith(GuiceRunner.class)
public abstract class SnapshotDaoTest {

	protected SnapshotDao snapshotDao;
	
	@Test
	public void getWhenSyncKeyDoesNotMatch() {
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();
		SnapshotKey snapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("deviceId"))
				.syncKey(new SyncKey("8b7d5982-cbb3-4cd0-8151-0d20f9118fe7"))
				.collectionId(1).build();
		Snapshot storedSnapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(2)
				.addEmail(email).build();
		
		snapshotDao.put(snapshotKey, storedSnapshot);

		SnapshotKey otherSnapshotKey = SnapshotKey.builder()
				.deviceId(snapshotKey.getDeviceId())
				.syncKey(new SyncKey("c1c98fd7-5692-44fe-a73f-85b6ef0dcaa6"))
				.collectionId(snapshotKey.getCollectionId()).build();
		Snapshot snapshot = snapshotDao.get(otherSnapshotKey);
		
		assertThat(snapshot).isNull();
	}
	
	@Test
	public void getWhenCollectionDoesNotMatch() {
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();
		SnapshotKey snapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("deviceId"))
				.syncKey(new SyncKey("b2704aef-26a7-49eb-baf8-e1a3efbccf8b"))
				.collectionId(1).build();
		Snapshot storedSnapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(2)
				.addEmail(email).build();
		
		snapshotDao.put(snapshotKey, storedSnapshot);

		SnapshotKey otherSnapshotKey = SnapshotKey.builder()
				.deviceId(snapshotKey.getDeviceId())
				.syncKey(snapshotKey.getSyncKey())
				.collectionId(15).build();
		Snapshot snapshot = snapshotDao.get(otherSnapshotKey);
		
		assertThat(snapshot).isNull();
	}
	
	@Test
	public void getWhenDeviceDoesNotMatch() {
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();
		SnapshotKey snapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("deviceId"))
				.syncKey(new SyncKey("2ae02b70-3de8-4da0-8241-3cb7e948ab24"))
				.collectionId(1).build();
		Snapshot storedSnapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(2)
				.addEmail(email).build();
		
		snapshotDao.put(snapshotKey, storedSnapshot);

		SnapshotKey otherSnapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("otherDeviceId"))
				.syncKey(snapshotKey.getSyncKey())
				.collectionId(snapshotKey.getCollectionId()).build();
		Snapshot snapshot = snapshotDao.get(otherSnapshotKey);
		
		assertThat(snapshot).isNull();
	}
	
	@Test
	public void getWhenParamsMatch() {
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();
		SnapshotKey snapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("deviceId"))
				.syncKey(new SyncKey("8b5dd1d5-9fd7-423f-81c7-89ffe4e5cfb6"))
				.collectionId(1).build();
		Snapshot expectedSnapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(5)
				.addEmail(email)
				.build();
		
		snapshotDao.put(snapshotKey, expectedSnapshot);
		Snapshot snapshot = snapshotDao.get(snapshotKey);
		
		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}
	
	@Test(expected=DaoException.class)
	public void linkWhenSyncKeyDoesNotMatch() {
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();
		SnapshotKey snapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("deviceId"))
				.syncKey(new SyncKey("079962a8-ffa5-48ca-9de5-de0949a55b32"))
				.collectionId(1).build();
		Snapshot storedSnapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(2)
				.addEmail(email).build();
		
		snapshotDao.put(snapshotKey, storedSnapshot);

		SnapshotKey otherSnapshotKey = SnapshotKey.builder()
				.deviceId(snapshotKey.getDeviceId())
				.syncKey(new SyncKey("0a713c3f-a79b-43bb-a706-d467ab62f37b"))
				.collectionId(snapshotKey.getCollectionId()).build();
		SyncKey linkingSyncKey = new SyncKey("25df65ac-ed3e-46ab-9d36-9ca2e352b407");
		snapshotDao.linkSyncKeyToSnapshot(linkingSyncKey, otherSnapshotKey);
	}

	@Test(expected=DaoException.class)
	public void linkWhenCollectionDoesNotMatch() {
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();
		SnapshotKey snapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("deviceId"))
				.syncKey(new SyncKey("d4c03a3e-2c08-4a4e-aea5-646189c8b1ab"))
				.collectionId(1).build();
		Snapshot storedSnapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(2)
				.addEmail(email).build();
		
		snapshotDao.put(snapshotKey, storedSnapshot);

		SnapshotKey otherSnapshotKey = SnapshotKey.builder()
				.deviceId(snapshotKey.getDeviceId())
				.syncKey(snapshotKey.getSyncKey())
				.collectionId(546).build();
		SyncKey linkingSyncKey = new SyncKey("cb32c35b-0b8a-4ccd-b36b-130f3c96777e");
		snapshotDao.linkSyncKeyToSnapshot(linkingSyncKey, otherSnapshotKey);
	}

	@Test(expected=DaoException.class)
	public void linkWhenDeviceDoesNotMatch() {
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();
		SnapshotKey snapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("deviceId"))
				.syncKey(new SyncKey("5e1a2713-ee71-44af-87ae-0e46e428ca8d"))
				.collectionId(1).build();
		Snapshot storedSnapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(2)
				.addEmail(email).build();
		
		snapshotDao.put(snapshotKey, storedSnapshot);

		SnapshotKey otherSnapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("otherDeviceId"))
				.syncKey(snapshotKey.getSyncKey())
				.collectionId(snapshotKey.getCollectionId()).build();
		SyncKey linkingSyncKey = new SyncKey("5257f839-fe1b-47cf-a818-c988d49d8624");
		snapshotDao.linkSyncKeyToSnapshot(linkingSyncKey, otherSnapshotKey);
	}
	
	@Test
	public void linkWhenParamsMatch() {
		Email email = Email.builder().uid(3).read(false).date(DateUtils.getCurrentDate()).build();
		SnapshotKey snapshotKey = SnapshotKey.builder()
				.deviceId(new DeviceId("deviceId"))
				.syncKey(new SyncKey("8b5dd1d5-9fd7-423f-81c7-89ffe4e5cfb6"))
				.collectionId(1).build();
		Snapshot expectedSnapshot = Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(5)
				.addEmail(email)
				.build();
		
		snapshotDao.put(snapshotKey, expectedSnapshot);

		SyncKey linkingSyncKey = new SyncKey("5257f839-fe1b-47cf-a818-c988d49d8624");
		snapshotDao.linkSyncKeyToSnapshot(linkingSyncKey, snapshotKey);
		
		SnapshotKey linkedSnapshotKey = SnapshotKey.builder()
				.deviceId(snapshotKey.getDeviceId())
				.syncKey(linkingSyncKey)
				.collectionId(snapshotKey.getCollectionId()).build();
		Snapshot snapshot = snapshotDao.get(linkedSnapshotKey);
		
		assertThat(snapshot).isEqualTo(expectedSnapshot);
	}
	
	@Test(expected=NullPointerException.class)
	public void putWhenSnapshotKeyIsNull() {
		snapshotDao.put(null, 
			Snapshot.builder()
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(5)
				.build());
	}
	
	@Test(expected=NullPointerException.class)
	public void putWhenSnapshotIsNull() {
		snapshotDao.put(
			SnapshotKey.builder()
				.deviceId(new DeviceId("deviceId"))
				.syncKey(new SyncKey("f4ed6b42-979e-4343-8c9b-a6fcc9ae37aa"))
				.collectionId(1).build(),
			null);
	}
}
