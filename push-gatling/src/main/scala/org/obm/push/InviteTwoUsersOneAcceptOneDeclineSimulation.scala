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
import io.gatling.core.Predef.scenario
import io.gatling.core.check.Matcher
import io.gatling.core.session.Session
import io.gatling.core.validation.Success
import io.gatling.http.Predef.http
import io.gatling.http.Predef.httpProtocolBuilder2HttpProtocol
import io.gatling.http.Predef.requestBuilder2ActionBuilder
import io.gatling.http.request.builder.PostHttpRequestBuilder

class InviteTwoUsersOneAcceptOneDeclineSimulation extends Simulation {

	val wbTools: WBXMLTools = new WBXMLTools
	val configuration: Configuration = GatlingConfiguration.build

	val usedMailCollection = FolderType.DEFAULT_INBOX_FOLDER 
	val usedCalendarCollection = FolderType.DEFAULT_CALENDAR_FOLDER
	
	val organizerKey = new UserKey("organizer")
	val attendee1Key = new UserKey("attendee1")
	val attendee2Key = new UserKey("attendee2")
	val invitation = new InvitationContext(
		organizer = organizerKey,
		attendees = Set(attendee1Key, attendee2Key),
		startTime = date("2014-01-12T09:00:00"),
		endTime = date("2014-01-12T10:00:00"),
		folderType = usedCalendarCollection)
	
		
	val users = for (userNumber <- Iterator.range(1, 100)) yield null//new User(userNumber, configuration)
	
	val httpConf = http
		.baseURL(configuration.baseUrl)
		.disableFollowRedirect
		.disableCaching
	
	var organizer: User = null
	var attendee1: User = null
	var attendee2: User = null
	
	for (user <- users) {
		Breaks.breakable {
			if (organizer == null) {
				organizer = user
				Breaks.break
			}
			if (attendee1 == null) {
				attendee1 = user
				Breaks.break
			}
			if (attendee2 == null) {
				attendee2 = user
			}
			
			setUp(buildScenario(organizer, attendee1, attendee2).inject(
			    nothingFor(60 seconds),
			    constantRate(configuration.usersPerSec userPerSec) during (configuration.duration)
			)).protocols(httpConf)
	
			organizer = null
			attendee1 = null
			attendee2 = null
		}
	}

	def buildScenario(organizer: User, attendee1: User, attendee2: User) = {

		scenario("Send an invitation at two attendees for organizer: " + organizer.login)
			.exitBlockOnFail(
				exec(ProvisioningCommand.buildInitialProvisioningCommand(organizerKey))
				.exec(ProvisioningCommand.buildAcceptProvisioningCommand(organizerKey))
				.exec(ProvisioningCommand.buildInitialProvisioningCommand(attendee1Key))
				.exec(ProvisioningCommand.buildAcceptProvisioningCommand(attendee1Key))
				.exec(ProvisioningCommand.buildInitialProvisioningCommand(attendee2Key))
				.exec(ProvisioningCommand.buildAcceptProvisioningCommand(attendee2Key))
				.exec(buildInitialFolderSyncCommand(organizerKey))
				.exec(buildInitialFolderSyncCommand(attendee1Key))
				.exec(buildInitialFolderSyncCommand(attendee2Key))
				.exec(buildInitialSyncCommand(organizerKey, usedCalendarCollection))
				.exec(s => Success(organizerKey.sessionHelper.setupNextInvitationClientId(s)))
				.exec(buildSendInvitationCommand(invitation))
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
	}
	
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
