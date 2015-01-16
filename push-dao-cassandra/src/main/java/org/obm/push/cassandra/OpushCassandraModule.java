/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2013  Linagora
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

import org.obm.push.cassandra.dao.CassandraSchemaDao;
import org.obm.push.cassandra.dao.CassandraStructure.ContactCreation;
import org.obm.push.cassandra.dao.CassandraStructure.MonitoredCollection;
import org.obm.push.cassandra.dao.CassandraStructure.Schema;
import org.obm.push.cassandra.dao.CassandraStructure.SnapshotIndex;
import org.obm.push.cassandra.dao.CassandraStructure.SnapshotTable;
import org.obm.push.cassandra.dao.CassandraStructure.SyncedCollection;
import org.obm.push.cassandra.dao.CassandraStructure.V1;
import org.obm.push.cassandra.dao.CassandraStructure.Windowing;
import org.obm.push.cassandra.dao.CassandraStructure.WindowingIndex;
import org.obm.push.cassandra.dao.ContactCreationDaoImpl;
import org.obm.push.cassandra.dao.MonitoredCollectionDaoCassandraImpl;
import org.obm.push.cassandra.dao.SchemaProducer;
import org.obm.push.cassandra.dao.SchemaProducerImpl;
import org.obm.push.cassandra.dao.SnapshotDaoCassandraImpl;
import org.obm.push.cassandra.dao.SyncedCollectionDaoCassandraImpl;
import org.obm.push.cassandra.dao.WindowingDaoCassandraImpl;
import org.obm.push.cassandra.schema.DaoTables;
import org.obm.push.configuration.CassandraConfiguration;
import org.obm.push.configuration.CassandraConfigurationFileImpl;
import org.obm.push.store.ContactCreationDao;
import org.obm.push.store.MonitoredCollectionDao;
import org.obm.push.store.SnapshotDao;
import org.obm.push.store.SyncedCollectionDao;
import org.obm.push.store.WindowingDao;
import org.obm.sync.LifecycleListener;

import com.datastax.driver.core.Session;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;

public class OpushCassandraModule extends AbstractModule {

	public static final DaoTables TABLES_OF_DAO = DaoTables.builder()
		.put(ContactCreationDaoImpl.class, ContactCreation.TABLE)
		.put(MonitoredCollectionDaoCassandraImpl.class, MonitoredCollection.TABLE, V1.MonitoredCollection.TABLE)
		.put(SnapshotDaoCassandraImpl.class, SnapshotIndex.TABLE, SnapshotTable.TABLE)
		.put(SyncedCollectionDaoCassandraImpl.class, SyncedCollection.TABLE, V1.SyncedCollection.TABLE)
		.put(WindowingDaoCassandraImpl.class, WindowingIndex.TABLE, Windowing.TABLE)
		.put(CassandraSchemaDao.class, Schema.TABLE)
		.build();
	
	@Override
	protected void configure() {
		install(new MigrationModule());
		bind(CassandraConfiguration.class).toInstance(new CassandraConfigurationFileImpl.Factory().create());
		bind(CassandraSessionSupplier.class).to(CassandraSessionSupplierImpl.class);
		bind(SchemaProducer.class).to(SchemaProducerImpl.class);
		bindSession();
		bindDao();
		
		Multibinder<LifecycleListener> lifecycleListeners = Multibinder.newSetBinder(binder(), LifecycleListener.class);
		lifecycleListeners.addBinding().to(CassandraSessionProvider.class);
	}

	private void bindDao() {
		bind(SyncedCollectionDao.class).to(SyncedCollectionDaoCassandraImpl.class);
		bind(MonitoredCollectionDao.class).to(MonitoredCollectionDaoCassandraImpl.class);
		bind(WindowingDao.class).to(WindowingDaoCassandraImpl.class);
		bind(SnapshotDao.class).to(SnapshotDaoCassandraImpl.class);
		bind(ContactCreationDao.class).to(ContactCreationDaoImpl.class);
		bind(DaoTables.class).toProvider(new Provider<DaoTables>() {

			@Override
			public DaoTables get() {
				return TABLES_OF_DAO;
			}
		}).asEagerSingleton();
	}

	private void bindSession() {
		bind(Session.class).toProvider(CassandraSessionProvider.class);
	}
	
}
