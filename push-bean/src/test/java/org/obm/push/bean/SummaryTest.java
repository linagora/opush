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

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;


public class SummaryTest {


	@Test
	public void builderChangesNotSet() {
		Summary summary = Summary.builder()
			.deletions(1)
			.fetchs(1)
			.build();
		assertThat(summary.getChanges()).isEqualTo(0);
		assertThat(summary.getDeletions()).isEqualTo(1);
		assertThat(summary.getFetchs()).isEqualTo(1);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void builderChangesNegative() {
		Summary.builder()
			.changes(-1);
	}

	@Test
	public void builderDeletionsNotSet() {
		Summary summary = Summary.builder()
			.changes(1)
			.fetchs(1)
			.build();
		assertThat(summary.getChanges()).isEqualTo(1);
		assertThat(summary.getDeletions()).isEqualTo(0);
		assertThat(summary.getFetchs()).isEqualTo(1);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void builderDeletionsNegative() {
		Summary.builder()
			.deletions(-1);
	}

	@Test
	public void builderFetchsNotSet() {
		Summary summary = Summary.builder()
			.deletions(1)
			.changes(1)
			.build();
		assertThat(summary.getChanges()).isEqualTo(1);
		assertThat(summary.getDeletions()).isEqualTo(1);
		assertThat(summary.getFetchs()).isEqualTo(0);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void builderFetchsNegative() {
		Summary.builder()
			.fetchs(-1);
	}

	@Test
	public void builder() {
		Summary summary = Summary.builder()
			.changes(1)
			.deletions(2)
			.fetchs(3)
			.build();

		assertThat(summary.getChanges()).isEqualTo(1);
		assertThat(summary.getDeletions()).isEqualTo(2);
		assertThat(summary.getFetchs()).isEqualTo(3);
	}
	
	@Test
	public void testEmptySummaryPrint() {
		Summary summary = Summary.empty();
		assertThat(summary.summary()).isEqualTo("CHANGE: 0, DELETE: 0, FETCH: 0");
	}
	
	@Test
	public void testSummaryPrint() {
		Summary summary = Summary.builder()
				.changes(1)
				.deletions(2)
				.fetchs(3)
				.build();

		assertThat(summary.summary()).isEqualTo("CHANGE: 1, DELETE: 2, FETCH: 3");
	}
}
