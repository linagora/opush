/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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

import org.obm.push.bean.ProvisionStatus
import org.obm.push.checks.Check
import org.obm.push.checks.{WholeBodyExtractorCheckBuilder => bodyExtractor}
import org.obm.push.context.UserKey
import org.obm.push.encoder.GatlingEncoders.provisioningProtocol
import org.obm.push.protocol.bean.ProvisionRequest
import org.obm.push.protocol.bean.ProvisionResponse
import org.obm.push.wbxml.WBXMLTools
import io.gatling.core.Predef.Session
import io.gatling.core.validation.Failure
import io.gatling.core.validation.Success
import io.gatling.core.validation.Validation
import io.gatling.http.request.ByteArrayBody
import io.gatling.http.request.builder.PostHttpRequestBuilder
import org.obm.push.checks.DataRequiredMatcher

class ProvisioningCommand(provisioningContext: ProvisioningContext, wbTools: WBXMLTools)
	extends AbstractActiveSyncCommand(provisioningContext.userKey) {

	val namespace = "Provision"
	
	override val commandTitle = "Provisioning command"
	override val commandName = "Provision"
	  
	override def buildCommand() = {
		super.buildCommand()
			.body(new ByteArrayBody((session: Session) => buildProvisioningRequest(session)))
			.check(bodyExtractor.bodyBytes
			    .find
			    .transform(response => getOrNoneIfEmpty(response, toProvisioningResponse))
			    .matchWith(provisioningContext.matcher, s => Success(s))
			    .saveAs(provisioningContext.userKey.lastProvisioningSessionKey)
			    .build)
	}

	def buildProvisioningRequest(session: Session): Validation[Array[Byte]] = {
		val requestBuilder = ProvisionRequest.builder().policyType(ProvisioningCommand.protocolPolicyType)
		val lastProvisioning = provisioningContext.lastProvisioning(session)
		val requestDoc = provisioningProtocol.encodeRequest(lastProvisioning match {
			case None => requestBuilder.build()
			case Some(lastProvisioning) => requestBuilder.policyKey(lastProvisioning.getPolicyKey()).build()
		})
		Success(wbTools.toWbxml(namespace, requestDoc))
	}
	
	def toProvisioningResponse(response: Array[Byte]) = provisioningProtocol.decodeResponse(wbTools.toXml(response))
}

object ProvisioningCommand {

	val protocolPolicyType = "MS-EAS-Provisioning-WBXML"
	  
	val validPolicyKey = new DataRequiredMatcher[ProvisionResponse, Session] {
		override def name = "Provisioning - valid policy key"
		override def apply(value: ProvisionResponse, session: Session) = {
			val policyKey = value.getPolicyKey()
			if (policyKey != null && policyKey > 0) Success(Option.apply(value))
			else Failure("Invalid PolicyKey in response")
		}
	}
	
	val validPolicyType = new DataRequiredMatcher[ProvisionResponse, Session] {
		override def name = "Provisioning - valid policy type"
		override def apply(value: ProvisionResponse, session: Session) = {
			val policyType = value.getPolicyType()
			if (protocolPolicyType.equalsIgnoreCase(policyType)) Success(Option.apply(value))
			else Failure("Invalid PolicyKey in response")
		}
	}
	
	val hasPolicyData = new DataRequiredMatcher[ProvisionResponse, Session] {
		override def name = "Provisioning - has policy data"
		override def apply(value: ProvisionResponse, session: Session) = {
			if (value.getPolicy() != null) Success(Option.apply(value))
			else Failure("Reponse should not have policy data  : " + value.getPolicy())
		}
	}
	val hasNoPolicyData = new DataRequiredMatcher[ProvisionResponse, Session] {
		override def name = "Provisioning - has no policy data"
		override def apply(value: ProvisionResponse, session: Session) = {
			if (value.getPolicy() == null) Success(Option.apply(value))
			else Failure("Reponse should have policy data  : " + value.getPolicy())
		}
	}
	
	val statusOk = new DataRequiredMatcher[ProvisionResponse, Session] {
		override def name = "Provisioning - valid status"
		override def apply(value: ProvisionResponse, session: Session) = {
			if (value.getStatus() == ProvisionStatus.SUCCESS) Success(Option.apply(value))
			else Failure("Status isn't ok : " + value.getStatus())
		}
	}
	
	val validInitialProvisioningResponse = Check.manyToOne(validPolicyKey, validPolicyType, hasPolicyData, statusOk)
	val validAcceptProvisioningResponse = Check.manyToOne(validPolicyKey, validPolicyType, hasNoPolicyData, statusOk)
	
	val wbTools: WBXMLTools = new WBXMLTools
	
	def buildInitialProvisioningCommand(userKey: UserKey) : PostHttpRequestBuilder = 
		new ProvisioningCommand(new InitialProvisioningContext(userKey), wbTools).buildCommand
	
	def buildAcceptProvisioningCommand(userKey: UserKey) : PostHttpRequestBuilder = 
		new ProvisioningCommand(new AcceptProvisioningContext(userKey), wbTools).buildCommand
}