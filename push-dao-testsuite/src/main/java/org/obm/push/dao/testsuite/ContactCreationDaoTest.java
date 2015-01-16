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

import static org.assertj.guava.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.guice.GuiceRunner;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.User;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.store.ContactCreationDao;

import com.google.common.hash.HashCode;

@RunWith(GuiceRunner.class)
public abstract class ContactCreationDaoTest {

	protected ContactCreationDao testee;
	
	private User user;
	private DeviceId deviceId;
	private CollectionId collectionId;
	private HashCode hashCode;
	private ServerId serverId;

	@Before
	public void setUp() {
		user = User.builder().login("login").domain("domain").email("user@domain").build();
		deviceId = new DeviceId("deviceId");
		collectionId = CollectionId.of(5);
		hashCode = HashCode.fromString("abcd");
		serverId = ServerId.of("5:12");
	}
	
	@Test(expected=NullPointerException.class)
	public void registerCreationWhenUserIsNull() {
		testee.registerCreation(null, deviceId, collectionId, hashCode, serverId);
	}
	
	@Test(expected=NullPointerException.class)
	public void registerCreationWhenDeviceIdIsNull() {
		testee.registerCreation(user, null, collectionId, hashCode, serverId);
	}
	
	@Test(expected=NullPointerException.class)
	public void registerCreationWhenCollectionIdIsNull() {
		testee.registerCreation(user, deviceId, null, hashCode, serverId);
	}
	
	@Test(expected=NullPointerException.class)
	public void registerCreationWhenHashCodeIsNull() {
		testee.registerCreation(user, deviceId, collectionId, null, serverId);
	}
	
	@Test(expected=NullPointerException.class)
	public void registerCreationWhenServerIdIsNull() {
		testee.registerCreation(user, deviceId, collectionId, hashCode, null);
	}

	@Test
	public void registerCreationShouldMakeItFindableWithMatchingParams() {
		testee.registerCreation(user, deviceId, collectionId, hashCode, serverId);
		assertThat(testee.find(user, deviceId, collectionId, hashCode)).contains(serverId);
	}
	
	@Test
	public void registerCreationShouldMakeItUnfindableWithDifferentUser() {
		testee.registerCreation(user, deviceId, collectionId, hashCode, serverId);
		User otherUser = User.builder().login("otherlogin").domain("otherdomain").email("otheruser@domain").build();
		assertThat(testee.find(otherUser, deviceId, collectionId, hashCode)).isAbsent();
	}
	
	@Test
	public void registerCreationShouldMakeItUnfindableWithDifferentDeviceId() {
		testee.registerCreation(user, deviceId, collectionId, hashCode, serverId);
		assertThat(testee.find(user, new DeviceId("other one"), collectionId, hashCode)).isAbsent();
	}
	
	@Test
	public void registerCreationShouldMakeItUnfindableWithDifferentCollectionId() {
		testee.registerCreation(user, deviceId, collectionId, hashCode, serverId);
		assertThat(testee.find(user, deviceId, CollectionId.of(6), hashCode)).isAbsent();
	}
	
	@Test
	public void registerCreationShouldMakeItUnfindableWithDifferentHashCode() {
		testee.registerCreation(user, deviceId, collectionId, hashCode, serverId);
		assertThat(testee.find(user, deviceId, collectionId, HashCode.fromString("0000"))).isAbsent();
	}

	@Test
	public void registerCreationShouldAcceptManyTimesTheSameCreation() {
		testee.registerCreation(user, deviceId, collectionId, hashCode, serverId);
		testee.registerCreation(user, deviceId, collectionId, hashCode, serverId);
		testee.registerCreation(user, deviceId, collectionId, hashCode, serverId);
		assertThat(testee.find(user, deviceId, collectionId, hashCode)).contains(serverId);
	}
	
	@Test
	public void registerCreationOnSomeItems() {
		User user2 = User.builder().login("otherlogin").domain("otherdomain").email("otheruser@domain").build();
		DeviceId deviceId2 = new DeviceId("deviceId 2");
		CollectionId collectionId2 = CollectionId.of(8);
		HashCode hashCode2 = HashCode.fromString("0001");
		ServerId serverId2 = ServerId.of("8:15");

		User user3 = User.builder().login("anotherlogin").domain("anotherdomain").email("anotheruser@domain").build();
		DeviceId deviceId3 = new DeviceId("deviceId 3");
		CollectionId collectionId3 = CollectionId.of(15);
		HashCode hashCode3 = HashCode.fromString("0002");
		ServerId serverId3 = ServerId.of("120:10");
		
		testee.registerCreation(user, deviceId, collectionId, hashCode, serverId);
		testee.registerCreation(user2, deviceId2, collectionId2, hashCode2, serverId2);
		testee.registerCreation(user3, deviceId3, collectionId3, hashCode3, serverId3);
		assertThat(testee.find(user, deviceId, collectionId, hashCode)).contains(serverId);
		assertThat(testee.find(user2, deviceId2, collectionId2, hashCode2)).contains(serverId2);
		assertThat(testee.find(user3, deviceId3, collectionId3, hashCode3)).contains(serverId3);
		assertThat(testee.find(user3, deviceId, collectionId3, hashCode3)).isAbsent();
		assertThat(testee.find(user3, deviceId3, collectionId2, hashCode3)).isAbsent();
	}
}