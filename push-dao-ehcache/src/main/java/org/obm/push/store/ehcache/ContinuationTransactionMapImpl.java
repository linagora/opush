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
package org.obm.push.store.ehcache;

import java.util.concurrent.ConcurrentMap;

import org.obm.push.ContinuationTransactionMap;
import org.obm.push.ElementNotFoundException;
import org.obm.push.bean.Device;
import org.obm.push.bean.User;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ContinuationTransactionMapImpl<T> implements ContinuationTransactionMap<T> {

	public static class Key {
		
		private final User user;
		private final Device device;
		
		public Key(User user, Device device) {
			this.user = user;
			this.device = device;
		}
		
		@Override
		public int hashCode(){
			return Objects.hashCode(user, device);
		}
		
		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (object instanceof Key) {
				Key that = (Key) object;
				return Objects.equal(this.user, that.user) && 
						Objects.equal(this.device, that.device);
			}
			return false;
		}
	}
	

	private final ConcurrentMap<Key, T> continuations;
	
	@Inject
	@VisibleForTesting ContinuationTransactionMapImpl() {
		continuations = new MapMaker().<Key, T>makeMap();
	}
	
	@Override
	public T getContinuationForDevice(User user, Device device) throws ElementNotFoundException {
		T element = continuations.get(new Key(user, device));
		if (element == null) {
			throw new ElementNotFoundException();
		}
		return element;
	}

	
	@Override
	public boolean putContinuationForDevice(User user, Device device, T continuation) {
		return continuations.put(new Key(user, device), continuation) != null;
	}
	
	@Override
	public void delete(User user, Device device) {
		continuations.remove(new Key(user, device));
	}
}
