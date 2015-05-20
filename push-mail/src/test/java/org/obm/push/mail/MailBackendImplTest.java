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
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.obm.DateUtils.date;
import static org.obm.configuration.EmailConfiguration.IMAP_DRAFTS_NAME;
import static org.obm.configuration.EmailConfiguration.IMAP_INBOX_NAME;
import static org.obm.configuration.EmailConfiguration.IMAP_SENT_NAME;
import static org.obm.configuration.EmailConfiguration.IMAP_TRASH_NAME;
import static org.obm.push.mail.MSMailTestsUtils.loadEmail;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import org.apache.james.mime4j.dom.Message;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.icalendar.ICalendar;
import org.obm.push.backend.DataDelta;
import org.obm.push.bean.Address;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SnapshotKey;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.WindowingChanges;
import org.obm.push.bean.change.WindowingKey;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.bean.change.item.MSEmailChanges;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.configuration.OpushEmailConfiguration;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.EmailViewPartsFetcherException;
import org.obm.push.exception.activesync.InvalidSyncKeyException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.mail.MailBackendSyncData.MailBackendSyncDataFactory;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.EmailReader;
import org.obm.push.mail.bean.MailboxFolder;
import org.obm.push.mail.bean.MailboxFolders;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.mail.transformer.Transformer.TransformersFactory;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.AuthenticationService;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.SmtpSender;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.SnapshotDao;
import org.obm.push.store.WindowingDao;
import org.obm.push.utils.DateUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;


public class MailBackendImplTest {

	private UserDataRequest udr;
	private CollectionId collectionId;
	private MailboxPath collectionPath;
	private DeviceId devId;
	private Device device;
	private User user;
	private Folder folder;

	private IMocksControl control;
	private MailboxService mailboxService;
	private MappingService mappingService;
	private SnapshotDao snapshotDao;
	private EmailChangesFetcher serverEmailChangesBuilder;
	private MSEmailFetcher msEmailFetcher;
	private TransformersFactory transformersFactory;
	private MailBackendSyncDataFactory mailBackendSyncDataFactory;
	private WindowingDao windowingDao;
	private SmtpSender smtpSender;
	private OpushEmailConfiguration emailConfiguration;
	private DateService dateService;
	private AuthenticationService authenticationService;
	private FolderSnapshotDao folderSnapshotDao;

	private MailBackendImpl testee;

	@Before
	public void setup() {
		collectionId = CollectionId.of(13411);
		collectionPath = MailboxPath.of("mailboxCollectionPath");
		user = Factory.create().createUser("user@domain", "user@domain", "user@domain");
		devId = new DeviceId("my phone");
		device = new Device.Factory().create(null, "MultipleCalendarsDevice", "iOs 5", devId, null);
		udr = new UserDataRequest(new Credentials(user, "password".toCharArray()),  null, device);
		folder = Folder.builder()
			.collectionId(collectionId)
			.backendId(collectionPath)
			.displayName(collectionPath.getPath())
			.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
			.parentBackendIdOpt(Optional.<BackendId>absent())
			.build();
		
		control = createControl();
		
		mailboxService = control.createMock(MailboxService.class);
		snapshotDao = control.createMock(SnapshotDao.class);
		mappingService = control.createMock(MappingService.class);
		serverEmailChangesBuilder = control.createMock(EmailChangesFetcher.class);
		msEmailFetcher = control.createMock(MSEmailFetcher.class);
		transformersFactory = control.createMock(TransformersFactory.class);
		mailBackendSyncDataFactory = control.createMock(MailBackendSyncDataFactory.class);
		windowingDao = control.createMock(WindowingDao.class);
		smtpSender = control.createMock(SmtpSender.class);
		dateService = control.createMock(DateService.class);
		emailConfiguration = control.createMock(OpushEmailConfiguration.class);
		authenticationService = control.createMock(AuthenticationService.class);
		folderSnapshotDao = control.createMock(FolderSnapshotDao.class);
		
		testee = new MailBackendImpl(mailboxService, authenticationService, null, null, snapshotDao,
				serverEmailChangesBuilder, mappingService, msEmailFetcher, transformersFactory, mailBackendSyncDataFactory,
				windowingDao, smtpSender, emailConfiguration, dateService, folderSnapshotDao);
	}
	
	@Test
	public void testInitialGetChangesWithInitialSyncKey() throws Exception {
		testInitialGetChangesUsingSyncKey(SyncKey.INITIAL_SYNC_KEY, new SyncKey("1234"));
	}
	
	@Test
	public void testInitialGetChangesWithNotInitialSyncKey() throws Exception {
		testInitialGetChangesUsingSyncKey(new SyncKey("1234"), new SyncKey("5678"));
	}

	private void testInitialGetChangesUsingSyncKey(SyncKey syncKey, SyncKey newSyncKey) throws Exception {
		long uidNext = 45612;
		int windowSize = 10;
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Email email1 = Email.builder().uid(245).read(false).date(date("2004-12-14T22:00:00")).build();
		Email email2 = Email.builder().uid(546).read(true).date(date("2012-12-12T23:59:00")).build();
		MSEmail email1Data = control.createMock(MSEmail.class);
		MSEmail email2Data = control.createMock(MSEmail.class);
		
		Set<Email> previousEmailsInServer = ImmutableSet.of();
		Set<Email> actualEmailsInServer = ImmutableSet.of(email1, email2);
		EmailChanges emailChanges = EmailChanges.builder().additions(actualEmailsInServer).build();

		EmailChanges.Builder changesBuilder = control.createMock(EmailChanges.Builder.class);
		expect(changesBuilder.build()).andReturn(emailChanges);
		
		ItemChange itemChange1 = ItemChange.builder().serverId(collectionId.serverId(245)).isNew(true).data(email1Data).build();
		ItemChange itemChange2 = ItemChange.builder().serverId(collectionId.serverId(546)).isNew(true).data(email2Data).build();
		MSEmailChanges itemChanges = MSEmailChanges.builder()
			.changes(ImmutableList.of(itemChange1, itemChange2))
			.build();

		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build();
		
		AnalysedSyncCollection syncCollectionRequest = AnalysedSyncCollection.builder()
				.dataType(PIMDataType.EMAIL)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.windowSize(windowSize)
				.options(syncCollectionOptions)
				.build();

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		
		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		expectSnapshotDaoRecordOneSnapshot(newSyncKey, uidNext, syncCollectionOptions, actualEmailsInServer);
		
		WindowingKey windowingKey = new WindowingKey(udr.getUser(), udr.getDevId(), collectionId, syncKey);
		expect(windowingDao.hasPendingChanges(windowingKey)).andReturn(false);
		expect(windowingDao.hasPendingChanges(windowingKey.withSyncKey(newSyncKey))).andReturn(false);
		expectMailBackendSyncData(uidNext, syncCollectionOptions, null, previousEmailsInServer,
				actualEmailsInServer, emailChanges, fromDate, syncState);

		windowingDao.pushPendingChanges(windowingKey, emailChanges, PIMDataType.EMAIL);
		expectLastCall();
		expect(windowingDao.popNextChanges(eq(windowingKey), eq(windowSize), eq(newSyncKey), isA(EmailChanges.Builder.class))).andReturn(changesBuilder);
		expect(dateService.getCurrentDate()).andReturn(date("2004-12-14T22:00:00"));

		expectBuildItemChangesByFetchingMSEmailsData(syncCollectionOptions, emailChanges, itemChanges);
		
		control.replay();
		DataDelta actual = testee.getChanged(udr, syncState, syncCollectionRequest, newSyncKey);
		control.verify();
		
		assertThat(actual.getDeletions()).isEmpty();
		assertThat(actual.getChanges()).containsOnly(itemChange1, itemChange2);
	}

	@Test
	public void testInitialWhenNoChange() throws Exception {
		SyncKey syncKey = SyncKey.INITIAL_SYNC_KEY;
		SyncKey newSyncKey = new SyncKey("1234");
		long uidNext = 45612;
		int windowSize = 10;
		
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Set<Email> previousEmailsInServer = ImmutableSet.of();
		Set<Email> actualEmailsInServer = ImmutableSet.of();
		EmailChanges emailChanges = EmailChanges.builder().build();

		MSEmailChanges itemChanges = MSEmailChanges.builder().build();
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build();

		AnalysedSyncCollection syncCollectionRequest = AnalysedSyncCollection.builder()
				.dataType(PIMDataType.EMAIL)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.windowSize(windowSize)
				.options(syncCollectionOptions)
				.build();
		
		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		WindowingKey windowingKey = new WindowingKey(udr.getUser(), udr.getDevId(), collectionId, syncKey);
		expect(windowingDao.hasPendingChanges(windowingKey)).andReturn(false);
		expect(windowingDao.hasPendingChanges(windowingKey.withSyncKey(newSyncKey))).andReturn(false);
		expectSnapshotDaoRecordOneSnapshot(newSyncKey, uidNext, syncCollectionOptions, actualEmailsInServer);
		expectMailBackendSyncData(uidNext, syncCollectionOptions, null, previousEmailsInServer, actualEmailsInServer, emailChanges, fromDate, syncState);
		expectBuildItemChangesByFetchingMSEmailsData(syncCollectionOptions, emailChanges, itemChanges);
		
		EmailChanges.Builder changesBuilder = control.createMock(EmailChanges.Builder.class);
		expect(changesBuilder.build()).andReturn(emailChanges);
		expect(windowingDao.popNextChanges(eq(windowingKey), eq(windowSize), eq(newSyncKey), isA(EmailChanges.Builder.class))).andReturn(changesBuilder);
		expect(dateService.getCurrentDate()).andReturn(date("2004-12-14T22:00:00"));
		
		control.replay();
		DataDelta actual = testee.getChanged(udr, syncState, syncCollectionRequest, newSyncKey);
		control.verify();

		assertThat(actual.getDeletions()).isEmpty();
		assertThat(actual.getChanges()).isEmpty();
	}
	
	@Test
	public void testNotInitial() throws Exception {
		int windowSize = 10;
		SyncKey syncKey = new SyncKey("1234");
		SyncKey newSyncKey = new SyncKey("5678");
		ImmutableList<BodyPreference> bodyPreferences = ImmutableList.<BodyPreference>of();
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).bodyPreferences(bodyPreferences).build();

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		
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

		Set<Email> fetchedEmails = ImmutableSet.of(modifiedEmail, newEmail);
		Set<Email> previousEmailsInServer = ImmutableSet.of(snapedEmail, deletedEmail);
		
		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();

		Snapshot existingSnapshot = Snapshot.builder()
			.emails(previousEmailsInServer)
			.filterType(syncCollectionOptions.getFilterType())
			.uidNext(previousUIDNext)
			.build();
		expectSnapshotDaoRecordOneSnapshot(newSyncKey, currentUIDNext, syncCollectionOptions, fetchedEmails);
		
		EmailChanges emailChanges = EmailChanges.builder()
				.changes(ImmutableSet.<Email> of(modifiedEmail))
				.additions(ImmutableSet.<Email> of(newEmail))
				.deletions(ImmutableSet.<Email> of(deletedEmail))
				.build();
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build();

		AnalysedSyncCollection syncCollectionRequest = AnalysedSyncCollection.builder()
				.dataType(PIMDataType.EMAIL)
				.syncKey(syncKey)
				.collectionId(collectionId)
				.windowSize(windowSize)
				.options(syncCollectionOptions)
				.build();
		
		WindowingKey windowingKey = new WindowingKey(udr.getUser(), udr.getDevId(), collectionId, syncKey);
		expect(windowingDao.hasPendingChanges(windowingKey)).andReturn(false);
		expect(windowingDao.hasPendingChanges(windowingKey.withSyncKey(newSyncKey))).andReturn(false);
		expectMailBackendSyncData(currentUIDNext, syncCollectionOptions, existingSnapshot, previousEmailsInServer, fetchedEmails, emailChanges, fromDate, syncState);
		
		windowingDao.pushPendingChanges(windowingKey, emailChanges, PIMDataType.EMAIL);
		expectLastCall();
		EmailChanges.Builder changesBuilder = control.createMock(EmailChanges.Builder.class);
		expect(changesBuilder.build()).andReturn(emailChanges);
		expect(windowingDao.popNextChanges(eq(windowingKey), eq(windowSize), eq(newSyncKey), isA(EmailChanges.Builder.class))).andReturn(changesBuilder);
		expect(dateService.getCurrentDate()).andReturn(date("2004-12-14T22:00:00"));

		expectServerItemChanges(syncCollectionOptions, emailChanges, modifiedEmail, newEmail, deletedEmail);
		
		control.replay();
		testee.getChanged(udr, syncState, syncCollectionRequest, newSyncKey);
		
		control.verify();
	}

	private void expectServerItemChanges(SyncCollectionOptions options, EmailChanges emailChanges, Email modifiedEmail, Email newEmail, Email deletedEmail)
			throws EmailViewPartsFetcherException, DaoException {
		
		ImmutableList<ItemChange> itemChanges = itemChanges(modifiedEmail, newEmail);
		ImmutableList<ItemDeletion> itemDeletions = itemDeletions(deletedEmail);
		expect(serverEmailChangesBuilder.fetch(udr, collectionId, collectionPath, options, emailChanges))
			.andReturn(MSEmailChanges.builder()
					.changes(itemChanges)
					.deletions(itemDeletions)
					.build()).once();
	}

	private ImmutableList<ItemChange> itemChanges(Email modifiedEmail, Email newEmail) {
		ItemChange changeItemChange = ItemChange.builder()
			.serverId(collectionId.serverId(Ints.checkedCast(modifiedEmail.getUid())))
			.build();
		ItemChange newItemChange = ItemChange.builder()
			.serverId(collectionId.serverId(Ints.checkedCast(newEmail.getUid())))
			.build();
		ImmutableList<ItemChange> itemChanges = ImmutableList.<ItemChange> of(changeItemChange, newItemChange);
		return itemChanges;
	}

	private ImmutableList<ItemDeletion> itemDeletions(Email deletedEmail) {
		ItemDeletion deletedItemDeletion = ItemDeletion.builder()
				.serverId(collectionId.serverId(Ints.checkedCast(deletedEmail.getUid())))
				.build();
		ImmutableList<ItemDeletion> itemDeletions = ImmutableList.<ItemDeletion> of(deletedItemDeletion);
		return itemDeletions;
	}
	
	@Test
	public void testGetItemEstimateInitialWhenNoChange() throws Exception {
		SyncKey syncKey = SyncKey.INITIAL_SYNC_KEY;
		long uidNext = 45612;
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Set<Email> previousEmailsInServer = ImmutableSet.of();
		Set<Email> actualEmailsInServer = ImmutableSet.of();
		EmailChanges emailChanges = EmailChanges.builder().build();

		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build();

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expectMailBackendSyncData(uidNext, syncCollectionOptions, null, previousEmailsInServer, actualEmailsInServer, emailChanges, fromDate, syncState);
		
		control.replay();
		int itemEstimateSize = testee.getItemEstimateSize(udr, syncState, collectionId, syncCollectionOptions);
		control.verify();
		
		assertThat(itemEstimateSize).isEqualTo(0);
	}

	@Test
	public void testGetItemEstimateInitialWhithChanges() throws Exception {
		SyncKey syncKey = SyncKey.INITIAL_SYNC_KEY;
		long uidNext = 45612;
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();

		Date fromDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build();
		
		Email email1 = Email.builder().uid(245).read(false).date(date("2004-12-14T22:00:00")).build();
		Email email2 = Email.builder().uid(546).read(true).date(date("2012-12-12T23:59:00")).build();
		
		Set<Email> previousEmailsInServer = ImmutableSet.of();
		Set<Email> actualEmailsInServer = ImmutableSet.of(email1, email2);
		EmailChanges emailChanges = EmailChanges.builder().additions(actualEmailsInServer).build();

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expectMailBackendSyncData(uidNext, syncCollectionOptions, null, previousEmailsInServer, actualEmailsInServer, emailChanges, fromDate, syncState);
		
		control.replay();
		int itemEstimateSize = testee.getItemEstimateSize(udr, syncState, collectionId, syncCollectionOptions);
		control.verify();
		
		assertThat(itemEstimateSize).isEqualTo(2);
	}

	@Test
	public void testGetItemEstimateNoChange() throws Exception {
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
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build();

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expectMailBackendSyncData(uidNext, syncCollectionOptions, snapshot, emailsInServer, emailsInServer, emailChanges, fromDate, syncState);
		
		control.replay();
		int itemEstimateSize = testee.getItemEstimateSize(udr, syncState, collectionId, syncCollectionOptions);
		control.verify();
		
		assertThat(itemEstimateSize).isEqualTo(0);
	}

	@Test
	public void testGetItemEstimateWithChanges() throws Exception {
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
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(syncKey)
				.build();

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expectMailBackendSyncData(uidNext, syncCollectionOptions, snapshot, previousEmailsInServer, actualEmailsInServer, emailChanges, fromDate, syncState);
		
		control.replay();
		int itemEstimateSize = testee.getItemEstimateSize(udr, syncState, collectionId, syncCollectionOptions);
		control.verify();
		
		assertThat(itemEstimateSize).isEqualTo(3);
	}
	
	@Test
	public void testGetChangedNoPendingResponseFittingWindowSize() throws Exception {
		long uidNext = 45612;
		int windowSize = 10;
		SyncKey previousSyncKey = new SyncKey("132");
		SyncKey newSyncKey = new SyncKey("546");
		ItemSyncState syncState = ItemSyncState.builder().syncDate(date("2004-12-14T22:00:00")).syncKey(previousSyncKey).build();
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();
		AnalysedSyncCollection syncCollectionRequest = AnalysedSyncCollection.builder()
				.dataType(PIMDataType.EMAIL)
				.syncKey(previousSyncKey)
				.collectionId(collectionId)
				.windowSize(windowSize)
				.options(syncCollectionOptions)
				.build();
		
		Email email1 = Email.builder().uid(245).read(false).date(date("2004-12-14T22:00:00")).build();
		Email email2 = Email.builder().uid(546).read(true).date(date("2012-12-12T23:59:00")).build();
		Set<Email> previousEmails = ImmutableSet.of();
		Set<Email> actualEmails = ImmutableSet.of(email1, email2);
		EmailChanges allChanges = EmailChanges.builder().additions(actualEmails).build();
		EmailChanges fittingChanges = allChanges;

		Date syncDataDate = syncCollectionOptions.getFilterType().getFilteredDateTodayAtMidnight();
		WindowingKey windowingKey = new WindowingKey(udr.getUser(), udr.getDevId(), collectionId, previousSyncKey);
		expect(windowingDao.hasPendingChanges(windowingKey)).andReturn(false);
		expect(windowingDao.hasPendingChanges(windowingKey.withSyncKey(newSyncKey))).andReturn(false);
		expect(dateService.getCurrentDate()).andReturn(syncDataDate);
		
		Snapshot previousSnapshot = Snapshot.builder()
				.emails(previousEmails)
				.filterType(FilterType.ALL_ITEMS)
				.uidNext(uidNext)
				.build();
		
		expectMailBackendSyncData(uidNext, syncCollectionOptions, previousSnapshot, previousEmails, actualEmails, allChanges, syncDataDate, syncState);
		expectSnapshotDaoRecordOneSnapshot(newSyncKey, uidNext, syncCollectionOptions, actualEmails);
		
		windowingDao.pushPendingChanges(windowingKey, allChanges, PIMDataType.EMAIL);
		expectLastCall();
		EmailChanges.Builder changesBuilder = control.createMock(EmailChanges.Builder.class);
		expect(changesBuilder.build()).andReturn(fittingChanges);
		expect(windowingDao.popNextChanges(eq(windowingKey), eq(windowSize), eq(newSyncKey), isA(EmailChanges.Builder.class))).andReturn(changesBuilder);
		
		MSEmail itemChangeData1 = control.createMock(MSEmail.class);
		MSEmail itemChangeData2 = control.createMock(MSEmail.class);
		ItemChange itemChange1 = ItemChange.builder().serverId(collectionId.serverId(245)).data(itemChangeData1).build();
		ItemChange itemChange2 = ItemChange.builder().serverId(collectionId.serverId(546)).data(itemChangeData2).build();
		MSEmailChanges itemChanges = MSEmailChanges.builder()
				.changes(ImmutableList.of(itemChange1, itemChange2))
				.build();
		
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expectBuildItemChangesByFetchingMSEmailsData(syncCollectionOptions, fittingChanges, itemChanges);
		
		control.replay();
		DataDelta windowedChanges = testee.getChanged(udr, syncState, syncCollectionRequest, newSyncKey);
		control.verify();
		
		assertThat(windowedChanges).isEqualTo(DataDelta.builder()
				.changes(ImmutableList.of(itemChange1, itemChange2))
				.deletions(ImmutableList.<ItemDeletion>of())
				.syncDate(syncDataDate)
				.syncKey(newSyncKey)
				.moreAvailable(false)
				.build());
	}
	
	@Test
	public void testGetChangedNoPendingResponseNotFittingWindowSize() throws Exception {
		long uidNext = 45612;
		int windowSize = 1;
		SyncKey previousSyncKey = new SyncKey("132");
		SyncKey newSyncKey = new SyncKey("546");
		Date syncDataDate = date("2004-12-14T22:00:00");
		
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expect(dateService.getCurrentDate()).andReturn(syncDataDate);
		
		ItemSyncState syncState = ItemSyncState.builder().syncDate(syncDataDate).syncKey(previousSyncKey).build();
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();
		AnalysedSyncCollection syncCollectionRequest = AnalysedSyncCollection.builder()
				.dataType(PIMDataType.EMAIL)
				.syncKey(previousSyncKey)
				.collectionId(collectionId)
				.windowSize(windowSize)
				.options(syncCollectionOptions)
				.build();
		
		Email email1 = Email.builder().uid(245).read(false).date(syncDataDate).build();
		Email email2 = Email.builder().uid(546).read(true).date(date("2012-12-12T23:59:00")).build();
		Set<Email> previousEmails = ImmutableSet.of();
		Set<Email> actualEmails = ImmutableSet.of(email1, email2);
		EmailChanges allChanges = EmailChanges.builder().additions(actualEmails).build();
		
		EmailChanges.Builder changesBuilder = control.createMock(EmailChanges.Builder.class);
		EmailChanges fittingChanges = control.createMock(EmailChanges.class);
		expect(changesBuilder.build()).andReturn(fittingChanges);
	
		WindowingKey windowingKey = new WindowingKey(udr.getUser(), udr.getDevId(), collectionId, previousSyncKey);
		expect(windowingDao.hasPendingChanges(windowingKey)).andReturn(false);
		expect(windowingDao.hasPendingChanges(windowingKey.withSyncKey(newSyncKey))).andReturn(true);
		
		Snapshot previousSnapshot = Snapshot.builder()
				.emails(previousEmails)
				.filterType(FilterType.ALL_ITEMS)
				.uidNext(uidNext)
				.build();
		
		expectMailBackendSyncData(uidNext, syncCollectionOptions, previousSnapshot, previousEmails, actualEmails, allChanges, syncDataDate, syncState);
		expectSnapshotDaoRecordOneSnapshot(newSyncKey, uidNext, syncCollectionOptions, actualEmails);
		windowingDao.pushPendingChanges(windowingKey, allChanges, PIMDataType.EMAIL);
		expectLastCall();
		expect(windowingDao.popNextChanges(eq(windowingKey), eq(windowSize), eq(newSyncKey), isA(EmailChanges.Builder.class))).andReturn(changesBuilder);
		
		MSEmail itemChangeData1 = control.createMock(MSEmail.class);
		ItemChange itemChange1 = ItemChange.builder().serverId(collectionId.serverId(245)).data(itemChangeData1).build();
		MSEmailChanges itemChanges = MSEmailChanges.builder()
				.changes(ImmutableList.of(itemChange1))
				.build();
		expectBuildItemChangesByFetchingMSEmailsData(syncCollectionOptions, fittingChanges, itemChanges);
		
		control.replay();
		DataDelta windowedChanges = testee.getChanged(udr, syncState, syncCollectionRequest, newSyncKey);
		control.verify();
		

		assertThat(windowedChanges).isEqualTo(DataDelta.builder()
				.changes(ImmutableList.of(itemChange1))
				.deletions(ImmutableList.<ItemDeletion>of())
				.syncDate(syncDataDate)
				.syncKey(newSyncKey)
				.moreAvailable(true)
				.build());
	}
	
	@Test
	public void testGetChangedPendingResponseFittingWindowSize() throws Exception {
		int windowSize = 10;
		SyncKey previousSyncKey = new SyncKey("132");
		SyncKey newSyncKey = new SyncKey("546");
		ItemSyncState syncState = ItemSyncState.builder().syncDate(date("2004-12-14T22:00:00")).syncKey(previousSyncKey).build();
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();
		AnalysedSyncCollection syncCollectionRequest = AnalysedSyncCollection.builder()
				.dataType(PIMDataType.EMAIL)
				.syncKey(previousSyncKey)
				.collectionId(collectionId)
				.windowSize(windowSize)
				.options(syncCollectionOptions)
				.build();
		
		EmailChanges.Builder changesBuilder = control.createMock(EmailChanges.Builder.class);
		EmailChanges fittingChanges = control.createMock(EmailChanges.class);
		expect(changesBuilder.build()).andReturn(fittingChanges);

		WindowingKey windowingKey = new WindowingKey(udr.getUser(), udr.getDevId(), collectionId, previousSyncKey);
		expect(windowingDao.hasPendingChanges(windowingKey)).andReturn(true);
		SnapshotKey snapshotKey = SnapshotKey.builder().deviceId(devId).syncKey(previousSyncKey).collectionId(collectionId).build();
		snapshotDao.linkSyncKeyToSnapshot(newSyncKey, snapshotKey);
		expectLastCall();
		expect(windowingDao.popNextChanges(eq(windowingKey), eq(windowSize), eq(newSyncKey), isA(EmailChanges.Builder.class))).andReturn(changesBuilder);
		expect(windowingDao.hasPendingChanges(windowingKey.withSyncKey(newSyncKey))).andReturn(false);
		expect(dateService.getCurrentDate()).andReturn(date("2004-12-14T22:00:00"));

		MSEmail itemChangeData1 = control.createMock(MSEmail.class);
		MSEmail itemChangeData2 = control.createMock(MSEmail.class);
		ItemChange itemChange1 = ItemChange.builder().serverId(collectionId.serverId(245)).data(itemChangeData1).build();
		ItemChange itemChange2 = ItemChange.builder().serverId(collectionId.serverId(546)).data(itemChangeData2).build();
		MSEmailChanges itemChanges = MSEmailChanges.builder()
				.changes(ImmutableList.of(itemChange1, itemChange2))
				.build();
		expectBuildItemChangesByFetchingMSEmailsData(syncCollectionOptions, fittingChanges, itemChanges);
		
		control.replay();
		DataDelta windowedChanges = testee.getChanged(udr, syncState, syncCollectionRequest, newSyncKey);
		control.verify();
		
		assertThat(windowedChanges).isEqualTo(DataDelta.builder()
				.changes(ImmutableList.of(itemChange1, itemChange2))
				.deletions(ImmutableList.<ItemDeletion>of())
				.syncDate(syncState.getSyncDate())
				.syncKey(newSyncKey)
				.moreAvailable(false)
				.build());
	}
	
	@Test
	public void testGetChangedPendingResponseNotFittingWindowSize() throws Exception {
		int windowSize = 1;
		SyncKey previousSyncKey = new SyncKey("132");
		SyncKey newSyncKey = new SyncKey("546");
		Date syncDataDate = date("2004-12-14T22:00:00");
		
		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expect(dateService.getCurrentDate()).andReturn(syncDataDate);
		
		ItemSyncState syncState = ItemSyncState.builder().syncDate(syncDataDate).syncKey(previousSyncKey).build();
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();
		AnalysedSyncCollection syncCollectionRequest = AnalysedSyncCollection.builder()
				.dataType(PIMDataType.EMAIL)
				.syncKey(previousSyncKey)
				.collectionId(collectionId)
				.windowSize(windowSize)
				.options(syncCollectionOptions)
				.build();
		
		EmailChanges.Builder changesBuilder = control.createMock(EmailChanges.Builder.class);
		EmailChanges fittingChanges = control.createMock(EmailChanges.class);
		expect(changesBuilder.build()).andReturn(fittingChanges);

		WindowingKey windowingKey = new WindowingKey(udr.getUser(), udr.getDevId(), collectionId, previousSyncKey);
		expect(windowingDao.hasPendingChanges(windowingKey)).andReturn(true);
		SnapshotKey snapshotKey = SnapshotKey.builder().deviceId(devId).syncKey(previousSyncKey).collectionId(collectionId).build();
		snapshotDao.linkSyncKeyToSnapshot(newSyncKey, snapshotKey);
		expectLastCall();
		
		expect(windowingDao.popNextChanges(eq(windowingKey), eq(windowSize), eq(newSyncKey), isA(EmailChanges.Builder.class))).andReturn(changesBuilder);
		expect(windowingDao.hasPendingChanges(windowingKey.withSyncKey(newSyncKey))).andReturn(true);
		
		MSEmail itemChangeData1 = control.createMock(MSEmail.class);
		ItemChange itemChange1 = ItemChange.builder().serverId(collectionId.serverId(245)).data(itemChangeData1).build();
		MSEmailChanges itemChanges = MSEmailChanges.builder()
				.changes(ImmutableList.of(itemChange1))
				.build();
		expectBuildItemChangesByFetchingMSEmailsData(syncCollectionOptions, fittingChanges, itemChanges);
		
		control.replay();
		DataDelta windowedChanges = testee.getChanged(udr, syncState, syncCollectionRequest, newSyncKey);
		control.verify();

		assertThat(windowedChanges).isEqualTo(DataDelta.builder()
				.changes(ImmutableList.of(itemChange1))
				.deletions(ImmutableList.<ItemDeletion>of())
				.syncDate(syncState.getSyncDate())
				.syncKey(newSyncKey)
				.moreAvailable(true)
				.build());
	}
	
	@Test(expected=ProcessingEmailException.class)
	public void testGetChangedFetchingTriggersException() throws Exception {
		int windowSize = 1;
		SyncKey previousSyncKey = new SyncKey("132");
		SyncKey newSyncKey = new SyncKey("546");
		ItemSyncState syncState = ItemSyncState.builder().syncDate(date("2004-12-14T22:00:00")).syncKey(previousSyncKey).build();

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().filterType(FilterType.ALL_ITEMS).build();
		AnalysedSyncCollection syncCollectionRequest = AnalysedSyncCollection.builder()
				.dataType(PIMDataType.EMAIL)
				.syncKey(previousSyncKey)
				.collectionId(collectionId)
				.windowSize(windowSize)
				.options(syncCollectionOptions)
				.build();
		
		EmailChanges.Builder changesBuilder = control.createMock(EmailChanges.Builder.class);
		EmailChanges fittingChanges = control.createMock(EmailChanges.class);
		expect(changesBuilder.build()).andReturn(fittingChanges);

		WindowingKey windowingKey = new WindowingKey(udr.getUser(), udr.getDevId(), collectionId, previousSyncKey);
		expect(windowingDao.hasPendingChanges(windowingKey)).andReturn(true);
		SnapshotKey snapshotKey = SnapshotKey.builder().deviceId(devId).syncKey(previousSyncKey).collectionId(collectionId).build();
		snapshotDao.linkSyncKeyToSnapshot(newSyncKey, snapshotKey);
		expectLastCall();
		expect(windowingDao.popNextChanges(eq(windowingKey), eq(windowSize), eq(newSyncKey), isA(EmailChanges.Builder.class))).andReturn(changesBuilder);
		expect(serverEmailChangesBuilder.fetch(udr, collectionId, collectionPath, syncCollectionOptions, fittingChanges))
			.andThrow(new EmailViewPartsFetcherException("error"));
		
		control.replay();
		try {
			testee.getChanged(udr, syncState, syncCollectionRequest, newSyncKey);
		} catch (ProcessingEmailException e) {
			control.verify();
			throw e;
		}
	}
	
	@Test(expected=InvalidSyncKeyException.class)
	public void testFetchWhenNoSnapshotLinkedToSyncKey() {
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().build();
		List<ServerId> itemIds = ImmutableList.of(CollectionId.of(1).serverId(1));

		SyncKey previousSyncKey = new SyncKey("123");
		SyncKey nextSyncKey = new SyncKey("456");
		ItemSyncState previousItemSyncState = ItemSyncState.builder()
				.syncKey(previousSyncKey)
				.syncDate(date("2004-12-14T22:00:00"))
				.build();

		SnapshotKey snapshotKey = SnapshotKey.builder()
				.deviceId(devId)
				.syncKey(previousItemSyncState.getSyncKey())
				.collectionId(collectionId)
				.build();
		
		expect(snapshotDao.get(snapshotKey)).andReturn(null);
		snapshotDao.linkSyncKeyToSnapshot(nextSyncKey, snapshotKey);
		expectLastCall();
		control.replay();
		
		try {
			testee.fetch(udr, collectionId, itemIds, syncCollectionOptions, previousItemSyncState, nextSyncKey);
		} catch (InvalidSyncKeyException e) {
			control.verify();
			throw e;
		}
	}
	
	private void expectBuildItemChangesByFetchingMSEmailsData(SyncCollectionOptions options,
			WindowingChanges<Email> emailChanges, MSEmailChanges itemChanges)
					throws EmailViewPartsFetcherException, DaoException {
		
		expect(serverEmailChangesBuilder.fetch(udr, collectionId, collectionPath, options, emailChanges))
			.andReturn(itemChanges);
	}

	private void expectMailBackendSyncData(long uidNext,
			SyncCollectionOptions syncCollectionOptions,
			Snapshot snapshot,
			Set<Email> previousEmailsInServer, Set<Email> actualEmailsInServer,
			EmailChanges emailChanges, Date fromDate, ItemSyncState syncState)
			throws Exception {
		MailBackendSyncData syncData = new MailBackendSyncData(fromDate, collectionPath, uidNext, snapshot, previousEmailsInServer, actualEmailsInServer, emailChanges);
		expect(mailBackendSyncDataFactory.create(udr, syncState, folder, syncCollectionOptions))
			.andReturn(syncData).once();
	}
	
	private void expectSnapshotDaoRecordOneSnapshot(SyncKey syncKey, long uidNext,
			SyncCollectionOptions syncCollectionOptions, Collection<Email> actualEmailsInServer) {
		
		snapshotDao.put(
			SnapshotKey.builder()
				.deviceId(device.getDevId())
				.syncKey(syncKey)
				.collectionId(collectionId)
				.build(),
			Snapshot.builder()
				.emails(actualEmailsInServer)
				.filterType(syncCollectionOptions.getFilterType())
				.uidNext(uidNext)
				.build());
		expectLastCall();
	}

	@Test
	public void testGetInvitation() throws Exception {
		int itemId = 1;
		
		ICalendar expectedCalendar = control.createMock(ICalendar.class);

		expect(folderSnapshotDao.get(user, device, collectionId)).andReturn(folder);
		expect(msEmailFetcher.fetchInvitation(udr, collectionId, collectionPath, Long.valueOf(itemId)))
			.andReturn(expectedCalendar);
		
		control.replay();
		
		ICalendar ics = testee.getInvitation(udr, collectionId, collectionId.serverId(itemId));
		
		control.verify();
		assertThat(ics).isEqualTo(expectedCalendar);
	}
	
	@Test
	public void testSendEmailWithBigInputStreamNoSaveInSent() throws Exception {
		boolean saveInSent = false;

		Set<Address> addrs = Sets.newHashSet();
		smtpSender.sendEmail(anyObject(UserDataRequest.class), anyObject(Address.class),
				anyObject(addrs.getClass()),
				anyObject(addrs.getClass()),
				anyObject(addrs.getClass()), anyObject(InputStream.class));
		expectLastCall().once();
		
		SendEmail sendEmail = control.createMock(SendEmail.class);
		expect(sendEmail.getFrom()).andReturn(new InternetAddress("test@test.fr"));
		expect(sendEmail.getTo()).andReturn(addrs);
		expect(sendEmail.getCc()).andReturn(addrs);
		expect(sendEmail.getCci()).andReturn(addrs);
		expect(sendEmail.getMessage()).andReturn(loadEmail("bigEml.eml"));
		
		control.replay();
		testee.sendEmail(udr, sendEmail, saveInSent);
		control.verify();
	}
	
	@Test
	public void testSendEmailWithBigInputStreamSaveInSent() throws Exception {
		boolean saveInSent = true;
		
		Set<Address> addrs = Sets.newHashSet();
		smtpSender.sendEmail(anyObject(UserDataRequest.class), anyObject(Address.class),
				anyObject(addrs.getClass()),
				anyObject(addrs.getClass()),
				anyObject(addrs.getClass()), anyObject(InputStream.class));
		expectLastCall().once();
		
		Message message = control.createMock(Message.class);
		expect(message.getCharset()).andReturn("UTF-8");
		SendEmail sendEmail = control.createMock(SendEmail.class);
		expect(sendEmail.getFrom()).andReturn(new InternetAddress("test@test.fr"));
		expect(sendEmail.getTo()).andReturn(addrs);
		expect(sendEmail.getCc()).andReturn(addrs);
		expect(sendEmail.getCci()).andReturn(addrs);
		expect(sendEmail.getMimeMessage()).andReturn(message);
		expect(sendEmail.getMessage()).andReturn(loadEmail("bigEml.eml"));
		
		mailboxService.storeInSent(eq(udr), isA(EmailReader.class));
		expectLastCall();
		
		control.replay();
		testee.sendEmail(udr, sendEmail, saveInSent);
		control.verify();
	}
	
	@Test
	public void sendEmailShouldNotTriggerExceptionWhenSaveInSentAndUnknownCharset() throws Exception {
		boolean saveInSent = true;
		
		Set<Address> addrs = Sets.newHashSet();
		smtpSender.sendEmail(anyObject(UserDataRequest.class), anyObject(Address.class),
				anyObject(addrs.getClass()),
				anyObject(addrs.getClass()),
				anyObject(addrs.getClass()), anyObject(InputStream.class));
		expectLastCall().once();
		
		Message message = control.createMock(Message.class);
		expect(message.getCharset()).andReturn("I'm not a charset!");
		SendEmail sendEmail = control.createMock(SendEmail.class);
		expect(sendEmail.getFrom()).andReturn(new InternetAddress("test@test.fr"));
		expect(sendEmail.getTo()).andReturn(addrs);
		expect(sendEmail.getCc()).andReturn(addrs);
		expect(sendEmail.getCci()).andReturn(addrs);
		expect(sendEmail.getMimeMessage()).andReturn(message);
		expect(sendEmail.getMessage()).andReturn(loadEmail("bigEml.eml"));
		
		control.replay();
		testee.sendEmail(udr, sendEmail, saveInSent);
		control.verify();
	}
	
	@Test
	public void sendEmailShouldGiveEmailAddressOnlyEvenWhenDisplayNameAvailable() throws Exception {
		boolean saveInSent = true;
		String mailAddress = "test@test.fr";
		
		Set<Address> addrs = Sets.newHashSet();
		smtpSender.sendEmail(anyObject(UserDataRequest.class), eq(new Address(mailAddress)),
				anyObject(addrs.getClass()),
				anyObject(addrs.getClass()),
				anyObject(addrs.getClass()), anyObject(InputStream.class));
		expectLastCall().once();
		
		Message message = control.createMock(Message.class);
		expect(message.getCharset()).andReturn("I'm not a charset!");
		SendEmail sendEmail = control.createMock(SendEmail.class);
		expect(sendEmail.getFrom()).andReturn(new InternetAddress(mailAddress, "display name"));
		expect(sendEmail.getTo()).andReturn(addrs);
		expect(sendEmail.getCc()).andReturn(addrs);
		expect(sendEmail.getCci()).andReturn(addrs);
		expect(sendEmail.getMimeMessage()).andReturn(message);
		expect(sendEmail.getMessage()).andReturn(loadEmail("bigEml.eml"));
		
		control.replay();
		testee.sendEmail(udr, sendEmail, saveInSent);
		control.verify();
	}

	@Test
	public void currentFoldersShouldReturnOnlyDefaultCalendar() {
		MailboxFolders folders = new MailboxFolders(ImmutableList.of(
			new MailboxFolder("custom/mail/box", '/'))
		);
		
		expect(mailboxService.listSubscribedFolders(udr)).andReturn(folders);
		
		control.replay();
		BackendFolders currentFolders = testee.getBackendFolders(udr);
		control.verify();
		
		assertThat(currentFolders).containsOnly(
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_INBOX_NAME))
				.displayName(IMAP_INBOX_NAME)
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_DRAFTS_NAME))
				.displayName(IMAP_DRAFTS_NAME)
				.folderType(FolderType.DEFAULT_DRAFTS_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_SENT_NAME))
				.displayName(IMAP_SENT_NAME)
				.folderType(FolderType.DEFAULT_SENT_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of(IMAP_TRASH_NAME))
				.displayName(IMAP_TRASH_NAME)
				.folderType(FolderType.DEFAULT_DELETED_ITEMS_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build(),
			BackendFolder.builder()
				.backendId(MailboxPath.of("custom/mail/box"))
				.displayName("custom/mail/box")
				.folderType(FolderType.USER_CREATED_EMAIL_FOLDER)
				.parentId(Optional.<BackendId>absent())
				.build()
		);
	}

	@Test
	public void getEmailUserTest() throws Exception {

		expect(authenticationService.getUserEmail(udr)).andReturn("test@test.fr");
		control.replay();
		InternetAddress internetAddresse = testee.getUserEmail(udr);
		control.verify();
		assertThat(internetAddresse.toString()).isEqualTo("\"user@domain\" <test@test.fr>");
	}

	@Test(expected=RuntimeException.class)
	public void getEmailUserWithoutEmailTest(){
		expect(authenticationService.getUserEmail(udr)).andReturn("");
		control.replay();
		InternetAddress internetAddresse = testee.getUserEmail(udr);
		control.verify();
		assertThat(internetAddresse.toString()).isEqualTo("\"user@domain\" <>");
	}

	@Test
	public void getEmailUserWithoutDisplayNameTest(){
		User userWithoutDisplayName = Factory.create().createUser("user@domain", "user@domain",null);
		UserDataRequest userDataRequest = new UserDataRequest(new Credentials(userWithoutDisplayName, "password".toCharArray()),  null, device);
		expect(authenticationService.getUserEmail(userDataRequest)).andReturn("user@domain");
		control.replay();
		InternetAddress internetAddresse = testee.getUserEmail(userDataRequest);
		control.verify();
		assertThat(internetAddresse.toString()).isEqualTo("user@domain");
	}
	
}
