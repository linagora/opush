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
package org.obm.opush.command.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.obm.DateUtils.date;

import java.util.Date;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Files;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.SyncKeyTestUtils;
import org.obm.opush.Users;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSContact;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncCollectionCommand;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.SyncCollection;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.EncoderFactory;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.service.DateService;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.ItemTrackingDao;
import org.obm.sync.base.EmailAddress;
import org.obm.sync.book.AddressBook;
import org.obm.sync.book.Contact;
import org.obm.sync.book.Phone;
import org.obm.sync.client.book.BookClient;
import org.obm.sync.items.ContactChanges;
import org.obm.sync.push.client.WBXMLOPClient;
import org.obm.sync.push.client.commands.Sync;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

@GuiceModule(SyncHandlerWithBackendTestModule.class)
@RunWith(GuiceRunner.class)
public class SyncHandlerOnContactsTest {

	@Inject private	Users users;
	@Inject private	OpushServer opushServer;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private SyncDecoder decoder;
	@Inject private EncoderFactory encoderFactory;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private SyncKeyTestUtils syncKeyTestUtils;
	@Inject private SyncTestUtils syncTestUtils;
	@Inject private ItemTrackingDao itemTrackingDao;
	@Inject private CollectionDao collectionDao;
	@Inject private BookClient bookClient;
	@Inject private DateService dateService;

	private OpushUser user;
	private String contactCollectionPath;
	private CollectionId contactCollectionId;

	private CloseableHttpClient httpClient;

	@Before
	public void init() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
		user = users.jaures;

		contactCollectionId = CollectionId.of(7891);
		contactCollectionPath = testUtils.buildContactCollectionPath(user, contactCollectionId);
		
		expect(collectionDao.getCollectionPath(contactCollectionId)).andReturn(contactCollectionPath).anyTimes();
		
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration");
	}

	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		httpClient.close();
		Files.delete(configuration.dataDir);
	}

	@Test
	public void newContactOnServerShouldBeSentToClient() throws Exception {
		SyncKey firstAllocatedSyncKey = new SyncKey("4a2c7db8-b532-40a0-92c3-bfebb8da8f00");
		SyncKey secondAllocatedSyncKey = new SyncKey("55df3cf4-b70d-4df2-ac48-d31646994321");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		
		Date syncDate = date("2012-10-09T16:22:53");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, contactCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		Contact initialContact = new Contact();
		initialContact.setUid(1);
		initialContact.setFirstname("firstname");
		initialContact.setLastname("lastname");
		initialContact.setEmails(ImmutableMap.of("INTERNET;X-OBM-Ref1", EmailAddress.loginAtDomain("contact@mydomain.org")));
		initialContact.setPhones(ImmutableMap.of("HOME;FAX;X-OBM-Ref1", new Phone("1234")));

		MSContact expectedMSContact = new MSContact();
		expectedMSContact.setFileAs("firstname lastname");
		expectedMSContact.setFirstName("firstname");
		expectedMSContact.setLastName("lastname");
		expectedMSContact.setEmail1Address("contact@mydomain.org");
		expectedMSContact.setHomeFaxNumber("1234");
		
		ServerId serverId = contactCollectionId.serverId(initialContact.getUid());
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId))
			.andReturn(false);
		
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(serverId));
		expectLastCall().once();

		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name("contacts")
					.uid(AddressBook.Id.valueOf(contactCollectionId.asInt()))
					.readOnly(false)
					.build()));
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + "contacts"))
			.andReturn(contactCollectionId);
		
		expect(bookClient.listContactsChanged(user.accessToken, syncDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(initialContact),
					ImmutableSet.<Integer> of(),
					syncDate));
		
		mocksControl.replay();
		opushServer.start();

		WBXMLOPClient opushClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opushClient.run(
				Sync.builder(decoder).syncKey(firstAllocatedSyncKey)
					.collection(SyncCollection.builder().collectionId(contactCollectionId).build()).build());
		
		mocksControl.verify();
		SyncCollectionCommand expectedCommandResponse = SyncCollectionCommand.builder()
				.type(SyncCommand.ADD)
				.serverId(serverId)
				.clientId(null)
				.applicationData(expectedMSContact)
				.build();
		
		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		SyncCollectionResponse syncCollectionResponse = syncTestUtils.getCollectionWithId(syncResponse, contactCollectionId);
		assertThat(syncCollectionResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		List<SyncCollectionCommand> commands = syncCollectionResponse.getCommands().getCommands();
		assertThat(commands).hasSize(1);
		SyncCollectionCommand SyncCollectionCommand = FluentIterable.from(commands).first().get();
		assertThat(SyncCollectionCommand).isEqualTo(expectedCommandResponse);
		
		MSContact msContact = (MSContact) SyncCollectionCommand.getApplicationData();
		assertThat(msContact.getFirstName()).isEqualTo("firstname");
		assertThat(msContact.getLastName()).isEqualTo("lastname");
		assertThat(msContact.getEmail1Address()).isEqualTo("contact@mydomain.org");
		assertThat(msContact.getHomeFaxNumber()).isEqualTo("1234");
	}

	@Test
	public void newContactOnClientShouldBePopulatedToServer() throws Exception {
		SyncKey firstAllocatedSyncKey = new SyncKey("4a2c7db8-b532-40a0-92c3-bfebb8da8f00");
		SyncKey secondAllocatedSyncKey = new SyncKey("55df3cf4-b70d-4df2-ac48-d31646994321");
		SyncKey thirdAllocatedSyncKey = new SyncKey("4872672c-95b9-4c1c-90ae-ebea7d0e83ed");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int thirdAllocatedStateId = 5;
		int uid = 456;
		ServerId serverId = contactCollectionId.serverId(uid);
		
		Date syncDate = date("2012-10-09T16:22:53");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState thirdAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(thirdAllocatedSyncKey)
				.id(thirdAllocatedStateId)
				.build();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey, thirdAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, contactCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, secondAllocatedSyncKey, secondAllocatedState, thirdAllocatedState, contactCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(thirdAllocatedState.getSyncDate()).once();
		// first sync
		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name("contacts")
					.uid(AddressBook.Id.valueOf(contactCollectionId.asInt()))
					.readOnly(false)
					.build()));
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + "contacts"))
			.andReturn(contactCollectionId);
		
		expect(bookClient.listContactsChanged(user.accessToken, syncDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(),
					ImmutableSet.<Integer> of(),
					syncDate));
		
		// second sync
		String clientId = "123";
		String hashedClientId = "e7cf79a18a015f6b4a97d26b1a07ca70ab6c703a";
		MSContact createdMSContact = new MSContact();
		createdMSContact.setFirstName("firstname");
		createdMSContact.setLastName("lastname");
		createdMSContact.setEmail1Address("contact@mydomain.org");
		createdMSContact.setFileAs("lastname, firstname");
		
		Contact convertedContact = new Contact();
		convertedContact.setFirstname("firstname");
		convertedContact.setLastname("lastname");
		convertedContact.setEmails(ImmutableMap.of("INTERNET;X-OBM-Ref1", EmailAddress.loginAtDomain("contact@mydomain.org")));
		
		Contact createdContact = new Contact();
		createdContact.setFirstname("firstname");
		createdContact.setLastname("lastname");
		createdContact.setEmails(ImmutableMap.of("INTERNET;X-OBM-Ref1", EmailAddress.loginAtDomain("contact@mydomain.org")));
		
		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name("contacts")
					.uid(AddressBook.Id.valueOf(contactCollectionId.asInt()))
					.readOnly(false)
					.build())).times(2);
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + "contacts"))
			.andReturn(contactCollectionId).times(2);
		
		Contact storedContact = new Contact();
		storedContact.setFirstname("firstname");
		storedContact.setLastname("lastname");
		storedContact.setEmails(ImmutableMap.of("INTERNET;X-OBM-Ref1", EmailAddress.loginAtDomain("contact@mydomain.org")));
		storedContact.setUid(uid);
		expect(bookClient.storeContact(user.accessToken, contactCollectionId.asInt(), createdContact, hashedClientId))
			.andReturn(storedContact);
		
		itemTrackingDao.markAsSynced(thirdAllocatedState, ImmutableSet.of(serverId));
		expectLastCall().once();
		
		expect(bookClient.listContactsChanged(user.accessToken, syncDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(storedContact),
					ImmutableSet.<Integer> of(),
					syncDate));
		mocksControl.replay();
		opushServer.start();

		WBXMLOPClient opushClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		opushClient.run(Sync.builder(decoder).syncKey(firstAllocatedSyncKey)
					.collection(SyncCollection.builder().collectionId(contactCollectionId).build()).build());
		
		SyncResponse updateSyncResponse = opushClient.run(
				Sync.builder(decoder).encoder(encoderFactory).device(user.device)
					.syncKey(secondAllocatedSyncKey)
					.collection(SyncCollection.builder().collectionId(contactCollectionId).build()).command(SyncCommand.ADD)
					.clientId(clientId).data(createdMSContact).build());
		
		mocksControl.verify();
		assertThat(updateSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		SyncCollectionResponse syncCollectionResponse = syncTestUtils.getCollectionWithId(updateSyncResponse, contactCollectionId);
		assertThat(syncCollectionResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		assertThat(syncCollectionResponse.getCommands().getCommands()).hasSize(0);
	}

	@Test
	public void updatingContactShouldWorkWhenClientModifiesAnAttribute() throws Exception {
		SyncKey firstAllocatedSyncKey = new SyncKey("4a2c7db8-b532-40a0-92c3-bfebb8da8f00");
		SyncKey secondAllocatedSyncKey = new SyncKey("55df3cf4-b70d-4df2-ac48-d31646994321");
		SyncKey thirdAllocatedSyncKey = new SyncKey("4872672c-95b9-4c1c-90ae-ebea7d0e83ed");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int thirdAllocatedStateId = 5;
		
		Date syncDate = date("2012-10-09T16:22:53");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState thirdAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(thirdAllocatedSyncKey)
				.id(thirdAllocatedStateId)
				.build();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey, thirdAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, contactCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, secondAllocatedSyncKey, secondAllocatedState, thirdAllocatedState, contactCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(thirdAllocatedState.getSyncDate()).once();
		Contact initialContact = new Contact();
		initialContact.setUid(1);
		initialContact.setFirstname("firstname");
		initialContact.setLastname("lastname");
		initialContact.setEmails(ImmutableMap.of("INTERNET;X-OBM-Ref1", EmailAddress.loginAtDomain("contact@mydomain.org")));
		initialContact.setPhones(ImmutableMap.of("HOME;FAX;X-OBM-Ref1", new Phone("1234")));
		
		ServerId serverId = contactCollectionId.serverId(initialContact.getUid());
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId))
			.andReturn(false);
		
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(serverId));
		expectLastCall().once();

		// first sync
		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name("contacts")
					.uid(AddressBook.Id.valueOf(contactCollectionId.asInt()))
					.readOnly(false)
					.build()));
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + "contacts"))
			.andReturn(contactCollectionId);
		
		expect(bookClient.listContactsChanged(user.accessToken, syncDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.of(initialContact),
					ImmutableSet.<Integer> of(),
					syncDate));
		
		// second sync
		MSContact modifiedMSContact = new MSContact();
		modifiedMSContact.setFirstName(initialContact.getFirstname());
		modifiedMSContact.setLastName(initialContact.getLastname());
		modifiedMSContact.setEmail1Address("contact@mydomain.org");
		modifiedMSContact.setHomeFaxNumber("4567");
		modifiedMSContact.setFileAs("lastname, firstname");
		
		Contact modifiedContact = new Contact();
		modifiedContact.setUid(1);
		modifiedContact.setFirstname("firstname");
		modifiedContact.setLastname("lastname");
		modifiedContact.setEmails(ImmutableMap.of("INTERNET;X-OBM-Ref1", EmailAddress.loginAtDomain("contact@mydomain.org")));
		modifiedContact.setPhones(ImmutableMap.of("HOME;FAX;X-OBM-Ref1", new Phone("4567")));
		
		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name("contacts")
					.uid(AddressBook.Id.valueOf(contactCollectionId.asInt()))
					.readOnly(false)
					.build())).times(2);
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + "contacts"))
			.andReturn(contactCollectionId).times(2);
		
		expect(bookClient.listContactsChanged(user.accessToken, syncDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(),
					ImmutableSet.<Integer> of(),
					syncDate));
		
		String clientId = null;
		expect(bookClient.storeContact(user.accessToken, contactCollectionId.asInt(), modifiedContact, clientId))
			.andReturn(modifiedContact);
		
		mocksControl.replay();
		opushServer.start();

		WBXMLOPClient opushClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		opushClient.run(
				Sync.builder(decoder).syncKey(firstAllocatedSyncKey)
				.collection(SyncCollection.builder().collectionId(contactCollectionId).build()).build());
		
		SyncResponse updateSyncResponse = opushClient.run(
				Sync.builder(decoder).encoder(encoderFactory).device(user.device)
					.syncKey(secondAllocatedSyncKey)
					.collection(SyncCollection.builder().collectionId(contactCollectionId).build()).command(SyncCommand.CHANGE)
					.serverId(serverId).clientId(clientId).data(modifiedMSContact).build());
		
		mocksControl.verify();
		assertThat(updateSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		SyncCollectionResponse syncCollectionResponse = syncTestUtils.getCollectionWithId(updateSyncResponse, contactCollectionId);
		assertThat(syncCollectionResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		assertThat(syncCollectionResponse.getCommands().getCommands()).hasSize(0);
	}

	@Test
	public void updatingContactShouldWorkWhenClientDeletesAnAttribute() throws Exception {
		SyncKey firstAllocatedSyncKey = new SyncKey("4a2c7db8-b532-40a0-92c3-bfebb8da8f00");
		SyncKey secondAllocatedSyncKey = new SyncKey("55df3cf4-b70d-4df2-ac48-d31646994321");
		SyncKey thirdAllocatedSyncKey = new SyncKey("4872672c-95b9-4c1c-90ae-ebea7d0e83ed");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int thirdAllocatedStateId = 5;
		
		Date syncDate = date("2012-10-09T16:22:53");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState thirdAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(thirdAllocatedSyncKey)
				.id(thirdAllocatedStateId)
				.build();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey, thirdAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, contactCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, secondAllocatedSyncKey, secondAllocatedState, thirdAllocatedState, contactCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(thirdAllocatedState.getSyncDate()).once();
		Contact initialContact = new Contact();
		initialContact.setUid(1);
		initialContact.setFirstname("firstname");
		initialContact.setLastname("lastname");
		initialContact.setEmails(ImmutableMap.of("INTERNET;X-OBM-Ref1", EmailAddress.loginAtDomain("contact@mydomain.org")));
		initialContact.setPhones(ImmutableMap.of("HOME;FAX;X-OBM-Ref1", new Phone("1234")));
		
		ServerId serverId = contactCollectionId.serverId(initialContact.getUid());
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId))
			.andReturn(false);
		
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(serverId));
		expectLastCall().once();

		// first sync
		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name("contacts")
					.uid(AddressBook.Id.valueOf(contactCollectionId.asInt()))
					.readOnly(false)
					.build()));
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + "contacts"))
			.andReturn(contactCollectionId);
		
		expect(bookClient.listContactsChanged(user.accessToken, syncDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(initialContact),
					ImmutableSet.<Integer> of(),
					syncDate));
		
		// second sync
		MSContact modifiedMSContact = new MSContact();
		modifiedMSContact.setFirstName(initialContact.getFirstname());
		modifiedMSContact.setLastName(initialContact.getLastname());
		modifiedMSContact.setEmail1Address("contact@mydomain.org");
		modifiedMSContact.setFileAs("lastname, firstname");
		
		Contact modifiedContact = new Contact();
		modifiedContact.setUid(1);
		modifiedContact.setFirstname("firstname");
		modifiedContact.setLastname("lastname");
		modifiedContact.setEmails(ImmutableMap.of("INTERNET;X-OBM-Ref1", EmailAddress.loginAtDomain("contact@mydomain.org")));
		
		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name("contacts")
					.uid(AddressBook.Id.valueOf(contactCollectionId.asInt()))
					.readOnly(false)
					.build())).times(2);
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + "contacts"))
			.andReturn(contactCollectionId).times(2);
		
		expect(bookClient.listContactsChanged(user.accessToken, syncDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(),
					ImmutableSet.<Integer> of(),
					syncDate));
		
		String clientId = null;
		expect(bookClient.storeContact(user.accessToken, contactCollectionId.asInt(), modifiedContact, clientId))
			.andReturn(modifiedContact);
		
		mocksControl.replay();
		opushServer.start();

		WBXMLOPClient opushClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		opushClient.run(Sync.builder(decoder).syncKey(firstAllocatedSyncKey)
				.collection(SyncCollection.builder().collectionId(contactCollectionId).build()).build());
		
		SyncResponse updateSyncResponse = opushClient.run(
				Sync.builder(decoder).encoder(encoderFactory).device(user.device)
					.syncKey(secondAllocatedSyncKey)
					.collection(SyncCollection.builder().collectionId(contactCollectionId).build()).command(SyncCommand.CHANGE)
					.serverId(serverId).clientId(clientId).data(modifiedMSContact).build());
		
		mocksControl.verify();
		assertThat(updateSyncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		SyncCollectionResponse syncCollectionResponse = syncTestUtils.getCollectionWithId(updateSyncResponse, contactCollectionId);
		assertThat(syncCollectionResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		assertThat(syncCollectionResponse.getCommands().getCommands()).hasSize(0);
	}

	@Test
	public void clientMayAskForAnOldSyncKey() throws Exception {
		SyncKey firstAllocatedSyncKey = new SyncKey("4a2c7db8-b532-40a0-92c3-bfebb8da8f00");
		SyncKey secondAllocatedSyncKey = new SyncKey("55df3cf4-b70d-4df2-ac48-d31646994321");
		SyncKey thirdAllocatedSyncKey = new SyncKey("6214c5ef-e76b-4d71-90ee-1ab92f28181c");
		int firstAllocatedStateId = 3;
		int secondAllocatedStateId = 4;
		int thirdAllocatedStateId = 5;
		
		Date syncDate = date("2012-10-09T16:22:53");
		ItemSyncState firstAllocatedState = ItemSyncState.builder()
				.syncDate(syncDate)
				.syncKey(firstAllocatedSyncKey)
				.id(firstAllocatedStateId)
				.build();
		ItemSyncState secondAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-09T17:22:53"))
				.syncKey(secondAllocatedSyncKey)
				.id(secondAllocatedStateId)
				.build();
		ItemSyncState thirdAllocatedState = ItemSyncState.builder()
				.syncDate(date("2012-10-09T18:22:53"))
				.syncKey(thirdAllocatedSyncKey)
				.id(thirdAllocatedStateId)
				.build();
		
		userAccessUtils.mockUsersAccess(user);
		syncKeyTestUtils.mockNextGeneratedSyncKey(secondAllocatedSyncKey, thirdAllocatedSyncKey);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, secondAllocatedState, contactCollectionId);
		syncTestUtils.mockCollectionDaoPerformSync(user.device, firstAllocatedSyncKey, firstAllocatedState, thirdAllocatedState, contactCollectionId);
		
		expect(dateService.getCurrentDate()).andReturn(secondAllocatedState.getSyncDate()).once();
		expect(dateService.getCurrentDate()).andReturn(thirdAllocatedState.getSyncDate()).once();
		Contact initialContact = new Contact();
		initialContact.setUid(1);
		initialContact.setFirstname("firstname");
		initialContact.setLastname("lastname");
		initialContact.setEmails(ImmutableMap.of("INTERNET;X-OBM-Ref1", EmailAddress.loginAtDomain("contact@mydomain.org")));
		initialContact.setPhones(ImmutableMap.of("HOME;FAX;X-OBM-Ref1", new Phone("1234")));
		
		ServerId serverId = contactCollectionId.serverId(initialContact.getUid());
		expect(itemTrackingDao.isServerIdSynced(firstAllocatedState, serverId))
			.andReturn(false)
			.times(2);
		
		itemTrackingDao.markAsSynced(secondAllocatedState, ImmutableSet.of(serverId));
		expectLastCall();
		itemTrackingDao.markAsSynced(thirdAllocatedState, ImmutableSet.of(serverId));
		expectLastCall();

		expect(bookClient.listAllBooks(user.accessToken))
			.andReturn(ImmutableList.<AddressBook> of(AddressBook
					.builder()
					.name("contacts")
					.uid(AddressBook.Id.valueOf(contactCollectionId.asInt()))
					.readOnly(false)
					.build()));
		expect(collectionDao.getCollectionMapping(user.device, contactCollectionPath + ":" + "contacts"))
			.andReturn(contactCollectionId);
		
		expect(bookClient.listContactsChanged(user.accessToken, syncDate, contactCollectionId.asInt()))
			.andReturn(new ContactChanges(ImmutableList.<Contact> of(initialContact),
					ImmutableSet.<Integer> of(),
					syncDate));
		
		mocksControl.replay();
		opushServer.start();

		WBXMLOPClient opushClient = testUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		SyncResponse syncResponse = opushClient.run(
				Sync.builder(decoder).syncKey(firstAllocatedSyncKey)
				.collection(SyncCollection.builder().collectionId(contactCollectionId).build()).build());
		SyncResponse sameSyncResponse = opushClient.run(
				Sync.builder(decoder).syncKey(firstAllocatedSyncKey)
				.collection(SyncCollection.builder().collectionId(contactCollectionId).build()).build());
		
		mocksControl.verify();
		
		MSContact newMSContact = new MSContact();
		newMSContact.setFirstName("firstname");
		newMSContact.setLastName("lastname");
		newMSContact.setEmail1Address("contact@mydomain.org");
		newMSContact.setHomeFaxNumber("1234");
		newMSContact.setFileAs("firstname lastname");
		
		SyncCollectionCommand expectedCommandResponse = SyncCollectionCommand.builder()
				.type(SyncCommand.ADD)
				.serverId(serverId)
				.clientId(null)
				.applicationData(newMSContact)
				.build();
		
		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		SyncCollectionResponse syncCollectionResponse = syncTestUtils.getCollectionWithId(syncResponse, contactCollectionId);
		assertThat(syncCollectionResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		List<SyncCollectionCommand> commands = syncCollectionResponse.getCommands().getCommands();
		assertThat(commands).hasSize(1);
		SyncCollectionCommand syncCollectionCommandResponse = FluentIterable.from(commands).first().get();
		assertThat(syncCollectionCommandResponse).isEqualTo(expectedCommandResponse);
		
		MSContact msContact = (MSContact) syncCollectionCommandResponse.getApplicationData();
		assertThat(msContact.getFirstName()).isEqualTo("firstname");
		assertThat(msContact.getLastName()).isEqualTo("lastname");
		assertThat(msContact.getEmail1Address()).isEqualTo("contact@mydomain.org");
		assertThat(msContact.getHomeFaxNumber()).isEqualTo("1234");
		
		assertThat(syncResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		SyncCollectionResponse sameSyncCollectionResponse = syncTestUtils.getCollectionWithId(sameSyncResponse, contactCollectionId);
		assertThat(sameSyncCollectionResponse.getStatus()).isEqualTo(SyncStatus.OK);
		
		List<SyncCollectionCommand> sameCommands = sameSyncCollectionResponse.getCommands().getCommands();
		assertThat(sameCommands).hasSize(1);
		SyncCollectionCommand sameSyncCollectionCommandResponse = FluentIterable.from(sameCommands).first().get();
		assertThat(sameSyncCollectionCommandResponse).isEqualTo(expectedCommandResponse);
		
		MSContact sameMSContact = (MSContact) sameSyncCollectionCommandResponse.getApplicationData();
		assertThat(sameMSContact.getFirstName()).isEqualTo("firstname");
		assertThat(sameMSContact.getLastName()).isEqualTo("lastname");
		assertThat(sameMSContact.getEmail1Address()).isEqualTo("contact@mydomain.org");
		assertThat(sameMSContact.getHomeFaxNumber()).isEqualTo("1234");
	}
}
