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
		private Integer fetchs;

		private Builder() {
		}
		
		public Builder changes(int changes) {
			Preconditions.checkArgument(changes >= 0, "Illegal changes value: " + changes);
			this.changes = changes;
			return this;
		}
		
		public Builder deletions(int deletions) {
			Preconditions.checkArgument(deletions >= 0, "Illegal deletions value: " + deletions);
			this.deletions = deletions;
			return this;
		}
		
		public Builder fetchs(int fetchs) {
			Preconditions.checkArgument(fetchs >= 0, "Illegal fetchs value: " + fetchs);
			this.fetchs = fetchs;
			return this;
		}
		
		public Summary build() {
			return new Summary(
					Objects.firstNonNull(changes, 0), 
					Objects.firstNonNull(deletions, 0), 
					Objects.firstNonNull(fetchs, 0));
		}
	}
	
	private int changes;
	private int deletions;
	private int fetchs;

	private Summary(int changes, int deletions, int fetchs) {
		this.changes = changes;
		this.deletions = deletions;
		this.fetchs = fetchs;
	}
	
	public int getChanges() {
		return changes;
	}
	
	public int getDeletions() {
		return deletions;
	}
	
	public int getFetchs() {
		return fetchs;
	}
	
	public String summary() {
		return String.format("CHANGE: %d, DELETE: %d, FETCH: %d", changes, deletions, fetchs);
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(changes, deletions, fetchs);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof Summary) {
			Summary that = (Summary) object;
			return Objects.equal(this.changes, that.changes)
				&& Objects.equal(this.deletions, that.deletions)
				&& Objects.equal(this.fetchs, that.fetchs);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("changes", changes)
			.add("deletions", deletions)
			.add("fetchs", fetchs)
			.toString();
	}
	
}
