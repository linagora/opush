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
package org.obm.opush.command.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.Users.OpushUser;
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.Device;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.HierarchyChangedException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.mail.exception.FilterTypeChangedException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.ItemTrackingDao;
import org.obm.push.utils.DateUtils;
import org.obm.sync.auth.AuthFault;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

public class SyncTestUtils {
	
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private IContentsExporter contentsExporter;
	@Inject private CollectionDao collectionDao;
	@Inject private ItemTrackingDao itemTrackingDao;
	
	public void checkMailFolderHasNoChange(SyncResponse response, CollectionId serverId) {
		SyncCollectionResponse collection = getCollectionWithId(response, serverId);
		assertThat(collection.getItemChanges()).isEmpty();
		assertThat(collection.getItemDeletions()).isEmpty();
	}

	public void checkMailFolderHasAddItems(SyncResponse response, CollectionId serverId, ItemChange... changes) {
		SyncCollectionResponse collection = getCollectionWithId(response, serverId);
		assertThat(collection.getItemChanges()).containsOnly(changes);
		assertThat(collection.getItemDeletions()).isEmpty();
	}

	public void checkMailFolderHasFetchItems(SyncResponse response, CollectionId serverId, ServerId... fetches) {
		SyncCollectionResponse collection = getCollectionWithId(response, serverId);
		assertThat(collection.getResponses().fetches()).containsOnly(fetches);
	}

	public void checkMailFolderHasDeleteItems(SyncResponse response, CollectionId serverId, ItemDeletion... deletes) {
		SyncCollectionResponse collection = getCollectionWithId(response, serverId);
		assertThat(collection.getItemDeletions()).containsOnly(deletes);
	}

	public void checkMailFolderHasItems(
			SyncResponse response, CollectionId serverId, Iterable<ItemChange> changes, Iterable<ItemDeletion> deletes) {
		SyncCollectionResponse collection = getCollectionWithId(response, serverId);
		assertThat(collection.getItemChanges()).containsOnly(Iterables.toArray(changes, ItemChange.class));
		assertThat(collection.getItemDeletions()).containsOnly(Iterables.toArray(deletes, ItemDeletion.class));
	}


	public void assertEqualsWithoutApplicationData(List<ItemChange> itemChanges, List<ItemChange> expectedChanges) {
		assertThat(itemChanges).hasSize(expectedChanges.size());
		
		for (ItemChange change : itemChanges) {
			for (ItemChange expected : expectedChanges) {
				if (change.getServerId().equals(expected.getServerId())) {
					assertThat(change.isNew()).isEqualTo(expected.isNew());
				}
			}
		}
	}
	
	public SyncCollectionResponse getCollectionWithId(SyncResponse response, CollectionId serverId) {
		for (SyncCollectionResponse collection : response.getCollectionResponses()) {
			if (serverId.equals(collection.getCollectionId())) {
				return collection;
			}
		}
		return null;
	}

	public CollectionChange lookupByType(Iterable<CollectionChange> items, final FolderType type) {
		return FluentIterable
				.from(items)
				.firstMatch(new Predicate<CollectionChange>() {
		
					@Override
					public boolean apply(CollectionChange input) {
						return input.getFolderType() == type;
					}
				}).get();
	}

	public CollectionChange lookupInbox(Iterable<CollectionChange> items) {
		return lookupByType(items, FolderType.DEFAULT_INBOX_FOLDER);
	}

	public void mockEmailSyncClasses(SyncKey syncEmailSyncKey, DataDelta delta, 
			List<OpushUser> fakeTestUsers)
			throws DaoException, CollectionNotFoundException, ProcessingEmailException, UnexpectedObmSyncServerException, AuthFault,
			ConversionException, FilterTypeChangedException, HierarchyChangedException {
		
		userAccessUtils.mockUsersAccess(fakeTestUsers);
		mockEmailSync(syncEmailSyncKey, delta);
	}
	
	private void mockEmailSync(SyncKey syncEmailSyncKey, DataDelta delta)
			throws DaoException, CollectionNotFoundException, ProcessingEmailException, UnexpectedObmSyncServerException,
			ConversionException, FilterTypeChangedException, HierarchyChangedException {
		
		mockContentsExporter(delta);
		testUtils.expectUserCollectionsNeverChange();
		mockCollectionDaoForEmailSync(syncEmailSyncKey);
		mockItemTrackingDao();
	}

	public void mockItemTrackingDao() throws DaoException {
		itemTrackingDao.markAsSynced(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		itemTrackingDao.markAsDeleted(anyObject(ItemSyncState.class), anyObject(Set.class));
		expectLastCall().anyTimes();
		expect(itemTrackingDao.isServerIdSynced(anyObject(ItemSyncState.class), anyObject(ServerId.class))).andReturn(false).anyTimes();
	}

	public void mockCollectionDaoForEmailSync(SyncKey syncEmailSyncKey) throws DaoException {
		expect(collectionDao.updateState(anyObject(Device.class), anyObject(CollectionId.class), anyObject(SyncKey.class), anyObject(Date.class)))
				.andReturn(ItemSyncState.builder()
						.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
						.syncKey(syncEmailSyncKey)
						.id((int)(Math.random()*10000))
						.build()).anyTimes();
		ItemSyncState state = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncEmailSyncKey)
				.build();
		expect(collectionDao.findItemStateForKey(syncEmailSyncKey)).andReturn(state).anyTimes();
	}

	private void mockContentsExporter(DataDelta delta) 
			throws CollectionNotFoundException, ProcessingEmailException, DaoException, UnexpectedObmSyncServerException, ConversionException,
			FilterTypeChangedException, HierarchyChangedException {

		expect(contentsExporter.getChanged(
				anyObject(UserDataRequest.class),
				anyObject(ItemSyncState.class),
				anyObject(AnalysedSyncCollection.class),
				anyObject(SyncKey.class)))
				.andReturn(delta).once();
		
		expect(contentsExporter.getItemEstimateSize(
				anyObject(UserDataRequest.class), 
				anyObject(PIMDataType.class),
				anyObject(AnalysedSyncCollection.class),
				anyObject(ItemSyncState.class)))
			.andReturn(delta.getItemEstimateSize()).anyTimes();
	}


	public void mockCollectionDaoPerformSync(Device device, SyncKey requestSyncKey,
			ItemSyncState requestSyncState, ItemSyncState updateSyncState, CollectionId collectionId)
					throws DaoException {
		
		expect(collectionDao.findItemStateForKey(requestSyncKey)).andReturn(requestSyncState);
		expect(collectionDao.updateState(device, collectionId, updateSyncState.getSyncKey(), updateSyncState.getSyncDate()))
				.andReturn(updateSyncState);
	}
}
