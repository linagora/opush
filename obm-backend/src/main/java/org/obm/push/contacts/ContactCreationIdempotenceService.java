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
package org.obm.push.contacts;

import org.obm.push.bean.MSContact;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.store.ContactCreationDao;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ContactCreationIdempotenceService {

	private final ContactCreationDao contactCreationDao;

	@Inject
	@VisibleForTesting ContactCreationIdempotenceService(ContactCreationDao contactCreationDao) {
		this.contactCreationDao = contactCreationDao;
	}
	
	public ServerId registerCreation(UserDataRequest udr, MSContact contact, ServerId serverId) {
		contactCreationDao.registerCreation(udr.getUser(), udr.getDevId(), serverId.getCollectionId(), hash(contact), serverId);
		return serverId;
	}
	
	public Optional<ServerId> find(UserDataRequest udr, CollectionId collectionId, MSContact contact) {
		return contactCreationDao.find(udr.getUser(), udr.getDevId(), collectionId, hash(contact));
	}

	public void remove(UserDataRequest udr, CollectionId collectionId, ServerId serverId) {
		contactCreationDao.remove(udr.getUser(), udr.getDevId(), collectionId, serverId);
	}

	@VisibleForTesting HashCode hash(MSContact contact) {
		return Hashing.sha1().newHasher()
			.putUnencodedChars(Strings.nullToEmpty(contact.getLastName()))
			.putUnencodedChars(Strings.nullToEmpty(contact.getFirstName()))
			.putUnencodedChars(Strings.nullToEmpty(contact.getMiddleName()))
			.putUnencodedChars(Strings.nullToEmpty(contact.getFileAs()))
			.putUnencodedChars(Strings.nullToEmpty(contact.getEmail1Address()))
			.putUnencodedChars(Strings.nullToEmpty(contact.getEmail2Address()))
			.putUnencodedChars(Strings.nullToEmpty(contact.getMobilePhoneNumber()))
			.putUnencodedChars(Strings.nullToEmpty(contact.getBusinessPhoneNumber()))
			.hash();
	}
	
}
