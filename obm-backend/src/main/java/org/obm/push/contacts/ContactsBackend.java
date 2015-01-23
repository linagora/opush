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
package org.obm.push.contacts;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.NoPermissionException;

import org.obm.breakdownduration.bean.Watch;
import org.obm.configuration.ContactConfiguration;
import org.obm.push.backend.CollectionPath;
import org.obm.push.backend.OpushCollection;
import org.obm.push.backend.PathsToCollections;
import org.obm.push.backend.PathsToCollections.Builder;
import org.obm.push.backend.WindowingContact;
import org.obm.push.backend.WindowingContactChanges;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSContact;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.CollectionChange;
import org.obm.push.bean.change.hierarchy.CollectionDeletion;
import org.obm.push.bean.change.hierarchy.HierarchyCollectionChanges;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.HierarchyChangesException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.InvalidFolderSyncKeyException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.impl.ObmSyncBackend;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.resource.OpushResourcesHolder;
import org.obm.push.service.ClientIdService;
import org.obm.push.service.DateService;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.WindowingDao;
import org.obm.push.utils.DateUtils;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.book.AddressBook;
import org.obm.sync.book.Contact;
import org.obm.sync.book.Folder;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.exception.ContactNotFoundException;
import org.obm.sync.items.ContactChanges;
import org.obm.sync.items.FolderChanges;
import org.obm.sync.services.IAddressBook;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
@Watch(BreakdownGroups.CONTACTS)
public class ContactsBackend extends ObmSyncBackend<WindowingContact> {
	
	private final ContactConfiguration contactConfiguration;
	private final BookClient.Factory bookClientFactory;
	private final ClientIdService clientIdService;
	private final ContactConverter contactConverter;
	private final ContactCreationIdempotenceService creationIdempotenceService;
	
	@Inject
	@VisibleForTesting ContactsBackend(MappingService mappingService, 
			BookClient.Factory bookClientFactory, 
			ContactConfiguration contactConfiguration,
			Provider<CollectionPath.Builder> collectionPathBuilderProvider,
			WindowingDao windowingDao,
			ClientIdService clientIdService,
			ContactConverter contactConverter,
			DateService dateService,
			OpushResourcesHolder opushResourcesHolder,
			ContactCreationIdempotenceService creationIdempotenceService) {
		
		super(mappingService, collectionPathBuilderProvider, windowingDao, dateService, opushResourcesHolder);
		this.bookClientFactory = bookClientFactory;
		this.contactConfiguration = contactConfiguration;
		this.clientIdService = clientIdService;
		this.contactConverter = contactConverter;
		this.creationIdempotenceService = creationIdempotenceService;
	}

	@Override
	public PIMDataType getPIMDataType() {
		return PIMDataType.CONTACTS;
	}
	
	@Override
	public HierarchyCollectionChanges getHierarchyChanges(UserDataRequest udr, 
			FolderSyncState lastKnownState, FolderSyncState outgoingSyncState)
			throws DaoException {

		try {
			FolderChanges folderChanges = listAddressBooksChanged(lastKnownState);
			Set<CollectionPath> lastKnownCollections = lastKnownCollectionPath(udr, lastKnownState, getPIMDataType());
			
			PathsToCollections changedCollections = changedCollections(udr, folderChanges);
			Set<CollectionPath> deletedCollections = deletedCollections(udr, folderChanges, lastKnownCollections, changedCollections);
			Iterable<OpushCollection> addCollections = addedCollections(lastKnownCollections, changedCollections);
			snapshotHierarchy(udr, lastKnownCollections, changedCollections, deletedCollections, outgoingSyncState);

			return buildHierarchyItemsChanges(udr, addCollections, deletedCollections);
		} catch (CollectionNotFoundException e) {
			throw new HierarchyChangesException(e);
		}
	}

	private Date backendLastSyncDate(FolderSyncState lastKnownState) throws DaoException {

		if (lastKnownState.isInitialFolderSync()) {
			return DateUtils.getEpochCalendar().getTime();
		} else {
			return getLastSyncDateFromSyncState(lastKnownState);
		}
	}

	private Date getLastSyncDateFromSyncState(FolderSyncState lastKnownState)
			throws DaoException {
		
		Date lastSyncDate = mappingService.getLastBackendMapping(getPIMDataType(), lastKnownState);
		if (lastSyncDate != null) {
			return lastSyncDate;
		}
		throw new InvalidFolderSyncKeyException(lastKnownState.getSyncKey());
	}

	private void snapshotHierarchy(UserDataRequest udr, Set<CollectionPath> lastKnownCollections,
			PathsToCollections changedCollections, Set<CollectionPath> deletedCollections,
			FolderSyncState outgoingSyncState) throws DaoException {

		Set<CollectionPath> remainingKnownCollections = Sets.difference(lastKnownCollections, deletedCollections);
		Set<CollectionPath> currentCollections = Sets.union(remainingKnownCollections, changedCollections.pathKeys());
		snapshotHierarchy(udr, currentCollections, outgoingSyncState);
	}

	@Override
	protected CollectionChange createCollectionChange(UserDataRequest udr, OpushCollection collection)
			throws DaoException, CollectionNotFoundException {
		
		CollectionPath collectionPath = collection.collectionPath();
		return CollectionChange.builder()
				.collectionId(getCollectionIdFromCollectionPath(udr, collectionPath.collectionPath()))
				.parentCollectionId(CollectionId.of(contactConfiguration.getDefaultParentId()))
				.folderType(getFolderType(udr, collection))
				.displayName(collection.displayName())
				.isNew(true)
				.build();
	}

	@Override
	protected CollectionDeletion createCollectionDeletion(UserDataRequest udr, CollectionPath collectionPath)
			throws CollectionNotFoundException, DaoException {
		
		return CollectionDeletion.builder()
				.collectionId(getCollectionIdFromCollectionPath(udr, collectionPath.collectionPath()))
				.build();
	}

	@VisibleForTesting Set<CollectionPath> deletedCollections(UserDataRequest udr, FolderChanges folderChanges, 
			Set<CollectionPath> lastKnownCollections, PathsToCollections changedCollections) {
		
		PathsToCollections removedCollections = foldersToCollection(udr, folderChanges.getRemoved());
		return FluentIterable
				.from(removedCollections.pathKeys())
				.filter(Predicates.in(lastKnownCollections))
				.filter(Predicates.not(Predicates.in(changedCollections.pathKeys())))
				.toSet();
	}

	@VisibleForTesting PathsToCollections changedCollections(UserDataRequest udr, FolderChanges folderChanges) {
		Iterable<Folder> folderChangesSorted = 
				sortedFolderChangesByDefaultAddressBook(folderChanges, contactConfiguration.getDefaultAddressBookName());
		return foldersToCollection(udr, folderChangesSorted);
	}

	private PathsToCollections foldersToCollection(final UserDataRequest udr, Iterable<Folder> folders) {
		Builder builder = PathsToCollections.builder();
		for (Folder folder : folders) {
			OpushCollection collection = collectionFromFolder(udr, folder);
			builder.put(collection.collectionPath(), collection);
		}
		return builder.build();
	}

	protected OpushCollection collectionFromFolder(UserDataRequest udr, Folder folder) {
		String backendName = ContactCollectionPath.backendName(folder);
		return OpushCollection.builder()
				.collectionPath(collectionPathBuilderProvider.get()
						.userDataRequest(udr)
						.pimType(getPIMDataType())
						.backendName(backendName)
						.build())
				.ownerLoginAtDomain(folder.getOwnerLoginAtDomain())
				.displayName(folder.getName())
				.build();
	}

	@VisibleForTesting Iterable<Folder> sortedFolderChangesByDefaultAddressBook(FolderChanges folderChanges, String defaultAddressBookName) {
		return ImmutableSortedSet
				.orderedBy(new ComparatorUsingFolderName(defaultAddressBookName))
				.addAll(folderChanges.getUpdated())
				.build();
	}

	private FolderChanges listAddressBooksChanged(FolderSyncState lastKnownState)
			throws UnexpectedObmSyncServerException, DaoException {
		
		AccessToken token = getAccessToken();
		Date lastSyncDate = backendLastSyncDate(lastKnownState);
		try {
			return getBookClient().listAddressBooksChanged(token, lastSyncDate);
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		}
	}
	
	private CollectionId getCollectionIdFromCollectionPath(UserDataRequest udr, String collectionPath)
			throws DaoException, CollectionNotFoundException {
		
		return mappingService.getCollectionIdFor(udr.getDevice(), collectionPath);
	}
	
	private FolderType getFolderType(UserDataRequest udr, OpushCollection collection) {
		if (isDefaultFolder(udr, collection)) {
			return FolderType.DEFAULT_CONTACTS_FOLDER;
		} else {
			return FolderType.USER_CREATED_CONTACTS_FOLDER;
		}
	}
	
	@VisibleForTesting boolean isDefaultFolder(UserDataRequest udr, OpushCollection collection) {
		String folderName = ContactCollectionPath.folderName(collection.collectionPath());
		boolean isOwner = udr.getUser().getLoginAtDomain().equalsIgnoreCase(collection.getOwnerLoginAtDomain());
		boolean isDefaultAddressBookName = folderName.equalsIgnoreCase(contactConfiguration.getDefaultAddressBookName());
		return isOwner && isDefaultAddressBookName;
	}
	
	@Override
	public int getItemEstimateSize(UserDataRequest udr, ItemSyncState state, CollectionId collectionId, 
		SyncCollectionOptions syncCollectionOptions) throws CollectionNotFoundException, 
		DaoException, UnexpectedObmSyncServerException {
	
		WindowingChangesDelta<WindowingContact> allChanges = getAllChanges(udr, state, collectionId, syncCollectionOptions);
		return allChanges.getWindowingChanges().sumOfChanges();
	}
	
	@Override
	protected WindowingContactChanges.Builder windowingChangesBuilder() {
		return WindowingContactChanges.builder();
	}

	@Override
	protected WindowingChangesDelta<WindowingContact> getAllChanges(UserDataRequest udr, ItemSyncState state, CollectionId collectionId, SyncCollectionOptions collectionOptions) {
		
		Integer addressBookId = findAddressBookIdFromCollectionId(udr, collectionId);
		ContactChanges contactChanges = listContactsChanged(state, addressBookId);
		
		WindowingContactChanges.Builder builder  = WindowingContactChanges.builder();
		for (Contact contact : contactChanges.getUpdated()) {
			builder.change(WindowingContact.builder()
					.uid(contact.getUid())
					.applicationData(contactConverter.convert(contact))
					.build());
		}
		
		for (Integer remove : contactChanges.getRemoved()) {
			builder.deletion(WindowingContact.builder()
					.uid(remove)
					.build());
		}
		
		return WindowingChangesDelta.<WindowingContact> builder()
				.deltaDate(contactChanges.getLastSync())
				.windowingChanges(builder.build())
				.build();
	}
	
	private Integer findAddressBookIdFromCollectionId(UserDataRequest udr, CollectionId collectionId) 
			throws UnexpectedObmSyncServerException, DaoException, CollectionNotFoundException {
		
		List<AddressBook> addressBooks = listAddressBooks();
		for (AddressBook addressBook: addressBooks) {
			String backendName = ContactCollectionPath.backendName(addressBook);
			String collectionPath = collectionPathBuilderProvider.get()
					.userDataRequest(udr)
					.pimType(getPIMDataType())
					.backendName(backendName)
					.build()
					.collectionPath();
			try {
				CollectionId addressBookCollectionId = mappingService.getCollectionIdFor(udr.getDevice(), collectionPath);
				if (addressBookCollectionId.equals(collectionId)) {
					return addressBook.getUid().getId();
				}
			} catch (CollectionNotFoundException e) {
				logger.warn(e.getMessage());
			}
		}
		throw new CollectionNotFoundException(collectionId);
	}
	
	private List<AddressBook> listAddressBooks() throws UnexpectedObmSyncServerException {
		AccessToken token = getAccessToken();
		try {
			return getBookClient().listAllBooks(token);
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		}
	}

	private ContactChanges listContactsChanged(ItemSyncState state, Integer addressBookId) throws UnexpectedObmSyncServerException {
		AccessToken token = getAccessToken();
		try {
			if (state.isInitial()) {
				return getBookClient().firstListContactsChanged(token, state.getSyncDate(), addressBookId);
			}
			return getBookClient().listContactsChanged(token, state.getSyncDate(), addressBookId);
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		}
	}
	
	private ItemChange convertContactToItemChange(CollectionId collectionId, Contact contact) {
		return ItemChange.builder()
			.serverId( mappingService.getServerIdFor(collectionId, String.valueOf(contact.getUid())))
			.data(contactConverter.convert(contact))
			.build();
	}

	@Override
	public ServerId createOrUpdate(UserDataRequest udr, CollectionId collectionId,
			ServerId serverId, String clientId, IApplicationData data)
			throws CollectionNotFoundException, ProcessingEmailException,
			DaoException, UnexpectedObmSyncServerException,
			ItemNotFoundException, NoPermissionException {

		MSContact contact = (MSContact) data;
		
		if (isUpdate(serverId)) {
			return storeContact(udr, collectionId, serverId, clientId, contact);
		}
		return createContact(udr, collectionId, serverId, clientId, contact);
	}

	private boolean isUpdate(ServerId serverId) {
		return serverId != null;
	}

	private Contact convertContact(ServerId serverId, MSContact contact) {
		if (serverId == null) {
			return contactConverter.contact(contact);
		}
		
		Contact convertedContact = contactConverter.contact(contact);
		convertedContact.setUid(serverId.getItemId());
		return convertedContact;
	}

	private String hashClientId(UserDataRequest udr, String clientId) {
		if (clientId == null) {
			return null;
		}
		return clientIdService.hash(udr, clientId);
	}

	private ServerId createContact(UserDataRequest udr, CollectionId colId, 
				ServerId serverId, String clientId, MSContact contact)
			throws UnexpectedObmSyncServerException, NoPermissionException {
		
		Optional<ServerId> alreadyCreatedServerId = creationIdempotenceService.find(udr, colId, contact);
		if(alreadyCreatedServerId.isPresent() && contactExists(udr, colId, alreadyCreatedServerId.get())) {
			logger.warn("A creation is discarded as a recent similar creation has been found");
			return alreadyCreatedServerId.get();
		}
		return creationIdempotenceService.registerCreation(udr, contact, storeContact(udr, colId, serverId, clientId, contact));
	}

	private boolean contactExists(UserDataRequest udr, CollectionId collectionId, ServerId serverId) {
		try {
			int addressBookId = findAddressBookIdFromCollectionId(udr, collectionId);
			return getBookClient().getContactFromId(getAccessToken(), addressBookId, serverId.getItemId()) != null;
		} catch (ServerFault | ContactNotFoundException e) {
			logger.info("This contact has not been found by obm-sync", e);
		}
		return false;
	}

	private ServerId storeContact(UserDataRequest udr, CollectionId collectionId, 
				ServerId serverId, String clientId, MSContact msContact)
			throws UnexpectedObmSyncServerException, NoPermissionException {
		
		try {
			int addressBookId = findAddressBookIdFromCollectionId(udr, collectionId);
			Contact contact = convertContact(serverId, msContact);
			Contact storedContact = getBookClient().storeContact(getAccessToken(), addressBookId, contact, hashClientId(udr, clientId));
			return mappingService.getServerIdFor(collectionId, String.valueOf(storedContact.getUid()));
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		} catch (ContactNotFoundException e) {
			throw new ItemNotFoundException(e);
		}
	}

	@Override
	public void delete(UserDataRequest udr, CollectionId collectionId, ServerId serverId, Boolean moveToTrash)
			throws CollectionNotFoundException, DaoException,
			UnexpectedObmSyncServerException, ItemNotFoundException {
		
		Integer contactId = serverId.getItemId();
		Integer addressBookId = findAddressBookIdFromCollectionId(udr, collectionId);
		try {
			removeContact(addressBookId, contactId);
		} catch (NoPermissionException e) {
			logger.warn(e.getMessage());
		} catch (ContactNotFoundException e) {
			logger.warn(e.getMessage());
		} finally {
			// Try to remove creation commit even if there was an error.
			// For example, if the contact has been already removed from 
			// elsewhere, opush must allow to create it again.
			creationIdempotenceService.remove(udr, collectionId, serverId);
		}
	}

	private Contact removeContact(Integer addressBookId, Integer contactId) 
			throws UnexpectedObmSyncServerException, NoPermissionException, ContactNotFoundException {
		
		AccessToken token = getAccessToken();
		try {
			return getBookClient().removeContact(token, addressBookId, contactId);
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		}
	}

	@Override
	public List<ItemChange> fetch(UserDataRequest udr, CollectionId collectionId, List<ServerId> fetchServerIds, SyncCollectionOptions syncCollectionOptions,
				ItemSyncState previousItemSyncState, SyncKey newSyncKey)
			throws DaoException, UnexpectedObmSyncServerException, ConversionException {
	
		return fetch(udr, collectionId, fetchServerIds, syncCollectionOptions);
	}
	
	@Override
	public List<ItemChange> fetch(UserDataRequest udr, CollectionId collectionId, List<ServerId> fetchServerIds, SyncCollectionOptions syncCollectionOptions)
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException {
		
		List<ItemChange> ret = new LinkedList<ItemChange>();
		for (ServerId serverId: fetchServerIds) {
			try {

				Integer contactId = serverId.getItemId();
				Integer addressBookId = findAddressBookIdFromCollectionId(udr, collectionId);
				
				Contact contact = getContactFromId(addressBookId, contactId);
				ret.add( convertContactToItemChange(collectionId, contact) );
				
			} catch (ContactNotFoundException e) {
				logger.error(e.getMessage());
			}
		}
		return ret;
	}

	private Contact getContactFromId(Integer addressBookId, Integer contactId) 
			throws UnexpectedObmSyncServerException, ContactNotFoundException {
		
		AccessToken token = getAccessToken();
		try {
			return getBookClient().getContactFromId(token, addressBookId, contactId);
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		}
	}

	private IAddressBook getBookClient() {
		return bookClientFactory.create(opushResourcesHolder.getHttpClient());
	}
	
	@Override
	public ServerId move(UserDataRequest udr, String srcFolder, String dstFolder,
			ServerId messageId) throws CollectionNotFoundException,
			ProcessingEmailException {
		return null;
	}

	@Override
	public void emptyFolderContent(UserDataRequest udr, String collectionPath,
			boolean deleteSubFolder) throws NotAllowedException {
		throw new NotAllowedException(
				"emptyFolderContent is only supported for emails, collection was "
						+ collectionPath);
	}
	
	@Override
	public void initialize(DeviceId deviceId, CollectionId collectionId, FilterType filterType, SyncKey newSyncKey) {
		// nothing to do
	}
}
