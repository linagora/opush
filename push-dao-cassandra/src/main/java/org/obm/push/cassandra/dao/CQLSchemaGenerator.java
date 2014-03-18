/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014  Linagora
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.obm.configuration.VMArgumentsUtils;
import org.obm.push.cassandra.OpushCassandraModule;
import org.obm.push.cassandra.schema.Version;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

public class CQLSchemaGenerator {

	private static final String MAVEN_SECTION_BREAK = "------------------------------------------------------------------------";
	
	private static final int NUMBER_OF_ARGUMENTS = 2;
	private static final String CQL_SCHEMA = "schema.cql";

	private final URL resourcesURL;
	private final File buildDirectory;

	private final Version fromVersion;
	private final Version toVersion;
	
	public static void main(String[] args) {
		appendInfoToStdOutput(MAVEN_SECTION_BREAK);
		appendInfoToStdOutput("Generating CQL schema");
		appendInfoToStdOutput(MAVEN_SECTION_BREAK);

		Version fromVersion = Version.of(VMArgumentsUtils.integerArgumentValue("fromVersion"));
		Version toVersion = Version.of(VMArgumentsUtils.integerArgumentValue("toVersion"));
		
		Preconditions.checkArgument(args.length >= NUMBER_OF_ARGUMENTS, "Not enough arguments");
		
		String buildDirectoryArgument = args[0];
		File buildDirectory = buildDirectory(buildDirectoryArgument);
		
		String resourcesDirectoryArgument = args[1];
		URL resourcesURL = resourcesURL(resourcesDirectoryArgument);
		
		CQLSchemaGenerator cqlSchemaGenerator = new CQLSchemaGenerator(resourcesURL, buildDirectory, fromVersion, toVersion);
		cqlSchemaGenerator.generate();
	}
	
	private static File buildDirectory(String buildDirectoryArgument) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(buildDirectoryArgument), "Missing build directory argument");
		return new File(buildDirectoryArgument);
	}
	
	private static URL resourcesURL(String resourcesDirectoryArgument) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(resourcesDirectoryArgument), "Missing resources directory argument");
		
		URL resourcesURL = null;
		try {
			resourcesURL = new File(resourcesDirectoryArgument + File.separatorChar + "versions").toURI().toURL();
		} catch (MalformedURLException e) {
			Throwables.propagate(e);
		}
		return resourcesURL;
	}
	
	private CQLSchemaGenerator(URL resourcesURL, File buildDirectory, Version fromVersion, Version toVersion) {
		Preconditions.checkArgument(fromVersion != null, "Missing fromVersion argument");
		Preconditions.checkArgument(toVersion != null, "Missing toVersion argument");
		
		this.resourcesURL = resourcesURL;
		this.buildDirectory = buildDirectory;
		this.fromVersion = fromVersion;
		this.toVersion = toVersion;
		
	}
	
	private void generate() {
		new MavenSchemaProducer().writeSchema(fromVersion, toVersion);
	}

	public class MavenSchemaProducer extends SchemaProducerImpl {

		public MavenSchemaProducer() {
			super(OpushCassandraModule.TABLES_OF_DAO);
		}
		
		@Override
		protected URL versionDirectory() {
			return resourcesURL;
		}
		
		public void writeSchema(Version fromVersion, Version toVersion) {
			try (FileWriter fileWriter = new FileWriter(createSchemaFile(buildDirectory))) {
				fileWriter.write(schema(fromVersion, toVersion));
			} catch (IOException e) {
				appendErrorToStdOutput(e);
			}
		}
	}

	private File createSchemaFile(File buildDirectory) {
		return new File(buildDirectory, CQL_SCHEMA);
	}

	private static void appendInfoToStdOutput(String message) {
		System.out.println("[INFO] " + message);
	}

	private static void appendErrorToStdOutput(Exception e) {
		System.out.println("[ERROR] " + e);
	}
}
