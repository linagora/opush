/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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

import java.util.List;

import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.client.SyncClientCommands;
import org.obm.push.bean.change.client.SyncClientCommands.Add;
import org.obm.push.bean.change.client.SyncClientCommands.Deletion;
import org.obm.push.bean.change.client.SyncClientCommands.Fetch;
import org.obm.push.bean.change.client.SyncClientCommands.Update;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

public class SyncCollectionResponsesResponse extends SyncCollectionCommands {

	private static final long serialVersionUID = -6871877347639563687L;

	private SyncCollectionResponsesResponse(
			ImmutableListMultimap<SyncCommand, SyncCollectionCommand> commandsByType, 
			List<SyncCollectionCommand> commands) {
		super(commandsByType, commands);
	}
	
	public static SyncCollectionResponsesResponse empty() {
		return builder().build();
	}
	
	public static SyncCollectionResponsesResponse from(SyncClientCommands clientCommands) {
		return builder()
				.adds(clientCommands.getAdds())
				.updates(clientCommands.getUpdates())
				.deletions(clientCommands.getDeletions())
				.fetchs(clientCommands.getFetches())
				.build();
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder extends SyncCollectionCommands.Builder<SyncCollectionResponsesResponse> {
		
		private Builder() {
			super();
		}
		
		@Override
		protected SyncCollectionResponsesResponse buildImpl(
				ImmutableListMultimap<SyncCommand, SyncCollectionCommand> commandsByType, 
				List<SyncCollectionCommand> commands) {
			return new SyncCollectionResponsesResponse(commandsByType, commands);
		}

		@Override
		public SyncCollectionResponsesResponse build() {
			return abstractBuild();
		}

		public Builder adds(List<Add> adds) {
			for (Add add: adds) {
				addCommand(
						SyncCollectionCommand.builder()
							.status(add.getSyncStatus())
							.type(add.syncCommand())
							.serverId(add.getServerId())
							.clientId(add.getClientId())
							.build());
			}
			return this;
		}

		public Builder updates(List<Update> updates) {
			for (Update update: updates) {
				addCommand(
						SyncCollectionCommand.builder()
							.status(update.getSyncStatus())
							.type(update.syncCommand())
							.serverId(update.getServerId())
							.build());
			}
			return this;
		}

		public Builder deletions(List<Deletion> deletions) {
			for (Deletion deletion: deletions) {
				addCommand(
						SyncCollectionCommand.builder()
							.status(deletion.getSyncStatus())
							.type(deletion.syncCommand())
							.serverId(deletion.getServerId())
							.build());
			}
			return this;
		}

		public Builder fetchs(List<Fetch> fetches) {
			for (Fetch fetch: fetches) {
				addCommand(
						SyncCollectionCommand.builder()
							.status(fetch.getSyncStatus())
							.type(fetch.syncCommand())
							.serverId(fetch.getServerId())
							.applicationData(fetch.getApplicationData())
							.build());
			}
			return this;
		}
	}
	
	public List<ServerId> adds() {
		return serverIdsOfCommandType(SyncCommand.ADD);
	}
	
	public List<ServerId> updates() {
		return ImmutableList.<ServerId> builder()
				.addAll(serverIdsOfCommandType(SyncCommand.CHANGE))
				.addAll(serverIdsOfCommandType(SyncCommand.MODIFY))
				.build();
	}
	
	public List<ServerId> deletions() {
		return serverIdsOfCommandType(SyncCommand.DELETE);
	}
	
	public List<ServerId> fetches() {
		return serverIdsOfCommandType(SyncCommand.FETCH);
	}

	private List<ServerId> serverIdsOfCommandType(SyncCommand syncCommand) {
		return FluentIterable
				.from(getCommandsForType(syncCommand))
				.transform(new Function<SyncCollectionCommand, ServerId>() {

					@Override
					public ServerId apply(SyncCollectionCommand SyncCollectionCommand) {
						return SyncCollectionCommand.getServerId();
					}
				}).toList();
	}
}
