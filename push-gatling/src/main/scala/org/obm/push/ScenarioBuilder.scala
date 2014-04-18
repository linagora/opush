package org.obm.push

import org.obm.push.context.Configuration

trait ScenarioBuilder {

	def build(configuration: Configuration): io.gatling.core.structure.ScenarioBuilder
	
}