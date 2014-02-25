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
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.change.WindowingChangesBuilder;
import org.obm.push.bean.change.WindowingKey;
import org.obm.push.mail.EmailChanges;
import org.obm.push.mail.EmailChanges.Builder;
import org.obm.push.mail.bean.Email;
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
	public void popNextChangesNullBuilder() {
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		int expectedSize = 12;
		Builder builder = null;
		
		testee.popNextChanges(key, expectedSize, syncKey, builder);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void popNextChangesNullKey() {
		WindowingKey key = null;
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		int expectedSize = 12;

		testee.popNextChanges(key, expectedSize, syncKey, EmailChanges.builder());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void popNextChangesZeroExpectedSize() {
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		int expectedSize = 0;

		testee.popNextChanges(key, expectedSize, syncKey, EmailChanges.builder());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void popNextChangesNegativeExpectedSize() {
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		int expectedSize = -5;

		testee.popNextChanges(key, expectedSize, syncKey, EmailChanges.builder());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void popNextChangesNullSyncKey() {
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1"));
		SyncKey syncKey = null;
		int expectedSize = 45;

		testee.popNextChanges(key, expectedSize, syncKey, EmailChanges.builder());
	}
	
	@Test
	public void popNextChangesEmpty() {
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		int expectedSize = 12;
		EmailChanges.Builder givenBuilder = EmailChanges.builder();

		WindowingChangesBuilder<Email> resultBuilder = 
				testee.popNextChanges(key, expectedSize, syncKey, givenBuilder);

		assertThat(resultBuilder.build().sumOfChanges()).isEqualTo(0);
	}
	
	@Test
	public void popNextChangesFewElements() {
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		int expectedSize = 12;
		EmailChanges emailChanges = generateEmails(2);
		testee.pushPendingChanges(key, syncKey, emailChanges, PIMDataType.EMAIL, 2);
		EmailChanges.Builder givenBuilder = EmailChanges.builder();

		WindowingChangesBuilder<Email> resultBuilder = 
				testee.popNextChanges(key, expectedSize, syncKey, givenBuilder);

		assertThat(resultBuilder.build().sumOfChanges()).isEqualTo(2);
	}
	
	@Test
	public void popNextChangesEnoughElements() {
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		int expectedSize = 2;
		EmailChanges emailChanges = generateEmails(2);
		testee.pushPendingChanges(key, syncKey, emailChanges, PIMDataType.EMAIL, 2);
		EmailChanges.Builder givenBuilder = EmailChanges.builder();

		WindowingChangesBuilder<Email> resultBuilder = 
				testee.popNextChanges(key, expectedSize, syncKey, givenBuilder);

		assertThat(resultBuilder.build().sumOfChanges()).isEqualTo(2);
	}

	@Test
	public void hasPendingElementsNoIndex() {
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);

		boolean hasPendingElements = testee.hasPendingChanges(key);
		
		assertThat(hasPendingElements).isFalse();
	}

	@Test
	public void hasPendingElementsSameIndexSyncKey() {
		SyncKey requestSyncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, requestSyncKey);
		EmailChanges emailChanges = generateEmails(1);
		testee.pushPendingChanges(key, requestSyncKey, emailChanges, PIMDataType.EMAIL, 2);
		
		boolean hasPendingElements = testee.hasPendingChanges(key);
		
		assertThat(hasPendingElements).isTrue();
	}
	
	@Test
	public void hasPendingElementsWhenHasToConsume() {
		int expectedSize = 1;
		EmailChanges emailChanges = generateEmails(3);
		
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		SyncKey syncKey2 = new SyncKey("64dd1fc0-3519-480a-850f-b84c0153855d");
		SyncKey syncKey3 = new SyncKey("720fc208-1e70-43a1-bfad-112d64548c7b");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		WindowingKey key2 = new WindowingKey(user, deviceId, collectionId, syncKey2);
		WindowingKey key3 = new WindowingKey(user, deviceId, collectionId, syncKey3);
		testee.pushPendingChanges(key, syncKey2, emailChanges, PIMDataType.EMAIL, 3);

		WindowingChangesBuilder<Email> changes1 = testee.popNextChanges(key, expectedSize, syncKey2, EmailChanges.builder());
		WindowingChangesBuilder<Email> changes2 = testee.popNextChanges(key2, expectedSize, syncKey3, EmailChanges.builder());
		boolean hasPendingElements = testee.hasPendingChanges(key3);

		assertThat(changes1.build().sumOfChanges()).isEqualTo(expectedSize);
		assertThat(changes2.build().sumOfChanges()).isEqualTo(expectedSize);
		assertThat(hasPendingElements).isTrue();
	}
	
	@Test
	public void hasNoPendingElementsWhenAllConsumed() {
		int expectedSize = 1;
		EmailChanges emailChanges = generateEmails(2);
		
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		SyncKey syncKey2 = new SyncKey("64dd1fc0-3519-480a-850f-b84c0153855d");
		SyncKey syncKey3 = new SyncKey("720fc208-1e70-43a1-bfad-112d64548c7b");
		SyncKey syncKey4 = new SyncKey("7d813223-dee0-4344-b78b-931373cd4507");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		WindowingKey key2 = new WindowingKey(user, deviceId, collectionId, syncKey2);
		WindowingKey key3 = new WindowingKey(user, deviceId, collectionId, syncKey3);
		testee.pushPendingChanges(key, syncKey2, emailChanges, PIMDataType.EMAIL, 2);
		
		WindowingChangesBuilder<Email> changes1 = testee.popNextChanges(key, expectedSize, syncKey2, EmailChanges.builder());
		WindowingChangesBuilder<Email> changes2 = testee.popNextChanges(key2, expectedSize, syncKey3, EmailChanges.builder());
		WindowingChangesBuilder<Email> changes3 = testee.popNextChanges(key3, expectedSize, syncKey4, EmailChanges.builder());
		boolean hasPendingElements = testee.hasPendingChanges(key3);

		assertThat(changes1.build().sumOfChanges()).isEqualTo(expectedSize);
		assertThat(changes2.build().sumOfChanges()).isEqualTo(expectedSize);
		assertThat(changes3.build().sumOfChanges()).isZero();
		assertThat(hasPendingElements).isFalse();
	}
	
	@Test
	public void hasPendingElementsWhenAskingWithPreviousSyncKey() {
		int expectedSize = 1;
		EmailChanges emailChanges = generateEmails(2);
		
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		SyncKey syncKey2 = new SyncKey("64dd1fc0-3519-480a-850f-b84c0153855d");
		SyncKey syncKey3 = new SyncKey("720fc208-1e70-43a1-bfad-112d64548c7b");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		WindowingKey key2 = new WindowingKey(user, deviceId, collectionId, syncKey2);
		testee.pushPendingChanges(key, syncKey2, emailChanges, PIMDataType.EMAIL, 2);

		
		WindowingChangesBuilder<Email> firstChangesOfKey = testee.popNextChanges(key, expectedSize, syncKey2, EmailChanges.builder());
		WindowingChangesBuilder<Email> firstChangesOfKey2 = testee.popNextChanges(key2, expectedSize, syncKey3, EmailChanges.builder());
		WindowingChangesBuilder<Email> secondChangesOfKey = testee.popNextChanges(key, expectedSize, syncKey2, EmailChanges.builder());
		boolean hasPendingElements = testee.hasPendingChanges(key2);

		assertThat(firstChangesOfKey.build().sumOfChanges()).isEqualTo(expectedSize);
		assertThat(firstChangesOfKey2.build().sumOfChanges()).isEqualTo(expectedSize);
		assertThat(firstChangesOfKey.build()).isEqualTo(secondChangesOfKey.build());
		assertThat(hasPendingElements).isTrue();
	}
	
	@Test
	public void oneElementForEachKind() {
		int expectedSize = 3;
		EmailChanges emailChanges = EmailChanges.builder()
						.addition(Email.builder().uid(1).build())
						.change(Email.builder().uid(2).build())
						.deletion(Email.builder().uid(2).build())
						.build();
		
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		SyncKey syncKey2 = new SyncKey("64dd1fc0-3519-480a-850f-b84c0153855d");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		testee.pushPendingChanges(key, syncKey2, emailChanges, PIMDataType.EMAIL, 3);

		WindowingChangesBuilder<Email> changes = testee.popNextChanges(key, expectedSize, syncKey2, EmailChanges.builder());
		assertThat(changes.build().sumOfChanges()).isEqualTo(expectedSize);
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
	
	@Test
	public void countElementsWhenNoElements() {
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		assertThat(testee.countPendingChanges(key)).isEqualTo(0);
	}
	
	@Test
	public void countElements() {
		int numberOfElements = 2;
		SyncKey syncKey = new SyncKey("e05fe721-adf6-416d-a2d9-657347096aa1");
		WindowingKey key = new WindowingKey(user, deviceId, collectionId, syncKey);
		EmailChanges emailChanges = generateEmails(numberOfElements);
		testee.pushPendingChanges(key, syncKey, emailChanges, PIMDataType.EMAIL, numberOfElements);
		
		assertThat(testee.countPendingChanges(key)).isEqualTo(numberOfElements);
	}
}
