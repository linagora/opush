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
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.obm.DateUtils.date;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSEmailBodyType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SnapshotKey;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.configuration.OpushEmailConfiguration;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.mail.bean.Address;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.EmailMetadata;
import org.obm.push.mail.bean.Envelope;
import org.obm.push.mail.bean.Flag;
import org.obm.push.mail.bean.FlagsList;
import org.obm.push.mail.bean.MessageSet;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.mail.bean.UIDEnvelope;
import org.obm.push.mail.mime.MimeAddress;
import org.obm.push.mail.mime.MimeMessage;
import org.obm.push.mail.mime.MimeMessageImpl;
import org.obm.push.mail.mime.MimePart;
import org.obm.push.mail.mime.MimePartImpl;
import org.obm.push.mail.transformer.Transformer;
import org.obm.push.mail.transformer.Transformer.TransformersFactory;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.DateService;
import org.obm.push.service.EventService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.SmtpSender;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.SnapshotDao;
import org.obm.push.store.WindowingDao;
import org.obm.push.utils.SerializableInputStream;
import org.obm.push.utils.UserEmailParserUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;


public class MailboxBackendTest {

	private String mailbox;
	private char[] password;
	private UserDataRequest udr;
	private Device device;
	private MailboxPath inboxPath;
	private Folder inboxFolder;
	
	private IMocksControl mocks;
	private MailBackendImpl mailBackendImpl;
	private MailboxService mailboxService;
	private MappingService mappingService;
	private MSEmailFetcher msEmailFetcher;
	private TransformersFactory transformersFactory;
	private MailViewToMSEmailConverter msEmailConverter;
	private Transformer transformer;
	private EventService eventService;
	private SnapshotDao snapshotDao;
	private WindowingDao windowingDao;
	private SmtpSender smtpSender;
	private OpushEmailConfiguration emailConfiguration;
	private DateService dateService;
	private FolderSnapshotDao folderSnapshotDao;
	private CollectionId inboxCollectionId;

	@Before
	public void setUp() {
		mailbox = "to@localhost.com";
		password = "password".toCharArray();
		device = new Device(1, "devType", new DeviceId("devId"), new Properties(), null);
		udr = new UserDataRequest(
				new Credentials(User.Factory.create()
						.createUser(mailbox, mailbox, null), password), null, device);
		inboxCollectionId = CollectionId.of(1);
		inboxPath = MailboxPath.of("INBOX");
		inboxFolder = Folder.builder()
				.backendId(inboxPath)
				.collectionId(inboxCollectionId)
				.parentBackendIdOpt(Optional.<BackendId>absent())
				.displayName("displayName")
				.folderType(FolderType.DEFAULT_INBOX_FOLDER)
				.build();
		
		mocks = createControl();
		mailboxService = mocks.createMock(MailboxService.class);
		mappingService = mocks.createMock(MappingService.class);
		UserEmailParserUtils emailParserUtils = new UserEmailParserUtils();
		MSEmailHeaderConverter msEmailHeaderConverter = new MSEmailHeaderConverter(emailParserUtils );
		eventService = mocks.createMock(EventService.class);
		msEmailConverter = new MailViewToMSEmailConverterImpl(msEmailHeaderConverter , eventService );
		transformersFactory = mocks.createMock(TransformersFactory.class);
		transformer = mocks.createMock(Transformer.class);
		expect(transformersFactory.create(anyObject(FetchInstruction.class))).andReturn(transformer).anyTimes();
		msEmailFetcher = new MSEmailFetcher(mailboxService, transformersFactory, msEmailConverter);
		snapshotDao = mocks.createMock(SnapshotDao.class);
		windowingDao = mocks.createMock(WindowingDao.class);
		smtpSender = mocks.createMock(SmtpSender.class);
		emailConfiguration = mocks.createMock(OpushEmailConfiguration.class);
		dateService = mocks.createMock(DateService.class);
		folderSnapshotDao = mocks.createMock(FolderSnapshotDao.class);
		
		mailBackendImpl = new MailBackendImpl(mailboxService, null, null, null, 
				snapshotDao, null, mappingService, msEmailFetcher, null, null, 
				windowingDao, smtpSender, emailConfiguration, dateService, folderSnapshotDao);
	}
	
	@Test
	public void fetchShouldQuietlyIgnoreMissingEmail() throws Exception {
		int itemId = 2;
		CollectionId collectionId = CollectionId.of(1);
		ServerId serverId = collectionId.serverId(itemId);
		ImmutableList<BodyPreference> bodyPreferences = ImmutableList.of(BodyPreference.builder().bodyType(MSEmailBodyType.MIME).build());
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().bodyPreferences(bodyPreferences).build();

		expect(mailboxService.fetchEmailMetadata(udr, inboxPath, itemId)).andThrow(new ItemNotFoundException("failure"));
		expect(folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId)).andReturn(inboxFolder);
		
		mocks.replay();
		List<ItemChange> items = mailBackendImpl.fetch(udr, collectionId, ImmutableList.of(serverId), syncCollectionOptions);
		mocks.verify();
		
		assertThat(items).isEmpty();
	}

	@Test
	public void testFetchMimeSinglePartBase64Email() throws Exception {
		int itemId = 2;
		ServerId serverId = inboxCollectionId.serverId(itemId);
		ImmutableList<BodyPreference> bodyPreferences = ImmutableList.of(BodyPreference.builder().bodyType(MSEmailBodyType.MIME).build());
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder().bodyPreferences(bodyPreferences).build();
		
		InputStream mailStream = loadEmail("SinglePartBase64.eml");
		expect(transformer.targetType()).andReturn(MSEmailBodyType.MIME);
		expect(transformer.transform(mailStream, Charsets.UTF_8)).andReturn(mailStream);
		
		mockMailboxServiceFetchFullMail(mailStream, itemId, inboxPath);
		expect(folderSnapshotDao.get(udr.getUser(), udr.getDevice(), inboxCollectionId)).andReturn(inboxFolder);
		expect(mappingService.getServerIdFor(inboxCollectionId, String.valueOf(itemId))).andReturn(serverId);
		
		SyncKey previousSyncKey = new SyncKey("123");
		SyncKey nextSyncKey = new SyncKey("456");
		ItemSyncState previousItemSyncState = ItemSyncState.builder()
				.id(1)
				.syncKey(previousSyncKey)
				.syncDate(date("2012-01-01T11:22:33"))
				.build();

		SnapshotKey existingSnapshotKey = SnapshotKey.builder()
				.collectionId(inboxCollectionId)
				.deviceId(device.getDevId())
				.syncKey(previousSyncKey).build();
		Snapshot existingSnapshot = Snapshot.builder()
			.uidNext(15l)
			.addEmail(Email.builder().uid(itemId).build())
			.filterType(FilterType.ALL_ITEMS).build();
		snapshotDao.linkSyncKeyToSnapshot(nextSyncKey, existingSnapshotKey);
		expectLastCall();
		expect(snapshotDao.get(existingSnapshotKey)).andReturn(existingSnapshot);
		
		mocks.replay();		
		List<ItemChange> emails = mailBackendImpl.fetch(udr, inboxCollectionId, ImmutableList.of(serverId), syncCollectionOptions, previousItemSyncState, nextSyncKey);
		mocks.verify();
		
		MSEmail actual = (MSEmail) Iterables.getOnlyElement(emails).getData();
		assertThat(actual.getBody().getMimeData().get()).hasContentEqualTo(loadEmail("SinglePartBase64.eml"));
	}

	@Ignore("greenmail seems to unexpectedly decode base64 part on-the-fly")
	@Test
	public void testFetchTextPlainSinglePartBase64Email() throws Exception {
		int itemId = 2;
		ServerId serverId = inboxCollectionId.serverId(itemId);

		InputStream mailStream = loadEmail("SinglePartBase64.eml");
		expect(transformer.targetType()).andReturn(MSEmailBodyType.PlainText);
		expect(transformer.transform(mailStream, Charsets.UTF_8)).andReturn(mailStream);
		
		mockMailboxServiceFetchFullMail(mailStream, itemId, inboxPath);
		expect(folderSnapshotDao.get(udr.getUser(), udr.getDevice(), inboxCollectionId)).andReturn(inboxFolder);
		expect(mappingService.getServerIdFor(inboxCollectionId, String.valueOf(itemId))).andReturn(serverId);
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder()
				.bodyPreferences(ImmutableList.of(BodyPreference.builder().bodyType(MSEmailBodyType.PlainText).build()))
				.build();
		
		SyncKey previousSyncKey = new SyncKey("123");
		SyncKey nextSyncKey = new SyncKey("456");
		ItemSyncState previousItemSyncState = ItemSyncState.builder()
				.id(1)
				.syncKey(previousSyncKey)
				.syncDate(date("2012-01-01T11:22:33"))
				.build();

		SnapshotKey existingSnapshotKey = SnapshotKey.builder()
				.collectionId(inboxCollectionId)
				.deviceId(device.getDevId())
				.syncKey(previousSyncKey).build();
		
		snapshotDao.linkSyncKeyToSnapshot(nextSyncKey, existingSnapshotKey);
		expectLastCall();

		mocks.replay();
		List<ItemChange> emails = mailBackendImpl.fetch(udr, inboxCollectionId, ImmutableList.of(serverId), syncCollectionOptions, previousItemSyncState, nextSyncKey);
		mocks.verify();
		
		MSEmail actual = (MSEmail) Iterables.getOnlyElement(emails).getData();
		String bodyText = new String(ByteStreams.toByteArray(actual.getBody().getMimeData().get()), Charsets.UTF_8);
		assertThat(bodyText).contains("Envoyé de mon iPhone");
	}
	
	private void mockMailboxServiceFetchFullMail(InputStream mailStream, long itemId, MailboxPath path) {
		mockMailboxServiceFetchEmailView(itemId, path);
		expectFetchMailStream(path, itemId, mailStream);
	}

	private void mockMailboxServiceFetchEmailView(long itemId, MailboxPath path) {
		MimeMessage mimeMessage = buildMimeMessage(itemId);
		expect(mailboxService.fetchEmailMetadata(udr, path, itemId)).andReturn(
				EmailMetadata.builder()
					.uid(mimeMessage.getUid())
					.size(mimeMessage.getSize())
					.flags(new FlagsList(ImmutableList.of(Flag.SEEN)))
					.envelope(buildEnvelope())
					.mimeMessage(mimeMessage)
					.build());
	}
	
	@Test
	public void testFetchWithoutCorrespondingBodyPreference() throws Exception {
		int itemId = 2;
		ServerId serverId = inboxCollectionId.serverId(itemId);

		final Capture<InputStream> capturedStream = newCapture();
		expect(transformer.targetType()).andReturn(MSEmailBodyType.MIME);
		expect(transformer.transform(capture(capturedStream), eq(Charsets.UTF_8)))
			.andAnswer(new IAnswer<InputStream>() {

				@Override
				public InputStream answer() throws Throwable {
					return capturedStream.getValue();
				}
			});

		mockMailboxServiceFetchFullMailWithMimePartAddress(loadEmail("OBMFULL-4123.eml"), itemId, inboxPath);
		expect(folderSnapshotDao.get(udr.getUser(), udr.getDevice(), inboxCollectionId)).andReturn(inboxFolder);
		expect(mappingService.getServerIdFor(inboxCollectionId, String.valueOf(itemId))).andReturn(serverId);
		
		SyncCollectionOptions syncCollectionOptions = SyncCollectionOptions.builder()
				.bodyPreferences(ImmutableList.of(BodyPreference.builder().bodyType(MSEmailBodyType.PlainText).build()))
				.build();
		
		SyncKey previousSyncKey = new SyncKey("123");
		SyncKey nextSyncKey = new SyncKey("456");
		ItemSyncState previousItemSyncState = ItemSyncState.builder()
				.id(1)
				.syncKey(previousSyncKey)
				.syncDate(date("2012-01-01T11:22:33"))
				.build();

		SnapshotKey existingSnapshotKey = SnapshotKey.builder()
				.collectionId(inboxCollectionId)
				.deviceId(device.getDevId())
				.syncKey(previousSyncKey).build();
		Snapshot existingSnapshot = Snapshot.builder()
				.uidNext(15l)
				.addEmail(Email.builder().uid(itemId).build())
				.filterType(FilterType.ALL_ITEMS).build();
		expect(snapshotDao.get(existingSnapshotKey)).andReturn(existingSnapshot);
		snapshotDao.linkSyncKeyToSnapshot(nextSyncKey, existingSnapshotKey);
		expectLastCall();
		
		mocks.replay();
		List<ItemChange> emails = mailBackendImpl.fetch(udr, inboxCollectionId, ImmutableList.of(serverId), syncCollectionOptions, previousItemSyncState, nextSyncKey);
		mocks.verify();
		
		MSEmail actual = (MSEmail) Iterables.getOnlyElement(emails).getData();
		SerializableInputStream emailStream = actual.getBody().getMimeData().get();
		assertThat(emailStream).hasContentEqualTo(loadEmail("OBMFULL-4123.eml"));
		emailStream.reset();
		assertThat(capturedStream.hasCaptured()).isTrue();
		assertThat(capturedStream.getValue()).hasContentEqualTo(loadEmail("OBMFULL-4123.eml"));
	}

	private void mockMailboxServiceFetchFullMailWithMimePartAddress(InputStream mailStream, int itemId, MailboxPath path) {
		mockMailboxServiceFetchEmailView(itemId, path);
		expectFetchMimePartStream(path, itemId, mailStream, new MimeAddress("1"));
	}
	
	private MimeMessage buildMimeMessage(long uid) {
		return MimeMessageImpl.builder()
				.uid(uid)
				.addChild(buildMimePart())
				.size(1)
				.build();
	}
	
	private MimePart buildMimePart() {
		return MimePartImpl.builder()
				.contentType("text/plain; charset= utf-8")
				.size(1)
				.build();
	}
	
	private Envelope buildEnvelope() {
		Address address = new Address(mailbox);
		return Envelope.builder()
				.to(ImmutableList.of(address))
				.cc(ImmutableList.of(address))
				.from(ImmutableList.of(address))
				.bcc(ImmutableList.of(address))
				.replyTo(ImmutableList.of(address))
				.build();
	}

	private InputStream loadEmail(String name) throws IOException {
		return new ByteArrayInputStream(ByteStreams.toByteArray(ClassLoader.getSystemResourceAsStream("eml/" + name)));
	}

	public void expectFetchFlags(MailboxPath path, long uid, FlagsList value) {
		expect(mailboxService.fetchFlags(udr, path, MessageSet.singleton(uid))).andReturn(ImmutableMap.of(uid, value));
	}

	public void expectFetchEnvelope(MailboxPath path, long uid, UIDEnvelope envelope) {
		expect(mailboxService.fetchEnvelope(udr, path, MessageSet.singleton(uid)))
			.andReturn(ImmutableList.of(envelope));
	}

	public void expectFetchBodyStructure(MailboxPath path, long uid, MimeMessage mimeMessage) {
		expect(mailboxService.fetchBodyStructure(udr, path, MessageSet.singleton(uid)))
			.andReturn(ImmutableList.of(mimeMessage));
	}

	public void expectFetchMailStream(MailboxPath path, long uid, InputStream mailStream) {
		expect(mailboxService.fetchMailStream(udr, path, uid, Optional.<Long> absent()))
				.andReturn(mailStream);
	}

	public void expectFetchMimePartStream(MailboxPath path, long uid, InputStream mailStream, MimeAddress partAddress) {
		expect(mailboxService.fetchMimePartStream(udr, path, uid, partAddress))
			.andReturn(mailStream);
	}
}
