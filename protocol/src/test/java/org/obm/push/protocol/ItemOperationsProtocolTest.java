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
package org.obm.push.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.obm.push.TestUtils.getXml;

import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.FactoryConfigurationError;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.ProtocolVersion;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ItemOperationsStatus;
import org.obm.push.bean.MSEmailBodyType;
import org.obm.push.bean.MSEmailHeader;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.bean.ms.MSEmailBody;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.ItemOperationsRequest;
import org.obm.push.protocol.bean.ItemOperationsResponse;
import org.obm.push.protocol.bean.ItemOperationsResponse.EmptyFolderContentsResult;
import org.obm.push.protocol.bean.ItemOperationsResponse.MailboxFetchResult;
import org.obm.push.protocol.bean.ItemOperationsResponse.MailboxFetchResult.FetchAttachmentResult;
import org.obm.push.protocol.bean.ItemOperationsResponse.MailboxFetchResult.FetchItemResult;
import org.obm.push.protocol.data.EncoderFactory;
import org.obm.push.utils.DOMUtils;
import org.obm.push.utils.SerializableInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;


public class ItemOperationsProtocolTest {

	private ItemOperationsProtocol itemOperationsProtocol;
	private Device device;

	@Before
	public void setup() {
		device = new Device(1, "devType", new DeviceId("devId"), new Properties(), ProtocolVersion.V121);
		itemOperationsProtocol = new ItemOperationsProtocol.Factory(null).create(device, true);
	}
	
	@Test
	public void test() throws SAXException, IOException, FactoryConfigurationError {
		Document document = getXml(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<ItemOperations>" +
				"<Fetch>" +
				"<Store>Mailbox</Store>" +
				"<CollectionId>1400</CollectionId>" +
				"<ServerId>1400:350025</ServerId>" +
				"<Options>" +
				"<BodyPreference>" +
				"<Type>2</Type>" +
				"</BodyPreference>" +
				"</Options>" +
				"</Fetch>" +
				"</ItemOperations>");

		ItemOperationsRequest decodedRequest = itemOperationsProtocol.decodeRequest(document);
		
		assertThat(decodedRequest).isNotNull();
		assertThat(decodedRequest.getFetch().getCollectionId()).contains(CollectionId.of(1400));
		assertThat(decodedRequest.getFetch().getServerId()).contains(CollectionId.of(1400).serverId(350025));
		assertThat(decodedRequest.getFetch().getType()).isEqualTo(MSEmailBodyType.HTML);
	}

	@Test
	public void testNoOptions() throws SAXException, IOException, FactoryConfigurationError {
		Document document = getXml(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<ItemOperations>" +
				"<Fetch>" +
				"<Store>Mailbox</Store>" +
				"<CollectionId>1400</CollectionId>" +
				"<ServerId>1400:350025</ServerId>" +
				"</Fetch>" +
				"</ItemOperations>");

		ItemOperationsRequest decodedRequest = itemOperationsProtocol.decodeRequest(document);

		assertThat(decodedRequest).isNotNull();
		assertThat(decodedRequest.getFetch().getCollectionId()).contains(CollectionId.of(1400));
		assertThat(decodedRequest.getFetch().getServerId()).contains(CollectionId.of(1400).serverId(350025));
		assertThat(decodedRequest.getFetch().getType()).isNull();
	}

	@Test
	public void testByFileReference() throws SAXException, IOException, FactoryConfigurationError {
		Document document = getXml(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<ItemOperations>" +
				"<Fetch>" +
				"<Store>Mailbox</Store>" +
				"<FileReference>expected value</FileReference>" +
				"</Fetch>" +
				"</ItemOperations>");

		ItemOperationsRequest decodedRequest = itemOperationsProtocol.decodeRequest(document);

		assertThat(decodedRequest).isNotNull();
		assertThat(decodedRequest.getFetch().getCollectionId()).isAbsent();
		assertThat(decodedRequest.getFetch().getServerId()).isAbsent();
		assertThat(decodedRequest.getFetch().getType()).isNull();
		assertThat(decodedRequest.getFetch().getFileReference()).isEqualTo("expected value");
	}

	@Test
	public void testNoApplicationDataEncodingResponse() throws Exception {
		FetchItemResult fetchItemResult = new FetchItemResult();
		fetchItemResult.setServerId(CollectionId.of(1).serverId(2));
		fetchItemResult.setStatus(ItemOperationsStatus.SUCCESS);
		fetchItemResult.setSyncCollection(SyncCollectionResponse.builder()
				.collectionId(CollectionId.of(1))
				.syncKey(SyncKey.INITIAL_SYNC_KEY)
				.build());
		fetchItemResult.setItemChange(null);
		
		MailboxFetchResult mailboxFetchResult = new MailboxFetchResult();
		mailboxFetchResult.setFetchItemResult(fetchItemResult);
		
		boolean isMultipart = true;
		ItemOperationsResponse response = new ItemOperationsResponse();
		response.setMailboxFetchResult(mailboxFetchResult);

		EncoderFactory applicationDataEncoder = createMock(EncoderFactory.class);
		replay(applicationDataEncoder);

		ItemOperationsProtocol protocol = new ItemOperationsProtocol.Factory(applicationDataEncoder)
			.create(device, isMultipart);
		Document doc = protocol.encodeResponse(device, response);
		
		verify(applicationDataEncoder);
		
		assertThat(DOMUtils.serialize(doc)).isEqualTo(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<ItemOperations>" +
				"<Status>1</Status>" +
				"<Response>" +
					"<Fetch>" +
						"<Status>1</Status>" +
						"<AirSync:ServerId>1:2</AirSync:ServerId>" +
					"</Fetch>" +
				"</Response>" +
			"</ItemOperations>");
	}

	@Test
	public void testMailboxEncodingResponse() throws Exception {
		ItemChange itemChange = ItemChange.builder()
				.serverId(CollectionId.of(1).serverId(2))
				.isNew(true)
				.data(msEmail("my message"))
				.build();

		FetchItemResult fetchItemResult = new FetchItemResult();
		fetchItemResult.setServerId(CollectionId.of(1).serverId(2));
		fetchItemResult.setStatus(ItemOperationsStatus.SUCCESS);
		fetchItemResult.setSyncCollection(SyncCollectionResponse.builder()
				.collectionId(CollectionId.of(1))
				.syncKey(SyncKey.INITIAL_SYNC_KEY)
				.build());
		fetchItemResult.setItemChange(itemChange);
		
		MailboxFetchResult mailboxFetchResult = new MailboxFetchResult();
		mailboxFetchResult.setFetchItemResult(fetchItemResult);

		boolean isMultipart = true;
		ItemOperationsResponse response = new ItemOperationsResponse();
		response.setMailboxFetchResult(mailboxFetchResult);
		
		EncoderFactory applicationDataEncoder = assertApplicationDataEncodeIsCalled();
		replay(applicationDataEncoder);

		ItemOperationsProtocol protocol = new ItemOperationsProtocol.Factory(applicationDataEncoder)
			.create(device, isMultipart);
		Document doc = protocol.encodeResponse(device, response);

		verify(applicationDataEncoder);
		
		assertThat(DOMUtils.serialize(doc)).isEqualTo(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<ItemOperations>" +
				"<Status>1</Status>" +
				"<Response>" +
					"<Fetch>" +
						"<Status>1</Status>" +
						"<AirSync:ServerId>1:2</AirSync:ServerId>" +
						"<Properties/>" +
					"</Fetch>" +
				"</Response>" +
			"</ItemOperations>");
	}

	@Test
	public void testMailboxServerErrorEncodingResponse() throws Exception {
		ItemChange itemChange = ItemChange.builder()
				.serverId(CollectionId.of(1).serverId(2))
				.isNew(true)
				.data(msEmail("my message"))
				.build();

		FetchItemResult fetchItemResult = new FetchItemResult();
		fetchItemResult.setServerId(CollectionId.of(1).serverId(2));
		fetchItemResult.setStatus(ItemOperationsStatus.SERVER_ERROR);
		fetchItemResult.setSyncCollection(SyncCollectionResponse.builder()
				.collectionId(CollectionId.of(1))
				.syncKey(SyncKey.INITIAL_SYNC_KEY)
				.build());
		fetchItemResult.setItemChange(itemChange);
		
		MailboxFetchResult mailboxFetchResult = new MailboxFetchResult();
		mailboxFetchResult.setFetchItemResult(fetchItemResult);

		boolean isMultipart = true;
		ItemOperationsResponse response = new ItemOperationsResponse();
		response.setMailboxFetchResult(mailboxFetchResult);

		EncoderFactory applicationDataEncoder = createMock(EncoderFactory.class);
		ItemOperationsProtocol protocol = new ItemOperationsProtocol.Factory(applicationDataEncoder)
			.create(device, isMultipart);
		Document doc = protocol.encodeResponse(device, response);

		assertThat(DOMUtils.serialize(doc)).isEqualTo(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<ItemOperations>" +
				"<Status>1</Status>" +
				"<Response>" +
					"<Fetch>" +
						"<Status>3</Status>" +
						"<AirSync:ServerId>1:2</AirSync:ServerId>" +
					"</Fetch>" +
				"</Response>" +
			"</ItemOperations>");
	}
	
	@Test
	public void testEmptyFolderEncodingResponse() throws Exception {
		FetchItemResult fetchItemResult = new FetchItemResult();
		fetchItemResult.setServerId(CollectionId.of(1).serverId(2));
		fetchItemResult.setStatus(ItemOperationsStatus.SUCCESS);
		fetchItemResult.setSyncCollection(SyncCollectionResponse.builder()
				.collectionId(CollectionId.of(1))
				.syncKey(SyncKey.INITIAL_SYNC_KEY)
				.build());
		
		EmptyFolderContentsResult emptyFolderContentsResult = new EmptyFolderContentsResult();
		emptyFolderContentsResult.setCollectionId(CollectionId.of(1));
		emptyFolderContentsResult.setItemOperationsStatus(ItemOperationsStatus.SUCCESS);
		
		boolean isMultipart = true;
		ItemOperationsResponse response = new ItemOperationsResponse();
		response.setEmptyFolderContentsResult(emptyFolderContentsResult);

		EncoderFactory applicationDataEncoder = createMock(EncoderFactory.class);
		ItemOperationsProtocol protocol = new ItemOperationsProtocol.Factory(applicationDataEncoder)
			.create(device, isMultipart);
		Document doc = protocol.encodeResponse(device, response);

		assertThat(DOMUtils.serialize(doc)).isEqualTo(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<ItemOperations>" +
				"<Status>1</Status>" +
				"<Response>" +
					"<EmptyFolderContents>" +
						"<Status>1</Status>" +
						"<AirSync:CollectionId>1</AirSync:CollectionId>" +
					"</EmptyFolderContents>" +
				"</Response>" +
			"</ItemOperations>");
	}
	
	@Test
	public void testFetchAttachmentEncodingResponseWhenMultipartIsAccepted() throws Exception {
		boolean multipartIsAccpeted = true;
		
		Document doc = testFetchAttachmentEncodingResponseWithMultipart(multipartIsAccpeted);
		
		assertThat(DOMUtils.serialize(doc)).isEqualTo(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<ItemOperations>" +
				"<Status>1</Status>" +
				"<Response>" +
					"<Fetch>" +
						"<Status>1</Status>" +
						"<AirSyncBase:FileReference>ref</AirSyncBase:FileReference>" +
						"<Properties>" +
							"<AirSyncBase:ContentType>text/plain</AirSyncBase:ContentType>" +
							"<Part>1</Part>" +
						"</Properties>" +
					"</Fetch>" +
				"</Response>" +
			"</ItemOperations>");
	}
	
	@Test
	public void testFetchAttachmentEncodingResponseWhenMultipartIsNotAccepted() throws Exception {
		boolean multipartIsAccpeted = false;
		
		Document doc = testFetchAttachmentEncodingResponseWithMultipart(multipartIsAccpeted);
		
		assertThat(DOMUtils.serialize(doc)).isEqualTo(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<ItemOperations>" +
				"<Status>1</Status>" +
				"<Response>" +
					"<Fetch>" +
						"<Status>1</Status>" +
						"<AirSyncBase:FileReference>ref</AirSyncBase:FileReference>" +
						"<Properties>" +
							"<AirSyncBase:ContentType>text/plain</AirSyncBase:ContentType>" +
							"<Data>data</Data>" +
						"</Properties>" +
					"</Fetch>" +
				"</Response>" +
			"</ItemOperations>");
	}

	private Document testFetchAttachmentEncodingResponseWithMultipart(boolean isMultipart) {
		FetchAttachmentResult fetchAttachmentResult = new FetchAttachmentResult();
		fetchAttachmentResult.setReference("ref");
		fetchAttachmentResult.setAttch("data".getBytes());
		fetchAttachmentResult.setContentType("text/plain");
		fetchAttachmentResult.setStatus(ItemOperationsStatus.SUCCESS);
		
		MailboxFetchResult mailboxFetchResult = new MailboxFetchResult();
		mailboxFetchResult.setFetchAttachmentResult(fetchAttachmentResult);

		ItemOperationsResponse response = new ItemOperationsResponse();
		response.setMailboxFetchResult(mailboxFetchResult);

		EncoderFactory applicationDataEncoder = createMock(EncoderFactory.class);
		replay(applicationDataEncoder);

		ItemOperationsProtocol protocol = new ItemOperationsProtocol.Factory(applicationDataEncoder)
			.create(device, isMultipart);
		Document doc = protocol.encodeResponse(device, response);

		verify(applicationDataEncoder);
		return doc;
	}
	
	private EncoderFactory assertApplicationDataEncodeIsCalled() throws Exception {
		EncoderFactory encoderFactory = createMock(EncoderFactory.class);
		encoderFactory.encode(anyObject(Device.class), anyObject(Element.class),
				anyObject(IApplicationData.class), anyBoolean());
		expectLastCall();
		return encoderFactory;
	}
	
	private MSEmail msEmail(String message) {
		return MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(Optional.of(new SerializableInputStream(message)))
					.bodyType(MSEmailBodyType.MIME)
					.estimatedDataSize(0)
					.charset(Charsets.UTF_8)
					.truncated(false)
					.build())
			.build();
	}

}
