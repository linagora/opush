/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013-2014  Linagora
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

import java.nio.file.Paths;

import org.obm.configuration.ConfigurationService;
import org.obm.configuration.GlobalAppConfiguration;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.configuration.OpushConfiguration;
import org.obm.push.configuration.OpushConfigurationLoader;
import org.obm.push.utils.jvm.VMArgumentsUtils;
import org.slf4j.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public class OpushServerLauncher {

	private static final String OPUSH_CONFIGURATION_PATH = "/etc/opush/opush.ini";
	private static final int DEFAULT_SERVER_PORT = 8082; 
	private static final int SERVER_PORT = Objects.firstNonNull( 
			VMArgumentsUtils.integerArgumentValue("opushPort"), DEFAULT_SERVER_PORT);

	public static void main(String... args) {
		/******************************************************************
		 * EVERY CHANGES DONE THERE CAN SILENTLY BREAK THE OPUSH START UP *
		 ******************************************************************/
		GlobalAppConfiguration<OpushConfiguration> configuration = 
				OpushConfigurationLoader.loadFromFiles(
					Paths.get(OPUSH_CONFIGURATION_PATH), 
					Paths.get(ConfigurationService.GLOBAL_OBM_CONFIGURATION_PATH));
		
		
		Injector injector = Guice.createInjector(new ServerFactoryModule(
				ServerConfiguration.builder().port(SERVER_PORT).build()), new OpushModule(configuration));
		OpushServer opushServer = injector.getInstance(OpushServer.class);
		
		try {
			start(opushServer).join();
		} catch (Exception e) {
			Logger logger = injector.getInstance(Key.get(Logger.class, Names.named(LoggerModule.CONTAINER)));
			logger.error("Unable to start opush, exiting. {}", e.getMessage());
			Runtime.getRuntime().exit(1);
		}
	}

	public static OpushServer start(OpushServer server) throws Exception {
		server.start();
		registerSigTermHandler(server);
		return server;
	}

	private static void registerSigTermHandler(final OpushServer server) {
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				try {
					server.stop();
				} catch (Exception e) {
					Throwables.propagate(e);
				}
			}
		});
	}
}
