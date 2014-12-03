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
package org.obm.push.state;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.obm.push.bean.Device;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionCommandResponse;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.bean.change.item.ASItem;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.InvalidFolderSyncKeyException;
import org.obm.push.exception.activesync.InvalidServerId;
import org.obm.push.exception.activesync.InvalidSyncKeyException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.ItemTrackingDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StateMachine implements IStateMachine {

	private static final Logger logger = LoggerFactory.getLogger(StateMachine.class);
	
	private final CollectionDao collectionDao;
	private final ItemTrackingDao itemTrackingDao;
	private final FolderSyncKeyFactory folderSyncKeyFactory;

	@Inject
	@VisibleForTesting StateMachine(CollectionDao collectionDao, ItemTrackingDao itemTrackingDao,
			FolderSyncKeyFactory folderSyncKeyFactory) {
		this.collectionDao = collectionDao;
		this.itemTrackingDao = itemTrackingDao;
		this.folderSyncKeyFactory = folderSyncKeyFactory;
	}

	@Override
	public ItemSyncState lastKnownState(Device device, CollectionId collectionId) throws DaoException {
		return collectionDao.lastKnownState(device, collectionId);
	}
	
	@Override
	public ItemSyncState getItemSyncState(SyncKey syncKey) throws DaoException {
		return collectionDao.findItemStateForKey(syncKey);
	}
	
	@Override
	public FolderSyncState getFolderSyncState(FolderSyncKey folderSyncKey) throws DaoException, InvalidSyncKeyException {
		Preconditions.checkArgument(folderSyncKey != null && !Strings.isNullOrEmpty(folderSyncKey.asString()));
		
		if (folderSyncKey.isInitialFolderSync()) {
			return FolderSyncState.builder()
					.syncKey(folderSyncKey)
					.build();
		} else {
			return findFolderSyncState(folderSyncKey);
		}
	}

	private FolderSyncState findFolderSyncState(FolderSyncKey folderSyncKey) throws DaoException, InvalidSyncKeyException {
		FolderSyncState folderSyncStateForKey = collectionDao.findFolderStateForKey(folderSyncKey);
		if (folderSyncStateForKey == null) {
			throw new InvalidFolderSyncKeyException(folderSyncKey);
		}
		return folderSyncStateForKey;
	}
	
	@Override
	public FolderSyncState allocateNewFolderSyncState(UserDataRequest udr) throws DaoException {
		FolderSyncKey newSk = folderSyncKeyFactory.randomSyncKey();
		FolderSyncState newFolderState = collectionDao.allocateNewFolderSyncState(udr.getDevice(), newSk);
		
		log(udr, newFolderState);
		return newFolderState;
	}
	
	@Override
	public void allocateNewSyncState(UserDataRequest udr, CollectionId collectionId, Date lastSync, SyncCollectionResponse syncCollectionResponse, SyncKey newSyncKey) 
			throws DaoException, InvalidServerId {

		ItemSyncState newState = collectionDao.updateState(udr.getDevice(), collectionId, newSyncKey, lastSync);
		
		Set<ServerId> listNewItems = ImmutableSet.<ServerId> builder()
				.addAll(listNewItems(syncCollectionResponse.getItemChanges()))
				.addAll(filterNotOk(syncCollectionResponse.getResponses().getCommandsForType(SyncCommand.ADD)))
				.build();
		if (!listNewItems.isEmpty()) {
			itemTrackingDao.markAsSynced(newState, listNewItems);
		}
		
		Set<ServerId> listDeletedItems = ImmutableSet.<ServerId> builder()
				.addAll(itemDeletionsAsServerIdSet(syncCollectionResponse.getItemDeletions()))
				.addAll(filterNotOk(syncCollectionResponse.getResponses().getCommandsForType(SyncCommand.DELETE)))
				.build();
		if (!listDeletedItems.isEmpty()) {
			itemTrackingDao.markAsDeleted(newState, listDeletedItems);
		}
		
		log(udr, newState);
	}
	
	private Iterable<ServerId> filterNotOk(List<SyncCollectionCommandResponse> commands) {
		return FluentIterable
				.from(commands)
				.filter(new Predicate<SyncCollectionCommandResponse>() {
		
					@Override
					public boolean apply(SyncCollectionCommandResponse input) {
						return SyncStatus.OK == input.getStatus();
					}
				})
				.transform(new Function<SyncCollectionCommandResponse, ServerId>() {

					@Override
					public ServerId apply(SyncCollectionCommandResponse input) {
						return input.getServerId();
					}
				});
	}

	@Override
	public void allocateNewSyncStateWithoutTracking(UserDataRequest udr, CollectionId collectionId, Date lastSync, SyncKey newSyncKey) throws DaoException, InvalidServerId {

		ItemSyncState newState = collectionDao.updateState(udr.getDevice(), collectionId, newSyncKey, lastSync);
		log(udr, newState);
	}

	private Set<ServerId> itemDeletionsAsServerIdSet(Iterable<ItemDeletion> deletions) throws InvalidServerId {
		Set<ServerId> ids = Sets.newHashSet();
		if (ids == null) {
			return ids;
		}
		
		for (ItemDeletion deletion: deletions) {
			addServerItemId(ids, deletion);
		}
		return ids;
	}

	private Set<ServerId> listNewItems(Collection<ItemChange> changes) throws InvalidServerId {
		HashSet<ServerId> serverIds = Sets.newHashSet();
		if (changes == null) {
			return serverIds;
		}
		
		for (ItemChange change: changes) {
			if (change.isNew()) {
				addServerItemId(serverIds, change);
			}
		}
		return serverIds;
	}

	private void addServerItemId(Set<ServerId> serverIds, ASItem change) throws InvalidServerId {
		ServerId serverId = change.getServerId();
		if (serverId.isItem()) {
			serverIds.add(serverId);
		}
	}
	
	private void log(UserDataRequest udr, ItemSyncState newState) {
		String collectionPath = "obm:\\\\" + udr.getUser().getLoginAtDomain();
		logger.info("Allocate new synckey {} for collectionPath {} with {} last sync", 
				newState.getSyncKey(), collectionPath, newState.getSyncDate());
	}
	
	private void log(UserDataRequest udr, FolderSyncState newState) {
		String collectionPath = "obm:\\\\" + udr.getUser().getLoginAtDomain();
		logger.info("Allocate new synckey {} for collectionPath {}", 
				newState.getSyncKey(), collectionPath);
	}
}
