/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.push.bean.change;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.change.WindowingKey;
import org.obm.push.protocol.bean.CollectionId;

public class WindowingKeyTest {

	@SuppressWarnings("unused")
	@Test(expected=IllegalArgumentException.class)
	public void testPreconditionUserNull() {
		User user = null;
		DeviceId deviceId = new DeviceId("132");
		CollectionId collectionId = CollectionId.of(5);
		SyncKey syncKey = new SyncKey("123");
		new WindowingKey(user, deviceId, collectionId, syncKey);
	}

	@SuppressWarnings("unused")
	@Test(expected=IllegalArgumentException.class)
	public void testPreconditionDeviceIdNull() {
		User user = Factory.create().createUser("user@domain", "user@domain", "user@domain");
		DeviceId deviceId = null;
		SyncKey syncKey = new SyncKey("123");
		CollectionId collectionId = CollectionId.of(5);
		new WindowingKey(user, deviceId, collectionId, syncKey);
	}

	@SuppressWarnings("unused")
	@Test(expected=IllegalArgumentException.class)
	public void testPreconditionSyncKeyNull() {
		User user = Factory.create().createUser("user@domain", "user@domain", "user@domain");
		DeviceId deviceId = new DeviceId("132");
		SyncKey syncKey = null;
		CollectionId collectionId = CollectionId.of(5);
		new WindowingKey(user, deviceId, collectionId, syncKey);
	}

	@Test
	public void testWindowingKey() {
		User user = Factory.create().createUser("user@domain", "user@domain", "user@domain");
		DeviceId deviceId = new DeviceId("132");
		SyncKey syncKey = new SyncKey("123");
		CollectionId collectionId = CollectionId.of(5);
		WindowingKey windowingKey = new WindowingKey(user, deviceId, collectionId, syncKey);
		
		assertThat(windowingKey.getUser()).isEqualTo(user);
		assertThat(windowingKey.getDeviceId()).isEqualTo(deviceId);
		assertThat(windowingKey.getCollectionId()).isEqualTo(collectionId);
		assertThat(windowingKey.getSyncKey()).isEqualTo(syncKey);
	}
}
