/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
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

import java.net.URI
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import scala.reflect.io.File
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.obm.push.bean.DeviceId
import org.obm.push.bean.SyncKey
import org.obm.push.context.Configuration
import org.obm.push.context.User
import org.obm.push.scenario.Scenario
import org.obm.push.scenario.Scenarios
import org.obm.push.wbxml.WBXMLTools
import org.obm.sync.push.client.WBXMLOPClient
import io.gatling.charts.report.ReportsGenerator
import io.gatling.core.Predef.csv
import io.gatling.core.Predef.array2FeederBuilder
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.feeder.Feeder
import io.gatling.core.result.reader.DataReader
import scopt.OptionParser
import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.forkjoin.ForkJoinPool
import io.gatling.core.feeder.AdvancedFeederBuilder
import io.gatling.core.feeder.Circular
import io.gatling.core.util.RoundRobin
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object RunGatling {

	val DEFAULT_DESCRIPTION = "opush-benchmark run"
	val DEFAULT_SIMULATION_ID = "1"
	val USERS_PREPARATION_PARALLELISM = 2
	
	def main(args: Array[String]) {
		try {
			parse(args)
			.map(run)
			.getOrElse {
				// arguments are bad, usage message will have been displayed
			}
		} catch {
			case e: Exception => {
				System.err.println(e.getMessage());
			}
		}
	}
	
	def run(config: Configuration) {
		GatlingConfiguration.setUp()
		
		val fullConfig = prepareUsers(config)
		
		val compositeSimulation = new CompositeSimulation(fullConfig, config.scenario)
		val (runId, simulation) = run(compositeSimulation)
		
		val dataReader = DataReader.newInstance(runId)
		generateReports(dataReader)
	}
	
	def prepareUsers(config: Configuration) = {
		val httpClient = buildHttpClient
		val wbxmlTools = new WBXMLTools
		
		def buildUserFromMap(fields: Map[String, String]) = {
			val domain = config.userDomain
			val login = fields("username")
			val password = fields("password")
			val email = fields("email")
			val deviceId = new DeviceId(Configuration.defaultUserDeviceId + UUID.randomUUID().toString())
			val deviceType = Configuration.defaultUserDeviceType
			val loginAtDomain = login + "@" + domain
			val client = new WBXMLOPClient(httpClient, loginAtDomain, password, deviceId, deviceType, "opush-benchmark agent",
					config.baseUrl + "/Microsoft-Server-ActiveSync", wbxmlTools, ProtocolVersion.V121)
			val firstPolicyKey = client.provisionStepOne().getResponse().getPolicyKey()
			val provisionResponse = client.provisionStepTwo(firstPolicyKey).getResponse()
			val folderSyncResponse = client.folderSync(SyncKey.INITIAL_FOLDER_SYNC_KEY)
			new User(
				domain, login, password, email, deviceId, deviceType, provisionResponse, folderSyncResponse 
			)
		}

		config.copy(users = provisionUsers(config, buildUserFromMap))
	}
	
	def provisionUsers(config: Configuration, buildUserFromMap: (Map[String, String] => User)) = {
		println("Prepare csv users for the run ...");
		val csvUsers = csv(config.csv).build.toParArray
		
		if (csvUsers.size < config.usersPerSec) {
			throw new IllegalStateException(
				s"""|csv user count:${csvUsers.size} is less than users-per-sec:${config.usersPerSec}
					|Please provide enough users in the csv to avoid conflicts""".stripMargin)
		}
		
		csvUsers.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(USERS_PREPARATION_PARALLELISM))
		RoundRobin(csvUsers.map(buildUserFromMap).toArray)
	}

	def generateReports(dataReader: DataReader) {
		val outputDir = "opush-benchmark_" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date)
		val indexFile = ReportsGenerator.generateFor(outputDir, dataReader)
		println(s"Please open the following file: $indexFile")
	}
	
	def run(compositeSimulation: CompositeSimulation) = new Runner(
			compositeSimulation,
			DEFAULT_SIMULATION_ID, 
			DEFAULT_DESCRIPTION
	).run
	
	def parse(args: Array[String]) : Option[Configuration] = {
		new OptionParser[Configuration]("opush-benchmark") {
			head("opush-benchmark")
			opt[String]("csv")
				.required
				.validate(value => if (Paths.get(value).toFile().exists()) success else failure(s"""The CSV "${value}" file does not exist"""))
				.action((value, config) => config.copy(csv = new File(Paths.get(value).toFile())))
				.text("User feeder CSV file with header respecting the format: username,password,email")
			opt[URI]("base-url")
				.required
				.action((value, config) => config.copy(baseUrl = value.toString))
				.text("OBM http server")
			opt[String]("user-domain")
				.required
				.action((value, config) => config.copy(userDomain = value))
				.text("User domain")
			opt[String]("scenario")
				.optional
				.validate( value => if (Scenarios.exists(value)) success else failure(s"""The scenario "${value}" does not exists"""))
				.action((value, config) => config.copy(scenario = Scenarios(value)))
				.text(s"The scenarios to run, possible values: ${Scenarios.all.keys.mkString(", ")}")
				.valueName(s"<value> (default value: ${Configuration.DEFAULT_SCENARIO.name})")
			opt[Double]("users-per-sec")
				.optional
				.validate( value => if (0 < value) success else failure("users-per-sec must be positive"))
				.action((value, config) => config.copy(usersPerSec = value))
				.text("Amount of users per seconds running the scenario")
				.valueName(s"<value> (default value: ${Configuration.DEFAULT_USERS_PER_SEC})")
			opt[Int]("duration")
				.optional
				.validate( value => if (0 < value) success else failure("duration must be positive"))
				.action((value, config) => config.copy(durationTime = value))
				.text("How long will take the run, see duration-unit")
				.valueName(s"<value> (default value: ${Configuration.DEFAULT_DURATION})")
			opt[String]("duration-unit")
				.optional
				.validate( value => {
					try {
						TimeUnit.valueOf(value.toUpperCase)
						success
					} catch { 
						case _: Throwable => failure("Illegal duration-unit value: " + value) 
					}
				})
				.action((value, config) => config.copy(durationUnit = TimeUnit.valueOf(value.toUpperCase)))
				.text(s"Run duration unit, possible values: ${TimeUnit.values().map(_.name.toLowerCase).mkString(", ")}")
				.valueName(s"<value> (default value: ${Configuration.DEFAULT_DURATION_UNIT.name.toLowerCase})")
		}.parse(args, Configuration())
	}
  
  private def buildHttpClient: org.apache.http.impl.client.CloseableHttpClient = {
	  val connManager = new PoolingHttpClientConnectionManager()
	  connManager.setMaxTotal(8)
	  connManager.setDefaultMaxPerRoute(8)
	  HttpClients.custom().setConnectionManager(connManager).build()
	}
}