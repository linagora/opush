/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2015  Linagora
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
package org.obm.push.task;

import java.util.Date;
import java.util.List;

import org.obm.breakdownduration.bean.Watch;
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.PIMBackend;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderCreateRequest;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.activesync.BackendNotSupportedException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.base.Optional;

@Watch(BreakdownGroups.TASKS)
public class TaskBackend implements PIMBackend {

	@Override
	public List<ItemChange> fetch(UserDataRequest udr, CollectionId collectionId, List<ServerId> fetchServerIds, 
				SyncCollectionOptions syncCollectionOptions)
			throws ProcessingEmailException, CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException {
		
		return fetch(udr, collectionId, fetchServerIds, syncCollectionOptions);
	}
	
	@Override
	public List<ItemChange> fetch(UserDataRequest udr, CollectionId collectionId, List<ServerId> fetchServerIds,
			SyncCollectionOptions syncCollectionOptions, ItemSyncState previousItemSyncState, SyncKey newSyncKey)
		throws ProcessingEmailException, CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException {
		throw new CollectionNotFoundException();
	}
	
	@Override
	public DataDelta getChanged(UserDataRequest udr, ItemSyncState itemSyncState, AnalysedSyncCollection collection, SyncKey newSyncKey)
			throws DaoException, CollectionNotFoundException, UnexpectedObmSyncServerException, ProcessingEmailException {
		return DataDelta.newEmptyDelta(new Date(), newSyncKey);
	}
	
	@Override
	public int getItemEstimateSize(UserDataRequest udr, ItemSyncState state, CollectionId collectionId, 
			SyncCollectionOptions collectionOptions) 
		throws CollectionNotFoundException, ProcessingEmailException, 
			DaoException, UnexpectedObmSyncServerException {
		return 0;
	}
	
	@Override
	public PIMDataType getPIMDataType() {
		return PIMDataType.TASKS;
	}

	@Override
	public ServerId createOrUpdate(UserDataRequest udr, CollectionId collectionId,
			ServerId serverId, String clientId, IApplicationData data)
			throws CollectionNotFoundException, ProcessingEmailException,
			DaoException, UnexpectedObmSyncServerException,
			ItemNotFoundException {
		return null;
	}

	@Override
	public ServerId move(UserDataRequest udr, Folder srcFolder, Folder dstFolder,
			ServerId serverId) throws CollectionNotFoundException,
			ProcessingEmailException {
		return null;
	}

	@Override
	public void delete(UserDataRequest udr, CollectionId collectionId, ServerId serverId, Boolean moveToTrash)
			throws CollectionNotFoundException, DaoException,
			UnexpectedObmSyncServerException, ItemNotFoundException {
		
	}

	@Override
	public void emptyFolderContent(UserDataRequest udr, Folder folder,
			boolean deleteSubFolder) throws NotAllowedException {
	}

	@Override
	public BackendFolders getBackendFolders(UserDataRequest udr) {
		return BackendFolders.EMPTY.instance();
	}
	
	@Override
	public void initialize(DeviceId deviceId, CollectionId collectionId, FilterType filterType, SyncKey newSyncKey) {
		// nothing to do
	}

	@Override
	public BackendId createFolder(UserDataRequest udr, FolderCreateRequest folderCreateRequest,
			Optional<BackendId> backendId)
			throws BackendNotSupportedException {
		throw new BackendNotSupportedException("Create a folder is not supported for tasks");
	}
}
