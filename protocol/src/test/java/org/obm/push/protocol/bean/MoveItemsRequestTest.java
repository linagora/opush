/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
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
package org.obm.push.protocol.bean;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.bean.MoveItem;
import org.obm.push.bean.Summary;

public class MoveItemsRequestTest {

	@Test
	public void summaryWhenNothing() {
		MoveItemsRequest request = MoveItemsRequest.builder().build();

		assertThat(request.getSummary()).isEqualTo(
			Summary.empty());
	}

	@Test
	public void summaryWhenSome() {
		MoveItemsRequest request = MoveItemsRequest.builder()
			.add(MoveItem.builder()
				.destinationFolderId("dest1")
				.sourceFolderId("src1")
				.sourceMessageId("1").build())
			.add(MoveItem.builder()
				.destinationFolderId("dest2")
				.sourceFolderId("src2")
				.sourceMessageId("2").build())
			.add(MoveItem.builder()
				.destinationFolderId("dest3")
				.sourceFolderId("src3")
				.sourceMessageId("3").build())
			.build();

		assertThat(request.getSummary()).isEqualTo(
			Summary.builder().changeCount(3).build());
	}
	
}
