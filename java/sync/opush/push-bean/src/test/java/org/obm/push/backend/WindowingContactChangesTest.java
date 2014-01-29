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
import org.obm.push.backend.WindowingContactChanges.Builder;
import org.obm.push.bean.MSContact;

import com.google.common.collect.ImmutableSet;

public class WindowingContactChangesTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void buildWithNullDeletions() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("must not be null");
		
		WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of())
			.additions(ImmutableSet.<WindowingContact>of())
			.deletions(null)
			.build();
		
	}

	@Test
	public void buildWithNullChanges() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("must not be null");
		
		WindowingContactChanges.builder()
			.deletions(ImmutableSet.<WindowingContact>of())
			.additions(ImmutableSet.<WindowingContact>of())
			.changes(null)
			.build();
	}
	
	@Test
	public void buildWithNullAdditions() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("must not be null");
		
		WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of())
			.deletions(ImmutableSet.<WindowingContact>of())
			.additions(null)
			.build();
	}

	@Test
	public void buildOneContactInEachCollection() {
		WindowingContact change = WindowingContact.builder().uid(1).applicationData(new MSContact()).build();
		WindowingContact deletion = WindowingContact.builder().uid(2).applicationData(new MSContact()).build();
		WindowingContact addition = WindowingContact.builder().uid(3).applicationData(new MSContact()).build();
		WindowingContactChanges emailChanges = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.additions(ImmutableSet.<WindowingContact>of(addition))
			.build();

		assertThat(emailChanges.changes()).containsOnly(change);
		assertThat(emailChanges.deletions()).containsOnly(deletion);
		assertThat(emailChanges.additions()).containsOnly(addition);
	}
	
	@Test
	public void mergeWithEmpty() {
		WindowingContact change = WindowingContact.builder().uid(1).applicationData(new MSContact()).build();
		WindowingContact change2 = WindowingContact.builder().uid(4).applicationData(new MSContact()).build();
		WindowingContact deletion = WindowingContact.builder().uid(2).applicationData(new MSContact()).build();
		WindowingContact addition = WindowingContact.builder().uid(3).applicationData(new MSContact()).build();
		Builder emailChanges = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change, change2))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.additions(ImmutableSet.<WindowingContact>of(addition));
		
		assertThat(emailChanges.merge(WindowingContactChanges.empty()).build()).isEqualTo(emailChanges.build());
	}

	@Test
	public void mergeFromEmpty() {
		WindowingContact change = WindowingContact.builder().uid(1).applicationData(new MSContact()).build();
		WindowingContact change2 = WindowingContact.builder().uid(4).applicationData(new MSContact()).build();
		WindowingContact deletion = WindowingContact.builder().uid(2).applicationData(new MSContact()).build();
		WindowingContact addition = WindowingContact.builder().uid(3).applicationData(new MSContact()).build();
		WindowingContactChanges emailChanges = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change, change2))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.additions(ImmutableSet.<WindowingContact>of(addition))
			.build();
		
		Builder empty = WindowingContactChanges.builder();
		
		assertThat(empty.merge(emailChanges).build()).isEqualTo(emailChanges);
	}
	
	@Test
	public void merge() {
		WindowingContact change = WindowingContact.builder().uid(1).applicationData(new MSContact()).build();
		WindowingContact deletion = WindowingContact.builder().uid(2).applicationData(new MSContact()).build();
		WindowingContact addition = WindowingContact.builder().uid(3).applicationData(new MSContact()).build();
		Builder emailChanges = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.additions(ImmutableSet.<WindowingContact>of(addition));
		
		WindowingContact change2 = WindowingContact.builder().uid(4).applicationData(new MSContact()).build();
		WindowingContact deletion2 = WindowingContact.builder().uid(5).applicationData(new MSContact()).build();
		WindowingContact addition2 = WindowingContact.builder().uid(6).applicationData(new MSContact()).build();
		WindowingContactChanges emailChanges2 = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change2))
			.deletions(ImmutableSet.<WindowingContact>of(deletion2))
			.additions(ImmutableSet.<WindowingContact>of(addition2))
			.build();
		
		WindowingContactChanges merged = emailChanges.merge(emailChanges2).build();
		
		assertThat(merged.changes()).containsOnly(change, change2);
		assertThat(merged.deletions()).containsOnly(deletion, deletion2);
		assertThat(merged.additions()).containsOnly(addition, addition2);
	}

	@Test
	public void mergeWithDuplicates() {
		WindowingContact change = WindowingContact.builder().uid(1).applicationData(new MSContact()).build();
		WindowingContact deletion = WindowingContact.builder().uid(2).applicationData(new MSContact()).build();
		WindowingContact addition = WindowingContact.builder().uid(3).applicationData(new MSContact()).build();
		Builder emailChanges = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.additions(ImmutableSet.<WindowingContact>of(addition));
		
		WindowingContact change2 = WindowingContact.builder().uid(4).applicationData(new MSContact()).build();
		WindowingContactChanges emailChanges2 = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change2))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.additions(ImmutableSet.<WindowingContact>of(addition))
			.build();
		
		WindowingContactChanges merged = emailChanges.merge(emailChanges2).build();
		
		assertThat(merged.sumOfChanges()).isEqualTo(4);
		assertThat(merged.changes()).containsOnly(change, change2);
		assertThat(merged.deletions()).containsOnly(deletion);
		assertThat(merged.additions()).containsOnly(addition);
	}
	
	@Test
	public void sumOfChangesOnEmpty() {
		assertThat(WindowingContactChanges.builder().sumOfChanges()).isEqualTo(0);
	}
	
	@Test
	public void sumOfChanges() {
		WindowingContact change = WindowingContact.builder().uid(1).applicationData(new MSContact()).build();
		WindowingContact deletion = WindowingContact.builder().uid(2).applicationData(new MSContact()).build();
		WindowingContact addition = WindowingContact.builder().uid(3).applicationData(new MSContact()).build();
		Builder emailChanges = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.additions(ImmutableSet.<WindowingContact>of(addition));
		
		assertThat(emailChanges.sumOfChanges()).isEqualTo(3);
	}
	
	@Test
	public void sumOfChangesWithDuplicates() {
		WindowingContact change = WindowingContact.builder().uid(1).applicationData(new MSContact()).build();
		WindowingContact deletion = WindowingContact.builder().uid(2).applicationData(new MSContact()).build();
		WindowingContact addition = WindowingContact.builder().uid(3).applicationData(new MSContact()).build();
		Builder emailChanges = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change))
			.changes(ImmutableSet.<WindowingContact>of(change))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.additions(ImmutableSet.<WindowingContact>of(addition))
			.additions(ImmutableSet.<WindowingContact>of(addition));
		
		assertThat(emailChanges.sumOfChanges()).isEqualTo(3);
	}
	
	@Test
	public void sumOfChangesWithDuplicatesInMerge() {
		WindowingContact change = WindowingContact.builder().uid(1).applicationData(new MSContact()).build();
		WindowingContact deletion = WindowingContact.builder().uid(2).applicationData(new MSContact()).build();
		WindowingContact addition = WindowingContact.builder().uid(3).applicationData(new MSContact()).build();
		Builder emailChanges = WindowingContactChanges.builder()
			.changes(ImmutableSet.<WindowingContact>of(change))
			.deletions(ImmutableSet.<WindowingContact>of(deletion))
			.additions(ImmutableSet.<WindowingContact>of(addition))
			.merge(WindowingContactChanges.builder()
				.changes(ImmutableSet.<WindowingContact>of(change))
				.deletions(ImmutableSet.<WindowingContact>of(deletion))
				.additions(ImmutableSet.<WindowingContact>of(addition))
				.build());
			
		assertThat(emailChanges.sumOfChanges()).isEqualTo(3);
	}
}
