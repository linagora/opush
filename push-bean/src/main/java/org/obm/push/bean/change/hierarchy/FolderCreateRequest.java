/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015  Linagora
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
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.state.FolderSyncKey;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class FolderCreateRequest {

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private FolderSyncKey syncKey;
		private CollectionId folderParentId;
		private String folderDisplayName;
		private FolderType folderType;
		
		private Builder() {}
		
		public Builder folderSyncKey(FolderSyncKey syncKey) {
			Preconditions.checkNotNull(syncKey, "Field synckey is required.");
			this.syncKey = syncKey;
			return this;
		}
		
		public Builder folderParentId(CollectionId folderParentId) {
			Preconditions.checkNotNull(folderParentId, "Field parentId is required.");
			this.folderParentId = folderParentId;
			return this;
		}
		
		public Builder folderDisplayName(String folderDisplayName) {
			Preconditions.checkNotNull(folderDisplayName, "Field displayName is required.");
			this.folderDisplayName = folderDisplayName;
			return this;
		}
		
		public Builder folderType(FolderType folderType) {
			Preconditions.checkNotNull(folderType, "Field type is required.");
			this.folderType = folderType;
			return this;
		}

		public FolderCreateRequest build() {
			Preconditions.checkNotNull(syncKey, "Field synckey is required.");
			Preconditions.checkNotNull(folderParentId, "Field parentId is required.");
			Preconditions.checkNotNull(folderDisplayName, "Field displayName is required.");
			Preconditions.checkNotNull(folderType, "Field type is required.");
			return new FolderCreateRequest(syncKey, folderParentId, folderDisplayName, folderType);
		}
	}

	private final FolderSyncKey syncKey;
	private final CollectionId folderParentId;
	private final String folderDisplayName;
	private final FolderType folderType;
	
	private FolderCreateRequest(FolderSyncKey syncKey, CollectionId folderParentId,
			String folderDisplayName, FolderType folderType) {      
		this.syncKey = syncKey;
		this.folderParentId = folderParentId;
		this.folderDisplayName = folderDisplayName;
		this.folderType = folderType;
	} 
	
	public FolderSyncKey getSyncKey() {
		return syncKey;
	}

	public CollectionId getFolderParentId() {
		return folderParentId;
	}

	public String getFolderDisplayName() {
		return folderDisplayName;
	}

	public FolderType getFolderType() {
		return folderType;
	}
	
	@Override
	public final int hashCode() {
		return Objects.hashCode(syncKey, folderParentId, folderDisplayName, folderType);
	}
	
	@Override
	public final boolean equals(Object object) {
		if (object instanceof FolderCreateRequest) {
			FolderCreateRequest that = (FolderCreateRequest) object;
			return Objects.equal(this.syncKey, that.syncKey)
				&& Objects.equal(this.folderParentId, that.folderParentId)
				&& Objects.equal(this.folderDisplayName, that.folderDisplayName)
				&& Objects.equal(this.folderType, that.folderType);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("syncKey", syncKey)
			.add("folderParentId", folderParentId)
			.add("folderDisplayName", folderDisplayName)
			.add("folderType", folderType)
			.toString();
	}
}

