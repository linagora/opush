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
package org.obm.push.configuration;

import java.nio.file.Path;


import org.obm.configuration.DatabaseConfiguration;
import org.obm.configuration.DatabaseConfigurationImpl;
import org.obm.configuration.DefaultTransactionConfiguration;
import org.obm.configuration.GlobalAppConfiguration;
import org.obm.configuration.LocatorConfiguration;
import org.obm.configuration.LocatorConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.net.InetAddresses;

public class OpushConfigurationLoader {

	private static Logger LOGGER = LoggerFactory.getLogger(LoggerModule.CONFIGURATION);
	
	private static final String APPLICATION_NAME = "opush";
	
	private static GlobalAppConfiguration<OpushConfiguration> buildConfiguration(Path configurationFile) 
			throws javax.naming.ConfigurationException {
		
		OpushConfigurationImpl mainConfiguration = 
				new OpushConfigurationImpl.Factory().create(configurationFile.toString(), APPLICATION_NAME);
		GlobalAppConfiguration<OpushConfiguration> globalAppConfiguration = GlobalAppConfiguration.<OpushConfiguration>builder()
				.mainConfiguration(mainConfiguration)
				.locatorConfiguration(new LocatorConfigurationImpl.Factory().create(configurationFile.toString()))
				.databaseConfiguration(new DatabaseConfigurationImpl.Factory().create(configurationFile.toString()))
				.transactionConfiguration(new DefaultTransactionConfiguration.Factory().create(APPLICATION_NAME, mainConfiguration))
				.build();
		checkMandatoryParameters(configurationFile, globalAppConfiguration);
		return globalAppConfiguration;
	}

	private static void checkMandatoryParameters(Path file,
			GlobalAppConfiguration<OpushConfiguration> configuration) throws javax.naming.ConfigurationException {
		checkMandatoryDatabaseConfiguration(file, configuration.getDatabaseConfiguration());
		checkMandatoryLocatorConfiguration(file, configuration.getLocatorConfiguration());
		checkMandatoryMainConfiguration(file, configuration.getConfiguration());
	}

	private static void checkMandatoryDatabaseConfiguration(Path file, DatabaseConfiguration databaseConfiguration) {
		checkConfigurationEntry(file, "host",
				databaseConfiguration.getDatabaseHost() != null && InetAddresses.isInetAddress(databaseConfiguration.getDatabaseHost()));
		checkConfigurationEntry(file, "dbtype",
				databaseConfiguration.getDatabaseSystem() != null);
		checkConfigurationEntry(file, "user", databaseConfiguration.getDatabaseLogin() != null);
		checkConfigurationEntry(file, "password", databaseConfiguration.getDatabasePassword() != null);
	}
	

	private static void checkMandatoryLocatorConfiguration(Path file, 
			LocatorConfiguration locatorConfiguration) throws javax.naming.ConfigurationException {
		checkConfigurationEntry(file, "host",locatorConfiguration.getLocatorUrl() != null);
	}
	

	private static void checkMandatoryMainConfiguration(Path file, OpushConfiguration configuration) {
		try {
			checkConfigurationEntry(file, "external-url", configuration.getActiveSyncServletUrl() != null);
		} catch (ConfigurationException e) {
			throw new InvalidConfigurationEntry(file, "external-url");
		}
	}

	public static GlobalAppConfiguration<OpushConfiguration> loadFromFiles(Path... files) {
		for (Path file: files) {
			try {
				return buildConfiguration(file);
			} catch (ConfigurationException e) {
				LOGGER.warn("Configuration loading error : {}", e.getMessage());
			} catch (Exception e) {
				LOGGER.warn("Configuration loading error", e);
			}
		}
		String error = String.format("No suitable configuration in list : %s", Joiner.on(", ").join(files));
		LOGGER.error(error);
		throw new ConfigurationException(error);
	}

	private static void checkConfigurationEntry(Path file, String entry, boolean value) {
		if (!value) {
			throw new InvalidConfigurationEntry(file, entry);
		}
	}
}
