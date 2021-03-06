/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2016 Linagora
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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.naming.NoPermissionException;

import org.eclipse.jetty.continuation.ContinuationThrowable;
import org.obm.push.ContinuationService;
import org.obm.push.SummaryLoggerService;
import org.obm.push.backend.CollectionChangeListener;
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.IBackend;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.backend.IContinuation;
import org.obm.push.backend.IListenerRegistration;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.Device;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.Sync;
import org.obm.push.bean.SyncCollectionCommandRequest;
import org.obm.push.bean.SyncCollectionCommandsResponse;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncCollectionResponsesResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.client.SyncClientCommands;
import org.obm.push.bean.change.client.SyncClientCommands.Add;
import org.obm.push.bean.change.client.SyncClientCommands.Deletion;
import org.obm.push.bean.change.client.SyncClientCommands.Fetch;
import org.obm.push.bean.change.client.SyncClientCommands.Update;
import org.obm.push.bean.change.hierarchy.Folder;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.exception.CollectionPathException;
import org.obm.push.exception.ConversionException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.UnexpectedObmSyncServerException;
import org.obm.push.exception.UnsupportedBackendFunctionException;
import org.obm.push.exception.WaitIntervalOutOfRangeException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.HierarchyChangedException;
import org.obm.push.exception.activesync.InvalidServerId;
import org.obm.push.exception.activesync.InvalidSyncKeyException;
import org.obm.push.exception.activesync.ItemNotFoundException;
import org.obm.push.exception.activesync.NoDocumentException;
import org.obm.push.exception.activesync.PartialException;
import org.obm.push.exception.activesync.ProcessingEmailException;
import org.obm.push.exception.activesync.ProtocolException;
import org.obm.push.exception.activesync.ServerErrorException;
import org.obm.push.exception.activesync.TimeoutException;
import org.obm.push.impl.DOMDumper;
import org.obm.push.impl.Responder;
import org.obm.push.mail.exception.FilterTypeChangedException;
import org.obm.push.protocol.SyncProtocol;
import org.obm.push.protocol.bean.AnalysedSyncRequest;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.SyncRequest;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.SyncAnalyser;
import org.obm.push.protocol.request.ActiveSyncRequest;
import org.obm.push.service.DateService;
import org.obm.push.service.FolderSnapshotDao;
import org.obm.push.state.StateMachine;
import org.obm.push.state.SyncKeyFactory;
import org.obm.push.store.ItemTrackingDao;
import org.obm.push.store.MonitoredCollectionDao;
import org.obm.push.wbxml.WBXMLTools;
import org.w3c.dom.Document;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class SyncHandler extends WbxmlRequestHandler implements IContinuationHandler {

	private final SyncProtocol syncProtocol;
	private final MonitoredCollectionDao monitoredCollectionService;
	private final ItemTrackingDao itemTrackingDao;
	private final ContinuationService continuationService;
	private final boolean enablePush;
	private final SyncKeyFactory syncKeyFactory;
	private final DateService dateService;
	private final SyncAnalyser syncAnalyser;
	private final SummaryLoggerService summaryLoggerService;
	private final IContentsImporter contentsImporter;
	private final IContentsExporter contentsExporter;
	private final IBackend backend;
	private final StateMachine stMachine;
	private final FolderSnapshotDao folderSnapshotDao;

	@Inject SyncHandler(IBackend backend, IContentsImporter contentsImporter, IContentsExporter contentsExporter,
			StateMachine stMachine,
			MonitoredCollectionDao monitoredCollectionService, SyncProtocol syncProtocol,
			ItemTrackingDao itemTrackingDao,
			FolderSnapshotDao folderSnapshotDao,
			WBXMLTools wbxmlTools, DOMDumper domDumper,
			ContinuationService continuationService,
			@Named("enable-push") boolean enablePush,
			SyncKeyFactory syncKeyFactory,
			DateService dateService, SyncAnalyser syncAnalyser,
			SummaryLoggerService summaryLoggerService) {
		
		super(wbxmlTools, domDumper);

		this.backend = backend;
		this.contentsImporter = contentsImporter;
		this.contentsExporter = contentsExporter;
		this.stMachine = stMachine;
		this.monitoredCollectionService = monitoredCollectionService;
		this.syncProtocol = syncProtocol;
		this.itemTrackingDao = itemTrackingDao;
		this.folderSnapshotDao = folderSnapshotDao;
		this.continuationService = continuationService;
		this.enablePush = enablePush;
		this.syncKeyFactory = syncKeyFactory;
		this.dateService = dateService;
		this.syncAnalyser = syncAnalyser;
		this.summaryLoggerService = summaryLoggerService;
	}

	@Override
	public void process(IContinuation continuation, UserDataRequest udr, Document doc, ActiveSyncRequest request, Responder responder) {
		try {
			SyncRequest syncRequest = syncProtocol.decodeRequest(doc);
			AnalysedSyncRequest analyzedSyncRequest = analyzeRequest(udr, syncRequest);
			
			continuationService.cancel(udr.getUser(), udr.getDevice());
			if (analyzedSyncRequest.getSync().getWaitInSecond() > 0) {
				registerWaitingSync(continuation, udr, analyzedSyncRequest.getSync());
			} else {
				SyncResponse syncResponse = doTheJob(udr, analyzedSyncRequest.getSync().getCollections(), continuation);
				sendResponse(responder, syncProtocol.encodeResponse(udr.getDevice(), syncResponse));
			}
		} catch (InvalidServerId e) {
			sendError(udr.getDevice(), responder, SyncStatus.PROTOCOL_ERROR, continuation, e);
		} catch (ProtocolException convExpt) {
			sendError(udr.getDevice(), responder, SyncStatus.PROTOCOL_ERROR, continuation, convExpt);
		} catch (PartialException pe) {
			sendError(udr.getDevice(), responder, SyncStatus.PARTIAL_REQUEST, continuation, pe);
		} catch (CollectionNotFoundException ce) {
			sendError(udr.getDevice(), responder, SyncStatus.OBJECT_NOT_FOUND, continuation, ce);
		} catch (ContinuationThrowable e) {
			throw e;
		} catch (NoDocumentException e) {
			sendError(udr.getDevice(), responder, SyncStatus.PARTIAL_REQUEST, continuation, e);
		} catch (WaitIntervalOutOfRangeException e) {
			sendResponse(responder, syncProtocol.encodeResponse());
		} catch (WaitSyncFolderLimitException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR.asSpecificationValue(), continuation);
		} catch (DaoException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (UnexpectedObmSyncServerException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (ProcessingEmailException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (CollectionPathException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (ConversionException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (UnsupportedBackendFunctionException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (ServerErrorException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (HierarchyChangedException e) {
			sendError(udr.getDevice(), responder, SyncStatus.HIERARCHY_CHANGED, continuation, e);
		} catch (ItemNotFoundException e) {
			sendError(udr.getDevice(), responder, SyncStatus.CONVERSATION_ERROR_OR_INVALID_ITEM, continuation, e);
		} catch (InvalidSyncKeyException e) {
			sendError(udr.getDevice(), responder, SyncStatus.INVALID_SYNC_KEY, continuation, e);
		} catch (TimeoutException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (RuntimeException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} 
	}

	public AnalysedSyncRequest analyzeRequest(UserDataRequest userDataRequest, SyncRequest syncRequest) 
			throws PartialException, ProtocolException, DaoException, CollectionPathException {
		Preconditions.checkNotNull(userDataRequest);
		Preconditions.checkNotNull(syncRequest);
		
		Sync analyseSync = syncAnalyser.analyseSync(userDataRequest, syncRequest);
		summaryLoggerService.logIncomingSync(analyseSync);
		return new AnalysedSyncRequest( analyseSync );
	}

	private void sendResponse(Responder responder, Document document) {
		responder.sendWBXMLResponse("AirSync", document);
	}
	
	private void registerWaitingSync(IContinuation continuation, UserDataRequest udr, Sync syncRequest) 
			throws CollectionNotFoundException, WaitIntervalOutOfRangeException, DaoException, CollectionPathException, WaitSyncFolderLimitException {
		
		if (!enablePush) {
			throw new WaitSyncFolderLimitException(0);
		}
		
		if (syncRequest.getWaitInSecond() > 3540) {
			throw new WaitIntervalOutOfRangeException();
		}
		
		for (AnalysedSyncCollection sc: syncRequest.getCollections()) {
			Folder folder = folderSnapshotDao.get(udr.getUser(), udr.getDevice(), sc.getCollectionId());
			PIMDataType dataClass = folder.getFolderType().getPIMDataType();
			if (dataClass == PIMDataType.EMAIL) {
				backend.startEmailMonitoring(udr, sc.getCollectionId());
				break;
			}
		}
		
		continuation.setLastContinuationHandler(this);
		monitoredCollectionService.put(udr.getUser(), udr.getDevice(), syncRequest.getCollections());
		CollectionChangeListener l = new CollectionChangeListener(udr, continuation, syncRequest.getCollections());
		IListenerRegistration reg = backend.addChangeListener(l);
		continuation.setListenerRegistration(reg);
		continuation.setCollectionChangeListener(l);
		
		continuationService.suspend(udr, continuation, syncRequest.getWaitInSecond(), SyncStatus.NEED_RETRY.asSpecificationValue());
	}

	private Date doUpdates(UserDataRequest udr, AnalysedSyncCollection request, SyncCollectionResponse.Builder responseBuilder, ItemSyncState syncState, SyncKey newSyncKey) 
			throws DaoException, CollectionNotFoundException, UnexpectedObmSyncServerException, ProcessingEmailException, 
				ConversionException, FilterTypeChangedException, HierarchyChangedException, InvalidServerId, UnsupportedBackendFunctionException {

		SyncClientCommands clientCommands = processClientModifications(udr, request, syncState, newSyncKey);
		DataDelta serverDelta = retreiveServerModifications(udr, request, syncState, newSyncKey, clientCommands);
		
		responseBuilder
			.commands(SyncCollectionCommandsResponse.builder()
					.changes(identifyNewItems(serverDelta.getChanges(), syncState))
					.deletions(serverDelta.getDeletions())
					.build())
			.responses(SyncCollectionResponsesResponse.from(clientCommands))
			.moreAvailable(serverDelta.hasMoreAvailable());
		
		return serverDelta.getSyncDate();
	}

	private DataDelta retreiveServerModifications(UserDataRequest udr, AnalysedSyncCollection request, ItemSyncState syncState,
			SyncKey newSyncKey, SyncClientCommands clientCommands) {

		if (clientCommands.getFetches().isEmpty()) {
			return filterAddByClient(contentsExporter.getChanged(udr, syncState, request, newSyncKey), clientCommands.getAdds());
		}
		return DataDelta.newEmptyDelta(syncState.getSyncDate(), newSyncKey);
	}

	private DataDelta filterAddByClient(DataDelta dataDelta, final List<Add> adds) {
		return DataDelta.builder()
			.syncDate(dataDelta.getSyncDate())
			.syncKey(dataDelta.getSyncKey())
			.moreAvailable(dataDelta.hasMoreAvailable())
			.deletions(dataDelta.getDeletions())
			.changes(FluentIterable.from(dataDelta.getChanges())
						.filter(new Predicate<ItemChange>() {

							@Override
							public boolean apply(ItemChange itemChange) {
								for (Add add : adds) {
									if (itemChange.getServerId().equals(add.getServerId())) {
										return false;
									}
								}
								return true;
							}
						}).toList())
			.build();
	}

	private boolean isDataTypeKnown(PIMDataType dataType) {
		return dataType != PIMDataType.UNKNOWN;
	}

	@VisibleForTesting SyncClientCommands processClientModifications(UserDataRequest udr, AnalysedSyncCollection collection,
			ItemSyncState syncState, SyncKey newSyncKey)
				throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException,
				ProcessingEmailException, UnsupportedBackendFunctionException, ConversionException, HierarchyChangedException {

		SyncClientCommands.Builder clientCommandsBuilder = SyncClientCommands.builder();
		
		for (SyncCollectionCommandRequest change: collection.getCommands()) {
			try {
				switch (change.getType()) {
				case FETCH:
					clientCommandsBuilder.putFetch(fetchServerItem(udr, collection, change, syncState, newSyncKey));
					break;
				case MODIFY:
				case CHANGE:
					clientCommandsBuilder.putUpdate(updateServerItem(udr, collection, change));
					break;
				case DELETE:
					clientCommandsBuilder.putDeletion(deleteServerItem(udr, collection, change));
					break;
				case ADD:
					clientCommandsBuilder.putAdd(addServerItem(udr, collection, change));
					break;
				}
			} catch (NoPermissionException e) {
				logger.warn("Client is not allowed to perform the command: {}", change);
			}
		}
		return clientCommandsBuilder.build();
	}

	private Update updateServerItem(UserDataRequest udr, AnalysedSyncCollection collection, SyncCollectionCommandRequest change) 
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException,
			ProcessingEmailException, ConversionException, HierarchyChangedException, NoPermissionException {

		try {
			return new SyncClientCommands.Update(contentsImporter.importMessageChange(
						udr, collection.getCollectionId(), change.getServerId(), change.getClientId(), change.getApplicationData()),
					SyncStatus.OK);
		} catch (ItemNotFoundException e) {
			logger.warn("Item with server id {} not found.", change.getServerId());
			return new SyncClientCommands.Update(change.getServerId(), SyncStatus.OBJECT_NOT_FOUND);
		} catch (ConversionException e) {
			logger.error("Error while converting client UPDATE command", e);
			return new SyncClientCommands.Update(change.getServerId(), SyncStatus.SERVER_ERROR);
		}
	}

	private Add addServerItem(UserDataRequest udr, AnalysedSyncCollection collection, SyncCollectionCommandRequest addition)
			throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException,
			ProcessingEmailException, ConversionException, HierarchyChangedException, NoPermissionException {

		try {
			return new SyncClientCommands.Add(addition.getClientId(), contentsImporter.importMessageChange(
						udr, collection.getCollectionId(), addition.getServerId(), addition.getClientId(), addition.getApplicationData()),
					SyncStatus.OK);
		} catch (ItemNotFoundException e) {
			logger.warn("Item with server id {} not found.", addition.getServerId());
			return new SyncClientCommands.Add(addition.getClientId(), addition.getServerId(), SyncStatus.OBJECT_NOT_FOUND);
		} catch (ConversionException e) {
			logger.error("Error while converting client ADD command", e);
			return new SyncClientCommands.Add(addition.getClientId(), addition.getServerId(), SyncStatus.SERVER_ERROR);
		}
	}
	
	private Deletion deleteServerItem(UserDataRequest udr, AnalysedSyncCollection collection, SyncCollectionCommandRequest deletion)
			throws CollectionNotFoundException, DaoException,
			UnexpectedObmSyncServerException, ProcessingEmailException, UnsupportedBackendFunctionException {

		ServerId serverId = deletion.getServerId();
		try {
			contentsImporter.importMessageDeletion(udr, collection.getDataType(), collection.getCollectionId(), serverId,
					collection.getOptions().isDeletesAsMoves());
			return new SyncClientCommands.Deletion(serverId, SyncStatus.OK);
		} catch (ItemNotFoundException e) {
			logger.warn("Item with server id {} not found.", deletion.getServerId());
			return new SyncClientCommands.Deletion(deletion.getServerId(), SyncStatus.OBJECT_NOT_FOUND);
		} catch (ConversionException e) {
			logger.error("Error while converting client DELETE command", e);
			return new SyncClientCommands.Deletion(serverId, SyncStatus.SERVER_ERROR);
		}
	}
	
	private Fetch fetchServerItem(UserDataRequest udr, AnalysedSyncCollection collection, SyncCollectionCommandRequest fetch,
			ItemSyncState syncState, SyncKey newSyncKey)
					throws CollectionNotFoundException, DaoException, UnexpectedObmSyncServerException, ProcessingEmailException {

		final ServerId serverId = fetch.getServerId();
		try {
			Optional<ItemChange> optional = contentsExporter.fetch(udr, syncState, collection.getDataType(), 
					collection.getCollectionId(), collection.getOptions(), serverId, newSyncKey);
			
			if (optional.isPresent()) {
				return new SyncClientCommands.Fetch(serverId, SyncStatus.OK, optional.get().getData());
			}
			return new SyncClientCommands.Fetch(serverId, SyncStatus.OBJECT_NOT_FOUND, null);
		} catch (ItemNotFoundException e) {
			logger.warn("Item with server id {} not found.", fetch.getServerId());
			return new SyncClientCommands.Fetch(serverId, SyncStatus.OBJECT_NOT_FOUND, null);
		} catch (ConversionException e) {
			logger.error("Error while converting client FETCH command", e);
			return new SyncClientCommands.Fetch(serverId, SyncStatus.SERVER_ERROR, null);
		}
	}

	@Override
	public void sendResponseWithoutHierarchyChanges(UserDataRequest udr, Responder responder, IContinuation continuation) {
		sendResponse(udr, responder, false, continuation);
	}

	@Override
	public void sendResponse(UserDataRequest udr, Responder responder, boolean sendHierarchyChange, IContinuation continuation) {
		try {
			if (enablePush) {
				SyncResponse syncResponse = doTheJob(udr, monitoredCollectionService.list(udr.getCredentials(), udr.getDevice()), continuation);
				sendResponse(responder, syncProtocol.encodeResponse(udr.getDevice(), syncResponse));
			} else {
				//Push is not supported, after the heartbeat interval is over, we ask the phone to retry
				sendError(udr.getDevice(), responder, SyncStatus.NEED_RETRY.asSpecificationValue(), continuation);
			}
		} catch (DaoException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (CollectionNotFoundException e) {
			sendError(udr.getDevice(), responder, SyncStatus.OBJECT_NOT_FOUND, continuation, e);
		} catch (UnexpectedObmSyncServerException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (ProcessingEmailException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (InvalidServerId e) {
			sendError(udr.getDevice(), responder, SyncStatus.PROTOCOL_ERROR, continuation, e);			
		} catch (ConversionException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (ServerErrorException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		} catch (HierarchyChangedException e) {
			sendError(udr.getDevice(), responder, SyncStatus.HIERARCHY_CHANGED, continuation, e);
		} catch (InvalidSyncKeyException e) {
			sendError(udr.getDevice(), responder, SyncStatus.INVALID_SYNC_KEY, continuation, e);
		} catch (UnsupportedBackendFunctionException e) {
			sendError(udr.getDevice(), responder, SyncStatus.SERVER_ERROR, continuation, e);
		}
	}

	public SyncResponse doTheJob(UserDataRequest udr, Collection<AnalysedSyncCollection> changedFolders, 
			IContinuation continuation) throws DaoException, CollectionNotFoundException, 
			UnexpectedObmSyncServerException, ProcessingEmailException, InvalidServerId, ConversionException, HierarchyChangedException, UnsupportedBackendFunctionException {

		SyncResponse.Builder builder = SyncResponse.builder();
		for (AnalysedSyncCollection c : changedFolders) {
			builder.addResponse(computeSyncState(udr, c));
		}
		SyncResponse response = builder.build();

		logger.info("Resp for requestId = {}", continuation.getReqId());
		summaryLoggerService.logOutgoingSync(response);
		return response;
	}

	private SyncCollectionResponse computeSyncState(UserDataRequest udr,
			AnalysedSyncCollection syncCollectionRequest)
			throws DaoException, CollectionNotFoundException, InvalidServerId,
			UnexpectedObmSyncServerException, ProcessingEmailException, ConversionException, HierarchyChangedException, UnsupportedBackendFunctionException  {

		PIMDataType dataType = syncCollectionRequest.getDataType();
		SyncCollectionResponse.Builder builder = SyncCollectionResponse.builder()
				.collectionId(syncCollectionRequest.getCollectionId())
				.dataType(dataType);
			
		ItemSyncState newItemSyncState = null;
		if (syncCollectionRequest.getStatus() != SyncStatus.OK) {
			builder.status(syncCollectionRequest.getStatus());
		} else if (isDataTypeKnown(dataType)) {
			if (SyncKey.INITIAL_SYNC_KEY.equals(syncCollectionRequest.getSyncKey())) {
				handleInitialSync(udr, syncCollectionRequest, builder);
			} else {
				try {
					newItemSyncState = handleDataSync(udr, syncCollectionRequest, builder);
				} catch (FilterTypeChangedException e) {
					builder.status(SyncStatus.INVALID_SYNC_KEY);
				}
			}
		} else {
			builder.status(SyncStatus.OBJECT_NOT_FOUND);
		}
		
		SyncCollectionResponse syncCollectionResponse = builder.build();
		allocateSyncStateIfNew(udr, syncCollectionRequest, syncCollectionResponse, newItemSyncState);
		return syncCollectionResponse;
	}

	private ItemSyncState handleDataSync(UserDataRequest udr, AnalysedSyncCollection request,
			SyncCollectionResponse.Builder builder) throws CollectionNotFoundException, DaoException, 
			UnexpectedObmSyncServerException, ProcessingEmailException, InvalidServerId, ConversionException, FilterTypeChangedException, HierarchyChangedException, UnsupportedBackendFunctionException {

		ItemSyncState st = stMachine.getItemSyncState(request.getSyncKey());
		if (st == null) {
			builder.status(SyncStatus.INVALID_SYNC_KEY);
			return null;
		} else {
			SyncKey newSyncKey = syncKeyFactory.randomSyncKey();
			Date newSyncDate = doUpdates(udr, request, builder, st, newSyncKey);
			builder.syncKey(newSyncKey).status(SyncStatus.OK);
			
			return ItemSyncState.builder()
					.syncDate(newSyncDate)
					.syncKey(newSyncKey)
					.build();
		}
	}

	private void allocateSyncStateIfNew(UserDataRequest udr, AnalysedSyncCollection request, SyncCollectionResponse syncCollectionResponse, ItemSyncState itemSyncState) 
				throws DaoException, InvalidServerId {

		if (itemSyncState != null && !Objects.equal(request.getSyncKey(), itemSyncState.getSyncKey())) {
			stMachine.allocateNewSyncState(udr, request.getCollectionId(), itemSyncState.getSyncDate(), syncCollectionResponse, itemSyncState.getSyncKey());
		}
	}

	private void handleInitialSync(UserDataRequest udr, AnalysedSyncCollection syncCollectionRequest, SyncCollectionResponse.Builder builder) 
			throws DaoException, InvalidServerId {
		
		CollectionId collectionId = syncCollectionRequest.getCollectionId();
		backend.resetCollection(udr, collectionId);
		SyncKey newSyncKey = syncKeyFactory.randomSyncKey();
		stMachine.allocateNewSyncStateWithoutTracking(udr, collectionId, dateService.getEpochPlusOneSecondDate(), newSyncKey);
		contentsExporter.initialize(udr.getDevId(), collectionId, syncCollectionRequest.getDataType(), syncCollectionRequest.getOptions().getFilterType(), newSyncKey);
		
		builder.syncKey(newSyncKey)
			.status(SyncStatus.OK);
	}

	private List<ItemChange> identifyNewItems(List<ItemChange> itemChanges, ItemSyncState st)
			throws DaoException, InvalidServerId {

		Builder<ItemChange> builder = ImmutableList.builder();
		for (ItemChange change: itemChanges) {
			boolean isItemAddition = st.getSyncKey().equals(SyncKey.INITIAL_SYNC_KEY) || 
					!itemTrackingDao.isServerIdSynced(st, change.getServerId());
			builder.add(ItemChange.builder()
					.serverId(change.getServerId())
					.isNew(isItemAddition)
					.data(change.getData())
					.build());
		}
		return builder.build();
	}
	
	private void sendError(Device device, Responder responder, SyncStatus errorStatus,
			IContinuation continuation, Exception exception) {
		logError(errorStatus, exception);
		sendError(device, responder, errorStatus.asSpecificationValue(), continuation);
	}

	private void logError(SyncStatus errorStatus, Exception exception) {
		if (errorStatus == SyncStatus.SERVER_ERROR) {
			logger.error(exception.getMessage(), exception);
		} else {
			logger.warn(exception.getMessage(), exception);
		}
	}

	@Override
	public void sendError(Device device, Responder responder, String errorStatus, IContinuation continuation) {
		try {
			logger.info("Resp for requestId = {}", continuation.getReqId());
			responder.sendWBXMLResponse("AirSync", syncProtocol.encodeResponse(errorStatus) );
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
