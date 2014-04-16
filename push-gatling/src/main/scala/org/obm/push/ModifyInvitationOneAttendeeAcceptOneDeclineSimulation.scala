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

import org.obm.DateUtils.date
import org.obm.push.bean.AttendeeStatus
import org.obm.push.checks.Check
import org.obm.push.command.InvitationCommand
import org.obm.push.command.InvitationContext
import org.obm.push.command.ModifyInvitationCommand
import org.obm.push.command.SyncCollectionCommand.atLeastOneDeleteResponse
import org.obm.push.command.SyncCollectionCommand.atLeastOneMeetingRequest
import org.obm.push.context.User

import io.gatling.core.Predef.bootstrap.exec
import io.gatling.core.session.Session
import io.gatling.core.validation.Success
import io.gatling.http.Predef.requestBuilder2ActionBuilder

class ModifyInvitationOneAttendeeAcceptOneDeclineSimulation extends InviteTwoUsersOneAcceptOneDeclineSimulation {

	override def buildScenario(organizer: User, attendee1: User, attendee2: User) = {
		super.buildScenario(organizer, attendee1, attendee2).exitHereIfFailed.exitBlockOnFail(
			exec(buildModifyInvitationCommand(invitation))
			.exec(s => Success(organizerKey.sessionHelper.updatePendingInvitation(s)))
			.pause(configuration.asynchronousChangeTime)
			.exec(buildSyncCommand(attendee1Key, usedMailCollection, atLeastOneMeetingRequest)) // Change notification reception
			.exec(buildSyncCommand(attendee2Key, usedMailCollection, atLeastOneMeetingRequest)) // Change notification reception
			.exec(buildMeetingResponseCommand(attendee1Key, AttendeeStatus.DECLINE))
			.exec(buildMeetingResponseCommand(attendee2Key, AttendeeStatus.ACCEPT))
			.exec(buildSyncCommand(attendee1Key, usedMailCollection, atLeastOneDeleteResponse)) // notification deletion
			.exec(buildSyncCommand(attendee2Key, usedMailCollection, atLeastOneDeleteResponse)) // notification deletion
			.pause(configuration.asynchronousChangeTime)
			.exec(buildSyncCommand(organizerKey, usedCalendarCollection, Check.matcher((s, response) 
					=> (organizerKey.sessionHelper.attendeeRepliesAreReceived(s.asInstanceOf[Session], response.get), "Each users havn't replied"))))
		)
	}
	
	def buildModifyInvitationCommand(invitation: InvitationContext) = {
		val modifiedInvitation = invitation.modify(
				startTime = date("2014-01-14T09:00:00"),
				endTime = date("2014-01-14T11:00:00"),
				matcher = InvitationCommand.validModifiedInvitation)
		new ModifyInvitationCommand(modifiedInvitation, wbTools).buildCommand
	}
}
