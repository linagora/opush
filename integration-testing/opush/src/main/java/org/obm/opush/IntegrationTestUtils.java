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
package org.obm.opush;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import javax.mail.Session;

import org.apache.http.impl.client.CloseableHttpClient;
import org.obm.opush.Users.OpushUser;
import org.obm.push.ProtocolVersion;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.bean.Device;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.mail.bean.FlagsList;
import org.obm.push.mail.bean.MessageSet;
import org.obm.push.mail.bean.UIDEnvelope;
import org.obm.push.mail.imap.LinagoraMailboxService;
import org.obm.push.mail.mime.MimeAddress;
import org.obm.push.mail.mime.MimeMessage;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.state.StateMachine;
import org.obm.push.store.CollectionDao;
import org.obm.sync.client.login.LoginClient;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.WBXMLOPClient;
import org.obm.sync.push.client.XMLOPClient;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;

public class IntegrationTestUtils {

	@Inject LoginClient loginClient;
	@Inject StateMachine stateMachine;
	@Inject CollectionDao collectionDao;
	@Inject LinagoraMailboxService mailboxService;
	@Inject IContentsExporter contentsExporter;
	
	public void expectSyncState(SyncKey syncKey, ItemSyncState syncState) throws DaoException {
		expect(stateMachine.getItemSyncState(syncKey)).andReturn(syncState).anyTimes();
	}
	
	public void expectUserCollectionsNeverChange() throws DaoException, CollectionNotFoundException {
		
		Date lastSync = new Date();
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(lastSync)
				.syncKey(new SyncKey("sync state"))
				.build();
		expect(collectionDao.lastKnownState(anyObject(Device.class), anyObject(CollectionId.class))).andReturn(syncState).anyTimes();
	}

	public OPClient buildOpushClient(OpushUser user, int port, CloseableHttpClient httpClient) {
		return new XMLOPClient(httpClient, 
				user.user.getLoginAtDomain(), 
				user.password, 
				user.deviceId, 
				user.deviceType, 
				user.userAgent, port
			);
	}
	
	public WBXMLOPClient buildWBXMLOpushClient(OpushUser user, int port, ProtocolVersion protocolVersion, CloseableHttpClient httpClient) {
		return new WBXMLOPClient.Factory().create(
				httpClient,
				user.user.getLoginAtDomain(), 
				user.password, 
				user.deviceId, 
				user.deviceType, 
				user.userAgent, "localhost", port, "/opush/ActiveSyncServlet/",
				protocolVersion);
	}
	
	public WBXMLOPClient buildWBXMLOpushClient(OpushUser user, int port, CloseableHttpClient httpClient) {
		return buildWBXMLOpushClient(user, port, ProtocolVersion.V121, httpClient);
	}

	public void appendToINBOX(GreenMailUser greenMailUser, String emailPath) throws Exception {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		javax.mail.internet.MimeMessage mimeMessage = new javax.mail.internet.MimeMessage(session, streamEmail(emailPath));
		greenMailUser.deliver(mimeMessage);
	}
	
	public void sendMultipleEmails(GreenMail greenMail, String email, int numberOfEmails) throws InterruptedException {
		for (int i = 0; i< numberOfEmails; i++) {
			GreenMailUtil.sendTextEmail(email, email, "subject" + i, "body", greenMail.getSmtp().getServerSetup());
		}
		greenMail.waitForIncomingEmail(numberOfEmails);
	}

	public byte[] loadEmail(String emailPath) throws IOException {
		return ByteStreams.toByteArray(streamEmail(emailPath));
	}

	public InputStream streamEmail(String emailPath) {
		return ClassLoader.getSystemResourceAsStream(emailPath);
	}

	public void expectFetchFlags(UserDataRequest udr, MailboxPath path, long uid, FlagsList value) {
		expect(mailboxService.fetchFlags(udr, path, MessageSet.singleton(uid))).andReturn(ImmutableMap.of(uid, value));
	}

	public void expectFetchEnvelope(UserDataRequest udr, MailboxPath path, int uid, UIDEnvelope envelope) {
		expect(mailboxService.fetchEnvelope(udr, path, MessageSet.singleton(uid)))
			.andReturn(ImmutableList.of(envelope));
	}

	public void expectFetchBodyStructure(UserDataRequest udr, MailboxPath path, int uid, MimeMessage mimeMessage) {
		expect(mailboxService.fetchBodyStructure(udr, path, MessageSet.singleton(uid)))
			.andReturn(ImmutableList.of(mimeMessage));
	}

	public void expectFetchMailStream(UserDataRequest udr, MailboxPath path, int uid, InputStream mailStream) {
		expect(mailboxService.fetchMailStream(udr, path, uid))
				.andReturn(mailStream);
	}

	public void expectFetchMimePartStream(UserDataRequest udr, MailboxPath path, int uid, InputStream mailStream, MimeAddress partAddress) {
		expect(mailboxService.fetchMimePartStream(udr, path, uid, partAddress))
			.andReturn(mailStream);
	}
	
	public void expectContentExporterFetching(UserDataRequest userDataRequest, ItemChange itemChange) throws Exception {
		expect(contentsExporter.fetch(eq(userDataRequest), anyObject(ItemSyncState.class), anyObject(PIMDataType.class), anyObject(CollectionId.class), anyObject(SyncCollectionOptions.class), eq(itemChange.getServerId()), anyObject(SyncKey.class)))
			.andReturn(Optional.of(itemChange));
	}
}
