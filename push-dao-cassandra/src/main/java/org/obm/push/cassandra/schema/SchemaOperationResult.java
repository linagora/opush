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
package org.obm.push.cassandra.schema;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class SchemaOperationResult {

	public static SchemaOperationResult success(String message) {
		return result(CQLScriptExecutionStatus.OK, message);
	}

	public static SchemaOperationResult error(String message) {
		return result(CQLScriptExecutionStatus.ERROR, message);
	}
	
	private static SchemaOperationResult result(CQLScriptExecutionStatus status, String message) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(message), "message required");
		return new SchemaOperationResult(status, message);
	}

	private final CQLScriptExecutionStatus status;
	private final String message;

	private SchemaOperationResult(CQLScriptExecutionStatus status, String message) {
		this.status = status;
		this.message = message;
	}
	
	public CQLScriptExecutionStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(status, message);
	}
	
	@Override
	public final boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object instanceof SchemaOperationResult) {
			SchemaOperationResult that = (SchemaOperationResult) object;
			return Objects.equal(this.status, that.status)
				&& Objects.equal(this.message, that.message);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("status", status)
				.add("message", message)
				.toString();
	}
}
