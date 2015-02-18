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
package org.obm.push.impl;

import java.util.Date;

import org.obm.push.backend.CollectionPath.Builder;
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.OpushBackend;
import org.obm.push.backend.PIMBackend;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.WindowingChanges;
import org.obm.push.bean.change.WindowingChangesBuilder;
import org.obm.push.bean.change.WindowingItemWithData;
import org.obm.push.bean.change.WindowingKey;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.resource.OpushResourcesHolder;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.WindowingDao;
import org.obm.sync.auth.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.inject.Provider;

public abstract class ObmSyncBackend<WindowingItemType extends WindowingItemWithData> extends OpushBackend implements PIMBackend {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	protected String obmSyncHost;
	private final WindowingDao windowingDao;
	private final DateService dateService;
	protected final OpushResourcesHolder opushResourcesHolder;

	protected ObmSyncBackend(MappingService mappingService, Provider<Builder> collectionPathBuilderProvider, 
			WindowingDao windowingDao, DateService dateService, 
			OpushResourcesHolder opushResourcesHolder, FolderSnapshotDao folderSnapshotDao) {
		super(mappingService, collectionPathBuilderProvider, folderSnapshotDao);
		this.windowingDao = windowingDao;
		this.dateService = dateService;
		this.opushResourcesHolder = opushResourcesHolder;
	}
	
	protected AccessToken getAccessToken() {
		return opushResourcesHolder.getAccessToken();
	}
	
	@Override
	public DataDelta getChanged(UserDataRequest udr, ItemSyncState itemSyncState, AnalysedSyncCollection syncCollection, SyncKey newSyncKey)
		throws UnexpectedObmSyncServerException, DaoException, CollectionNotFoundException {

		SyncKey requestSyncKey = syncCollection.getSyncKey();
		WindowingKey key = new WindowingKey(udr.getUser(), udr.getDevId(), syncCollection.getCollectionId(), requestSyncKey);
		
		if (windowingDao.hasPendingChanges(key)) {
			return continueWindowing(syncCollection, key, newSyncKey);
		} else {
			return startWindowing(udr, itemSyncState, syncCollection, key, newSyncKey);
		}
	}
	
	private DataDelta startWindowing(UserDataRequest udr, ItemSyncState syncState, AnalysedSyncCollection collection, WindowingKey key, SyncKey newSyncKey) {
		
		final CollectionId collectionId = collection.getCollectionId();
		
		WindowingChangesDelta<WindowingItemType> allChanges = 
				getAllChanges(udr, syncState, collectionId, collection.getOptions());
		WindowingChanges<WindowingItemType> windowingChanges = allChanges.windowingChanges;
		
		if (windowingChanges.sumOfChanges() >= 0) {
			windowingDao.pushPendingChanges(key, windowingChanges, PIMDataType.CALENDAR, collection.getWindowSize());
		}
		return continueWindowing(collection, key, newSyncKey);
	}

	protected abstract WindowingChangesDelta<WindowingItemType> getAllChanges(UserDataRequest udr, ItemSyncState state, CollectionId collectionId, SyncCollectionOptions collectionOptions);

	@VisibleForTesting DataDelta.Builder builderWithChangesAndDeletions(WindowingChanges<WindowingItemType> changes, final CollectionId collectionId) {
		return DataDelta.builder()
				.changes(FluentIterable.from(Iterables.concat(changes.additions(), changes.changes()))
						.transform(new Function<WindowingItemType, ItemChange>() {
			
							@Override
							public ItemChange apply(WindowingItemType item) {
								return ItemChange.builder()
										.serverId(mappingService.getServerIdFor(collectionId, String.valueOf(item.getUid())))
										.data(item.getApplicationData())
										.build();
							}
						}).toList())
				.deletions(FluentIterable.from(changes.deletions())
						.transform(new Function<WindowingItemType, ItemDeletion>() {
			
							@Override
							public ItemDeletion apply(WindowingItemType item) {
								return ItemDeletion.builder()
										.serverId(mappingService.getServerIdFor(collectionId, String.valueOf(item.getUid())))
										.build();
							}
						}).toList());
	}

	private DataDelta continueWindowing(AnalysedSyncCollection collection, WindowingKey key, SyncKey syncKey) throws DaoException {
		WindowingChanges<WindowingItemType> pendingChanges = windowingDao.popNextChanges(key, collection.getWindowSize(), syncKey, windowingChangesBuilder()).build();
		return builderWithChangesAndDeletions(pendingChanges, collection.getCollectionId())
				.syncDate(dateService.getCurrentDate())
				.syncKey(syncKey)
				.moreAvailable(windowingDao.hasPendingChanges(key.withSyncKey(syncKey)))
				.build();
	}
	
	protected abstract WindowingChangesBuilder<WindowingItemType> windowingChangesBuilder();
	
	public static class WindowingChangesDelta<ITEM extends WindowingItemWithData> {
		
		public static <ITEM extends WindowingItemWithData> Builder<ITEM> builder() {
			return new Builder<ITEM>();
		}
		
		public static class Builder<ITEM extends WindowingItemWithData> {
			private Date deltaDate;
			private WindowingChanges<ITEM>windowingChanges;
			
			private Builder() {}
			
			public Builder<ITEM> deltaDate(Date deltaDate) {
				this.deltaDate = deltaDate;
				return this;
			}
			
			public Builder<ITEM> windowingChanges(WindowingChanges<ITEM> windowingChanges) {
				this.windowingChanges = windowingChanges;
				return this;
			}
			
			public WindowingChangesDelta<ITEM> build() {
				return new WindowingChangesDelta<ITEM>(deltaDate, windowingChanges);
			}
		}
		
		private final Date deltaDate;
		private final WindowingChanges<ITEM> windowingChanges;
		
		public WindowingChangesDelta(Date deltaDate, WindowingChanges<ITEM> windowingChanges) {
			this.deltaDate = deltaDate;
			this.windowingChanges = windowingChanges;
		}
		
		public Date getDeltaDate() {
			return deltaDate;
		}

		public WindowingChanges<ITEM> getWindowingChanges() {
			return windowingChanges;
		}
	}
}
