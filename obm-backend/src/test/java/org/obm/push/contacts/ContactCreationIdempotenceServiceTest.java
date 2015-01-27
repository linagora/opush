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
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentLastname() {
		MSContact contact1 = new MSContact();
		contact1.setLastName("lastname");

		MSContact contact2 = new MSContact();
		contact2.setLastName("other lastname");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentMiddlename() {
		MSContact contact1 = new MSContact();
		contact1.setMiddleName("middlename");
		
		MSContact contact2 = new MSContact();
		contact1.setMiddleName("other middlename");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentFileAs() {
		MSContact contact1 = new MSContact();
		contact1.setFileAs("value");
		
		MSContact contact2 = new MSContact();
		contact1.setFileAs("other value");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentEmail1() {
		MSContact contact1 = new MSContact();
		contact1.setEmail1Address("value");
		
		MSContact contact2 = new MSContact();
		contact1.setEmail1Address("other value");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentEmail2() {
		MSContact contact1 = new MSContact();
		contact1.setEmail2Address("value");
		
		MSContact contact2 = new MSContact();
		contact1.setEmail2Address("other value");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentMobilePhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setMobilePhoneNumber("value");
		
		MSContact contact2 = new MSContact();
		contact1.setMobilePhoneNumber("other value");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentBusinessPhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setBusinessPhoneNumber("value");
		
		MSContact contact2 = new MSContact();
		contact1.setBusinessPhoneNumber("other value");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
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
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentCompany() {
		MSContact contact1 = new MSContact();
		contact1.setCompanyName("value");
		
		MSContact contact2 = new MSContact();
		contact1.setCompanyName("other value");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentEmail3Address() {
		MSContact contact1 = new MSContact();
		contact1.setEmail3Address("value");
		
		MSContact contact2 = new MSContact();
		contact1.setEmail3Address("other value");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}
	
	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentAssistantName() {
		MSContact contact1 = new MSContact();
		contact1.setAssistantName("value");
		
		MSContact contact2 = new MSContact();
		contact1.setAssistantName("other value");
		
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentAssistantPhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setAssistantPhoneNumber("value");

		MSContact contact2 = new MSContact();
		contact1.setAssistantPhoneNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentAssistnamePhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setAssistnamePhoneNumber("value");

		MSContact contact2 = new MSContact();
		contact1.setAssistnamePhoneNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentBusiness2PhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setBusiness2PhoneNumber("value");

		MSContact contact2 = new MSContact();
		contact1.setBusiness2PhoneNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentBusinessAddressCity() {
		MSContact contact1 = new MSContact();
		contact1.setBusinessAddressCity("value");

		MSContact contact2 = new MSContact();
		contact1.setBusinessAddressCity("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentWebPage() {
		MSContact contact1 = new MSContact();
		contact1.setWebPage("value");

		MSContact contact2 = new MSContact();
		contact1.setWebPage("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentBusinessAddressCountry() {
		MSContact contact1 = new MSContact();
		contact1.setBusinessAddressCountry("value");

		MSContact contact2 = new MSContact();
		contact1.setBusinessAddressCountry("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentDepartment() {
		MSContact contact1 = new MSContact();
		contact1.setDepartment("value");

		MSContact contact2 = new MSContact();
		contact1.setDepartment("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentEmail1Address() {
		MSContact contact1 = new MSContact();
		contact1.setEmail1Address("value");

		MSContact contact2 = new MSContact();
		contact1.setEmail1Address("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentEmail2Address() {
		MSContact contact1 = new MSContact();
		contact1.setEmail2Address("value");

		MSContact contact2 = new MSContact();
		contact1.setEmail2Address("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentBusinessFaxNumber() {
		MSContact contact1 = new MSContact();
		contact1.setBusinessFaxNumber("value");

		MSContact contact2 = new MSContact();
		contact1.setBusinessFaxNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentFirstName() {
		MSContact contact1 = new MSContact();
		contact1.setFirstName("value");

		MSContact contact2 = new MSContact();
		contact1.setFirstName("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentMiddleName() {
		MSContact contact1 = new MSContact();
		contact1.setMiddleName("value");

		MSContact contact2 = new MSContact();
		contact1.setMiddleName("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentHomeAddressCity() {
		MSContact contact1 = new MSContact();
		contact1.setHomeAddressCity("value");

		MSContact contact2 = new MSContact();
		contact1.setHomeAddressCity("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentHomeAddressCountry() {
		MSContact contact1 = new MSContact();
		contact1.setHomeAddressCountry("value");

		MSContact contact2 = new MSContact();
		contact1.setHomeAddressCountry("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentHomeFaxNumber() {
		MSContact contact1 = new MSContact();
		contact1.setHomeFaxNumber("value");
		MSContact contact2 = new MSContact();
		contact1.setHomeFaxNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentHomePhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setHomePhoneNumber("value");

		MSContact contact2 = new MSContact();
		contact1.setHomePhoneNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentHome2PhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setHome2PhoneNumber("value");

		MSContact contact2 = new MSContact();
		contact1.setHome2PhoneNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentHomeAddressPostalCode() {
		MSContact contact1 = new MSContact();
		contact1.setHomeAddressPostalCode("value");

		MSContact contact2 = new MSContact();
		contact1.setHomeAddressPostalCode("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentHomeAddressState() {
		MSContact contact1 = new MSContact();
		contact1.setHomeAddressState("value");

		MSContact contact2 = new MSContact();
		contact1.setHomeAddressState("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentHomeAddressStreet() {
		MSContact contact1 = new MSContact();
		contact1.setHomeAddressStreet("value");

		MSContact contact2 = new MSContact();
		contact1.setHomeAddressStreet("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentSuffix() {
		MSContact contact1 = new MSContact();
		contact1.setSuffix("value");

		MSContact contact2 = new MSContact();
		contact1.setSuffix("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentCompanyName() {
		MSContact contact1 = new MSContact();
		contact1.setCompanyName("value");

		MSContact contact2 = new MSContact();
		contact1.setCompanyName("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentOtherAddressCity() {
		MSContact contact1 = new MSContact();
		contact1.setOtherAddressCity("value");

		MSContact contact2 = new MSContact();
		contact1.setOtherAddressCity("other value");
		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentOtherAddressCountry() {
		MSContact contact1 = new MSContact();
		contact1.setOtherAddressCountry("value");

		MSContact contact2 = new MSContact();
		contact1.setOtherAddressCountry("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentCarPhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setCarPhoneNumber("value");

		MSContact contact2 = new MSContact();
		contact1.setCarPhoneNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentOtherAddressPostalCode() {
		MSContact contact1 = new MSContact();
		contact1.setOtherAddressPostalCode("value");

		MSContact contact2 = new MSContact();
		contact1.setOtherAddressPostalCode("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentOtherAddressState() {
		MSContact contact1 = new MSContact();
		contact1.setOtherAddressState("value");

		MSContact contact2 = new MSContact();
		contact1.setOtherAddressState("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentOtherAddressStreet() {
		MSContact contact1 = new MSContact();
		contact1.setOtherAddressStreet("value");

		MSContact contact2 = new MSContact();
		contact1.setOtherAddressStreet("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentPagerNumber() {
		MSContact contact1 = new MSContact();
		contact1.setPagerNumber("value");

		MSContact contact2 = new MSContact();
		contact1.setPagerNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentTitle() {
		MSContact contact1 = new MSContact();
		contact1.setTitle("value");

		MSContact contact2 = new MSContact();
		contact1.setTitle("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentBusinessPostalCode() {
		MSContact contact1 = new MSContact();
		contact1.setBusinessPostalCode("value");

		MSContact contact2 = new MSContact();
		contact1.setBusinessPostalCode("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentLastName() {
		MSContact contact1 = new MSContact();
		contact1.setLastName("value");

		MSContact contact2 = new MSContact();
		contact1.setLastName("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentSpouse() {
		MSContact contact1 = new MSContact();
		contact1.setSpouse("value");

		MSContact contact2 = new MSContact();
		contact1.setSpouse("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentBusinessState() {
		MSContact contact1 = new MSContact();
		contact1.setBusinessState("value");

		MSContact contact2 = new MSContact();
		contact1.setBusinessState("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentBusinessStreet() {
		MSContact contact1 = new MSContact();
		contact1.setBusinessStreet("value");

		MSContact contact2 = new MSContact();
		contact1.setBusinessStreet("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentJobTitle() {
		MSContact contact1 = new MSContact();
		contact1.setJobTitle("value");

		MSContact contact2 = new MSContact();
		contact1.setJobTitle("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentOfficeLocation() {
		MSContact contact1 = new MSContact();
		contact1.setOfficeLocation("value");

		MSContact contact2 = new MSContact();
		contact1.setOfficeLocation("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveDifferentResultWhenCalledWithDifferentRadioPhoneNumber() {
		MSContact contact1 = new MSContact();
		contact1.setRadioPhoneNumber("value");

		MSContact contact2 = new MSContact();
		contact1.setRadioPhoneNumber("other value");

		assertThat(testee.hash(contact1)).isNotEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveSameResultWhenCalledWithDifferentYomiFirstName() {
		MSContact contact1 = new MSContact();
		contact1.setYomiFirstName("value");

		MSContact contact2 = new MSContact();
		contact1.setYomiFirstName("other value");

		assertThat(testee.hash(contact1)).isEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveSameResultWhenCalledWithDifferentYomiLastName() {
		MSContact contact1 = new MSContact();
		contact1.setYomiLastName("value");

		MSContact contact2 = new MSContact();
		contact1.setYomiLastName("other value");

		assertThat(testee.hash(contact1)).isEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveSameResultWhenCalledWithDifferentYomiCompanyName() {
		MSContact contact1 = new MSContact();
		contact1.setYomiCompanyName("value");

		MSContact contact2 = new MSContact();
		contact1.setYomiCompanyName("other value");

		assertThat(testee.hash(contact1)).isEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveSameResultWhenCalledWithDifferentPicture() {
		MSContact contact1 = new MSContact();
		contact1.setPicture("value");

		MSContact contact2 = new MSContact();
		contact1.setPicture("other value");

		assertThat(testee.hash(contact1)).isEqualTo(testee.hash(contact2));
	}

	@Test
	public void hashShouldHaveSameResultWhenCalledWithDifferentData() {
		MSContact contact1 = new MSContact();
		contact1.setData("value");

		MSContact contact2 = new MSContact();
		contact1.setData("other value");

		assertThat(testee.hash(contact1)).isEqualTo(testee.hash(contact2));
	}

}
