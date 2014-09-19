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

import java.io.Serializable;
import java.util.List;

import org.obm.push.bean.change.SyncCommand;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

public abstract class SyncCollectionCommands implements Serializable {

	private static final long serialVersionUID = 5403154747427044879L;

	public abstract static class Builder<C extends SyncCollectionCommands> {
		
		private final ImmutableList.Builder<SyncCollectionCommand> commandsBuilder;
		private ImmutableList<SyncCollectionCommand> commands;
 		private ImmutableListMultimap<SyncCommand, SyncCollectionCommand> commandsByType;

		protected Builder() {
			super();
			commandsBuilder = ImmutableList.builder();
		}
		
		public Builder<C> addCommand(SyncCollectionCommand command) {
			commandsBuilder.add(command);
			return this;
		}
		
		protected C abstractBuild() {
			commands = this.commandsBuilder.build();
			commandsByType = commandsByType(commands);
			return buildImpl(commandsByType, commands);
		}
		
		public abstract C build();
		
		protected abstract C buildImpl(
			ImmutableListMultimap<SyncCommand, SyncCollectionCommand> commandsByType, 
			List<SyncCollectionCommand> commands);
		
		private ImmutableListMultimap<SyncCommand, SyncCollectionCommand> commandsByType(List<SyncCollectionCommand> commands) {
			return FluentIterable.from(commands)
						.index(new Function<SyncCollectionCommand, SyncCommand>() {

					@Override
					public SyncCommand apply(SyncCollectionCommand input) {
						return input.getType();
					}
				});
		}
	}
	
	private final ImmutableListMultimap<SyncCommand, SyncCollectionCommand> commandsByType;
	private final List<SyncCollectionCommand> commands;
	
	protected SyncCollectionCommands(
		ImmutableListMultimap<SyncCommand, SyncCollectionCommand> commandsByType, 
		List<SyncCollectionCommand> commands) {
		
		this.commandsByType = commandsByType;
		this.commands = commands;
	}

	public List<SyncCollectionCommand> getCommandsForType(SyncCommand type) {
		return Objects.firstNonNull(commandsByType.get(type), ImmutableList.<SyncCollectionCommand>of());
	}
	
	public List<SyncCollectionCommand> getCommands() {
		return commands;
	}

	private int countChanges() {
		return getCommandsForType(SyncCommand.ADD).size() 
				+ getCommandsForType(SyncCommand.CHANGE).size() 
				+ getCommandsForType(SyncCommand.MODIFY).size();
	}
	
	private int countDeletions() {
		return getCommandsForType(SyncCommand.DELETE).size();
	}

	private int countFetchs() {
		return getCommandsForType(SyncCommand.FETCH).size();
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(commandsByType, commands);
	}

	public Summary getSummary() {
		return Summary.builder()
				.changeCount(countChanges())
				.deletionCount(countDeletions())
				.fetchCount(countFetchs())
				.build();
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof SyncCollectionCommands) {
			SyncCollectionCommands that = (SyncCollectionCommands) object;
			return Objects.equal(this.commandsByType, that.commandsByType)
				&& Objects.equal(this.commands, that.commands);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("commandsByType", commandsByType)
			.toString();
	}
	
}
