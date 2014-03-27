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

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.ServerFactoryModule.NoopServer;
import org.obm.push.cassandra.schema.CassandraSchemaService;
import org.obm.push.cassandra.schema.StatusSummary;
import org.obm.push.cassandra.schema.StatusSummary.Status;
import org.obm.push.configuration.LoggerModule;
import org.slf4j.Logger;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;


public class ServerFactoryModuleTest {

	private int port;
	private IMocksControl mocks;
	private Injector injector;
	private CassandraSchemaService cassandraSchemaService;
	private Logger logger;
	private OpushJettyServerFactory jettyFactory;
	private OpushServer opushServer;
	private NoopServer noopServer;

	@Before
	public void setUp() {
		port = 9999;

		mocks = createControl();
		injector = mocks.createMock(Injector.class);
		cassandraSchemaService = mocks.createMock(CassandraSchemaService.class);
		logger = mocks.createMock(Logger.class);
		jettyFactory = mocks.createMock(OpushJettyServerFactory.class);
		noopServer = mocks.createMock(NoopServer.class);
		opushServer = mocks.createMock(OpushServer.class);
		
		expect(injector.getInstance(CassandraSchemaService.class)).andReturn(cassandraSchemaService);
		expect(injector.getInstance(Key.get(Logger.class, Names.named(LoggerModule.CONTAINER)))).andReturn(logger);
	}
	
	@Test
	public void printNoLogWhenUpToDate() {
		StatusSummary status = StatusSummary.status(Status.UP_TO_DATE).build();
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(OpushJettyServerFactory.class)).andReturn(jettyFactory);
		expect(jettyFactory.buildServer(port)).andReturn(opushServer);
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, port).createServer();
		mocks.verify();
	}

	@Test
	public void printLogWhenUpdateAvailable() {
		StatusSummary status = StatusSummary.status(Status.UPGRADE_AVAILABLE).build();
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(OpushJettyServerFactory.class)).andReturn(jettyFactory);
		expect(jettyFactory.buildServer(port)).andReturn(opushServer);
		
		logger.warn("Cassandra schema not up-to-date, update is advised");
		expectLastCall();
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, port).createServer();
		mocks.verify();
	}

	@Test
	public void printLogWhenUpdateRequired() {
		StatusSummary status = StatusSummary.status(Status.UPGRADE_REQUIRED).build();
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(NoopServer.class)).andReturn(noopServer);
		
		logger.error("Cassandra schema too old, starting administration services only");
		expectLastCall();
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, port).createServer();
		mocks.verify();
	}
	
	@Test
	public void printLogWhenNoSchema() {
		StatusSummary status = StatusSummary.status(Status.NOT_INITIALIZED).build();
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(NoopServer.class)).andReturn(noopServer);
		
		logger.error("Cassandra schema not installed, starting administration services only");
		expectLastCall();
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, port).createServer();
		mocks.verify();
	}

	@Test
	public void printLogWhenExecutionError() {
		StatusSummary status = StatusSummary.status(Status.EXECUTION_ERROR).message("expected message").build();
		expect(cassandraSchemaService.getStatus()).andReturn(status);
		expect(injector.getInstance(NoopServer.class)).andReturn(noopServer);
		
		logger.error("{}, starting administration services only", "expected message");
		expectLastCall();
		
		mocks.replay();
		new ServerFactoryModule.LateInjectionServer(injector, port).createServer();
		mocks.verify();
	}
}
