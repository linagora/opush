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
package org.obm.push;

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.sql.Connection;
import java.sql.SQLException;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.dbcp.DatabaseConnectionProvider;
import org.obm.push.ServerFactoryModule.NoopServer;
import org.obm.push.bean.migration.StatusSummary;
import org.obm.push.bean.migration.StatusSummary.Status;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.migration.OpushMigrationService;
import org.slf4j.Logger;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;


public class ServerFactoryModuleTest {

	private IMocksControl mocks;
	private Injector injector;
	private OpushMigrationService cassandraSchemaService;
	private Logger logger;
	private OpushJettyServerFactory jettyFactory;
	private OpushServer opushServer;
	private NoopServer noopServer;
	private ServerConfiguration serverConfiguration;
	private DatabaseConnectionProvider databaseConnectionProvider;
	private Connection databaseConnection;

	@Before
	public void setUp() {
		serverConfiguration = ServerConfiguration.builder().port(9999).threadPoolSize(2).selectorCount(1).build();
		
		mocks = createControl();
		injector = mocks.createMock(Injector.class);
		cassandraSchemaService = mocks.createMock(OpushMigrationService.class);
		logger = mocks.createMock(Logger.class);
		jettyFactory = mocks.createMock(OpushJettyServerFactory.class);
		noopServer = mocks.createMock(NoopServer.class);
		opushServer = mocks.createMock(OpushServer.class);
		databaseConnectionProvider = mocks.createMock(DatabaseConnectionProvider.class);
		databaseConnection = mocks.createMock(Connection.class);
		
		expect(injector.getInstance(OpushMigrationService.class)).andReturn(cassandraSchemaService).times(0, 1);
		expect(injector.getInstance(Key.get(Logger.class, Names.named(LoggerModule.CONTAINER)))).andReturn(logger);
		expect(injector.getInstance(DatabaseConnectionProvider.class)).andReturn(databaseConnectionProvider);
		
		logger.warn("Checking for database connection...");
		expectLastCall();
	}
	
	@Test
	public void printNoLogWhenUpToDate() throws SQLException {
		StatusSummary status = StatusSummary.status(Status.UP_TO_DATE).build();
		expect(databaseConnectionProvider.getConnection()).andReturn(databaseConnection);
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(OpushJettyServerFactory.class)).andReturn(jettyFactory);
		expect(jettyFactory.buildServer(
				serverConfiguration.port(), serverConfiguration.threadPoolSize(), serverConfiguration.selectorCount()))
				.andReturn(opushServer);
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, serverConfiguration).createServer();
		mocks.verify();
	}

	@Test
	public void printLogWhenUpdateAvailable() throws SQLException {
		StatusSummary status = StatusSummary.status(Status.UPGRADE_AVAILABLE).build();
		expect(databaseConnectionProvider.getConnection()).andReturn(databaseConnection);
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(OpushJettyServerFactory.class)).andReturn(jettyFactory);
		expect(jettyFactory.buildServer(
				serverConfiguration.port(), serverConfiguration.threadPoolSize(), serverConfiguration.selectorCount()))
				.andReturn(opushServer);
		
		logger.warn("Cassandra schema not up-to-date, update is advised");
		expectLastCall();
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, serverConfiguration).createServer();
		mocks.verify();
	}

	@Test
	public void printLogWhenUpdateRequired() throws SQLException {
		StatusSummary status = StatusSummary.status(Status.UPGRADE_REQUIRED).build();
		expect(databaseConnectionProvider.getConnection()).andReturn(databaseConnection);
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(NoopServer.class)).andReturn(noopServer);
		
		logger.error("Cassandra schema too old, starting administration services only");
		expectLastCall();
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, serverConfiguration).createServer();
		mocks.verify();
	}
	
	@Test
	public void printLogWhenNoSchema() throws SQLException {
		StatusSummary status = StatusSummary.status(Status.NOT_INITIALIZED).build();
		expect(databaseConnectionProvider.getConnection()).andReturn(databaseConnection);
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(NoopServer.class)).andReturn(noopServer);
		
		logger.error("Cassandra schema not installed, starting administration services only");
		expectLastCall();
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, serverConfiguration).createServer();
		mocks.verify();
	}

	@Test
	public void printLogWhenExecutionError() throws SQLException {
		StatusSummary status = StatusSummary.status(Status.EXECUTION_ERROR).message("expected message").build();
		expect(databaseConnectionProvider.getConnection()).andReturn(databaseConnection);
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(NoopServer.class)).andReturn(noopServer);
		
		logger.error("{}, starting administration services only", "expected message");
		expectLastCall();
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, serverConfiguration).createServer();
		mocks.verify();
	}

	@Test(expected=RuntimeException.class)
	public void printLogAndTriggerExceptionWhenNoDBConnection() throws SQLException {
		expect(databaseConnectionProvider.getConnection()).andThrow(new SQLException("reason"));

		logger.error("Cannot get database connection, verify your configuration then restart opush");
		expectLastCall();
		
		mocks.replay();
		try {
			new ServerFactoryModule.LateInjectionServer(injector, serverConfiguration).createServer();
		} finally {
			mocks.verify();
		}
	}
}
