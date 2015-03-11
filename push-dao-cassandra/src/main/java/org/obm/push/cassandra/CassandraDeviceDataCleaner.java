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

import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

import org.obm.breakdownduration.bean.Watch;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.User;
import org.obm.push.cassandra.dao.CassandraStructure;
import org.obm.push.cassandra.dao.CassandraStructure.FolderMapping;
import org.obm.push.cassandra.dao.CassandraStructure.FolderSnapshot;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.exception.DaoException;
import org.obm.push.service.DeviceDataCleaner;
import org.slf4j.Logger;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
@Watch(BreakdownGroups.CASSANDRA)
public class CassandraDeviceDataCleaner implements DeviceDataCleaner {

	private final Logger logger;
	private final Provider<Session> sessionProvider;

	@Inject  
	@VisibleForTesting CassandraDeviceDataCleaner(
			@Named(LoggerModule.CASSANDRA)Logger logger,
			Provider<Session> sessionProvider) {
		this.sessionProvider = sessionProvider;
		this.logger = logger;
	}

	@Override
	public void clean(User user, DeviceId deviceId) throws DaoException {
		try {
			cleanFolderSnapshot(user, deviceId);
			cleanFolderMapping(user, deviceId);
		} catch (RuntimeException e) {
			throw new DaoException("An error occured during the user-device cleaning", e);
		}
	}

	private void cleanFolderSnapshot(User user, DeviceId deviceId) {
		logger.debug("Cleaning {}", CassandraStructure.FolderSnapshot.TABLE.get());
		Statement statement = delete()
			.from(FolderSnapshot.TABLE.get())
			.where(eq(FolderSnapshot.Columns.USER, user.getLoginAtDomain()))
			.and(eq(FolderSnapshot.Columns.DEVICE_ID, deviceId.getDeviceId()));
		sessionProvider.get().execute(statement);
	}
	
	private void cleanFolderMapping(User user, DeviceId deviceId) {
		logger.debug("Cleaning {}", CassandraStructure.FolderMapping.TABLE.get());
		Statement statement = delete()
				.from(FolderMapping.TABLE.get())
				.where(eq(FolderMapping.Columns.USER, user.getLoginAtDomain()))
				.and(eq(FolderMapping.Columns.DEVICE_ID, deviceId.getDeviceId()));
		sessionProvider.get().execute(statement);
	}

}
