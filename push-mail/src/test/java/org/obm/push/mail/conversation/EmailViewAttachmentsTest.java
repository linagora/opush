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
package org.obm.push.mail.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.mail.mime.BodyParam;
import org.obm.push.mail.mime.BodyParams;
import org.obm.push.mail.mime.ContentType;
import org.obm.push.mail.mime.MimeAddress;
import org.obm.push.mail.mime.MimePart;
import org.obm.push.mail.mime.MimePartImpl;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.base.Optional;


public class EmailViewAttachmentsTest {

	private IMocksControl control;
	
	@Before
	public void setup() {
		control = createControl();
	}
	
	private Optional<String> getDisplayNameOfMimePart(MimePart attachment) {
		return EmailViewAttachments.builder().uid(1l).collectionId(CollectionId.of(15)).selectDisplayName(attachment, 0);
	}
	
	@Test
	public void testDisplayNameWhenNoNameNoContentId() {
		Optional<String> displayName = getDisplayNameOfMimePart(MimePartImpl.builder()
				.contentType("text/plain")
				.build());
		
		assertThat(displayName.isPresent()).isFalse();
	}
	
	@Test
	public void testDisplayNameWhenName() {
		Optional<String> displayName = getDisplayNameOfMimePart(MimePartImpl.builder()
				.contentType("text/plain")
				.bodyParams(BodyParams.builder().add(
						new BodyParam("name", "hello"))
						.build())
				.build());
		
		assertThat(displayName.isPresent()).isTrue();
		assertThat(displayName.get()).isEqualTo("hello");
	}
	
	@Test
	public void testDisplayNameWhenContentId() {
		Optional<String> displayName = getDisplayNameOfMimePart(MimePartImpl.builder()
				.contentType("text/plain")
				.contentId("hello")
				.build());
		
		assertThat(displayName.isPresent()).isTrue();
		assertThat(displayName.get()).isEqualTo("ATT00000");
	}
	
	@Test
	public void testDisplayNameGetNameWhenBoth() {
		Optional<String> displayName = getDisplayNameOfMimePart(MimePartImpl.builder()
				.contentType("text/plain")
				.contentId("hello contentId")
				.bodyParams(BodyParams.builder().add(
						new BodyParam("name", "hello Name"))
						.build())
				.build());
		
		assertThat(displayName.isPresent()).isTrue();
		assertThat(displayName.get()).isEqualTo("hello Name");
	}
	
	@Test
	public void testDisplayNameWhenExtensionMapped() {
		Optional<String> displayName = getDisplayNameOfMimePart(MimePartImpl.builder()
				.contentType("image/jpeg")
				.contentId("hello contentId")
				.bodyParams(BodyParams.builder().add(
						new BodyParam("name", "hello Name"))
						.build())
				.build());
		
		assertThat(displayName.isPresent()).isTrue();
		assertThat(displayName.get()).isEqualTo("hello Name");
	}
	
	@Test(expected=IllegalStateException.class)
	public void buildShouldThrowWhenNoUid() {
		EmailViewAttachments.builder().build();
	}
	
	@Test(expected=IllegalStateException.class)
	public void buildShouldThrowWhenNoCollectionId() {
		EmailViewAttachments.builder().uid(1l).build();
	}
	
	@Test
	public void builderShouldBuildWhenNoAttachment() {
		EmailViewAttachments emailViewAttachments = EmailViewAttachments.builder()
				.uid(789l)
				.collectionId(CollectionId.of(2))
				.build();
		
		assertThat(emailViewAttachments.getEmailViewAttachments()).isEmpty();
	}
	
	@Test
	public void builderShouldBuildWhenAddingASingleAttachment() {
		MimePart mimePart = control.createMock(MimePart.class);
		ContentType contentType = control.createMock(ContentType.class);
		String name = "name";
		int size = 123;
		
		mockAttachmentMimePart(mimePart, contentType, name, size, "1");
		
		control.replay();
		EmailViewAttachments emailViewAttachments = EmailViewAttachments.builder()
				.uid(789l)
				.collectionId(CollectionId.of(2))
				.addAttachment(mimePart)
				.build();
		control.verify();
		
		assertThat(emailViewAttachments.getEmailViewAttachments()).containsOnly(
				EmailViewAttachment.builder()
					.id("at_789_0")
					.displayName(name)
					.fileReference("2_789_1_dGV4dC9wbGFpbg==_OGJpdA==")
					.size(size)
					.contentType(contentType)
					.inline(true)
					.build());
	}
	
	@Test
	public void builderShouldBuildWhenAddingTwoAttachments() {
		MimePart mimePart = control.createMock(MimePart.class);
		ContentType contentType = control.createMock(ContentType.class);
		String name = "name";
		int size = 123;
		
		mockAttachmentMimePart(mimePart, contentType, name, size, "1");
		
		MimePart mimePart2 = control.createMock(MimePart.class);
		ContentType contentType2 = control.createMock(ContentType.class);
		String name2 = "name2";
		int size2 = 456;
		
		mockAttachmentMimePart(mimePart2, contentType2, name2, size2, "2");
		
		control.replay();
		EmailViewAttachments emailViewAttachments = EmailViewAttachments.builder()
				.uid(789l)
				.collectionId(CollectionId.of(2))
				.addAttachment(mimePart)
				.addAttachment(mimePart2)
				.build();
		control.verify();
		
		assertThat(emailViewAttachments.getEmailViewAttachments()).containsOnly(
				EmailViewAttachment.builder()
					.id("at_789_0")
					.displayName(name)
					.fileReference("2_789_1_dGV4dC9wbGFpbg==_OGJpdA==")
					.size(size)
					.contentType(contentType)
					.inline(true)
					.build(),
				EmailViewAttachment.builder()
					.id("at_789_1")
					.displayName(name2)
					.fileReference("2_789_2_dGV4dC9wbGFpbg==_OGJpdA==")
					.size(size2)
					.contentType(contentType2)
					.inline(true)
					.build());
	}
	
	@Test
	public void builderShouldBuildWhenAddingNestedAttachment() {
		MimePart mimePart = control.createMock(MimePart.class);
		ContentType contentType = control.createMock(ContentType.class);
		int size = 123;
		
		mockNestedMimePart(mimePart, contentType, size, "1");
		
		control.replay();
		EmailViewAttachments emailViewAttachments = EmailViewAttachments.builder()
				.uid(789l)
				.collectionId(CollectionId.of(2))
				.addAttachment(mimePart)
				.build();
		control.verify();
		
		assertThat(emailViewAttachments.getEmailViewAttachments()).containsOnly(
				EmailViewAttachment.builder()
					.id("at_789_0")
					.displayName("ForwardedMessage.eml")
					.fileReference("2_789_1_dGV4dC9wbGFpbg==_OGJpdA==")
					.size(size)
					.contentType(contentType)
					.inline(true)
					.build());
	}
	
	@Test
	public void builderShouldBuildWhenAddingOneAttachmentAndOneNested() {
		MimePart mimePart = control.createMock(MimePart.class);
		ContentType contentType = control.createMock(ContentType.class);
		String name = "name";
		int size = 123;
		
		mockAttachmentMimePart(mimePart, contentType, name, size, "1");
		
		MimePart mimePart2 = control.createMock(MimePart.class);
		ContentType contentType2 = control.createMock(ContentType.class);
		int size2 = 456;
		
		mockNestedMimePart(mimePart2, contentType2, size2, "2");
		
		control.replay();
		EmailViewAttachments emailViewAttachments = EmailViewAttachments.builder()
				.uid(789l)
				.collectionId(CollectionId.of(2))
				.addAttachment(mimePart)
				.addAttachment(mimePart2)
				.build();
		control.verify();
		
		assertThat(emailViewAttachments.getEmailViewAttachments()).containsOnly(
				EmailViewAttachment.builder()
					.id("at_789_0")
					.displayName(name)
					.fileReference("2_789_1_dGV4dC9wbGFpbg==_OGJpdA==")
					.size(size)
					.contentType(contentType)
					.inline(true)
					.build(),
				EmailViewAttachment.builder()
					.id("at_789_1")
					.displayName("ForwardedMessage.eml")
					.fileReference("2_789_2_dGV4dC9wbGFpbg==_OGJpdA==")
					.size(size2)
					.contentType(contentType2)
					.inline(true)
					.build());
	}

	private void mockNestedMimePart(MimePart mimePart, ContentType contentType, int size, String mimePartAddress) {
		expect(mimePart.getName())
			.andReturn(null);
		expect(mimePart.isNested())
			.andReturn(true);
		mockMimePart(mimePart, contentType, size, mimePartAddress);
	}

	private void mockAttachmentMimePart(MimePart mimePart, ContentType contentType, String name, int size, String mimePartAddress) {
		expect(mimePart.getName())
			.andReturn(name);
		mockMimePart(mimePart, contentType, size, mimePartAddress);
	}

	private void mockMimePart(MimePart mimePart, ContentType contentType, int size, String mimePartAddress) {
		expect(mimePart.getSize())
			.andReturn(size);
		
		MimeAddress mimeAddress = control.createMock(MimeAddress.class);
		expect(mimeAddress.getAddress())
			.andReturn(mimePartAddress);
		expect(mimePart.getAddress())
			.andReturn(mimeAddress);
		expect(mimePart.getFullMimeType())
			.andReturn("text/plain");
		expect(mimePart.getContentTransfertEncoding())
			.andReturn("8bit");
		
		expect(mimePart.getContentType())
			.andReturn(contentType);
		expect(mimePart.getContentId())
			.andReturn("content");
		expect(mimePart.getContentLocation())
			.andReturn("location");
		expect(mimePart.isInline())
			.andReturn(true);
	}
}
