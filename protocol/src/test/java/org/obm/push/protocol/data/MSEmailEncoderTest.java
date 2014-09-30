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

package org.obm.push.protocol.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createMock;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.MSAttachement;
import org.obm.push.bean.MSEmailBodyType;
import org.obm.push.bean.MSEmailHeader;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.bean.ms.MSEmailBody;
import org.obm.push.utils.DOMUtils;
import org.obm.push.utils.IntEncoder;
import org.obm.push.utils.SerializableInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;


public class MSEmailEncoderTest {
	
	private MSEmailEncoder msEmailEncoder;

	@Before
	public void setup(){
		IntEncoder intEncoder = createMock(IntEncoder.class);
		TimeZoneEncoder timeZoneEncoder = createMock(TimeZoneEncoder.class);
		TimeZoneConverter timeZoneConverter = createMock(TimeZoneConverter.class);
		
		msEmailEncoder = new MSEmailEncoder(intEncoder, timeZoneEncoder, timeZoneConverter);
	}
	
	@Test
	public void testBodyTagsOrder() throws IOException, TransformerException {
		Document reply = DOMUtils.createDoc(null, "Sync");
		Element root = reply.getDocumentElement();
		
		msEmailEncoder.encode(root, applicationData("text", MSEmailBodyType.PlainText));
		String result = DOMUtils.serialize(reply);
		
		String expectedTagsOrder = "<AirSyncBase:Type>1</AirSyncBase:Type>" +
				"<AirSyncBase:Truncated>1</AirSyncBase:Truncated>" +
				"<AirSyncBase:EstimatedDataSize>10</AirSyncBase:EstimatedDataSize>" +
				"<AirSyncBase:Data>";
		assertThat(result).contains(expectedTagsOrder);
	}

	private MSEmail applicationData(String message, MSEmailBodyType emailBodyType) {
		return MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(new SerializableInputStream(new ByteArrayInputStream(message.getBytes())))
					.bodyType(emailBodyType)
					.estimatedDataSize(10)
					.charset(Charsets.UTF_8)
					.truncated(true)
					.build())
			.build();
	}
	
	@Test
	public void testEncodeDataPlainText() throws Exception {
		Document reply = DOMUtils.createDoc(null, "Sync");
		Element root = reply.getDocumentElement();
		
		MSEmailBody emailBody = MSEmailBody.builder()
			.mimeData(new SerializableInputStream(new ByteArrayInputStream("text".getBytes())))
			.bodyType(MSEmailBodyType.PlainText)
			.estimatedDataSize(10)
			.charset(Charsets.UTF_8)
			.truncated(true)
			.build();
		
		msEmailEncoder.encodeData(emailBody, root);
		String result = DOMUtils.serialize(reply);
		
		String expectedData = "<AirSyncBase:Data>" +
				"<![CDATA[text]]>" +
				"</AirSyncBase:Data>";
		assertThat(result).contains(expectedData);
		
	}
	
	@Test
	public void testEncodeDataHtml() throws Exception {
		Document reply = DOMUtils.createDoc(null, "Sync");
		Element root = reply.getDocumentElement();
		
		MSEmailBody emailBody = MSEmailBody.builder()
			.mimeData(new SerializableInputStream(new ByteArrayInputStream("<html></html>".getBytes())))
			.bodyType(MSEmailBodyType.HTML)
			.estimatedDataSize(10)
			.charset(Charsets.UTF_8)
			.truncated(true)
			.build();
		
		msEmailEncoder.encodeData(emailBody, root);
		String result = DOMUtils.serialize(reply);
		
		String expectedData = "<AirSyncBase:Data>" +
				"&lt;html&gt;&lt;/html&gt;" +
				"</AirSyncBase:Data>";
		assertThat(result).contains(expectedData);
		
	}
	
	@Test
	public void attachmentsShouldBeBeforeBody() throws IOException, TransformerException {
		Document reply = DOMUtils.createDoc(null, "Sync");
		Element root = reply.getDocumentElement();
		
		MSAttachement msAttachement = new MSAttachement();
		msAttachement.setDisplayName("displayName");
		msAttachement.setFileReference("fileReference");
		msAttachement.setEstimatedDataSize(123);
		MSEmail msEmail = MSEmail.builder()
			.header(MSEmailHeader.builder().build())
			.body(MSEmailBody.builder()
					.mimeData(new SerializableInputStream(new ByteArrayInputStream("text".getBytes())))
					.bodyType(MSEmailBodyType.PlainText)
					.estimatedDataSize(10)
					.charset(Charsets.UTF_8)
					.truncated(true)
					.build())
			.attachements(ImmutableSet.of(msAttachement))
			.build();
		
		msEmailEncoder.encode(root, msEmail);
		String result = DOMUtils.serialize(reply);
		
		String expectedTagsOrder = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
				"<Sync>" +
					"<Email:From>\"Empty From\" &lt;o-push@linagora.com&gt; </Email:From>" +
					"<Email:Importance>1</Email:Importance>" +
					"<Email:Read>0</Email:Read>" +
					"<AirSyncBase:Attachments>" +
						"<AirSyncBase:Attachment>" +
						"<AirSyncBase:DisplayName>displayName</AirSyncBase:DisplayName>" +
						"<AirSyncBase:FileReference>fileReference</AirSyncBase:FileReference>" +
						"<AirSyncBase:Method>1</AirSyncBase:Method>" +
						"<AirSyncBase:EstimatedDataSize>123</AirSyncBase:EstimatedDataSize>" +
						"<AirSyncBase:IsInline>0</AirSyncBase:IsInline>" +
						"</AirSyncBase:Attachment>" +
					"</AirSyncBase:Attachments>" +
					"<AirSyncBase:Body>" +
						"<AirSyncBase:Type>1</AirSyncBase:Type>" +
						"<AirSyncBase:Truncated>1</AirSyncBase:Truncated>" +
						"<AirSyncBase:EstimatedDataSize>10</AirSyncBase:EstimatedDataSize>" +
						"<AirSyncBase:Data><![CDATA[text]]></AirSyncBase:Data>" +
					"</AirSyncBase:Body>" +
					"<Email:MessageClass>IPM.Note</Email:MessageClass>" +
					"<Email:ContentClass>urn:content-classes:message</Email:ContentClass>" +
					"<Email:InternetCPID>65001</Email:InternetCPID>" +
					"<AirSyncBase:NativeBodyType>1</AirSyncBase:NativeBodyType>" +
				"</Sync>";
		assertThat(result).isEqualTo(expectedTagsOrder);
	}
}
