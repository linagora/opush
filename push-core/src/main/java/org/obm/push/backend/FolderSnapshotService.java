/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015 Linagora
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
package org.obm.push.backend;

import java.util.Map;
import java.util.Set;

import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.InvalidFolderSyncKeyException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.FolderSnapshotDao.FolderSnapshotNotFoundException;
import org.obm.push.state.FolderSyncKey;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FolderSnapshotService {

	protected final FolderSnapshotDao folderSnapshotDao;

	@Inject
	protected FolderSnapshotService(FolderSnapshotDao folderSnapshotDao) {
		this.folderSnapshotDao = folderSnapshotDao;
	}

	public FolderSnapshot findFolderSnapshot(UserDataRequest udr, FolderSyncKey folderSyncKey) {
		try {
			if (folderSyncKey.isInitialFolderSync()) {
				return FolderSnapshot.empty();
			}
			return folderSnapshotDao.get(udr.getUser(), udr.getDevice(), folderSyncKey);
		} catch (FolderSnapshotNotFoundException e) {
			throw new InvalidFolderSyncKeyException(folderSyncKey, e);
		}
	}

	public FolderSnapshot snapshot(UserDataRequest udr, FolderSyncKey outgoingSyncKey,
			FolderSnapshot knownSnapshot, BackendFolders<?> currentFolders) {
		
		int nextId = knownSnapshot.getNextId();
		Map<String, Folder> knownFolders = knownSnapshot.getFoldersByBackendId();

		ImmutableSet.Builder<Folder> allFolders = ImmutableSet.builder();
		for (BackendFolder<?> currentFolder : currentFolders) {
			if (isKnownFolder(knownFolders, currentFolder)) {
				CollectionId collectionId = knownFolders.get(currentFolder.getBackendId().asString()).getCollectionId();
				allFolders.add(Folder.from(currentFolder, collectionId));
			} else {
				allFolders.add(Folder.from(currentFolder, CollectionId.of(nextId++)));
			}
		}
		
		FolderSnapshot snapshot = FolderSnapshot.nextId(nextId).folders(allFolders.build());
		folderSnapshotDao.create(udr.getUser(), udr.getDevice(), outgoingSyncKey, snapshot);
		return snapshot;
	}

	private boolean isKnownFolder(Map<String, Folder> knownFolders, BackendFolder<?> currentFolder) {
		return knownFolders.containsKey(currentFolder.getBackendId().asString());
	}
	
	public HierarchyCollectionChanges buildDiff(final FolderSnapshot knownSnapshot, final FolderSnapshot currentSnapshot) throws DaoException {
		final Map<String, Folder> knownFolders = knownSnapshot.getFoldersByBackendId();
		final Map<String, Folder> currentFolders = currentSnapshot.getFoldersByBackendId();

		final Set<String> adds = Sets.difference(currentFolders.keySet(), knownFolders.keySet());
		final Set<String> dels = Sets.difference(knownFolders.keySet(), currentFolders.keySet());
		
		return HierarchyCollectionChanges.builder()
			.deletions(FluentIterable.from(dels).transform(folderToCollectionDeletion(knownFolders)).toSet())
			.additions(FluentIterable.from(adds).transform(folderToCollectionCreation(currentFolders)).toSet())
			.changes(FluentIterable.from(currentSnapshot.getFolders())
					.filter(new Predicate<Folder>() {

						@Override
						public boolean apply(Folder folder) {
							return !adds.contains(folder.getBackendId())
								&& !dels.contains(folder.getBackendId())
								&& !knownSnapshot.getFolders().contains(folder);
						}
					})
					.transform(folderToCollectionChange(currentFolders)).toSet())
			.build();
	}

	@VisibleForTesting Function<String, CollectionDeletion> folderToCollectionDeletion(final Map<String, Folder> knownFolders) {
		return new Function<String, CollectionDeletion>() {
		
			@Override
			public CollectionDeletion apply(String id) {
				return CollectionDeletion.builder()
						.collectionId(knownFolders.get(id).getCollectionId())
						.build();
			}
		};
	}

	@VisibleForTesting Function<String, CollectionChange> folderToCollectionCreation(final Map<String, Folder> currentFoldersMap) {
		return new Function<String, CollectionChange>() {
	
				@Override
				public CollectionChange apply(String id) {
					Folder folder = currentFoldersMap.get(id);
					return CollectionChange.builder()
							.isNew(true)
							.displayName(folder.getDisplayName())
							.folderType(folder.getFolderType())
							.collectionId(folder.getCollectionId())
							.parentCollectionId(getParentCollectionId(currentFoldersMap, folder))
							.build();
				}
		};
	}

	@VisibleForTesting Function<Folder, CollectionChange> folderToCollectionChange(final Map<String, Folder> currentFolders) {
		return new Function<Folder, CollectionChange>() {
		
			@Override
			public CollectionChange apply(Folder folder) {
				return CollectionChange.builder()
						.isNew(false)
						.displayName(folder.getDisplayName())
						.folderType(folder.getFolderType())
						.collectionId(folder.getCollectionId())
						.parentCollectionId(getParentCollectionId(currentFolders, folder))
						.build();
			}
		};
	}

	private CollectionId getParentCollectionId(Map<String, Folder> currentFoldersMap, Folder folder) {
		Optional<String> parentBackendId = folder.getParentBackendId();
		if (!parentBackendId.isPresent()) {
			return CollectionId.ROOT;
		}
		Folder parentFolder = currentFoldersMap.get(parentBackendId.get());
		return parentFolder != null ? parentFolder.getCollectionId() : CollectionId.ROOT;
	}

}
