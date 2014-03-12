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
package org.obm.push.cassandra.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.cassandra.schema.NoVersionException;
import org.obm.push.cassandra.schema.Version;
import org.obm.push.json.JSONService;
import org.slf4j.Logger;

import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableSet;

public class SchemaProducerImplTest {

	private SchemaProducerImpl schemaProducerImpl;
	private MonitoredCollectionDaoCassandraImpl monitoredCollectionDaoCassandraImpl;
	private SnapshotDaoCassandraImpl snapshotDaoCassandraImpl;
	private SyncedCollectionDaoCassandraImpl syncedCollectionDaoCassandraImpl;
	private WindowingDaoCassandraImpl windowingDaoCassandraImpl;

	@Before
	public void setup() {
		Session serssion = null;
		JSONService jsonService = null;
		Logger logger = null;
		monitoredCollectionDaoCassandraImpl = new MonitoredCollectionDaoCassandraImpl(serssion, jsonService, logger);
		snapshotDaoCassandraImpl = new SnapshotDaoCassandraImpl(serssion, jsonService, logger);
		syncedCollectionDaoCassandraImpl = new SyncedCollectionDaoCassandraImpl(serssion, jsonService, logger);
		windowingDaoCassandraImpl = new WindowingDaoCassandraImpl(serssion, jsonService, logger);
		schemaProducerImpl = new TestResourcesSchemaProducerImpl(monitoredCollectionDaoCassandraImpl, 
				snapshotDaoCassandraImpl, 
				syncedCollectionDaoCassandraImpl, 
				windowingDaoCassandraImpl);
	}
	
	public class TestResourcesSchemaProducerImpl extends SchemaProducerImpl {

		TestResourcesSchemaProducerImpl(
				MonitoredCollectionDaoCassandraImpl monitoredCollectionDaoCassandraImpl,
				SnapshotDaoCassandraImpl snapshotDaoCassandraImpl,
				SyncedCollectionDaoCassandraImpl syncedCollectionDaoCassandraImpl,
				WindowingDaoCassandraImpl windowingDaoCassandraImpl) {
			super(monitoredCollectionDaoCassandraImpl, snapshotDaoCassandraImpl,
					syncedCollectionDaoCassandraImpl, windowingDaoCassandraImpl);
		}

		@Override
		protected URL versionDirectory() {
			return ClassLoader.getSystemResource("versionsTests");
		}
	}
	
	public class NoVersionExceptionSchemaProducerImpl extends SchemaProducerImpl {

		NoVersionExceptionSchemaProducerImpl(
				MonitoredCollectionDaoCassandraImpl monitoredCollectionDaoCassandraImpl,
				SnapshotDaoCassandraImpl snapshotDaoCassandraImpl,
				SyncedCollectionDaoCassandraImpl syncedCollectionDaoCassandraImpl,
				WindowingDaoCassandraImpl windowingDaoCassandraImpl) {
			super(monitoredCollectionDaoCassandraImpl, snapshotDaoCassandraImpl,
					syncedCollectionDaoCassandraImpl, windowingDaoCassandraImpl);
		}

		@Override
		protected URL versionDirectory() {
			return null;
		}
	}
	
	public class BadVersionsFolderSchemaProducerImpl extends SchemaProducerImpl {

		BadVersionsFolderSchemaProducerImpl(
				MonitoredCollectionDaoCassandraImpl monitoredCollectionDaoCassandraImpl,
				SnapshotDaoCassandraImpl snapshotDaoCassandraImpl,
				SyncedCollectionDaoCassandraImpl syncedCollectionDaoCassandraImpl,
				WindowingDaoCassandraImpl windowingDaoCassandraImpl) {
			super(monitoredCollectionDaoCassandraImpl, snapshotDaoCassandraImpl,
					syncedCollectionDaoCassandraImpl, windowingDaoCassandraImpl);
		}

		@Override
		protected URL versionDirectory() {
			return ClassLoader.getSystemResource("badVersionsFolder");
		}
	}
	
	@Test(expected=NoVersionException.class)
	public void testVersionDirectoriesThrowsException() {
		SchemaProducerImpl schemaProducerImpl = new NoVersionExceptionSchemaProducerImpl(monitoredCollectionDaoCassandraImpl, 
				snapshotDaoCassandraImpl, 
				syncedCollectionDaoCassandraImpl, 
				windowingDaoCassandraImpl);
		schemaProducerImpl.versionDirectories();
	}
	
	@Test
	public void testVersionDirectories() {
		File[] versionDirectories = schemaProducerImpl.versionDirectories();
		assertThat(versionDirectories).hasSize(3);
	}
	
	@Test
	public void testVersionsAvailableWhenBadVersionsFolder() {
		SchemaProducerImpl schemaProducerImpl = new BadVersionsFolderSchemaProducerImpl(monitoredCollectionDaoCassandraImpl, 
				snapshotDaoCassandraImpl, 
				syncedCollectionDaoCassandraImpl, 
				windowingDaoCassandraImpl);
		SortedSet<Version> versionsAvailable = schemaProducerImpl.versionsAvailable();
		assertThat(versionsAvailable).isEmpty();
	}
	
	@Test
	public void testVersionsAvailable() {
		SortedSet<Version> versionsAvailable = schemaProducerImpl.versionsAvailable();
		assertThat(versionsAvailable).containsExactly(Version.of(1), Version.of(2), Version.of(3));
	}
	
	@Test
	public void testLastSchema() {
		String schema = schemaProducerImpl.lastSchema();
		assertThat(schema).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n" +
			"ALTER TABLE monitored_collection VERSION 2\n" +
			"CREATE TABLE snapshot_index (\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	snapshot_id uuid,\n" +
			"	PRIMARY KEY ((device_id), collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE snapshot (\n" +
			"	id uuid,\n" +
			"	snapshot text,\n" +
			"	PRIMARY KEY (id)\n" +
			");\n" +
			"ALTER TABLE snapshot VERSION 2\n" +
			"ALTER TABLE snapshot VERSION 3\n" +
			"CREATE TABLE synced_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	collection_id int,\n" +
			"	analysed_sync_collection text,\n" +
			"	PRIMARY KEY (credentials, device, collection_id)\n" +
			");\n" +
			"CREATE TABLE windowing_index (\n" +
			"	user text,\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	windowing_id uuid,\n" +
			"	windowing_kind text,\n" +
			"	windowing_index int,\n" +
			"	PRIMARY KEY (user, device_id, collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE windowing (\n" +
			"	id uuid,\n" +
			"	change_index int,\n" +
			"	change_type text,\n" +
			"	change_value text,\n" +
			"	PRIMARY KEY ((id), change_index, change_type)\n" +
			");\n" +
			"ALTER TABLE windowing_index VERSION 3\n");
	}
	
	@Test
	public void testSchemaVersion1() {
		String schema = schemaProducerImpl.schema(Version.of(1));
		assertThat(schema).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n" +
			"CREATE TABLE snapshot_index (\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	snapshot_id uuid,\n" +
			"	PRIMARY KEY ((device_id), collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE snapshot (\n" +
			"	id uuid,\n" +
			"	snapshot text,\n" +
			"	PRIMARY KEY (id)\n" +
			");\n" +
			"CREATE TABLE synced_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	collection_id int,\n" +
			"	analysed_sync_collection text,\n" +
			"	PRIMARY KEY (credentials, device, collection_id)\n" +
			");\n" +
			"CREATE TABLE windowing_index (\n" +
			"	user text,\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	windowing_id uuid,\n" +
			"	windowing_kind text,\n" +
			"	windowing_index int,\n" +
			"	PRIMARY KEY (user, device_id, collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE windowing (\n" +
			"	id uuid,\n" +
			"	change_index int,\n" +
			"	change_type text,\n" +
			"	change_value text,\n" +
			"	PRIMARY KEY ((id), change_index, change_type)\n" +
			");\n");
	}
	
	@Test
	public void testSchemaVersion2() {
		String schema = schemaProducerImpl.schema(Version.of(2));
		assertThat(schema).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n" +
			"ALTER TABLE monitored_collection VERSION 2\n" +
			"CREATE TABLE snapshot_index (\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	snapshot_id uuid,\n" +
			"	PRIMARY KEY ((device_id), collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE snapshot (\n" +
			"	id uuid,\n" +
			"	snapshot text,\n" +
			"	PRIMARY KEY (id)\n" +
			");\n" +
			"ALTER TABLE snapshot VERSION 2\n" +
			"CREATE TABLE synced_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	collection_id int,\n" +
			"	analysed_sync_collection text,\n" +
			"	PRIMARY KEY (credentials, device, collection_id)\n" +
			");\n" +
			"CREATE TABLE windowing_index (\n" +
			"	user text,\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	windowing_id uuid,\n" +
			"	windowing_kind text,\n" +
			"	windowing_index int,\n" +
			"	PRIMARY KEY (user, device_id, collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE windowing (\n" +
			"	id uuid,\n" +
			"	change_index int,\n" +
			"	change_type text,\n" +
			"	change_value text,\n" +
			"	PRIMARY KEY ((id), change_index, change_type)\n" +
			");\n");
	}
	
	@Test
	public void testSchemaVersion3() {
		String schema = schemaProducerImpl.schema(Version.of(3));
		assertThat(schema).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n" +
			"ALTER TABLE monitored_collection VERSION 2\n" +
			"CREATE TABLE snapshot_index (\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	snapshot_id uuid,\n" +
			"	PRIMARY KEY ((device_id), collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE snapshot (\n" +
			"	id uuid,\n" +
			"	snapshot text,\n" +
			"	PRIMARY KEY (id)\n" +
			");\n" +
			"ALTER TABLE snapshot VERSION 2\n" +
			"ALTER TABLE snapshot VERSION 3\n" +
			"CREATE TABLE synced_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	collection_id int,\n" +
			"	analysed_sync_collection text,\n" +
			"	PRIMARY KEY (credentials, device, collection_id)\n" +
			");\n" +
			"CREATE TABLE windowing_index (\n" +
			"	user text,\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	windowing_id uuid,\n" +
			"	windowing_kind text,\n" +
			"	windowing_index int,\n" +
			"	PRIMARY KEY (user, device_id, collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE windowing (\n" +
			"	id uuid,\n" +
			"	change_index int,\n" +
			"	change_type text,\n" +
			"	change_value text,\n" +
			"	PRIMARY KEY ((id), change_index, change_type)\n" +
			");\n" +
			"ALTER TABLE windowing_index VERSION 3\n");
	}
	
	@Test
	public void testLoadDaoScriptsNoScript() {
		String scripts = schemaProducerImpl.loadDaoScripts(ImmutableSet.of(Table.of("unknown")), Version.of(1));
		assertThat(scripts).isEmpty();
	}
	
	@Test
	public void testLoadDaoScriptsVersion1() {
		String scripts = schemaProducerImpl.loadDaoScripts(ImmutableSet.of(Table.of("monitored_collection")), Version.of(1));
		assertThat(scripts).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n");
	}
	
	@Test
	public void testLoadDaoScriptsVersion2() {
		String scripts = schemaProducerImpl.loadDaoScripts(ImmutableSet.of(Table.of("monitored_collection")), Version.of(2));
		assertThat(scripts).isEqualTo(
			"ALTER TABLE monitored_collection VERSION 2\n");
	}
	
	@Test
	public void testLoadDaoScriptsVersion1WithTablesFromDao() {
		String scripts = schemaProducerImpl.loadDaoScripts(monitoredCollectionDaoCassandraImpl.tables(), Version.of(1));
		assertThat(scripts).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n");
	}
	
	@Test
	public void testLoadDaoScriptsVersion2WithTablesFromDao() {
		String scripts = schemaProducerImpl.loadDaoScripts(monitoredCollectionDaoCassandraImpl.tables(), Version.of(2));
		assertThat(scripts).isEqualTo(
			"ALTER TABLE monitored_collection VERSION 2\n");
	}
	
	@Test
	public void testLastSchemaForDAO() {
		String scripts = schemaProducerImpl.lastSchemaForDAO(MonitoredCollectionDaoCassandraImpl.class);
		assertThat(scripts).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n" +
			"ALTER TABLE monitored_collection VERSION 2\n");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testVersionsToApplyToVersionMandatory() {
		schemaProducerImpl.versionsToApply(null, null);
	}
	
	@Test
	public void testVersionsToApplyWithoutFromVersionBringsThemAll() {
		List<Version> versions = schemaProducerImpl.versionsToApply(null, Version.of(3));
		assertThat(versions).containsExactly(Version.of(1), Version.of(2), Version.of(3));
	}
	
	@Test
	public void testVersionsToApply() {
		List<Version> versions = schemaProducerImpl.versionsToApply(Version.of(2), Version.of(3));
		assertThat(versions).containsExactly(Version.of(2), Version.of(3));
	}
	
	@Test
	public void testSchemaForDAOVersion1() {
		String scripts = schemaProducerImpl.schemaForDAO(MonitoredCollectionDaoCassandraImpl.class, Version.of(1));
		assertThat(scripts).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n");
	}
	
	@Test
	public void testSchemaForDAOVersion2() {
		String scripts = schemaProducerImpl.schemaForDAO(MonitoredCollectionDaoCassandraImpl.class, Version.of(2));
		assertThat(scripts).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n" +
			"ALTER TABLE monitored_collection VERSION 2\n");
	}
	
	@Test
	public void testSchemaForDAOVersion3() {
		String scripts = schemaProducerImpl.schemaForDAO(MonitoredCollectionDaoCassandraImpl.class, Version.of(3));
		assertThat(scripts).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n" +
			"ALTER TABLE monitored_collection VERSION 2\n");
	}
	
	@Test
	public void testSchemaWithoutFromVersionBringsThemAll() {
		String schema = schemaProducerImpl.schema(null, Version.of(3));
		assertThat(schema).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n" +
			"ALTER TABLE monitored_collection VERSION 2\n" +
			"CREATE TABLE snapshot_index (\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	snapshot_id uuid,\n" +
			"	PRIMARY KEY ((device_id), collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE snapshot (\n" +
			"	id uuid,\n" +
			"	snapshot text,\n" +
			"	PRIMARY KEY (id)\n" +
			");\n" +
			"ALTER TABLE snapshot VERSION 2\n" +
			"ALTER TABLE snapshot VERSION 3\n" +
			"CREATE TABLE synced_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	collection_id int,\n" +
			"	analysed_sync_collection text,\n" +
			"	PRIMARY KEY (credentials, device, collection_id)\n" +
			");\n" +
			"CREATE TABLE windowing_index (\n" +
			"	user text,\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	windowing_id uuid,\n" +
			"	windowing_kind text,\n" +
			"	windowing_index int,\n" +
			"	PRIMARY KEY (user, device_id, collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE windowing (\n" +
			"	id uuid,\n" +
			"	change_index int,\n" +
			"	change_type text,\n" +
			"	change_value text,\n" +
			"	PRIMARY KEY ((id), change_index, change_type)\n" +
			");\n" +
			"ALTER TABLE windowing_index VERSION 3\n");
	}
	
	@Test
	public void testSchemaFrom1To3() {
		String schema = schemaProducerImpl.schema(Version.of(1), Version.of(3));
		assertThat(schema).isEqualTo(
			"CREATE TABLE monitored_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	analysed_sync_collections set<text>,\n" +
			"	PRIMARY KEY (credentials, device)\n" +
			");\n" +
			"ALTER TABLE monitored_collection VERSION 2\n" +
			"CREATE TABLE snapshot_index (\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	snapshot_id uuid,\n" +
			"	PRIMARY KEY ((device_id), collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE snapshot (\n" +
			"	id uuid,\n" +
			"	snapshot text,\n" +
			"	PRIMARY KEY (id)\n" +
			");\n" +
			"ALTER TABLE snapshot VERSION 2\n" +
			"ALTER TABLE snapshot VERSION 3\n" +
			"CREATE TABLE synced_collection (\n" +
			"	credentials text,\n" +
			"	device text,\n" +
			"	collection_id int,\n" +
			"	analysed_sync_collection text,\n" +
			"	PRIMARY KEY (credentials, device, collection_id)\n" +
			");\n" +
			"CREATE TABLE windowing_index (\n" +
			"	user text,\n" +
			"	device_id text,\n" +
			"	collection_id int,\n" +
			"	sync_key uuid,\n" +
			"	windowing_id uuid,\n" +
			"	windowing_kind text,\n" +
			"	windowing_index int,\n" +
			"	PRIMARY KEY (user, device_id, collection_id, sync_key)\n" +
			");\n" +
			"CREATE TABLE windowing (\n" +
			"	id uuid,\n" +
			"	change_index int,\n" +
			"	change_type text,\n" +
			"	change_value text,\n" +
			"	PRIMARY KEY ((id), change_index, change_type)\n" +
			");\n" +
			"ALTER TABLE windowing_index VERSION 3\n");
	}
	
	@Test
	public void testSchemaFrom2To2() {
		String schema = schemaProducerImpl.schema(Version.of(2), Version.of(2));
		assertThat(schema).isEqualTo(
			"ALTER TABLE monitored_collection VERSION 2\n" +
			"ALTER TABLE snapshot VERSION 2\n");
	}
	
	@Test
	public void testSchemaFrom2To3() {
		String schema = schemaProducerImpl.schema(Version.of(2), Version.of(3));
		assertThat(schema).isEqualTo(
			"ALTER TABLE monitored_collection VERSION 2\n" +
			"ALTER TABLE snapshot VERSION 2\n" +
			"ALTER TABLE snapshot VERSION 3\n" +
			"ALTER TABLE windowing_index VERSION 3\n");
	}
	
	@Test
	public void testSchemaFrom3To3() {
		String schema = schemaProducerImpl.schema(Version.of(3), Version.of(3));
		assertThat(schema).isEqualTo(
			"ALTER TABLE snapshot VERSION 3\n" +
			"ALTER TABLE windowing_index VERSION 3\n");
	}
}
