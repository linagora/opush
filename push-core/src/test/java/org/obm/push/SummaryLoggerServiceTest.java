/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014  Linagora
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

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.FolderSyncStatus;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.Sync;
import org.obm.push.bean.SyncCollectionCommandsResponse;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.change.client.SyncClientCommands;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.protocol.bean.FolderSyncResponse;
import org.obm.push.protocol.bean.SyncResponse;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

public class SummaryLoggerServiceTest {

	private IMocksControl mocks;
	private Logger loggerIn;
	private Logger loggerOut;
	
	private SummaryLoggerService testee;

	@Before
	public void setUp() {
		mocks = createControl();
		loggerIn = mocks.createMock(Logger.class);
		loggerOut = mocks.createMock(Logger.class);
		
		testee = new SummaryLoggerService(loggerIn, loggerOut);
	}

	
	@Test
	public void syncLoggerInWhenInfoNotEnabled() {
		Sync sync = Sync.builder()
			.addCollection(AnalysedSyncCollection.builder()
				.collectionId(1)
				.syncKey(new SyncKey("123"))
				.dataType(PIMDataType.EMAIL)
				.build())
			.build();
		
		expect(loggerIn.isInfoEnabled()).andReturn(false);
		mocks.replay();
		testee.logIncomingSync(sync);
		mocks.verify();
	}

	@Test
	public void syncLoggerInWhenNoCollection() {
		Sync sync = Sync.builder().build();
		
		expect(loggerIn.isInfoEnabled()).andReturn(true);
		loggerIn.info("CHANGE: 0, DELETE: 0, FETCH: 0");
		expectLastCall();
		mocks.replay();
		testee.logIncomingSync(sync);
		mocks.verify();
	}

	@Test
	public void syncLoggerInWhenMergeOfCollection() {
		Sync sync = Sync.builder()
			.addCollection(AnalysedSyncCollection.builder()
				.collectionId(1)
				.syncKey(new SyncKey("123"))
				.dataType(PIMDataType.EMAIL)
				.commands(SyncCollectionCommandsResponse.builder()
					.fetchs(ImmutableList.of(ItemChange.builder().serverId("321").build()))
					.build())
				.build())
			.addCollection(AnalysedSyncCollection.builder()
				.collectionId(3)
				.syncKey(new SyncKey("456"))
				.dataType(PIMDataType.CALENDAR)
				.commands(SyncCollectionCommandsResponse.builder()
					.changes(ImmutableList.of(ItemChange.builder().serverId("987").build()), SyncClientCommands.empty())
					.build())
				.build())
			.build();
		
		expect(loggerIn.isInfoEnabled()).andReturn(true);
		loggerIn.info("CHANGE: 1, DELETE: 0, FETCH: 1");
		expectLastCall();
		mocks.replay();
		testee.logIncomingSync(sync);
		mocks.verify();
	}
	
	@Test
	public void syncLoggerOutWhenInfoNotEnabled() {
		SyncResponse response = SyncResponse.builder()
			.status(SyncStatus.OK)
			.addResponse(SyncCollectionResponse.builder()
				.collectionId(1)
				.syncKey(new SyncKey("123"))
				.dataType(PIMDataType.EMAIL)
				.responses(SyncCollectionCommandsResponse.builder()
						.changes(ImmutableList.of(ItemChange.builder().serverId("987").build()), SyncClientCommands.empty())
						.build())
				.build())
			.build();
		
		expect(loggerOut.isInfoEnabled()).andReturn(false);
		mocks.replay();
		testee.logOutgoingSync(response);
		mocks.verify();
	}

	@Test
	public void syncLoggerOutWhenNoCollection() {
		SyncResponse response = SyncResponse.builder()
			.status(SyncStatus.OK)
			.build();
		
		expect(loggerOut.isInfoEnabled()).andReturn(true);
		loggerOut.info("CHANGE: 0, DELETE: 0, FETCH: 0");
		expectLastCall();
		mocks.replay();
		testee.logOutgoingSync(response);
		mocks.verify();
	}
	
	@Test
	public void syncLoggerOutWhenNoResponse() {
		SyncResponse response = SyncResponse.builder()
			.status(SyncStatus.OK)
			.addResponse(SyncCollectionResponse.builder()
				.collectionId(1)
				.syncKey(new SyncKey("123"))
				.dataType(PIMDataType.EMAIL)
				.build())
			.build();
		
		expect(loggerOut.isInfoEnabled()).andReturn(true);
		loggerOut.info("CHANGE: 0, DELETE: 0, FETCH: 0");
		expectLastCall();
		mocks.replay();
		testee.logOutgoingSync(response);
		mocks.verify();
	}
	
	@Test
	public void syncLoggerOutWhenNoCommandsInResponses() {
		SyncResponse response = SyncResponse.builder()
			.status(SyncStatus.OK)
			.addResponse(SyncCollectionResponse.builder()
				.collectionId(1)
				.syncKey(new SyncKey("123"))
				.dataType(PIMDataType.EMAIL)
				.responses(SyncCollectionCommandsResponse.builder().build())
				.build())
			.build();
		
		expect(loggerOut.isInfoEnabled()).andReturn(true);
		loggerOut.info("CHANGE: 0, DELETE: 0, FETCH: 0");
		expectLastCall();
		mocks.replay();
		testee.logOutgoingSync(response);
		mocks.verify();
	}

	@Test
	public void syncLoggerOutWhenMergeOfCollection() {
		SyncResponse response = SyncResponse.builder()
			.status(SyncStatus.OK)
			.addResponse(SyncCollectionResponse.builder()
				.collectionId(1)
				.syncKey(new SyncKey("123"))
				.dataType(PIMDataType.EMAIL)
				.responses( SyncCollectionCommandsResponse.builder()
					.fetchs(ImmutableList.of(ItemChange.builder().serverId("103").build()))
					.deletions(ImmutableList.of(ItemDeletion.builder().serverId("234").build()))
					.build())
				.build())
			.addResponse(SyncCollectionResponse.builder()
				.collectionId(1)
				.syncKey(new SyncKey("123"))
				.dataType(PIMDataType.EMAIL)
				.responses( SyncCollectionCommandsResponse.builder()
					.changes(
						ImmutableList.of(ItemChange.builder().serverId("456").build()),
						SyncClientCommands.empty())
					.deletions(ImmutableList.of(ItemDeletion.builder().serverId("789").build()))
					.build())
				.build())
			.build();
		
		expect(loggerOut.isInfoEnabled()).andReturn(true);
		loggerOut.info("CHANGE: 1, DELETE: 2, FETCH: 1");
		expectLastCall();
		mocks.replay();
		testee.logOutgoingSync(response);
		mocks.verify();
	}
	
	@Test
	public void folderSyncloggerOutWhenEmpty() {
		FolderSyncResponse response = FolderSyncResponse.builder()
			.status(FolderSyncStatus.OK)
			.hierarchyItemsChanges(HierarchyCollectionChanges.empty())
			.newSyncKey(new SyncKey("123"))
			.build();

		loggerOut.info("CHANGE: 0, DELETE: 0, FETCH: 0");
		expectLastCall();
		mocks.replay();
		testee.logOutgoingFolderSync(response);
		mocks.verify();
	}
	
	@Test
	public void folderSyncloggerOutWhenOneOfEach() {
		CollectionChange add = CollectionChange.builder()
			.collectionId("1")
			.parentCollectionId("0")
			.displayName("INBOX")
			.folderType(FolderType.DEFAULT_INBOX_FOLDER)
			.isNew(true)
			.build();
		CollectionChange change = CollectionChange.builder()
			.collectionId("3")
			.parentCollectionId("0")
			.displayName("FOLDER")
			.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
			.isNew(false)
			.build();
		CollectionDeletion del = CollectionDeletion.builder()
			.collectionId("8")
			.build();
		
		FolderSyncResponse response = FolderSyncResponse.builder()
			.status(FolderSyncStatus.OK)
			.hierarchyItemsChanges(HierarchyCollectionChanges.builder()
				.changes(ImmutableList.of(add, change))
				.deletions(ImmutableList.of(del))
				.build())
			.newSyncKey(new SyncKey("123"))
			.build();

		loggerOut.info("CHANGE: 2, DELETE: 1, FETCH: 0");
		expectLastCall();
		mocks.replay();
		testee.logOutgoingFolderSync(response);
		mocks.verify();
	}
}
