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
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableListMultimap;

@JsonDeserialize(builder=SyncCollectionCommandsResponse.Builder.class)
public class SyncCollectionCommandsResponse extends SyncCollectionCommands<SyncCollectionCommandResponse> {

	private static final long serialVersionUID = -6871877347639563687L;

	private SyncCollectionCommandsResponse(
			ImmutableListMultimap<SyncCommand, SyncCollectionCommandResponse> commandsByType, 
			List<SyncCollectionCommandResponse> commands) {
		super(commandsByType, commands);
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder extends SyncCollectionCommands.Builder<SyncCollectionCommandResponse, SyncCollectionCommandsResponse> {
		
		private Builder() {
			super();
		}
		
		@Override
		protected SyncCollectionCommandsResponse buildImpl(
				ImmutableListMultimap<SyncCommand, SyncCollectionCommandResponse> commandsByType, 
				List<SyncCollectionCommandResponse> commands) {
			return new SyncCollectionCommandsResponse(commandsByType, commands);
		}

		@Override
		public SyncCollectionCommandsResponse build() {
			return abstractBuild();
		}
		
		public Builder changes(List<ItemChange> changes, SyncClientCommands clientCommands) {
			for (ItemChange change: changes) {
				String serverId = change.getServerId();
				
				SyncCollectionCommandResponse.Builder builder = SyncCollectionCommandResponse.builder();
				builder.applicationData(change.getData())
					.type(retrieveCommandType(change))
					.serverId(serverId);
				
				if (clientCommands.hasAddWithServerId(serverId)){
					builder.clientId(clientCommands.getAddWithServerId(serverId).get().getClientId());
				}
				addCommand(builder.build());
			}
			return this;
		}

		public Builder fetchs(List<ItemChange> fetchs) {
			for (ItemChange fetch: fetchs) {
				addCommand(
						SyncCollectionCommandResponse.builder()
							.applicationData(fetch.getData())
							.type(SyncCommand.FETCH)
							.serverId(fetch.getServerId())
							.build());
			}
			return this;
		}

		private SyncCommand retrieveCommandType(ItemChange change) {
			return change.isNew() ? SyncCommand.ADD : SyncCommand.CHANGE;
		}

		public Builder deletions(List<ItemDeletion> deletions) {
			for (ItemDeletion deletion: deletions) {
				addCommand(
						SyncCollectionCommandResponse.builder()
							.type(SyncCommand.DELETE)
							.serverId(deletion.getServerId())
							.build());
			}
			return this;
		}
	}
}
