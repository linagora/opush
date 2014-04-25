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
package org.obm.push

import com.google.common.io.Resources
import java.net.URI
import org.junit.runner.RunWith
import org.obm.push.scenario.Scenarios
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.immutable.Seq
import scala.reflect.io.File
import java.nio.file.Paths
import org.obm.push.context.User
import io.gatling.core.config.GatlingConfiguration

@RunWith(classOf[JUnitRunner])
class RunGatlingTest extends FunSuite with BeforeAndAfter {
	
	val requiredArguments = Map(
			"user-domain" -> "test",
			"base-url" -> "http://localhost",
			"csv" -> Resources.getResource("users.csv").getPath()
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

	test("argument scenario defined") {
		val configuration = RunGatling.parse(mapToList(requiredArguments + ("scenario" -> "calendar")))
		assert(configuration.isDefined)
		assert(configuration.get.scenario === Scenarios.calendar)
	}
	
	test("argument scenario undefined") {
		val configuration = RunGatling.parse(mapToList(requiredArguments + ("scenario" -> "undefined")))
		assert(configuration === Option.empty)
	}
	
	test("argument csv missing") {
		val configuration = RunGatling.parse(mapToList(requiredArguments - ("csv")))
		assert(configuration === Option.empty)
	}
	
	test("argument csv missing file") {
		val configuration = RunGatling.parse(mapToList(requiredArguments + ("csv" -> "my-path")))
		assert(configuration === Option.empty)
	}

	test("argument users-per-sec defined as double") {
		val configuration = RunGatling.parse(mapToList(requiredArguments + ("users-per-sec" -> "2.5")))
		assert(configuration.isDefined)
		assert(configuration.get.usersPerSec === 2.5d)
	}

	test("argument users-per-sec defined as int") {
		val configuration = RunGatling.parse(mapToList(requiredArguments + ("users-per-sec" -> "5")))
		assert(configuration.isDefined)
		assert(configuration.get.usersPerSec === 5d)
	}
	
	test("argument users-per-sec not a number") {
		val configuration = RunGatling.parse(mapToList(requiredArguments + ("users-per-sec" -> "not a number")))
		assert(configuration === Option.empty)
	}
	
	test("argument users-per-sec is more than csv user count") {
		GatlingConfiguration.setUp()
		val configuration = RunGatling.parse(mapToList(requiredArguments + ("users-per-sec" -> Int.MaxValue.toString)))
		val user: User = null
		
		intercept[IllegalStateException] {
			RunGatling.provisionUsers(configuration.get, a => user)
		}
	}
	
	test("arguments are valid") {
		val configuration = RunGatling.parse(mapToList(requiredArguments))
		assert(configuration === Option(RunGatlingConfig(
				baseURI = new URI("http://localhost"),
				userDomain = "test",
				scenario = Scenarios.defaultScenario,
				csv = new File(Paths.get(requiredArguments("csv")).toFile()),
				usersPerSec = 1)))
	}
}