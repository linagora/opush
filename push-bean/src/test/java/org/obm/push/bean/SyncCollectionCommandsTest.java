/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
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
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.collect.ImmutableList;


public class SyncCollectionCommandsTest {

	@Test
	public void testBuilderCommandsIsNotRequired() {
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
			.build();
		
		assertThat(commands.getCommands()).isEmpty();
	}

	@Test
	public void testBuilderCommandsValid() {
		ServerId serverIdDel = CollectionId.of(1).serverId(23);
		ServerId serverIdFetch = CollectionId.of(1).serverId(33);
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
			.addCommand(SyncCollectionCommand.builder().type(SyncCommand.DELETE).serverId(serverIdDel).build())
			.addCommand(SyncCollectionCommand.builder().type(SyncCommand.FETCH).serverId(serverIdFetch).build())
			.build();
		
		assertThat(commands.getCommands()).containsOnly(
				SyncCollectionCommand.builder().type(SyncCommand.DELETE).serverId(serverIdDel).build(),
				SyncCollectionCommand.builder().type(SyncCommand.FETCH).serverId(serverIdFetch).build());
	}
	
	@Test
	public void testChangesAndDeletions() {
		ServerId serverId = CollectionId.of(1).serverId(23);
		ServerId serverIdDel = CollectionId.of(1).serverId(23);
		ImmutableList<ItemChange> changes = ImmutableList.<ItemChange> of(ItemChange.builder().serverId(serverId).build());
		ImmutableList<ItemDeletion> deletions = ImmutableList.<ItemDeletion> of(ItemDeletion.builder().serverId(serverIdDel).build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.changes(changes)
				.deletions(deletions)
				.build();
				
		assertThat(commands.getCommandsForType(SyncCommand.CHANGE)).containsOnly(SyncCollectionCommand.builder()
				.type(SyncCommand.CHANGE)
				.serverId(serverId)
				.build());
		assertThat(commands.getCommandsForType(SyncCommand.DELETE)).containsOnly(SyncCollectionCommand.builder()
				.type(SyncCommand.DELETE)
				.serverId(serverIdDel)
				.build());
	}
	
	@Test
	public void testChangesWithClientId() {
		ServerId serverId = CollectionId.of(1).serverId(23);
		ImmutableList<ItemChange> changes = ImmutableList.<ItemChange> of(ItemChange.builder().serverId(serverId).build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.changes(changes)
				.build();
				
		assertThat(commands.getCommandsForType(SyncCommand.CHANGE)).containsOnly(SyncCollectionCommand.builder()
				.type(SyncCommand.CHANGE)
				.serverId(serverId)
				.build());
	}
}
