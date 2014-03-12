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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import org.obm.push.cassandra.schema.NoVersionException;
import org.obm.push.cassandra.schema.Version;
import org.obm.push.utils.FileUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableSortedSet.Builder;
import com.google.inject.Inject;

public class SchemaProducerImpl implements SchemaProducer {

	private static final String LINE_BREAK = System.lineSeparator();
	private static final String CQL_EXTENSION = ".cql";
	private static final char SEPARATOR_CHAR = File.separatorChar;
	private static final String VERSIONS = "versions";
	private static final String VERSIONS_DIRECTORY = "/usr/share/opush/versions/";
	
	private final Map<Class<? extends CassandraDao>, Set<Table>> daoTablesMap; 
	private final URL versionDirectory;
	
	@Inject
	@VisibleForTesting SchemaProducerImpl(MonitoredCollectionDaoCassandraImpl monitoredCollectionDaoCassandraImpl,
			SnapshotDaoCassandraImpl snapshotDaoCassandraImpl,
			SyncedCollectionDaoCassandraImpl syncedCollectionDaoCassandraImpl,
			WindowingDaoCassandraImpl windowingDaoCassandraImpl) {
		
		versionDirectory = versionDirectory();
		
		daoTablesMap = ImmutableMap.<Class<? extends CassandraDao>, Set<Table>> of(
				monitoredCollectionDaoCassandraImpl.getClass(), monitoredCollectionDaoCassandraImpl.tables(), 
				snapshotDaoCassandraImpl.getClass(), snapshotDaoCassandraImpl.tables(),
				syncedCollectionDaoCassandraImpl.getClass(), syncedCollectionDaoCassandraImpl.tables(),
				windowingDaoCassandraImpl.getClass(), windowingDaoCassandraImpl.tables());
	}
	
	protected URL versionDirectory() {
		try {
			URL url = new URL("file", null, VERSIONS_DIRECTORY);
			if (new File(url.toURI()).isDirectory()) {
				return url;
			}
			return ClassLoader.getSystemResource(VERSIONS);
		} catch (MalformedURLException e) {
			Throwables.propagate(e);
			return null;
		} catch (URISyntaxException e) {
			Throwables.propagate(e);
			return null;
		}
	}
	
	@Override
	public SortedSet<Version> versionsAvailable(){
		Builder<Version> builder = ImmutableSortedSet.<Version> naturalOrder();
		for (File versionDirectory : versionDirectories()) {
			builder.add(Version.of(Integer.valueOf(versionDirectory.getName())));
		}
		return builder.build();
	}
	
	@VisibleForTesting File[] versionDirectories() {
		if (versionDirectory == null) {
			throw new NoVersionException();
		}
		try {
			return new File(versionDirectory.toURI()).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					try {
						Integer.valueOf(name);
						Path path = Paths.get(dir.getAbsolutePath(), name);
						return path.toFile().isDirectory(); 
					} catch (NumberFormatException e) {
						return false;
					}
				}
			});
		} catch (URISyntaxException e) {
			Throwables.propagate(e);
			return null;
		}
	}

	@Override
	public String lastSchema() {
		StringBuilder schema = new StringBuilder();
		for (Entry<Class<? extends CassandraDao>, Set<Table>> entry : daoTablesMap.entrySet()) {
			schema.append(lastSchemaForDAO(entry.getKey()));
		}
		return schema.toString();
	}

	@Override
	public String schema(Version version) {
		StringBuilder schema = new StringBuilder();
		for (Entry<Class<? extends CassandraDao>, Set<Table>> entry : daoTablesMap.entrySet()) {
			schema.append(schemaForDAO(entry.getKey(), version));
		}
		return schema.toString();
	}
	
	private String loadTableScript(Table table, Version version) {
		try {
			String name = versionDirectory.getPath() + SEPARATOR_CHAR + version.get() + SEPARATOR_CHAR + table.get() + CQL_EXTENSION;
			InputStream inputStream = new FileInputStream(name);
			return FileUtils.streamString(inputStream, true);
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			Throwables.propagate(e);
			return null;
		}
	}
	
	@VisibleForTesting String loadDaoScripts(Set<Table> tables, Version version) {
		StringBuilder schema = new StringBuilder();
		for (Table table : tables) {
			String script = loadTableScript(table, version);
			if (!Strings.isNullOrEmpty(script)) {
				schema.append(script)
					.append(LINE_BREAK);
			}
		}
		return schema.toString();
	}

	@Override
	public String lastSchemaForDAO(Class<? extends CassandraDao> clazz) {
		StringBuilder schema = new StringBuilder();
		for (Version version : versionsAvailable()) {
			schema.append(loadDaoScripts(daoTablesMap.get(clazz), version));
		}
		return schema.toString();
	}

	private List<Version> versionsToApply(Version toVersion) {
		return versionsToApply(null, toVersion);
	}
	
	@VisibleForTesting List<Version> versionsToApply(final Version fromVersion, final Version toVersion) {
		Preconditions.checkArgument(toVersion != null);
		return FluentIterable.from(versionsAvailable())
				.filter(new Predicate<Version>() {
					
					@Override
					public boolean apply(Version version) {
						if (fromVersion == null) {
							return version.get() <= toVersion.get();
						}
						return fromVersion.get() <= version.get()
								&& version.get() <= toVersion.get();
					}
				})
				.toList();
	}

	@Override
	public String schemaForDAO(Class<? extends CassandraDao> clazz, final Version version) {
		StringBuilder schema = new StringBuilder();
		for (Version vers : versionsToApply(version)) {
			schema.append(loadDaoScripts(daoTablesMap.get(clazz), vers));
		}
		return schema.toString();
	}

	@Override
	public String schema(Version fromVersion, Version toVersion) {
		StringBuilder schema = new StringBuilder();
		List<Version> versionsToApply = versionsToApply(fromVersion, toVersion);
		for (Entry<Class<? extends CassandraDao>, Set<Table>> entry : daoTablesMap.entrySet()) {
			for (Version version : versionsToApply) {
				schema.append(loadDaoScripts(entry.getValue(), version));
			}
		}
		return schema.toString();
	}
}
