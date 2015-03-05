/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.opush.env;

import java.util.Date;

import org.easymock.IMocksControl;
import org.obm.DateUtils;
import org.obm.dbcp.DatabaseConnectionProvider;
import org.obm.dbcp.DatabaseDriverConfigurationProvider;
import org.obm.guice.AbstractOverrideModule;
import org.obm.opush.CassandraSessionSupplierImpl;
import org.obm.push.cassandra.CassandraService;
import org.obm.push.cassandra.CassandraSessionSupplier;
import org.obm.push.cassandra.dao.CassandraSchemaDao;
import org.obm.push.cassandra.migration.CodedMigrationService;
import org.obm.push.cassandra.schema.Version;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.json.JSONService;
import org.obm.sync.date.DateProvider;
import org.slf4j.Logger;

import com.datastax.driver.core.Session;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class OpushCassandraModule extends AbstractOverrideModule {

	public OpushCassandraModule(IMocksControl mocksControl) {
		super(mocksControl);
	}
	
	@Provides @Singleton
	public CassandraSchemaDao buildSchemaDao(Provider<Session> sessionProvider, JSONService jsonService, 
			@Named(LoggerModule.CASSANDRA)Logger logger,
			CassandraService cassandraService) {
		return new CassandraSchemaDao(sessionProvider, jsonService, logger, cassandraService, new DateProvider() {
			
			@Override
			public Date getDate() {
				return DateUtils.date("2004-12-14T22:00:00");
			}
		});
	}
	
	@Provides @Singleton
	public CassandraServer getCassandraServer(CassandraServerImpl cassandraServerImpl) {
		return cassandraServerImpl;
	}

	@Override
	protected void configureImpl() {
		bind(CassandraSessionSupplier.class).to(CassandraSessionSupplierImpl.class);
		bind(CodedMigrationService.class).to(NoopMigrationService.class);
	}
	
	public static class NoopMigrationService extends CodedMigrationService {

		private static final Logger logger = null;
		private static final Provider<Session> sessionProvider = null;
		private static final DatabaseConnectionProvider dbcp = null;
		private static final DatabaseDriverConfigurationProvider configurationProvider = null;
		
		NoopMigrationService() {
			super(logger, sessionProvider, dbcp, configurationProvider);
		}

		@Override
		public void migrate(Version currentVersion, Version toVersion) {
			// nothing to do
		}
	}

}