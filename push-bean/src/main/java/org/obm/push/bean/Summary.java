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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class Summary {

	public static Summary empty() {
		return builder().build();
	}
	
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		
		private Integer changes;
		private Integer deletions;
		private Integer fetches;

		private Builder() {
		}
		
		public Builder changeCount(int changes) {
			Preconditions.checkArgument(changes >= 0, "Illegal changeCount value: " + changes);
			this.changes = changes;
			return this;
		}
		
		public Builder deletionCount(int deletions) {
			Preconditions.checkArgument(deletions >= 0, "Illegal deletionCount value: " + deletions);
			this.deletions = deletions;
			return this;
		}
		
		public Builder fetchCount(int fetches) {
			Preconditions.checkArgument(fetches >= 0, "Illegal fetchCount value: " + fetches);
			this.fetches = fetches;
			return this;
		}
		
		public Summary build() {
			return new Summary(
					Objects.firstNonNull(changes, 0), 
					Objects.firstNonNull(deletions, 0), 
					Objects.firstNonNull(fetches, 0));
		}
	}
	
	private int changeCount;
	private int deletionCount;
	private int fetchCount;

	private Summary(int changeCount, int deletionCount, int fetchCount) {
		this.changeCount = changeCount;
		this.deletionCount = deletionCount;
		this.fetchCount = fetchCount;
	}
	
	public int getChangeCount() {
		return changeCount;
	}
	
	public int getDeletionCount() {
		return deletionCount;
	}
	
	public int getFetchCount() {
		return fetchCount;
	}
	
	public String summary() {
		return String.format("CHANGE: %d, DELETE: %d, FETCH: %d", changeCount, deletionCount, fetchCount);
	}

	public Summary merge(Summary other) {
		Preconditions.checkNotNull(other);
		
		return Summary.builder()
			.changeCount(changeCount + other.changeCount)
			.deletionCount(deletionCount + other.deletionCount)
			.fetchCount(fetchCount + other.fetchCount)
			.build();
	}
	
	@Override
	public int hashCode(){
		return Objects.hashCode(changeCount, deletionCount, fetchCount);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof Summary) {
			Summary that = (Summary) object;
			return Objects.equal(this.changeCount, that.changeCount)
				&& Objects.equal(this.deletionCount, that.deletionCount)
				&& Objects.equal(this.fetchCount, that.fetchCount);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("changes", changeCount)
			.add("deletions", deletionCount)
			.add("fetches", fetchCount)
			.toString();
	}

}
