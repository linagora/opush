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

import java.util.Set;

import org.obm.push.bean.FolderType;
import org.obm.push.bean.Stringable;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class FolderSnapshot {
	
	public static final int FIRST_NEXT_ID = 1;
	
	public static FolderSnapshot empty() {
		return new Builder(FIRST_NEXT_ID).folders(ImmutableSet.<Folder>of());
	}
	
	public static FolderSnapshot.Builder nextId(int nextId) {
		return new Builder(nextId);
	}
	
	public static class Builder {
		
		private Integer nextId;

		private Builder(int nextId) {
			Preconditions.checkArgument(nextId > 0, "nextId must be a positive integer");
			this.nextId = nextId;
		}
		
		public FolderSnapshot folders(Set<Folder> folders) {
			Preconditions.checkNotNull(nextId, "nextId must be a positive integer");
			Preconditions.checkNotNull(folders, "folders can't be null");
			return new FolderSnapshot(nextId, ImmutableSet.copyOf(folders));
		}
	}
	
	private final int nextId;
	private final Set<Folder> folders;
	private final ImmutableMap<String, Folder> foldersByBackendId;

	private FolderSnapshot(int nextId, Set<Folder> folders) {
		this.nextId = nextId;
		this.folders = folders;
		this.foldersByBackendId = Folder.mapByBackendId(folders);
	}

	public int getNextId() {
		return nextId;
	}

	public Set<Folder> getFolders() {
		return folders;
	}

	public ImmutableMap<String, Folder> getFoldersByBackendId() {
		return foldersByBackendId;
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(nextId, folders);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof FolderSnapshot) {
			FolderSnapshot that = (FolderSnapshot) object;
			return Objects.equal(this.nextId, that.nextId)
				&& Objects.equal(this.folders, that.folders);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("nextId", nextId)
			.add("folders", folders)
			.toString();
	}
	
	public static class Folder {

		public static ImmutableMap<String, Folder> mapByBackendId(Iterable<Folder> folders) {
			return Maps.uniqueIndex(folders,  new Function<Folder, String>() {

				@Override
				public String apply(Folder folder) {
					return folder.getBackendId();
				}
			});
		}

		public static <T extends Stringable> Folder from(BackendFolder<T> folder, CollectionId collectionId) {
			return Folder.builder()
				.collectionId(collectionId)
				.backendId(folder.getBackendId().asString())
				.displayName(folder.getDisplayName())
				.folderType(folder.getFolderType())
				.parentBackendId(folder.getParentBackendId().transform(new Function<T, String>() {
					@Override
					public String apply(T input) {
						return input.asString();
					}
				}))
				.build();
		}
		
		public static Folder.Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private CollectionId collectionId;
			private String backendId;
			private Optional<String> parentBackendId;
			private String displayName;
			private FolderType folderType;
			
			private Builder() {
				super();
			}
			
			public Builder collectionId(CollectionId collectionId) {
				Preconditions.checkNotNull(collectionId);
				this.collectionId = collectionId;
				return this;
			}
			
			public Builder backendId(String backendId) {
				Preconditions.checkNotNull(backendId);
				this.backendId = backendId;
				return this;
			}

			public Builder displayName(String displayName) {
				Preconditions.checkArgument(!Strings.isNullOrEmpty(displayName));
				this.displayName = displayName;
				return this;
			}

			public Builder folderType(FolderType folderType) {
				Preconditions.checkNotNull(folderType);
				this.folderType = folderType;
				return this;
			}


			public Builder parentBackendId(Optional<String> parentBackendId) {
				Preconditions.checkNotNull(parentBackendId);
				this.parentBackendId = parentBackendId;
				return this;
			}

			public Folder build() {
				Preconditions.checkNotNull(collectionId);
				Preconditions.checkNotNull(backendId);
				Preconditions.checkArgument(!Strings.isNullOrEmpty(displayName));
				Preconditions.checkNotNull(folderType);
				Preconditions.checkNotNull(parentBackendId);
				
				return new Folder(collectionId, backendId, displayName, folderType, parentBackendId);
			}
		}

		private final CollectionId collectionId;
		private final String backendId;
		private final Optional<String> parentBackendId;
		private final String displayName;
		private final FolderType folderType;
		
		private Folder(CollectionId collectionId, String backendId,
				String displayName, FolderType folderType, Optional<String> parentBackendId) {
			this.collectionId = collectionId;
			this.backendId = backendId;
			this.displayName = displayName;
			this.folderType = folderType;
			this.parentBackendId = parentBackendId;
		}

		public CollectionId getCollectionId() {
			return collectionId;
		}

		public String getBackendId() {
			return backendId;
		}

		public Optional<String> getParentBackendId() {
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
}