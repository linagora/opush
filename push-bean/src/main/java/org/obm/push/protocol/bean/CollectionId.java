/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014  Linagora
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

import java.io.Serializable;

import org.obm.push.bean.ServerId;
import org.obm.push.bean.Stringable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class CollectionId implements Serializable, Stringable {

	public static final CollectionId ROOT = of(0);
	
	public static CollectionId of(String id) {
		Preconditions.checkNotNull(id);
		Integer intValue = Integer.valueOf(id);
		return of(intValue);
	}

	public static CollectionId of(int collectionId) {
		Preconditions.checkArgument(collectionId >= 0);
		return new CollectionId(collectionId);
	}
	
	private int id;
	
	private CollectionId(int collectionId) {
		this.id = collectionId;
	}

	@Override
	public String asString() {
		return String.valueOf(id);
	}
	
	public int asInt() {
		return id;
	}

	public ServerId serverId(int itemId) {
		return ServerId.of(this, itemId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CollectionId) {
			return id == ((CollectionId)obj).id;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("id", id)
			.toString();
	}
}
