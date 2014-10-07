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

import java.util.List;

import org.obm.push.mail.AttachmentHelper;
import org.obm.push.mail.mime.MimePart;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;


public class EmailViewAttachments {

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private static final String NESTED_DISPLAY_NAME = "ForwardedMessage.eml";
		private Long uid;
		private CollectionId collectionId;
		private int attachmentId;
		private ImmutableList.Builder<MimePart> attachments;
		
		private Builder() {
			attachmentId = 0;
			attachments = ImmutableList.builder();
		}
		
		public Builder uid(Long uid) {
			this.uid = uid;
			return this;
		}
		
		public Builder collectionId(CollectionId collectionId) {
			this.collectionId = collectionId;
			return this;
		}

		public Builder addAttachment(MimePart mimePart) {
			attachments.add(mimePart);
			return this;
		}
		
		public EmailViewAttachments build() {
			Preconditions.checkState(uid != null, "uid is required");
			Preconditions.checkState(collectionId != null, "collectionId is required");
			return new EmailViewAttachments(convertAttachments());
		}

		private ImmutableList<EmailViewAttachment> convertAttachments() {
			ImmutableList.Builder<EmailViewAttachment> builder = ImmutableList.builder();
			for (MimePart mimePart : attachments.build()) {
				Optional<String> displayName = selectDisplayName(mimePart, attachmentId);
				if (displayName.isPresent()) {
					builder.add(attachment(displayName.get(), mimePart));
				}
			}
			return builder.build();
		}

		@VisibleForTesting Optional<String> selectDisplayName(MimePart attachment, int attachmentId) {
			String partName = attachment.getName();
			if (!Strings.isNullOrEmpty(partName)) {
				return Optional.of(partName);
			}
			if (attachment.isNested()) {
				return Optional.of(NESTED_DISPLAY_NAME);
			}
			String contentId = attachment.getContentId();
			if (Strings.isNullOrEmpty(contentId)) {
				return Optional.absent();
			}
			return Optional.of(String.format("ATT%05d%s", attachmentId, Strings.nullToEmpty(attachment.getAttachmentExtension())));
		}

		private EmailViewAttachment attachment(String displayName, MimePart mimePart) {
			return EmailViewAttachment.builder()
					.id(id())
					.displayName(displayName)
					.fileReference(fileReference(mimePart))
					.size(mimePart.getSize())
					.contentType(mimePart.getContentType())
					.contentId(mimePart.getContentId())
					.contentLocation(mimePart.getContentLocation())
					.inline(mimePart.isInline())
					.build();
		}
		
		private String id() {
			return "at_" + uid + "_" + attachmentId++;
		}

		private String fileReference(MimePart mimePart) {
			return AttachmentHelper.getAttachmentId(collectionId, String.valueOf(uid), 
					mimePart.getAddress().getAddress(), mimePart.getFullMimeType(), mimePart.getContentTransfertEncoding());
		}
	}
	
	private final List<EmailViewAttachment> attachments;
	
	private EmailViewAttachments(List<EmailViewAttachment> attachments) {
		this.attachments = attachments;
	}
	
	public List<EmailViewAttachment> getEmailViewAttachments() {
		return attachments;
	}
	
	@Override
	public final int hashCode(){
		return Objects.hashCode(attachments);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof EmailViewAttachments) {
			EmailViewAttachments that = (EmailViewAttachments) object;
			return Objects.equal(this.attachments, that.attachments);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("attachments", attachments)
			.toString();
	}
}
