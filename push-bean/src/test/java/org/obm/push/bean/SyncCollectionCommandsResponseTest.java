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
package org.obm.push.bean;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.bean.change.client.SyncClientCommands;
import org.obm.push.bean.change.client.SyncClientCommands.Add;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;

import com.google.common.collect.ImmutableList;


public class SyncCollectionCommandsResponseTest {

	@Test
	public void testResponseSummaryNoEntry() {
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder().build();
		assertThat(commands.getSummary()).isEqualTo(Summary.empty());
	}
	
	@Test
	public void testResponseSummaryOneChange() {
		ImmutableList<ItemChange> changes = ImmutableList.of(ItemChange.builder().serverId("123").build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.changes(changes, SyncClientCommands.empty())
				.build();
		assertThat(commands.getSummary()).isEqualTo(Summary.builder().changeCount(1).build());
	}

	@Test
	public void testResponseSummaryOneAddition() {
		ImmutableList<ItemChange> changes = ImmutableList.of(ItemChange.builder().serverId("123").isNew(true).build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.changes(changes, SyncClientCommands.empty())
				.build();
		assertThat(commands.getSummary()).isEqualTo(Summary.builder().changeCount(1).build());
	}

	@Test
	public void testResponseSummaryOneAdditionFromClient() {
		ImmutableList<ItemChange> changes = ImmutableList.of(ItemChange.builder().serverId("123").isNew(true).build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.changes(changes, SyncClientCommands.builder().putAdd(new Add("789456", "123")).build())
				.build();
		assertThat(commands.getSummary()).isEqualTo(Summary.builder().changeCount(1).build());
	}
	
	@Test
	public void testResponseSummaryOneFetch() {
		ImmutableList<ItemChange> fetchs = ImmutableList.of(ItemChange.builder().serverId("123").build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.fetchs(fetchs)
				.build();
		assertThat(commands.getSummary()).isEqualTo(Summary.builder().fetchCount(1).build());
	}
	
	@Test
	public void testResponseSummaryOneDeletion() {
		ImmutableList<ItemDeletion> deletions = ImmutableList.of(ItemDeletion.builder().serverId("234").build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.deletions(deletions)
				.build();
		assertThat(commands.getSummary()).isEqualTo(Summary.builder().deletionCount(1).build());
	}

	@Test
	public void testResponseSummaryManyEntries() {
		ImmutableList<ItemChange> changes = ImmutableList.of(
				ItemChange.builder().serverId("123").build(),
				ItemChange.builder().serverId("543").isNew(true).build(),
				ItemChange.builder().serverId("2343").build(),
				ItemChange.builder().serverId("4566").isNew(true).build(),
				ItemChange.builder().serverId("1322").isNew(true).build(),
				ItemChange.builder().serverId("54123").build(),
				ItemChange.builder().serverId("54653").isNew(true).build(),
				ItemChange.builder().serverId("12883").build(),
				ItemChange.builder().serverId("797").build(),
				ItemChange.builder().serverId("543243").isNew(true).build());
		ImmutableList<ItemDeletion> deletions = ImmutableList.of(ItemDeletion.builder().serverId("234").build());
		ImmutableList<ItemChange> fetchs = ImmutableList.of(ItemChange.builder().serverId("14523").build(),
				ItemChange.builder().serverId("56756").build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.changes(changes, SyncClientCommands.empty())
				.fetchs(fetchs)
				.deletions(deletions)
				.build();
		assertThat(commands.getSummary()).isEqualTo(Summary.builder().changeCount(10).deletionCount(1).fetchCount(2).build());
	}
}
