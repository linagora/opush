/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2013  Linagora
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

import java.util.Collection;

import org.obm.configuration.utils.IniFile;

import com.datastax.driver.core.SocketOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.inject.Singleton;

@Singleton
public class CassandraConfigurationFileImpl implements CassandraConfiguration {

	@VisibleForTesting static final String CONFIG_FILE_PATH = "/etc/opush/cassandra.ini";
	private final IniFile iniFile;
	
	@VisibleForTesting static final char CASSANDRA_SEEDS_SEPARATOR = ',';
	@VisibleForTesting static final String CASSANDRA_SEEDS = "cassandra.seeds";
	@VisibleForTesting static final String CASSANDRA_KEYSPACE = "cassandra.keyspace";
	@VisibleForTesting static final String CASSANDRA_USER = "cassandra.user";
	@VisibleForTesting static final String CASSANDRA_PASSWORD = "cassandra.password";
	@VisibleForTesting static final String CASSANDRA_READ_TIMEOUT_MS = "cassandra.read-timeout-ms";
	@VisibleForTesting static final String CASSANDRA_RETRY_POLICY = "cassandra.retry-policy";
	@VisibleForTesting static final CassandraRetryPolicy DEFAULT_CASSANDRA_RETRY_POLICY = CassandraRetryPolicy.ALWAYS_RETRY;
	@VisibleForTesting static final String CASSANDRA_WRITE_RETRIES = "cassandra.max-retries";
	@VisibleForTesting static final int DEFAULT_CASSANDRA_WRITE_RETRIES = 3;
	
	public static class Factory {
		
		protected IniFile.Factory iniFileFactory;

		public Factory() {
			iniFileFactory = new IniFile.Factory();
		}
		
		public CassandraConfigurationFileImpl create() {
			return new CassandraConfigurationFileImpl(iniFileFactory.build(CONFIG_FILE_PATH));
		}
	}
	
	@VisibleForTesting CassandraConfigurationFileImpl(IniFile iniFile) {
		this.iniFile = iniFile;
	}

	@Override
	public Collection<String> seeds() {
		String allSeeds = getMandatoryStringValue(CASSANDRA_SEEDS);
		return Splitter.on(CASSANDRA_SEEDS_SEPARATOR)
				.omitEmptyStrings()
				.trimResults()
				.splitToList(allSeeds);
	}

	@Override
	public String keyspace() {
		return getMandatoryStringValue(CASSANDRA_KEYSPACE);
	}

	@Override
	public String user() {
		return getMandatoryStringValue(CASSANDRA_USER);
	}

	@Override
	public String password() {
		return getMandatoryStringValue(CASSANDRA_PASSWORD);
	}
	
	@Override
	public int readTimeoutMs() {
		int value = iniFile.getIntValue(CASSANDRA_READ_TIMEOUT_MS, SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS);
		Preconditions.checkState(value >= 0, "Negative read timeout value in " + CONFIG_FILE_PATH + " configuration file");
		return value;
	}

	private String getMandatoryStringValue(String key) {
		String value = iniFile.getStringValue(key);
		Preconditions.checkNotNull(value, "Missing Cassandra " + key + " key in " + CONFIG_FILE_PATH + " configuration file");
		return value;
	}

	@Override
	public CassandraRetryPolicy retryPolicy() {
		String value = iniFile.getStringValue(CASSANDRA_RETRY_POLICY);
		return Strings.isNullOrEmpty(value) ? DEFAULT_CASSANDRA_RETRY_POLICY : CassandraRetryPolicy.valueOf(value.toUpperCase());
	}

	@Override
	public int maxRetries() {
		int value = iniFile.getIntValue(CASSANDRA_WRITE_RETRIES, DEFAULT_CASSANDRA_WRITE_RETRIES);
		Preconditions.checkState(value > 0, "Negative or null write retries value in " + CONFIG_FILE_PATH + " configuration file");
		return value;
	}
}
