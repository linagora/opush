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
package org.obm.push.cassandra.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.obm.DateUtils.dateUTC;

import java.util.Date;

import org.cassandraunit.CassandraCQLUnit;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.push.cassandra.PublicCassandraService;
import org.obm.push.cassandra.TestCassandraConfiguration;
import org.obm.push.cassandra.exception.NoVersionException;
import org.obm.push.cassandra.schema.Version;
import org.obm.push.cassandra.schema.VersionUpdate;
import org.obm.push.configuration.CassandraConfiguration;
import org.obm.sync.date.DateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraSchemaDaoTest {

	private static final String DAO_SCHEMA = new DaoTestsSchemaProducer().schemaForDAO(CassandraSchemaDao.class);
	@Rule public CassandraCQLUnit cassandraCQLUnit = new OpushCassandraCQLUnit(DAO_SCHEMA);
	
	private Logger logger = LoggerFactory.getLogger(CassandraSchemaDaoTest.class);

	protected IMocksControl control;
	protected DateProvider dateProvider;
	protected CassandraSchemaDao schemaDao;
	protected CassandraConfiguration configuration;
	
	@Before
	public void init() {
		control = createControl();
		dateProvider = control.createMock(DateProvider.class);
		CassandraConfiguration configuration = new TestCassandraConfiguration(OpushCassandraCQLUnit.KEYSPACE);
		
		SessionProvider sessionProvider = new SessionProvider(cassandraCQLUnit.session);
		schemaDao = new CassandraSchemaDao(sessionProvider, new PublicJSONService(), logger, 
				new PublicCassandraService(sessionProvider, configuration), dateProvider);
	}
	
	@Test(expected=NoVersionException.class)
	public void getCurrentVersionWhenNone() {
		schemaDao.getCurrentVersion();
	}
	
	@Test
	public void getCurrentVersionWhenOnlyOne() {
		Version versionNumber = Version.of(5);
		Date versionDate = dateUTC("2015-04-07T12:09:37");
		expect(dateProvider.getDate()).andReturn(versionDate);
		
		control.replay();
		schemaDao.updateVersion(versionNumber);
		control.verify();
		
		assertThat(schemaDao.getCurrentVersion()).isEqualTo(
				VersionUpdate.version(versionNumber).date(versionDate));
	}
	
	@Test
	public void getCurrentVersionOnlyNumberMatters() {
		Version versionNumber1 = Version.of(5);
		Date versionDate1 = dateUTC("2015-04-07T12:09:37");
		expect(dateProvider.getDate()).andReturn(versionDate1);

		Version versionNumber2 = Version.of(8);
		Date versionDate2 = dateUTC("2014-04-07T12:09:37");
		expect(dateProvider.getDate()).andReturn(versionDate2);

		Version versionNumber3 = Version.of(1);
		Date versionDate3 = dateUTC("2013-04-07T12:09:37");
		expect(dateProvider.getDate()).andReturn(versionDate3);
	
		control.replay();
		schemaDao.updateVersion(versionNumber1);
		schemaDao.updateVersion(versionNumber2);
		schemaDao.updateVersion(versionNumber3);
		control.verify();

		assertThat(schemaDao.getCurrentVersion()).isEqualTo(
				VersionUpdate.version(versionNumber2).date(versionDate2));
	}
	
	@Test
	public void getHistoryWhenNone() {
		assertThat(schemaDao.getHistory()).isEmpty();
	}
	
	@Test
	public void getHistoryOnlyNumberMatters() {
		Version versionNumber1 = Version.of(5);
		Date versionDate1 = dateUTC("2015-04-07T12:09:37");
		expect(dateProvider.getDate()).andReturn(versionDate1);

		Version versionNumber2 = Version.of(8);
		Date versionDate2 = dateUTC("2014-04-07T12:09:37");
		expect(dateProvider.getDate()).andReturn(versionDate2);

		Version versionNumber3 = Version.of(1);
		Date versionDate3 = dateUTC("2013-04-07T12:09:37");
		expect(dateProvider.getDate()).andReturn(versionDate3);

		control.replay();
		schemaDao.updateVersion(versionNumber1);
		schemaDao.updateVersion(versionNumber2);
		schemaDao.updateVersion(versionNumber3);
		control.verify();
		
		assertThat(schemaDao.getHistory()).containsExactly(
				VersionUpdate.version(versionNumber2).date(versionDate2),
				VersionUpdate.version(versionNumber1).date(versionDate1),
				VersionUpdate.version(versionNumber3).date(versionDate3));
	}
}
