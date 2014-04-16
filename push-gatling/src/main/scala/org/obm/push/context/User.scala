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
package org.obm.push.context

import org.apache.james.mime4j.dom.address.Mailbox
import org.obm.push.bean.Device
import org.obm.push.bean.DeviceId
import org.obm.push.helper.SessionHelper
import io.gatling.core.session.Session
import io.gatling.core.validation.Success
import org.obm.push.protocol.bean.ProvisionResponse
import org.obm.push.protocol.bean.FolderSyncResponse
import io.gatling.core.validation.Validation

case class User(
	domain: String,
	login: String,
	password: String,
	email: String,
	deviceId: DeviceId,
	deviceType: String
) {
  
	def hasNoFolderSyncResponse = new Success(folderSyncResponse.isEmpty)
	var folderSyncResponse: Option[FolderSyncResponse] = Option.empty
	
	var provisionResponse: Option[ProvisionResponse] = Option.empty
	def hasNoProvisionResponse = new Success(provisionResponse.isEmpty)
  
	val userProtocol = "%s@%s".format(login, domain)
	lazy val mailbox = new Mailbox(email.split("@")(0), email.split("@")(1))
	lazy val device = new Device.Factory().create(
			null, 
			deviceType,
			"Mozilla/5.0 (X11; Linux x86_64; rv:10.0.7) Gecko/20100101 Firefox/10.0.7 Iceweasel/10.0.7",
			deviceId,
			ActiveSyncConfiguration.activeSyncVersion)
}

class UserKey (val key: String) {
	
	val elUserPolicyKey = key + ":PolicyKey"
	
	def getUser(session: Session) = session.attributes.get(key).get.asInstanceOf[User]
	
	def updateProvisionResponse(session: Session): Validation[Session] = {
		getUser(session).provisionResponse = sessionHelper.findLastProvisioning(session)
		Success(session)
	}
	
	def updateFolderSyncResponseResponse(session: Session): Validation[Session] = {
		getUser(session).folderSyncResponse = sessionHelper.findLastFolderSync(session)
		Success(session)
	}
	
	lazy val sessionHelper = new SessionHelper(this) 
	lazy val lastProvisioningSessionKey = buildSessionKey(UserSessionKeys.LAST_PROVISIONING_KEY)
	lazy val lastFolderSyncSessionKey = buildSessionKey(UserSessionKeys.LAST_FOLDER_SYNC)
	lazy val lastSyncSessionKey = buildSessionKey(UserSessionKeys.LAST_SYNC)
	lazy val lastMeetingResponseSessionKey = buildSessionKey(UserSessionKeys.MEETING_RESPONSE)
	lazy val lastInvitationClientIdSessionKey = buildSessionKey(UserSessionKeys.INVITATION_CLIENT_ID)
	lazy val lastContactClientIdSessionKey = buildSessionKey(UserSessionKeys.CONTACT_CLIENT_ID)
	lazy val lastContactServerIdSessionKey = buildSessionKey(UserSessionKeys.CONTACT_SERVER_ID)
	lazy val lastPendingInvitationSessionKey = buildSessionKey(UserSessionKeys.PENDING_INVITATION)
	
	private[this] def buildSessionKey(sessionKey: UserSessionKeys.Keys) = "%s:%s".format(sessionKey, key)
}

object UserSessionKeys extends Enumeration {
	type Keys = Value
	
	val LAST_PROVISIONING_KEY = Value("lastProvisioningKey")
	val LAST_FOLDER_SYNC = Value("lastFolderSync")
	val LAST_SYNC = Value("lastSync")
	val MEETING_RESPONSE = Value("meetingResponse")
	val INVITATION_CLIENT_ID = Value("invitationClientId")
	val CONTACT_CLIENT_ID = Value("contactClientId")
	val CONTACT_SERVER_ID = Value("contactServerId")
	val PENDING_INVITATION = Value("pendingInvitation")
	
}