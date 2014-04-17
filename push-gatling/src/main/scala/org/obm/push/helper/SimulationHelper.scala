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