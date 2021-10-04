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

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.WriteType;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.policies.RetryPolicy;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;
import org.obm.push.bean.migration.CodedMigration;
import org.obm.push.bean.migration.Version;
import org.obm.push.cassandra.dao.CassandraStructure.MonitoredCollection;
import org.obm.push.cassandra.dao.CassandraStructure.SnapshotIndex;
import org.obm.push.cassandra.dao.CassandraStructure.SnapshotTable;
import org.obm.push.cassandra.dao.CassandraStructure.SyncedCollection;
import org.obm.push.cassandra.dao.CassandraStructure.V1;
import org.obm.push.cassandra.dao.CassandraStructure.Windowing;
import org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex;
import org.obm.push.utils.jvm.VMArgumentsUtils;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

public class V2ToV3_TTL implements CodedMigration {

  public static final int MAX_BATCH_SIZE = Objects.firstNonNull(VMArgumentsUtils.integerArgumentValue("MigrationV3BatchSize"), 100);
  public static final int SELECT_PAGING_SIZE = Objects.firstNonNull(VMArgumentsUtils.integerArgumentValue("MigrationV3PagingSize"), 100);
  private static final long DEFAULT_TTL = TimeUnit.SECONDS.convert(30, TimeUnit.DAYS);
  private static final RetryPolicy ALWAYS_RETRY_POLICY = new AlwaysRetryPolicy();

  private final Logger logger;
  private final Provider<Session> sessionProvider;
  private final Set<Table> tables;

  public V2ToV3_TTL(
      Logger logger,
      Provider<Session> sessionProvider) {
    this.logger = logger;
    this.sessionProvider = sessionProvider;
    this.tables = ImmutableSet.<Table>builder()
        .add(new V1MonitoredCollection())
        .add(new V2MonitoredCollection())
        .add(new V1SyncedCollection())
        .add(new V2SyncedCollection())
        .add(new V2Snapshot())
        .add(new V2SnapshotIndex())
        .add(new V2WindowingIndex())
        .add(new V2Windowing())
        .build();
  }

  @Override
  public Version from() {
    return Version.of(2);
  }

  @Override
  public Version to() {
    return Version.of(3);
  }

  @Override
  public void apply() {
    logger.info("TTL migration started");
    Session session = sessionProvider.get();
    for (Table table : tables) {
      logger.info("Setup TTL for {}", table.name());
      session.execute(String.format("ALTER TABLE %s WITH default_time_to_live = %d;", table.name(), DEFAULT_TTL));
      logger.info("Apply TTL for existing rows");
      reinsertAll(session, table);
      logger.info("Done");
    }
  }

  private void reinsertAll(Session session, Table table) {
    Iterator<Row> rows = session.execute(table
        .selectStatement()
        .setRetryPolicy(ALWAYS_RETRY_POLICY)
        .setFetchSize(SELECT_PAGING_SIZE)).iterator();

    BatchStatement batch = newBatchStatement();
    int batchSize = 0;

    while (rows.hasNext()) {
      batch.add(table.insertStatement(rows.next()));
      batchSize++;

      if (batchSize >= MAX_BATCH_SIZE) {
        logger.info("Executing batch, size: {}", MAX_BATCH_SIZE);
        session.execute(batch);
        batchSize = 0;
        batch = newBatchStatement();
      }
    }

    if (batchSize > 0) {
      logger.info("Executing batch, size: {}", batchSize);
      session.execute(batch);
    }
  }

  private BatchStatement newBatchStatement() {
    BatchStatement batch = new BatchStatement(Type.UNLOGGED);
    batch.setRetryPolicy(ALWAYS_RETRY_POLICY);
    return batch;
  }

  interface Table {
    String name();

    Statement selectStatement();

    Statement insertStatement(Row row);
  }

  static class V1MonitoredCollection implements Table {

    @Override
    public String name() {
      return V1.MonitoredCollection.TABLE.get();
    }

    @Override
    public Statement selectStatement() {
      return select(
          V1.MonitoredCollection.Columns.CREDENTIALS,
          V1.MonitoredCollection.Columns.DEVICE,
          V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS).
          from(V1.MonitoredCollection.TABLE.get());
    }

    @Override
    public Statement insertStatement(Row row) {
      return insertInto(V1.MonitoredCollection.TABLE.get())
          .value(V1.MonitoredCollection.Columns.CREDENTIALS, row.getString(V1.MonitoredCollection.Columns.CREDENTIALS))
          .value(V1.MonitoredCollection.Columns.DEVICE, row.getString(V1.MonitoredCollection.Columns.DEVICE))
          .value(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, row.getSet(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, String.class));
    }
  }

  static class V2MonitoredCollection implements Table {

    @Override
    public String name() {
      return MonitoredCollection.TABLE.get();
    }

    @Override
    public Statement selectStatement() {
      return select(
          MonitoredCollection.Columns.USER,
          MonitoredCollection.Columns.DEVICE,
          MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS).
          from(MonitoredCollection.TABLE.get());
    }

    @Override
    public Statement insertStatement(Row row) {
      return insertInto(MonitoredCollection.TABLE.get())
          .value(MonitoredCollection.Columns.USER, row.getString(MonitoredCollection.Columns.USER))
          .value(MonitoredCollection.Columns.DEVICE, row.getString(MonitoredCollection.Columns.DEVICE))
          .value(MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, row.getSet(MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, String.class));
    }
  }

  static class V1SyncedCollection implements Table {

    @Override
    public String name() {
      return V1.SyncedCollection.TABLE.get();
    }

    @Override
    public Statement selectStatement() {
      return select(
          V1.SyncedCollection.Columns.CREDENTIALS,
          V1.SyncedCollection.Columns.DEVICE,
          V1.SyncedCollection.Columns.COLLECTION_ID,
          V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION).
          from(V1.SyncedCollection.TABLE.get());
    }

    @Override
    public Statement insertStatement(Row row) {
      return insertInto(V1.SyncedCollection.TABLE.get())
          .value(V1.SyncedCollection.Columns.CREDENTIALS, row.getString(V1.SyncedCollection.Columns.CREDENTIALS))
          .value(V1.SyncedCollection.Columns.DEVICE, row.getString(V1.SyncedCollection.Columns.DEVICE))
          .value(V1.SyncedCollection.Columns.COLLECTION_ID, row.getInt(V1.SyncedCollection.Columns.COLLECTION_ID))
          .value(V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION, row.getString(V1.SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION));
    }
  }

  static class V2SyncedCollection implements Table {

    @Override
    public String name() {
      return SyncedCollection.TABLE.get();
    }

    @Override
    public Statement selectStatement() {
      return select(
          SyncedCollection.Columns.USER,
          SyncedCollection.Columns.DEVICE,
          SyncedCollection.Columns.COLLECTION_ID,
          SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION).
          from(SyncedCollection.TABLE.get());
    }

    @Override
    public Statement insertStatement(Row row) {
      return insertInto(SyncedCollection.TABLE.get())
          .value(SyncedCollection.Columns.USER, row.getString(SyncedCollection.Columns.USER))
          .value(SyncedCollection.Columns.DEVICE, row.getString(SyncedCollection.Columns.DEVICE))
          .value(SyncedCollection.Columns.COLLECTION_ID, row.getInt(SyncedCollection.Columns.COLLECTION_ID))
          .value(SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION, row.getString(SyncedCollection.Columns.ANALYSED_SYNC_COLLECTION));
    }
  }

  static class V2Windowing implements Table {

    @Override
    public String name() {
      return Windowing.TABLE.get();
    }

    @Override
    public Statement selectStatement() {
      return select(
          Windowing.Columns.ID,
          Windowing.Columns.CHANGE_INDEX,
          Windowing.Columns.CHANGE_TYPE,
          Windowing.Columns.CHANGE_VALUE).
          from(Windowing.TABLE.get());
    }

    @Override
    public Statement insertStatement(Row row) {
      return insertInto(Windowing.TABLE.get())
          .value(Windowing.Columns.ID, row.getUUID(Windowing.Columns.ID))
          .value(Windowing.Columns.CHANGE_INDEX, row.getInt(Windowing.Columns.CHANGE_INDEX))
          .value(Windowing.Columns.CHANGE_TYPE, row.getString(Windowing.Columns.CHANGE_TYPE))
          .value(Windowing.Columns.CHANGE_VALUE, row.getString(Windowing.Columns.CHANGE_VALUE));
    }
  }

  static class V2WindowingIndex implements Table {

    @Override
    public String name() {
      return WindowingIndex.TABLE.get();
    }

    @Override
    public Statement selectStatement() {
      return select(
          WindowingIndex.Columns.SYNC_KEY,
          WindowingIndex.Columns.COLLECTION_ID,
          WindowingIndex.Columns.DEVICE_ID,
          WindowingIndex.Columns.USER,
          WindowingIndex.Columns.WINDOWING_ID,
          WindowingIndex.Columns.WINDOWING_INDEX,
          WindowingIndex.Columns.WINDOWING_KIND).
          from(WindowingIndex.TABLE.get());
    }

    @Override
    public Statement insertStatement(Row row) {
      return insertInto(WindowingIndex.TABLE.get())
          .value(WindowingIndex.Columns.SYNC_KEY, row.getUUID(WindowingIndex.Columns.SYNC_KEY))
          .value(WindowingIndex.Columns.COLLECTION_ID, row.getInt(WindowingIndex.Columns.COLLECTION_ID))
          .value(WindowingIndex.Columns.DEVICE_ID, row.getString(WindowingIndex.Columns.DEVICE_ID))
          .value(WindowingIndex.Columns.USER, row.getString(WindowingIndex.Columns.USER))
          .value(WindowingIndex.Columns.WINDOWING_ID, row.getUUID(WindowingIndex.Columns.WINDOWING_ID))
          .value(WindowingIndex.Columns.WINDOWING_INDEX, row.getInt(WindowingIndex.Columns.WINDOWING_INDEX))
          .value(WindowingIndex.Columns.WINDOWING_KIND, row.getString(WindowingIndex.Columns.WINDOWING_KIND));
    }
  }

  static class V2Snapshot implements Table {

    @Override
    public String name() {
      return SnapshotTable.TABLE.get();
    }

    @Override
    public Statement selectStatement() {
      return select(
          SnapshotTable.Columns.ID,
          SnapshotTable.Columns.SNAPSHOT).
          from(SnapshotTable.TABLE.get());
    }

    @Override
    public Statement insertStatement(Row row) {
      return insertInto(SnapshotTable.TABLE.get())
          .value(SnapshotTable.Columns.ID, row.getUUID(SnapshotTable.Columns.ID))
          .value(SnapshotTable.Columns.SNAPSHOT, row.getString(SnapshotTable.Columns.SNAPSHOT));
    }
  }

  static class V2SnapshotIndex implements Table {

    @Override
    public String name() {
      return SnapshotIndex.TABLE.get();
    }

    @Override
    public Statement selectStatement() {
      return select(
          SnapshotIndex.Columns.DEVICE_ID,
          SnapshotIndex.Columns.COLLECTION_ID,
          SnapshotIndex.Columns.SNAPSHOT_ID,
          SnapshotIndex.Columns.SYNC_KEY).
          from(SnapshotIndex.TABLE.get());
    }

    @Override
    public Statement insertStatement(Row row) {
      return insertInto(SnapshotIndex.TABLE.get())
          .value(SnapshotIndex.Columns.DEVICE_ID, row.getString(SnapshotIndex.Columns.DEVICE_ID))
          .value(SnapshotIndex.Columns.COLLECTION_ID, row.getInt(SnapshotIndex.Columns.COLLECTION_ID))
          .value(SnapshotIndex.Columns.SNAPSHOT_ID, row.getUUID(SnapshotIndex.Columns.SNAPSHOT_ID))
          .value(SnapshotIndex.Columns.SYNC_KEY, row.getUUID(SnapshotIndex.Columns.SYNC_KEY));
    }
  }

  public static class AlwaysRetryPolicy implements RetryPolicy {

    @Override
    public RetryDecision onReadTimeout(Statement statement, ConsistencyLevel cl, int requiredResponses, int receivedResponses, boolean dataRetrieved, int nbRetry) {
      return RetryDecision.retry(cl);
    }

    @Override
    public RetryDecision onWriteTimeout(Statement statement, ConsistencyLevel cl, WriteType writeType, int requiredAcks, int receivedAcks, int nbRetry) {
      return RetryDecision.retry(cl);
    }

    @Override
    public RetryDecision onUnavailable(Statement statement, ConsistencyLevel cl, int requiredReplica, int aliveReplica, int nbRetry) {
      return RetryDecision.retry(cl);
    }

    @Override
    public RetryDecision onRequestError(Statement statement, ConsistencyLevel cl, DriverException e, int nbRetry) {
      return RetryDecision.retry(cl);
    }

    @Override
    public void init(Cluster cluster) {

    }

    @Override
    public void close() {

    }

  }
}
