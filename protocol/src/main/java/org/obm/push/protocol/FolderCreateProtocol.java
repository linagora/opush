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

import static org.obm.push.protocol.data.FolderCreateRequestFields.DISPLAY_NAME;
import static org.obm.push.protocol.data.FolderCreateRequestFields.PARENT_ID;
import static org.obm.push.protocol.data.FolderCreateRequestFields.SYNC_KEY;
import static org.obm.push.protocol.data.FolderCreateRequestFields.TYPE;
import static org.obm.push.protocol.data.FolderCreateResponseFields.SERVER_ID;
import static org.obm.push.protocol.data.FolderCreateResponseFields.STATUS;

import org.obm.push.bean.Device;
import org.obm.push.bean.FolderCreateStatus;
import org.obm.push.bean.FolderType;
import org.obm.push.bean.change.hierarchy.FolderCreateRequest;
import org.obm.push.bean.change.hierarchy.FolderCreateResponse;
import org.obm.push.exception.activesync.NoDocumentException;
import org.obm.push.exception.activesync.ProtocolException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.data.ActiveSyncDecoder;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Preconditions;

public class FolderCreateProtocol extends ActiveSyncDecoder implements ActiveSyncProtocol<FolderCreateRequest, FolderCreateResponse> {
	
	public Document encodeErrorResponse(FolderCreateStatus status) {
		Document ret = DOMUtils.createDoc(null, "FolderCreate");
		DOMUtils.createElementAndText(ret.getDocumentElement(), "Status", status.asSpecificationValue());
		return ret;
	}
	
	@Override
	public FolderCreateRequest decodeRequest(Document requestDocument) throws NoDocumentException {
		if (requestDocument == null) {
			throw new NoDocumentException("Document of FolderCreate request is null.");
		}
		Element root = requestDocument.getDocumentElement();
		
		FolderSyncKey syncKey = new FolderSyncKey(requiredStringFieldValue(root, SYNC_KEY));
		CollectionId folderParentId = CollectionId.of(requiredIntegerFieldValue(root, PARENT_ID));
		String folderDisplayName = requiredStringFieldValue(root, DISPLAY_NAME);
		FolderType folderType = FolderType.fromSpecificationValue(requiredStringFieldValue(root, TYPE));
		return FolderCreateRequest.builder()
			.folderSyncKey(syncKey)
			.folderParentId(folderParentId)
			.folderDisplayName(folderDisplayName)
			.folderType(folderType)
			.build();
	}

	@Override
	public Document encodeRequest(FolderCreateRequest folderCreateRequest) throws ProtocolException {
		Preconditions.checkArgument(folderCreateRequest != null, "FolderCreateRequest is null.");

		Document folderCreateRequestDocument = DOMUtils.createDoc(null, "FolderCreate");
		Element root = folderCreateRequestDocument.getDocumentElement();

		DOMUtils.createElementAndText(root, SYNC_KEY.getName(), folderCreateRequest.getSyncKey().asString());
		DOMUtils.createElementAndText(root, PARENT_ID.getName(), folderCreateRequest.getFolderParentId().asString());
		DOMUtils.createElementAndText(root, DISPLAY_NAME.getName(), folderCreateRequest.getFolderDisplayName());
		DOMUtils.createElementAndText(root, TYPE.getName(), folderCreateRequest.getFolderType().asSpecificationValue());
		
		return folderCreateRequestDocument;
	}
	
	@Override
	public FolderCreateResponse decodeResponse(Document responseDocument) throws NoDocumentException {
		if (responseDocument == null) {
			throw new NoDocumentException("Document of FolderCreate response is null.");
		}

		Element root = responseDocument.getDocumentElement();
		
		FolderCreateResponse.Builder builder = FolderCreateResponse.builder();
		
		String syncKey = uniqueStringFieldValue(root, SYNC_KEY);
		if (syncKey != null) {
			builder.syncKey(new FolderSyncKey(syncKey));
		} 
		
		String collectionId = uniqueStringFieldValue(root, SERVER_ID);
		if (collectionId != null) {
			builder.collectionId(CollectionId.of(collectionId));
		}

		String status = requiredStringFieldValue(root, STATUS);
		builder.status(FolderCreateStatus.fromSpecificationValue(status));
		
		return builder.build();
	}

	@Override
	public Document encodeResponse(Device device, FolderCreateResponse folderCreateResponse) 
			throws ProtocolException {
		Preconditions.checkArgument(folderCreateResponse != null, "FolderCreateResponse is null.");

		Document folderCreateResponseDocument = DOMUtils.createDoc(null, "FolderCreate");
		Element root = folderCreateResponseDocument.getDocumentElement();
		
		DOMUtils.createElementAndText(root, STATUS.getName(), folderCreateResponse.getStatus().asSpecificationValue());
		DOMUtils.createElementAndText(root, SYNC_KEY.getName(), folderCreateResponse.getSyncKey().asString());
		DOMUtils.createElementAndText(root, SERVER_ID.getName(), folderCreateResponse.getCollectionId().asString());

		return folderCreateResponseDocument;
	}
}
