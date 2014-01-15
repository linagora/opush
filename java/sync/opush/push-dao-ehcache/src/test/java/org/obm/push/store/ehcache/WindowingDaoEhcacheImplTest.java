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
package org.obm.push.store.ehcache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createMock;

import java.io.IOException;
import java.util.Properties;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.obm.annotations.transactional.TransactionProvider;
import org.obm.push.configuration.OpushConfiguration;
import org.obm.push.ProtocolVersion;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.mail.EmailChanges;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.WindowingIndexKey;
import org.obm.transaction.TransactionManagerRule;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Range;

public class WindowingDaoEhcacheImplTest {

	@Rule public TemporaryFolder tempFolder =  new TemporaryFolder();
	@Rule public TransactionManagerRule transactionManagerRule = new TransactionManagerRule();
	
	private WindowingDaoEhcacheImpl.PartitionDao testee;
	private ObjectStoreManager objectStoreManager;

	private User user;
	private Device device;

	@Before
	public void init() throws NotSupportedException, SystemException, IOException {
		user = Factory.create().createUser("login@domain", "email@domain", "displayName");
		device = new Device(1, "devType", new DeviceId("devId"), new Properties(), ProtocolVersion.V121);
		
		Logger logger = EasyMock.createNiceMock(Logger.class);
		TransactionProvider transactionProvider = EasyMock.createNiceMock(TransactionProvider.class);
		OpushConfiguration opushConfiguration = new EhCacheOpushConfiguration().mock(tempFolder);

		TestingEhCacheConfiguration config = new TestingEhCacheConfiguration();
		objectStoreManager = new ObjectStoreManager(opushConfiguration, config, logger, transactionProvider);
		CacheEvictionListener cacheEvictionListener = createMock(CacheEvictionListener.class);
		testee = new WindowingDaoEhcacheImpl.PartitionDao(objectStoreManager, cacheEvictionListener);
		
		transactionManagerRule.getTransactionManager().begin();
	}
	
	@After
	public void cleanup() throws IllegalStateException, SecurityException, SystemException {
		transactionManagerRule.getTransactionManager().rollback();
		objectStoreManager.shutdown();
	}
	
	@Test
	public void testGetWindowingSyncKeyOnEmptyStore() {
		WindowingIndexKey windowingIndexKey = new WindowingIndexKey(user, device.getDevId(), 1);
		SyncKey syncKey = testee.getWindowingSyncKey(windowingIndexKey);
		assertThat(syncKey).isNull();
	}
	
	@Test
	public void testGetWindowingSyncKey() {
		SyncKey expectedSyncKey = new SyncKey("123");
		WindowingIndexKey windowingIndexKey = new WindowingIndexKey(user, device.getDevId(), 1);
		testee.pushPendingElements(windowingIndexKey, expectedSyncKey, EmailChanges.builder().build());
		SyncKey syncKey = testee.getWindowingSyncKey(windowingIndexKey);
		assertThat(syncKey).isEqualTo(expectedSyncKey);
	}
	
	@Test
	public void testGetWindowingSyncKeyBadKey() {
		WindowingIndexKey windowingIndexKey = new WindowingIndexKey(user, device.getDevId(), 1);
		testee.pushPendingElements(windowingIndexKey, new SyncKey("123"), EmailChanges.builder().build());
		SyncKey syncKey = testee.getWindowingSyncKey(new WindowingIndexKey(user, device.getDevId(), 2));
		assertThat(syncKey).isNull();
	}
	
	@Test
	public void testPushNextAndConsume() {
		SyncKey syncKey = new SyncKey("123");
		WindowingIndexKey windowingIndexKey = new WindowingIndexKey(user, device.getDevId(), 1);
		
		EmailChanges firstEmails = generateEmails(25);
		EmailChanges secondEmails = generateEmails(25);
		testee.pushPendingElements(windowingIndexKey, syncKey, firstEmails);
		testee.pushNextRequestPendingElements(windowingIndexKey, syncKey, secondEmails);
		
		Iterable<EmailChanges> emailChanges = testee.consumingChunksIterable(windowingIndexKey);
		assertThat(emailChanges).containsOnly(firstEmails, secondEmails);
	}
	
	@Test
	public void testPushNextAndConsumeWithDataRemaining() {
		SyncKey syncKey = new SyncKey("123");
		WindowingIndexKey windowingIndexKey = new WindowingIndexKey(user, device.getDevId(), 1);
		EmailChanges generateEmails = generateEmails(25);
		testee.pushPendingElements(windowingIndexKey, syncKey, generateEmails);
		testee.pushNextRequestPendingElements(windowingIndexKey, syncKey, EmailChanges.builder().build());
		
		Iterable<EmailChanges> emailChanges = testee.consumingChunksIterable(windowingIndexKey);
		assertThat(emailChanges).containsOnly(generateEmails);
	}
	
	@Test
	public void testRemovePreviousCollectionWindowing() {
		SyncKey requestedSyncKey = new SyncKey("123");
		WindowingIndexKey windowingIndexKey = new WindowingIndexKey(user, device.getDevId(), 1);
		testee.pushPendingElements(windowingIndexKey, requestedSyncKey, EmailChanges.builder().build());
		testee.removePreviousCollectionWindowing(windowingIndexKey);
		SyncKey syncKey = testee.getWindowingSyncKey(windowingIndexKey);
		assertThat(syncKey).isNull();
	}
	
	@Test
	public void testConsumingChunksIterableCleansStore() {
		SyncKey syncKey = new SyncKey("123");
		WindowingIndexKey windowingIndexKey = new WindowingIndexKey(user, device.getDevId(), 1);
		EmailChanges generateEmails = generateEmails(25);
		testee.pushPendingElements(windowingIndexKey, syncKey, generateEmails);
		
		Iterable<EmailChanges> emailChanges = testee.consumingChunksIterable(windowingIndexKey);
		assertThat(emailChanges).containsOnly(generateEmails);
		
		Iterable<EmailChanges> emailChangesSecondTime = testee.consumingChunksIterable(windowingIndexKey);
		assertThat(emailChangesSecondTime).isEmpty();
	}
	
	@Test
	public void testConsumingChunksIterableWithIndex() {
		SyncKey syncKey = new SyncKey("123");
		WindowingIndexKey windowingIndexKey = new WindowingIndexKey(user, device.getDevId(), 1);
		EmailChanges firstEmails = generateEmails(25);
		testee.pushPendingElements(windowingIndexKey, syncKey, firstEmails);
		SyncKey secondSyncKey = new SyncKey("456");
		EmailChanges secondEmails = generateEmails(25, 30);
		testee.pushPendingElements(windowingIndexKey, secondSyncKey, secondEmails);
		
		Iterable<EmailChanges> emailChanges = testee.consumingChunksIterable(windowingIndexKey);
		assertThat(emailChanges).containsOnly(firstEmails, secondEmails);
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
