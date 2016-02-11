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

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.obm.DateUtils.date;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.easymock.IMocksControl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.SnapshotKey;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.WindowingKey;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.mail.MailBackendSyncData.MailBackendSyncDataFactory;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.MessageSet;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.mail.exception.FilterTypeChangedException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.DateService;
import org.obm.push.store.SnapshotDao;
import org.obm.push.store.WindowingToSnapshotDao;
import org.obm.push.utils.DateUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;


public class MailBackendSyncDataTest {

	private UserDataRequest udr;
	private CollectionId collectionId;
	private DeviceId devId;
	private Device device;
	private User user;
	private MailboxPath inboxPath;
	private Folder inboxFolder;

	private IMocksControl control;
	private MailboxService mailboxService;
	private SnapshotDao snapshotDao;
	private WindowingToSnapshotDao windowingToSnapshotDao;
	private EmailChangesComputer emailChangesComputer;
	private DateService dateService;
	private MailBackendSyncDataFactory testee;

	@Before
	public void setup() {
		collectionId = CollectionId.of(13411);
		user = Factory.create().createUser("test@test", "test@domain", "displayName");
		devId = new DeviceId("my phone");
		device = new Device.Factory().create(null, "MyPhone", "MyUA", devId, null);
		udr = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		
		inboxPath = MailboxPath.of("INBOX");
		inboxFolder = Folder.builder()
				.backendId(inboxPath)
				.collectionId(collectionId)
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("displayName")
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.build();
		
		control = createControl();
		mailboxService = control.createMock(MailboxService.class);
		snapshotDao = control.createMock(SnapshotDao.class);
		windowingToSnapshotDao = control.createMock(WindowingToSnapshotDao.class);
		emailChangesComputer = control.createMock(EmailChangesComputer.class);
		dateService = control.createMock(DateService.class);
		
		testee = new MailBackendSyncDataFactory(dateService, mailboxService, snapshotDao, 
				windowingToSnapshotDao, emailChangesComputer);
	}
	
	@Test
	public void testGetManagedEmailsIsEmptyForNull() {
		control.replay();
		assertThat(testee.getManagedEmails(null)).isEmpty();
		control.verify();
	}
	
	@Test
	public void testGetManagedEmailsAreTookFromSnapshot() {
		Snapshot snapshot = Snapshot.builder()
			.addEmail(Email.builder()
				.uid(5)
				.date(date("2004-12-14T22:00:00"))
				.read(false)
				.answered(false)
				.build())
			.addEmail(Email.builder()
				.uid(15)
				.date(date("2014-12-14T22:00:00"))
				.read(true)
				.answered(true)
				.build())
			.filterType(FilterType.ALL_ITEMS)
			.uidNext(5000)
			.build();

		control.replay();
		assertThat(testee.getManagedEmails(snapshot)).containsOnly(
			Email.builder()
				.uid(5)
				.date(date("2004-12-14T22:00:00"))
				.read(false)
				.answered(false)
				.build(),
			Email.builder()
				.uid(15)
				.date(date("2014-12-14T22:00:00"))
				.read(true)
				.answered(true)
				.build());
		control.verify();
	}
	
	@Test
	public void testMustSyncByDateIsTrueWhenNoSnapshot() {
		control.replay();
		assertThat(testee.mustSyncByDate(null)).isTrue();
		control.verify();
	}
	
	@Test
	public void testMustSyncByDateIsFalseWhenPreviousSnapshot() {
		Snapshot snapshot = Snapshot.builder()
				.addEmail(Email.builder()
					.uid(5)
					.date(date("2004-12-14T22:00:00"))
					.read(false)
					.answered(false)
					.build())
				.filterType(FilterType.ALL_ITEMS)
				.uidNext(5000)
				.build();

		control.replay();
		assertThat(testee.mustSyncByDate(snapshot)).isFalse();
		control.verify();
	}
	
	@Test
	public void testSearchEmailsToManagerIsByDateForNullWithoutEmails() throws FilterTypeChangedException {
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();
		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		
		Set<Email> emailsExpected = ImmutableSet.of();
		expect(mailboxService.fetchEmails(udr, inboxPath, fromDate))
			.andReturn(emailsExpected);

		control.replay();
		Collection<Email> result = testee.searchEmailsToManage(udr, inboxPath, null, syncCollectionOptions, date("2004-10-14T22:00:00"), 0);
		control.verify();
		
		assertThat(result).isEmpty();
	}
	
	@Test
	public void testSearchEmailsToManagerIsByDateForNullWithEmails() throws FilterTypeChangedException {
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();
		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		
		expect(mailboxService.fetchEmails(udr, inboxPath, fromDate))
			.andReturn(ImmutableSet.of(
					Email.builder()
					.uid(5)
					.date(date("2004-12-14T22:00:00"))
					.read(false)
					.answered(false)
					.build()));

		control.replay();
		Collection<Email> result = testee.searchEmailsToManage(udr, inboxPath, null, syncCollectionOptions, date("2004-10-14T22:00:00"), 0);
		control.verify();
		
		assertThat(result).containsOnly(
			Email.builder()
				.uid(5)
				.date(date("2004-12-14T22:00:00"))
				.read(false)
				.answered(false)
				.build());
	}
	
	@Test(expected=FilterTypeChangedException.class)
	public void testSearchEmailsToManagerThrowExecptionWhenDifferentFolderType() throws FilterTypeChangedException {
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Snapshot snapshot = Snapshot.builder()
				.addEmail(Email.builder()
					.uid(5)
					.date(date("2004-12-14T22:00:00"))
					.read(false)
					.answered(false)
					.build())
				.filterType(FilterType.ONE_MONTHS_BACK)
				.uidNext(5000)
				.build();
				
		control.replay();
		testee.searchEmailsToManage(udr, inboxPath, snapshot, syncCollectionOptions, date("2004-10-14T22:00:00"), 0);
	}
	
	@Test
	public void testSearchEmailsToManagerIsByUIDsWhenPreviousSnapshot() throws FilterTypeChangedException {
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		long snapedEmailUID = 5;
		long deletedEmailUID = 6;
		Email snapedEmail = Email.builder()
				.uid(snapedEmailUID)
				.date(date("2004-12-14T22:00:00"))
				.read(false)
				.answered(false)
				.build();
		Email modifiedEmail = Email.builder()
				.uid(snapedEmailUID)
				.date(date("2004-12-14T22:00:00"))
				.read(true)
				.answered(false)
				.build();
		Email deletedEmail = Email.builder()
				.uid(deletedEmailUID)
				.date(date("2004-12-14T22:00:00"))
				.read(true)
				.answered(false)
				.build();
		
		long newEmailUID = 9;
		Email newEmail = Email.builder()
				.uid(newEmailUID)
				.date(date("2004-12-14T22:00:00"))
				.read(false)
				.answered(false)
				.build();
		
		long previousUIDNext = 8;
		long currentUIDNext = 10;
		ImmutableList<Email> expectedEmails = ImmutableList.of(modifiedEmail, newEmail);
		expect(mailboxService.fetchEmails(udr, inboxPath, 
				MessageSet.builder().add(snapedEmailUID)
					.add(deletedEmailUID).add(previousUIDNext).add(newEmailUID).add(currentUIDNext).build()))
			.andReturn(expectedEmails).once();

		Snapshot snapshot = Snapshot.builder()
				.addEmail(snapedEmail)
				.addEmail(deletedEmail)
				.filterType(FilterType.ALL_ITEMS)
				.uidNext(previousUIDNext)
				.build();
		
		control.replay();
		Collection<Email> searchEmailsToManage = testee.searchEmailsToManage(udr, inboxPath, snapshot, syncCollectionOptions, date("2004-10-14T22:00:00"), currentUIDNext);
		
		control.verify();
		assertThat(searchEmailsToManage).isEqualTo(expectedEmails);
	}
	
	@Test
	public void searchEmailsToManageShouldNotCheckOptionsWhenFirstSync() {
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		long newEmailUID = 9;
		Email newEmail = Email.builder()
				.uid(newEmailUID)
				.date(date("2004-12-14T22:00:00"))
				.read(false)
				.answered(false)
				.build();
		
		long currentUIDNext = 10;
		Set<Email> expectedEmails = ImmutableSet.of(newEmail);
		expect(mailboxService.fetchEmails(udr, inboxPath, DateUtils.getEpochPlusOneSecondCalendar().getTime()))
			.andReturn(expectedEmails).once();

		control.replay();
		testee.searchEmailsToManage(udr, inboxPath, null, syncCollectionOptions, date("2004-10-14T22:00:00"), currentUIDNext);
		control.verify();
	}

	@Test(expected=FilterTypeChangedException.class)
	public void testSyncByDateWhenFilterTypeChanged() throws Exception {
		SyncKey syncKey = new SyncKey("1234");
		long uidNext = 45612;
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Email email = Email.builder()
				.uid(5)
				.date(date("2004-12-14T22:00:00"))
				.read(false)
				.answered(false)
				.build();
		Set<Email> previousEmailsInServer = ImmutableSet.of(email);
		
		Snapshot snapshot = Snapshot.builder()
				.emails(previousEmailsInServer)
				.filterType(FilterType.THREE_DAYS_BACK)
				.uidNext(5000)
				.build();

		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		expect(dateService.getCurrentDate()).andReturn(fromDate);
		expectSnapshotDaoHasEntry(syncKey, snapshot);
		expect(windowingToSnapshotDao.get(new WindowingKey(user, devId, collectionId, syncKey)))
			.andReturn(Optional.<UUID>absent());
		
		expect(mailboxService.fetchUIDNext(udr, inboxPath)).andReturn(uidNext);
		
		control.replay();
		try {
			testee.create(udr, ItemSyncState.builder()
					.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
					.syncKey(syncKey)
					.build(), 
					inboxFolder, syncCollectionOptions);
		} finally {
			control.verify();
		}
	}
	
	@Test
	public void testMailBackendSyncDataCreationInitialWhenNoChange() throws Exception {
		SyncKey syncKey = SyncKey.INITIAL_SYNC_KEY;
		long uidNext = 45612;
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Set<Email> previousEmailsInServer = ImmutableSet.of();
		Set<Email> actualEmailsInServer = ImmutableSet.of();
		EmailChanges emailChanges = EmailChanges.builder().build();

		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		expect(dateService.getCurrentDate()).andReturn(fromDate);
		expect(windowingToSnapshotDao.get(new WindowingKey(user, devId, collectionId, syncKey)))
			.andReturn(Optional.<UUID>absent());
		expectSnapshotDaoHasNoEntry(syncKey);
		expectActualEmailServerStateByDate(actualEmailsInServer, fromDate, uidNext);
		expectEmailsDiff(previousEmailsInServer, actualEmailsInServer, emailChanges);
		
		control.replay();
		MailBackendSyncData syncData = testee.create(udr, ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build(), 
				inboxFolder, syncCollectionOptions);
		control.verify();
		
		assertThat(syncData.getDataDeltaDate()).isEqualTo(fromDate);
		assertThat(syncData.getCurrentUIDNext()).isEqualTo(uidNext);
		assertThat(syncData.getPreviousStateSnapshot()).isEqualTo(null);
		assertThat(syncData.getManagedEmails()).isEqualTo(previousEmailsInServer);
		assertThat(syncData.getNewManagedEmails()).isEqualTo(actualEmailsInServer);
		assertThat(syncData.getEmailChanges()).isEqualTo(emailChanges);
	}

	@Test
	public void testMailBackendSyncDataCreationInitialWhithChanges() throws Exception {
		SyncKey syncKey = SyncKey.INITIAL_SYNC_KEY;
		long uidNext = 45612;
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Email email1 = Email.builder().uid(245).read(false).date(date("2004-12-14T22:00:00")).build();
		Email email2 = Email.builder().uid(546).read(true).date(date("2012-12-12T23:59:00")).build();
		
		Set<Email> previousEmailsInServer = ImmutableSet.of();
		Set<Email> actualEmailsInServer = ImmutableSet.of(email1, email2);
		EmailChanges emailChanges = EmailChanges.builder().additions(actualEmailsInServer).build();

		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		expect(dateService.getCurrentDate()).andReturn(fromDate);
		expect(windowingToSnapshotDao.get(new WindowingKey(user, devId, collectionId, syncKey)))
			.andReturn(Optional.<UUID>absent());
		expectSnapshotDaoHasNoEntry(syncKey);
		expectActualEmailServerStateByDate(actualEmailsInServer, fromDate, uidNext);
		expectEmailsDiff(previousEmailsInServer, actualEmailsInServer, emailChanges);
		
		control.replay();
		MailBackendSyncData syncData = testee.create(udr, ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build(), 
				inboxFolder, syncCollectionOptions);
		control.verify();
		
		assertThat(syncData.getDataDeltaDate()).isEqualTo(fromDate);
		assertThat(syncData.getCurrentUIDNext()).isEqualTo(uidNext);
		assertThat(syncData.getPreviousStateSnapshot()).isEqualTo(null);
		assertThat(syncData.getManagedEmails()).isEqualTo(previousEmailsInServer);
		assertThat(syncData.getNewManagedEmails()).isEqualTo(actualEmailsInServer);
		assertThat(syncData.getEmailChanges()).isEqualTo(emailChanges);
	}

	@Test
	public void testMailBackendSyncDataCreationNoChange() throws Exception {
		SyncKey syncKey = new SyncKey("1");
		long uidNext = 10;
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Email email = Email.builder().uid(2).read(false).date(date("2004-12-14T22:00:00")).build();
		Set<Email> emailsInServer = ImmutableSet.of(email);
		
		Snapshot snapshot = Snapshot.builder()
				.emails(emailsInServer)
				.filterType(FilterType.ALL_ITEMS)
				.uidNext(2)
				.build();
		
		EmailChanges emailChanges = EmailChanges.builder().build();

		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		expect(dateService.getCurrentDate()).andReturn(fromDate);
		expectSnapshotDaoHasEntry(syncKey, snapshot);
		expect(windowingToSnapshotDao.get(new WindowingKey(user, devId, collectionId, syncKey)))
			.andReturn(Optional.<UUID>absent());

		MessageSet messages = MessageSet.builder().add(Range.closed(2l, 10l)).build();
		expect(mailboxService.fetchEmails(udr, inboxPath, messages)).andReturn(emailsInServer).once();
		expect(mailboxService.fetchUIDNext(udr, inboxPath)).andReturn(uidNext);
	
		expectEmailsDiff(ImmutableList.copyOf(emailsInServer), emailsInServer, emailChanges);
		
		control.replay();
		MailBackendSyncData syncData = testee.create(udr, ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build(), 
				inboxFolder, syncCollectionOptions);
		control.verify();
		
		assertThat(syncData.getDataDeltaDate()).isEqualTo(fromDate);
		assertThat(syncData.getCurrentUIDNext()).isEqualTo(uidNext);
		assertThat(syncData.getPreviousStateSnapshot()).isEqualTo(snapshot);
		assertThat(syncData.getManagedEmails()).isEqualTo(ImmutableList.copyOf(emailsInServer));
		assertThat(syncData.getNewManagedEmails()).isEqualTo(emailsInServer);
		assertThat(syncData.getEmailChanges()).isEqualTo(emailChanges);
	}

	@Test
	public void testMailBackendSyncDataCreationWithChanges() throws Exception {
		SyncKey syncKey = new SyncKey("1");
		long uidNext = 10;
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Email deletedEmail = Email.builder().uid(2).read(false).date(date("2004-12-14T22:00:00")).build();
		Email modifiedEmail = Email.builder().uid(3).read(false).date(date("2004-12-14T22:00:00")).build();
		Email modifiedEmail2 = Email.builder().uid(3).read(true).date(date("2004-12-14T22:00:00")).build();
		Email newEmail = Email.builder().uid(4).read(false).date(date("2004-12-14T22:00:00")).build();
		Set<Email> previousEmailsInServer = ImmutableSet.of(deletedEmail, modifiedEmail);
		Set<Email> actualEmailsInServer = ImmutableSet.of(modifiedEmail2, newEmail);
		
		Snapshot snapshot = Snapshot.builder()
				.emails(previousEmailsInServer)
				.filterType(FilterType.ALL_ITEMS)
				.uidNext(2)
				.build();
		
		
		EmailChanges emailChanges = EmailChanges.builder()
				.additions(ImmutableSet.<Email>of(newEmail))
				.changes(ImmutableSet.<Email>of(modifiedEmail2))
				.deletions(ImmutableSet.<Email>of(deletedEmail))
				.build();

		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		expect(dateService.getCurrentDate()).andReturn(fromDate);
		expectSnapshotDaoHasEntry(syncKey, snapshot);
		expect(windowingToSnapshotDao.get(new WindowingKey(user, devId, collectionId, syncKey)))
			.andReturn(Optional.<UUID>absent());
		
		MessageSet messages = MessageSet.builder().add(Range.closed(2l, 10l)).build();
		expect(mailboxService.fetchEmails(udr, inboxPath, messages)).andReturn(actualEmailsInServer).once();
		expect(mailboxService.fetchUIDNext(udr, inboxPath)).andReturn(uidNext);
		expectEmailsDiff(ImmutableList.copyOf(previousEmailsInServer), actualEmailsInServer, emailChanges);
		
		control.replay();
		MailBackendSyncData syncData = testee.create(udr, ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build(), 
				inboxFolder, syncCollectionOptions);
		control.verify();
		
		assertThat(syncData.getDataDeltaDate()).isEqualTo(fromDate);
		assertThat(syncData.getCurrentUIDNext()).isEqualTo(uidNext);
		assertThat(syncData.getPreviousStateSnapshot()).isEqualTo(snapshot);
		assertThat(syncData.getManagedEmails()).isEqualTo(ImmutableList.copyOf(previousEmailsInServer));
		assertThat(syncData.getNewManagedEmails()).isEqualTo(actualEmailsInServer);
		assertThat(syncData.getEmailChanges()).isEqualTo(emailChanges);
	}

	@Test
	public void testSearchEmailsFromDateNoFilterType() {
		DateTime dateTime = new DateTime(DateUtils.getCurrentDate()).withZone(DateTimeZone.UTC);
		Date expectedDate = dateTime.minusDays(3).toDate();

		control.replay();
		Date searchEmailsFromDate = testee.searchEmailsFromDate(FilterType.THREE_DAYS_BACK, dateTime.toDate());
		
		control.verify();
		assertThat(searchEmailsFromDate).isEqualTo(expectedDate);
	}

	@Test
	public void testSearchEmailsFromDateWithFilterType() {
		Date date = DateUtils.getEpochPlusOneSecondCalendar().getTime();
		
		control.replay();
		Date searchEmailsFromDate = testee.searchEmailsFromDate(null, null);
		
		control.verify();
		assertThat(searchEmailsFromDate).isEqualTo(date);
	}

	@Test
	public void getPreviousSnapshotShouldRequestWindowingDaoThenSnapshotDao() {
		SyncKey syncKey = new SyncKey("cc1b58c8-26ad-465a-969f-c4c3b563f885");
		ItemSyncState state = ItemSyncState.builder().syncDate(date("2016-12-14T22:00:00")).syncKey(syncKey).build();
		UUID expectedSnapshotId = UUID.fromString("d022ff60-a960-4e5c-8797-8f643241646c");
		Snapshot expectedSnapshot = Snapshot.builder()
				.addEmail(Email.builder().uid(15).date(date("2016-12-14T21:30:00")).build())
				.filterType(FilterType.ALL_ITEMS)
				.uidNext(5000)
				.build();
		
		expect(windowingToSnapshotDao.get(new WindowingKey(user, devId, collectionId, syncKey)))
			.andReturn(Optional.of(expectedSnapshotId));
		expect(snapshotDao.get(expectedSnapshotId))
			.andReturn(expectedSnapshot);
		
		control.replay();
		Snapshot result = testee.getPreviousSnapshot(udr, state, inboxFolder);
		control.verify();
		
		assertThat(result).isEqualTo(expectedSnapshot);
	}

	@Test
	public void getPreviousSnapshotShouldFallbackOnSnapshotDaoWhenNotFoundInWindowingDao() {
		SyncKey syncKey = new SyncKey("cc1b58c8-26ad-465a-969f-c4c3b563f885");
		ItemSyncState state = ItemSyncState.builder().syncDate(date("2016-12-14T22:00:00")).syncKey(syncKey).build(); 
		Snapshot expectedSnapshot = Snapshot.builder()
				.addEmail(Email.builder().uid(15).date(date("2016-12-14T21:30:00")).build())
				.filterType(FilterType.ALL_ITEMS)
				.uidNext(5000)
				.build();
		
		expect(windowingToSnapshotDao.get(new WindowingKey(user, devId, collectionId, syncKey)))
			.andReturn(Optional.<UUID>absent());
		expect(snapshotDao.get(SnapshotKey.builder().deviceId(devId).syncKey(syncKey).collectionId(collectionId).build()))
			.andReturn(expectedSnapshot);
		
		control.replay();
		Snapshot result = testee.getPreviousSnapshot(udr, state, inboxFolder);
		control.verify();
		
		assertThat(result).isEqualTo(expectedSnapshot);
	}
	
	private void expectActualEmailServerStateByDate(Set<Email> emailsInServer, Date fromDate, long uidNext) {
		expect(mailboxService.fetchEmails(udr, inboxPath, fromDate))
			.andReturn(emailsInServer);
		expect(mailboxService.fetchUIDNext(udr, inboxPath)).andReturn(uidNext);
	}

	private void expectSnapshotDaoHasNoEntry(SyncKey syncKey) {
		expectSnapshotDaoHasEntry(syncKey, null);
	}

	private void expectSnapshotDaoHasEntry(SyncKey syncKey, Snapshot snapshot) {
		expect(snapshotDao.get(SnapshotKey.builder()
				.deviceId(devId)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.build()))
			.andReturn(snapshot);
	}
	
	private void expectEmailsDiff(Collection<Email> previousEmailsInServer, Collection<Email> actualEmailsInServer, EmailChanges diff) {
		expect(emailChangesComputer.computeChanges(previousEmailsInServer, actualEmailsInServer)).andReturn(diff);
	}
	
}
