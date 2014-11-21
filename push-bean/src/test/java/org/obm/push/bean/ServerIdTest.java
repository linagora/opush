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
package org.obm.push.bean;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.exception.activesync.InvalidServerId;
import org.obm.push.protocol.bean.CollectionId;



public class ServerIdTest {

	@Test(expected=InvalidServerId.class)
	public void emptyStringShouldThrowAtCreation() throws InvalidServerId {
		ServerId.of("");
	}

	@Test(expected=NullPointerException.class)
	public void nullStringShouldThrowAtCreation() throws InvalidServerId {
		ServerId.of(null);
	}

	@Test(expected=InvalidServerId.class)
	public void nonIntegerValueShouldThrowAtCreation() throws InvalidServerId {
		ServerId.of("azd");
	}
	
	@Test(expected=InvalidServerId.class)
	public void nonIntegerCollectionIdShouldThrowAtCreation() throws InvalidServerId {
		ServerId.of("azd:123");
	}
	
	@Test(expected=InvalidServerId.class)
	public void tooLargeCollectionIdShouldThrowAtCreation() throws InvalidServerId {
		ServerId.of("123456789123456");
	}
	
	@Test(expected=InvalidServerId.class)
	public void nonIntegerItemIdShouldThrowAtCreation() throws InvalidServerId {
		ServerId.of("123:abc");
	}

	@Test(expected=InvalidServerId.class)
	public void tooLargeItemIdShouldThrowAtCreation() throws InvalidServerId {
		ServerId.of("123:123456789123456");
	}

	
	@Test(expected=InvalidServerId.class)
	public void tooManyPartsStringShouldThrowAtCreation() throws InvalidServerId {
		ServerId.of("123:123:123");
	}
	
	@Test(expected=InvalidServerId.class)
	public void weirdStringValueShouldThrowAtCreation() throws InvalidServerId {
		ServerId.of(":123:123");
	}
	
	@Test
	public void collectionValueShouldBuild() throws InvalidServerId {
		ServerId serverId = ServerId.of("123");
		assertThat(serverId.getCollectionId()).isEqualTo(CollectionId.of(123));
		assertThat(serverId.getItemId()).isNull();
		assertThat(serverId.isItem()).isFalse();
	}
	
	@Test
	public void itemIdStringShouldBuild() throws InvalidServerId {
		ServerId serverId = ServerId.of("123:345");
		assertThat(serverId.getCollectionId()).isEqualTo(CollectionId.of(123));
		assertThat(serverId.getItemId()).isEqualTo(Integer.valueOf(345));
		assertThat(serverId.isItem()).isTrue();
	}
	
	@Test
	public void twoIdenticalStringsShouldBuildEqualServerId() throws InvalidServerId {
		ServerId serverId1 = ServerId.of("123:345");
		ServerId serverId2 = ServerId.of("123:345");
		assertThat(serverId1.equals(serverId2)).isTrue();
		assertThat(serverId1.hashCode()).isEqualTo(serverId2.hashCode());
	}
	
	@Test
	public void twoDifferentStringsShouldBuildDifferentServerId() throws InvalidServerId {
		ServerId serverId1 = ServerId.of("123:456");
		ServerId serverId2 = ServerId.of("123:345");
		assertThat(serverId1.hashCode()).isNotEqualTo(serverId2.hashCode());
		assertThat(serverId1).isNotEqualTo(serverId2);
	}
	
	@Test
	public void aCollectionIdShouldNotEqualAnItemId() throws InvalidServerId {
		ServerId serverId1 = ServerId.of("123");
		ServerId serverId2 = ServerId.of("123:345");
		assertThat(serverId1.hashCode()).isNotEqualTo(serverId2.hashCode());
		assertThat(serverId1).isNotEqualTo(serverId2);
	}

	@Test(expected=InvalidServerId.class)
	public void nullCollectionIdShouldThrow() {
		ServerId.of(null, 12);
	}
	
	@Test
	public void nullItemIdShouldBuildACollectionServerId() {
		ServerId actual = ServerId.of(CollectionId.of(1), null);
		assertThat(actual.asString()).isEqualTo("1");
		assertThat(actual.getCollectionId()).isEqualTo(CollectionId.of(1));
		assertThat(actual.getItemId()).isNull();
		assertThat(actual.isItem()).isFalse();
	}
	
	@Test
	public void negativeItemIdShouldBuild() {
		ServerId actual = ServerId.of(CollectionId.of(1), -10);
		assertThat(actual.asString()).isEqualTo("1:-10");
		assertThat(actual.getCollectionId()).isEqualTo(CollectionId.of(1));
		assertThat(actual.getItemId()).isEqualTo(-10);
		assertThat(actual.isItem()).isTrue();
	}
	
	@Test
	public void simpleServerIdShouldBuild() {
		ServerId actual = ServerId.of(CollectionId.of(1), 2);
		assertThat(actual.asString()).isEqualTo("1:2");
		assertThat(actual.getCollectionId()).isEqualTo(CollectionId.of(1));
		assertThat(actual.getItemId()).isEqualTo(2);
		assertThat(actual.isItem()).isTrue();
	}
	
	@Test(expected=NullPointerException.class)
	public void belongsToThrowOnNullPointer() {
		ServerId.of(CollectionId.of(1), 2).belongsTo(null);
	}
	
	@Test
	public void belongsToIsTrueOnSameCollection() {
		assertThat(ServerId.of(CollectionId.of(1), 2).belongsTo(CollectionId.of(1))).isTrue();
	}
	
	@Test
	public void belongsToIsFalseOnDifferentCollection() {
		assertThat(ServerId.of(CollectionId.of(1), 2).belongsTo(CollectionId.of(2))).isFalse();
	}
}
