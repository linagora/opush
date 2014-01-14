/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013-2014  Linagora
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
package org.obm.push.dao.testsuite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.guice.GuiceRunner;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.mail.EmailChanges;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.WindowingIndexKey;
import org.obm.push.store.WindowingDao;

import com.google.common.base.Function;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Range;

@RunWith(GuiceRunner.class)
public abstract class WindowingDaoTest {

	protected int collectionId;
	protected DeviceId deviceId;
	protected User user;

	protected WindowingDao testee;

	@Before
	public void setup() {
		collectionId = 5;
		deviceId = new DeviceId("ab123");
		user = Factory.create().createUser("user@domain", "user@domain", "user@domain");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void popNextPendingElementsNullKey() {
		WindowingIndexKey key = null;
		SyncKey syncKey = new SyncKey("123");
		int expectedSize = 12;

		testee.popNextPendingElements(key, expectedSize, syncKey);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void popNextPendingElementsZeroExpectedSize() {
		WindowingIndexKey key = new WindowingIndexKey(user, deviceId, collectionId);
		SyncKey syncKey = new SyncKey("123");
		int expectedSize = 0;
		testee.popNextPendingElements(key, expectedSize, syncKey);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void popNextPendingElementsNegativeExpectedSize() {
		WindowingIndexKey key = new WindowingIndexKey(user, deviceId, collectionId);
		SyncKey syncKey = new SyncKey("123");
		int expectedSize = -5;
		testee.popNextPendingElements(key, expectedSize, syncKey);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void popNextPendingElementsNullSyncKey() {
		WindowingIndexKey key = new WindowingIndexKey(user, deviceId, collectionId);
		SyncKey syncKey = null;
		int expectedSize = 45;
		testee.popNextPendingElements(key, expectedSize, syncKey);
	}
	
	@Test
	public void popNextPendingElementsEmpty() {
		WindowingIndexKey key = new WindowingIndexKey(user, deviceId, collectionId);
		SyncKey syncKey = new SyncKey("123");
		int expectedSize = 12;

		EmailChanges elements = testee.popNextPendingElements(key, expectedSize, syncKey);

		assertThat(elements.sumOfChanges()).isEqualTo(0);
	}
	
	@Test
	public void popNextPendingElementsFewElements() {
		WindowingIndexKey key = new WindowingIndexKey(user, deviceId, collectionId);
		SyncKey syncKey = new SyncKey("123");
		int expectedSize = 12;
		EmailChanges emailChanges = generateEmails(2);
		testee.pushPendingElements(key, syncKey, emailChanges, 2);

		EmailChanges elements = testee.popNextPendingElements(key, expectedSize, syncKey);

		assertThat(elements.sumOfChanges()).isEqualTo(2);
	}
	
	@Test
	public void popNextPendingElementsEnoughElements() {
		WindowingIndexKey key = new WindowingIndexKey(user, deviceId, collectionId);
		SyncKey syncKey = new SyncKey("123");
		int expectedSize = 2;
		EmailChanges emailChanges = generateEmails(2);
		testee.pushPendingElements(key, syncKey, emailChanges, 2);

		EmailChanges elements = testee.popNextPendingElements(key, expectedSize, syncKey);
		
		assertThat(elements.sumOfChanges()).isEqualTo(2);
	}

	@Test
	public void hasPendingElementsNoIndex() {
		WindowingIndexKey key = new WindowingIndexKey(user, deviceId, collectionId);
		SyncKey syncKey = new SyncKey("123");

		boolean hasPendingElements = testee.hasPendingElements(key, syncKey);
		
		assertThat(hasPendingElements).isFalse();
	}

	@Test
	public void hasPendingElementsSameIndexSyncKey() {
		WindowingIndexKey key = new WindowingIndexKey(user, deviceId, collectionId);
		SyncKey requestSyncKey = new SyncKey("123");
		EmailChanges emailChanges = generateEmails(1);
		testee.pushPendingElements(key, requestSyncKey, emailChanges, 2);
		
		boolean hasPendingElements = testee.hasPendingElements(key, requestSyncKey);
		
		assertThat(hasPendingElements).isTrue();
	}
	
	@Test
	public void hasPendingElementsDifferentIndexSyncKey() {
		WindowingIndexKey key = new WindowingIndexKey(user, deviceId, collectionId);
		SyncKey requestSyncKey = new SyncKey("123");

		boolean hasPendingElements = testee.hasPendingElements(key, requestSyncKey);
		
		assertThat(hasPendingElements).isFalse();
	}

	private EmailChanges generateEmails(long number) {
		return generateEmails(0, number);
	}
	
	private EmailChanges generateEmails(long start, long number) {
		return EmailChanges.builder()
				.additions(
					FluentIterable.from(ContiguousSet.create(Range.closedOpen(start, start + number), DiscreteDomain.longs()))
						.transform(new Function<Long, Email>() {
							@Override
							public Email apply(Long uid) {
								return Email.builder().uid(uid).build();
							}
						}).toSet())
				.build();
	}
}
