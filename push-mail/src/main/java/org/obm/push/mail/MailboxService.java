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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.ImapMessageNotFoundException;
import org.obm.push.exception.MailException;
import org.obm.push.exception.UnsupportedBackendFunctionException;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.EmailMetadata;
import org.obm.push.mail.bean.EmailReader;
import org.obm.push.mail.bean.FastFetch;
import org.obm.push.mail.bean.FlagsList;
import org.obm.push.mail.bean.IMAPHeaders;
import org.obm.push.mail.bean.MailboxFolder;
import org.obm.push.mail.bean.MailboxFolders;
import org.obm.push.mail.bean.MessageSet;
import org.obm.push.mail.bean.UIDEnvelope;
import org.obm.push.mail.mime.MimeAddress;
import org.obm.push.mail.mime.MimeMessage;
import org.obm.push.mail.mime.MimePart;

import com.google.common.base.Optional;

public interface MailboxService {
	
	MailboxFolders listSubscribedFolders(UserDataRequest udr) throws MailException;
	
	void updateReadFlag(UserDataRequest udr, MailboxPath path, MessageSet messages, boolean read) throws MailException, ImapMessageNotFoundException;

	String parseMailBoxName(UserDataRequest udr, MailboxPath path) throws MailException;

	void delete(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException, ImapMessageNotFoundException;

	MessageSet move(UserDataRequest udr, MailboxPath srcFolder, MailboxPath dstFolder, MessageSet messages)
			throws MailException, DaoException, ImapMessageNotFoundException, UnsupportedBackendFunctionException;

	InputStream fetchMailStream(UserDataRequest udr, MailboxPath path, long uid, Optional<Long> truncation) throws MailException;

	void setAnsweredFlag(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException, ImapMessageNotFoundException;

	void setDeletedFlag(UserDataRequest udr, MailboxPath path, MessageSet messages);
	
	InputStream findAttachment(UserDataRequest udr, MailboxPath path, Long mailUid, MimeAddress mimePartAddress) throws MailException;

	MessageSet purgeFolder(UserDataRequest udr, Integer devId, MailboxPath path) throws MailException, DaoException;

	/**
	 * Store the mail's inputstream in INBOX.
	 * The mailContent is only guaranteed to be streamed if it's a SharedInputStream.
	 */
	void storeInInbox(UserDataRequest udr, EmailReader mailContent, boolean isRead) throws MailException;

	boolean getLoginWithDomain();

	boolean getActivateTLS();
	
	Collection<Email> fetchEmails(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException;

	Set<Email> fetchEmails(UserDataRequest udr, MailboxPath path, Date windows) throws MailException;

	MailboxFolders listAllFolders(UserDataRequest udr) throws MailException;
	
	void createFolder(UserDataRequest udr, MailboxFolder folder) throws MailException;

	void subscribeToFolder(UserDataRequest udr, MailboxFolder folder) throws MailException;
	
	Collection<FastFetch> fetchFast(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException;

	Collection<MimeMessage> fetchBodyStructure(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException;

	Map<Long, FlagsList> fetchFlags(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException;
	
	EmailMetadata fetchEmailMetadata(UserDataRequest udr, MailboxPath path, long uid) throws MailException;

	InputStream fetchPartialMimePartStream(UserDataRequest udr, MailboxPath path, long uid, MimeAddress partAddress, int limit) 
			throws MailException;

	InputStream fetchMimePartStream(UserDataRequest udr, MailboxPath path, long uid, MimeAddress partAddress) 
			throws MailException;
	
	Collection<UIDEnvelope> fetchEnvelope(UserDataRequest udr, MailboxPath path, MessageSet messages) throws MailException;

	Map<Long, IMAPHeaders> fetchPartHeaders(UserDataRequest udr, MailboxPath path, MessageSet uid, MimePart mimePart) throws IOException;

	void storeInSent(UserDataRequest udr, EmailReader mailContent) throws MailException;

	long fetchUIDNext(UserDataRequest udr, MailboxPath path) throws MailException;
	
	long fetchUIDValidity(UserDataRequest udr, MailboxPath path) throws MailException;

	void expunge(UserDataRequest udr, MailboxPath path);

	boolean folderExists(UserDataRequest udr, MailboxPath path) throws MailException;

	Optional<MailboxFolder> getFolder(UserDataRequest udr, MailboxPath path) throws MailException;
}
