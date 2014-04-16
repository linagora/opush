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
package org.obm.push

import scala.concurrent.duration.DurationInt

import org.joda.time.DateTime
import org.obm.DateUtils.date
import org.obm.push.bean.FolderType
import org.obm.push.command.DeleteInvitationCommand
import org.obm.push.command.FolderSyncCommand
import org.obm.push.command.InitialFolderSyncContext
import org.obm.push.command.InitialSyncContext
import org.obm.push.command.InvitationCommand.validDeleteInvitation
import org.obm.push.command.InvitationCommand.validModifiedInvitation
import org.obm.push.command.InvitationCommand.validSentInvitation
import org.obm.push.command.InvitationContext
import org.obm.push.command.ModifyInvitationCommand
import org.obm.push.command.SendInvitationCommand
import org.obm.push.command.SyncCollectionCommand
import org.obm.push.command.SyncContext
import org.obm.push.context.Configuration
import org.obm.push.context.GatlingConfiguration
import org.obm.push.context.UserKey
import org.obm.push.context.feeder.UserFeeder
import org.obm.push.helper.SimulationHelper.initializedUsers
import org.obm.push.wbxml.WBXMLTools

import io.gatling.core.Predef.Simulation
import io.gatling.core.Predef.UsersPerSecImplicit
import io.gatling.core.Predef.constantRate
import io.gatling.core.Predef.nothingFor
import io.gatling.core.Predef.scenario
import io.gatling.core.validation.Success
import io.gatling.http.Predef.http
import io.gatling.http.Predef.httpProtocolBuilder2HttpProtocol
import io.gatling.http.Predef.requestBuilder2ActionBuilder

class MeetingCreateUpdateDeleteSimulation extends Simulation {

	val wbTools: WBXMLTools = new WBXMLTools
	val configuration: Configuration = GatlingConfiguration.build

	val usedCalendarCollection = FolderType.DEFAULT_CALENDAR_FOLDER
	
	val httpConf = http
		.baseURL(configuration.baseUrl)
		.disableFollowRedirect
		.disableCaching
	
	lazy val scenarioForOrganizer = {
		val organizer = new UserKey("organizer")
		val invitee1 = new UserKey("invitee1")
		val invitee2 = new UserKey("invitee2")
		
		val invitation = new InvitationContext(
				organizer = organizer,
				attendees = Set(invitee1, invitee2),
				startTime = date("2014-01-12T09:00:00"),
				endTime = date("2014-01-12T10:00:00"),
				folderType = usedCalendarCollection)

		scenario("Create, update then delete a meeting").exitBlockOnFail(
		    initializedUsers(UserFeeder.newCSV("users.csv", configuration, organizer, invitee1, invitee2), organizer)
			.pause(configuration.pause)
			.exec(buildInitialSyncCommand(organizer, usedCalendarCollection)).pause(configuration.pause)
			.exec(s => Success(organizer.sessionHelper.setupNextInvitationClientId(s)))
			.exec(buildSendInvitationCommand(invitation))
			.pause(configuration.pause)
			.exec(s => Success(organizer.sessionHelper.setupPendingInvitation(s, invitation)))
			.exec(buildModifyInvitationCommand(invitation))
			.pause(configuration.pause)
			.exec(s => Success(organizer.sessionHelper.updatePendingInvitation(s)))
			.exec(buildDeleteInvitationCommand(invitation))
		)	
	}
	
	setUp(scenarioForOrganizer.inject(
	    nothingFor(5 seconds),
	    constantRate(configuration.usersPerSec userPerSec) during (configuration.duration)
	)).protocols(httpConf)
    
	def buildInitialFolderSyncCommand(userKey: UserKey) = {
		new FolderSyncCommand(new InitialFolderSyncContext(userKey), wbTools).buildCommand
	}
	
	def buildInitialSyncCommand(userKey: UserKey, folderType: FolderType) = {
		buildSyncCommand(new InitialSyncContext(userKey, folderType))
	}
	
	def buildSyncCommand(syncContext: SyncContext) = {
		new SyncCollectionCommand(syncContext, wbTools).buildCommand
	}
	
	def buildSendInvitationCommand(invitation: InvitationContext) = {
		new SendInvitationCommand(invitation.modify(matcher = validSentInvitation), wbTools).buildCommand
	}
	
	def buildModifyInvitationCommand(invitation: InvitationContext) = {
		val updatedInvitation = invitation.modify(
				startTime = new DateTime(invitation.startTime).plusHours(1).toDate(),
				endTime = new DateTime(invitation.endTime).plusHours(1).toDate(),
				matcher = validModifiedInvitation)
		new ModifyInvitationCommand(updatedInvitation, wbTools).buildCommand
	}
	
	def buildDeleteInvitationCommand(invitation: InvitationContext) = {
		new DeleteInvitationCommand(invitation.modify(matcher = validDeleteInvitation), wbTools).buildCommand
	}
}
