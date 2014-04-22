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
package org.obm.push.context.feeder

import org.obm.push.context.Configuration
import org.obm.push.context.User
import org.obm.push.context.UserKey
import io.gatling.core.Predef.csv
import io.gatling.core.feeder.Feeder
import io.gatling.core.feeder.FeederBuilder
import java.util.UUID
import org.obm.push.bean.DeviceId
import java.io.InputStream
import scala.reflect.io.File
import io.gatling.core.feeder.AdvancedFeederBuilder
import org.obm.push.command.ProvisioningCommand
import org.obm.push.helper.SessionHelper

class UserFeeder(originalFeeder: AdvancedFeederBuilder[String], config: Configuration, userKeys: UserKey*) extends FeederBuilder[Any] {
	
	val cyclicUserData = originalFeeder.circular.build
	val alreadyBuiltUsers = scala.collection.mutable.Map[String, User]()
	
	def build: Feeder[Any] = new Feeder[Any] {
		override def hasNext = true
		override def next = {
			val sessionMap = scala.collection.mutable.Map[String, Any]()
			for (userKey <- userKeys; userData = cyclicUserData.next) {
				val user = alreadyBuiltUsers.getOrElseUpdate(userData("username"), buildUserFromMap(userData))
				if (user.provisionResponse.isDefined) {
					sessionMap(userKey.lastProvisioningSessionKey) = user.provisionResponse.get
				}
				if (user.folderSyncResponse.isDefined) {
					sessionMap(userKey.lastFolderSyncSessionKey) = user.folderSyncResponse.get
				}
				sessionMap(userKey.key) = user
			}
			sessionMap.toMap
		}
	}
	
	def buildUserFromMap(fields: Map[String, String]) = new User(
		domain = config.domain,
		login = fields("username"),
		password = fields("password"),
		email = fields("email"),
		deviceId = new DeviceId(config.defaultUserDeviceId + UUID.randomUUID().toString()),
		deviceType = config.defaultUserDeviceType
	)
}

object UserFeeder {
	
	def newCSV(config: Configuration, userKeys: UserKey*) = new UserFeeder(csv(config.csvFile), config, userKeys: _*)
}