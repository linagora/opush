package org.obm.push.helper

import scala.concurrent.duration.DurationInt
import org.obm.push.bean.FolderType
import org.obm.push.command.ContactContext
import org.obm.push.command.ContactCommand.{validCreatedContact, validModifiedContact, validDeletedContact}
import org.obm.push.command.CreateContactCommand
import org.obm.push.command.DeleteContactCommand
import org.obm.push.command.FolderSyncCommand
import org.obm.push.command.InitialFolderSyncContext
import org.obm.push.command.InitialSyncContext
import org.obm.push.command.ModifyContactCommand
import org.obm.push.command.ProvisioningCommand
import org.obm.push.command.SyncCollectionCommand
import org.obm.push.context.Configuration
import org.obm.push.context.GatlingConfiguration
import org.obm.push.context.UserKey
import org.obm.push.context.feeder.UserFeeder
import org.obm.push.wbxml.WBXMLTools
import io.gatling.core.Predef.Simulation
import io.gatling.core.Predef.UsersPerSecImplicit
import io.gatling.core.Predef.bootstrap._
import io.gatling.core.Predef.constantRate
import io.gatling.core.Predef.nothingFor
import io.gatling.core.Predef.scenario
import io.gatling.core.validation.Success
import io.gatling.http.Predef.http
import io.gatling.http.Predef.httpProtocolBuilder2HttpProtocol
import io.gatling.http.Predef.requestBuilder2ActionBuilder
import io.gatling.core.feeder.FeederBuilder
import org.obm.push.context.User

object SimulationHelper {
  
	def initializedUsers(feeder: FeederBuilder[Any], userKey: UserKey) = 
		provisionedUsers(feeder, userKey)
		.doIf(s => userKey.getUser(s).hasNoFolderSyncResponse) (
			exec(new FolderSyncCommand(new InitialFolderSyncContext(userKey)).buildCommand)
			.exec(s => userKey.updateFolderSyncResponseResponse(s))
		)

	def provisionedUsers(feeder: FeederBuilder[Any], userKey: UserKey) = feed(feeder)
		.doIf(s => userKey.getUser(s).hasNoProvisionResponse) (
			exec(ProvisioningCommand.buildInitialProvisioningCommand(userKey))
			.exec(ProvisioningCommand.buildAcceptProvisioningCommand(userKey))
			.exec(s => userKey.updateProvisionResponse(s))
		)
}