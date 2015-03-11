/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015 Linagora
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
package org.obm.push.cassandra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Date;
import java.util.Properties;

import org.cassandraunit.CassandraCQLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.DateUtils;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.push.ProtocolVersion;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.cassandra.dao.DaoTestsSchemaProducer;
import org.obm.push.cassandra.dao.OpushCassandraCQLUnit;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.FolderSnapshotDao.FolderSnapshotNotFoundException;
import org.obm.push.state.FolderSyncKey;
import org.obm.sync.date.DateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

@RunWith(GuiceRunner.class)
@GuiceModule(CassandraDeviceDataCleanerTest.Env.class)
public class CassandraDeviceDataCleanerTest {

	public static class Env extends AbstractModule {

		@Override
		protected void configure() {
			bind(Logger.class).annotatedWith(Names.named("CASSANDRA")).toInstance(LoggerFactory.getLogger(Env.class));
			bind(Logger.class).annotatedWith(Names.named("CONFIGURATION")).toInstance(LoggerFactory.getLogger(Env.class));
			bind(DateProvider.class).toInstance(new DateProvider() {
				
				@Override
				public Date getDate() {
					return DateUtils.dateUTC("2015-05-04T11:09:37Z");
				}
			});
			
			install(Modules.override(new OpushCassandraModule())
				.with(new AbstractModule(){

					@Override
					protected void configure() {
						bind(Session.class).toProvider(DelayedSessionProvider.class);
					}
				})
			);
		}
	}
	
	private static final String DAO_SCHEMA = new DaoTestsSchemaProducer().lastSchema();
	@Rule public CassandraCQLUnit cassandraCQLUnit = new OpushCassandraCQLUnit(DAO_SCHEMA);

	private Device device1;
	private User user1;
	private Device device2;
	private User user2;
	
	@Inject DelayedSessionProvider sessionProvider;
	@Inject FolderSnapshotDao folderSnapshotDao;
	@Inject CassandraDeviceDataCleaner testee;
	
	@Before
	public void init() {
		device1 = new Device(1, "devType", new DeviceId("devId"), new Properties(), ProtocolVersion.V121);
		user1 = Factory.create().createUser("login@domain", "email@domain", "displayName");
		device2 = new Device(2, "devType2", new DeviceId("devId2"), new Properties(), ProtocolVersion.V121);
		user2 = Factory.create().createUser("login2@domain", "email2@domain", "displayName2");
		
		cassandraCQLUnit.session.getCluster().connect();
		sessionProvider.ready(cassandraCQLUnit.session);
	}

	@Test(expected=DaoException.class)
	public void cleanShouldPropagateExceptionAsDaoException() {
		cassandraCQLUnit.session.close();
		testee.clean(user2, device2.getDevId());
	}
	
	@Test
	public void cleanShouldDoNothingWhenNothingMatch() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		CollectionId collectionId = CollectionId.of(2);
		FolderSnapshot snapshot = FolderSnapshot.nextId(2).folders(
				ImmutableSet.of(Folder.builder()
						.displayName("name")
						.backendId(MailboxPath.of("15"))
						.collectionId(collectionId)
						.parentBackendId(MailboxPath.of("12"))
						.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build()));
		
		folderSnapshotDao.create(user1, device1, syncKey, snapshot);
		
		testee.clean(user2, device2.getDevId());

		assertThatUserHasFolderSnapshot(user1, device1, syncKey);
		assertThatUserHasFolderMapping(user1, device1, collectionId);
	}
	
	@Test
	public void cleanShouldDropUserDeviceFolderSnapshotAndMapping() throws Exception {
		FolderSyncKey syncKey = new FolderSyncKey("26b7ebcd-9dca-4d89-8725-411223ebb5e8");
		CollectionId collectionId = CollectionId.of(2);
		FolderSnapshot snapshot = FolderSnapshot.nextId(2).folders(
			ImmutableSet.of(Folder.builder()
				.displayName("name")
				.backendId(MailboxPath.of("15"))
				.collectionId(collectionId)
				.parentBackendId(MailboxPath.of("12"))
				.folderType(FolderType.USER_CREATED_CONTACTS_FOLDER).build()));
		
		folderSnapshotDao.create(user1, device1, syncKey, snapshot);
		folderSnapshotDao.create(user1, device2, syncKey, snapshot);
		folderSnapshotDao.create(user2, device1, syncKey, snapshot);
		folderSnapshotDao.create(user2, device2, syncKey, snapshot);
		
		testee.clean(user2, device1.getDevId());
		
		assertThatUserHasFolderSnapshot(user1, device1, syncKey);
		assertThatUserHasFolderSnapshot(user1, device2, syncKey);
		assertThatUserHasNoFolderSnapshot(user2, device1, syncKey);
		assertThatUserHasFolderSnapshot(user2, device2, syncKey);
		
		assertThatUserHasFolderMapping(user1, device1, collectionId);
		assertThatUserHasFolderMapping(user1, device2, collectionId);
		assertThatUserHasNoFolderMapping(user2, device1, collectionId);
		assertThatUserHasFolderMapping(user2, device2, collectionId);
	}

	private void assertThatUserHasFolderSnapshot(User user, Device device, FolderSyncKey synckKey) {
		try {
			assertThat(folderSnapshotDao.get(user, device, synckKey)).isNotNull();
		} catch (FolderSnapshotNotFoundException e) {
			fail("FolderSnapshotNotFoundException was not expected");
		}
	}
	
	private void assertThatUserHasFolderMapping(User user, Device device, CollectionId collectionId) {
		try {
			assertThat(folderSnapshotDao.get(user, device, collectionId)).isNotNull();
		} catch (CollectionNotFoundException e) {
			fail("CollectionNotFoundException was not expected");
		}
	}

	private void assertThatUserHasNoFolderSnapshot(User user, Device device, FolderSyncKey synckKey) {
		try {
			folderSnapshotDao.get(user, device, synckKey);
			fail("FolderSnapshotNotFoundException was expected");
		} catch (FolderSnapshotNotFoundException e) {
			// expected
		}
	}
	
	private void assertThatUserHasNoFolderMapping(User user, Device device, CollectionId collectionId) {
		try {
			folderSnapshotDao.get(user, device, collectionId);
			fail("CollectionNotFoundException was expected");
		} catch (CollectionNotFoundException e) {
			// expected
		}
	}

}
