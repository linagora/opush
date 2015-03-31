/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015  Linagora
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

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.ProtocolVersion;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderCreateStatus;
import org.obm.push.bean.change.hierarchy.FolderCreateRequest;
import org.obm.push.bean.change.hierarchy.FolderCreateResponse;
import org.obm.push.exception.activesync.ProtocolException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;


public class FolderCreateProtocolTest {
	
	private FolderCreateProtocol folderCreateProtocol;
	private Device device;
	
	@Before
	public void init() {
		folderCreateProtocol = new FolderCreateProtocol();
		device = new Device(1, "devType", new DeviceId("devId"), new Properties(), ProtocolVersion.V121);
	}

	@Test
	public void testDecodeAndEncodeFolderCreateRequestProtocolMethods() throws Exception {
		String initialDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FolderCreate>" +
				"<SyncKey>2f70eaa5-a95a-4f4e-af94-e062955be19b</SyncKey>" +
				"<ParentId>11</ParentId>" +
				"<DisplayName>displayName</DisplayName>" +
				"<Type>18</Type>" +
				"</FolderCreate>";
		
		FolderCreateRequest folderCreateRequest = 
			folderCreateProtocol.decodeRequest(DOMUtils.parse(initialDocument));

		Document encodedRequest = folderCreateProtocol.encodeRequest(folderCreateRequest);
		
		assertThat(initialDocument).isEqualTo(DOMUtils.serialize(encodedRequest));
	}
	
	@Test
	public void testDecodeAndEncodeFolderCreateResponseProtocolMethods() throws Exception {
		String initialDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FolderCreate>" +
				"<Status>1</Status>" +
				"<SyncKey>2056e98e-be0e-4d1a-8f39-328614a32f3a</SyncKey>" +
				"<ServerId>13</ServerId>" +
				"</FolderCreate>";
		
		FolderCreateResponse folderCreateResponse = 
			folderCreateProtocol.decodeResponse(DOMUtils.parse(initialDocument));
		Document encodedResponse = folderCreateProtocol.encodeResponse(device, folderCreateResponse);
		
		assertThat(DOMUtils.serialize(encodedResponse)).isEqualTo(initialDocument);
	}
	
	@Test
	public void testDecodeStatusOK() throws Exception {
		String initialDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FolderCreate>" +
				"<Status>1</Status>" +
				"<SyncKey>2056e98e-be0e-4d1a-8f39-328614a32f3a</SyncKey>" +
				"<ServerId>13</ServerId>" +
				"</FolderCreate>";
		
		FolderCreateResponse folderCreateResponse =  
			folderCreateProtocol.decodeResponse(DOMUtils.parse(initialDocument));

		assertThat(folderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.OK);
	}
	
	@Test
	public void testDecodeStatusInvalidSyncKey() throws Exception {
		String initialDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FolderCreate>" +
				"<Status>9</Status>" +
				"<SyncKey>2056e98e-be0e-4d1a-8f39-328614a32f3a</SyncKey>" +
				"<ServerId>13</ServerId>" +
				"</FolderCreate>";
		
		FolderCreateResponse folderCreateResponse =  
			folderCreateProtocol.decodeResponse(DOMUtils.parse(initialDocument));

		assertThat(folderCreateResponse.getStatus()).isEqualTo(FolderCreateStatus.INVALID_SYNC_KEY);
	}
	
	@Test(expected = ProtocolException.class)
	public void testDecodeMissingStatusShouldThrowError() throws Exception {
		String initialDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FolderCreate>" +
				"<SyncKey>2056e98e-be0e-4d1a-8f39-328614a32f3a</SyncKey>" +
				"<ServerId>13</ServerId>" +
				"</FolderCreate>";
		
		folderCreateProtocol.decodeResponse(DOMUtils.parse(initialDocument));
	}
	
	@Test
	public void testDecodeMissingSyncKeyShouldThrowError() throws Exception {
		String initialDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FolderCreate>" +
				"<Status>9</Status>" +
				"<ServerId>13</ServerId>" +
				"</FolderCreate>";
		
		FolderCreateResponse response = folderCreateProtocol.decodeResponse(DOMUtils.parse(initialDocument));
		
		assertThat(response.getSyncKey()).isNull();
	}
	
	@Test
	public void testDecodeMissingServerIdShouldBeOptional() throws Exception {
		String initialDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FolderCreate>" +
				"<Status>9</Status>" +
				"<SyncKey>2056e98e-be0e-4d1a-8f39-328614a32f3a</SyncKey>" +
				"</FolderCreate>";
		
		FolderCreateResponse response = folderCreateProtocol.decodeResponse(DOMUtils.parse(initialDocument));
		
		assertThat(response.getCollectionId()).isNull();
	}
	
	@Test
	public void testEncodeStatusOK() throws Exception {
		FolderCreateResponse folderCreateResponse = FolderCreateResponse.builder()
				.status(FolderCreateStatus.OK)
				.syncKey(new FolderSyncKey("1234"))
				.collectionId(CollectionId.of("11"))
				.build();
		 
		Document encodedDocument = folderCreateProtocol.encodeResponse(device, folderCreateResponse);

		assertThat(DOMUtils.serialize(encodedDocument)).isEqualTo(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<FolderCreate>" +
			"<Status>1</Status>" +
			"<SyncKey>1234</SyncKey>" +
			"<ServerId>11</ServerId>" +
			"</FolderCreate>");
	}
	
	@Test
	public void testEncodeStatusInvalidSyncKey() throws Exception {
		FolderCreateResponse folderCreateResponse = FolderCreateResponse.builder()
				.status(FolderCreateStatus.OK)
				.syncKey(new FolderSyncKey("1234"))
				.collectionId(CollectionId.of("11"))
				.build();
		
		Document encodedDocument = folderCreateProtocol.encodeResponse(device, folderCreateResponse);
		
		assertThat(DOMUtils.serialize(encodedDocument)).isEqualTo(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<FolderCreate>" +
			"<Status>1</Status>" +
			"<SyncKey>1234</SyncKey>" +
			"<ServerId>11</ServerId>" +
			"</FolderCreate>");
	}
}
