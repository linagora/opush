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
package org.obm.push.handler;

import org.obm.push.backend.FolderSnapshotService;
import org.obm.push.backend.IContinuation;
import org.obm.push.backend.IHierarchyExporter;
import org.obm.push.bean.FolderCreateStatus;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.FolderCreateRequest;
import org.obm.push.bean.change.hierarchy.FolderCreateResponse;
import org.obm.push.bean.change.hierarchy.FolderSnapshot;
import org.obm.push.exception.activesync.BackendNotSupportedException;
import org.obm.push.exception.activesync.InvalidFolderSyncKeyException;
import org.obm.push.exception.activesync.TimeoutException;
import org.obm.push.impl.DOMDumper;
import org.obm.push.impl.Responder;
import org.obm.push.protocol.FolderCreateProtocol;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.request.ActiveSyncRequest;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.state.FolderSyncKeyFactory;
import org.obm.push.wbxml.WBXMLTools;
import org.w3c.dom.Document;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FolderCreateHandler extends WbxmlRequestHandler {

	private final IHierarchyExporter hierarchyExporter;
	private final FolderCreateProtocol protocol;
	private final FolderSyncKeyFactory folderSyncKeyFactory;
	private final FolderSnapshotService folderSnapshotService;
	
	@Inject
	protected FolderCreateHandler(IHierarchyExporter hierarchyExporter, 
			FolderCreateProtocol protocol,
			WBXMLTools wbxmlTools, DOMDumper domDumper,
			FolderSyncKeyFactory folderSyncKeyFactory,
			FolderSnapshotService folderSnapshotService) {
		
		super(wbxmlTools, domDumper);
		
		this.hierarchyExporter = hierarchyExporter;
		this.protocol = protocol;
		this.folderSyncKeyFactory = folderSyncKeyFactory;
		this.folderSnapshotService = folderSnapshotService;
	}

	@Override
	public void process(IContinuation continuation, UserDataRequest udr,
			Document doc, ActiveSyncRequest request, Responder responder) {
		
		try {
			FolderCreateRequest folderCreateRequest = protocol.decodeRequest(doc);
			
			FolderCreateResponse folderCreateResponse = createFolder(udr, folderCreateRequest);
			Document docToReturn = protocol.encodeResponse(udr.getDevice(), folderCreateResponse);
			sendResponse(responder, docToReturn);
			
		} catch (InvalidFolderSyncKeyException e) {
			logger.warn(e.getMessage(), e);
			sendResponse(responder, protocol.encodeErrorResponse(FolderCreateStatus.INVALID_SYNC_KEY));
		} catch (TimeoutException e) {
			sendError(responder, FolderCreateStatus.REQUEST_TIMED_OUT, e);
		} catch (BackendNotSupportedException e) {
			//ALREADY_EXISTS is the sole error well supported by the devices
			sendError(responder, FolderCreateStatus.ALREADY_EXISTS, e);
		} catch (Exception e) {
			sendError(responder, FolderCreateStatus.SERVER_ERROR, e);
		}
	}

	private void sendResponse(Responder responder, Document ret) {
		responder.sendWBXMLResponse("FolderHierarchy", ret);
	}
	
	private void sendError(Responder responder, FolderCreateStatus status, Exception exception) {
		logger.error(exception.getMessage(), exception);
		sendResponse(responder, protocol.encodeErrorResponse(status));
	}
	
	private FolderCreateResponse createFolder(UserDataRequest udr, 
			FolderCreateRequest folderCreateRequest) throws Exception {
		
		FolderSyncKey outgoingSyncKey = folderSyncKeyFactory.randomSyncKey();
		FolderSnapshot knownSnapshot = folderSnapshotService
				.findFolderSnapshot(udr, folderCreateRequest.getSyncKey());
		
		BackendId backendId = hierarchyExporter.createFolder(udr, folderCreateRequest);
		
		CollectionId collectionId = snapshotWithNewFolder(udr,
				folderCreateRequest, outgoingSyncKey, knownSnapshot, backendId);
		
		return FolderCreateResponse.builder()
				.status(FolderCreateStatus.OK)
				.collectionId(collectionId)
				.syncKey(outgoingSyncKey)
				.build();
	}

	private CollectionId snapshotWithNewFolder(UserDataRequest udr,
			FolderCreateRequest folderCreateRequest,
			FolderSyncKey outgoingSyncKey, FolderSnapshot knownSnapshot,
			BackendId backendId) throws Exception {
		
		BackendFolder backendFolder = BackendFolder.builder()
				.backendId(backendId)
				.displayName(folderCreateRequest.getFolderDisplayName())
				.folderType(folderCreateRequest.getFolderType())
				.parentId(Optional.<BackendId>absent())
				.build();
		
		return folderSnapshotService.createSnapshotAddingFolder(
				udr, outgoingSyncKey, knownSnapshot, backendFolder);
	}
}
