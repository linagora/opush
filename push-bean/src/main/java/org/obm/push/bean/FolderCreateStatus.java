/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015  Linagora
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

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public enum FolderCreateStatus {

	OK("1"),
	ALREADY_EXISTS("2"),
	PARENT_FOLDER_NOT_FOUND("5"),
	SERVER_ERROR("6"),
	ACCESS_DENIED("7"),
	REQUEST_TIMED_OUT("8"),
	INVALID_SYNC_KEY("9"),
	INCORRECTLY_FORMATTED_REQUEST("10"),
	UNKNOWN_ERROR("11");

	private final String specificationValue;

	private FolderCreateStatus(String asSpecificationValue) {
		this.specificationValue = asSpecificationValue;
	}
	
	public String asSpecificationValue() {
		return specificationValue;
	}
	
	public static FolderCreateStatus fromSpecificationValue(String specificationValue) {
		if (specValueToEnum.containsKey(specificationValue)) {
			return specValueToEnum.get(specificationValue);
		}
		throw new IllegalArgumentException("No FolderCreateStatus for '" + specificationValue + "'");
	}

	private static Map<String, FolderCreateStatus> specValueToEnum;
	
	static {
		Builder<String, FolderCreateStatus> builder = ImmutableMap.builder();
		for (FolderCreateStatus folderCreateStatus : values()) {
			builder.put(folderCreateStatus.specificationValue, folderCreateStatus);
		}
		specValueToEnum = builder.build();
	}
}
