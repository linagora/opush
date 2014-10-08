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

import org.obm.push.protocol.bean.CollectionId;

import com.google.common.base.Objects;

public class MoveItem {

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private ServerId sourceMessageId;
		private CollectionId sourceFolderId;
		private CollectionId destinationFolderId;

		private Builder() {}
		
		public Builder sourceMessageId(ServerId sourceMessageId) {
			this.sourceMessageId = sourceMessageId;
			return this;
		}
		
		public Builder sourceFolderId(CollectionId collectionId) {
			this.sourceFolderId = collectionId;
			return this;
		}
		
		public Builder destinationFolderId(CollectionId collectionId) {
			this.destinationFolderId = collectionId;
			return this;
		}
		
		public MoveItem build() {
			return new MoveItem(sourceMessageId, sourceFolderId, destinationFolderId);
		}
	}
	
	private final ServerId sourceMessageId;
	private final CollectionId sourceFolderId;
	private final CollectionId destinationFolderId;

	private MoveItem(ServerId sourceMessageId, CollectionId sourceFolderId, CollectionId destinationFolderId) {
		this.sourceMessageId = sourceMessageId;
		this.sourceFolderId = sourceFolderId;
		this.destinationFolderId = destinationFolderId;
	}
	
	public ServerId getSourceMessageId() {
		return sourceMessageId;
	}

	public CollectionId getSourceFolderId() {
		return sourceFolderId;
	}

	public CollectionId getDestinationFolderId() {
		return destinationFolderId;
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(sourceMessageId, sourceFolderId, destinationFolderId);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof MoveItem) {
			MoveItem that = (MoveItem) object;
			return Objects.equal(this.sourceMessageId, that.sourceMessageId)
				&& Objects.equal(this.sourceFolderId, that.sourceFolderId)
				&& Objects.equal(this.destinationFolderId, that.destinationFolderId);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("sourceMessageId", sourceMessageId)
			.add("sourceFolderId", sourceFolderId)
			.add("destinationFolderId", destinationFolderId)
			.toString();
	}
}
