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
package org.obm.push.cassandra.dao;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.obm.breakdownduration.bean.Watch;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.migration.NoVersionException;
import org.obm.push.bean.migration.Version;
import org.obm.push.bean.migration.VersionUpdate;
import org.obm.push.cassandra.CassandraService;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.json.JSONService;
import org.obm.push.store.SchemaDao;
import org.obm.sync.date.DateProvider;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.desc;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.obm.push.cassandra.dao.CassandraStructure.Schema.Columns.DATE;
import static org.obm.push.cassandra.dao.CassandraStructure.Schema.Columns.ID;
import static org.obm.push.cassandra.dao.CassandraStructure.Schema.Columns.VERSION;
import static org.obm.push.cassandra.dao.CassandraStructure.Schema.TABLE;

@Singleton
@Watch(BreakdownGroups.CASSANDRA)
public class CassandraSchemaDao extends AbstractCassandraDao implements CassandraDao, SchemaDao {

  private static final int SINGLE_ROW_ID = 1;
  private static final int ONE_RESULT = 1;

  private final CassandraService cassandraService;
  private final DateProvider dateProvider;

  @Inject
  @VisibleForTesting
  public CassandraSchemaDao(Provider<Session> sessionProvider, JSONService jsonService,
                            @Named(LoggerModule.CASSANDRA) Logger logger,
                            CassandraService cassandraService,
                            DateProvider dateProvider) {
    super(sessionProvider, jsonService, logger);
    this.cassandraService = cassandraService;
    this.dateProvider = dateProvider;
  }

  @Override
  public VersionUpdate getCurrentVersion() {
    cassandraService.errorIfNoTable(TABLE.get());
    Select query = select(VERSION, DATE).from(TABLE.get())
        .where(eq(ID, SINGLE_ROW_ID))
        .limit(ONE_RESULT)
        .orderBy(desc(VERSION));
    logger.debug("perform getCurrentVersion request {}", query.getQueryString());

    ResultSet resultSet = getSession().execute(query);
    if (resultSet.isExhausted()) {
      throw new NoVersionException();
    }

    VersionUpdate schemaUpdate = rowToSchemaUpdate(resultSet.one());
    logger.debug("Current version found {}", schemaUpdate);
    return schemaUpdate;
  }

  @Override
  public List<VersionUpdate> getHistory() {
    cassandraService.errorIfNoTable(TABLE.get());
    Select query = select(VERSION, DATE).from(TABLE.get())
        .where(eq(ID, SINGLE_ROW_ID))
        .orderBy(desc(VERSION));
    logger.debug("perform getHistory request {}", query.getQueryString());

    ImmutableList.Builder<VersionUpdate> historyBuilder = ImmutableList.builder();
    Iterator<Row> results = getSession().execute(query).iterator();
    while (results.hasNext()) {
      historyBuilder.add(rowToSchemaUpdate(results.next()));
    }
    return historyBuilder.build();
  }

  @Override
  public void updateVersion(Version target) {
    cassandraService.errorIfNoTable(TABLE.get());
    Insert query = insertInto(TABLE.get())
        .value(ID, SINGLE_ROW_ID)
        .value(VERSION, target.get())
        .value(DATE, dateProvider.getDate());
    logger.debug("perform updateVersion request {}", query.getQueryString());
    getSession().execute(query);
  }

  private VersionUpdate rowToSchemaUpdate(Row schemaUpdateRow) {
    return VersionUpdate
        .version(Version.of(schemaUpdateRow.getInt(VERSION)))
        .date(schemaUpdateRow.getTimestamp(DATE));
  }
}
