/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015 Linagora
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
package org.obm.push.contacts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.MSContact;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.User;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.store.ContactCreationDao;

import com.google.common.base.Optional;
import com.google.common.hash.HashCode;

public class ContactCreationIdempotenceServiceTest {

	private IMocksControl mocks;
	private ContactCreationDao dao;
	private UserDataRequest udr;
	private User user;
	private DeviceId deviceId;
	private ContactCreationIdempotenceService testee;

	@Before
	public void setUp() {
		mocks = EasyMock.createControl();
		dao = mocks.createMock(ContactCreationDao.class);
		udr = mocks.createMock(UserDataRequest.class);
		user = mocks.createMock(User.class);
		deviceId = mocks.createMock(DeviceId.class);
		
		expect(udr.getUser()).andReturn(user);
		expect(udr.getDevId()).andReturn(deviceId);
		
		testee = new ContactCreationIdempotenceService(dao);
	}

	@Test
	public void registerCreationShouldDelegateToDao() {
		MSContact contact = new MSContact();
		contact.setFirstName("firstname");
		contact.setLastName("lastname");
		contact.setEmail1Address("contact@mydomain.org");
		contact.setFileAs("lastname, firstname");
		
		CollectionId colId = CollectionId.of(56);
		ServerId serverId = colId.serverId(45);
		HashCode hash = testee.hash(contact);

		dao.registerCreation(user, deviceId, colId, hash, serverId);
		expectLastCall();
		
		mocks.replay();
		testee.registerCreation(udr, contact, serverId);
		mocks.verify();
	}

	@Test
	public void findShouldDelegateToDao() {
		MSContact contact = new MSContact();
		contact.setFirstName("firstname");
		contact.setLastName("lastname");
		contact.setEmail1Address("contact@mydomain.org");
		contact.setFileAs("lastname, firstname");
		
		CollectionId colId = CollectionId.of(56);
		ServerId serverId = ServerId.of("12:45");
		HashCode hash = testee.hash(contact);
		
		expect(dao.find(user, deviceId, colId, hash)).andReturn(Optional.of(serverId));
		
		mocks.replay();
		testee.find(udr, colId, contact);
		mocks.verify();
	}

	@Test
	public void hashShouldHaveSameResultWhenCalledWithSameMSContact() {
		MSContact contact1 = new MSContact();
		contact1.setFirstName("firstname");
		contact1.setLastName("lastname");
		contact1.setEmail1Address("contact@mydomain.org");
		contact1.setFileAs("lastname, firstname");

		MSContact contact2 = new MSContact();
		contact2.setFirstName(contact1.getFirstName());
		contact2.setLastName(contact1.getLastName());
		contact2.setEmail1Address(contact1.getEmail1Address());
		contact2.setFileAs(contact1.getFileAs());
		
		HashCode hash1 = testee.hash(contact1);
		HashCode hash2 = testee.hash(contact2);
		
		assertThat(hash1).isEqualTo(hash2);
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentFirstname() {
		MSContact contact1 = new MSContact();
		contact1.setFirstName("firstname");

		MSContact contact2 = new MSContact();
		contact2.setFirstName("other firstname");
		
		HashCode hash1 = testee.hash(contact1);
		HashCode hash2 = testee.hash(contact2);
		
		assertThat(hash1).isNotEqualTo(hash2);
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentLastname() {
		MSContact contact1 = new MSContact();
		contact1.setLastName("lastname");

		MSContact contact2 = new MSContact();
		contact2.setLastName("other lastname");
		
		HashCode hash1 = testee.hash(contact1);
		HashCode hash2 = testee.hash(contact2);
		
		assertThat(hash1).isNotEqualTo(hash2);
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentMiddlename() {
		MSContact contact1 = new MSContact();
		contact1.setMiddleName("middlename");
		
		MSContact contact2 = new MSContact();
		contact1.setMiddleName("other middlename");
		
		HashCode hash1 = testee.hash(contact1);
		HashCode hash2 = testee.hash(contact2);
		
		assertThat(hash1).isNotEqualTo(hash2);
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentFileAs() {
		MSContact contact1 = new MSContact();
		contact1.setFileAs("value");
		
		MSContact contact2 = new MSContact();
		contact1.setFileAs("other value");
		
		HashCode hash1 = testee.hash(contact1);
		HashCode hash2 = testee.hash(contact2);
		
		assertThat(hash1).isNotEqualTo(hash2);
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentEmail1() {
		MSContact contact1 = new MSContact();
		contact1.setEmail1Address("value");
		
		MSContact contact2 = new MSContact();
		contact1.setEmail1Address("other value");
		
		HashCode hash1 = testee.hash(contact1);
		HashCode hash2 = testee.hash(contact2);
		
		assertThat(hash1).isNotEqualTo(hash2);
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentEmail2() {
		MSContact contact1 = new MSContact();
		contact1.setEmail2Address("value");
		
		MSContact contact2 = new MSContact();
		contact1.setEmail2Address("other value");
		
		HashCode hash1 = testee.hash(contact1);
		HashCode hash2 = testee.hash(contact2);
		
		assertThat(hash1).isNotEqualTo(hash2);
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentMobilePhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setMobilePhoneNumber("value");
		
		MSContact contact2 = new MSContact();
		contact1.setMobilePhoneNumber("other value");
		
		HashCode hash1 = testee.hash(contact1);
		HashCode hash2 = testee.hash(contact2);
		
		assertThat(hash1).isNotEqualTo(hash2);
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentBusinessPhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setBusinessPhoneNumber("value");
		
		MSContact contact2 = new MSContact();
		contact1.setBusinessPhoneNumber("other value");
		
		HashCode hash1 = testee.hash(contact1);
		HashCode hash2 = testee.hash(contact2);
		
		assertThat(hash1).isNotEqualTo(hash2);
	}

	@Test
	public void hashShouldAcceptNullFirstName() {
		MSContact contact = new MSContact();
		contact.setFirstName(null);
		contact.setLastName("lastname");
		contact.setEmail1Address("contact@mydomain.org");
		contact.setFileAs("lastname, firstname");
		
		assertThat(testee.hash(contact)).isNotNull();
	}
	
	@Test
	public void hashShouldAcceptNullLastName() {
		MSContact contact = new MSContact();
		contact.setFirstName("firstname");
		contact.setLastName(null);
		contact.setEmail1Address("contact@mydomain.org");
		contact.setFileAs("lastname, firstname");
		
		assertThat(testee.hash(contact)).isNotNull();
	}
	
	@Test
	public void hashShouldAcceptNullEmail() {
		MSContact contact = new MSContact();
		contact.setFirstName("firstname");
		contact.setLastName("lastname");
		contact.setEmail1Address(null);
		contact.setFileAs("lastname, firstname");
		
		assertThat(testee.hash(contact)).isNotNull();
	}
	
	@Test
	public void hashShouldAcceptAllNullFieldsAndEquals() {
		MSContact contact = new MSContact();
		contact.setFirstName(null);
		contact.setLastName(null);
		contact.setMiddleName(null);
		contact.setEmail1Address(null);
		contact.setEmail2Address(null);
		contact.setMobilePhoneNumber(null);
		contact.setBusinessPhoneNumber(null);

		MSContact contact2 = new MSContact();
		contact2.setFirstName(null);
		contact2.setLastName(null);
		contact2.setMiddleName(null);
		contact2.setEmail1Address(null);
		contact2.setEmail2Address(null);
		contact2.setMobilePhoneNumber(null);
		contact2.setBusinessPhoneNumber(null);
		
		assertThat(testee.hash(contact)).isEqualTo(testee.hash(contact2));
	}
}
