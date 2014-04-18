package org.obm.push.scenario

import org.obm.push.context.Configuration

trait ScenarioBuilder {

	def build(configuration: Configuration): io.gatling.core.structure.ScenarioBuilder
	
}