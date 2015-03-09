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
package org.obm.push.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.obm.dbcp.DatabaseConnectionProvider;
import org.obm.dbcp.DatabaseDriverConfigurationProvider;
import org.obm.push.bean.migration.CodedMigration;
import org.obm.push.bean.migration.Version;
import org.obm.push.migration.CodedMigrationService;
import org.slf4j.Logger;

import com.datastax.driver.core.Session;
import com.google.inject.Provider;

public class CodedMigrationServiceTest {

	CodedMigrationService testee;

	@Before
	public void setUp() {
		Provider<Session> sessionProvider = null;
		Logger logger = null;
		DatabaseConnectionProvider dbcp = null;
		DatabaseDriverConfigurationProvider driverProvider = null;
		testee = new CodedMigrationService(logger, sessionProvider, dbcp, driverProvider);
	}

	@Test
	public void needsThisMigrationShouldBeFalseWhenSameFromButDifferentTo() {
		Version currentVersion = Version.of(2);
		Version toVersion = Version.of(3);
		CodedMigration migration = new TestCodedMigration(currentVersion, Version.of(4));
		
		assertThat(testee.versionGapNeedsThisMigration(currentVersion, toVersion, migration)).isFalse();
	}
	
	@Test
	public void needsThisMigrationShouldBeFalseWhenDifferentFromButSameTo() {
		Version currentVersion = Version.of(2);
		Version toVersion = Version.of(3);
		CodedMigration migration = new TestCodedMigration(Version.of(1), toVersion);
		
		assertThat(testee.versionGapNeedsThisMigration(currentVersion, toVersion, migration)).isFalse();
	}
	
	@Test
	public void needsThisMigrationShouldBeFalseWhenBothDifferent() {
		Version currentVersion = Version.of(2);
		Version toVersion = Version.of(5);
		CodedMigration migration = new TestCodedMigration(Version.of(3), Version.of(4));
		
		assertThat(testee.versionGapNeedsThisMigration(currentVersion, toVersion, migration)).isFalse();
	}

	@Test
	public void needsThisMigrationShouldBeTrueWhenSameFromAndTo() {
		Version currentVersion = Version.of(2);
		Version toVersion = Version.of(3);
		CodedMigration migration = new TestCodedMigration(currentVersion, toVersion);
		
		assertThat(testee.versionGapNeedsThisMigration(currentVersion, toVersion, migration)).isTrue();
	}
	
	public static class TestCodedMigration implements CodedMigration {

		Version from;
		Version to;
		boolean applied;

		public TestCodedMigration(Version from, Version to) {
			this.from = from;
			this.to = to;
			this.applied = false;
		}
		
		@Override
		public Version from() {
			return from;
		}

		@Override
		public Version to() {
			return to;
		}

		@Override
		public void apply() {
			applied = true;
		}
		
	}
}
