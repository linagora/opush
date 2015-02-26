/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
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
package org.obm.push.protocol.data;

import java.util.Set;

import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.configuration.OpushConfiguration;
import org.obm.push.exception.CollectionPathException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.protocol.bean.AnalysedPingRequest;
import org.obm.push.protocol.bean.PingRequest;
import org.obm.push.protocol.bean.SyncCollection;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.StateMachine;
import org.obm.push.store.HeartbeatDao;
import org.obm.push.store.MonitoredCollectionDao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PingAnalyser {

	protected static final long MIN_SANE_HEARTBEAT_VALUE = 5L;
	
	private final HeartbeatDao heartbeatDao;
	private final MonitoredCollectionDao monitoredCollectionDao;
	private final StateMachine stateMachine;
	private final FolderSnapshotDao folderSnapshotDao;
	private final OpushConfiguration configuration;

	@Inject
	protected PingAnalyser(HeartbeatDao heartbeatDao,
			MonitoredCollectionDao monitoredCollectionDao,
			StateMachine stateMachine,
			FolderSnapshotDao folderSnapshotDao,
			OpushConfiguration configuration) {
		
		this.heartbeatDao = heartbeatDao;
		this.monitoredCollectionDao = monitoredCollectionDao;
		this.stateMachine = stateMachine;
		this.folderSnapshotDao = folderSnapshotDao;
		this.configuration = configuration;
	}

	public AnalysedPingRequest analysePing(UserDataRequest udr, PingRequest pingRequest) 
			throws DaoException, CollectionPathException, MissingRequestParameterException {

		Preconditions.checkNotNull(udr);
		Preconditions.checkNotNull(pingRequest);
		
		AnalysedPingRequest.Builder builder = AnalysedPingRequest.builder()
			.heartbeatInterval(checkHeartbeatInterval(udr, pingRequest))
			.syncCollections(checkSyncCollections(udr, pingRequest));
		return builder.build();
	}

	private long checkHeartbeatInterval(UserDataRequest udr, PingRequest pingRequest) 
			throws DaoException, MissingRequestParameterException {
		
		if (pingRequest.getHeartbeatInterval() == null) {
			Long heartbeatInterval = heartbeatDao.findLastHeartbeat(udr.getDevice());
			if (heartbeatInterval == null) {
				throw new MissingRequestParameterException();
			}
			return heartbeatInterval;
		} else {
			long heartbeatInterval = Math.max(MIN_SANE_HEARTBEAT_VALUE, pingRequest.getHeartbeatInterval());
			heartbeatDao.updateLastHeartbeat(udr.getDevice(), heartbeatInterval);
			return heartbeatInterval;
		}
	}
	
	private Set<AnalysedSyncCollection> checkSyncCollections(UserDataRequest udr, PingRequest pingRequest)
			throws MissingRequestParameterException, CollectionNotFoundException, DaoException, CollectionPathException {
		
		Set<AnalysedSyncCollection> analysedSyncCollections = Sets.newHashSet();
		Set<SyncCollection> syncCollections = pingRequest.getSyncCollections();
		if (syncCollections == null || syncCollections.isEmpty()) {
			Set<AnalysedSyncCollection> lastMonitoredCollection = monitoredCollectionDao.list(udr.getCredentials(), udr.getDevice());
			if (lastMonitoredCollection.isEmpty()) {
				throw new MissingRequestParameterException();
			}
			analysedSyncCollections.addAll(lastMonitoredCollection);
		} else {
			analysedSyncCollections.addAll(loadSyncKeys(udr, syncCollections));
		}
		return analysedSyncCollections;
	}

	private Set<AnalysedSyncCollection> loadSyncKeys(UserDataRequest udr, Set<SyncCollection> syncCollections) {
		Set<AnalysedSyncCollection> analysedSyncCollections = Sets.newHashSet();
		for (SyncCollection collection: syncCollections) {
			Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collection.getCollectionId());
			
			AnalysedSyncCollection.Builder syncCollectionRequestBuilder = AnalysedSyncCollection.builder()
					.syncKey(collection.getSyncKey())
					.collectionId(collection.getCollectionId())
					.deletesAsMoves(collection.getDeletesAsMoves())
					.changes(collection.isChanges())
					.options(collection.getOptions())
					.dataType(folder.getFolderType().getPIMDataType());

			
			Optional<Integer> windowSize = collection.getWindowSize();
			if (windowSize.isPresent()) {
				syncCollectionRequestBuilder.windowSize(windowSize.get());
			} else {
				syncCollectionRequestBuilder.windowSize(configuration.defaultWindowSize());
			}
			
			ItemSyncState lastKnownState = stateMachine.lastKnownState(udr.getDevice(), collection.getCollectionId());
			if (lastKnownState != null) {
				syncCollectionRequestBuilder.syncKey(lastKnownState.getSyncKey());
			} else {
				syncCollectionRequestBuilder.syncKey(SyncKey.INITIAL_SYNC_KEY);
			}
			analysedSyncCollections.add(syncCollectionRequestBuilder.build());
		}
		return analysedSyncCollections;
	}
}
