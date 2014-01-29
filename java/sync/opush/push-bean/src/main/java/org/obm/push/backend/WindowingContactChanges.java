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
package org.obm.push.backend;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.obm.push.bean.change.WindowingChanges;
import org.obm.push.bean.change.WindowingChangesBuilder;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class WindowingContactChanges implements WindowingChanges<WindowingContact>, Serializable {
	
	public static WindowingContactChanges empty() {
		return builder().build();
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder implements WindowingChangesBuilder<WindowingContact> {
	
		private ImmutableSet.Builder<WindowingContact> additions;
		private ImmutableSet.Builder<WindowingContact> changes;
		private ImmutableSet.Builder<WindowingContact> deletions;
		
		private Builder() {
			super();
			additions = ImmutableSet.builder();
			changes = ImmutableSet.builder();
			deletions = ImmutableSet.builder();
		}
		
		@Override
		public Class<WindowingContact> getPIMDataClass() {
			return WindowingContact.class;
		}
		
		@Override
		public Builder deletion(WindowingContact contact) {
			this.deletions.add(contact);
			return this;
		}

		public Builder deletion(WindowingContact... contacts) {
			this.deletions.addAll(Arrays.asList(contacts));
			return this;
		}

		public Builder deletions(Collection<WindowingContact> deletions) {
			Preconditions.checkArgument(deletions != null, "deletions must not be null");
			this.deletions.addAll(deletions);
			return this;
		}
		
		@Override
		public Builder change(WindowingContact contact) {
			this.changes.add(contact);
			return this;
		}

		public Builder change(WindowingContact... contacts) {
			this.changes.addAll(Arrays.asList(contacts));
			return this;
		}
		
		public Builder changes(Collection<WindowingContact> changes) {
			Preconditions.checkArgument(changes != null, "changes must not be null");
			this.changes.addAll(changes);
			return this;
		}

		@Override
		public Builder addition(WindowingContact contact) {
			this.additions.add(contact);
			return this;
		}
		
		public Builder addition(WindowingContact... contacts) {
			this.additions.addAll(Arrays.asList(contacts));
			return this;
		}
		
		public Builder additions(Collection<WindowingContact> additions) {
			Preconditions.checkArgument(additions != null, "additions must not be null");
			this.additions.addAll(additions);
			return this;
		}
		
		public Builder merge(WindowingContactChanges changes) {
			additions(changes.additions());
			changes(changes.changes());
			deletions(changes.deletions());
			return this;
		}
		
		public int sumOfChanges() {
			return additions.build().size() + changes.build().size() + deletions.build().size();
		}

		@Override
		public WindowingContactChanges build() {
			return new WindowingContactChanges(deletions.build(), changes.build(), additions.build());
		}
	}
	
	private final Set<WindowingContact> deletions;
	private final Set<WindowingContact> changes;
	private final Set<WindowingContact> additions;
	
	private WindowingContactChanges(Set<WindowingContact> deletions, Set<WindowingContact> changes, Set<WindowingContact> additions) {
		this.deletions = deletions;
		this.changes = changes;
		this.additions = additions;
	}
	
	@Override
	public Set<WindowingContact> deletions() {
		return deletions;
	}

	@Override
	public Set<WindowingContact> changes() {
		return changes;
	}

	@Override
	public Set<WindowingContact> additions() {
		return additions;
	}

	@Override
	public int sumOfChanges() {
		return additions.size() + changes.size() + deletions.size();
	}

	@Override
	public final int hashCode() {
		return Objects.hashCode(deletions, changes, additions);
	}
	
	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof WindowingContactChanges) {
			WindowingContactChanges other = (WindowingContactChanges) obj;
			return Objects.equal(this.deletions, other.deletions)
				&& Objects.equal(this.changes, other.changes)
				&& Objects.equal(this.additions, other.additions);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("additions", additions)
				.add("changes", changes)
				.add("deletions", deletions)
				.toString();
	}
}
