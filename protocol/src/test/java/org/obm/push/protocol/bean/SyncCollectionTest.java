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
import static org.assertj.guava.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.bean.EncodedSyncCollectionCommandRequest;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.MimeSupport;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.change.SyncCommand;

public class SyncCollectionTest {

	@Test
	public void testBuilderIdIsNotRequired() {
		SyncCollection syncRequestCollection = builderWithRequirement().collectionId(null).build();
		
		assertThat(syncRequestCollection.getCollectionId()).isNull();
	}

	@Test
	public void testBuilderIdValid() {
		SyncCollection syncRequestCollection = builderWithRequirement().collectionId(CollectionId.of(135)).build();
		
		assertThat(syncRequestCollection.getCollectionId()).isEqualTo(CollectionId.of(135));
	}
	
	@Test
	public void testBuilderSyncKeyIsNotRequired() {
		SyncCollection syncRequestCollection = builderWithRequirement().syncKey(null).build();
		
		assertThat(syncRequestCollection.getSyncKey()).isNull();
	}

	@Test
	public void testBuilderSyncKeyValid() {
		SyncCollection syncRequestCollection = builderWithRequirement().syncKey(new SyncKey("blabla")).build();
		
		assertThat(syncRequestCollection.getSyncKey()).isEqualTo(new SyncKey("blabla"));
	}
	
	@Test
	public void testBuilderDataClassIsNotRequired() {
		SyncCollection syncRequestCollection = builderWithRequirement().dataType(null).build();
		
		assertThat(syncRequestCollection.getDataType()).isNull();
	}

	@Test
	public void testBuilderDataClassValid() {
		SyncCollection syncRequestCollection = builderWithRequirement().dataType(PIMDataType.EMAIL).build();
		
		assertThat(syncRequestCollection.getDataType()).isEqualTo(PIMDataType.EMAIL);
	}
	
	@Test
	public void testBuilderWindowSizeIsNotRequired() {
		SyncCollection syncRequestCollection = builderWithRequirement().windowSize(null).build();
		
		assertThat(syncRequestCollection.getWindowSize()).isAbsent();
	}

	@Test
	public void testBuilderWindowSizeValid() {
		SyncCollection syncRequestCollection = builderWithRequirement().windowSize(5).build();
		
		assertThat(syncRequestCollection.getWindowSize()).isPresent();
		assertThat(syncRequestCollection.getWindowSize().get()).isEqualTo(5);
	}
	
	@Test
	public void testBuilderOptionsIsNotRequired() {
		SyncCollection syncRequestCollection = builderWithRequirement().options(null).build();
		
		assertThat(syncRequestCollection.getOptions()).isNull();
	}

	@Test
	public void testBuilderOptionsValid() {
		SyncCollectionOptions options = SyncCollectionOptions.builder()
				.filterType(FilterType.ONE_DAY_BACK)
				.conflict(2)
				.mimeTruncation(3)
				.mimeSupport(MimeSupport.ALWAYS)
				.build();
		
		SyncCollection syncRequestCollection = builderWithRequirement().options(options).build();
		
		assertThat(syncRequestCollection.getOptions()).isEqualTo(options);
	}
	
	@Test
	public void testHasOptionsWhenNull() {
		SyncCollection syncRequestCollection = builderWithRequirement().options(null).build();
		
		assertThat(syncRequestCollection.hasOptions()).isFalse();
	}

	@Test
	public void testHasOptionsWhenValid() {
		SyncCollection syncRequestCollection = builderWithRequirement().options(SyncCollectionOptions.builder().build()).build();
		
		assertThat(syncRequestCollection.hasOptions()).isTrue();
	}

	@Test(expected=NullPointerException.class)
	public void nullCommandShouldThrow() {
		builderWithRequirement().command(null).build();
	}
	
	@Test
	public void testBuilderCommandsIsNotRequired() {
		SyncCollection syncRequestCollection = builderWithRequirement().build();
		assertThat(syncRequestCollection.getCommands()).isEmpty();
	}

	@Test
	public void testBuilderCommandsValid() {
		EncodedSyncCollectionCommandRequest command = 
				EncodedSyncCollectionCommandRequest.builder()
					.serverId(CollectionId.of(1).serverId(35)).type(SyncCommand.DELETE).build();
		
		SyncCollection syncRequestCollection = builderWithRequirement()
				.command(command)
				.build();
		
		assertThat(syncRequestCollection.getCommands()).containsOnly(command);
	}

	private SyncCollection.Builder builderWithRequirement() {
		return SyncCollection.builder()
			.collectionId(CollectionId.of(140))
			.syncKey(new SyncKey("1234"));
	}
}
