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
package org.obm.opush.command.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Files;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.icalendar.ICalendar;
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.Users;
import org.obm.opush.env.CassandraServer;
import org.obm.opush.env.DefaultOpushModule;
import org.obm.push.OpushServer;
import org.obm.push.bean.AttendeeStatus;
import org.obm.push.bean.Device;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.MSEmailBodyType;
import org.obm.push.bean.MSEmailHeader;
import org.obm.push.bean.MeetingResponseStatus;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.bean.ms.MSEmailBody;
import org.obm.push.bean.ms.UidMSEmail;
import org.obm.push.calendar.CalendarBackend;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.ICalendarConverterException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.UnsupportedBackendFunctionException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.HierarchyChangedException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NotAllowedException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.mail.MailBackend;
import org.obm.push.protocol.MeetingProtocol;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.ItemChangeMeetingResponse;
import org.obm.push.protocol.bean.MeetingHandlerResponse;
import org.obm.push.store.CollectionDao;
import org.obm.push.utils.DOMUtils;
import org.obm.push.utils.DateUtils;
import org.obm.push.utils.SerializableInputStream;
import org.obm.push.wbxml.WBXmlException;
import org.obm.sync.push.client.HttpRequestException;
import org.obm.sync.push.client.OPClient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@RunWith(GuiceRunner.class)
@GuiceModule(DefaultOpushModule.class)
public class MeetingResponseHandlerTest {

	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private MeetingProtocol protocol;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationTestUtils testUtils;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private MailBackend mailBackend;
	@Inject private CalendarBackend calendarBackend;
	@Inject private CollectionDao collectionDao;
	
	private CollectionId meetingCollectionId;
	private int meetingItemId;
	private CollectionId invitationCollectionId;
	private int invitationItemId;
	private CloseableHttpClient httpClient;

	@Before
	public void setUp() throws Exception {
		httpClient = HttpClientBuilder.create().build();
		cassandraServer.start();
		meetingCollectionId = CollectionId.of(2);
		meetingItemId = 8;
		invitationCollectionId = CollectionId.of(5);
		invitationItemId = 10;

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
	public void testItemNotFoundInInvitationEmailDeletionDoesNotMakeFailTheCommand() throws Exception {
		prepareMockForEmailDeletionError(new ItemNotFoundException());
		opushServer.start();

		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsSuccess(serverResponse);
	}

	@Test
	public void testServerExceptionInInvitationEmailDeletionDoesNotMakeTheCommandFail() throws Exception {
		prepareMockForEmailDeletionError(new UnexpectedObmSyncServerException());
		opushServer.start();
		
		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsSuccess(serverResponse);
	}

	@Test
	public void testEmailExceptionInInvitationEmailDeletionDoesNotMakeTheCommandFail() throws Exception {
		prepareMockForEmailDeletionError(new ProcessingEmailException());
		opushServer.start();
		
		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsSuccess(serverResponse);
	}

	@Test
	public void testCollectionExceptionInInvitationEmailDeletionDoesNotMakeTheCommandFail() throws Exception {
		prepareMockForEmailDeletionError(new CollectionNotFoundException());
		opushServer.start();
		
		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsSuccess(serverResponse);
	}

	@Test
	public void testUnsupportedExceptionInInvitationEmailDeletionDoesNotMakeTheCommandFail() throws Exception {
		prepareMockForEmailDeletionError(new UnsupportedBackendFunctionException("No message"));
		opushServer.start();
		
		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsSuccess(serverResponse);
	}

	@Test
	public void testDaoExceptionInInvitationEmailDeletionDoesNotMakeTheCommandFail() throws Exception {
		prepareMockForEmailDeletionError(new DaoException("No message"));
		opushServer.start();
		
		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsSuccess(serverResponse);
	}

	@Test
	public void testRuntimeExceptionInInvitationEmailDeletionTriggersHttpServerErrorStatus() throws Exception {
		prepareMockForEmailDeletionError(new RuntimeException());
		opushServer.start();
		
		int expectedHttpStatus = -1;
		try {
			postMeetingAcceptedResponse();
		} catch (HttpRequestException e) {
			expectedHttpStatus = e.getStatusCode();
		}
		assertThat(expectedHttpStatus).isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR);
	}

	@Test
	public void testItemNotFoundInMeetingResponseHandlingMakesTheCommandFail() throws Exception {
		prepareMockForMeetingResponseHandlingError(new ItemNotFoundException());
		opushServer.start();

		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsFailure(serverResponse);
	}

	@Test
	public void testServerExceptionInMeetingResponseHandlingMakesTheCommandFail() throws Exception {
		prepareMockForMeetingResponseHandlingError(new UnexpectedObmSyncServerException());
		opushServer.start();

		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsFailure(serverResponse);
	}

	@Test
	public void testCollectionExceptionInMeetingResponseHandlingMakesTheCommandFail() throws Exception {
		prepareMockForMeetingResponseHandlingError(new CollectionNotFoundException());
		opushServer.start();

		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsInvalidRequest(serverResponse);
	}

	@Test
	public void testDaoExceptionInMeetingResponseHandlingMakesTheCommandFail() throws Exception {
		prepareMockForMeetingResponseHandlingError(new DaoException("No message"));
		opushServer.start();

		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsFailure(serverResponse);
	}

	@Test
	public void testRuntimeExceptionInMeetingResponseHandlingTriggersHttpServerErrorStatus() throws Exception {
		prepareMockForMeetingResponseHandlingError(new RuntimeException());
		opushServer.start();
		
		int expectedHttpStatus = -1;
		try {
			postMeetingAcceptedResponse();
		} catch (HttpRequestException e) {
			expectedHttpStatus = e.getStatusCode();
		}
		assertThat(expectedHttpStatus).isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR);
	}

	@Test
	public void testHierarchyChangedExceptionInMeetingResponseHandlingMakesTheCommandFail() throws Exception {
		prepareMockForMeetingResponseHandlingError(new HierarchyChangedException(new NotAllowedException("Not allowed")));
		opushServer.start();

		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsFailure(serverResponse);
	}

	@Test
	public void testICalendarConverterExceptionInMeetingResponseHandlingMakesTheCommandFail() throws Exception {
		prepareMockForMeetingResponseHandlingError(new ICalendarConverterException());
		opushServer.start();

		Document serverResponse = postMeetingAcceptedResponse();
		
		assertMeetingResponseIsFailure(serverResponse);
	}

	private Document postMeetingAcceptedResponse()
			throws TransformerException, WBXmlException, IOException, HttpRequestException, SAXException  {
		
		OPClient opClient = testUtils.buildWBXMLOpushClient(users.jaures, opushServer.getHttpPort(), httpClient);
		Document document = buildMeetingAcceptedResponse();

		Document serverResponse = opClient.postXml("MeetingResponse", document, "MeetingResponse");
		return serverResponse;
	}

	private void assertMeetingResponseIsSuccess(Document response) throws TransformerException {
		String responseAsText = DOMUtils.serialize(response);
		String expectedResponse = buildMeetingResponseCommandSuccess();
		assertThat(responseAsText).isEqualTo(expectedResponse);
	}

	private void assertMeetingResponseIsFailure(Document response) throws TransformerException {
		String responseAsText = DOMUtils.serialize(response);
		String expectedResponse = buildMeetingResponseCommandFailure();
		assertThat(responseAsText).isEqualTo(expectedResponse);
	}

	private void assertMeetingResponseIsInvalidRequest(Document response) throws TransformerException {
		String responseAsText = DOMUtils.serialize(response);
		String expectedResponse = buildMeetingResponseCommandInvalidRequest();
		assertThat(responseAsText).isEqualTo(expectedResponse);
	}

	private void prepareMockForEmailDeletionError(Exception triggeredException) throws Exception {
		prepareMockForCommonNeeds();

		expectMailbackendDeleteInvitationTriggersException(triggeredException);
		expectHandleMeetingResponseProcessCorrectly();
		
		mocksControl.replay();
	}

	private void prepareMockForMeetingResponseHandlingError(Exception triggeredException) throws Exception {
		prepareMockForCommonNeeds();
		
		expectMailbackendDeleteInvitationProcessCorrectly();
		expectHandleMeetingResponseTriggersException(triggeredException);
		
		mocksControl.replay();
	}
	
	private void prepareMockForCommonNeeds() throws Exception {
		
		userAccessUtils.mockUsersAccess(users.jaures);
		expectCollectionDaoUnchange();
		expectMailbackendGiveEmailForAnyIds();
		expectMailbackendGettingInvitation();
	}
	
	private void expectMailbackendDeleteInvitationProcessCorrectly() throws Exception {
		
		mailBackend.delete(anyObject(UserDataRequest.class), anyObject(CollectionId.class), anyObject(ServerId.class), anyBoolean());
		EasyMock.expectLastCall().once();
	}
	
	private void expectMailbackendDeleteInvitationTriggersException(Exception triggeredException)
			throws Exception {
		
		mailBackend.delete(anyObject(UserDataRequest.class), anyObject(CollectionId.class), anyObject(ServerId.class), anyBoolean());
		EasyMock.expectLastCall().andThrow(triggeredException);
	}

	private void expectHandleMeetingResponseProcessCorrectly()
			throws Exception {
		
		expect(calendarBackend.handleMeetingResponse(
				anyObject(UserDataRequest.class),
				anyObject(ICalendar.class),
				anyObject(AttendeeStatus.class)))
			.andReturn(meetingCollectionId.serverId(meetingItemId));
	}

	private void expectHandleMeetingResponseTriggersException(Exception triggeredException)
			throws Exception {
		
		expect(calendarBackend.handleMeetingResponse(
				anyObject(UserDataRequest.class),
				anyObject(ICalendar.class),
				anyObject(AttendeeStatus.class)))
			.andThrow(triggeredException);
	}

	private void expectMailbackendGiveEmailForAnyIds()
			throws CollectionNotFoundException, ProcessingEmailException {
		
		expect(mailBackend.getEmail(anyObject(UserDataRequest.class), anyObject(CollectionId.class), anyObject(ServerId.class)))
			.andReturn(UidMSEmail.uidBuilder()
					.uid(1)
					.email(msEmail())
					.build());
	}
	
	private MSEmail msEmail() {
		return MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(Optional.of(new SerializableInputStream()))
					.bodyType(MSEmailBodyType.MIME)
					.estimatedDataSize(0)
					.charset(Charsets.UTF_8)
					.truncated(false)
					.build())
			.build();
	}

	private void expectMailbackendGettingInvitation()
			throws CollectionNotFoundException, ProcessingEmailException {
		
		expect(mailBackend.getInvitation(anyObject(UserDataRequest.class), anyObject(CollectionId.class), anyObject(ServerId.class)))
			.andReturn(null);
	}

	private void expectCollectionDaoUnchange() throws DaoException {
		ItemSyncState syncState = ItemSyncState.builder()
				.syncDate(DateUtils.getEpochPlusOneSecondCalendar().getTime())
				.syncKey(new SyncKey("sync state"))
				.build();
		expect(collectionDao.lastKnownState(anyObject(Device.class), anyObject(CollectionId.class))).andReturn(syncState).anyTimes();
	}

	private String buildMeetingResponseCommandSuccess() throws TransformerException {
		return buildResponse(MeetingResponseStatus.SUCCESS, meetingCollectionId.serverId(meetingItemId));
	}

	private String buildMeetingResponseCommandInvalidRequest() throws TransformerException {
		return buildResponse(MeetingResponseStatus.INVALID_MEETING_RREQUEST);
	}

	private String buildMeetingResponseCommandFailure() throws TransformerException {
		return buildResponse(MeetingResponseStatus.SERVER_ERROR);
	}
	
	private String buildResponse(MeetingResponseStatus status) throws TransformerException {
		return buildResponse(status, null);
	}
		
	private String buildResponse(MeetingResponseStatus status, ServerId serverId) throws TransformerException {
		ItemChangeMeetingResponse itemChangeMeetingResponse = ItemChangeMeetingResponse.builder()
			.reqId(invitationCollectionId.serverId(invitationItemId))
			.calId(serverId)
			.status(status)
			.build();
		
		MeetingHandlerResponse response = MeetingHandlerResponse.builder()
			.itemChanges(Lists.newArrayList(itemChangeMeetingResponse))
			.build();
		
		Document encodeResponses = protocol.encodeResponse(users.jaures.device, response);
		return DOMUtils.serialize(encodeResponses);
	}

	private Document buildMeetingAcceptedResponse()
			throws SAXException, IOException {
		
		return DOMUtils.parse(
				"<MeetingResponse>" +
					"<Request>" +
						"<UserResponse>1</UserResponse>" + // MS-ASCMD 2.2.1.9.2.4 : 1 is SUCCESS
						"<CollectionId>" + invitationCollectionId.asString() + "</CollectionId>" +
						"<ReqId>" + invitationCollectionId.serverId(invitationItemId).asString() + "</ReqId>" +
					"</Request>" +
				"</MeetingResponse>");
	}

}
