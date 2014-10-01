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

import java.util.Set;

import org.obm.push.cassandra.migration.CassandraMigrationService.MigrationService;
import org.obm.push.cassandra.migration.coded.V2ToV3_TTL;
import org.obm.push.cassandra.schema.Version;
import org.obm.push.configuration.LoggerModule;
import org.slf4j.Logger;

import com.datastax.driver.core.Session;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class CodedMigrationService implements MigrationService {

	private final Logger logger;
	private final Set<? extends CodedMigration> migrations;

	@Inject
	@VisibleForTesting CodedMigrationService(
			@Named(LoggerModule.CASSANDRA) Logger logger,
			Provider<Session> sessionProvider) {
		this.logger = logger;
		this.migrations = ImmutableSet.of(
			new V2ToV3_TTL(logger, sessionProvider)
		);
	}
	
	@Override
	public void migrate(Version currentVersion, Version toVersion) {
		for (CodedMigration migration: migrations) {
			if (versionGapNeedsThisMigration(currentVersion, toVersion, migration)) {
				logger.warn("A scripted migration has been found from version {} to {}", currentVersion.get(), toVersion.get());
				migration.apply();
			}
		}
	}

	@VisibleForTesting boolean versionGapNeedsThisMigration(Version currentVersion, Version toVersion, CodedMigration migration) {
		return currentVersion.equals(migration.from()) && toVersion.equals(migration.to());
	}

	public static interface CodedMigration {
		
		Version from();
		Version to();
		void apply();
		
	}
}
