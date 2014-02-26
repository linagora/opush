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
package org.obm.push.spushnik;

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import com.google.inject.Module;

public class SpushnikServer {
	
	public static final int JETTY_SELECTED_PORT = 0;
	private static final long GRACEFUL_STOP_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);
	private static final int POOL_THREAD_SIZE = 10;

	private final Server jetty;
	private final ServerConnector httpConnector;

	public SpushnikServer(Class<? extends Module> module) {
		this(JETTY_SELECTED_PORT, module);
	}
	
	public SpushnikServer(int port, Class<? extends Module> module) {
		jetty = new Server(new QueuedThreadPool(POOL_THREAD_SIZE));
		jetty.setStopAtShutdown(true);
		jetty.setStopTimeout(GRACEFUL_STOP_TIMEOUT_MS);
		
		httpConnector = new ServerConnector(jetty, new HttpConnectionFactory());
		httpConnector.setPort(port);
		jetty.addConnector(httpConnector);
		jetty.setHandler(buildHandlers(module));
	}
	

	private Handler buildHandlers(Class<? extends Module> module) {
		HandlerCollection handlers = new HandlerCollection();
		handlers.addHandler(buildServletContext(module));
		return handlers;
	}
	
	private ServletContextHandler buildServletContext(Class<? extends Module> module) {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/spushnik/");
		context.setInitParameter("resteasy.guice.modules", module.getCanonicalName());
		context.addServlet(HttpServletDispatcher.class, "/*");
		context.addEventListener(new GuiceResteasyBootstrapServletContextListener());
		return context;
	}

	public void stop() throws Exception {
		jetty.stop();
	}


	public void start() throws Exception {
		jetty.start();
	}

	public void join() throws Exception {
		jetty.join();
	}

	public int getPort() {
		if (jetty.isRunning()) {
			return httpConnector.getLocalPort();
		}
		throw new IllegalStateException("Could not get server's listening port. Start the server first.");
	}

}
