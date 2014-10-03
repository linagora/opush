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
import static org.easymock.EasyMock.expectLastCall;
import static org.obm.DateUtils.dateUTC;
import static org.obm.push.cassandra.schema.StatusSummary.Status.NOT_INITIALIZED;
import static org.obm.push.cassandra.schema.StatusSummary.Status.UPGRADE_REQUIRED;

import java.net.InetAddress;
import java.util.Set;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.cassandra.dao.CassandraSchemaDao;
import org.obm.push.cassandra.exception.NoTableException;
import org.obm.push.cassandra.exception.NoVersionException;
import org.obm.push.cassandra.migration.CassandraMigrationService.MigrationService;
import org.obm.push.cassandra.schema.SchemaInstaller;
import org.obm.push.cassandra.schema.StatusSummary;
import org.obm.push.cassandra.schema.StatusSummary.Status;
import org.obm.push.cassandra.schema.Version;
import org.obm.push.cassandra.schema.VersionUpdate;

import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CassandraMigrationServiceTest {

	IMocksControl mocks;
	CassandraSchemaDao schemaDao;
	SchemaInstaller schemaInstaller;
	MigrationService migrationService;
	Set<MigrationService> migrationServices;

	@Before
	public void setUp() {
		mocks = createControl();
		schemaDao = mocks.createMock(CassandraSchemaDao.class);
		schemaInstaller = mocks.createMock(SchemaInstaller.class);
		migrationService = mocks.createMock(MigrationService.class);
		
		migrationServices = ImmutableSet.of(migrationService);
	}

	private CassandraMigrationService testee(Version minimalVersion, Version latestVersion) {
		return new CassandraMigrationService(schemaDao, schemaInstaller, migrationServices, minimalVersion, latestVersion);
	}
	
	@Test
	public void giveNotInitializedWhenNoTable() {
		Version minimalVersion = Version.of(1);
		Version latestVersion = Version.of(2);
		expect(schemaDao.getCurrentVersion()).andThrow(new NoTableException("tableName"));
		
		mocks.replay();
		StatusSummary result = testee(minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = testee(minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = testee(minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = testee(minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = testee(minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(
				StatusSummary.status(Status.UP_TO_DATE).currentVersion(daoCurrentVersion).build());
	}
	
	@Test
	public void giveRequestErrorWhenNoHostAvailable() {
		Version minimalVersion = Version.of(1);
		Version latestVersion = Version.of(3);
		expect(schemaDao.getCurrentVersion()).andThrow(new NoHostAvailableException(ImmutableMap.<InetAddress, Throwable> of()));
		
		mocks.replay();
		StatusSummary result = testee(minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(StatusSummary
				.status(Status.EXECUTION_ERROR)
				.message("All host(s) tried for query failed (no host was tried)").build());
	}
	
	@Test
	public void giveUpgradeRequiredWhenUnderMinimalVersion() {
		Version minimalVersion = Version.of(2);
		Version latestVersion = Version.of(3);
		VersionUpdate daoCurrentVersion = VersionUpdate.version(Version.of(1)).date(dateUTC("2015-04-07"));
		expect(schemaDao.getCurrentVersion()).andReturn(daoCurrentVersion);
		
		mocks.replay();
		StatusSummary result = testee(minimalVersion, latestVersion).getStatus();
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
		StatusSummary result = testee(minimalVersion, latestVersion).getStatus();
		mocks.verify();
		
		assertThat(result).isEqualTo(StatusSummary.status(Status.UPGRADE_AVAILABLE)
				.currentVersion(daoCurrentVersion)
				.upgradeAvailable(latestVersion)
				.build());
	}
	
	@Test
	public void installFirstVersion() {
		Version firstVersion = Version.of(1);
		schemaInstaller.install(firstVersion);
		expectLastCall();
		schemaDao.updateVersion(firstVersion);
		expectLastCall();
		expect(schemaDao.getCurrentVersion()).andReturn(VersionUpdate.version(firstVersion).date(dateUTC("2015-04-07")));
		
		mocks.replay();
		MigrationResult result = testee(firstVersion, firstVersion).install();
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.OK);
		assertThat(result.getMessage()).isEqualTo("Schema version 1 has been installed, please restart opush to get the service up");
	}
	
	@Test
	public void installShouldInstallFirstVersionThenUpdate() {
		Version firstVersion = Version.of(1);
		Version stepVersion = Version.of(2);
		Version toVersion = Version.of(3);
		
		schemaInstaller.install(firstVersion);
		expectLastCall();
		schemaDao.updateVersion(firstVersion);
		expectLastCall();
		expect(schemaDao.getCurrentVersion()).andReturn(VersionUpdate.version(firstVersion).date(dateUTC("2015-04-07")));
		
		migrationService.migrate(firstVersion, stepVersion);
		expectLastCall();
		migrationService.migrate(stepVersion, toVersion);
		expectLastCall();
		schemaDao.updateVersion(toVersion);
		expectLastCall();
		
		mocks.replay();
		MigrationResult result = testee(firstVersion, toVersion).install();
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.OK);
		assertThat(result.getMessage()).isEqualTo("Schema version 3 has been installed, please restart opush to get the service up");
	}
	
	@Test
	public void installInvalidScript() {
		Version version = Version.of(1);
		schemaInstaller.install(version);
		expectLastCall().andThrow(new InvalidQueryException("expected message"));
		
		mocks.replay();
		MigrationResult result = testee(null, version).install();
		mocks.verify();

		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.ERROR);
		assertThat(result.getMessage()).isEqualTo("An error occurred when installing the schema: expected message");
	}
	
	@Test
	public void installNoHostAvailable() {
		Version version = Version.of(1);
		schemaInstaller.install(version);
		expectLastCall().andThrow(new NoHostAvailableException(ImmutableMap.<InetAddress, Throwable> of()));
		
		mocks.replay();
		MigrationResult result = testee(null, version).install();
		mocks.verify();

		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.ERROR);
		assertThat(result.getMessage()).isEqualTo(
				"An error occurred when installing the schema: All host(s) tried for query failed (no host was tried)");
	}
	
	@Test
	public void update() {
		Version minVersion = Version.of(1);
		Version toVersion = Version.of(2);
		Version currentVersion = Version.of(1);
		VersionUpdate versionUpdate = VersionUpdate.version(currentVersion).date(dateUTC("2013-04-07T12:09:37"));
		expect(schemaDao.getCurrentVersion()).andReturn(versionUpdate);
		migrationService.migrate(currentVersion, toVersion);
		expectLastCall();
		schemaDao.updateVersion(toVersion);
		expectLastCall();
		
		mocks.replay();
		MigrationResult result = testee(minVersion, toVersion).update();
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.OK);
		assertThat(result.getMessage()).isEqualTo("Your schema has been updated from version 1 to 2");
	}

	@Test
	public void updateRequired() {
		Version minVersion = Version.of(2);
		Version toVersion = Version.of(2);
		Version currentVersion = Version.of(1);
		VersionUpdate versionUpdate = VersionUpdate.version(currentVersion).date(dateUTC("2013-04-07T12:09:37"));
		expect(schemaDao.getCurrentVersion()).andReturn(versionUpdate);
		migrationService.migrate(currentVersion, toVersion);
		expectLastCall();
		schemaDao.updateVersion(toVersion);
		expectLastCall();
		
		mocks.replay();
		MigrationResult result = testee(minVersion, toVersion).update();
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.OK);
		assertThat(result.getMessage()).isEqualTo("Your schema has been updated from version 1 to 2, please restart opush to get the service up");
	}
	
	@Test
	public void updateNothingToDo() {
		Version version = Version.of(2);
		VersionUpdate versionUpdate = VersionUpdate.version(version).date(dateUTC("2013-04-07T12:09:37"));
		expect(schemaDao.getCurrentVersion()).andReturn(versionUpdate);
		
		mocks.replay();
		MigrationResult result = testee(null, version).update();
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.OK);
		assertThat(result.getMessage()).isEqualTo("Nothing to do, your schema is already at the latest version");
	}

	@Test
	public void updateShouldBeSequencialWhenGapBetweenVersion() {
		Version minVersion = Version.of(4);
		Version toVersion = Version.of(4);
		Version currentVersion = Version.of(2);
		VersionUpdate versionUpdate = VersionUpdate.version(currentVersion).date(dateUTC("2013-04-07T12:09:37"));
		expect(schemaDao.getCurrentVersion()).andReturn(versionUpdate);
		
		migrationService.migrate(currentVersion, Version.of(3));
		expectLastCall();
		migrationService.migrate(Version.of(3), toVersion);
		expectLastCall();
		
		schemaDao.updateVersion(toVersion);
		expectLastCall();
		
		mocks.replay();
		MigrationResult result = testee(minVersion, toVersion).update();
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.OK);
		assertThat(result.getMessage()).isEqualTo("Your schema has been updated from version 2 to 4, please restart opush to get the service up");
	}
	
	@Test
	public void updateBadVersioning() {
		Version toVersion = Version.of(1);
		Version currentVersion = Version.of(2);
		VersionUpdate versionUpdate = VersionUpdate.version(currentVersion).date(dateUTC("2013-04-07T12:09:37"));
		expect(schemaDao.getCurrentVersion()).andReturn(versionUpdate);

		mocks.replay();
		MigrationResult result = testee(null, toVersion).update();
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.ERROR);
		assertThat(result.getMessage()).isEqualTo("Version 2 conflicts with latest version 1");
	}
	
	@Test
	public void updateInvalidScript() {
		Version toVersion = Version.of(2);
		Version currentVersion = Version.of(1);
		VersionUpdate versionUpdate = VersionUpdate.version(currentVersion).date(dateUTC("2013-04-07T12:09:37"));
		expect(schemaDao.getCurrentVersion()).andReturn(versionUpdate);
		migrationService.migrate(currentVersion, toVersion);
		expectLastCall().andThrow(new InvalidQueryException("expected message"));

		mocks.replay();
		MigrationResult result = testee(null, toVersion).update();
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.ERROR);
		assertThat(result.getMessage()).isEqualTo("An error occurred when updating the schema: expected message");
	}
	
	@Test
	public void updateNoHostAvailable() {
		Version toVersion = Version.of(2);
		Version currentVersion = Version.of(1);
		VersionUpdate versionUpdate = VersionUpdate.version(currentVersion).date(dateUTC("2013-04-07T12:09:37"));
		expect(schemaDao.getCurrentVersion()).andReturn(versionUpdate);
		migrationService.migrate(currentVersion, toVersion);
		expectLastCall().andThrow(new NoHostAvailableException(ImmutableMap.<InetAddress, Throwable> of()));

		mocks.replay();
		MigrationResult result = testee(null, toVersion).update();
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(MigrationResult.Status.ERROR);
		assertThat(result.getMessage()).isEqualTo(
				"An error occurred when updating the schema: All host(s) tried for query failed (no host was tried)");
	}
}
