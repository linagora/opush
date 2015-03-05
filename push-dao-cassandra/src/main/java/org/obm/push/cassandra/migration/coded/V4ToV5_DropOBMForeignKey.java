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
package org.obm.push.cassandra.migration.coded;

import java.sql.Connection;
import java.sql.SQLException;

import org.obm.dbcp.DatabaseConnectionProvider;
import org.obm.dbcp.DatabaseDriverConfigurationProvider;
import org.obm.push.cassandra.migration.CodedMigrationService.CodedMigration;
import org.obm.push.cassandra.schema.Version;
import org.slf4j.Logger;

import com.google.common.base.Throwables;

public class V4ToV5_DropOBMForeignKey implements CodedMigration {

	private static final String MY_ALTER_QUERY = 
		"ALTER TABLE opush_sync_state " + 
		"DROP FOREIGN KEY opush_sync_state_collection_id_opush_folder_mapping_id_fkey;";
	
	private static final String PG_ALTER_QUERY = 
		"ALTER TABLE opush_sync_state " +
		"DROP CONSTRAINT opush_sync_state_collection_id_fkey;";
	
	private final Logger logger;
	private final DatabaseConnectionProvider dbcp;
	private final DatabaseDriverConfigurationProvider databaseDriverConfigurationProvider;
	
	public V4ToV5_DropOBMForeignKey(Logger logger, 
			DatabaseConnectionProvider dbcp, 
			DatabaseDriverConfigurationProvider databaseDriverConfigurationProvider) {
		this.logger = logger;
		this.dbcp = dbcp;
		this.databaseDriverConfigurationProvider = databaseDriverConfigurationProvider;
	}
	
	@Override
	public Version from() {
		return Version.of(4);
	}

	@Override
	public Version to() {
		return Version.of(5);
	}

	@Override
	public void apply() {
		logger.info("Dropping a foreign key from the SQL table opush_sync_state");
		try {
			switch (databaseDriverConfigurationProvider.get().getFlavour()) {
			case PGSQL:
				execute(dbcp, PG_ALTER_QUERY);
				break;
			case MYSQL:
				execute(dbcp, MY_ALTER_QUERY);
				break;
			default:
				logger.info("Nothing to do with a {} database", databaseDriverConfigurationProvider.get().getFlavour());
			}
		} catch (SQLException e) {
			logger.error("Failed!");
			throw Throwables.propagate(e);
		}
	}

	private void execute(DatabaseConnectionProvider dbcp, String sqlRequest)
			throws SQLException {

		Connection connection = dbcp.getConnection();
		connection.setReadOnly(false);
		
		logger.info("The request performed is {}", sqlRequest);
		connection.prepareCall(sqlRequest).execute();
		logger.info("Done");
	}

}
