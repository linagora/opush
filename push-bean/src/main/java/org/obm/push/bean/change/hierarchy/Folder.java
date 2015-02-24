/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015 Linagora
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
package org.obm.push.bean.change.hierarchy;

import org.obm.push.bean.FolderType;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class Folder {

	public static ImmutableMap<BackendId, Folder> mapByBackendId(Iterable<Folder> folders) {
		return Maps.uniqueIndex(folders,  new Function<Folder, BackendId>() {

			@Override
			public BackendId apply(Folder folder) {
				return folder.getBackendId();
			}
		});
	}

	public static Folder from(BackendFolder folder, CollectionId collectionId) {
		return Folder.builder()
			.collectionId(collectionId)
			.backendId(folder.getBackendId())
			.displayName(folder.getDisplayName())
			.folderType(folder.getFolderType())
			.parentBackendIdOpt(folder.getParentBackendId())
			.build();
	}
	
	public static Folder.Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private CollectionId collectionId;
		private BackendId backendId;
		private BackendId parentBackendId;
		private String displayName;
		private FolderType folderType;
		
		private Builder() {
			super();
		}
		
		public Folder.Builder collectionId(CollectionId collectionId) {
			Preconditions.checkNotNull(collectionId);
			this.collectionId = collectionId;
			return this;
		}
		
		public Folder.Builder backendId(BackendId backendId) {
			Preconditions.checkNotNull(backendId);
			this.backendId = backendId;
			return this;
		}

		public Folder.Builder displayName(String displayName) {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(displayName));
			this.displayName = displayName;
			return this;
		}

		public Folder.Builder folderType(FolderType folderType) {
			Preconditions.checkNotNull(folderType);
			this.folderType = folderType;
			return this;
		}
		
		public Folder.Builder parentBackendId(BackendId parentBackendId) {
			this.parentBackendId = parentBackendId;
			return this;
		}

		public Folder.Builder parentBackendIdOpt(Optional<BackendId> parentBackendId) {
			return parentBackendId(parentBackendId.orNull());
		}

		public Folder build() {
			Preconditions.checkNotNull(collectionId);
			Preconditions.checkNotNull(backendId);
			Preconditions.checkArgument(!Strings.isNullOrEmpty(displayName));
			Preconditions.checkNotNull(folderType);
			
			return new Folder(collectionId, backendId, displayName, folderType, parentBackendId);
		}
	}

	private final CollectionId collectionId;
	private final BackendId backendId;
	private final BackendId parentBackendId;
	private final String displayName;
	private final FolderType folderType;
	
	private Folder(CollectionId collectionId, BackendId backendId,
			String displayName, FolderType folderType, BackendId parentBackendId) {
		this.collectionId = collectionId;
		this.backendId = backendId;
		this.displayName = displayName;
		this.folderType = folderType;
		this.parentBackendId = parentBackendId;
	}

	public CollectionId getCollectionId() {
		return collectionId;
	}

	public BackendId getBackendId() {
		return backendId;
	}

	public Optional<BackendId> getParentBackendIdOpt() {
		return Optional.fromNullable(parentBackendId);
	}

	public BackendId getParentBackendId() {
		return parentBackendId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public FolderType getFolderType() {
		return folderType;
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(collectionId, backendId, displayName, folderType, parentBackendId);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof Folder) {
			Folder that = (Folder) object;
			return Objects.equal(this.collectionId, that.collectionId)
				&& Objects.equal(this.backendId, that.backendId)
				&& Objects.equal(this.displayName, that.displayName)
				&& Objects.equal(this.folderType, that.folderType)
				&& Objects.equal(this.parentBackendId, that.parentBackendId);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("collectionId", collectionId)
			.add("backendId", backendId)
			.add("displayName", displayName)
			.add("folderType", folderType)
			.add("parentBackendId", parentBackendId)
			.toString();
	}
	
}