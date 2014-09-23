/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2014  Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version, provided you comply with the Additional Terms applicable for OBM
 * software by Linagora pursuant to Section 7 of the GNU Affero General Public
 * License, subsections (b), (c), and (e), pursuant to which you must notably (i)
 * retain the displaying by the interactive user interfaces of the “OBM, Free
 * Communication by Linagora” Logo with the “You are using the Open Source and
 * free version of OBM developed and supported by Linagora. Contribute to OBM R&D
 * by subscribing to an Enterprise offer !” infobox, (ii) retain all hypertext
 * links between OBM and obm.org, between Linagora and linagora.com, as well as
 * between the expression “Enterprise offer” and pro.obm.org, and (iii) refrain
 * from infringing Linagora intellectual property rights over its trademarks and
 * commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License and
 * its applicable Additional Terms for OBM along with this program. If not, see
 * <http://www.gnu.org/licenses/> for the GNU Affero General   Public License
 * version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
 * applicable to the OBM software.
 * ***** END LICENSE BLOCK ***** */

package org.obm.push.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.obm.DateUtils.dateUTC;

import java.util.Date;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.WindowingContact;
import org.obm.push.backend.WindowingContactChanges;
import org.obm.push.bean.MSContact;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.service.DateService;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.WindowingDao;

import com.google.inject.Provider;

public class ObmSyncBackendTest {

	private IMocksControl mocks;
	private MappingService mappingService;
	private ObmSyncBackend<WindowingContact> testee;
	
	@Before
	public void setUp() {
		mocks = createControl();
		
		mappingService = mocks.createMock(MappingService.class);
		testee = createMockBuilder(ObmSyncBackend.class)
				.withConstructor(mappingService, 
						mocks.createMock(Provider.class), 
						mocks.createMock(WindowingDao.class),
						mocks.createMock(DateService.class))
				.createMock();
	}
	
	@Test
	public void convertToDataDeltaWhenEmpty() {
		WindowingContactChanges changes = WindowingContactChanges.empty();
		int collectionId = 12;
		Date syncDate = dateUTC("2013-04-07T12:09:37");
		SyncKey syncKey = new SyncKey("88dd7c4d-8b9a-4917-8e9d-8b8d3440932e");
		
		DataDelta result = testee.builderWithChangesAndDeletions(changes, collectionId)
				.syncDate(syncDate)
				.syncKey(syncKey)
				.build();
		
		assertThat(result.getChanges()).isEmpty();
		assertThat(result.getDeletions()).isEmpty();
		assertThat(result.getSyncDate()).isEqualTo(syncDate);
		assertThat(result.getSyncKey()).isEqualTo(syncKey);
	}
	
	@Test
	public void convertToDataDeltaWhenTwoAdds() {
		MSContact contact1 = new MSContact();
		MSContact contact2 = new MSContact();
		WindowingContactChanges changes = WindowingContactChanges.builder()
				.addition(WindowingContact.builder().uid(14).applicationData(contact1).build())
				.addition(WindowingContact.builder().uid(16).applicationData(contact2).build())
				.build();
		int collectionId = 12;
		Date syncDate = dateUTC("2013-04-07T12:09:37");
		SyncKey syncKey = new SyncKey("88dd7c4d-8b9a-4917-8e9d-8b8d3440932e");
		
		expect(mappingService.getServerIdFor(collectionId, "14")).andReturn("12:14");
		expect(mappingService.getServerIdFor(collectionId, "16")).andReturn("12:16");
		
		mocks.replay();
		DataDelta result = testee.builderWithChangesAndDeletions(changes, collectionId)
				.syncDate(syncDate)
				.syncKey(syncKey)
				.build();
		mocks.verify();
		
		assertThat(result.getChanges()).containsOnly(
				ItemChange.builder().serverId("12:14").data(contact1).build(),
				ItemChange.builder().serverId("12:16").data(contact2).build());
		assertThat(result.getDeletions()).isEmpty();
	}

	@Test
	public void convertToDataDeltaWhenTwoChanges() {
		MSContact contact1 = new MSContact();
		MSContact contact2 = new MSContact();
		WindowingContactChanges changes = WindowingContactChanges.builder()
				.change(WindowingContact.builder().uid(14).applicationData(contact1).build())
				.change(WindowingContact.builder().uid(16).applicationData(contact2).build())
				.build();
		int collectionId = 12;
		Date syncDate = dateUTC("2013-04-07T12:09:37");
		SyncKey syncKey = new SyncKey("88dd7c4d-8b9a-4917-8e9d-8b8d3440932e");

		expect(mappingService.getServerIdFor(collectionId, "14")).andReturn("12:14");
		expect(mappingService.getServerIdFor(collectionId, "16")).andReturn("12:16");
		
		mocks.replay();
		DataDelta result = testee.builderWithChangesAndDeletions(changes, collectionId)
				.syncDate(syncDate)
				.syncKey(syncKey)
				.build();
		mocks.verify();
		
		assertThat(result.getChanges()).containsOnly(
				ItemChange.builder().serverId("12:14").data(contact1).build(),
				ItemChange.builder().serverId("12:16").data(contact2).build());
		assertThat(result.getDeletions()).isEmpty();
	}
	
	@Test
	public void convertToDataDeltaWhenTwoDeletions() {
		MSContact contact1 = new MSContact();
		MSContact contact2 = new MSContact();
		WindowingContactChanges changes = WindowingContactChanges.builder()
				.deletion(WindowingContact.builder().uid(14).applicationData(contact1).build())
				.deletion(WindowingContact.builder().uid(16).applicationData(contact2).build())
				.build();
		int collectionId = 12;
		Date syncDate = dateUTC("2013-04-07T12:09:37");
		SyncKey syncKey = new SyncKey("88dd7c4d-8b9a-4917-8e9d-8b8d3440932e");

		expect(mappingService.getServerIdFor(collectionId, "14")).andReturn("12:14");
		expect(mappingService.getServerIdFor(collectionId, "16")).andReturn("12:16");
		
		mocks.replay();
		DataDelta result = testee.builderWithChangesAndDeletions(changes, collectionId)
				.syncDate(syncDate)
				.syncKey(syncKey)
				.build();
		mocks.verify();
		
		assertThat(result.getDeletions()).containsOnly(
				ItemDeletion.builder().serverId("12:14").build(),
				ItemDeletion.builder().serverId("12:16").build());
		assertThat(result.getChanges()).isEmpty();
	}
	
	@Test
	public void convertToDataDeltaWhenOneOfEach() {
		MSContact contact1 = new MSContact();
		MSContact contact2 = new MSContact();
		MSContact contact3 = new MSContact();
		WindowingContactChanges changes = WindowingContactChanges.builder()
				.addition(WindowingContact.builder().uid(14).applicationData(contact1).build())
				.change(WindowingContact.builder().uid(15).applicationData(contact2).build())
				.deletion(WindowingContact.builder().uid(16).applicationData(contact3).build())
				.build();
		int collectionId = 12;
		Date syncDate = dateUTC("2013-04-07T12:09:37");
		SyncKey syncKey = new SyncKey("88dd7c4d-8b9a-4917-8e9d-8b8d3440932e");

		expect(mappingService.getServerIdFor(collectionId, "14")).andReturn("12:14");
		expect(mappingService.getServerIdFor(collectionId, "15")).andReturn("12:15");
		expect(mappingService.getServerIdFor(collectionId, "16")).andReturn("12:16");
		
		mocks.replay();
		DataDelta result = testee.builderWithChangesAndDeletions(changes, collectionId)
				.syncDate(syncDate)
				.syncKey(syncKey)
				.build();
		mocks.verify();
		
		assertThat(result.getDeletions()).containsOnly(ItemDeletion.builder().serverId("12:16").build());
		assertThat(result.getChanges()).containsOnly(
				ItemChange.builder().serverId("12:14").data(contact1).build(),
				ItemChange.builder().serverId("12:15").data(contact2).build());
	}
}
