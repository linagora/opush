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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.obm.push.mail.MSMailTestsUtils.loadEmail;
import static org.obm.push.mail.MSMailTestsUtils.mockOpushConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.configuration.EmailConfiguration;
import org.obm.push.ExpungePolicy;
import org.obm.push.bean.Address;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.configuration.OpushEmailConfiguration;
import org.obm.push.exception.SendEmailException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.exception.activesync.StoreEmailException;
import org.obm.push.mail.bean.EmailReader;
import org.obm.push.mail.bean.MessageSet;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.service.AuthenticationService;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.service.SmtpSender;
import org.obm.push.service.impl.MappingService;
import org.obm.push.store.WindowingDao;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;


public class MailBackendTest {

	private User user;
	private Device device;
	private UserDataRequest udr;
	private MailboxService mailboxService;
	private MappingService mappingService;
	private WindowingDao windowingDao;
	private SmtpSender smtpSender;
	private OpushEmailConfiguration emailConfiguration;
	private DateService dateService;
	private FolderSnapshotDao folderSnapshotDao;

	private IMocksControl mocksControl;
	private MailBackend testee;

	@Before
	public void setUp() {
		user = Factory.create().createUser("test@test", "test@domain", "displayName");
		device = new Device.Factory().create(null, "iPhone", "iOs 5", new DeviceId("my phone"), null);
		udr = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
		
		mocksControl = createControl();
		
		mailboxService = mocksControl.createMock(MailboxService.class);
		mappingService = mocksControl.createMock(MappingService.class);
		windowingDao = mocksControl.createMock(WindowingDao.class);
		smtpSender = mocksControl.createMock(SmtpSender.class);
		emailConfiguration = mocksControl.createMock(OpushEmailConfiguration.class);
		dateService = mocksControl.createMock(DateService.class);
		folderSnapshotDao = mocksControl.createMock(FolderSnapshotDao.class);
		
		testee = new MailBackendImpl(mailboxService, null, null, null, null, null,  
				mappingService, null, null, null, 
				windowingDao, smtpSender, emailConfiguration, dateService, folderSnapshotDao);
	}
	
	@Test
	public void testSendEmailWithBigMail()
			throws ProcessingEmailException, StoreEmailException, SendEmailException, IOException {
		
		AuthenticationService authenticationService = mocksControl.createMock(AuthenticationService.class);
		UserDataRequest userDataRequest = mocksControl.createMock(UserDataRequest.class);
		
		expect(authenticationService.getUserEmail(userDataRequest)).andReturn(user.getLoginAtDomain()).once();
		expect(userDataRequest.getUser()).andReturn(user).once();
		
		Set<Address> addrs = Sets.newHashSet();
		smtpSender.sendEmail(anyObject(UserDataRequest.class), anyObject(Address.class), 
				anyObject(addrs.getClass()), anyObject(addrs.getClass()), anyObject(addrs.getClass()), 
				anyObject(InputStream.class));
		expectLastCall().once();
		
		mailboxService.storeInSent(anyObject(UserDataRequest.class), anyObject(EmailReader.class));
		expectLastCall().once();
				
		MailBackend mailBackend = new MailBackendImpl(mailboxService, authenticationService, new Mime4jUtils(),
				mockOpushConfiguration(), null, null, mappingService, null, null,
				null, windowingDao, smtpSender, emailConfiguration,
				dateService, folderSnapshotDao);

		mocksControl.replay();
		
		InputStream emailStream = loadEmail("bigEml.eml");
		mailBackend.sendEmail(userDataRequest, ByteStreams.toByteArray(emailStream), true);
		
		mocksControl.verify();
	}

	@Test
	public void testDeleteItemInTrash() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		int itemId = 2;
		ServerId serverId = collectionId.serverId(itemId);
		
		MailboxPath trashPath = MailboxPath.of(EmailConfiguration.IMAP_TRASH_NAME);
		
		Folder folder = Folder.builder()
			.collectionId(collectionId)
			.backendId(trashPath)
			.displayName(EmailConfiguration.IMAP_TRASH_NAME)
			.folderType(FolderType.DEFAULT_DELETED_ITEMS_FOLDER)
			.parentBackendIdOpt(Optional.<BackendId>absent())
			.build();
		
		expect(folderSnapshotDao.get(udr.getUser(), udr.getDevice(), collectionId)).andReturn(folder);
		
		mailboxService.delete(udr, trashPath, MessageSet.singleton(itemId));
		expectLastCall();

		expect(emailConfiguration.expungePolicy()).andReturn(ExpungePolicy.ALWAYS).once();
		mailboxService.expunge(udr, trashPath);
		expectLastCall().once();
		
		mocksControl.replay();
		 
		testee.delete(udr, collectionId, serverId, true);
		mocksControl.verify();
	}
}