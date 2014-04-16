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

import scala.collection.JavaConversions.collectionAsScalaIterable

import org.obm.push.bean.SyncCollectionRequest
import org.obm.push.bean.SyncKey
import org.obm.push.bean.SyncStatus
import org.obm.push.bean.change.SyncCommand
import org.obm.push.checks.Check
import org.obm.push.checks.DataRequiredMatcher
import org.obm.push.helper.SyncHelper
import org.obm.push.protocol.bean.SyncResponse
import org.obm.push.wbxml.WBXMLTools

import com.google.common.base.Strings

import io.gatling.core.Predef.Session
import io.gatling.core.validation.Failure
import io.gatling.core.validation.Success

class SyncCollectionCommand(syncContext: SyncContext, wbTools: WBXMLTools)
	extends AbstractSyncCommand(syncContext, wbTools) {

	def buildSyncCollectionRequest(session: Session) = {
		SyncCollectionRequest.builder()
				.collectionId(syncContext.findCollectionId(session))
				.syncKey(syncContext.nextSyncKey(session))
				.build()
	}
	
}

object SyncCollectionCommand {
  
	val onlyOneCollection = new DataRequiredMatcher[SyncResponse, Session] {
		override def name = "Sync - only one collection"
		override def apply(value: SyncResponse, session: Session) = {
			if (value.getCollectionResponses().size() <= 1) Success(Option.apply(value)) 
			else Failure("Only one collection expected in resposne")
		}
	}
	
	val validSyncKey = new DataRequiredMatcher[SyncResponse, Session] {
		override def name = "Sync - valid sync key"
		override def apply(value: SyncResponse, session: Session) = {
			val hasCollectionWithInvalidSyncKey = value
					.getCollectionResponses()
					.find(c => !isValidSyncKey(c.getSyncKey()))
					.isDefined
			if (!hasCollectionWithInvalidSyncKey) Success(Option.apply(value))
			else Failure("Invalid SyncKey in response")
		}
	}
	def isValidSyncKey(syncKey: SyncKey) : Boolean = {
		if (syncKey != null) {
			val syncKeyString = syncKey.getSyncKey()
			if (!Strings.isNullOrEmpty(syncKeyString) && syncKeyString != "0") {
				return true
			}
		}
		return false
	} 
	
	val statusOk = new DataRequiredMatcher[SyncResponse, Session] {
		override def name = "Sync - status ok"
		override def apply(value: SyncResponse, session: Session) = {
			if (value.getStatus() == SyncStatus.OK) Success(Option.apply(value))
			else Failure("Status isn't ok : " + value.getStatus())
		}
	}
	
	val noChange = new DataRequiredMatcher[SyncResponse, Session] {
		override def name = "Sync - no change"
		override def apply(value: SyncResponse, session: Session) = {
			val hasNoChange = SyncHelper.findChanges(value).isEmpty
			if (hasNoChange) Success(Option.apply(value)) 
			else Failure("Non expected change found in response")
		}
	}
	
	val atLeastOneAddResponse: DataRequiredMatcher[SyncResponse, Session] = atLeastOneTypedResponse(SyncCommand.ADD)
	val atLeastOneModifyResponse: DataRequiredMatcher[SyncResponse, Session] = atLeastOneTypedResponse(SyncCommand.CHANGE)
	val atLeastOneDeleteResponse: DataRequiredMatcher[SyncResponse, Session] = atLeastOneTypedResponse(SyncCommand.DELETE)
	def atLeastOneTypedResponse(commandType: SyncCommand): DataRequiredMatcher[SyncResponse, Session] = new DataRequiredMatcher[SyncResponse, Session] {
		override def name = "Sync - at least one response of type: " + commandType.asSpecificationValue()
		override def apply(value: SyncResponse, session: Session) = {
			val hasAddChange = value
					.getCollectionResponses()
					.flatMap(_.getResponses().getCommands())
					.find(_.getType() == commandType)
					.isDefined
			if (hasAddChange) Success(Option.apply(value)) 
			else Failure("No %s in response".format(commandType.asSpecificationValue()))
		}
	}
	
	val atLeastOneMeetingRequest = new DataRequiredMatcher[SyncResponse, Session] {
		override def name = "Sync - at least one meeting request"
		override def apply(value: SyncResponse, session: Session) = {
			val meetingRequests = SyncHelper.findChangesWithMeetingRequest(value)
			if (!meetingRequests.isEmpty) Success(Option.apply(value)) 
			else Failure("No meeting request in response")
		}
	}
	
	val validSync = Check.manyToOne(onlyOneCollection, validSyncKey, statusOk)
}