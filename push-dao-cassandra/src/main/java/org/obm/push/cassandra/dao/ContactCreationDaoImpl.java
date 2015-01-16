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
package org.obm.push.cassandra.dao;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.obm.push.cassandra.dao.CassandraStructure.ContactCreation.TABLE;
import static org.obm.push.cassandra.dao.CassandraStructure.ContactCreation.Columns.COLLECTION_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.ContactCreation.Columns.DEVICE_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.ContactCreation.Columns.HASH;
import static org.obm.push.cassandra.dao.CassandraStructure.ContactCreation.Columns.SERVER_ID;
import static org.obm.push.cassandra.dao.CassandraStructure.ContactCreation.Columns.USER;

import org.obm.breakdownduration.bean.Watch;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.User;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.json.JSONService;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.store.ContactCreationDao;
import org.slf4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.hash.HashCode;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
@Watch(BreakdownGroups.CASSANDRA)
public class ContactCreationDaoImpl extends AbstractCassandraDao implements ContactCreationDao, CassandraStructure, CassandraDao {

	@Inject
	@VisibleForTesting ContactCreationDaoImpl(Provider<Session> sessionProvider, 
			JSONService jsonService, @Named(LoggerModule.CASSANDRA)Logger logger) {
		super(sessionProvider, jsonService, logger);
	}

	@Override
	public void registerCreation(User user, DeviceId device, CollectionId collectionId, HashCode hash, ServerId serverId) {
		Insert query = insertInto(TABLE.get())
				.value(USER, user.getLoginAtDomain())
				.value(DEVICE_ID, device.getDeviceId())
				.value(COLLECTION_ID, collectionId.asInt())
				.value(SERVER_ID, serverId.asString())
				.value(HASH, hash.toString());
		logger.debug("Inserting contact creation {}", query.getQueryString());
		getSession().execute(query);
	}

	@Override
	public Optional<ServerId> find(User user, DeviceId device, CollectionId collectionId, HashCode hash) {
		Where statement = select(SERVER_ID).from(TABLE.get())
				.where(eq(USER, user.getLoginAtDomain()))
				.and(eq(DEVICE_ID, device.getDeviceId()))
				.and(eq(COLLECTION_ID, collectionId.asInt()))
				.and(eq(HASH, hash.toString()));
		logger.debug("Selecting contact creation: {}", statement.getQueryString());

		ResultSet results = getSession().execute(statement);
		if (results.isExhausted()) {
			return Optional.absent();
		}
		return Optional.of(ServerId.of(results.one().getString(SERVER_ID)));
	}

}
