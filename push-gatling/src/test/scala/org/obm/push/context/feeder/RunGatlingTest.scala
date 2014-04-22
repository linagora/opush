/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014  Linagora
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
import org.obm.push.RunGatling
import org.obm.push.RunGatlingConfig
import java.net.URI
import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class RunGatlingTest extends FunSuite with BeforeAndAfter {
	
	val requiredArguments = Map(
			"user-domain" -> "test",
			"base-url" -> "http://localhost"
	)
	
	before {
	}

	after {
	}

	def mapToList(m: Map[String, String]) : Array[String] = {
		m.flatten((x) => Seq("--" + x._1, x._2)).toArray
	}
	
	test("argument user-domain missing") {
		val configuration = RunGatling.parse(mapToList(requiredArguments - ("user-domain")))
		assert(configuration === Option.empty)
	}

	test("argument base-url missing") {
		val configuration = RunGatling.parse(mapToList(requiredArguments - ("base-url")))
		assert(configuration === Option.empty)
	}

	test("argument base-url not an URL") {
		val configuration = RunGatling.parse(mapToList(requiredArguments + ("base-url" -> ":/localhost")))
		assert(configuration === Option.empty)
	}

	test("argument base-url space in the URL") {
		val configuration = RunGatling.parse(mapToList(requiredArguments + ("base-url" -> "http:// localhost")))
		assert(configuration === Option.empty)
	}
	
	test("arguments are valid") {
		val configuration = RunGatling.parse(mapToList(requiredArguments))
		assert(configuration === Option(RunGatlingConfig(
				baseURI = new URI("http://localhost"),
				userDomain = "test")))
	}
}