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
package org.obm.push.mail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.codec.binary.Base64;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.obm.breakdownduration.bean.Watch;
import org.obm.configuration.EmailConfiguration;
import org.obm.push.ExpungePolicy;
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.OpushBackend;
import org.obm.push.bean.Address;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSAttachementData;
import org.obm.push.bean.MSEmailBodyType;
import org.obm.push.bean.MimeSupport;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SnapshotKey;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.WindowingChanges;
import org.obm.push.bean.change.WindowingKey;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.FolderCreateRequest;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.MSEmailChanges;
import org.obm.push.bean.ms.UidMSEmail;
import org.obm.push.configuration.OpushConfiguration;
import org.obm.push.configuration.OpushEmailConfiguration;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.EmailViewBuildException;
import org.obm.push.exception.EmailViewPartsFetcherException;
import org.obm.push.exception.ImapMessageNotFoundException;
import org.obm.push.exception.MailException;
import org.obm.push.exception.OpushLocatorException;
import org.obm.push.exception.SendEmailException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.UnsupportedBackendFunctionException;
import org.obm.push.exception.activesync.AttachementNotFoundException;
import org.obm.push.exception.activesync.BackendNotSupportedException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.FolderAlreadyExistsException;
import org.obm.push.exception.activesync.InvalidSyncKeyException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.exception.activesync.ParentFolderNotFoundException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.exception.activesync.StoreEmailException;
import org.obm.push.mail.MailBackendSyncData.MailBackendSyncDataFactory;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.EmailReader;
import org.obm.push.mail.bean.MailboxFolder;
import org.obm.push.mail.bean.MessageSet;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.mail.conversation.EmailView;
import org.obm.push.mail.conversation.EmailViewAttachment;
import org.obm.push.mail.exception.FilterTypeChangedException;
import org.obm.push.mail.mime.MimeAddress;
import org.obm.push.mail.transformer.Transformer.TransformersFactory;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.AuthenticationService;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.SmtpSender;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.SnapshotDao;
import org.obm.push.store.WindowingDao;
import org.obm.push.tnefconverter.TNEFConverterException;
import org.obm.push.tnefconverter.TNEFUtils;
import org.obm.push.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.mail.util.QPDecoderStream;

@Singleton
@Watch(BreakdownGroups.EMAIL)
public class MailBackendImpl extends OpushBackend implements MailBackend {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final MailboxService mailboxService;
	private final Mime4jUtils mime4jUtils;
	private final OpushConfiguration opushConfiguration;
	private final AuthenticationService authenticationService;
	private final MSEmailFetcher msEmailFetcher;
	private final TransformersFactory transformersFactory;
	private final SnapshotDao snapshotDao;
	private final EmailChangesFetcher emailChangesFetcher;
	private final MailBackendSyncDataFactory mailBackendSyncDataFactory;
	private final WindowingDao windowingDao;
	private final SmtpSender smtpSender;
	private final DateService dateService;
	private final FolderSnapshotDao folderSnapshotDao;

	private final OpushEmailConfiguration emailConfiguration;

	@Inject
	/* package */ MailBackendImpl(MailboxService mailboxService, 
			AuthenticationService authenticationService, 
			Mime4jUtils mime4jUtils, OpushConfiguration opushConfiguration,
			SnapshotDao snapshotDao,
			EmailChangesFetcher emailChangesFetcher,
			MappingService mappingService,
			MSEmailFetcher msEmailFetcher,
			TransformersFactory transformersFactory,
			MailBackendSyncDataFactory mailBackendSyncDataFactory,
			WindowingDao windowingDao,
			SmtpSender smtpSender, 
			OpushEmailConfiguration emailConfiguration,
			DateService dateService,
			FolderSnapshotDao folderSnapshotDao)  {

		super(mappingService);
		this.mailboxService = mailboxService;
		this.mime4jUtils = mime4jUtils;
		this.opushConfiguration = opushConfiguration;
		this.authenticationService = authenticationService;
		this.snapshotDao = snapshotDao;
		this.emailChangesFetcher = emailChangesFetcher;
		this.msEmailFetcher = msEmailFetcher;
		this.transformersFactory = transformersFactory;
		this.mailBackendSyncDataFactory = mailBackendSyncDataFactory;
		this.windowingDao = windowingDao;
		this.smtpSender = smtpSender;
		this.emailConfiguration = emailConfiguration;
		this.dateService = dateService;
		this.folderSnapshotDao = folderSnapshotDao;
	}

	@Override
	public PIMDataType getPIMDataType() {
		return PIMDataType.EMAIL;
	}

	@Override
	public BackendFolders getBackendFolders(UserDataRequest udr) {
		return new MailBackendFoldersBuilder()
			.addFolders(mailboxService.listSubscribedFolders(udr))
			.addSpecialFolders(generateSpecialFolders())
			.build();
	}

	private List<Entry<MailboxPath, FolderType>> generateSpecialFolders() {
		return ImmutableList.of(
			Maps.immutableEntry(MailboxPath.of(OpushEmailConfiguration.IMAP_INBOX_NAME), FolderType.DEFAULT_INBOX_FOLDER),
			Maps.immutableEntry(MailboxPath.of(emailConfiguration.imapMailboxDraft()), FolderType.DEFAULT_DRAFTS_FOLDER),
			Maps.immutableEntry(MailboxPath.of(emailConfiguration.imapMailboxSent()), FolderType.DEFAULT_SENT_EMAIL_FOLDER),
			Maps.immutableEntry(MailboxPath.of(emailConfiguration.imapMailboxTrash()), FolderType.DEFAULT_DELETED_ITEMS_FOLDER));
	}
	
	@Override
	public int getItemEstimateSize(UserDataRequest udr, ItemSyncState state, CollectionId collectionId, 
			SyncCollectionOptions options) throws ProcessingEmailException, 
			CollectionNotFoundException, DaoException, FilterTypeChangedException {
		
		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		MailBackendSyncData syncData = mailBackendSyncDataFactory.create(udr, state, folder, options);
		return syncData.getEmailChanges().sumOfChanges();
	}

	/**
	 * @throws FilterTypeChangedException when a snapshot 
	 * exists for the given syncKey and the snapshot.filterType != options.filterType
	 */
	@Override
	public DataDelta getChanged(UserDataRequest udr, ItemSyncState syncState, AnalysedSyncCollection syncCollection, SyncKey newSyncKey)
		throws DaoException, CollectionNotFoundException, UnexpectedObmSyncServerException, ProcessingEmailException, FilterTypeChangedException {

		try {
			CollectionId collectionId = syncCollection.getCollectionId();
			SyncKey syncKey = syncCollection.getSyncKey();
			Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
			WindowingKey windowingKey = new WindowingKey(udr.getUser(), udr.getDevId(), collectionId, syncKey);
			
			if (windowingDao.hasPendingChanges(windowingKey)) {
				snapshotDao.linkSyncKeyToSnapshot(newSyncKey, SnapshotKey.builder()
						.collectionId(collectionId)
						.deviceId(udr.getDevId())
						.syncKey(syncKey).build());
				
				return continueWindowing(udr, syncCollection, folder, windowingKey, newSyncKey);
			} else {
				return startWindowing(udr, syncState, syncCollection, folder, windowingKey, newSyncKey);
			}
		} catch (EmailViewPartsFetcherException e) {
			throw new ProcessingEmailException(e);
		}
	}

	private DataDelta startWindowing(UserDataRequest udr, ItemSyncState syncState, AnalysedSyncCollection collection,
			Folder folder, WindowingKey key, SyncKey newSyncKey) throws EmailViewPartsFetcherException {
		
		MailBackendSyncData syncData = mailBackendSyncDataFactory.create(udr, syncState, folder, collection.getOptions());
		takeSnapshot(udr, collection.getCollectionId(), collection.getOptions(), syncData, newSyncKey);
		
		if (syncData.getEmailChanges().hasChanges()) {
			windowingDao.pushPendingChanges(key, syncData.getEmailChanges(), PIMDataType.EMAIL);
		}
		
		return continueWindowing(udr, collection, folder, key, newSyncKey);
	}

	private DataDelta continueWindowing(UserDataRequest udr, AnalysedSyncCollection collection, Folder folder,
			WindowingKey key, SyncKey newSyncKey) throws DaoException, EmailViewPartsFetcherException {

		WindowingChanges<Email> pendingChanges = windowingDao.popNextChanges(
				key, collection.getWindowSize().get(), newSyncKey, EmailChanges.builder()).build();
		return fetchChanges(udr, collection, folder, key, newSyncKey, pendingChanges);
	}
	
	private DataDelta fetchChanges(UserDataRequest udr, AnalysedSyncCollection collection, Folder folder, 
			WindowingKey key, SyncKey newSyncKey, WindowingChanges<Email> pendingChanges)
		throws EmailViewPartsFetcherException {
		
		MailboxPath path = folder.getTypedBackendId();
		MSEmailChanges serverItemChanges = emailChangesFetcher.fetch(udr, collection.getCollectionId(),
				path, collection.getOptions(), pendingChanges);
		
		return DataDelta.builder()
				.changes(serverItemChanges.getItemChanges())
				.deletions(serverItemChanges.getItemDeletions())
				.syncDate(dateService.getCurrentDate())
				.syncKey(newSyncKey)
				.moreAvailable(windowingDao.hasPendingChanges(key.withSyncKey(newSyncKey)))
				.build();
	}

	private void takeSnapshot(UserDataRequest udr, CollectionId collectionId, 
			SyncCollectionOptions syncCollectionOptions, MailBackendSyncData syncData, SyncKey newSyncKey) {
		
		snapshotDao.put(
			SnapshotKey.builder()
				.collectionId(collectionId)
				.deviceId(udr.getDevId())
				.syncKey(newSyncKey).build(),
			Snapshot.builder()
				.emails(syncData.getNewManagedEmails())
				.filterType(syncCollectionOptions.getFilterType())
				.uidNext(syncData.getCurrentUIDNext())
				.build());
	}

	private Map<CollectionId, Collection<Long>> getEmailUidByCollectionId(List<ServerId> fetchIds) {
		Map<CollectionId, Collection<Long>> ret = Maps.newHashMap();
		for (ServerId serverId : fetchIds) {
			CollectionId collectionId = serverId.getCollectionId();
			Collection<Long> set = ret.get(collectionId);
			if (set == null) {
				set = Sets.newHashSet();
				ret.put(collectionId, set);
			}
			set.add(getEmailUidFromServerId(serverId));
		}
		return ret;
	}

	private List<ItemChange> fetchItems(UserDataRequest udr, Folder folder, Collection<Long> uids, 
			SyncCollectionOptions options) throws CollectionNotFoundException, ProcessingEmailException {
		
		try {
			Builder<ItemChange> ret = ImmutableList.builder();
			MailboxPath path = folder.getTypedBackendId();
			
			List<UidMSEmail> emails = msEmailFetcher.fetch(
					udr, folder.getCollectionId(), path, uids, options.getBodyPreferences(), options.getMimeSupport());
			
			for (UidMSEmail email: emails) {
				ItemChange ic = ItemChange.builder()
					.serverId(mappingService.getServerIdFor(folder.getCollectionId(), String.valueOf(email.getUid())))
					.data(email)
					.build();
				ret.add(ic);
			}
			return ret.build();	
		} catch (DaoException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		} catch (EmailViewPartsFetcherException e) {
			throw new ProcessingEmailException(e);
		}
	}
	
	@Override
	public void delete(UserDataRequest udr, CollectionId collectionId, ServerId serverId, Boolean moveToTrash)
			throws CollectionNotFoundException, DaoException,
			UnexpectedObmSyncServerException, ItemNotFoundException, ProcessingEmailException, UnsupportedBackendFunctionException {
		try {
			boolean trash = Objects.firstNonNull(moveToTrash, true);
			if (trash) {
				logger.info("move to trash serverId {}", serverId);
			} else {
				logger.info("delete serverId {}", serverId);
			}
			if (serverId != null) {
				Long uid = getEmailUidFromServerId(serverId);
				Folder sourceFolder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
				MailboxPath sourcePath = sourceFolder.getTypedBackendId();
				MailboxPath destinationPath = MailboxPath.of(emailConfiguration.imapMailboxTrash());
				if (trash && !destinationPath.equals(sourcePath)) {
					mailboxService.move(udr, sourcePath, destinationPath, MessageSet.singleton(uid));
				} else {
					mailboxService.delete(udr, sourcePath, MessageSet.singleton(uid));
				}
				expunge(udr, sourcePath);
			}	
		} catch (MailException e) {
			throw new ProcessingEmailException(e);
		} catch (DaoException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		} catch (ImapMessageNotFoundException e) {
			throw new ItemNotFoundException(e);
		}
	}


	protected String getDefaultCalendarName(UserDataRequest udr) {
		return "obm:\\\\" + udr.getUser().getLoginAtDomain() + "\\calendar\\"
				+ udr.getUser().getLoginAtDomain();
	}
	
	@Override
	public ServerId createOrUpdate(UserDataRequest udr, CollectionId collectionId, ServerId serverId, String clientId, IApplicationData data)
			throws CollectionNotFoundException, ProcessingEmailException, DaoException, ItemNotFoundException {
		
		org.obm.push.bean.ms.MSEmail msEmail = (org.obm.push.bean.ms.MSEmail)data;
		try {
			Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
			MailboxPath path = folder.getTypedBackendId();
			logger.info("createOrUpdate( {}, {}, {} )", folder.getBackendId(), serverId, clientId);
			if (serverId != null) {
				MessageSet messages = MessageSet.singleton(getEmailUidFromServerId(serverId));
				mailboxService.updateReadFlag(udr, path, messages, msEmail.isRead());
			}
			return serverId;
		} catch (MailException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		} catch (ImapMessageNotFoundException e) {
			throw new ItemNotFoundException(e);
		}
	}

	@Override
	public ServerId move(UserDataRequest udr, Folder srcFolder, Folder dstFolder, ServerId serverId) 
			throws CollectionNotFoundException, ProcessingEmailException, UnsupportedBackendFunctionException {
		
		try {
			MailboxPath srcFolderPath = srcFolder.getTypedBackendId();
			MailboxPath dstFolderPath = dstFolder.getTypedBackendId();
			logger.info("move( messageId =  {}, from = {}, to = {} )", serverId, srcFolderPath, dstFolderPath);
			Long currentMailUid = getEmailUidFromServerId(serverId);
			MessageSet messages = mailboxService.move(udr, srcFolderPath, dstFolderPath, MessageSet.singleton(currentMailUid));
			if (!messages.isEmpty()) {
				expunge(udr, srcFolderPath);
				return dstFolder.getCollectionId().serverId(Ints.checkedCast(Iterables.getOnlyElement(messages)));	
			}
			throw new ItemNotFoundException("The item to move may not exists anymore");
		} catch (MailException e) {
			throw new ProcessingEmailException(e);
		} catch (DaoException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		} catch (ImapMessageNotFoundException e) {
			throw new ProcessingEmailException(e);
		}
	}


	@Override
	public void sendEmail(UserDataRequest udr, byte[] mailContent, boolean saveInSent) throws ProcessingEmailException {
		try {
			Message message = mime4jUtils.parseMessage(mailContent);
			SendEmail sendEmail = new SendEmail(getUserEmail(udr), message);
			send(udr, sendEmail, saveInSent);
		} catch (UnexpectedObmSyncServerException e) {
			throw new ProcessingEmailException(e);
		} catch (MimeException e) {
			throw new ProcessingEmailException(e);
		} catch (IOException e) {
			throw new ProcessingEmailException(e);
		} 
	}

	@Override
	public void replyEmail(UserDataRequest udr, byte[] mailContent, boolean saveInSent, CollectionId collectionId, ServerId serverId)
			throws ProcessingEmailException, CollectionNotFoundException, ItemNotFoundException {
		
		try {
			MailboxPath path = null;
			if (collectionId != null) {
				Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
				path = folder.getTypedBackendId();
			}
			
			if (path == null && serverId != null) {
				Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), serverId.getCollectionId());
				path = folder.getTypedBackendId();
			}
			
			Long uid = getEmailUidFromServerId(serverId);
			Map<MSEmailBodyType, EmailView> emailViews = fetchMailInHTMLThenText(udr, collectionId, path, uid);

			if (emailViews.size() > 0) {
				Message message = mime4jUtils.parseMessage(mailContent);
				ReplyEmail replyEmail = new ReplyEmail(opushConfiguration, mime4jUtils, getUserEmail(udr), emailViews, message,
						ImmutableMap.<String, MSAttachementData>of());
				send(udr, replyEmail, saveInSent);
				mailboxService.setAnsweredFlag(udr, path, MessageSet.singleton(uid));
			} else {
				sendEmail(udr, mailContent, saveInSent);
			}
		} catch (DaoException e) {
			throw new ProcessingEmailException(e);
		} catch (MailException e) {
			throw new ProcessingEmailException(e);
		} catch (UnexpectedObmSyncServerException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		} catch (MimeException e) {
			throw new ProcessingEmailException(e);
		} catch (IOException e) {
			throw new ProcessingEmailException(e);
		} catch (ImapMessageNotFoundException e) {
			throw new ItemNotFoundException(e);
		} catch (EmailViewPartsFetcherException e) {
			throw new ProcessingEmailException(e);
		} 
	}

	@Override
	public void forwardEmail(UserDataRequest udr, byte[] mailContent, boolean saveInSent, CollectionId collectionId, ServerId serverId) 
			throws ProcessingEmailException, CollectionNotFoundException {
		
		try {
			Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
			MailboxPath path = folder.getTypedBackendId();
			Long uid = getEmailUidFromServerId(serverId);

			Map<MSEmailBodyType, EmailView> emailViews = fetchMailInHTMLThenText(udr, collectionId, path, uid);
			if (emailViews.size() > 0) {
				Message message = mime4jUtils.parseMessage(mailContent);
				
				Map<String, MSAttachementData> originalMailAttachments = new HashMap<String, MSAttachementData>();
				if (!mime4jUtils.isAttachmentsExist(message)) {
					loadAttachments(originalMailAttachments, udr, emailViews);
				}
				
				ForwardEmail forwardEmail = 
						new ForwardEmail(opushConfiguration, mime4jUtils, getUserEmail(udr), emailViews, message, originalMailAttachments);
				send(udr, forwardEmail, saveInSent);
				try{
					mailboxService.setAnsweredFlag(udr, path, MessageSet.singleton(uid));
				} catch (Throwable e) {
					logger.info("Can't set Answered Flag to mail["+uid+"]");
				}
			} else {
				sendEmail(udr, mailContent, saveInSent);
			}
		} catch (NumberFormatException e) {
			throw new ProcessingEmailException(e);
		} catch (DaoException e) {
			throw new ProcessingEmailException(e);
		} catch (MailException e) {
			throw new ProcessingEmailException(e);
		} catch (UnexpectedObmSyncServerException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		} catch (MimeException e) {
			throw new ProcessingEmailException(e);
		} catch (IOException e) {
			throw new ProcessingEmailException(e);
		} catch (EmailViewPartsFetcherException e) {
			throw new ProcessingEmailException(e);
		} 
	}
	
	@VisibleForTesting Map<MSEmailBodyType, EmailView> fetchMailInHTMLThenText(UserDataRequest udr, CollectionId collectionId, 
			MailboxPath path, Long uid) throws EmailViewPartsFetcherException {
		
		ImmutableMap.Builder<MSEmailBodyType, EmailView> emailViews = ImmutableMap.builder();
		if (!tryToPutViewForType(udr, collectionId, path, uid, emailViews, MSEmailBodyType.HTML)) {
			tryToPutViewForType(udr, collectionId, path, uid, emailViews, MSEmailBodyType.PlainText);
		}
		return emailViews.build();
	}

	private boolean tryToPutViewForType(UserDataRequest udr, CollectionId collectionId, MailboxPath path, Long uid,
			ImmutableMap.Builder<MSEmailBodyType, EmailView> emailViews, MSEmailBodyType type) throws EmailViewPartsFetcherException {
		
		try {
			EmailView view = fetchBodyType(udr, collectionId, path, uid, type);
			if (view.getBodyMimePartData().isPresent()) {
				emailViews.put(type, view);
				return true;
			}
		} catch (EmailViewBuildException e) {
			logger.debug("Cannot get view for body type " + type, e);
		}
		return false;
	}

	private EmailView fetchBodyType(UserDataRequest udr, CollectionId collectionId, MailboxPath path, Long uid, MSEmailBodyType bodyType)
			throws EmailViewPartsFetcherException, EmailViewBuildException {
		
		EmailViewPartsFetcherImpl emailViewPartsFetcherImpl = 
				new EmailViewPartsFetcherImpl(transformersFactory, mailboxService, 
						ImmutableList.of(BodyPreference.builder().bodyType(bodyType).build()), 
						udr, path, collectionId);
		return emailViewPartsFetcherImpl.fetch(uid, new StrictMatchBodyPreferencePolicy());
	}
	
	private void loadAttachments(Map<String, MSAttachementData> attachments, 
			UserDataRequest udr, Map<MSEmailBodyType, EmailView> emailViews) throws ProcessingEmailException {
		
		Collection<EmailView> values = emailViews.values();
		if (values == null || values.isEmpty()) {
			return;
		}
		
		EmailView emailView = FluentIterable.from(values).first().get();
		for (EmailViewAttachment msAttachement: emailView.getAttachments()) {
			try {
				MSAttachementData msAttachementData = getAttachment(udr, msAttachement.getFileReference());
				attachments.put(msAttachement.getDisplayName(), msAttachementData);
			} catch (AttachementNotFoundException e) {
				throw new ProcessingEmailException(e);
			} catch (CollectionNotFoundException e) {
				throw new ProcessingEmailException(e);
			} 
		}
	}

	@VisibleForTesting InternetAddress getUserEmail(UserDataRequest udr) throws UnexpectedObmSyncServerException {
		String email = authenticationService.getUserEmail(udr);
		try {
			InternetAddress address = new InternetAddress(email, true);
			address.setPersonal(udr.getUser().getDisplayName(), Charsets.UTF_8.name());
			return address;

		} catch (UnsupportedEncodingException | AddressException e) {
			logger.warn("Cannot set internet address", e);
			throw Throwables.propagate(e);
		} 

	}

	private void send(UserDataRequest udr, SendEmail sendEmail, boolean saveInSent) throws ProcessingEmailException {
		try {
			boolean isScheduleMeeting = !TNEFUtils.isScheduleMeetingRequest(sendEmail.getMessage());

			if (!sendEmail.isInvitation() && isScheduleMeeting) {
				sendEmail(udr, sendEmail, saveInSent);
			} else {
				logger.warn("OPUSH blocks email invitation sending by PDA. Now that obm-sync handle email sending on event creation/modification/deletion, we must filter mail from PDA for these actions.");
			}
		} catch (TNEFConverterException e) {
			throw new ProcessingEmailException(e);
		} catch (StoreEmailException e) {
			throw new ProcessingEmailException(e);
		}
	}

	@VisibleForTesting void sendEmail(UserDataRequest udr, SendEmail email, boolean saveInSent) {
		try (InputStream emailStream = loadEmailInMemory(email)) {
			smtpSender.sendEmail(udr, validateFrom(email.getFrom()), email.getTo(), email.getCc(), email.getCci(), emailStream);
			if (saveInSent) {
				tryToStoreInSent(udr, email, emailStream);
			} else {
				logger.info("The email mustn't be stored in Sent folder.{saveInSent=false}");
			}
		} catch (IOException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		} catch (SendEmailException e) {
			throw new ProcessingEmailException(e);
		} catch (MailException e) {
			throw new ProcessingEmailException(e);
		} catch (IllegalCharsetNameException e) {
			throw new ProcessingEmailException(e);
		}
	}

	private void tryToStoreInSent(UserDataRequest udr, SendEmail email, InputStream emailStream) {
		try {
			emailStream.reset();
			mailboxService.storeInSent(udr, multipleTimesReadable(emailStream, email.getMimeMessage().getCharset()));
		} catch (CollectionNotFoundException e) {
			logger.warn("Cannot store an email in the Sent folder, the collection was not found: {}", e.getMessage());
		} catch (Throwable t) {
			logger.error("Cannot store an email in the Sent folder", t);
		}
	}

	private InputStream loadEmailInMemory(SendEmail email) throws IOException {
		InputStream emailStream = new ByteArrayInputStream(FileUtils.streamBytes(email.getMessage(), true));
		emailStream.mark(0);
		return emailStream;
	}

	private EmailReader multipleTimesReadable(InputStream streamMail, String charsetName) {
		return new EmailReader(new InputStreamReader(streamMail, Charset.forName(charsetName)));
	}

	private Address validateFrom(InternetAddress address) throws ProcessingEmailException {
		String from = address.getAddress();
		if(from == null || !from.contains("@")){
			throw new ProcessingEmailException(""+from+"is not a valid email");
		}
		return new Address(from);
	}

	@Override
	public UidMSEmail getEmail(UserDataRequest udr, CollectionId collectionId, ServerId serverId) throws CollectionNotFoundException, ProcessingEmailException {
		try {
			Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
			MailboxPath path = folder.getTypedBackendId();
			Long uid = getEmailUidFromServerId(serverId);
			Set<Long> uids = new HashSet<Long>();
			uids.add(uid);
			List<UidMSEmail> emails = msEmailFetcher.fetch(udr, collectionId, path, uids, null, Optional.<MimeSupport>absent());
			if (emails.size() > 0) {
				return emails.get(0);
			}
			return null;	
		} catch (MailException e) {
			throw new ProcessingEmailException(e);
		} catch (DaoException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		} catch (EmailViewPartsFetcherException e) {
			throw new ProcessingEmailException(e);
		}
	}

	@Override
	public org.obm.icalendar.ICalendar getInvitation(UserDataRequest udr, CollectionId collectionId, ServerId serverId) 
			throws CollectionNotFoundException, ProcessingEmailException {
		
		try {
			return fetchInvitation(udr, collectionId, getEmailUidFromServerId(serverId));
			
		} catch (MailException e) {
			throw new ProcessingEmailException(e);
		} catch (DaoException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		} catch (EmailViewPartsFetcherException e) {
			throw new ProcessingEmailException(e);
		}
	}
	
	private org.obm.icalendar.ICalendar fetchInvitation(UserDataRequest udr, CollectionId collectionId, Long uid) throws DaoException, EmailViewPartsFetcherException {

		Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
		MailboxPath path = folder.getTypedBackendId();
		return msEmailFetcher.fetchInvitation(udr, collectionId, path, uid);
	}

	@Override
	public MSAttachementData getAttachment(UserDataRequest udr, String attachmentId) 
			throws AttachementNotFoundException, CollectionNotFoundException, ProcessingEmailException {
		
		if (attachmentId != null && !attachmentId.isEmpty()) {
			Map<String, String> parsedAttId = AttachmentHelper.parseAttachmentId(attachmentId);
			try {
				String collectionId = parsedAttId
						.get(AttachmentHelper.COLLECTION_ID);
				String messageId = parsedAttId.get(AttachmentHelper.MESSAGE_ID);
				String mimePartAddress = parsedAttId
						.get(AttachmentHelper.MIME_PART_ADDRESS);
				String contentType = parsedAttId
						.get(AttachmentHelper.CONTENT_TYPE);
				String contentTransferEncoding = parsedAttId
						.get(AttachmentHelper.CONTENT_TRANSFERE_ENCODING);
				logger.info("attachmentId= [collectionId:" + collectionId
						+ "] [emailUid" + messageId + "] [mimePartAddress:"
						+ mimePartAddress + "] [contentType" + contentType
						+ "] [contentTransferEncoding"
						+ contentTransferEncoding + "]");

				Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), CollectionId.of(collectionId));
				MailboxPath path = folder.getTypedBackendId();
				InputStream is = mailboxService.findAttachment(udr,
						path, Long.parseLong(messageId),
						new MimeAddress(mimePartAddress));

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				FileUtils.transfer(is, out, true);
				byte[] rawData = out.toByteArray();

				if ("QUOTED-PRINTABLE".equals(contentTransferEncoding)) {
					out = new ByteArrayOutputStream();
					InputStream in = new QPDecoderStream(new ByteArrayInputStream(rawData));
					FileUtils.transfer(in, out, true);
					rawData = out.toByteArray();
				} else if ("BASE64".equals(contentTransferEncoding)) {
					rawData = new Base64().decode(rawData);
				}

				return new MSAttachementData(contentType,
						new ByteArrayInputStream(rawData));
		
			} catch (NumberFormatException e) {
				throw new ProcessingEmailException(e);
			} catch (IOException e) {
				throw new ProcessingEmailException(e);
			} catch (MailException e) {
				throw new ProcessingEmailException(e);
			} catch (DaoException e) {
				throw new ProcessingEmailException(e);
			} catch (OpushLocatorException e) {
				throw new ProcessingEmailException(e);
			}
		}
		
		throw new AttachementNotFoundException();
	}

	@Override
	public void emptyFolderContent(UserDataRequest udr, Folder folder, boolean deleteSubFolder)
			throws NotAllowedException, CollectionNotFoundException, ProcessingEmailException {
		
		try {
			MailboxPath wasteBasketPath = MailboxPath.of(EmailConfiguration.IMAP_TRASH_NAME);
			if (!folder.getBackendId().equals(wasteBasketPath)) {
				throw new NotAllowedException(
						"Only the Trash folder can be purged.");
			}
			final Integer devDbId = udr.getDevice().getDatabaseId();
			mailboxService.purgeFolder(udr, devDbId, wasteBasketPath);
			expunge(udr, wasteBasketPath);
			if (deleteSubFolder) {
				logger.warn("deleteSubFolder isn't implemented because opush doesn't yet manage folders");
			}	
		} catch (MailException e) {
			throw new ProcessingEmailException(e);
		} catch (DaoException e) {
			throw new ProcessingEmailException(e);
		} catch (OpushLocatorException e) {
			throw new ProcessingEmailException(e);
		}
	}

	private void expunge(UserDataRequest udr, MailboxPath path) {
		if (ExpungePolicy.NEVER != emailConfiguration.expungePolicy()) {
			mailboxService.expunge(udr, path);
		}
	}
	
	@Override
	public Long getEmailUidFromServerId(ServerId serverId) {
		Integer itemIdFromServerId = serverId.getItemId();
		if (itemIdFromServerId != null) {
			return itemIdFromServerId.longValue();
		} else {
			return null;
		}
	}

	@Override
	public List<ItemChange> fetch(UserDataRequest udr, CollectionId collectionId, List<ServerId> itemIds, SyncCollectionOptions collectionOptions) 
			throws ProcessingEmailException {
		
		LinkedList<ItemChange> fetchs = new LinkedList<ItemChange>();
		Map<CollectionId, Collection<Long>> emailUids = getEmailUidByCollectionId(itemIds);
		try {
			Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId);
			for (Entry<CollectionId, Collection<Long>> entry : emailUids.entrySet()) {
				Collection<Long> uids = entry.getValue();
				fetchs.addAll(fetchItems(udr, folder, uids, collectionOptions));
			}
		} catch (CollectionNotFoundException e) {
			logger.error("fetchItems : collection {} not found !", collectionId);
		}
		return fetchs;
	}

	@Override
	public List<ItemChange> fetch(UserDataRequest udr, CollectionId collectionId, List<ServerId> itemIds, SyncCollectionOptions collectionOptions, 
				ItemSyncState previousItemSyncState, SyncKey newSyncKey) 
			throws ProcessingEmailException {

		snapshotDao.linkSyncKeyToSnapshot(newSyncKey, SnapshotKey.builder()
				.collectionId(collectionId)
				.deviceId(udr.getDevId())
				.syncKey(previousItemSyncState.getSyncKey()).build());
		
		Snapshot snapshot = snapshotDao.get(SnapshotKey.builder()
				.deviceId(udr.getDevId())
				.syncKey(previousItemSyncState.getSyncKey())
				.collectionId(collectionId).build());
		
		if (snapshot == null) {
			throw new InvalidSyncKeyException(previousItemSyncState.getSyncKey());
		}
		if (!snapshot.containsAllIds(itemIds)) {
			throw new ItemNotFoundException();
		}
		
		return fetch(udr, collectionId, itemIds, collectionOptions);
	}
	
	@Override
	public void initialize(DeviceId deviceId, CollectionId collectionId, FilterType filterType, SyncKey newSyncKey) {
		snapshotDao.put(
				SnapshotKey.builder()
					.collectionId(collectionId)
					.deviceId(deviceId)
					.syncKey(newSyncKey).build(),
				Snapshot.builder()
					.emails(ImmutableList.<Email> of())
					.filterType(filterType)
					.build());
	}

	@Override
	public BackendId createFolder(UserDataRequest udr, FolderCreateRequest folderCreateRequest,
			Optional<BackendId> parent)
		throws BackendNotSupportedException {
		
		char serverDelimiter = findServerSeparator(udr, parent);
		
		MailboxFolder newMailboxFolder = 
				findNameRelatedToParent(folderCreateRequest, parent, serverDelimiter);
		
		MailboxPath mailboxPath = MailboxPath.of(
				newMailboxFolder.getName(), newMailboxFolder.getImapSeparator());
		
		if (mailboxService.folderExists(udr, mailboxPath)) {
			throw new FolderAlreadyExistsException("Cannot create two times a folder.");
		} 
		
		mailboxService.createFolder(udr, newMailboxFolder);
		mailboxService.subscribeToFolder(udr, newMailboxFolder);
		
		return mailboxPath;
	}

	private char findServerSeparator(UserDataRequest udr, Optional<BackendId> parent) {
		if (!parent.isPresent()) {
			return MailboxPath.DEFAULT_SEPARATOR;
		}
		
		MailboxPath parentPath = (MailboxPath)parent.get();
		Optional<MailboxFolder> mailboxFolder = mailboxService.getFolder(udr, parentPath);

		if (mailboxFolder.isPresent()) {
			return mailboxFolder.get().getImapSeparator();
		} 
			
		throw new ParentFolderNotFoundException(parent.get().toString());	
	}

	private MailboxFolder findNameRelatedToParent(
			FolderCreateRequest folderCreateRequest, Optional<BackendId> parent, char serverDelimiter) {
		
		if (parent.isPresent()) {
			MailboxPath backendId = (MailboxPath) parent.get();
			return new MailboxFolder(backendId.getPath() + serverDelimiter + 
					folderCreateRequest.getFolderDisplayName(), serverDelimiter);
		}
		return new MailboxFolder(folderCreateRequest.getFolderDisplayName(), serverDelimiter);
	}
}
