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
package org.obm.push.cassandra.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.obm.DateUtils.dateUTC;
import static org.obm.push.cassandra.schema.StatusSummary.Status.NOT_INITIALIZED;
import static org.obm.push.cassandra.schema.StatusSummary.Status.UPGRADE_REQUIRED;

import java.net.InetAddress;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.cassandra.dao.CassandraSchemaDao;
import org.obm.push.cassandra.dao.SchemaProducer;
import org.obm.push.cassandra.exception.NoTableException;
import org.obm.push.cassandra.exception.NoVersionException;
import org.obm.push.cassandra.exception.SchemaOperationException;
import org.obm.push.cassandra.schema.StatusSummary.Status;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.google.common.collect.ImmutableMap;

public class CassandraSchemaServiceTest {

	private IMocksControl mocks;
	private CassandraSchemaDao schemaDao;
	private SchemaProducer schemaProducer;
	private Session session;

	@Before
	public void setUp() {
		mocks = createControl();
		schemaDao = mocks.createMock(CassandraSchemaDao.class);
		schemaProducer = mocks.createMock(SchemaProducer.class);
		session = mocks.createMock(Session.class);
	}
	
	@Test
	public void giveNotInitializedWhenNoTable() {
		Version minimalVersion = Version.of(1);
		Version latestVersion = Version.of(2);
		expect(schemaDao.getCurrentVersion()).andThrow(new NoTableException("tableName"));
		
		mocks.replay();
		StatusSummary result = new CassandraSchemaService(schemaDao, schemaProducer, session, minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(
				StatusSummary.status(NOT_INITIALIZED).upgradeAvailable(latestVersion).build());
	}
	
	@Test
	public void giveNotInitializedWhenNoEntry() {
		Version minimalVersion = Version.of(1);
		Version latestVersion = Version.of(2);
		expect(schemaDao.getCurrentVersion()).andThrow(new NoVersionException());
		
		mocks.replay();
		StatusSummary result = new CassandraSchemaService(schemaDao, schemaProducer, session, minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(
				StatusSummary.status(UPGRADE_REQUIRED).upgradeAvailable(latestVersion).build());
	}
	
	@Test
	public void giveLatestWhenOnlyOneVersion() {
		Version minimalVersion = Version.of(1);
		Version latestVersion = Version.of(1);
		VersionUpdate daoCurrentVersion = VersionUpdate.version(minimalVersion).date(dateUTC("2015-04-07"));
		expect(schemaDao.getCurrentVersion()).andReturn(daoCurrentVersion);
		
		mocks.replay();
		StatusSummary result = new CassandraSchemaService(schemaDao, schemaProducer, session, minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(
				StatusSummary.status(Status.UP_TO_DATE).currentVersion(daoCurrentVersion).build());
	}
	
	@Test
	public void giveLatestWhenAtLatestVersion() {
		Version minimalVersion = Version.of(1);
		Version latestVersion = Version.of(3);
		VersionUpdate daoCurrentVersion = VersionUpdate.version(latestVersion).date(dateUTC("2015-04-07"));
		expect(schemaDao.getCurrentVersion()).andReturn(daoCurrentVersion);
		
		mocks.replay();
		StatusSummary result = new CassandraSchemaService(schemaDao, schemaProducer, session, minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(
				StatusSummary.status(Status.UP_TO_DATE).currentVersion(daoCurrentVersion).build());
	}
	
	@Test
	public void giveLatestWhenMoreThanLatestVersion() {
		Version minimalVersion = Version.of(1);
		Version latestVersion = Version.of(3);
		VersionUpdate daoCurrentVersion = VersionUpdate.version(Version.of(4)).date(dateUTC("2015-04-07"));
		expect(schemaDao.getCurrentVersion()).andReturn(daoCurrentVersion);
		
		mocks.replay();
		StatusSummary result = new CassandraSchemaService(schemaDao, schemaProducer, session, minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(
				StatusSummary.status(Status.UP_TO_DATE).currentVersion(daoCurrentVersion).build());
	}
	
	@Test
	public void giveUpgradeRequiredWhenUnderMinimalVersion() {
		Version minimalVersion = Version.of(2);
		Version latestVersion = Version.of(3);
		VersionUpdate daoCurrentVersion = VersionUpdate.version(Version.of(1)).date(dateUTC("2015-04-07"));
		expect(schemaDao.getCurrentVersion()).andReturn(daoCurrentVersion);
		
		mocks.replay();
		StatusSummary result = new CassandraSchemaService(schemaDao, schemaProducer, session, minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(StatusSummary.status(Status.UPGRADE_REQUIRED)
					.currentVersion(daoCurrentVersion)
					.upgradeAvailable(latestVersion)
					.build());
	}
	
	@Test
	public void giveUpgradeAvailableWhenUnderLatestVersion() {
		Version minimalVersion = Version.of(2);
		Version latestVersion = Version.of(3);
		VersionUpdate daoCurrentVersion = VersionUpdate.version(minimalVersion).date(dateUTC("2015-04-07"));
		expect(schemaDao.getCurrentVersion()).andReturn(daoCurrentVersion);
		
		mocks.replay();
		StatusSummary result = new CassandraSchemaService(schemaDao, schemaProducer, session, minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(StatusSummary.status(Status.UPGRADE_AVAILABLE)
				.currentVersion(daoCurrentVersion)
				.upgradeAvailable(latestVersion)
				.build());
	}
	
	@Test
	public void install() {
		String schema = "schema";
		Version version = Version.of(1);
		expect(schemaProducer.schema(version)).andReturn(schema);
		expect(session.execute(schema)).andReturn(null);
		schemaDao.updateVersion(version);
		expectLastCall();
		
		mocks.replay();
		CQLScriptExecutionStatus cqlScriptExecutionStatus = new CassandraSchemaService(schemaDao, schemaProducer, session, null, version).install();
		mocks.verify();
		
		assertThat(cqlScriptExecutionStatus).isEqualTo(CQLScriptExecutionStatus.OK);
	}
	
	@Test(expected=SchemaOperationException.class)
	public void installInvalidScript() {
		Version version = Version.of(1);
		String schema = "schema";
		expect(schemaProducer.schema(version)).andReturn(schema);
		expect(session.execute(schema)).andThrow(new InvalidQueryException("invalid"));
		
		try {
			mocks.replay();
			new CassandraSchemaService(schemaDao, schemaProducer, session, null, version).install();
		} catch (SchemaOperationException e) {
			mocks.verify();
			throw e;
		}
	}
	
	@Test(expected=SchemaOperationException.class)
	public void installNoHostAvailable() {
		Version version = Version.of(1);
		String schema = "schema";
		expect(schemaProducer.schema(version)).andReturn(schema);
		expect(session.execute(schema)).andThrow(new NoHostAvailableException(ImmutableMap.<InetAddress, Throwable> of()));
		
		try {
			mocks.replay();
			new CassandraSchemaService(schemaDao, schemaProducer, session, null, version).install();
		} catch (SchemaOperationException e) {
			mocks.verify();
			throw e;
		}
	}
	
	@Test(expected=SchemaOperationException.class)
	public void installNoInitilizationSchema() {
		Version version = Version.of(1);
		expect(schemaProducer.schema(version)).andReturn(null);
		try {
			mocks.replay();
			new CassandraSchemaService(schemaDao, schemaProducer, session, null, version).install();
		} catch (SchemaOperationException e) {
			mocks.verify();
			throw e;
		}
	}
	
	@Test
	public void testSubQueriesEmptySchema() {
		String schema = "";
		mocks.replay();
		Iterable<String> subQueries = new CassandraSchemaService(schemaDao, schemaProducer, session, null, null).subQueries(schema);
		assertThat(subQueries).isEmpty();
	}
	
	@Test
	public void testSubQueriesOneQuery() {
		String subQuery = "1\n2\n3";
		String schema = subQuery + ";\n";
				
		mocks.replay();
		Iterable<String> subQueries = new CassandraSchemaService(schemaDao, schemaProducer, session, null, null).subQueries(schema);
		assertThat(subQueries).containsOnly(subQuery);
	}
	
	@Test
	public void testSubQueriesSomeQueries() {
		String subQuery = "1\n2\n3";
		String subQuery2 = "4\n5";
		String subQuery3 = "6\n7\n8\n9";
		String separator = ";\n";
		String schema = subQuery + separator + subQuery2 + separator + subQuery3 + separator;
				
		mocks.replay();
		Iterable<String> subQueries = new CassandraSchemaService(schemaDao, schemaProducer, session, null, null).subQueries(schema);
		assertThat(subQueries).containsOnly(subQuery, subQuery2, subQuery3);
	}
}
