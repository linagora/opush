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

import io.gatling.core.scenario.Simulation
import scopt.OptionParser
import java.net.URI
import io.gatling.core.runner.Selection
import org.obm.push.context.Configuration
import io.gatling.core.config.GatlingConfiguration
import org.obm.push.scenario.ScenarioBuilder
import org.obm.push.scenario.Scenarios
import scala.reflect.io.File
import java.nio.file.Paths

case class RunGatlingConfig(
	baseURI: URI = null,
	userDomain: String = null,
	scenarios: Seq[ScenarioBuilder] = Scenarios("default"),
	csv: File = null) {
	
	def hydrate() = new Configuration() {
		override val baseUrl = baseURI.toString()
		override val domain = userDomain
		override val csvFile = csv
  	}
}

object RunGatling {

	val DEFAULT_DESCRIPTION = "opush-benchmark run"
	val DEFAULT_SIMULATION_ID = "1"
	
	def main(args: Array[String]) {
		parse(args)
		.map(run)
		.getOrElse {
			// arguments are bad, usage message will have been displayed
		}
	}
	
	def run(config: RunGatlingConfig) {
		GatlingConfiguration.setUp()
		val compositeSimulation = new CompositeSimulation(config.hydrate(), config.scenarios)
		val (runId, simulation) = new Runner(
			compositeSimulation,
			DEFAULT_SIMULATION_ID, 
			DEFAULT_DESCRIPTION
		).run
	}
	
	def parse(args: Array[String]) : Option[RunGatlingConfig] = {
		new OptionParser[RunGatlingConfig]("opush-benchmark") {
			head("opush-benchmark")
			opt[String]("csv")
				.required
				.validate(value => if (Paths.get(value).toFile().exists()) success else failure(s"""The CSV "${value}" file does not exist"""))
				.action((value, config) => config.copy(csv = new File(Paths.get(value).toFile())))
				.text("User feeder CSV file with header respecting the format: username,password,email")
			opt[URI]("base-url")
				.required
				.action((value, config) => config.copy(baseURI = value))
				.text("OBM http server")
			opt[String]("user-domain")
				.required
				.action((value, config) => config.copy(userDomain = value))
				.text("User domain")
			opt[String]("scenario")
				.optional
				.validate( value => if (Scenarios.exists(value)) success else failure(s"""The scenario "${value}" does not exists"""))
				.action((value, config) => config.copy(scenarios = Scenarios(value)))
				.text("The scenarios to run, possible values : " + Scenarios.scenarios.keys.mkString(", "))
		}.parse(args, RunGatlingConfig())
	}
}