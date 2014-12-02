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
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.exception.activesync.ASRequestIntegerFieldException;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class SyncCollectionResponse implements Serializable {

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private PIMDataType dataType;
		private SyncKey syncKey;
		private CollectionId collectionId;
		private SyncStatus status;
		private boolean moreAvailable;
		private SyncCollectionCommandsResponse commands;
		private SyncCollectionResponsesResponse responses;

		private Builder() {
			super();
		}
		
		public Builder dataType(PIMDataType dataType) {
			this.dataType = dataType;
			return this;
		}
		
		public Builder syncKey(SyncKey syncKey) {
			this.syncKey = syncKey;
			return this;
		}
		
		public Builder collectionId(CollectionId collectionId) {
			this.collectionId = collectionId;
			return this;
		}
		
		public Builder status(SyncStatus status) {
			this.status = status;
			return this;
		}
		
		public Builder moreAvailable(boolean moreAvailable) {
			this.moreAvailable = moreAvailable;
			return this;
		}
		
		public Builder commands(SyncCollectionCommandsResponse commands) {
			this.commands = commands;
			return this;
		}
		
		public Builder responses(SyncCollectionResponsesResponse responses) {
			this.responses = responses;
			return this;
		}
		
		private void checkSyncCollectionCommonElements() {
			if (collectionId == null) {
				throw new ASRequestIntegerFieldException("Collection id field is required");
			}
		}
		
		public SyncCollectionResponse build() {
			checkSyncCollectionCommonElements();
			boolean moreAvailable = Objects.firstNonNull(this.moreAvailable, false);
			SyncKey syncKey = Objects.firstNonNull(this.syncKey, SyncKey.INITIAL_SYNC_KEY);
			return new SyncCollectionResponse(dataType, syncKey, collectionId, status, moreAvailable,
					Objects.firstNonNull(this.commands, SyncCollectionCommandsResponse.empty()),
					Objects.firstNonNull(this.responses, SyncCollectionResponsesResponse.empty()));
		}

	}
	
	private final PIMDataType dataType;
	private final SyncKey syncKey;
	private final CollectionId collectionId;
	private final SyncStatus status;
	private final boolean moreAvailable;
	private final SyncCollectionCommandsResponse commands;
	private final SyncCollectionResponsesResponse responses;
	
	private SyncCollectionResponse(PIMDataType dataType, SyncKey syncKey, CollectionId collectionId,
			SyncStatus status, boolean moreAvailable, SyncCollectionCommandsResponse commands, SyncCollectionResponsesResponse responses) {
		this.dataType = dataType;
		this.syncKey = syncKey;
		this.collectionId = collectionId;
		this.status = status;
		this.moreAvailable = moreAvailable;
		this.commands = commands;
		this.responses = responses;
	}
	
	public SyncCollectionCommandsResponse getCommands() {
		return commands;
	}
	
	public CollectionId getCollectionId() {
		return collectionId;
	}

	public PIMDataType getDataType() {
		return dataType;
	}
	
	public List<ServerId> getFetchIds() {
		return commands.getFetchIds();
	}
	
	public SyncStatus getStatus() {
		return status;
	}

	public SyncKey getSyncKey() {
		return syncKey;
	}
	
	public boolean isMoreAvailable() {
		return moreAvailable;
	}

	public SyncCollectionResponsesResponse getResponses() {
		return responses;
	}

	public List<ServerId> getResponseFetchIds() {
		return FluentIterable.from(
				responses.getCommandsForType(SyncCommand.FETCH))
				.transform(new Function<SyncCollectionCommand, ServerId>() {
					@Override
					public ServerId apply(SyncCollectionCommand input) {
						return input.getServerId();
					}
				}).toList();
	}

	public List<ItemDeletion> getItemDeletions() {
		if (commands != null) {
			return FluentIterable.from(
					commands.getCommandsForType(SyncCommand.DELETE))
					.transform(new Function<SyncCollectionCommand, ItemDeletion>() {
	
						@Override
						public ItemDeletion apply(SyncCollectionCommand input) {
							return ItemDeletion.builder().serverId(input.getServerId()).build();
						}
					}).toList();
		}
		return Lists.newArrayList();
	}
	
	public List<ItemChange> getItemFetchs() {
		Iterable<SyncCollectionCommand> fetchs = getFetchs();
		if (fetchs != null) {
			return FluentIterable.from(fetchs)
					.transform(new Function<SyncCollectionCommand, ItemChange>() {
	
						@Override
						public ItemChange apply(SyncCollectionCommand fetch) {
							return ItemChange.builder()
									.serverId(fetch.getServerId())
									.isNew(false)
									.data(fetch.getApplicationData())
									.build();
						}
					}).toList();
		}
		return Lists.newArrayList();
	}

	private Iterable<SyncCollectionCommand> getFetchs() {
		SyncCollectionResponsesResponse commands = getResponses();
		if (commands != null) {
			return commands.getCommandsForType(SyncCommand.FETCH);
		}
		return Lists.newArrayList();
	}

	public List<ItemChange> getItemChanges() {
		Iterable<SyncCollectionCommand> changes = getChanges();
		if (changes != null) {
			return FluentIterable.from(changes)
					.transform(new Function<SyncCollectionCommand, ItemChange>() {
	
						@Override
						public ItemChange apply(SyncCollectionCommand change) {
							return ItemChange.builder()
									.serverId(change.getServerId())
									.isNew(SyncCommand.ADD.equals(change.getType()))
									.data(change.getApplicationData())
									.build();
						}
					}).toList();
		}
		return Lists.newArrayList();
	}

	private Iterable<SyncCollectionCommand> getChanges() {
		if (commands != null) {
			return Iterables.concat(
					commands.getCommandsForType(SyncCommand.ADD),
					commands.getCommandsForType(SyncCommand.CHANGE),
					commands.getCommandsForType(SyncCommand.MODIFY));
		}
		return Lists.newArrayList();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(dataType, syncKey, collectionId, commands, status, moreAvailable);
	}
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof SyncCollectionResponse) {
			SyncCollectionResponse that = (SyncCollectionResponse) object;
			return Objects.equal(this.dataType, that.dataType)
					&& Objects.equal(this.syncKey, that.syncKey)
					&& Objects.equal(this.collectionId, that.collectionId)
					&& Objects.equal(this.commands, that.commands)
					&& Objects.equal(this.status, that.status)
					&& Objects.equal(this.moreAvailable, that.moreAvailable);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("dataType", dataType)
				.add("syncKey", syncKey)
				.add("collectionId", collectionId)
				.add("commands", commands)
				.add("status", status)
				.add("moreAvailable", moreAvailable)
			.toString();
	}

}
