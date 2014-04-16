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

import org.obm.push.bean.SyncCollectionRequest
import org.obm.push.checks.{WholeBodyExtractorCheckBuilder => bodyExtractor}
import org.obm.push.decoder.GatlingDecoders.syncDecoder
import org.obm.push.encoder.GatlingEncoders.syncEncoder
import org.obm.push.protocol.bean.SyncRequest
import org.obm.push.protocol.bean.SyncResponse
import org.obm.push.wbxml.WBXMLTools
import io.gatling.core.Predef.Session
import io.gatling.core.validation.Success
import io.gatling.core.validation.Validation
import io.gatling.http.request.ByteArrayBody
import org.xml.sax.SAXException
import java.io.IOException
import org.obm.push.checks.Check
import io.gatling.core.check.Matcher

abstract class AbstractSyncCommand(syncContext: SyncContext, wbTools: WBXMLTools)
	extends AbstractActiveSyncCommand(syncContext.userKey) {

	val syncNamespace = "AirSync"
	
	override val commandName = "Sync"
	override val commandTitle = syncContext match {
	  case _:InitialSyncContext => "Initial sync command"
	  case _ => "Sync command"
	}

	override def buildCommand() = {
		super.buildCommand()
			.body(new ByteArrayBody((session: Session) => buildSyncRequest(session)))
			.check(bodyExtractor.bodyBytes
			    .find
			    .transform(rawResponse => getOrNoneIfEmpty(rawResponse, toSyncResponse))
			    .matchWith(syncContext.matcher, s => Success(s))
			    .saveAs(syncContext.userKey.lastSyncSessionKey)
			    .build)
	}

	def buildSyncRequest(session: Session): Validation[Array[Byte]] = {
		val request = SyncRequest.builder()
			.addCollection(buildSyncCollectionRequest(session))
			.build()
		
		val requestDoc = syncEncoder.encodeSync(request)
		new Success(wbTools.toWbxml(syncNamespace, requestDoc))
	}
	
	def buildSyncCollectionRequest(session: Session): SyncCollectionRequest
	
	def toSyncResponse(response: Array[Byte]): SyncResponse = syncDecoder.decodeSyncResponse(wbTools.toXml(response))
}
