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

import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ICollectionPathHelper;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.Sync;
import org.obm.push.bean.SyncCollectionCommand;
import org.obm.push.bean.SyncCollectionCommandsRequest;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.exception.CollectionPathException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.ASRequestIntegerFieldException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.InvalidServerId;
import org.obm.push.exception.activesync.PartialException;
import org.obm.push.exception.activesync.ProtocolException;
import org.obm.push.exception.activesync.ServerErrorException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.SyncCollection;
import org.obm.push.protocol.bean.SyncRequest;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.SyncedCollectionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SyncAnalyser {

	private static final Logger logger = LoggerFactory.getLogger(SyncAnalyser.class);
	
	private final SyncedCollectionDao syncedCollectionStoreService;
	private final CollectionDao collectionDao;
	private final ICollectionPathHelper collectionPathHelper;
	private final DecoderFactory decoderFactory;

	@Inject
	protected SyncAnalyser(SyncedCollectionDao syncedCollectionStoreService,
			CollectionDao collectionDao, ICollectionPathHelper collectionPathHelper,
			DecoderFactory decoderFactory) {
		this.syncedCollectionStoreService = syncedCollectionStoreService;
		this.collectionDao = collectionDao;
		this.collectionPathHelper = collectionPathHelper;
		this.decoderFactory = decoderFactory;
	}

	public Sync analyseSync(UserDataRequest udr, SyncRequest syncRequest) 
			throws PartialException, ProtocolException, DaoException, CollectionPathException {
		assertNotPartialRequest(syncRequest);

		Sync.Builder builder = Sync.builder()
				.waitInMinutes(syncRequest.getWaitInMinute());
	
		for (SyncCollection syncCollectionRequest : syncRequest.getCollections()) {
			builder.addCollection(getCollection(syncRequest, udr, syncCollectionRequest, false));
		}
		return builder.build();
	}

	private void assertNotPartialRequest(SyncRequest syncRequest) {
		if (syncRequest.isPartial() != null && syncRequest.isPartial()) {
			throw new PartialException();
		}
	}
	
	private AnalysedSyncCollection getCollection(SyncRequest syncRequest, UserDataRequest udr, SyncCollection collectionRequest, boolean isPartial)
			throws PartialException, ProtocolException, DaoException, CollectionPathException{
		
		AnalysedSyncCollection.Builder builder = AnalysedSyncCollection.builder();
		CollectionId collectionId = getCollectionId(collectionRequest);
		builder
			.collectionId(collectionId)
			.syncKey(collectionRequest.getSyncKey());
		
		
		try {
			String collectionPath = collectionDao.getCollectionPath(collectionId);
			checkCollectionType(collectionRequest.getDataType(), collectionPath);
			builder
				.collectionPath(collectionPath)
				.dataType(collectionRequest.getDataType())
				.windowSize(getWindowSize(syncRequest, collectionRequest))
				.options(getUpdatedOptions(
						findLastSyncedCollectionOptions(udr, isPartial, collectionId), collectionRequest))
				.status(SyncStatus.OK);
			
			
			SyncCollectionCommandsRequest.Builder commands = SyncCollectionCommandsRequest.builder();
			for (SyncCollectionCommand command: collectionRequest.getCommands()) {
				checkRequiredData(command);
				try {
					checkServerId(command, collectionId);
					commands.addCommand(command);
				} catch (InvalidServerId e) {
					logger.warn("Error with a command", e);
				}
			}
			builder.commands(commands.build());
			
			AnalysedSyncCollection analysed = builder.build();
			syncedCollectionStoreService.put(udr.getUser(), udr.getDevice(), analysed);
			return analysed;
		} catch (CollectionNotFoundException e) {
			return builder.status(SyncStatus.OBJECT_NOT_FOUND).build();
		}
		// TODO sync supported
		// TODO sync <getchanges/>

	}

	private void checkRequiredData(SyncCollectionCommand command) {
		if (command.getType().requireApplicationData() && command.getApplicationData() == null) {
			throw new ProtocolException("No decodable " + command.getType() + " data");
		}
	}
	
	private void checkServerId(SyncCollectionCommand command, CollectionId collectionId) throws InvalidServerId {
		ServerId serverId = command.getServerId();
		if (serverId != null) {
			if (!serverId.isItem() || serverId.getItemId() < 1) {
				throw new InvalidServerId("item " + serverId.toString() + " must have an Id greater than O");
			}
			if (!serverId.belongsTo(collectionId)) {
				throw new InvalidServerId("item " + serverId.toString() + " doesn't belong to collection " + collectionId);
			}
		}
	}

	private void checkCollectionType(PIMDataType dataClass, String collectionPath) {
		PIMDataType collectionDataType = collectionPathHelper.recognizePIMDataType(collectionPath);
		if (!Objects.equal(dataClass, collectionDataType)) {
			String msg = "The type of the collection found:{%s} is not the same than received in DataClass:{%s}";
			throw new ServerErrorException(String.format(msg, collectionDataType , dataClass));
		}
	}

	private CollectionId getCollectionId(SyncCollection collectionRequest) {
		CollectionId collectionId = collectionRequest.getCollectionId();
		if (collectionId == null) {
			throw new ASRequestIntegerFieldException("Collection id field is required");
		}
		return collectionId;
	}

	private Integer getWindowSize(SyncRequest syncRequest, SyncCollection collectionRequest) {
		return Objects.firstNonNull(collectionRequest.getWindowSize(), syncRequest.getWindowSize());
	}

	private AnalysedSyncCollection findLastSyncedCollectionOptions(UserDataRequest udr, boolean isPartial, CollectionId collectionId) {
		AnalysedSyncCollection lastSyncCollection = 
				syncedCollectionStoreService.get(udr.getCredentials(), udr.getDevice(), collectionId);
		if (isPartial && lastSyncCollection == null) {
			throw new PartialException();
		}
		return lastSyncCollection;
	}

	private SyncCollectionOptions getUpdatedOptions(AnalysedSyncCollection lastSyncCollection, SyncCollection requestCollection) {
		SyncCollectionOptions lastUsedOptions = null;
		if (lastSyncCollection != null) {
			lastUsedOptions = lastSyncCollection.getOptions();
		}
		
		if (!requestCollection.hasOptions() && lastUsedOptions != null) {
			return lastUsedOptions;
		} else if (requestCollection.hasOptions()) {
			return SyncCollectionOptions.cloneOnlyByExistingFields(requestCollection.getOptions());
		}
		return SyncCollectionOptions.defaultOptions();
	}

	protected IApplicationData decode(Element data, PIMDataType dataType) {
		return decoderFactory.decode(data, dataType);
	}

}
