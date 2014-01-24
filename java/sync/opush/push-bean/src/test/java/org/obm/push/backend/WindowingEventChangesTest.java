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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.obm.push.backend.WindowingEventChanges.Builder;
import org.obm.push.bean.MSEvent;

import com.google.common.collect.ImmutableSet;

public class WindowingEventChangesTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void buildWithNullDeletions() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("must not be null");
		
		WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of())
			.additions(ImmutableSet.<WindowingEvent>of())
			.deletions(null)
			.build();
		
	}

	@Test
	public void buildWithNullChanges() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("must not be null");
		
		WindowingEventChanges.builder()
			.deletions(ImmutableSet.<WindowingEvent>of())
			.additions(ImmutableSet.<WindowingEvent>of())
			.changes(null)
			.build();
	}
	
	@Test
	public void buildWithNullAdditions() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("must not be null");
		
		WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of())
			.deletions(ImmutableSet.<WindowingEvent>of())
			.additions(null)
			.build();
	}

	@Test
	public void buildOneEventInEachCollection() {
		WindowingEvent change = WindowingEvent.builder().uid(1).msEvent(new MSEvent()).build();
		WindowingEvent deletion = WindowingEvent.builder().uid(2).msEvent(new MSEvent()).build();
		WindowingEvent addition = WindowingEvent.builder().uid(3).msEvent(new MSEvent()).build();
		WindowingEventChanges emailChanges = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.additions(ImmutableSet.<WindowingEvent>of(addition))
			.build();

		assertThat(emailChanges.changes()).containsOnly(change);
		assertThat(emailChanges.deletions()).containsOnly(deletion);
		assertThat(emailChanges.additions()).containsOnly(addition);
	}
	
	@Test
	public void mergeWithEmpty() {
		WindowingEvent change = WindowingEvent.builder().uid(1).msEvent(new MSEvent()).build();
		WindowingEvent change2 = WindowingEvent.builder().uid(4).msEvent(new MSEvent()).build();
		WindowingEvent deletion = WindowingEvent.builder().uid(2).msEvent(new MSEvent()).build();
		WindowingEvent addition = WindowingEvent.builder().uid(3).msEvent(new MSEvent()).build();
		Builder emailChanges = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change, change2))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.additions(ImmutableSet.<WindowingEvent>of(addition));
		
		assertThat(emailChanges.merge(WindowingEventChanges.empty()).build()).isEqualTo(emailChanges.build());
	}

	@Test
	public void mergeFromEmpty() {
		WindowingEvent change = WindowingEvent.builder().uid(1).msEvent(new MSEvent()).build();
		WindowingEvent change2 = WindowingEvent.builder().uid(4).msEvent(new MSEvent()).build();
		WindowingEvent deletion = WindowingEvent.builder().uid(2).msEvent(new MSEvent()).build();
		WindowingEvent addition = WindowingEvent.builder().uid(3).msEvent(new MSEvent()).build();
		WindowingEventChanges emailChanges = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change, change2))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.additions(ImmutableSet.<WindowingEvent>of(addition))
			.build();
		
		Builder empty = WindowingEventChanges.builder();
		
		assertThat(empty.merge(emailChanges).build()).isEqualTo(emailChanges);
	}
	
	@Test
	public void merge() {
		WindowingEvent change = WindowingEvent.builder().uid(1).msEvent(new MSEvent()).build();
		WindowingEvent deletion = WindowingEvent.builder().uid(2).msEvent(new MSEvent()).build();
		WindowingEvent addition = WindowingEvent.builder().uid(3).msEvent(new MSEvent()).build();
		Builder emailChanges = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.additions(ImmutableSet.<WindowingEvent>of(addition));
		
		WindowingEvent change2 = WindowingEvent.builder().uid(4).msEvent(new MSEvent()).build();
		WindowingEvent deletion2 = WindowingEvent.builder().uid(5).msEvent(new MSEvent()).build();
		WindowingEvent addition2 = WindowingEvent.builder().uid(6).msEvent(new MSEvent()).build();
		WindowingEventChanges emailChanges2 = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change2))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion2))
			.additions(ImmutableSet.<WindowingEvent>of(addition2))
			.build();
		
		WindowingEventChanges merged = emailChanges.merge(emailChanges2).build();
		
		assertThat(merged.changes()).containsOnly(change, change2);
		assertThat(merged.deletions()).containsOnly(deletion, deletion2);
		assertThat(merged.additions()).containsOnly(addition, addition2);
	}

	@Test
	public void mergeWithDuplicates() {
		WindowingEvent change = WindowingEvent.builder().uid(1).msEvent(new MSEvent()).build();
		WindowingEvent deletion = WindowingEvent.builder().uid(2).msEvent(new MSEvent()).build();
		WindowingEvent addition = WindowingEvent.builder().uid(3).msEvent(new MSEvent()).build();
		Builder emailChanges = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.additions(ImmutableSet.<WindowingEvent>of(addition));
		
		WindowingEvent change2 = WindowingEvent.builder().uid(4).msEvent(new MSEvent()).build();
		WindowingEventChanges emailChanges2 = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change2))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.additions(ImmutableSet.<WindowingEvent>of(addition))
			.build();
		
		WindowingEventChanges merged = emailChanges.merge(emailChanges2).build();
		
		assertThat(merged.sumOfChanges()).isEqualTo(4);
		assertThat(merged.changes()).containsOnly(change, change2);
		assertThat(merged.deletions()).containsOnly(deletion);
		assertThat(merged.additions()).containsOnly(addition);
	}
	
	@Test
	public void sumOfChangesOnEmpty() {
		assertThat(WindowingEventChanges.builder().sumOfChanges()).isEqualTo(0);
	}
	
	@Test
	public void sumOfChanges() {
		WindowingEvent change = WindowingEvent.builder().uid(1).msEvent(new MSEvent()).build();
		WindowingEvent deletion = WindowingEvent.builder().uid(2).msEvent(new MSEvent()).build();
		WindowingEvent addition = WindowingEvent.builder().uid(3).msEvent(new MSEvent()).build();
		Builder emailChanges = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.additions(ImmutableSet.<WindowingEvent>of(addition));
		
		assertThat(emailChanges.sumOfChanges()).isEqualTo(3);
	}
	
	@Test
	public void sumOfChangesWithDuplicates() {
		WindowingEvent change = WindowingEvent.builder().uid(1).msEvent(new MSEvent()).build();
		WindowingEvent deletion = WindowingEvent.builder().uid(2).msEvent(new MSEvent()).build();
		WindowingEvent addition = WindowingEvent.builder().uid(3).msEvent(new MSEvent()).build();
		Builder emailChanges = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change))
			.changes(ImmutableSet.<WindowingEvent>of(change))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.additions(ImmutableSet.<WindowingEvent>of(addition))
			.additions(ImmutableSet.<WindowingEvent>of(addition));
		
		assertThat(emailChanges.sumOfChanges()).isEqualTo(3);
	}
	
	@Test
	public void sumOfChangesWithDuplicatesInMerge() {
		WindowingEvent change = WindowingEvent.builder().uid(1).msEvent(new MSEvent()).build();
		WindowingEvent deletion = WindowingEvent.builder().uid(2).msEvent(new MSEvent()).build();
		WindowingEvent addition = WindowingEvent.builder().uid(3).msEvent(new MSEvent()).build();
		Builder emailChanges = WindowingEventChanges.builder()
			.changes(ImmutableSet.<WindowingEvent>of(change))
			.deletions(ImmutableSet.<WindowingEvent>of(deletion))
			.additions(ImmutableSet.<WindowingEvent>of(addition))
			.merge(WindowingEventChanges.builder()
				.changes(ImmutableSet.<WindowingEvent>of(change))
				.deletions(ImmutableSet.<WindowingEvent>of(deletion))
				.additions(ImmutableSet.<WindowingEvent>of(addition))
				.build());
			
		assertThat(emailChanges.sumOfChanges()).isEqualTo(3);
	}
}
