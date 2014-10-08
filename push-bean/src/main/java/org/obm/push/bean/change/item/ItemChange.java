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
package org.obm.push.bean.change.item;

import java.io.Serializable;

import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.ms.MSEmail;

import com.google.common.base.Objects;

public class ItemChange implements ASItem, Serializable {
	
	private static final long serialVersionUID = 4575240618131116466L;
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private IApplicationData data;
		private ServerId serverId;
		private boolean isNew;
		
		private Builder() {
		}
		
		public Builder data(IApplicationData data) {
			this.data = data;
			return this;
		}
		
		public Builder serverId(ServerId serverId) {
			this.serverId = serverId;
			return this;
		}
		
		public Builder isNew(boolean isNew) {
			this.isNew = isNew;
			return this;
		}
		
		public ItemChange build() {
			return new ItemChange(data, serverId, isNew);
		}
	}
	
	private final IApplicationData data;
	private final ServerId serverId;
	private final boolean isNew;

	private ItemChange(IApplicationData data, ServerId serverId, boolean isNew) {
		this.data = data;
		this.serverId = serverId;
		this.isNew = isNew;
	}
	
	@Override
	public ServerId getServerId() {
		return serverId;
	}

	public IApplicationData getData() {
		return data;
	}

	public boolean isNew() {
		return isNew;
	}

	public boolean isMSEmail() {
		if (getData() instanceof MSEmail) {
			return true;
		}
		return false;
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(serverId, isNew, data);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof ItemChange) {
			ItemChange that = (ItemChange) object;
			return Objects.equal(this.serverId, that.serverId)
				&& Objects.equal(this.isNew, that.isNew)
				&& Objects.equal(this.data, that.data);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("serverId", serverId)
			.add("isNew", isNew)
			.toString();
	}
}