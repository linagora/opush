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
package org.obm.push.mail.bean;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.ServerId;
import org.obm.push.mail.bean.Snapshot.Builder;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.utils.DateUtils;

import com.google.common.collect.ImmutableList;

public class SnapshotTest {
	
	@Test (expected=IllegalStateException.class)
	public void testNullFilterType() {
		Snapshot.builder().uidNext(15).build();
	}
	
	@Test (expected=IllegalStateException.class)
	public void testNullUidNext() {
		Snapshot.builder().filterType(FilterType.ALL_ITEMS).build();
	}
	
	@Test
	public void testBuilder() {
		FilterType filterType = FilterType.ONE_DAY_BACK;
		long uidNext = 2;
		
		long emailUID = 3;
		Email email = Email.builder()
				.uid(emailUID)
				.read(false)
				.date(DateUtils.getCurrentDate())
				.build();
		long emailUID2 = 4;
		Email email2 = Email.builder()
				.uid(emailUID2)
				.read(true)
				.date(DateUtils.getCurrentDate())
				.build();
		
		Snapshot snapshot = Snapshot.builder()
			.filterType(filterType)
			.uidNext(uidNext)
			.addEmail(email)
			.addEmail(email2)
			.build();
		
		assertThat(snapshot.getFilterType()).isEqualTo(filterType);
		assertThat(snapshot.getUidNext()).isEqualTo(uidNext);
		assertThat(snapshot.getEmails()).containsExactly(email, email2);
		assertThat(snapshot.getMessageSet().asDiscreteValues()).containsOnly(emailUID, emailUID2);
	}
	
	@Test
	public void testContainsAllEmptyArgument() {
		
		Snapshot snapshot = defaultSnapshotBuilder()
				.addEmail(Email.builder()
						.uid(1)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.build();
		assertThat(snapshot.containsAllIds(ImmutableList.<ServerId>of())).isTrue();
	}

	@Test(expected=NullPointerException.class)
	public void testContainsAllNullArgument() {
		
		Snapshot snapshot = defaultSnapshotBuilder()
				.addEmail(Email.builder()
						.uid(1)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.build();
		assertThat(snapshot.containsAllIds(null)).isTrue();
	}
		
	@Test
	public void testContainsAllMatchElement() {
		
		Snapshot snapshot = defaultSnapshotBuilder()
				.addEmail(Email.builder()
						.uid(1)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.build();
		assertThat(snapshot.containsAllIds(ImmutableList.of(CollectionId.of(1).serverId(1)))).isTrue();
	}
	
	@Test
	public void testContainsAllMatchElement2() {
		Snapshot snapshot = defaultSnapshotBuilder()
				.addEmail(Email.builder()
						.uid(1)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.addEmail(Email.builder()
						.uid(2)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.addEmail(Email.builder()
						.uid(3)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.addEmail(Email.builder()
						.uid(4)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.addEmail(Email.builder()
						.uid(223)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.build();
		CollectionId collectionId = CollectionId.of(1);
		assertThat(snapshot.containsAllIds(
				ImmutableList.of(collectionId.serverId(1), collectionId.serverId(2),
						collectionId.serverId(3), collectionId.serverId(4), collectionId.serverId(223)))).isTrue();
	}
	
	@Test
	public void testContainsAllDontMatchElement() {
		
		Snapshot snapshot = defaultSnapshotBuilder()
				.addEmail(Email.builder()
						.uid(1)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.build();
		assertThat(snapshot.containsAllIds(ImmutableList.of(CollectionId.of(1).serverId(2)))).isFalse();
	}
	
	@Test
	public void testContainsAllDontMatchElement2() {
		Snapshot snapshot = defaultSnapshotBuilder()
				.addEmail(Email.builder()
						.uid(1)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.addEmail(Email.builder()
						.uid(2)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.addEmail(Email.builder()
						.uid(3)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.addEmail(Email.builder()
						.uid(4)
						.read(true)
						.date(DateUtils.getCurrentDate())
						.build())
				.build();
		CollectionId collectionId = CollectionId.of(1);
		assertThat(snapshot.containsAllIds(
				ImmutableList.of(collectionId.serverId(1), collectionId.serverId(2),
						collectionId.serverId(3), collectionId.serverId(4), collectionId.serverId(5)))).isFalse();
	}
	
	private Builder defaultSnapshotBuilder() {
		return Snapshot.builder()
				.filterType(FilterType.ONE_DAY_BACK)
				.uidNext(2);
	}
}
