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
package org.obm.push.cassandra;

import org.obm.configuration.VMArgumentsUtils;
import org.obm.push.cassandra.dao.SchemaProducer;
import org.obm.push.cassandra.dao.SchemaProducerImpl;
import org.obm.push.cassandra.migration.CassandraMigrationService.MigrationService;
import org.obm.push.cassandra.migration.CodedMigrationService;
import org.obm.push.cassandra.migration.CqlFilesMigrationService;
import org.obm.push.cassandra.schema.SchemaInstaller;
import org.obm.push.cassandra.schema.Version;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class MigrationModule extends AbstractModule {

	public static final Version MINIMAL_SCHEMA_VERSION = Version.of(4);
	public static final Version LATEST_SCHEMA_VERSION = Version.of(4);
	public static final String MINIMAL_SCHEMA_VERSION_NAME = "minimalSchemaVersion";
	public static final String LATEST_SCHEMA_VERSION_NAME = "latestSchemaVersion";
	
	@Override
	protected void configure() {
		bind(Version.class).annotatedWith(Names.named(MINIMAL_SCHEMA_VERSION_NAME)).toInstance(minimalSchemaVersion());
		bind(Version.class).annotatedWith(Names.named(LATEST_SCHEMA_VERSION_NAME)).toInstance(latestSchemaVersion());
		bind(SchemaProducer.class).to(SchemaProducerImpl.class);
		bind(SchemaInstaller.class).to(CqlFilesMigrationService.class);
		
		Multibinder<MigrationService> migrationServices = Multibinder.newSetBinder(binder(), MigrationService.class);
		migrationServices.addBinding().to(CodedMigrationService.class);
		migrationServices.addBinding().to(CqlFilesMigrationService.class);
	}

	@VisibleForTesting Version latestSchemaVersion() {
		return versionArgumentOrDefault(LATEST_SCHEMA_VERSION_NAME, LATEST_SCHEMA_VERSION);
	}
	
	@VisibleForTesting Version minimalSchemaVersion() {
		return versionArgumentOrDefault(MINIMAL_SCHEMA_VERSION_NAME, MINIMAL_SCHEMA_VERSION);
	}

	private Version versionArgumentOrDefault(String argName, Version defaultVersion) {
		Integer versionArgument = VMArgumentsUtils.integerArgumentValue(argName);
		if (versionArgument != null) {
			return Version.of(versionArgument);
		}
		return defaultVersion;
	}
}
