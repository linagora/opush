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
package org.obm.push.cassandra.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;

import java.net.InetAddress;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.cassandra.dao.SchemaProducer;
import org.obm.push.cassandra.exception.InstallSchemaNotFoundException;
import org.obm.push.cassandra.schema.Version;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;

public class CqlFilesMigrationServiceTest {

	CqlFilesMigrationService testee;
	private IMocksControl mocks;
	private SchemaProducer schemaProducer;
	private Provider<Session> sessionProvider;
	private Session session;

	@Before
	public void setUp() {
		mocks = createControl();
		schemaProducer = mocks.createMock(SchemaProducer.class);
		sessionProvider = mocks.createMock(Provider.class);
		session = mocks.createMock(Session.class);
		expect(sessionProvider.get()).andReturn(session).anyTimes();
		
		testee = new CqlFilesMigrationService(schemaProducer, sessionProvider);
	}
	
	@Test
	public void testSubQueriesEmptySchema() {
		String schema = "";
		Iterable<String> subQueries = testee.subQueries(schema);
		assertThat(subQueries).isEmpty();
	}
	
	@Test
	public void testSubQueriesOneQuery() {
		String subQuery = "1\n2\n3";
		String schema = subQuery + ";\n";
				
		Iterable<String> subQueries = testee.subQueries(schema);
		assertThat(subQueries).containsOnly(subQuery);
	}
	
	@Test
	public void testSubQueriesSomeQueries() {
		String subQuery = "1\n2\n3";
		String subQuery2 = "4\n5";
		String subQuery3 = "6\n7\n8\n9";
		String separator = ";\n";
		String schema = subQuery + separator + subQuery2 + separator + subQuery3 + separator;
				
		Iterable<String> subQueries = testee.subQueries(schema);
		assertThat(subQueries).containsOnly(subQuery, subQuery2, subQuery3);
	}
	
	@Test
	public void install() {
		String schema = "schema";
		Version version = Version.of(1);
		expect(schemaProducer.schema(version)).andReturn(schema);
		expect(session.execute(schema)).andReturn(null);
		
		mocks.replay();
		testee.install(version);
		mocks.verify();
	}
	
	@Test(expected=InvalidQueryException.class)
	public void installInvalidScript() {
		String schema = "schema";
		Version version = Version.of(1);
		expect(schemaProducer.schema(version)).andReturn(schema);
		expect(session.execute(schema)).andThrow(new InvalidQueryException("expected message"));
		
		mocks.replay();
		try {
			testee.install(version);
		} catch (Exception e) {
			mocks.verify();
			throw e;
		}
	}

	@Test(expected=NoHostAvailableException.class)
	public void installNoHostAvailable() {
		String schema = "schema";
		Version version = Version.of(1);
		expect(schemaProducer.schema(version)).andReturn(schema);
		expect(session.execute(schema)).andThrow(new NoHostAvailableException(ImmutableMap.<InetAddress, Throwable> of()));
		
		mocks.replay();
		try {
			testee.install(version);
		} catch (Exception e) {
			mocks.verify();
			throw e;
		}
	}
	
	@Test(expected=InstallSchemaNotFoundException.class)
	public void installNoInitilizationSchema() {
		Version version = Version.of(1);
		expect(schemaProducer.schema(version)).andReturn(null);
		
		mocks.replay();
		try {
			testee.install(version);
		} catch (Exception e) {
			mocks.verify();
			throw e;
		}
	}
	
	@Test
	public void update() {
		Version fromVersion = Version.of(1);
		Version toVersion = Version.of(2);
		String schema = "schema";
		expect(schemaProducer.schema(fromVersion, toVersion)).andReturn(schema);
		expect(session.execute(schema)).andReturn(null);
		
		mocks.replay();
		testee.migrate(fromVersion, toVersion);
		mocks.verify();
	}
	
	@Test(expected=InvalidQueryException.class)
	public void updateInvalidScript() {
		Version fromVersion = Version.of(1);
		Version toVersion = Version.of(2);
		String schema = "schema";
		expect(schemaProducer.schema(fromVersion, toVersion)).andReturn(schema);
		expect(session.execute(schema)).andThrow(new InvalidQueryException("expected message"));
		
		mocks.replay();
		try {
			testee.migrate(fromVersion, toVersion);
		} catch (Exception e) {
			mocks.verify();
			throw e;
		}
	}

	@Test(expected=NoHostAvailableException.class)
	public void updateNoHostAvailable() {
		Version fromVersion = Version.of(1);
		Version toVersion = Version.of(2);
		String schema = "schema";
		expect(schemaProducer.schema(fromVersion, toVersion)).andReturn(schema);
		expect(session.execute(schema)).andThrow(new NoHostAvailableException(ImmutableMap.<InetAddress, Throwable> of()));
		
		mocks.replay();
		try {
			testee.migrate(fromVersion, toVersion);
		} catch (Exception e) {
			mocks.verify();
			throw e;
		}
	}
}
