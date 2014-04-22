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

import java.util.UUID

import org.apache.james.mime4j.dom.Message
import org.obm.push.mail.Mime4jUtils

import com.google.common.io.ByteStreams
import com.google.common.io.Resources

import io.gatling.core.Predef.Session
import io.gatling.core.Predef.checkBuilder2Check
import io.gatling.core.Predef.extractorCheckBuilder2MatcherCheckBuilder
import io.gatling.core.Predef.value2Expression
import io.gatling.core.validation.Success
import io.gatling.http.Predef.status
import io.gatling.http.request.ByteArrayBody

class SendEmailCommand(sendContext: SendEmailContext)
		extends AbstractActiveSyncCommand(sendContext.from) {

	val mime4jUtils = new Mime4jUtils()
	val saveInSent = if (sendContext.saveInSent) "T" else "F" 
  
	override val commandTitle = "SendMail command"
	override val commandName = "SendMail"
	  
	override def buildCommand() = {
		super.buildCommand()
			.queryParam((session: Session) =>Success("SaveInSent"), (session: Session) => Success(saveInSent))
			.body(new ByteArrayBody((session: Session) => Success(mailAsBytesArray(session))))
			.check(status.is(200))
	}

	def buildMail(session: Session): Message = {
		val mailPart = Resources.getResource("data/mixedEmail.eml-part").openStream()
		val message = mime4jUtils.parseMessage(mailPart)

		message.createMessageId("opush-gatling" + UUID.randomUUID().toString())
		message.setSubject("opush-gatling email")
		message.setFrom(sendContext.from.getUser(session).mailbox)
		message.setTo(sendContext.to.getUser(session).mailbox)
		return message
	}
	
	def mailAsBytesArray(session: Session) = ByteStreams.toByteArray(mime4jUtils.toInputStream(buildMail(session))) 

}
