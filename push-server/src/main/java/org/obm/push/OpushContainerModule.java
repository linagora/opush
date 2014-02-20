/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2011-2014  Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version, provided you comply with the Additional Terms applicable for OBM
 * software by Linagora pursuant to Section 7 of the GNU Affero General Public
 * License, subsections (b), (c), and (e), pursuant to which you must notably (i)
 * retain the displaying by the interactive user interfaces of the “OBM, Free
 * Communication by Linagora” Logo with the “You are using the Open Source and
 * free version of OBM developed and supported by Linagora. Contribute to OBM R&D
 * by subscribing to an Enterprise offer !” infobox, (ii) retain all hypertext
 * links between OBM and obm.org, between Linagora and linagora.com, as well as
 * between the expression “Enterprise offer” and pro.obm.org, and (iii) refrain
 * from infringing Linagora intellectual property rights over its trademarks and
 * commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License and
 * its applicable Additional Terms for OBM along with this program. If not, see
 * <http://www.gnu.org/licenses/> for the GNU Affero General   Public License
 * version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
 * applicable to the OBM software.
 * ***** END LICENSE BLOCK ***** */
package org.obm.push;

import java.util.EnumSet;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.obm.configuration.VMArgumentsUtils;
import org.obm.push.configuration.LoggerModule;
import org.obm.sync.LifecycleListenerHelper;
import org.obm.sync.XTrustProvider;
import org.slf4j.Logger;

import com.google.common.base.Objects;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;

public class OpushContainerModule extends AbstractModule {

	private static final long GRACEFUL_STOP_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(1);
	private static final int JETTY_SELECTED_PORT = 0;
	private static final int POOL_THREAD_SIZE = Objects.firstNonNull( 
			VMArgumentsUtils.integerArgumentValue("threadPoolSize"), 200);

	private final int port;

	public OpushContainerModule() {
		this(JETTY_SELECTED_PORT);
	}
	
	public OpushContainerModule(int port) {
		this.port = port;
		XTrustProvider.install();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
	}
	
	@Override
	protected void configure() {}

	@Provides @Singleton
	protected OpushServer buildServer(Injector injector) {
		
		final Server jetty = new Server(new QueuedThreadPool(POOL_THREAD_SIZE));
		jetty.setStopAtShutdown(true);
		jetty.setStopTimeout(GRACEFUL_STOP_TIMEOUT_MS);
		
		final ServerConnector httpConnector = new ServerConnector(jetty, new HttpConnectionFactory());
		httpConnector.setPort(port);
		jetty.addConnector(httpConnector);
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addFilter(new FilterHolder(injector.getInstance(GuiceFilter.class)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addServlet(DefaultServlet.class, "/");
		context.addEventListener(buildCleanupListener(injector));
		context.addLifeCycleListener(buildLifeCycleListener(injector));
		jetty.setHandler(context);
		
		return new OpushServer() {
			
			@Override
			public void stop() throws Exception {
				jetty.stop();
			}
			
			@Override
			public void start() throws Exception {
				jetty.start();
			}
			
			@Override
			public void join() throws Exception {
				jetty.join();
			}

			@Override
			public int getPort() {
				if (jetty.isRunning()) {
					return httpConnector.getLocalPort();
				}
				throw new IllegalStateException("Could not get server's listening port. Start the server first.");
			}
		};
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

	private Listener buildLifeCycleListener(Injector injector) {
		final Logger logger = injector.getInstance(Key.get(Logger.class, Names.named(LoggerModule.CONTAINER)));
		return new Listener() {
			@Override
			public void lifeCycleStopping(LifeCycle event) {
				logger.info("Application stopping");
			}
			@Override
			public void lifeCycleStopped(LifeCycle event) {
				logger.info("Application stopped");
			}
			@Override
			public void lifeCycleStarting(LifeCycle event) {
				logger.info("Application starting");
			}
			@Override
			public void lifeCycleStarted(LifeCycle event) {
				logger.info("Application started");
			}
			@Override
			public void lifeCycleFailure(LifeCycle event, Throwable cause) {
				logger.error("Application failure", cause);
			}
		};
	}
}
