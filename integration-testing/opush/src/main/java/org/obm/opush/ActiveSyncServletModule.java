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
package org.obm.opush;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.obm.configuration.GlobalAppConfiguration;
import org.obm.push.OpushModule;
import org.obm.push.configuration.BackendConfiguration;
import org.obm.push.configuration.OpushConfiguration;
import org.obm.push.utils.DOMUtils;
import org.obm.sync.LifecycleListenerHelper;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import com.google.inject.util.Modules;
import com.google.inject.util.Modules.OverriddenModuleBuilder;

public abstract class ActiveSyncServletModule extends AbstractModule {

	@Inject DOMUtils domUtils;
	
	protected abstract GlobalAppConfiguration<OpushConfiguration> opushConfiguration();
	protected abstract BackendConfiguration backendConfiguration();
	protected abstract Module overrideModule() throws Exception;
	protected abstract void onModuleInstalled();
	
	protected void configure() {
		OverriddenModuleBuilder override = Modules.override(new OpushModule(opushConfiguration(), backendConfiguration(), noDatabase()), new PendingQueryFilterModule());
		try {
			install(override.with(overrideModule()));
			onModuleInstalled();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static class PendingQueryFilterModule extends ServletModule {
		
		@Override
		protected void configureServlets() {
			filter("/*").through(PendingQueryFilter.class);
		}
		
	}
	
	private Module noDatabase() {
		return Modules.EMPTY_MODULE;
	}
	
	@Provides @Singleton
	protected OpushServer buildOpushServer(Injector injector) {
		return new OpushServer(injector);
	}
	
	public static class OpushServer {
		
		private final Server server;
		private final ServerConnector httpConnector;

		public OpushServer(Injector injector) {
			server = new Server();
			
			httpConnector = new ServerConnector(server, new HttpConnectionFactory());
			server.addConnector(httpConnector);
			
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			context.addFilter(new FilterHolder(injector.getInstance(GuiceFilter.class)), "/*", EnumSet.allOf(DispatcherType.class));
			context.addServlet(DefaultServlet.class, "/");
			context.addEventListener(buildCleanupListener(injector));
			server.setHandler(context);
		}

		private ServletContextListener buildCleanupListener(final Injector injector) {
			return new ServletContextListener() {

				@Override
				public void contextInitialized(ServletContextEvent sce) {
				}

				@Override
				public void contextDestroyed(ServletContextEvent sce) {
					LifecycleListenerHelper.shutdownListeners(injector);
				}
			};
		}

		public void start() throws Exception {
			server.start();
		}

		public void stop() throws Exception {
			server.stop();
		}
		
		public int getPort() {
			if (server.isRunning()) {
				return getLocalPort();
			}
			throw new IllegalStateException("Could not get server's listening port. Start the server first.");
		}

		private int getLocalPort() {
			int port = httpConnector.getLocalPort();
			if (port > 0) {
				return port;
			}
			throw new IllegalStateException("Could not get server's listening port. Received port is " + port);
		}
	}

}
