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
import org.obm.push.bean.change.client.SyncClientCommands;
import org.obm.push.bean.change.client.SyncClientCommands.Add;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;

import com.google.common.collect.ImmutableList;


public class SyncCollectionCommandsTest {

	@Test
	public void testBuilderCommandsIsNotRequired() {
		SyncCollectionCommandsRequest commands = SyncCollectionCommandsRequest.builder()
			.build();
		
		assertThat(commands.getCommands()).isEmpty();
	}

	@Test
	public void testBuilderCommandsValid() {
		SyncCollectionCommandsRequest commands = SyncCollectionCommandsRequest.builder()
			.addCommand(SyncCollectionCommandRequest.builder().name("Delete").serverId("3").build())
			.addCommand(SyncCollectionCommandRequest.builder().name("Fetch").serverId("8").build())
			.build();
		
		assertThat(commands.getCommands()).containsOnly(
				SyncCollectionCommandRequest.builder().name("Delete").serverId("3").build(),
				SyncCollectionCommandRequest.builder().name("Fetch").serverId("8").build());
	}
	
	@Test
	public void testChangesAndDeletions() {
		ImmutableList<ItemChange> changes = ImmutableList.<ItemChange> of(ItemChange.builder().serverId("123").build());
		ImmutableList<ItemDeletion> deletions = ImmutableList.<ItemDeletion> of(ItemDeletion.builder().serverId("234").build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.changes(changes, SyncClientCommands.builder().build())
				.deletions(deletions)
				.build();
				
		assertThat(commands.getCommandsForType(SyncCommand.CHANGE)).containsOnly(SyncCollectionCommandResponse.builder()
				.type(SyncCommand.CHANGE)
				.serverId("123")
				.build());
		assertThat(commands.getCommandsForType(SyncCommand.DELETE)).containsOnly(SyncCollectionCommandResponse.builder()
				.type(SyncCommand.DELETE)
				.serverId("234")
				.build());
	}
	
	@Test
	public void testChangesWithClientId() {
		String serverId = "123";
		String clientId = "456";
		ImmutableList<ItemChange> changes = ImmutableList.<ItemChange> of(ItemChange.builder().serverId(serverId).build());
		SyncCollectionCommandsResponse commands = SyncCollectionCommandsResponse.builder()
				.changes(changes, SyncClientCommands.builder()
						.putAdd(new Add(clientId, serverId))
						.build())
				.build();
				
		assertThat(commands.getCommandsForType(SyncCommand.CHANGE)).containsOnly(SyncCollectionCommandResponse.builder()
				.type(SyncCommand.CHANGE)
				.serverId(serverId)
				.clientId(clientId)
				.build());
	}
}
