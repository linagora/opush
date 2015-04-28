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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;


public class SyncCollectionOptionsTest {

	@Test
	public void testCloneOnlyByExistingFieldsWhenFullOptions() {
		List<BodyPreference> bodyPreferences = ImmutableList.of(
			BodyPreference.builder()
				.bodyType(MSEmailBodyType.HTML)
				.truncationSize(100)
				.allOrNone(true)
				.build(),
			BodyPreference.builder()
				.bodyType(MSEmailBodyType.PlainText)
				.truncationSize(1000)
				.allOrNone(false)
				.build());

		SyncCollectionOptions cloningFromOptions = SyncCollectionOptions.builder()
				.conflict(1)
				.deletesAsMoves(true)
				.filterType(FilterType.ONE_MONTHS_BACK)
				.mimeSupport(1)
				.mimeTruncation(2)
				.truncation(100)
				.bodyPreferences(bodyPreferences)
				.build();
		
		SyncCollectionOptions cloned = SyncCollectionOptions.cloneOnlyByExistingFields(cloningFromOptions);
		
		assertThat(cloned).isEqualTo(cloningFromOptions);
	}

	@Test
	public void testCloneOnlyByExistingFieldsWhenNoConflict() {
		SyncCollectionOptions cloningFromOptions = SyncCollectionOptions.builder()
				.conflict(null)
				.build();
		
		SyncCollectionOptions cloned = SyncCollectionOptions.cloneOnlyByExistingFields(cloningFromOptions);
		
		assertThat(cloned.getConflict()).isEqualTo(1);
	}

	@Test
	public void testCloneOnlyByExistingFieldsWhenNoDeleteAsMoved() {
		SyncCollectionOptions cloningFromOptions = SyncCollectionOptions.builder()
				.deletesAsMoves(null)
				.build();
		
		SyncCollectionOptions cloned = SyncCollectionOptions.cloneOnlyByExistingFields(cloningFromOptions);
		
		assertThat(cloned.isDeletesAsMoves()).isEqualTo(true);
	}

	@Test
	public void testCloneOnlyByExistingFieldsWhenNoFilterType() {
		SyncCollectionOptions cloningFromOptions = SyncCollectionOptions.builder()
				.filterType(null)
				.build();
		
		SyncCollectionOptions cloned = SyncCollectionOptions.cloneOnlyByExistingFields(cloningFromOptions);
		
		assertThat(cloned.getFilterType()).isEqualTo(FilterType.DEFAULT);
	}

	@Test
	public void testCloneOnlyByExistingFieldsWhenNoMimeSupport() {
		SyncCollectionOptions cloningFromOptions = SyncCollectionOptions.builder()
				.mimeSupport(null)
				.build();
		
		SyncCollectionOptions cloned = SyncCollectionOptions.cloneOnlyByExistingFields(cloningFromOptions);
		
		assertThat(cloned.getMimeSupport()).isAbsent();
	}

	@Test
	public void testCloneOnlyByExistingFieldsWhenNoMimeTruncation() {
		SyncCollectionOptions cloningFromOptions = SyncCollectionOptions.builder()
				.mimeTruncation(null)
				.build();
		
		SyncCollectionOptions cloned = SyncCollectionOptions.cloneOnlyByExistingFields(cloningFromOptions);
		
		assertThat(cloned.getMimeTruncation()).isNull();
	}

	@Test
	public void testCloneOnlyByExistingFieldsWhenNoTruncation() {
		SyncCollectionOptions cloningFromOptions = SyncCollectionOptions.builder()
				.truncation(null)
				.build();
		
		SyncCollectionOptions cloned = SyncCollectionOptions.cloneOnlyByExistingFields(cloningFromOptions);
		
		assertThat(cloned.getTruncation()).isEqualTo(9);
	}

	@Test
	public void testCloneOnlyByExistingFieldsWhenNoBodyPreference() {
		SyncCollectionOptions cloningFromOptions = SyncCollectionOptions.builder()
				.bodyPreferences(null)
				.build();
		
		SyncCollectionOptions cloned = SyncCollectionOptions.cloneOnlyByExistingFields(cloningFromOptions);
		
		assertThat(cloned.getBodyPreferences()).isEmpty();
	}

	@Test
	public void testCloneOnlyByExistingFieldsWhenEmptyBodyPreference() {
		SyncCollectionOptions cloningFromOptions = SyncCollectionOptions.builder()
				.bodyPreferences(ImmutableList.<BodyPreference>of())
				.build();
		
		SyncCollectionOptions cloned = SyncCollectionOptions.cloneOnlyByExistingFields(cloningFromOptions);
		
		assertThat(cloned.getBodyPreferences()).isEmpty();
	}
	
	@Test
	public void testDefaultOptions() {
		SyncCollectionOptions defaultOptions = SyncCollectionOptions.defaultOptions();
		assertThat(defaultOptions.getConflict()).isEqualTo(1);
		assertThat(defaultOptions.getFilterType()).isEqualTo(FilterType.DEFAULT);
		assertThat(defaultOptions.getMimeSupport()).isAbsent();
		assertThat(defaultOptions.getMimeTruncation()).isNull();
		assertThat(defaultOptions.getTruncation()).isEqualTo(9);
		assertThat(defaultOptions.getBodyPreferences()).isEmpty();
	}
}
