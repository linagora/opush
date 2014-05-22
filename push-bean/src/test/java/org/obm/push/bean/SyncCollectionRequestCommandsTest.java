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

import com.google.common.collect.ImmutableList;


public class SyncCollectionRequestCommandsTest {

	@Test
	public void testBuilderFetchIdsIsNotRequired() {
		SyncCollectionRequestCommands commands = SyncCollectionRequestCommands.builder()
			.fetchIds(null).build();
		
		assertThat(commands.getFetchIds()).isEmpty();
	}

	@Test
	public void testBuilderFetchIdsValid() {
		SyncCollectionRequestCommands commands = SyncCollectionRequestCommands.builder()
			.fetchIds(ImmutableList.of("1234", "5678")).build();
		
		assertThat(commands.getFetchIds()).containsOnly("1234", "5678");
	}

	@Test
	public void testBuilderCommandsIsNotRequired() {
		SyncCollectionRequestCommands commands = SyncCollectionRequestCommands.builder()
			.commands(null).build();
		
		assertThat(commands.getCommands()).isEmpty();
	}

	@Test
	public void testBuilderCommandsValid() {
		SyncCollectionRequestCommands commands = SyncCollectionRequestCommands.builder()
			.commands(ImmutableList.of(
					SyncCollectionRequestCommand.builder().name("Delete").serverId("3").build(),
					SyncCollectionRequestCommand.builder().name("Fetch").serverId("8").build())).build();
		
		assertThat(commands.getCommands()).containsOnly(
				SyncCollectionRequestCommand.builder().name("Delete").serverId("3").build(),
				SyncCollectionRequestCommand.builder().name("Fetch").serverId("8").build());
	}
	
	@Test
	public void testRequestSummaryManyEntries() {
		SyncCollectionCommandsRequest commands = SyncCollectionCommandsRequest.builder()
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.FETCH).serverId("123786").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD).clientId("45789").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD).clientId("165873").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.CHANGE).serverId("1234478").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.DELETE).serverId("234").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD).clientId("75332").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.CHANGE).serverId("78675").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD).clientId("4358").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.CHANGE).serverId("37534").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD).clientId("1321231").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.CHANGE).serverId("5469863").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.FETCH).serverId("123").build())
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.CHANGE).serverId("1234535").build())
				.build();
		assertThat(commands.summary()).isEqualTo("CHANGE: 10, DELETE: 1, FETCH: 2");
	}

	@Test
	public void testRequestSummaryOneDeletion() {
		SyncCollectionCommandsRequest commands = SyncCollectionCommandsRequest.builder()
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.DELETE).serverId("234").build())
				.build();
		assertThat(commands.summary()).isEqualTo("CHANGE: 0, DELETE: 1, FETCH: 0");
	}
	
	@Test
	public void testRequestSummaryOneFetch() {
		SyncCollectionCommandsRequest commands = SyncCollectionCommandsRequest.builder()
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.FETCH).serverId("123").build())
				.build();
		assertThat(commands.summary()).isEqualTo("CHANGE: 0, DELETE: 0, FETCH: 1");
	}
	

	@Test
	public void testRequestSummaryNoEntry() {
		SyncCollectionCommandsRequest commands = SyncCollectionCommandsRequest.builder().build();
		assertThat(commands.summary()).isEqualTo("CHANGE: 0, DELETE: 0, FETCH: 0");
	}

	@Test
	public void testRequestSummaryOneChange() {
		SyncCollectionCommandsRequest commands = SyncCollectionCommandsRequest.builder()
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.CHANGE).serverId("123").build())
				.build();
		assertThat(commands.summary()).isEqualTo("CHANGE: 1, DELETE: 0, FETCH: 0");
	}
	
	@Test
	public void testRequestSummaryOneAddition() {
		SyncCollectionCommandsRequest commands = SyncCollectionCommandsRequest.builder()
				.addCommand(SyncCollectionCommandRequest.builder().type(SyncCommand.ADD).clientId("45679").build())
				.build();
		assertThat(commands.summary()).isEqualTo("CHANGE: 1, DELETE: 0, FETCH: 0");
	}
	
}
