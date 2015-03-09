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
package org.obm.push.bean.migration;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class StatusSummary {

	public static enum Status {
		EXECUTION_ERROR(false),
		NOT_INITIALIZED(false),
		UPGRADE_REQUIRED(false),
		UPGRADE_AVAILABLE(true),
		UP_TO_DATE(true);

		private final boolean allowsStartup;

		private Status(boolean allowsStartup) {
			this.allowsStartup = allowsStartup;
		}
		
		public boolean allowsStartup() {
			return allowsStartup;
		}
	}
	
	public static Builder status(Status status) {
		return new Builder(status);
	}
	
	public static class Builder {
		
		private final Status status;
		private VersionUpdate currentVersion;
		private Version upgradeAvailable;
		private String message;

		private Builder(Status status) {
			this.status = status;
		}
		
		public Builder currentVersion(VersionUpdate currentVersion) {
			this.currentVersion = currentVersion;
			return this;
		}

		public Builder upgradeAvailable(Version upgradeAvailable) {
			this.upgradeAvailable = upgradeAvailable;
			return this;
		}

		public Builder message(String message) {
			this.message = message;
			return this;
		}
		
		public StatusSummary build() {
			Preconditions.checkNotNull(status, "status is required");
			return new StatusSummary(status, currentVersion, upgradeAvailable, message);
		}
	}
	
	private final StatusSummary.Status status;
	private final VersionUpdate currentVersion;
	private final Version upgradeAvailable;
	private final String message;

	private StatusSummary(Status status, VersionUpdate currentVersion, Version upgradeAvailable, String message) {
		this.status = status;
		this.currentVersion = currentVersion;
		this.upgradeAvailable = upgradeAvailable;
		this.message = message;
	}
	
	public StatusSummary.Status getStatus() {
		return status;
	}

	public VersionUpdate getCurrentVersion() {
		return currentVersion;
	}

	public Version getUpgradeAvailable() {
		return upgradeAvailable;
	}
	
	public String getMessage() {
		return message;
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(status, currentVersion, upgradeAvailable, message);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof StatusSummary) {
			StatusSummary that = (StatusSummary) object;
			return Objects.equal(this.status, that.status)
				&& Objects.equal(this.currentVersion, that.currentVersion)
				&& Objects.equal(this.upgradeAvailable, that.upgradeAvailable)
				&& Objects.equal(this.message, that.message);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("status", status)
			.add("currentVersion", currentVersion)
			.add("upgradeAvailable", upgradeAvailable)
			.add("message", message)
			.toString();
	}
}