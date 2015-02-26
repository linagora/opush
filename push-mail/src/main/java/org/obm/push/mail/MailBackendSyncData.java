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
package org.obm.push.mail;

import java.util.Collection;
import java.util.Date;

import org.obm.push.bean.FilterType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.SnapshotKey;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.MessageSet;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.mail.exception.FilterTypeChangedException;
import org.obm.push.minig.imap.impl.MessageSetUtils;
import org.obm.push.service.DateService;
import org.obm.push.store.SnapshotDao;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

public class MailBackendSyncData {
	
	@Singleton
	public static class MailBackendSyncDataFactory {
		
		private final DateService dateService;
		private final MailboxService mailboxService;
		private final SnapshotDao snapshotDao;
		private final EmailChangesComputer emailChangesComputer;

		@Inject
		@VisibleForTesting MailBackendSyncDataFactory(DateService dateService,
				MailboxService mailboxService,
				SnapshotDao snapshotDao,
				EmailChangesComputer emailChangesComputer) {
			
			this.dateService = dateService;
			this.mailboxService = mailboxService;
			this.snapshotDao = snapshotDao;
			this.emailChangesComputer = emailChangesComputer;
		}
		
		public MailBackendSyncData create(UserDataRequest udr, ItemSyncState state, Folder folder, 
				SyncCollectionOptions options) throws ProcessingEmailException, 
				CollectionNotFoundException, DaoException, FilterTypeChangedException {
			
			MailboxPath path = folder.getTypedBackendId();
			Date dataDeltaDate = dateService.getCurrentDate();
			long currentUIDNext = mailboxService.fetchUIDNext(udr, path);

			Snapshot previousStateSnapshot = snapshotDao.get(SnapshotKey.builder()
					.deviceId(udr.getDevId())
					.syncKey(state.getSyncKey())
					.collectionId(folder.getCollectionId())
					.build());
			Collection<Email> managedEmails = getManagedEmails(previousStateSnapshot);
			Collection<Email> newManagedEmails = searchEmailsToManage(
					udr, path, previousStateSnapshot, options, dataDeltaDate, currentUIDNext);
			
			EmailChanges emailChanges = emailChangesComputer.computeChanges(managedEmails, newManagedEmails);
				
			return new MailBackendSyncData(dataDeltaDate, path, currentUIDNext, previousStateSnapshot, managedEmails, newManagedEmails, emailChanges);
		}

		@VisibleForTesting Collection<Email> getManagedEmails(Snapshot previousStateSnapshot) {
			if (previousStateSnapshot != null) {
				return previousStateSnapshot.getEmails();
			}
			return ImmutableSet.of(); 
		}

		@VisibleForTesting Collection<Email> searchEmailsToManage(UserDataRequest udr, MailboxPath path,
				Snapshot previousStateSnapshot, SyncCollectionOptions actualOptions,
				Date dataDeltaDate, long currentUIDNext) throws FilterTypeChangedException {
			
			assertSnapshotHasSameOptionsThanRequest(previousStateSnapshot, actualOptions);
			if (mustSyncByDate(previousStateSnapshot)) {
				Date searchEmailsFromDate = searchEmailsFromDate(actualOptions.getFilterType(), dataDeltaDate);
				return mailboxService.fetchEmails(udr, path, searchEmailsFromDate);
			}
			return searchSnapshotAndActualChanges(udr, path, previousStateSnapshot, currentUIDNext);
		}

		@VisibleForTesting Date searchEmailsFromDate(FilterType filterType, Date dataDeltaDate) {
			return Objects.firstNonNull(filterType, FilterType.ALL_ITEMS).getFilteredDate(dataDeltaDate);	
		}

		private void assertSnapshotHasSameOptionsThanRequest(Snapshot snapshot, SyncCollectionOptions options)
				throws FilterTypeChangedException {
			
			if (!snapshotIsAbsent(snapshot) && filterTypeHasChanged(snapshot, options)) {
				manageFilterTypeChanged(snapshot.getFilterType(), options.getFilterType());
			}
		}

		private void manageFilterTypeChanged(FilterType previousFilterType, FilterType currentFilterType) throws FilterTypeChangedException {
			throw new FilterTypeChangedException(previousFilterType, currentFilterType);
		}

		@VisibleForTesting boolean mustSyncByDate(Snapshot previousStateSnapshot) {
			return snapshotIsAbsent(previousStateSnapshot) || !previousStateSnapshot.getUidNext().isPresent();
		}

		private boolean snapshotIsAbsent(Snapshot previousStateSnapshot) {
			return previousStateSnapshot == null;
		}

		private boolean filterTypeHasChanged(Snapshot snapshot, SyncCollectionOptions options) {
			return snapshot.getFilterType() != options.getFilterType();
		}

		private Collection<Email> searchSnapshotAndActualChanges(UserDataRequest udr, 
				MailboxPath path, Snapshot previousStateSnapshot, long currentUIDNext) {
			
			MessageSet messages = MessageSetUtils.computeEmailsUID(previousStateSnapshot, currentUIDNext);
			return mailboxService.fetchEmails(udr, path, messages);
		}
		
	}
	
	private final Date dataDeltaDate;
	private final MailboxPath mailboxPath;
	private final long currentUIDNext;
	private final Snapshot previousStateSnapshot;
	private final Collection<Email> managedEmails;
	private final Collection<Email> newManagedEmails;
	private final EmailChanges emailChanges;
	
	@VisibleForTesting MailBackendSyncData(Date dataDeltaDate,
			MailboxPath mailboxPath,
			long currentUIDNext,
			Snapshot previousStateSnapshot,
			Collection<Email> managedEmails,
			Collection<Email> newManagedEmails,
			EmailChanges emailChanges) {
		
		this.dataDeltaDate = dataDeltaDate;
		this.mailboxPath = mailboxPath;
		this.currentUIDNext = currentUIDNext;
		this.previousStateSnapshot = previousStateSnapshot;
		this.managedEmails = managedEmails;
		this.newManagedEmails = newManagedEmails;
		this.emailChanges = emailChanges;
	}
	
	public Date getDataDeltaDate() {
		return dataDeltaDate;
	}
	
	public MailboxPath getMailboxPath() {
		return mailboxPath;
	}
	
	public long getCurrentUIDNext() {
		return currentUIDNext;
	}
	
	public Snapshot getPreviousStateSnapshot() {
		return previousStateSnapshot;
	}
	
	public Collection<Email> getManagedEmails() {
		return managedEmails;
	}
	
	public Collection<Email> getNewManagedEmails() {
		return newManagedEmails;
	}
	
	public EmailChanges getEmailChanges() {
		return emailChanges;
	}
	
}
