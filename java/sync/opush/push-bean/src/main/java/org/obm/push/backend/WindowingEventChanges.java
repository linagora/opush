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

public class WindowingEventChanges implements WindowingChanges<WindowingEvent>, Serializable {
	
	public static WindowingEventChanges empty() {
		return builder().build();
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder implements WindowingChangesBuilder<WindowingEvent, WindowingEventChanges> {
	
		private ImmutableSet.Builder<WindowingEvent> additions;
		private ImmutableSet.Builder<WindowingEvent> changes;
		private ImmutableSet.Builder<WindowingEvent> deletions;
		
		private Builder() {
			super();
			additions = ImmutableSet.builder();
			changes = ImmutableSet.builder();
			deletions = ImmutableSet.builder();
		}
		
		@Override
		public Class<WindowingEvent> getPIMDataClass() {
			return WindowingEvent.class;
		}
		
		@Override
		public Builder deletion(WindowingEvent event) {
			this.deletions.add(event);
			return this;
		}

		public Builder deletion(WindowingEvent... events) {
			this.deletions.addAll(Arrays.asList(events));
			return this;
		}

		public Builder deletions(Collection<WindowingEvent> deletions) {
			Preconditions.checkArgument(deletions != null, "deletions must not be null");
			this.deletions.addAll(deletions);
			return this;
		}
		
		@Override
		public Builder change(WindowingEvent event) {
			this.changes.add(event);
			return this;
		}

		public Builder change(WindowingEvent... events) {
			this.changes.addAll(Arrays.asList(events));
			return this;
		}
		
		public Builder changes(Collection<WindowingEvent> changes) {
			Preconditions.checkArgument(changes != null, "changes must not be null");
			this.changes.addAll(changes);
			return this;
		}

		@Override
		public Builder addition(WindowingEvent event) {
			this.additions.add(event);
			return this;
		}
		
		public Builder addition(WindowingEvent... events) {
			this.additions.addAll(Arrays.asList(events));
			return this;
		}
		
		public Builder additions(Collection<WindowingEvent> additions) {
			Preconditions.checkArgument(additions != null, "additions must not be null");
			this.additions.addAll(additions);
			return this;
		}
		
		public Builder merge(WindowingEventChanges changes) {
			additions(changes.additions());
			changes(changes.changes());
			deletions(changes.deletions());
			return this;
		}
		
		public int sumOfChanges() {
			return additions.build().size() + changes.build().size() + deletions.build().size();
		}

		@Override
		public WindowingEventChanges build() {
			return new WindowingEventChanges(deletions.build(), changes.build(), additions.build());
		}
	}
	
	private final Set<WindowingEvent> deletions;
	private final Set<WindowingEvent> changes;
	private final Set<WindowingEvent> additions;
	
	private WindowingEventChanges(Set<WindowingEvent> deletions, Set<WindowingEvent> changes, Set<WindowingEvent> additions) {
		this.deletions = deletions;
		this.changes = changes;
		this.additions = additions;
	}
	
	@Override
	public Set<WindowingEvent> deletions() {
		return deletions;
	}

	@Override
	public Set<WindowingEvent> changes() {
		return changes;
	}

	@Override
	public Set<WindowingEvent> additions() {
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
		if (obj instanceof WindowingEventChanges) {
			WindowingEventChanges other = (WindowingEventChanges) obj;
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
