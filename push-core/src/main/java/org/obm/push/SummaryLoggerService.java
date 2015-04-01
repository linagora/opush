/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013-2015  Linagora
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
package org.obm.push;

import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.Summary;
import org.obm.push.bean.Sync;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.configuration.LoggerModule;
import org.obm.push.protocol.bean.FolderSyncResponse;
import org.obm.push.protocol.bean.MoveItemsRequest;
import org.obm.push.protocol.bean.MoveItemsResponse;
import org.obm.push.protocol.bean.SyncResponse;
import org.slf4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class SummaryLoggerService {

	private final Logger loggerIn;
	private final Logger loggerOut;

	@Inject
	@VisibleForTesting SummaryLoggerService(
			@Named(LoggerModule.SUMMARY_IN) Logger loggerIn,
			@Named(LoggerModule.SUMMARY_OUT) Logger loggerOut) {
		this.loggerIn = loggerIn;
		this.loggerOut = loggerOut;
	}
	
	public void logIncomingSync(Sync analyseSync) {
		if (!loggerIn.isInfoEnabled()) {
			return;
		}
		Summary summary = Summary.empty();
		for (AnalysedSyncCollection collection : analyseSync.getCollections()) {
			summary = summary.merge(collection.getSummary());
		}
		loggerIn.info(summary.summary());
	}

	public void logOutgoingSync(SyncResponse response) {
		if (!loggerOut.isInfoEnabled()) {
			return;
		}
		Summary summary = Summary.empty();
		for (SyncCollectionResponse collection : response.getCollectionResponses()) {
			summary = summary.merge(collection.getCommands().getSummary());
		}
		loggerOut.info(summary.summary());
	}

	public void logOutgoingFolderSync(FolderSyncResponse folderSyncResponse) {
		loggerOut.info(folderSyncResponse.getSummary().summary());
	}
	
	public void logIncomingMoveItem(MoveItemsRequest moveItemsRequest) {
		loggerIn.info(moveItemsRequest.getSummary().summary());
	}
	
	public void logOutgoingMoveItem(MoveItemsResponse moveItemsResponse) {
		loggerOut.info(moveItemsResponse.getSummary().summary());
	}

}
