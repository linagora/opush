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

import java.util.List;

import org.obm.push.bean.change.SyncCommand;

import com.google.common.base.Objects;

public class SyncCollectionCommandsRequest implements SyncCollectionCommands {
	
	public static SyncCollectionCommandsRequest empty() {
		return builder().build();
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private SyncCollectionCommandsIndex.Builder commandsBuilder;

		private Builder() {
			commandsBuilder = SyncCollectionCommandsIndex.builder();
		}
		
		public Builder addCommand(SyncCollectionCommand command) {
			commandsBuilder.addCommand(command);
			return this;
		}

		public SyncCollectionCommandsRequest build() {
			return new SyncCollectionCommandsRequest(commandsBuilder.build());
		}
	}
	
	private final SyncCollectionCommandsIndex commands;

	private SyncCollectionCommandsRequest(SyncCollectionCommandsIndex commands) {
		this.commands = commands;
	}

	@Override
	public List<SyncCollectionCommand> getCommands() {
		return commands.getCommands();
	}

	@Override
	public List<SyncCollectionCommand> getCommandsForType(SyncCommand type) {
		return commands.getCommandsForType(type);
	}

	@Override
	public Summary getSummary() {
		return commands.getSummary();
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(commands);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof SyncCollectionCommandsRequest) {
			SyncCollectionCommandsRequest that = (SyncCollectionCommandsRequest) object;
			return Objects.equal(this.commands, that.commands);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("commands", commands)
			.toString();
	}

}
