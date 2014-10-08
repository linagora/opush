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
package org.obm.push.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.ProtocolVersion;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.protocol.bean.MoveItemsRequest;
import org.obm.push.protocol.bean.MoveItemsResponse;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;


public class MoveItemsProtocolTest {
	
	private MoveItemsProtocol moveItemsProtocol;
	
	@Before
	public void init() {
		moveItemsProtocol = new MoveItemsProtocol();
	}
	
	@Test
	public void testLoopWithinRequestProtocolMethods() throws Exception {
		String initialDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
				"<MoveItems>" +
				"<Move>" +
				"<SrcMsgId>22:1</SrcMsgId>" +
				"<SrcFldId>22</SrcFldId>" +
				"<DstFldId>33</DstFldId>" +
				"</Move>" +
				"<Move>" +
				"<SrcMsgId>23:2</SrcMsgId>" +
				"<SrcFldId>23</SrcFldId>" +
				"<DstFldId>34</DstFldId>" +
				"</Move>" +
				"</MoveItems>";
		
		MoveItemsRequest moveItemsRequest = moveItemsProtocol.decodeRequest(DOMUtils.parse(initialDocument));
		Document encodeRequest = moveItemsProtocol.encodeRequest(moveItemsRequest);
		
		assertThat(initialDocument).isEqualTo(DOMUtils.serialize(encodeRequest));
	}
	
	@Test
	public void testLoopWithinResponseProtocolMethods() throws Exception {
		String initialDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
				"<MoveItems>" +
				"<Response>" +
				"<Status>3</Status>" +
				"<SrcMsgId>1:2</SrcMsgId>" +
				"<DstMsgId>2:3</DstMsgId>" +
				"</Response>" +
				"<Response>" +
				"<SrcMsgId>3:4</SrcMsgId>" +
				"<Status>5</Status>" +
				"</Response>" +
				"</MoveItems>";
		
		MoveItemsResponse moveItemsResponse = moveItemsProtocol.decodeResponse(DOMUtils.parse(initialDocument));
		Device device = new Device(1, "devType", new DeviceId("devId"), new Properties(), ProtocolVersion.V121);
		Document encodeResponse = moveItemsProtocol.encodeResponse(device, moveItemsResponse);
		
		assertThat(initialDocument).isEqualTo(DOMUtils.serialize(encodeResponse));
	}
}
