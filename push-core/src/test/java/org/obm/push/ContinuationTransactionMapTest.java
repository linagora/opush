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
package org.obm.push;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.ContinuationTransactionMap;
import org.obm.push.ElementNotFoundException;
import org.obm.push.ProtocolVersion;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.User;

public class ContinuationTransactionMapTest {


	public final static String PENDING_CONTINUATIONS = "pendingContinuation";
	public final static String KEY_ID_REQUEST = "key_id_request";
	private Device device;
	private User user;
	private ContinuationTransactionMap<TestingContinuation> testee;
	
	@Before
	public void setUp() {
		testee = new ContinuationTransactionMap<TestingContinuation>();
		device = new Device(1, "devType", new DeviceId("devId"), new Properties(), ProtocolVersion.V121);
		user = User.builder().email("jean.jaures@sfio.fr").domain("sfio.fr").login("jjaures").build();
	}
	
	@Test
	public void testGetContinuationForDevice() throws ElementNotFoundException {
		TestingContinuation expectedContinuation = new TestingContinuation();
		testee.putContinuationForDevice(user, device, expectedContinuation);
		
		assertThat(testee.getContinuationForDevice(user, device)).isEqualTo(expectedContinuation);
	}
	
	@Test (expected=ElementNotFoundException.class)
	public void testGetContinuationForDeviceElementNotFound() throws ElementNotFoundException {
		testee.getContinuationForDevice(user, device);
	}
	
	@Test
	public void testPutContinuationForDevice() {
		TestingContinuation expectedContinuation = new TestingContinuation();
		testee.putContinuationForDevice(user, device, expectedContinuation);

		boolean hasPreviousElement = testee.putContinuationForDevice(user, device, expectedContinuation);
		
		assertThat(hasPreviousElement).isTrue();
	}
	
	@Test
	public void testPutContinuationForDeviceButDifferentUser() {
		TestingContinuation expectedContinuation = new TestingContinuation();
		testee.putContinuationForDevice(user, device, expectedContinuation);
		User blum = User.builder().email("leon.blum@sfio.fr").domain("sfio.fr").login("bblum").build();
		
		boolean hasPreviousElement = testee.putContinuationForDevice(blum, device, expectedContinuation);
		
		assertThat(hasPreviousElement).isFalse();
	}
	
	@Test
	public void testPutContinuationForDeviceNoCachedElement() {
		TestingContinuation expectedContinuation = new TestingContinuation();
		
		boolean hasPreviousElement = testee.putContinuationForDevice(user, device, expectedContinuation);
		
		assertThat(hasPreviousElement).isFalse();
	}
	
	@Test(expected=ElementNotFoundException.class)
	public void testDelete() throws ElementNotFoundException {
		TestingContinuation expectedContinuation = new TestingContinuation();
		testee.putContinuationForDevice(user, device, expectedContinuation);
		
		testee.delete(user, device);

		testee.getContinuationForDevice(user, device);
	}

	@Test(expected=ElementNotFoundException.class)
	public void testDeleteNotCachedElement() throws ElementNotFoundException {
		testee.delete(user, device);
		testee.getContinuationForDevice(user, device);
	}
	
	public static class TestingContinuation {}
}
