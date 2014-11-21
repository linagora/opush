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

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.obm.push.exception.activesync.InvalidServerId;
import org.obm.push.protocol.bean.CollectionId;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class ServerId implements Serializable {

	private static final String SERVER_ID_SEPARATOR = ":";

	public static ServerId of(String value) {
		Iterable<String> parts = Splitter.on(SERVER_ID_SEPARATOR).split(value);
		if (Iterables.size(parts) > 2) {
			throw new InvalidServerId("Too many parts for a serverId");
		}
		Iterator<String> iterator = parts.iterator();
		return of(getCollectionId(iterator, value), getItemId(iterator));	
	}
	
	private static CollectionId getCollectionId(Iterator<String> iterator, String serverId) throws InvalidServerId {
		 try {
			 return CollectionId.of(iterator.next());
		 } catch (NoSuchElementException e) {
			 throw new InvalidServerId("serverId is invalid : " + serverId);
		 } catch (NumberFormatException e) {
			 throw new InvalidServerId("collectionId is not an integer", e);
		 }
	}

	private static Integer getItemId(Iterator<String> iterator) throws InvalidServerId {
		if (iterator.hasNext()) {
			try {
				return Integer.valueOf(iterator.next());
			} catch (NumberFormatException e) {
				throw new InvalidServerId("itemId is not an integer", e);
			}
		} else {
			return null;
		}
	}
	
	public static ServerId of(CollectionId collectionId, Integer ItemId) {
		if (collectionId == null) {
			throw new InvalidServerId("collectionId must not be null");
		}
		return new ServerId(collectionId, ItemId);
	}
	
	private final CollectionId collectionId;
	private final Integer itemId;
	
	private ServerId(CollectionId collectionId, Integer itemId) {
		this.collectionId = collectionId;
		this.itemId = itemId;
	}

	public CollectionId getCollectionId() {
		return collectionId;
	}
	
	public Integer getItemId() {
		return itemId;
	}

	public boolean isItem() {
		return itemId != null;
	}

	public boolean belongsTo(CollectionId collectionId) {
		Preconditions.checkNotNull(collectionId);
		return this.collectionId.equals(collectionId);
	}
	
	@Override
	public final int hashCode(){
		return Objects.hashCode(collectionId, itemId);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof ServerId) {
			ServerId that = (ServerId) object;
			return Objects.equal(this.collectionId, that.collectionId)
				&& Objects.equal(this.itemId, that.itemId);
		}
		return false;
	}

	public String asString() {
		if (isItem()) {
			return collectionId.asString() + SERVER_ID_SEPARATOR + itemId;
		} else {
			return collectionId.asString();
		}
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
					.add("collectionId", collectionId)
					.add("itemId", itemId)
					.toString();
	}
	
}
