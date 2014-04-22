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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.obm.push.wbxml.WBXMLTools
import org.scalatest.junit.JUnitRunner
import scala.reflect.io.File
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.FileWriter
import io.gatling.core.config.GatlingConfiguration
import org.obm.push.context.Configuration
import org.obm.push.context.UserKey
import org.obm.push.context.User
import org.obm.push.bean.DeviceId

@RunWith(classOf[JUnitRunner])
class UserFeederTest extends FunSuite with BeforeAndAfter {
	
	val tempFolderRule = new TemporaryFolder()

	var wbxmlTools: WBXMLTools = _
	var config: Configuration = _

	before {
		GatlingConfiguration.setUp()
		config = new Configuration() {
			override val baseUrl = "localhost"
			override val domain = "my.domain"
		}
		wbxmlTools = new WBXMLTools()
		tempFolderRule.create()
	}

	after {
		tempFolderRule.delete()
	}
	
	test("Feeder build users only once") {
		val userFile = tempFolderRule.newFile()
				val fileWriter = new FileWriter(userFile)
		fileWriter.write(
				"""username,email,password
user.1,user.1@my.domain,secret
user.2,user.2@my.domain,secret""");
		fileWriter.close();

		val feeder = UserFeeder.newCSV(new File(userFile), config, new UserKey("a"), new UserKey("b")).build
		
		val firstIteration = feeder.next()
		val user1 = firstIteration("a").asInstanceOf[User]
		assert(user1.domain === "my.domain")
		assert(user1.login === "user.1")
		assert(user1.password === "secret")
		assert(user1.email === "user.1@my.domain")
		assert(user1.deviceId.getDeviceId().startsWith("Appl5K14358AA4S"))
		assert(user1.deviceType === "iPhone")
		val user2 = firstIteration("b").asInstanceOf[User]
		assert(user2.domain === "my.domain")
		assert(user2.login === "user.2")
		assert(user2.password === "secret")
		assert(user2.email === "user.2@my.domain")
		assert(user2.deviceId.getDeviceId().startsWith("Appl5K14358AA4S"))
		assert(user2.deviceType === "iPhone")
		assert(user2.deviceId != user1.deviceId)
		
		val secondIteration = feeder.next()
		assert(user1.equals(secondIteration("a")))
		assert(user2.equals(secondIteration("b")))
		
	}

}