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
package org.obm.push

import scala.collection.Iterator
import scala.concurrent.duration.DurationInt
import scala.util.control.Breaks

import org.obm.DateUtils.date
import org.obm.push.bean.AttendeeStatus
import org.obm.push.bean.FolderType
import org.obm.push.checks.Check
import org.obm.push.command.FolderSyncCommand
import org.obm.push.command.InitialFolderSyncContext
import org.obm.push.command.InitialSyncContext
import org.obm.push.command.InvitationCommand
import org.obm.push.command.InvitationContext
import org.obm.push.command.MeetingResponseCommand
import org.obm.push.command.MeetingResponseContext
import org.obm.push.command.ProvisioningCommand
import org.obm.push.command.SendInvitationCommand
import org.obm.push.command.SyncCollectionCommand
import org.obm.push.command.SyncCollectionCommand.atLeastOneMeetingRequest
import org.obm.push.command.SyncCollectionCommand.validSync
import org.obm.push.command.SyncContext
import org.obm.push.context.Configuration
import org.obm.push.context.GatlingConfiguration
import org.obm.push.context.User
import org.obm.push.context.UserKey
import org.obm.push.context.feeder.UserFeeder
import org.obm.push.protocol.bean.SyncResponse
import org.obm.push.wbxml.WBXMLTools

import io.gatling.core.Predef.Simulation
import io.gatling.core.Predef.UsersPerSecImplicit
import io.gatling.core.Predef.bootstrap.exec
import io.gatling.core.Predef.constantRate
import io.gatling.core.Predef.nothingFor
import io.gatling.core.Predef.{scenario => createScenario}
import io.gatling.core.check.Matcher
import io.gatling.core.session.Session
import io.gatling.core.validation.Success
import io.gatling.http.Predef.http
import io.gatling.http.Predef.httpProtocolBuilder2HttpProtocol
import io.gatling.http.Predef.requestBuilder2ActionBuilder
import io.gatling.http.request.builder.PostHttpRequestBuilder

object InviteTwoUsersOneAcceptOneDeclineScenarioBuilder extends ScenarioBuilder {

	val inviteTwoUsersScenarioBuilder = InviteTwoUsersScenarioBuilder
	val wbTools: WBXMLTools = new WBXMLTools
	val usedMailCollection = FolderType.DEFAULT_INBOX_FOLDER 
	val usedCalendarCollection = FolderType.DEFAULT_CALENDAR_FOLDER
	val organizerKey = inviteTwoUsersScenarioBuilder.organizer
	val attendee1Key = inviteTwoUsersScenarioBuilder.invitee1
	val attendee2Key = inviteTwoUsersScenarioBuilder.invitee2
	
	val invitation = inviteTwoUsersScenarioBuilder.invitation
		
	override def build(configuration: Configuration) = 
		createScenario("Send an invitation at two attendees")
			.exitBlockOnFail(
				exec(inviteTwoUsersScenarioBuilder.build(configuration))
				.exec(ProvisioningCommand.buildInitialProvisioningCommand(attendee1Key))
				.exec(ProvisioningCommand.buildAcceptProvisioningCommand(attendee1Key))
				.exec(ProvisioningCommand.buildInitialProvisioningCommand(attendee2Key))
				.exec(ProvisioningCommand.buildAcceptProvisioningCommand(attendee2Key))
				.exec(buildInitialFolderSyncCommand(attendee1Key))
				.exec(buildInitialFolderSyncCommand(attendee2Key))
				.exec(s => Success(organizerKey.sessionHelper.setupPendingInvitation(s, invitation)))
				.pause(s => Success(configuration.asynchronousChangeTime))
				.exec(buildInitialSyncCommand(attendee1Key, usedMailCollection))
				.exec(buildInitialSyncCommand(attendee2Key, usedMailCollection))
				.exec(buildSyncCommand(attendee1Key, usedMailCollection, atLeastOneMeetingRequest))
				.exec(buildSyncCommand(attendee2Key, usedMailCollection, atLeastOneMeetingRequest))
				.exec(buildMeetingResponseCommand(attendee1Key, AttendeeStatus.ACCEPT))
				.exec(buildMeetingResponseCommand(attendee2Key, AttendeeStatus.DECLINE))
				.pause(configuration.asynchronousChangeTime)
				.exec(buildSyncCommand(organizerKey, usedCalendarCollection, Check.matcher((s, response) 
				    => (organizerKey.sessionHelper.attendeeRepliesAreReceived(s.asInstanceOf[Session], response.get), "Each users havn't replied"))))
			)
	
	def buildInitialFolderSyncCommand(userKey: UserKey): PostHttpRequestBuilder = {
		val context = new InitialFolderSyncContext(userKey, FolderSyncCommand.validInitialFolderSync)
		new FolderSyncCommand(context, wbTools).buildCommand
	}
	
	def buildInitialSyncCommand(userKey: UserKey, folderType: FolderType) = {
		buildSyncCommand(new InitialSyncContext(userKey, folderType, validSync))
	}
	
	def buildSyncCommand(userKey: UserKey, folderType: FolderType, matchers: Matcher[SyncResponse, Session]*): PostHttpRequestBuilder = {
		val matcher = null//Check.manyToOne(validSync :: matchers:_*)
		buildSyncCommand(new SyncContext(userKey, folderType, matcher))
	}
	
	def buildSyncCommand(syncContext: SyncContext) = {
		new SyncCollectionCommand(syncContext, wbTools).buildCommand
	}
	
	def buildSendInvitationCommand(invitation: InvitationContext) = {
		invitation.matcher = InvitationCommand.validSentInvitation
		new SendInvitationCommand(invitation, wbTools).buildCommand
	}
	
	def buildMeetingResponseCommand(userKey: UserKey, attendeeStatus: AttendeeStatus) = {
		val meetingResponse = new MeetingResponseContext(userKey, attendeeStatus, MeetingResponseCommand.validResponses)
		new MeetingResponseCommand(meetingResponse, wbTools).buildCommand
	}
	
}
