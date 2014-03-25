/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.push.cassandra;

import org.obm.configuration.module.LoggerModule;
import org.obm.push.configuration.CassandraConfiguration;
import org.slf4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class CassandraSessionSupplierImpl implements CassandraSessionSupplier {

	private Supplier<Session> sessionSupplier;
	private boolean hasBeenSupplied = false;

	@Inject
	@VisibleForTesting CassandraSessionSupplierImpl(final CassandraConfiguration cassandraConfiguration,
			@Named(LoggerModule.CONFIGURATION) final Logger configurationLogger) {

		configurationLogger.info("CASSANDRA SEEDS are {}", cassandraConfiguration.seeds());
		configurationLogger.info("CASSANDRA USER is {}", cassandraConfiguration.user());
		configurationLogger.info("CASSANDRA KEYSPACE is {}", cassandraConfiguration.keyspace());
		sessionSupplier = Suppliers.memoize(new Supplier<Session>() {

			@Override
			public Session get() {
				try {
					hasBeenSupplied = true;
					return Cluster.builder()
							.addContactPoints(Iterables.toArray(cassandraConfiguration.seeds(), String.class))
							.withCredentials(cassandraConfiguration.user(), cassandraConfiguration.password())
							.build()
							.connect(cassandraConfiguration.keyspace());
				} catch (NoHostAvailableException e) {
					configurationLogger.error("Cannot establish Cassandra connection", e);
					throw e;
				}
			}
		});
	}

	@Override
	public Session get() {
		return sessionSupplier.get();
	}

	@Override
	public boolean hasBeenSupplied() {
		return hasBeenSupplied;
	}
}
