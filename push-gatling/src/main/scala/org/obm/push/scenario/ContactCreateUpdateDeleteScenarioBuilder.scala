/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.push.scenario
import org.obm.push.bean.FolderType
import org.obm.push.command.ContactCommand.validCreatedContact
import org.obm.push.command.ContactCommand.validDeletedContact
import org.obm.push.command.ContactCommand.validModifiedContact
import org.obm.push.command.ContactContext
import org.obm.push.command.CreateContactCommand
import org.obm.push.command.DeleteContactCommand
import org.obm.push.command.InitialSyncContext
import org.obm.push.command.ModifyContactCommand
import org.obm.push.command.SyncCollectionCommand
import org.obm.push.context.Configuration
import org.obm.push.context.UserKey
import org.obm.push.context.feeder.UserFeeder
import org.obm.push.helper.SimulationHelper.initializedUsers
import org.obm.push.wbxml.WBXMLTools
import io.gatling.core.Predef.{scenario => createScenario}
import io.gatling.core.validation.Success
import io.gatling.http.Predef.requestBuilder2ActionBuilder
import io.gatling.core.Predef.{scenario => createScenario}

object ContactCreateUpdateDeleteScenarioBuilder extends ScenarioBuilder {

	val wbTools: WBXMLTools = new WBXMLTools
	val usedContactCollection = FolderType.DEFAULT_CONTACTS_FOLDER
	val userKey = new UserKey("user")

	override def build(configuration: Configuration) =
		createScenario("Create, modify then drop contact").exitBlockOnFail(
			initializedUsers(UserFeeder.newCSV("users.csv", configuration, userKey), userKey)
			.pause(configuration.pause)
			.exec(buildInitialSyncCommand(userKey, usedContactCollection))
			.pause(configuration.pause)
			.exec(s => Success(userKey.sessionHelper.setupNextContactClientId(s)))
			.exec(buildCreateContactCommand(userKey))
			.exec(s => Success(userKey.sessionHelper.setupLastContactServerId(s)))
			.exec(buildModifyContactCommand(userKey))
			.exec(buildDeleteContactCommand(userKey))
	)
	
	def buildInitialSyncCommand(userKey: UserKey, folderType: FolderType) = {
		new SyncCollectionCommand(new InitialSyncContext(userKey, folderType), wbTools).buildCommand
	}
	
	def buildCreateContactCommand(userKey: UserKey) = {
	  	new CreateContactCommand(new ContactContext(userKey, matcher = validCreatedContact), wbTools).buildCommand
	}
	
	def buildModifyContactCommand(userKey: UserKey) = {
	  	new ModifyContactCommand(new ContactContext(userKey, matcher = validModifiedContact), wbTools).buildCommand
	}
	
	def buildDeleteContactCommand(userKey: UserKey) = {
	  	new DeleteContactCommand(new ContactContext(userKey, matcher = validDeletedContact), wbTools).buildCommand
	}
}
