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
package org.obm.push.cassandra.migration.coded;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.cassandraunit.CassandraCQLUnit;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.push.cassandra.dao.CassandraStructure.MonitoredCollection;
import org.obm.push.cassandra.dao.CassandraStructure.Schema;
import org.obm.push.cassandra.dao.CassandraStructure.SnapshotIndex;
import org.obm.push.cassandra.dao.CassandraStructure.SnapshotTable;
import org.obm.push.cassandra.dao.CassandraStructure.SyncedCollection;
import org.obm.push.cassandra.dao.CassandraStructure.V1;
import org.obm.push.cassandra.dao.CassandraStructure.Windowing;
import org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex;
import org.obm.push.cassandra.dao.DaoTestsSchemaProducer;
import org.obm.push.cassandra.dao.SchemaCQLDataSet;
import org.obm.push.cassandra.dao.SessionProvider;
import org.obm.push.cassandra.schema.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableSet;

public class V2ToV3_TTLTest {


	private static final String KEYSPACE = "opush";
	private static final String DAO_SCHEMA = new DaoTestsSchemaProducer().schema(Version.of(2));
	@Rule public CassandraCQLUnit cassandraCQLUnit = new CassandraCQLUnit(new SchemaCQLDataSet(DAO_SCHEMA, KEYSPACE), "cassandra.yaml", "localhost", 9042);

	V2ToV3_TTL testee;
	Session session;
	SessionProvider sessionProvider;
	
	@Before
	public void setUp() {
		session = cassandraCQLUnit.session;
		sessionProvider = new SessionProvider(session);
		Logger logger = LoggerFactory.getLogger(V2ToV3_TTLTest.class);
		testee = new V2ToV3_TTL(logger , sessionProvider);
	}

	@Test
	public void applyShouldNotModifyCassandraSchemaData() {
		session.execute(
			insertInto(Schema.TABLE.get())
				.value(Schema.Columns.ID, 2)
				.value(Schema.Columns.VERSION, 5)
				.value(Schema.Columns.DATE, DateTime.parse("2014-10-01T12:09:37Z").toDate()));

		long originalWriteTime = session.execute(
			select()
				.writeTime(Schema.Columns.DATE)
				.from(Schema.TABLE.get()))
				.one().getLong(0);
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.writeTime(Schema.Columns.DATE)
				.ttl(Schema.Columns.DATE)
				.column(Schema.Columns.ID)
				.column(Schema.Columns.DATE)
				.column(Schema.Columns.VERSION).
			from(Schema.TABLE.get()));
		
		Row row = rs.one();
		assertThat(row.getLong(0)).isEqualTo(originalWriteTime);
		assertThat(row.getInt(1)).isEqualTo(0);
		assertThat(row.getInt(Schema.Columns.ID)).isEqualTo(2);
		assertThat(row.getInt(Schema.Columns.VERSION)).isEqualTo(5);
		assertThat(row.getDate(Schema.Columns.DATE)).isEqualTo(DateTime.parse("2014-10-01T12:09:37Z").toDate());
	}

	@Test
	public void applyShouldNotModifySnapshotData() {
		session.execute(
			insertInto(SnapshotTable.TABLE.get())
				.value(SnapshotTable.Columns.ID, UUID.fromString("a6249e7f-bab9-43f0-8ea7-fbf027cc65fa"))
				.value(SnapshotTable.Columns.SNAPSHOT, "snapshot"));

		long originalWriteTime = session.execute(
			select()
				.writeTime(SnapshotTable.Columns.SNAPSHOT)
				.from(SnapshotTable.TABLE.get()))
				.one().getLong(0);
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.writeTime(SnapshotTable.Columns.SNAPSHOT)
				.ttl(SnapshotTable.Columns.SNAPSHOT)
				.column(SnapshotTable.Columns.ID)
				.column(SnapshotTable.Columns.SNAPSHOT).
			from(SnapshotTable.TABLE.get()));
		
		Row row = rs.one();
		assertThat(row.getLong(0)).isGreaterThan(originalWriteTime);
		assertThat(row.getInt(1)).isGreaterThan(2500000);
		assertThat(row.getUUID(SnapshotTable.Columns.ID)).isEqualTo(UUID.fromString("a6249e7f-bab9-43f0-8ea7-fbf027cc65fa"));
		assertThat(row.getString(SnapshotTable.Columns.SNAPSHOT)).isEqualTo("snapshot");
	}

	@Test
	public void applyShouldNotModifySnapshotIndexData() {
		session.execute(
			insertInto(SnapshotIndex.TABLE.get())
				.value(SnapshotIndex.Columns.DEVICE_ID, "device id")
				.value(SnapshotIndex.Columns.COLLECTION_ID, 5)
				.value(SnapshotIndex.Columns.SNAPSHOT_ID, UUID.fromString("61d6ce72-c638-4345-ba2b-4cbafba691eb"))
				.value(SnapshotIndex.Columns.SYNC_KEY, UUID.fromString("24b2eddb-9532-41a6-82eb-92bb1f65221c")));

		long originalWriteTime = session.execute(
			select()
				.writeTime(SnapshotIndex.Columns.SNAPSHOT_ID)
				.from(SnapshotIndex.TABLE.get()))
				.one().getLong(0);
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.writeTime(SnapshotIndex.Columns.SNAPSHOT_ID)
				.ttl(SnapshotIndex.Columns.SNAPSHOT_ID)
				.column(SnapshotIndex.Columns.DEVICE_ID)
				.column(SnapshotIndex.Columns.COLLECTION_ID)
				.column(SnapshotIndex.Columns.SNAPSHOT_ID)
				.column(SnapshotIndex.Columns.SYNC_KEY).
			from(SnapshotIndex.TABLE.get()));
		
		Row row = rs.one();
		assertThat(row.getLong(0)).isGreaterThan(originalWriteTime);
		assertThat(row.getInt(1)).isGreaterThan(2500000);
		assertThat(row.getString(SnapshotIndex.Columns.DEVICE_ID)).isEqualTo("device id");
		assertThat(row.getInt(SnapshotIndex.Columns.COLLECTION_ID)).isEqualTo(5);
		assertThat(row.getUUID(SnapshotIndex.Columns.SNAPSHOT_ID)).isEqualTo(UUID.fromString("61d6ce72-c638-4345-ba2b-4cbafba691eb"));
		assertThat(row.getUUID(SnapshotIndex.Columns.SYNC_KEY)).isEqualTo(UUID.fromString("24b2eddb-9532-41a6-82eb-92bb1f65221c"));
	}

	@Test
	public void applyShouldNotModifyWindowingData() {
		session.execute(
			insertInto(Windowing.TABLE.get())
				.value(Windowing.Columns.ID, UUID.fromString("61d6ce72-c638-4345-ba2b-4cbafba691eb"))
				.value(Windowing.Columns.CHANGE_INDEX, 5)
				.value(Windowing.Columns.CHANGE_VALUE, "value")
				.value(Windowing.Columns.CHANGE_TYPE, "type"));

		long originalWriteTime = session.execute(
			select()
				.writeTime(Windowing.Columns.CHANGE_VALUE)
				.from(Windowing.TABLE.get()))
				.one().getLong(0);
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.writeTime(Windowing.Columns.CHANGE_VALUE)
				.ttl(Windowing.Columns.CHANGE_VALUE)
				.column(Windowing.Columns.ID)
				.column(Windowing.Columns.CHANGE_INDEX)
				.column(Windowing.Columns.CHANGE_VALUE)
				.column(Windowing.Columns.CHANGE_TYPE).
			from(Windowing.TABLE.get()));
		
		Row row = rs.one();
		assertThat(row.getLong(0)).isGreaterThan(originalWriteTime);
		assertThat(row.getInt(1)).isGreaterThan(2500000);
		assertThat(row.getUUID(Windowing.Columns.ID)).isEqualTo(UUID.fromString("61d6ce72-c638-4345-ba2b-4cbafba691eb"));
		assertThat(row.getInt(Windowing.Columns.CHANGE_INDEX)).isEqualTo(5);
		assertThat(row.getString(Windowing.Columns.CHANGE_VALUE)).isEqualTo("value");
		assertThat(row.getString(Windowing.Columns.CHANGE_TYPE)).isEqualTo("type");
	}

	@Test
	public void applyShouldNotModifyWindowingIndexData() {
		session.execute(
			insertInto(WindowingIndex.TABLE.get())
				.value(WindowingIndex.Columns.SYNC_KEY, UUID.fromString("61d6ce72-c638-4345-ba2b-4cbafba691eb"))
				.value(WindowingIndex.Columns.COLLECTION_ID, 5)
				.value(WindowingIndex.Columns.DEVICE_ID, "device id")
				.value(WindowingIndex.Columns.USER, "user")
				.value(WindowingIndex.Columns.WINDOWING_ID, UUID.fromString("82d6ce72-c638-4345-ba2b-4cbafba691eb"))
				.value(WindowingIndex.Columns.WINDOWING_INDEX, 6)
				.value(WindowingIndex.Columns.WINDOWING_KIND, "kind"));
		
		long originalWriteTime = session.execute(
			select()
				.writeTime(WindowingIndex.Columns.WINDOWING_ID)
				.from(WindowingIndex.TABLE.get()))
				.one().getLong(0);
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.writeTime(WindowingIndex.Columns.WINDOWING_ID)
				.ttl(WindowingIndex.Columns.WINDOWING_ID)
				.column(WindowingIndex.Columns.SYNC_KEY)
				.column(WindowingIndex.Columns.COLLECTION_ID)
				.column(WindowingIndex.Columns.DEVICE_ID)
				.column(WindowingIndex.Columns.USER)
				.column(WindowingIndex.Columns.WINDOWING_ID)
				.column(WindowingIndex.Columns.WINDOWING_INDEX)
				.column(WindowingIndex.Columns.WINDOWING_KIND).
			from(WindowingIndex.TABLE.get()));
		
		Row row = rs.one();
		assertThat(row.getLong(0)).isGreaterThan(originalWriteTime);
		assertThat(row.getInt(1)).isGreaterThan(2500000);
		assertThat(row.getUUID(WindowingIndex.Columns.SYNC_KEY)).isEqualTo(UUID.fromString("61d6ce72-c638-4345-ba2b-4cbafba691eb"));
		assertThat(row.getInt(WindowingIndex.Columns.COLLECTION_ID)).isEqualTo(5);
		assertThat(row.getString(WindowingIndex.Columns.DEVICE_ID)).isEqualTo("device id");
		assertThat(row.getString(WindowingIndex.Columns.USER)).isEqualTo("user");
		assertThat(row.getUUID(WindowingIndex.Columns.WINDOWING_ID)).isEqualTo(UUID.fromString("82d6ce72-c638-4345-ba2b-4cbafba691eb"));
		assertThat(row.getInt(WindowingIndex.Columns.WINDOWING_INDEX)).isEqualTo(6);
		assertThat(row.getString(WindowingIndex.Columns.WINDOWING_KIND)).isEqualTo("kind");
	}

	@Test
	public void applyShouldNotModifySyncedCollectionData() {
		session.execute(
			insertInto(SyncedCollection.TABLE.get())
				.value(SyncedCollection.Columns.USER, "SyncedCollection.Columns.USER")
				.value(SyncedCollection.Columns.DEVICE, "SyncedCollection.Columns.DEVICE")
				.value(SyncedCollection.Columns.COLLECTION_ID, 5)
				.value(SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION, "SyncedCollection.Columns.ANALYSED_SYNC_COLLECTIONS"));

		long originalWriteTime = session.execute(
			select()
				.writeTime(SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION)
				.from(SyncedCollection.TABLE.get()))
				.one().getLong(0);
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.writeTime(SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION)
				.ttl(SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION)
				.column(SyncedCollection.Columns.USER)
				.column(SyncedCollection.Columns.DEVICE)
				.column(SyncedCollection.Columns.COLLECTION_ID)
				.column(SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION).
			from(SyncedCollection.TABLE.get()));
		
		Row row = rs.one();
		assertThat(row.getLong(0)).isGreaterThan(originalWriteTime);
		assertThat(row.getInt(1)).isGreaterThan(2500000);
		assertThat(row.getInt(SyncedCollection.Columns.COLLECTION_ID)).isEqualTo(5);
		assertThat(row.getString(SyncedCollection.Columns.USER)).isEqualTo("SyncedCollection.Columns.USER");
		assertThat(row.getString(SyncedCollection.Columns.DEVICE)).isEqualTo("SyncedCollection.Columns.DEVICE");
		assertThat(row.getString(SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION)).isEqualTo("SyncedCollection.Columns.ANALYSED_SYNC_COLLECTIONS");
	}

	@Test
	public void applyShouldNotModifyV1SyncedCollectionData() {
		session.execute(
			insertInto(V1.SyncedCollection.TABLE.get())
				.value(V1.SyncedCollection.Columns.CREDENTIALS, "V1.SyncedCollection.Columns.CREDENTIALS")
				.value(V1.SyncedCollection.Columns.DEVICE, "V1.SyncedCollection.Columns.DEVICE")
				.value(V1.SyncedCollection.Columns.COLLECTION_ID, 5)
				.value(V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION, "V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTIONS"));

		long originalWriteTime = session.execute(
			select()
				.writeTime(V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION)
				.from(V1.SyncedCollection.TABLE.get()))
				.one().getLong(0);
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.writeTime(V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION)
				.ttl(V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION)
				.column(V1.SyncedCollection.Columns.CREDENTIALS)
				.column(V1.SyncedCollection.Columns.DEVICE)
				.column(V1.SyncedCollection.Columns.COLLECTION_ID)
				.column(V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION).
			from(V1.SyncedCollection.TABLE.get()));
		
		Row row = rs.one();
		assertThat(row.getLong(0)).isGreaterThan(originalWriteTime);
		assertThat(row.getInt(1)).isGreaterThan(2500000);
		assertThat(row.getInt(V1.SyncedCollection.Columns.COLLECTION_ID)).isEqualTo(5);
		assertThat(row.getString(V1.SyncedCollection.Columns.CREDENTIALS)).isEqualTo("V1.SyncedCollection.Columns.CREDENTIALS");
		assertThat(row.getString(V1.SyncedCollection.Columns.DEVICE)).isEqualTo("V1.SyncedCollection.Columns.DEVICE");
		assertThat(row.getString(V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION)).isEqualTo("V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTIONS");
	}

	@Test
	public void applyShouldNotModifyV1MonitoredCollectionRows() {
		session.execute(
			insertInto(V1.MonitoredCollection.TABLE.get())
				.value(V1.MonitoredCollection.Columns.CREDENTIALS, "V1.MonitoredCollection.Columns.CREDENTIALS")
				.value(V1.MonitoredCollection.Columns.DEVICE, "V1.MonitoredCollection.Columns.DEVICE")
				.value(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, ImmutableSet.of("V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS")));
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.column(V1.MonitoredCollection.Columns.CREDENTIALS)
				.column(V1.MonitoredCollection.Columns.DEVICE)
				.column(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS).
			from(V1.MonitoredCollection.TABLE.get()));
		
		// Cannot verify the TTL as it cannot be evaluated for neither primary key nor collections
		Row row = rs.one();
		assertThat(row.getString(V1.MonitoredCollection.Columns.CREDENTIALS)).isEqualTo("V1.MonitoredCollection.Columns.CREDENTIALS");
		assertThat(row.getString(V1.MonitoredCollection.Columns.DEVICE)).isEqualTo("V1.MonitoredCollection.Columns.DEVICE");
		assertThat(row.getSet(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, String.class)).isEqualTo(ImmutableSet.of("V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS"));
	}

	@Test
	public void applyShouldNotModifyMonitoredCollectionRows() {
		session.execute(
			insertInto(MonitoredCollection.TABLE.get())
				.value(MonitoredCollection.Columns.USER, "MonitoredCollection.Columns.USER")
				.value(MonitoredCollection.Columns.DEVICE, "MonitoredCollection.Columns.DEVICE")
				.value(MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, ImmutableSet.of("MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS")));
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.column(MonitoredCollection.Columns.USER)
				.column(MonitoredCollection.Columns.DEVICE)
				.column(MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS).
			from(MonitoredCollection.TABLE.get()));

		// Cannot verify the TTL as it cannot be evaluated for neither primary key nor collections
		Row row = rs.one();
		assertThat(row.getString(MonitoredCollection.Columns.USER)).isEqualTo("MonitoredCollection.Columns.USER");
		assertThat(row.getString(MonitoredCollection.Columns.DEVICE)).isEqualTo("MonitoredCollection.Columns.DEVICE");
		assertThat(row.getSet(MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, String.class)).isEqualTo(ImmutableSet.of("MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS"));
	}

	@Test
	public void applyShouldModifyEveryRowWhenBatchSizePlusOneRowCount() {
		int rowCountToRewrite = V2ToV3_TTL.MAX_BATCH_SIZE +1;
		UUID lastInsertionUUID = null;
		for (int i = 0; i < rowCountToRewrite ; i++) {
			lastInsertionUUID = UUID.randomUUID();
			session.execute(
				insertInto(SnapshotTable.TABLE.get())
					.value(SnapshotTable.Columns.ID, lastInsertionUUID)
					.value(SnapshotTable.Columns.SNAPSHOT, "snapshot" + i));
		}

		long latestOriginalWriteTime = session.execute(
			select()
				.writeTime(SnapshotTable.Columns.SNAPSHOT)
				.from(SnapshotTable.TABLE.get())
				.where(eq(SnapshotTable.Columns.ID, lastInsertionUUID)))
				.one().getLong(0);
		
		testee.apply();

		ResultSet rs = session.execute(
			select()
				.writeTime(SnapshotTable.Columns.SNAPSHOT)
				.ttl(SnapshotTable.Columns.SNAPSHOT).
			from(SnapshotTable.TABLE.get()));
		
		for (Row row : rs.all()) {
			assertThat(row.getLong(0)).isGreaterThan(latestOriginalWriteTime);
			assertThat(row.getInt(1)).isGreaterThan(2500000);
		}
	}
}
