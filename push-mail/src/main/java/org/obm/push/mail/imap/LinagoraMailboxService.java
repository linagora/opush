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
package org.obm.push.mail.imap;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.obm.configuration.EmailConfiguration;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.configuration.OpushEmailConfiguration;
import org.obm.push.exception.CollectionPathException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.ImapMessageNotFoundException;
import org.obm.push.exception.ImapTimeoutException;
import org.obm.push.exception.MailException;
import org.obm.push.exception.MailboxNotFoundException;
import org.obm.push.exception.OpushLocatorException;
import org.obm.push.exception.UnsupportedBackendFunctionException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.TimeoutException;
import org.obm.push.mail.EmailFactory;
import org.obm.push.mail.MailboxService;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.EmailMetadata;
import org.obm.push.mail.bean.EmailReader;
import org.obm.push.mail.bean.FastFetch;
import org.obm.push.mail.bean.Flag;
import org.obm.push.mail.bean.FlagsList;
import org.obm.push.mail.bean.IMAPHeaders;
import org.obm.push.mail.bean.ImapCapability;
import org.obm.push.mail.bean.ListInfo;
import org.obm.push.mail.bean.ListResult;
import org.obm.push.mail.bean.MailboxFolder;
import org.obm.push.mail.bean.MailboxFolders;
import org.obm.push.mail.bean.MessageSet;
import org.obm.push.mail.bean.SearchQuery;
import org.obm.push.mail.bean.UIDEnvelope;
import org.obm.push.mail.mime.MimeAddress;
import org.obm.push.mail.mime.MimeMessage;
import org.obm.push.mail.mime.MimePart;
import org.obm.push.minig.imap.CommandIOException;
import org.obm.push.minig.imap.StoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LinagoraMailboxService implements MailboxService {

	private static final Logger logger = LoggerFactory.getLogger(LinagoraMailboxService.class);

	private static final String NO_REFERENCE_NAME = "";

	private final boolean activateTLS;
	private final boolean loginWithDomain;
	private final LinagoraImapClientProvider imapClientProvider;

	@Inject
	LinagoraMailboxService(OpushEmailConfiguration emailConfiguration, 
			LinagoraImapClientProvider imapClientProvider) {
		
		this.imapClientProvider = imapClientProvider;
		this.activateTLS = emailConfiguration.activateTls();
		this.loginWithDomain = emailConfiguration.loginWithDomain();
	}

	@Override
	public EmailMetadata fetchEmailMetadata(UserDataRequest udr, MailboxPath path, long uid)
			throws MailException, ItemNotFoundException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			EmailMetadata response = store.uidFetchEmailMetadata(uid);
			if (response != null) {
				return response;
			}
			throw new ItemNotFoundException("Cannot find expected response:{" + uid + "} in imap results");
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public Map<Long, FlagsList> fetchFlags(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			Map<Long, FlagsList> fetchFlags = store.uidFetchFlags(messages);
			return fetchFlags;
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public boolean folderExists(UserDataRequest udr, MailboxPath path) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			return !store.listAll(NO_REFERENCE_NAME, path.getPath()).isEmpty();
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	@Override
	public Optional<MailboxFolder> getFolder(UserDataRequest udr, MailboxPath path) 
			throws MailException {
		try {
			ListResult listFolders = 
					imapClientProvider.getImapClient(udr).listAll(NO_REFERENCE_NAME, path.getPath());
			
			return Iterables.tryFind(mailboxFolders(listFolders), Predicates.alwaysTrue());
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	@Override
	public MailboxFolders listAllFolders(UserDataRequest udr) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			return mailboxFolders(store.listAll());
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	@Override
	public MailboxFolders listSubscribedFolders(UserDataRequest udr) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			return mailboxFolders(store.listSubscribed());
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	private MailboxFolders mailboxFolders(ListResult listResult) {
		List<MailboxFolder> mailboxFolders = Lists.newArrayList();
		for (ListInfo folder: listResult) {
			mailboxFolders.add(
					new MailboxFolder(folder.getName(), listResult.getImapSeparator()));
		}
		return new MailboxFolders(mailboxFolders);
	}

	@Override
	public void createFolder(UserDataRequest udr, MailboxFolder folder) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			if (!store.create(folder.getName())) {
				throw new MailException("Folder creation failed for : " + folder.getName());
			}
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	public void subscribeToFolder(UserDataRequest udr, MailboxFolder folder) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			if (!store.subscribe(folder.getName())) {
				throw new MailException("Mailbox subscription failed for : " + folder.getName());
			}
		} catch (OpushLocatorException e) {
			throw new MailException(e);
		} catch (IMAPException e) {
			throw new MailException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public void updateReadFlag(UserDataRequest udr, MailboxPath path, MessageSet messages, boolean read) 
			throws MailException, ImapMessageNotFoundException {
		
		updateMailFlag(udr, path, messages, Flag.SEEN, read);
	}

	private void updateMailFlag(UserDataRequest udr, MailboxPath path, MessageSet messages, Flag flag, 
			boolean status) throws MailException {
		
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			FlagsList fl = new FlagsList();
			fl.add(flag);
			store.uidStore(messages, fl, status);
			logger.info("Change flag for mail with UID {} in {} ( {}:{} )",
					messages, path.getPath(), flag.asCommandValue(), status);
		} catch (IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public String parseMailBoxName(UserDataRequest udr, MailboxPath path) throws MailException {
		try {
			return imapClientProvider.getImapClient(udr).findMailboxNameWithServerCase(path.getPath());
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	 
	@Override
	public void delete(UserDataRequest udr, MailboxPath path, MessageSet messages) 
			throws MailException, ImapMessageNotFoundException {

		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			logger.info("delete conv id = {}", messages);
			store.uidStore(messages, fl, true);
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public MessageSet move(UserDataRequest udr, MailboxPath srcFolder, MailboxPath dstFolder, MessageSet messages)
			throws DaoException, MailException, ImapMessageNotFoundException, UnsupportedBackendFunctionException {
		
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			assertMoveItemIsSupported(store);
			
			logger.debug("Moving email, USER:{} UIDs:{} SRC:{} DST:{}",
					udr.getUser().getLoginAtDomain(), messages, srcFolder, dstFolder);
			
			store.select(srcFolder.getPath());
			MessageSet newUids = store.uidCopy(messages, dstFolder.getPath());
			deleteMessage(store, messages);
			
			return newUids;
		} catch (IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	private void deleteMessage(StoreClient store, MessageSet messages) throws ImapTimeoutException {
		FlagsList fl = new FlagsList();
		fl.add(Flag.DELETED);
		logger.info("delete conv id = {}", messages);
		store.uidStore(messages, fl, true);
	}
	
	private void assertMoveItemIsSupported(StoreClient store) throws UnsupportedBackendFunctionException, ImapTimeoutException {
		if (!store.capabilities().contains(ImapCapability.UIDPLUS.capability())) {
			throw new UnsupportedBackendFunctionException("The IMAP server doesn't support UIDPLUS capability");
		}
	}

	@Override
	public InputStream fetchMailStream(UserDataRequest udr, MailboxPath path, long uid, Optional<Long> truncation) throws MailException {
		return getMessageInputStream(udr, path, uid, truncation);
	}

	private InputStream getMessageInputStream(UserDataRequest udr, MailboxPath path, long messageUID, Optional<Long> truncation) 
			throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			if (truncation.isPresent()) {
				return store.uidFetchMessage(messageUID, truncation.get());
			} else {
				return store.uidFetchMessage(messageUID);
			}
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public void setAnsweredFlag(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException, ImapMessageNotFoundException {
		updateMailFlag(udr, path, messages, Flag.ANSWERED, true);
	}

	@Override
	public void setDeletedFlag(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException, ImapMessageNotFoundException {
		updateMailFlag(udr, path, messages, Flag.DELETED, true);
	}	
	
	@Override
	public void storeInSent(UserDataRequest udr, EmailReader mailContent) throws MailException {
		logger.info("Store mail in folder[SentBox]");
		if (mailContent != null) {
			MailboxPath sentboxPath = MailboxPath.of(EmailConfiguration.IMAP_SENT_NAME);
			storeInFolder(udr, mailContent, true, sentboxPath);
		} else {
			throw new MailException("The mail that user try to store in sent box is null.");
		}
	}

	private void resetInputStream(Reader mailContent) throws IOException {
		try {
			mailContent.reset();
		} catch (IOException e) {
			mailContent.mark(0);
			mailContent.reset();
		}
	}
	
	@Override
	public InputStream findAttachment(UserDataRequest udr, MailboxPath path, Long mailUid, MimeAddress mimePartAddress)
			throws MailException {
		
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			return store.uidFetchPart(mailUid, Objects.firstNonNull(mimePartAddress.getAddress(), ""));
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public MessageSet purgeFolder(UserDataRequest udr, Integer devId, MailboxPath path) 
			throws DaoException, MailException {
		
		long time = System.currentTimeMillis();
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			logger.info("Mailbox folder[ {} ] will be purged...", path.getPath());
			MessageSet messages = store.uidSearch(SearchQuery.MATCH_ALL);
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			store.uidStore(messages, fl, true);
			time = System.currentTimeMillis() - time;
			logger.info("Mailbox folder[ {} ] was purged in {} millisec. {} messages have been deleted",
					path.getPath(), time, Iterables.size(messages));
			return messages;
		} catch (IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
		
	@Override
	public void storeInInbox(UserDataRequest udr, EmailReader mailContent, boolean isRead) throws MailException {
		logger.info("Store mail in folder[Inbox]");
		MailboxPath inboxPath = MailboxPath.of(EmailConfiguration.IMAP_INBOX_NAME);
		storeInFolder(udr, mailContent, isRead, inboxPath);
	}
	
	private void storeInFolder(UserDataRequest udr, Reader mailContent, boolean isRead, MailboxPath path) 
			throws MailException {

		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			FlagsList fl = new FlagsList();
			if(isRead){
				fl.add(Flag.SEEN);
			}
			resetInputStream(mailContent);
			store.append(path.getPath(), mailContent, fl);
		} catch (IMAPException | CommandIOException | IOException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public boolean getLoginWithDomain() {
		return loginWithDomain;
	}

	@Override
	public boolean getActivateTLS() {
		return activateTLS;
	}

	@Override
	public Collection<Email> fetchEmails(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException {
		return EmailFactory.listEmailFromFastFetch(fetchFast(udr, path, messages));
	}
	
	@Override
	public Set<Email> fetchEmails(UserDataRequest udr, MailboxPath path, Date windows) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			SearchQuery query = SearchQuery.builder().afterInclusive(windows).build();
			MessageSet messages = store.uidSearch(query);
			Collection<FastFetch> mails = fetchFast(udr, path, messages);
			return EmailFactory.listEmailFromFastFetch(mails);
		} catch (IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	@Override
	public Collection<UIDEnvelope> fetchEnvelope(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			return store.uidFetchEnvelope(messages);
		} catch (NoSuchElementException | IMAPException | CollectionPathException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	@Override
	public Collection<FastFetch> fetchFast(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			return FluentIterable
					.from(store.uidFetchFast(messages))
					.filter(new Predicate<FastFetch>() {
						@Override
						public boolean apply(FastFetch input) {
							return !input.isDeleted();
						}
					}).toList();
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	@Override
	public Collection<MimeMessage> fetchBodyStructure(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			return store.uidFetchBodyStructure(messages);
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	@Override
	public InputStream fetchMimePartStream(UserDataRequest udr, MailboxPath path, long uid, MimeAddress partAddress)
			throws MailException {
		Preconditions.checkNotNull(partAddress);
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			
			String addressAsString = Objects.firstNonNull(partAddress.getAddress(), "");
			return store.uidFetchPart(uid, addressAsString);
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	@Override
	public InputStream fetchPartialMimePartStream(UserDataRequest udr,
			MailboxPath path, long uid, MimeAddress partAddress, int limit)
			throws MailException {
		Preconditions.checkNotNull(partAddress);
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			
			String addressAsString = Objects.firstNonNull(partAddress.getAddress(), "");
			return store.uidFetchPart(uid, addressAsString, limit);
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public Map<Long, IMAPHeaders> fetchPartHeaders(UserDataRequest udr, MailboxPath path, MessageSet messages, MimePart mimePart) throws IOException {
		ImmutableMap.Builder<Long, IMAPHeaders> builder = ImmutableMap.builder();
		for (long uid: messages) {
			builder.put(uid, fetchPartHeaders(udr, path, uid, mimePart));
		}
		return builder.build();
	}

	private IMAPHeaders fetchPartHeaders(UserDataRequest udr, MailboxPath path, long uid, MimePart mimePart) throws IOException {
		MimeAddress address = mimePart.getAddress();
		String part = null;
		if (address == null) {
			part = "HEADER";
		} else {
			part = address.getAddress() + ".HEADER";
		}
		InputStream is = fetchMimePartStream(udr, path, uid, new MimeAddress(part));
		return mimePart.decodeHeaders(is);
	}
	
	@Override
	public long fetchUIDNext(UserDataRequest udr, MailboxPath path) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			return store.uidNext(path.getPath());
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}

	@Override
	public long fetchUIDValidity(UserDataRequest udr, MailboxPath path) throws MailException {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			return store.uidValidity(path.getPath());
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
	
	@Override
	public void expunge(UserDataRequest udr, MailboxPath path) {
		try {
			StoreClient store = imapClientProvider.getImapClient(udr);
			store.select(path.getPath());
			store.expunge();
		} catch (OpushLocatorException | IMAPException e) {
			throw new MailException(e);
		} catch (MailboxNotFoundException e) {
			throw new CollectionNotFoundException(e);
		} catch (ImapTimeoutException e) {
			throw new TimeoutException(e);
		}
	}
}
