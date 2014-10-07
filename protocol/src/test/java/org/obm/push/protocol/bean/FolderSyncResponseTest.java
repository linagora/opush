/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
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
package org.obm.push.protocol.bean;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.bean.FolderSyncStatus;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.Summary;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;

import com.google.common.collect.ImmutableList;

public class FolderSyncResponseTest {

	@Test(expected=NullPointerException.class)
	public void buildNeedStatus() {
		FolderSyncResponse.builder()
			.newSyncKey(new SyncKey("123"))
			.hierarchyItemsChanges(HierarchyCollectionChanges.empty())
			.build();
	}
	
	@Test(expected=NullPointerException.class)
	public void buildNeedChanges() {
		FolderSyncResponse.builder()
			.newSyncKey(new SyncKey("123"))
			.status(FolderSyncStatus.OK)
			.build();
	}

	@Test
	public void testWhenEmpty() {
		FolderSyncResponse response = FolderSyncResponse.builder()
			.status(FolderSyncStatus.OK)
			.hierarchyItemsChanges(HierarchyCollectionChanges.empty())
			.newSyncKey(new SyncKey("123"))
			.build();
		assertThat(response.getCount()).isEqualTo(0);
		assertThat(response.getSummary()).isEqualTo(Summary.empty());
	}
	
	@Test
	public void getCountWhenOneAddOnly() {
		CollectionChange add = CollectionChange.builder()
			.collectionId(CollectionId.of(1))
			.parentCollectionId(CollectionId.ROOT)
			.displayName("INBOX")
			.folderType(FolderType.DEFAULT_INBOX_FOLDER)
			.isNew(true)
			.build();
		
		FolderSyncResponse response = FolderSyncResponse.builder()
				.status(FolderSyncStatus.OK)
				.hierarchyItemsChanges(HierarchyCollectionChanges.builder()
						.changes(ImmutableList.of(add))
						.build())
				.newSyncKey(new SyncKey("123"))
				.build();
		
		assertThat(response.getCount()).isEqualTo(1);
		assertThat(response.getSummary()).isEqualTo(
				Summary.builder().changeCount(1).build());
	}
	
	@Test
	public void getCountWhenOneChangeOnly() {
		CollectionChange change = CollectionChange.builder()
			.collectionId(CollectionId.of(1))
			.parentCollectionId(CollectionId.ROOT)
			.displayName("INBOX")
			.folderType(FolderType.DEFAULT_INBOX_FOLDER)
			.isNew(false)
			.build();
		
		FolderSyncResponse response = FolderSyncResponse.builder()
				.status(FolderSyncStatus.OK)
				.hierarchyItemsChanges(HierarchyCollectionChanges.builder()
						.changes(ImmutableList.of(change))
						.build())
				.newSyncKey(new SyncKey("123"))
				.build();
		assertThat(response.getCount()).isEqualTo(1);
		assertThat(response.getSummary()).isEqualTo(
				Summary.builder().changeCount(1).build());
	}
	
	@Test
	public void getCountWhenOneDeletionOnly() {
		CollectionDeletion del = CollectionDeletion.builder()
			.collectionId(CollectionId.of(1))
			.build();
		
		FolderSyncResponse response = FolderSyncResponse.builder()
				.status(FolderSyncStatus.OK)
				.hierarchyItemsChanges(HierarchyCollectionChanges.builder()
						.deletions(ImmutableList.of(del))
						.build())
				.newSyncKey(new SyncKey("123"))
				.build();
		
		assertThat(response.getCount()).isEqualTo(1);
		assertThat(response.getSummary()).isEqualTo(
				Summary.builder().deletionCount(1).build());
	}
	
	@Test
	public void getCountWhenOneOfEach() {
		CollectionChange add = CollectionChange.builder()
			.collectionId(CollectionId.of(1))
			.parentCollectionId(CollectionId.ROOT)
			.displayName("INBOX")
			.folderType(FolderType.DEFAULT_INBOX_FOLDER)
			.isNew(true)
			.build();
		CollectionChange change = CollectionChange.builder()
			.collectionId(CollectionId.of(3))
			.parentCollectionId(CollectionId.ROOT)
			.displayName("FOLDER")
			.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
			.isNew(false)
			.build();
		CollectionDeletion del = CollectionDeletion.builder()
			.collectionId(CollectionId.of(8))
			.build();
		
		FolderSyncResponse response = FolderSyncResponse.builder()
				.status(FolderSyncStatus.OK)
				.hierarchyItemsChanges(HierarchyCollectionChanges.builder()
						.changes(ImmutableList.of(add, change))
						.deletions(ImmutableList.of(del))
						.build())
				.newSyncKey(new SyncKey("123"))
				.build();
		
		assertThat(response.getCount()).isEqualTo(3);
		assertThat(response.getSummary()).isEqualTo(
				Summary.builder().changeCount(2).deletionCount(1).build());
	}
}
