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
package org.obm.push.protocol.bean;

import java.util.List;

import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncCollectionCommandRequest;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class SyncCollection {

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private PIMDataType dataType;
		private SyncKey syncKey;
		private CollectionId collectionId;
		private Boolean deletesAsMoves;
		private Boolean changes;
		private Integer windowSize;
		private SyncCollectionOptions options;
		private ImmutableList.Builder<SyncCollectionCommandRequest> commands;

		private Builder() {
			commands = ImmutableList.builder();
		}
		
		public Builder dataType(PIMDataType dataType) {
			this.dataType = dataType;
			return this;
		}
		
		public Builder syncKey(SyncKey syncKey) {
			this.syncKey = syncKey;
			return this;
		}
		
		public Builder collectionId(CollectionId id) {
			this.collectionId = id;
			return this;
		}
		
		public Builder deletesAsMoves(Boolean deletesAsMoves) {
			this.deletesAsMoves = deletesAsMoves;
			return this;
		}
		
		public Builder changes(Boolean changes) {
			this.changes = changes;
			return this;
		}
		
		public Builder windowSize(Integer windowSize) {
			this.windowSize = windowSize;
			return this;
		}
		
		public Builder options(SyncCollectionOptions options) {
			this.options = options;
			return this;
		}

		public Builder command(SyncCollectionCommandRequest command) {
			Preconditions.checkNotNull(command);
			this.commands.add(command);
			return this;
		}
		
		public Builder commands(List<SyncCollectionCommandRequest> commands) {
			this.commands.addAll(commands);
			return this;
		}

		public SyncCollection build() {
			return new SyncCollection(dataType, syncKey, collectionId, 
					deletesAsMoves, changes, windowSize, options, 
					commands.build());
		}

	}
	
	private final Boolean deletesAsMoves;
	private final Boolean changes;
	private final Integer windowSize;
	private final SyncCollectionOptions options;
	private final PIMDataType dataType;
	private final SyncKey syncKey;
	private final CollectionId collectionId;
	private final List<SyncCollectionCommandRequest> commands;
	
	protected SyncCollection(PIMDataType dataType, SyncKey syncKey, CollectionId collectionId,
			Boolean deletesAsMoves, Boolean changes, Integer windowSize, 
			SyncCollectionOptions options, List<SyncCollectionCommandRequest> commands) {
		this.dataType = dataType;
		this.syncKey = syncKey;
		this.collectionId = collectionId;
		this.deletesAsMoves = deletesAsMoves;
		this.changes = changes;
		this.windowSize = windowSize;
		this.options = options;
		this.commands = commands;
	}

	public PIMDataType getDataType() {
		return dataType;
	}
	
	public CollectionId getCollectionId() {
		return collectionId;
	}

	public SyncKey getSyncKey() {
		return syncKey;
	}
	
	public List<SyncCollectionCommandRequest> getCommands() {
		return commands;
	}

	public Boolean getDeletesAsMoves() {
		return deletesAsMoves;
	}

	public Boolean isChanges() {
		return changes;
	}

	public Integer getWindowSize() {
		return windowSize;
	}

	public SyncCollectionOptions getOptions() {
		return options;
	}

	public boolean hasOptions() {
		return options != null;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(deletesAsMoves, changes, windowSize, options, dataType, syncKey, collectionId, commands);
	}
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof SyncCollection) {
			SyncCollection that = (SyncCollection) object;
			return Objects.equal(this.collectionId, that.collectionId)
				&& Objects.equal(this.syncKey, that.syncKey)
				&& Objects.equal(this.dataType, that.dataType)
				&& Objects.equal(this.windowSize, that.windowSize)
				&& Objects.equal(this.deletesAsMoves, that.deletesAsMoves)
				&& Objects.equal(this.changes, that.changes)
				&& Objects.equal(this.options, that.options)
				&& Objects.equal(this.commands, that.commands);
		}
		return false;
	}


	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("collectionId", collectionId)
			.add("syncKey", syncKey)
			.add("dataType", dataType)
			.add("windowSize", windowSize)
			.add("deletesAsMoves", deletesAsMoves)
			.add("changes", changes)
			.add("options", options)
			.add("syncKey", syncKey)
			.add("commands", commands)
			.toString();
	}
}
