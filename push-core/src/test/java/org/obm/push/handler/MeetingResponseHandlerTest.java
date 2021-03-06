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
package org.obm.push.handler;

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expectLastCall;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.ICalendarBackend;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.bean.AttendeeStatus;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.MeetingResponse;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.User;
import org.obm.push.bean.User.Factory;
import org.obm.push.bean.UserDataRequest;
import org.obm.push.impl.DOMDumper;
import org.obm.push.mail.MailBackend;
import org.obm.push.protocol.MeetingProtocol;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.wbxml.WBXMLTools;


public class MeetingResponseHandlerTest {

	private IMocksControl control;
	private User user;
	private Device device;
	private UserDataRequest udr;
	
	private MeetingResponseHandler meetingResponseHandler;
	private IContentsImporter contentsImporter;
	private MeetingProtocol meetingProtocol;
	private MailBackend mailBackend;
	private WBXMLTools wbxmlTools;
	private ICalendarBackend calendarBackend;
	private DOMDumper domDumper;
	
	@Before
	public void setup() {
		control = createControl();
		contentsImporter = control.createMock(IContentsImporter.class);
		meetingProtocol = control.createMock(MeetingProtocol.class);
		mailBackend = control.createMock(MailBackend.class);
		wbxmlTools = control.createMock(WBXMLTools.class);
		calendarBackend = control.createMock(ICalendarBackend.class);
		domDumper = control.createMock(DOMDumper.class);
		
		meetingResponseHandler = new MeetingResponseHandler(contentsImporter,
				meetingProtocol, mailBackend,
				wbxmlTools, calendarBackend, domDumper);
		
		user = Factory.create().createUser("test@test", "test@domain", "displayName");
		device = new Device.Factory().create(null, "iPhone", "iOs 5", new DeviceId("my phone"), null);
		udr = new UserDataRequest(new Credentials(user, "password".toCharArray()), "noCommand", device);
	}
	
	@Test
	public void TestDeleteInvitationEmailGoesToTrash() throws Exception {
		CollectionId collectionId = CollectionId.of(1);
		ServerId reqId = CollectionId.of(1).serverId(2);
		
		contentsImporter.importMessageDeletion(udr, PIMDataType.EMAIL, collectionId, reqId, true);
		expectLastCall();
		
		control.replay();
		
		meetingResponseHandler.deleteInvitationEmail(udr, MeetingResponse.builder()
				.userResponse(AttendeeStatus.ACCEPT)
				.collectionId(collectionId)
				.reqId(reqId)
				.build());
		
		control.verify();
	}
}
