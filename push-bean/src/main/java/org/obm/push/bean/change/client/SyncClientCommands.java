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
package org.obm.push.bean.change.client;

import java.util.List;

import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.change.SyncCommand;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

public class SyncClientCommands {

	public static SyncClientCommands empty() {
		return builder().build();
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private final ImmutableList.Builder<Add> adds;
		private final ImmutableList.Builder<Update> updates;
		private final ImmutableList.Builder<Deletion> deletions;
		private final ImmutableList.Builder<Fetch> fetches;

		private Builder() {
			adds = ImmutableList.builder();
			updates = ImmutableList.builder();
			deletions = ImmutableList.builder();
			fetches = ImmutableList.builder();
		}
		
		public Builder putAdd(Add add) {
			adds.add(add);
			return this;
		}
		
		public Builder putUpdate(Update update) {
			updates.add(update);
			return this;
		}
		
		public Builder putDeletion(Deletion deletion) {
			deletions.add(deletion);
			return this;
		}
		
		public Builder putFetch(Fetch fetch) {
			fetches.add(fetch);
			return this;
		}
		
		public SyncClientCommands build() {
			return new SyncClientCommands(adds.build(), updates.build(), deletions.build(), fetches.build());
		}
	}

	public static abstract class ClientCommand {
		
		protected final ServerId serverId;
		protected final SyncStatus syncStatus;

		protected ClientCommand(ServerId serverId, SyncStatus syncStatus) {
			Preconditions.checkArgument(serverId != null, "serverId is required");
			Preconditions.checkArgument(syncStatus != null, "syncStatus is required");
			this.serverId = serverId;
			this.syncStatus = syncStatus;
		}

		public abstract SyncCommand syncCommand();

		public ServerId getServerId() {
			return serverId;
		}
		
		public SyncStatus getSyncStatus() {
			return syncStatus;
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.add("serverId", serverId)
					.add("syncStatus", syncStatus)
					.toString();
		}
	}
	
	public static class Update extends ClientCommand {

		public Update(ServerId serverId, SyncStatus syncStatus) {
			super(serverId, syncStatus);
		}

		@Override
		public SyncCommand syncCommand() {
			return SyncCommand.CHANGE;
		}
		
		@Override
		public final int hashCode(){
			return Objects.hashCode(serverId, syncStatus);
		}

		@Override
		public boolean equals(Object object){
			if (object instanceof Update) {
				Update that = (Update) object;
				return Objects.equal(this.serverId, that.serverId)
					&& Objects.equal(this.syncStatus, that.syncStatus);
			}
			return false;
		}
	}
	
	public static class Deletion extends ClientCommand {

		public Deletion(ServerId serverId, SyncStatus syncStatus) {
			super(serverId, syncStatus);
		}

		@Override
		public SyncCommand syncCommand() {
			return SyncCommand.DELETE;
		}
		
		@Override
		public final int hashCode(){
			return Objects.hashCode(serverId, syncStatus);
		}

		@Override
		public final boolean equals(Object object){
			if (object instanceof Deletion) {
				Deletion that = (Deletion) object;
				return Objects.equal(this.serverId, that.serverId)
					&& Objects.equal(this.syncStatus, that.syncStatus);
			}
			return false;
		}
	}
	
	public static class Fetch extends ClientCommand {

		private final IApplicationData applicationData;

		public Fetch(ServerId serverId, SyncStatus syncStatus, IApplicationData applicationData) {
			super(serverId, syncStatus);
			this.applicationData = applicationData;
		}

		@Override
		public SyncCommand syncCommand() {
			return SyncCommand.FETCH;
		}

		public IApplicationData getApplicationData() {
			return applicationData;
		}
		
		@Override
		public final int hashCode(){
			return Objects.hashCode(serverId, syncStatus, applicationData);
		}
		
		@Override
		public final boolean equals(Object object){
			if (object instanceof Fetch) {
				Fetch that = (Fetch) object;
				return Objects.equal(this.serverId, that.serverId)
					&& Objects.equal(this.syncStatus, that.syncStatus)
					&& Objects.equal(this.applicationData, that.applicationData);
			}
			return false;
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this)
				.add("serverId", serverId)
				.add("syncStatus", syncStatus)
				.add("data", applicationData)
				.toString();
		}
	}

	public static class Add extends ClientCommand {
		
		public final String clientId;

		public Add(String clientId, ServerId serverId, SyncStatus syncStatus) {
			super(serverId, syncStatus);
			Preconditions.checkArgument(!Strings.isNullOrEmpty(clientId), "clientId is required");
			this.clientId = clientId;
		}

		public String getClientId() {
			return clientId;
		}
		
		@Override
		public SyncCommand syncCommand() {
			return SyncCommand.ADD;
		}

		public SyncStatus getSyncStatus() {
			return syncStatus;
		}
		
		@Override
		public final int hashCode(){
			return Objects.hashCode(serverId, syncStatus, clientId);
		}
		
		@Override
		public final boolean equals(Object object){
			if (object instanceof Add) {
				Add that = (Add) object;
				return Objects.equal(this.serverId, that.serverId)
					&& Objects.equal(this.syncStatus, that.syncStatus)
					&& Objects.equal(this.clientId, that.clientId);
			}
			return false;
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this)
				.add("serverId", serverId)
				.add("syncStatus", syncStatus)
				.add("clientId", clientId)
				.toString();
		}
	}
	
	private final List<Add> adds;
	private final List<Update> updates;
	private final List<Deletion> deletions;
	private final List<Fetch> fetches;

	private SyncClientCommands(List<Add> adds, List<Update> updates, List<Deletion> deletions, List<Fetch> fetches) {
		this.adds = adds;
		this.updates = updates;
		this.deletions = deletions;
		this.fetches = fetches;
	}
	
	public List<Add> getAdds() {
		return adds;
	}
	
	public List<Add> getAddsAsItemChange() {
		return adds;
	}
	
	public List<Update> getUpdates() {
		return updates;
	}
	
	public List<Deletion> getDeletions() {
		return deletions;
	}
	
	public List<Fetch> getFetches() {
		return fetches;
	}

	public int sumOfCommands() {
		return adds.size() + updates.size() + deletions.size() + fetches.size();
	}

	public boolean hasAddWithServerId(final ServerId serverId) {
		return getAddWithServerId(serverId).isPresent();
	}

	public Optional<Add> getAddWithServerId(final ServerId serverId) {
		return FluentIterable.from(adds).firstMatch(new Predicate<Add>() {
			
				@Override
				public boolean apply(Add input) {
					return serverId.equals(input.serverId);
				}
			});
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(adds, updates, deletions, fetches);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof SyncClientCommands) {
			SyncClientCommands that = (SyncClientCommands) object;
			return Objects.equal(this.adds, that.adds)
				&& Objects.equal(this.updates, that.updates)
				&& Objects.equal(this.deletions, that.deletions)
				&& Objects.equal(this.fetches, that.fetches);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("adds", adds)
			.add("updates", updates)
			.add("deletions", deletions)
			.add("fetches", fetches)
			.toString();
	}
	
}