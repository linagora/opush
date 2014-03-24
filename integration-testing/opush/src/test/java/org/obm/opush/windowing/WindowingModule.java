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
package org.obm.opush.windowing;

import static org.easymock.EasyMock.createControl;

import java.util.concurrent.TimeUnit;

import org.obm.StaticConfigurationService;
import org.obm.configuration.TransactionConfiguration;
import org.obm.opush.env.OpushConfigurationFixture;
import org.obm.opush.env.OpushStaticConfiguration;
import org.obm.push.cassandra.CassandraSessionProvider;
import org.obm.push.cassandra.OpushCassandraModule;
import org.obm.push.cassandra.dao.WindowingDaoCassandraImpl;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.configuration.OpushConfiguration;
import org.obm.push.store.WindowingDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Session;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

public class WindowingModule extends AbstractModule {

	private final Logger logger;

	public WindowingModule() {
		logger = LoggerFactory.getLogger(getClass());
	}
	
	@Override
	protected void configure() {
		OpushConfigurationFixture configuration = configuration();
		install(Modules.override(new OpushCassandraModule())
				.with(new org.obm.opush.env.OpushCassandraModule(createControl())));
		bind(Session.class).toProvider(CassandraSessionProvider.class);
		bind(OpushConfiguration.class).toInstance(new OpushStaticConfiguration(configuration));
		bind(TransactionConfiguration.class).toInstance(new StaticConfigurationService.Transaction(configuration.transaction));
		bind(WindowingDao.class).to(WindowingDaoCassandraImpl.class);
		bind(Logger.class).annotatedWith(Names.named(LoggerModule.CONFIGURATION)).toInstance(logger);
		bind(Logger.class).annotatedWith(Names.named(LoggerModule.CASSANDRA)).toInstance(logger);
	}		

	protected OpushConfigurationFixture configuration() {
		OpushConfigurationFixture configuration = new OpushConfigurationFixture();
		configuration.transaction.timeoutInSeconds = Ints.checkedCast(TimeUnit.MINUTES.toSeconds(10));
		configuration.dataDir = Files.createTempDir();
		return configuration;
	}

}
