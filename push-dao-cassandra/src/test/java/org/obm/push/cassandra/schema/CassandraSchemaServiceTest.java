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
import static org.obm.DateUtils.dateUTC;
import static org.obm.push.cassandra.schema.StatusSummary.Status.NOT_INITIALIZED;
import static org.obm.push.cassandra.schema.StatusSummary.Status.UPGRADE_REQUIRED;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.cassandra.dao.CassandraSchemaDao;
import org.obm.push.cassandra.exception.NoTableException;
import org.obm.push.cassandra.schema.StatusSummary.Status;

public class CassandraSchemaServiceTest {

	private IMocksControl mocks;
	private CassandraSchemaDao schemaDao;

	@Before
	public void setUp() {
		mocks = createControl();
		schemaDao = mocks.createMock(CassandraSchemaDao.class);
	}
	
	@Test
	public void giveNotInitializedWhenNoTable() {
		Version minimalVersion = Version.of(1);
		Version latestVersion = Version.of(2);
		expect(schemaDao.getCurrentVersion()).andThrow(new NoTableException("tableName"));
		
		mocks.replay();
		StatusSummary result = new CassandraSchemaService(schemaDao, minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = new CassandraSchemaService(schemaDao, minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = new CassandraSchemaService(schemaDao, minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = new CassandraSchemaService(schemaDao, minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = new CassandraSchemaService(schemaDao, minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = new CassandraSchemaService(schemaDao, minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = new CassandraSchemaService(schemaDao, minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(StatusSummary.status(Status.UPGRADE_AVAILABLE)
				.currentVersion(daoCurrentVersion)
				.upgradeAvailable(latestVersion)
				.build());
	}
}
