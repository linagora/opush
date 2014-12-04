/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014  Linagora
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

import java.util.Set;

import org.obm.push.bean.AnalysedSyncCollection;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

public class ClientSyncRequest {

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private Integer waitInMinute;
		private Boolean partial;
		private Integer windowSize;
		private ImmutableSet.Builder<AnalysedSyncCollection> collections;

		private Builder() {
			this.collections = ImmutableSet.builder();
		}
		
		public Builder waitInMinute(int waitInMinute) {
			this.waitInMinute = waitInMinute;
			return this;
		}

		public Builder partial(Boolean partial) {
			this.partial = partial;
			return this;
		}
		
		public Builder windowSize(int windowSize) {
			this.windowSize = windowSize;
			return this;
		}

		public Builder addCollection(AnalysedSyncCollection collection) {
			collections.add(collection);
			return this;
		}

		public ClientSyncRequest build() {
			return new ClientSyncRequest(waitInMinute, partial, windowSize, collections.build());
		}
	}

	private final Integer waitInMinute;
	private final Boolean partial;
	private final Integer windowSize;
	private final Set<AnalysedSyncCollection> collections;
	
	protected ClientSyncRequest(Integer waitInMinute, Boolean partial, Integer windowSize,
			Set<AnalysedSyncCollection> collections) {
		this.waitInMinute = waitInMinute;
		this.partial = partial;
		this.windowSize = windowSize;
		this.collections = collections;
	}
	
	public Integer getWaitInMinute() {
		return waitInMinute;
	}
	
	public Boolean isPartial() {
		return partial;
	}
	
	public Set<AnalysedSyncCollection> getCollections() {
		return collections;
	}

	public Integer getWindowSize() {
		return windowSize;
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(waitInMinute, partial, windowSize, collections);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof ClientSyncRequest) {
			ClientSyncRequest that = (ClientSyncRequest) object;
			return Objects.equal(this.waitInMinute, that.waitInMinute)
				&& Objects.equal(this.partial, that.partial)
				&& Objects.equal(this.windowSize, that.windowSize)
				&& Objects.equal(this.collections, that.collections);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("waitInSecond", waitInMinute)
			.add("partial", partial)
			.add("windowSize", windowSize)
			.add("collections", collections)
			.toString();
	}
}
