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

import java.util.List;

import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ICollectionPathHelper;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.Sync;
import org.obm.push.bean.SyncCollectionCommand;
import org.obm.push.bean.SyncCollectionCommandsResponse;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncStatus;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.exception.CollectionPathException;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.activesync.ASRequestIntegerFieldException;
import org.obm.push.exception.activesync.CollectionNotFoundException;
import org.obm.push.exception.activesync.PartialException;
import org.obm.push.exception.activesync.ProtocolException;
import org.obm.push.exception.activesync.ServerErrorException;
import org.obm.push.protocol.bean.SyncCollection;
import org.obm.push.protocol.bean.SyncCollectionCommandDto;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.SyncRequest;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.SyncedCollectionDao;
import org.w3c.dom.Element;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SyncAnalyser {

	private final CollectionDao collectionDao;
	private final SyncedCollectionDao syncedCollectionStoreService;
	private final ICollectionPathHelper collectionPathHelper;
	private final DecoderFactory decoderFactory;

	@Inject
	protected SyncAnalyser(SyncedCollectionDao syncedCollectionStoreService,
			CollectionDao collectionDao, ICollectionPathHelper collectionPathHelper,
			DecoderFactory decoderFactory) {
		this.collectionDao = collectionDao;
		this.syncedCollectionStoreService = syncedCollectionStoreService;
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
		try {
			builder.collectionId(collectionId)
				.syncKey(collectionRequest.getSyncKey())
				.windowSize(getWindowSize(syncRequest, collectionRequest))
				.options(getUpdatedOptions(
						findLastSyncedCollectionOptions(udr, isPartial, collectionId), collectionRequest))
				.status(SyncStatus.OK);
			
			PIMDataType dataType = recognizeCollection(builder, collectionId, collectionRequest.getDataClass());
			builder.commands(analyseCommands(collectionRequest.getCommands(), dataType));
			
			AnalysedSyncCollection analysed = builder.build();
			syncedCollectionStoreService.put(udr.getUser(), udr.getDevice(), analysed);
			return analysed;
		} catch (CollectionNotFoundException e) {
			return builder.status(SyncStatus.OBJECT_NOT_FOUND).build();
		}
		// TODO sync supported
		// TODO sync <getchanges/>

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

	private PIMDataType recognizeCollection(AnalysedSyncCollection.Builder builder, CollectionId collectionId, String dataClass) {
		String collectionPath = collectionDao.getCollectionPath(collectionId);
		PIMDataType dataType = collectionPathHelper.recognizePIMDataType(collectionPath);
		builder.collectionPath(collectionPath)
			.dataType(dataType);
		
		if (isDifferentClassThanType(dataClass, dataType)) {
			String msg = "The type of the collection found:{%s} is not the same than received in DataClass:{%s}";
			throw new ServerErrorException(String.format(msg, dataType.asXmlValue() , dataClass));
		}
		return dataType;
	}

	private boolean isDifferentClassThanType(String dataClass, PIMDataType dataType) {
		return !Strings.isNullOrEmpty(dataClass) && !dataType.asXmlValue().equals(dataClass);
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

	private SyncCollectionCommandsResponse analyseCommands(List<SyncCollectionCommandDto> requestCommands, PIMDataType dataType) {
		SyncCollectionCommandsResponse.Builder commandsResponseBuilder = SyncCollectionCommandsResponse.builder();
		for (SyncCollectionCommandDto command : requestCommands) {
			SyncCommand type = SyncCommand.fromSpecificationValue(command.getName());
			commandsResponseBuilder.addCommand(
				SyncCollectionCommand.builder()
					.type(type)
					.serverId(serverId(command))
					.clientId(command.getClientId())
					.applicationData(decodeApplicationData(command.getApplicationData(), dataType, type))
					.build());
		}
		return commandsResponseBuilder.build();
	}

	private ServerId serverId(SyncCollectionCommandDto dto) {
		if (dto.getServerId() != null) {
			return ServerId.of(dto.getServerId());
		}
		return null;
	}
	
	private IApplicationData decodeApplicationData(Element applicationData, PIMDataType dataType, SyncCommand syncCommand) {
		if (syncCommand.requireApplicationData()) {
			IApplicationData data = decode(applicationData, dataType);
			if (data == null) {
				throw new ProtocolException("No decodable " + dataType + " data for " + applicationData);
			}
			return data;
		}
		return null;
	}

	protected IApplicationData decode(Element data, PIMDataType dataType) {
		return decoderFactory.decode(data, dataType);
	}

}
