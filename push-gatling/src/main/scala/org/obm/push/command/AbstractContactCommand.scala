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

import org.obm.push.bean.MSContact
import org.obm.push.bean.SyncCollectionCommands
import org.obm.push.bean.SyncCollectionCommandRequest
import org.obm.push.bean.SyncCollectionCommandsRequest
import org.obm.push.bean.SyncCollectionRequest
import org.obm.push.bean.change.SyncCommand
import org.obm.push.checks.Check
import org.obm.push.encoder.GatlingEncoders.contactEncoder
import org.obm.push.utils.DOMUtils
import org.obm.push.wbxml.WBXMLTools

import io.gatling.core.session.Session

abstract class AbstractContactCommand(contact: ContactContext, wbTools: WBXMLTools)
		extends AbstractSyncCommand(contact, wbTools) {

	override val commandTitle = "abstract contact command"
  
	override def buildSyncCollectionRequest(session: Session) = {
		SyncCollectionRequest.builder()
				.collectionId(contact.findCollectionId(session))
				.syncKey(contact.nextSyncKey(session))
				.commands(SyncCollectionCommandsRequest.builder()
					.addCommand(org.obm.push.bean.SyncCollectionCommandRequest.builder()
							.name(collectionSyncCommand.asSpecificationValue())
							.clientId(clientId(session))
							.serverId(serverId(session))
							.applicationData(buildContactData(session))
							.build())
					.build())
				.build()
	}
	
	def buildContactData(session: Session) = {
		val user = contact.userKey.getUser(session)
		buildContact(session) match {
		  case Some(msContact) => {
		    val parent = DOMUtils.createDoc(null, "ApplicationData").getDocumentElement()
			contactEncoder.encode(user.device, parent, msContact)
			parent
		  }
		  case _ => null
		}
	}
	
	val collectionSyncCommand: SyncCommand
	def clientId(session: Session): String = null
	def serverId(session: Session) = contact.userKey.sessionHelper.findLastContactServerId(session)
	
	def buildContact(session: Session) = {
		val msContact = new MSContact()
		msContact.setFileAs(contact.userKey.getUser(session).login)
		msContact.setFirstName("FirstName")
		msContact.setEmail1Address("email@domain.org")
		msContact.setCompanyName("OBM")
		msContact.setHomeAddressStreet("1 street")
		msContact.setHomeAddressCity("Paris")
		msContact.setHomeAddressCountry("France")
		msContact.setHomePhoneNumber("0123456789")
		msContact.setBusinessAddressCity("Paris")
		msContact.setBusinessAddressCountry("France")
		msContact.setBusinessPhoneNumber("0123456781")
		Option.apply(msContact)
	}
}


object ContactCommand {
	val validCreatedContact = Check.manyToOne(SyncCollectionCommand.validSync, SyncCollectionCommand.atLeastOneAddResponse)
	val validModifiedContact = Check.manyToOne(SyncCollectionCommand.validSync, SyncCollectionCommand.atLeastOneModifyResponse)
	val validDeletedContact = Check.manyToOne(SyncCollectionCommand.validSync, SyncCollectionCommand.atLeastOneDeleteResponse)
}