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

import static org.assertj.core.api.Assertions.assertThat;

import org.cassandraunit.CassandraCQLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.push.cassandra.dao.OpushCassandraCQLUnit;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

public class CassandraSessionProviderTest {

	// In this test, we want to connect to EmbeddedCassandra without cassandraCQLUnit.session;
	// but the default port in com.datastax.driver.core.ProtocolOptions (9042) doesn't match the default port in cu-cassandra.yaml
	// so we have to give a working configuration.
	@Rule public CassandraCQLUnit cassandraCQLUnit = new OpushCassandraCQLUnit("empty.cql");

	private CassandraSessionProvider cassandraSessionProvider;
	
	@Before
	public void setup() {
		cassandraSessionProvider = new CassandraSessionProvider(new CassandraSessionSupplierImpl());
	}
	
	@Test
	public void testGetSession() {
		Session session = cassandraSessionProvider.get();
		// assertThat session is provided...
		assertThat(session).isNotNull();
		
		// and it's working
		Select query = QueryBuilder.select("keyspace_name").from("system", "schema_keyspaces");
		ResultSet resultSet = session.execute(query);
		assertThat(resultSet.isExhausted()).isFalse();
		assertThat(resultSet.one().getString("keyspace_name")).isEqualTo(OpushCassandraCQLUnit.KEYSPACE);
	}
	
	private final class CassandraSessionSupplierImpl implements CassandraSessionSupplier {

		@Override
		public Session get() {
			return cassandraCQLUnit.session;
		}

		@Override
		public boolean hasBeenSupplied() {
			return true;
		}
		
	}
}
