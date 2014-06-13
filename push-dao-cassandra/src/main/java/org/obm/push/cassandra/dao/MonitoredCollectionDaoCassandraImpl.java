/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2013  Linagora
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

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.obm.push.cassandra.dao.CassandraStructure.MonitoredCollection.TABLE;
import static org.obm.push.cassandra.dao.CassandraStructure.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS;
import static org.obm.push.cassandra.dao.CassandraStructure.MonitoredCollection.Columns.DEVICE;
import static org.obm.push.cassandra.dao.CassandraStructure.MonitoredCollection.Columns.USER;

import java.util.Set;

import org.obm.breakdownduration.bean.Watch;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.User;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.json.JSONService;
import org.obm.push.store.MonitoredCollectionDao;
import org.slf4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
@Watch(BreakdownGroups.CASSANDRA)
public class MonitoredCollectionDaoCassandraImpl extends AbstractCassandraDao implements MonitoredCollectionDao, CassandraStructure, CassandraDao {

	@Inject  
	@VisibleForTesting MonitoredCollectionDaoCassandraImpl(Provider<Session> sessionProvider, 
			JSONService jsonService, @Named(LoggerModule.CASSANDRA)Logger logger) {
		super(sessionProvider, jsonService, logger);
	}

	@Override
	public Set<AnalysedSyncCollection> list(Credentials credentials, Device device) {
		Optional<Set<AnalysedSyncCollection>> result = listV2(credentials.getUser(), device);
		if (result.isPresent()) {
			return result.get();
		}
		return listV1(credentials, device);
	}

	private Optional<Set<AnalysedSyncCollection>> listV2(User user, Device device) {
		Where query = select(ANALYSED_SYNC_COLLECTIONS).from(TABLE.get())
				.where(eq(USER, user.getLoginAtDomain()))
				.and(eq(DEVICE, jsonService.serialize(device)));
		logger.debug("Getting {}", query.getQueryString());
		ResultSet resultSet = getSession().execute(query);
		if (resultSet.isExhausted()) {
			logger.debug("No result found, returning empty set");
			return Optional.absent();
		}
		Set<String> jsons = resultSet.one().getSet(ANALYSED_SYNC_COLLECTIONS, String.class);
		logger.debug("Result found {}", jsons);
		return Optional.of(jsonService.deserializeSet(AnalysedSyncCollection.class, jsons));
	}

	private Set<AnalysedSyncCollection> listV1(Credentials credentials, Device device) {
		Where query = select(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS)
				.from(V1.MonitoredCollection.TABLE.get())
				.where(eq(V1.MonitoredCollection.Columns.CREDENTIALS, jsonService.serialize(credentials)))
				.and(eq(V1.MonitoredCollection.Columns.DEVICE, jsonService.serialize(device)));
		logger.debug("Getting {}", query.getQueryString());
		ResultSet resultSet = getSession().execute(query);
		if (resultSet.isExhausted()) {
			logger.debug("No result found, returning empty set");
			return ImmutableSet.<AnalysedSyncCollection> of();
		}
		Set<String> jsons = resultSet.one().getSet(V1.MonitoredCollection.Columns.ANALYSED_SYNC_COLLECTIONS, String.class);
		logger.debug("Result found {}", jsons);
		return jsonService.deserializeSet(AnalysedSyncCollection.class, jsons);
	}

	@Override
	public void put(User user, Device device, Set<AnalysedSyncCollection> collections) {
		Insert query = insertInto(TABLE.get())
				.value(USER, user.getLoginAtDomain())
				.value(DEVICE, jsonService.serialize(device))
				.value(ANALYSED_SYNC_COLLECTIONS, jsonService.serializeSet(collections));
		logger.debug("Inserting {}", query.getQueryString());
		getSession().execute(query);
	}
}
