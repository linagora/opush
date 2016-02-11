/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2016 Linagora
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

import static org.assertj.guava.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.guice.GuiceRunner;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.change.WindowingKey;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.store.WindowingToSnapshotDao;

@RunWith(GuiceRunner.class)
public abstract class WindowingToSnapshotDaoTest {

	protected CollectionId collectionId;
	protected DeviceId deviceId;
	protected User user;
	protected UUID snapshotId;
	protected SyncKey syncKey;
	protected WindowingKey windowingKey;

	protected WindowingToSnapshotDao testee;

	@Before
	public void setup() {
		collectionId = CollectionId.of(5);
		deviceId = new DeviceId("ab123");
		user = Factory.create().createUser("user@domain", "user@domain", "user@domain");
		
		snapshotId = UUID.fromString("218177c3-8a4f-40f6-a0a7-8dbb85b9e6d4");
		syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		windowingKey = new WindowingKey(user, deviceId, collectionId, syncKey);
	}
	
	@Test
	public void getShouldReturnEmptyOptionWhenThereIsNoRecord() {
		assertThat(testee.get(windowingKey)).isAbsent();
	}
	
	@Test
	public void getShouldReturnEmptyOptionWhenNoMatchingKey() {
		SyncKey otherSyncKey = new SyncKey("c2e0bfe5-228f-4f81-b291-9995f34712ae");
		
		testee.startWindowing(windowingKey, snapshotId);
		
		assertThat(testee.get(windowingKey.withSyncKey(otherSyncKey))).isAbsent();
	}
	
	@Test
	public void getShouldReturnExpectedUUIDWhenMatchingStartWindowingKey() {
		testee.startWindowing(windowingKey, snapshotId);
		
		assertThat(testee.get(windowingKey)).contains(snapshotId);
	}
	
	@Test
	public void getShouldReturnExpectedUUIDWhenMatchingInProgressWindowingKey() {
		SyncKey otherSyncKey = new SyncKey("c2e0bfe5-228f-4f81-b291-9995f34712ae");
		WindowingKey windowingKeyInProgress = windowingKey.withSyncKey(otherSyncKey);
		
		testee.startWindowing(windowingKey, snapshotId);
		testee.windowingInProgress(otherSyncKey, windowingKey);
		
		assertThat(testee.get(windowingKeyInProgress)).contains(snapshotId);
	}
	
	@Test
	public void getShouldKeepUUIDWhenWindowingIsProgressing() {
		SyncKey otherSyncKey = new SyncKey("c2e0bfe5-228f-4f81-b291-9995f34712ae");
		SyncKey otherSyncKey2 = new SyncKey("cde8b511-cf11-43b3-a8b2-0535ac9fb312");
		WindowingKey windowingKeyInProgress = windowingKey.withSyncKey(otherSyncKey);
		WindowingKey windowingKeyInProgress2 = windowingKeyInProgress.withSyncKey(otherSyncKey2);
		
		testee.startWindowing(windowingKey, snapshotId);
		testee.windowingInProgress(otherSyncKey, windowingKey);
		testee.windowingInProgress(otherSyncKey2, windowingKeyInProgress);
		
		assertThat(testee.get(windowingKey)).contains(snapshotId);
		assertThat(testee.get(windowingKeyInProgress)).contains(snapshotId);
		assertThat(testee.get(windowingKeyInProgress2)).contains(snapshotId);
	}
	
	@Test
	public void getShouldFindExpectedUUIDWhenParallelWindowingsAreProgressing() {
		UUID otherSnapshotId = UUID.fromString("cde8b511-cf11-43b3-a8b2-0535ac9fb312");
		SyncKey otherSyncKey = new SyncKey("c2e0bfe5-228f-4f81-b291-9995f34712ae");
		WindowingKey otherWindowingKey = new WindowingKey(user, deviceId, CollectionId.of(1337), otherSyncKey);
		
		SyncKey otherSyncKey2 = new SyncKey("89476cf6-09fa-49c9-a9cb-d966b197bde5");
		SyncKey otherSyncKey3 = new SyncKey("af263938-136d-4e1b-8baf-0a2d926bb301");
		WindowingKey windowingKeyInProgress = windowingKey.withSyncKey(otherSyncKey2);
		WindowingKey otherWindowingKeyInProgress = otherWindowingKey.withSyncKey(otherSyncKey3);
		
		testee.startWindowing(windowingKey, snapshotId);
		testee.startWindowing(otherWindowingKey, otherSnapshotId);
		testee.windowingInProgress(otherSyncKey2, windowingKey);
		testee.windowingInProgress(otherSyncKey3, otherWindowingKey);
		
		assertThat(testee.get(windowingKey)).contains(snapshotId);
		assertThat(testee.get(windowingKeyInProgress)).contains(snapshotId);

		assertThat(testee.get(otherWindowingKey)).contains(otherSnapshotId);
		assertThat(testee.get(otherWindowingKeyInProgress)).contains(otherSnapshotId);
	}
	
	@Test
	public void startTwiceWithDifferentValuesShouldOverrideTheFirstOne() {
		UUID newSnapshotId = UUID.fromString("cde8b511-cf11-43b3-a8b2-0535ac9fb312");
		
		testee.startWindowing(windowingKey, snapshotId);
		testee.startWindowing(windowingKey, newSnapshotId);
		
		assertThat(testee.get(windowingKey)).contains(newSnapshotId);
	}
	
	@Test
	public void windowingInProgressShouldDoNothingWhenNotPreviouslyStarted() {
		SyncKey otherSyncKey = new SyncKey("89476cf6-09fa-49c9-a9cb-d966b197bde5");
		
		testee.windowingInProgress(otherSyncKey, windowingKey);
		
		assertThat(testee.get(windowingKey.withSyncKey(otherSyncKey))).isAbsent();
	}
}
