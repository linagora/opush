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
import org.obm.push.backend.WindowingContact;
import org.obm.push.backend.WindowingContactChanges;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSContact;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.AddressBookId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.impl.ObmSyncBackend;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.resource.OpushResourcesHolder;
import org.obm.push.service.ClientIdService;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.WindowingDao;
import org.obm.push.utils.DateUtils;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.book.Contact;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.exception.ContactNotFoundException;
import org.obm.sync.items.ContactChanges;
import org.obm.sync.services.IAddressBook;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@Watch(BreakdownGroups.CONTACTS)
public class ContactsBackend extends ObmSyncBackend<WindowingContact> {
	
	private final ContactConfiguration contactConfiguration;
	private final BookClient.Factory bookClientFactory;
	private final ClientIdService clientIdService;
	private final ContactConverter contactConverter;
	private final ContactCreationIdempotenceService creationIdempotenceService;
	private FolderSnapshotDao folderSnapshotDao;
	
	@Inject
	@VisibleForTesting ContactsBackend(MappingService mappingService, 
			BookClient.Factory bookClientFactory, 
			ContactConfiguration contactConfiguration,
			WindowingDao windowingDao,
			ClientIdService clientIdService,
			ContactConverter contactConverter,
			DateService dateService,
			OpushResourcesHolder opushResourcesHolder,
			ContactCreationIdempotenceService creationIdempotenceService,
			FolderSnapshotDao folderSnapshotDao) {
		
		super(mappingService, windowingDao, dateService, opushResourcesHolder);
		this.bookClientFactory = bookClientFactory;
		this.contactConfiguration = contactConfiguration;
		this.clientIdService = clientIdService;
		this.contactConverter = contactConverter;
		this.creationIdempotenceService = creationIdempotenceService;
		this.folderSnapshotDao = folderSnapshotDao;
	}

	@Override
	public PIMDataType getPIMDataType() {
		return PIMDataType.CONTACTS;
	}

	@Override
	public BackendFolders getBackendFolders(UserDataRequest udr) {
		return new ContactBackendFoldersBuilder()
			.userDataRequest(udr)
			.defaultAddressBookName(contactConfiguration.getDefaultAddressBookName())
			.folders(listAllAddressBooks())
			.build();
	}

	private Set<org.obm.sync.book.Folder> listAllAddressBooks() throws UnexpectedObmSyncServerException {
		AccessToken token = getAccessToken();
		Date lastSyncDate = DateUtils.getEpochCalendar().getTime();
		try {
			// Can't use "listAllBooks" endpoint as it give shared books
			// and does not respect the "syncUsersAsAddressBook" policy
			return getBookClient().listAddressBooksChanged(token, lastSyncDate).getUpdated();
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		}
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

		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		AddressBookId addressBookId = folder.getTypedBackendId();
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

	private ContactChanges listContactsChanged(ItemSyncState state, AddressBookId addressBookId) throws UnexpectedObmSyncServerException {
		AccessToken token = getAccessToken();
		try {
			if (state.isInitial()) {
				return getBookClient().firstListContactsChanged(token, state.getSyncDate(), addressBookId.getId());
			}
			return getBookClient().listContactsChanged(token, state.getSyncDate(), addressBookId.getId());
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

		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		
		if (isUpdate(serverId)) {
			return storeContact(udr, folder, serverId, clientId, contact);
		}
		return createContact(udr, folder, serverId, clientId, contact);
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

	private ServerId createContact(UserDataRequest udr, Folder folder, 
				ServerId serverId, String clientId, MSContact contact)
			throws UnexpectedObmSyncServerException, NoPermissionException {
		
		Optional<ServerId> alreadyCreatedServerId = creationIdempotenceService.find(udr, folder.getCollectionId(), contact);
		if(alreadyCreatedServerId.isPresent() && contactExists(folder, alreadyCreatedServerId.get())) {
			logger.warn("A creation is discarded as a recent similar creation has been found");
			return alreadyCreatedServerId.get();
		}
		return creationIdempotenceService.registerCreation(udr, contact, storeContact(udr, folder, serverId, clientId, contact));
	}

	private boolean contactExists(Folder folder, ServerId serverId) {
		try {
			AddressBookId addressBookId = folder.getTypedBackendId();
			return getBookClient().getContactFromId(getAccessToken(), addressBookId.getId(), serverId.getItemId()) != null;
		} catch (ServerFault | ContactNotFoundException e) {
			logger.info("This contact has not been found by obm-sync", e);
		}
		return false;
	}

	private ServerId storeContact(UserDataRequest udr, Folder folder, 
				ServerId serverId, String clientId, MSContact msContact)
			throws UnexpectedObmSyncServerException, NoPermissionException {
		
		try {
			AddressBookId addressBookId = folder.getTypedBackendId();
			Contact contact = convertContact(serverId, msContact);
			Contact storedContact = getBookClient().storeContact(getAccessToken(), addressBookId.getId(), contact, hashClientId(udr, clientId));
			return mappingService.getServerIdFor(folder.getCollectionId(), String.valueOf(storedContact.getUid()));
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
		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		AddressBookId addressBookId = folder.getTypedBackendId();
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

	private Contact removeContact(AddressBookId addressBookId, Integer contactId) 
			throws UnexpectedObmSyncServerException, NoPermissionException, ContactNotFoundException {
		
		AccessToken token = getAccessToken();
		try {
			return getBookClient().removeContact(token, addressBookId.getId(), contactId);
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
		
		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		AddressBookId addressBookId = folder.getTypedBackendId();
		
		List<ItemChange> ret = new LinkedList<ItemChange>();
		for (ServerId serverId: fetchServerIds) {
			try {
				
				Integer contactId = serverId.getItemId();
				Contact contact = getContactFromId(addressBookId, contactId);
				ret.add( convertContactToItemChange(collectionId, contact) );
				
			} catch (ContactNotFoundException e) {
				logger.error(e.getMessage());
			}
		}
		return ret;
	}

	private Contact getContactFromId(AddressBookId addressBookId, Integer contactId) 
			throws UnexpectedObmSyncServerException, ContactNotFoundException {
		
		AccessToken token = getAccessToken();
		try {
			return getBookClient().getContactFromId(token, addressBookId.getId(), contactId);
		} catch (ServerFault e) {
			throw new UnexpectedObmSyncServerException(e);
		}
	}

	private IAddressBook getBookClient() {
		return bookClientFactory.create(opushResourcesHolder.getHttpClient());
	}
	
	@Override
	public ServerId move(UserDataRequest udr, Folder srcFolder, Folder dstFolder,
			ServerId messageId) throws CollectionNotFoundException,
			ProcessingEmailException {
		return null;
	}

	@Override
	public void emptyFolderContent(UserDataRequest udr, Folder folder,
			boolean deleteSubFolder) throws NotAllowedException {
		throw new NotAllowedException(
				"emptyFolderContent is only supported for emails, collection was "
						+ folder.getBackendId());
	}
	
	@Override
	public void initialize(DeviceId deviceId, CollectionId collectionId, FilterType filterType, SyncKey newSyncKey) {
		// nothing to do
	}
}
