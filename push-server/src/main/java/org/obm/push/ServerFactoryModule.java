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

import java.util.TimeZone;

import org.obm.push.cassandra.schema.CassandraSchemaService;
import org.obm.push.cassandra.schema.StatusSummary;
import org.obm.push.cassandra.schema.StatusSummary.Status;
import org.obm.push.configuration.LoggerModule;
import org.obm.sync.LifecycleListenerHelper;
import org.obm.sync.XTrustProvider;
import org.slf4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class ServerFactoryModule extends AbstractModule {

	static {
		XTrustProvider.install();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
	}
	
	private static final int JETTY_SELECTED_PORT = 0;
	private final int port;
	
	public ServerFactoryModule() {
		this(JETTY_SELECTED_PORT);
	}

	public ServerFactoryModule(int port) {
		this.port = port;
	}
	
	@Override
	protected void configure() {
	}
	
	@Provides @Singleton
	public OpushServer createServer(Injector injector) {
		return new LateInjectionServer(injector, port);
	}
	
	public static class LateInjectionServer implements OpushServer {

		private final Injector injector;
		private final int port;
		private final Supplier<OpushServer> realServerSupplier;

		public LateInjectionServer(Injector injector, int port) {
			this.injector = injector;
			this.port = port;
			this.realServerSupplier = Suppliers.memoize(new Supplier<OpushServer>() {
				
				@Override
				public OpushServer get() {
					return createServer();
				}
			});
		}
		
		@VisibleForTesting OpushServer createServer() {
			StatusSummary statusSummary = injector.getInstance(CassandraSchemaService.class).getStatus();
			Logger logger = injector.getInstance(Key.get(Logger.class, Names.named(LoggerModule.CONTAINER)));
			if (statusSummary.getStatus().allowsStartup()) {
				return createJettyServer(statusSummary, logger);
			} else {
				logger.error("Cassandra schema not installed or too old, starting administration services only");
				return injector.getInstance(NoopServer.class);
			}
		}

		private OpushServer createJettyServer(StatusSummary statusSummary, Logger logger) {
			if (!statusSummary.getStatus().equals(Status.UP_TO_DATE)) {
				logger.warn("Cassandra schema not up-to-date, the update is advised");
			}
			return injector.getInstance(OpushJettyServerFactory.class).buildServer(port);
		}
		
		private OpushServer getRealServer() {
			return realServerSupplier.get(); 
		}
		
		@Override
		public void start() throws Exception {
			getRealServer().start();
		}
		
		@Override
		public void stop() throws Exception {
			getRealServer().stop();
		}
		
		@Override
		public int getHttpPort() {
			return getRealServer().getHttpPort();
		}
		
		@Override
		public void join() throws Exception {
			getRealServer().join();
		}
	}
	
	public static class NoopServer implements OpushServer {
		
		private Injector injector;

		@Inject
		private NoopServer(Injector injector) {
			this.injector = injector;
		}
		
		@Override
		public void start() {
		}

		@Override
		public void stop() {
			LifecycleListenerHelper.shutdownListeners(injector);
		}

		@Override
		public void join() throws InterruptedException {
			Thread.currentThread().join();
		}

		@Override
		public int getHttpPort() {
			throw new IllegalStateException("not in a http context");
		}
	}
}
