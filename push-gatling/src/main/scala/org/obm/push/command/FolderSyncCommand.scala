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
package org.obm.push.command

import org.obm.push.bean.FolderSyncStatus
import org.obm.push.checks.Check
import org.obm.push.checks.{WholeBodyExtractorCheckBuilder => bodyExtractor}
import org.obm.push.encoder.GatlingEncoders.folderSyncProtocol
import org.obm.push.protocol.bean.FolderSyncRequest
import org.obm.push.protocol.bean.FolderSyncResponse
import org.obm.push.wbxml.WBXMLTools
import com.google.common.base.Strings
import io.gatling.core.Predef.Session
import io.gatling.core.validation.Failure
import io.gatling.core.validation.Success
import io.gatling.core.validation.Validation
import io.gatling.http.request.ByteArrayBody
import org.obm.push.checks.DataRequiredMatcher

class FolderSyncCommand(folderSyncContext: FolderSyncContext, wbTools: WBXMLTools = new WBXMLTools)
	extends AbstractActiveSyncCommand(folderSyncContext.userKey) {

	val folderSyncNamespace = "FolderHierarchy"
	
	override val commandTitle = "FolderSync command"
	override val commandName = "FolderSync"
	  
	override def buildCommand() = {
		super.buildCommand()
			.body(new ByteArrayBody((session: Session) => buildFolderSyncRequest(session)))
			.check(bodyExtractor.bodyBytes
			    .find
			    .transform(rawResponse => getOrNoneIfEmpty(rawResponse, toFolderSyncResponse))
			    .matchWith(folderSyncContext.matcher, s => Success(s))
			    .saveAs(folderSyncContext.userKey.lastFolderSyncSessionKey)
			    .build)
	}

	def buildFolderSyncRequest(session: Session): Validation[Array[Byte]] = {
		val nextFolderSyncSyncKey = folderSyncContext.nextSyncKey(session)
		val request = FolderSyncRequest.builder().syncKey(nextFolderSyncSyncKey).build()
		val requestDoc = folderSyncProtocol.encodeRequest(request)
		Success(wbTools.toWbxml(folderSyncNamespace, requestDoc))
	}
	
	def toFolderSyncResponse(response: Array[Byte]) = folderSyncProtocol.decodeResponse(wbTools.toXml(response))
}

object FolderSyncCommand {
	
	val validSyncKey = new DataRequiredMatcher[FolderSyncResponse, Session] {
		override def name = "FolderSync - valid synckey"
		override def apply(value: FolderSyncResponse, session: Session) = {
			val nextSyncKey = value.getNewSyncKey
			if (nextSyncKey != null &&
					!Strings.isNullOrEmpty(nextSyncKey.getSyncKey()) && nextSyncKey.getSyncKey() != "0") Success(Option.apply(value))
			else Failure("Invalid SyncKey in response")
		}
	}
	
	val statusOk = new DataRequiredMatcher[FolderSyncResponse, Session] {
		override def name = "FolderSync - valid status"
		override def apply(value: FolderSyncResponse, session: Session) = {
			if (value.getStatus() == FolderSyncStatus.OK) Success(Option.apply(value))
			else Failure("Status isn't ok : " + value.getStatus())
		}
	}
	
	val atLeastOneAdd = new DataRequiredMatcher[FolderSyncResponse, Session] {
		override def name = "FolderSync - at least one add"
		override def apply(value: FolderSyncResponse, session: Session) = {
			if (value.getCollectionsAddedAndUpdated().size() > 0) Success(Option.apply(value)) 
			else Failure("No add or update in response")
		}
	}
	
	val validInitialFolderSync = Check.manyToOne(validSyncKey, statusOk, atLeastOneAdd)
	val validFolderSync = Check.manyToOne(validSyncKey, statusOk)
}