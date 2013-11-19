/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.push.cassandra;

import java.io.IOException;

import org.obm.push.json.JSONModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JSONService {
	
	private final static Logger logger = LoggerFactory.getLogger(JSONService.class);
	
	@Inject
	@VisibleForTesting JSONService() {}
	
	public <T> T deserialize(Class<T> t, String json) {
		T value = null;
		try {
			value = (T) anyVisibilityObjectMapper().readValue(json, 
					TypeFactory.defaultInstance().constructFromCanonical(t.getCanonicalName())); 
		} catch (IOException e) {
			logger.error("Error on deserialize", e);
			Throwables.propagate(e);
		}
		return value;
	}
	
	public String serialize(Object object) {
		String value = null;
		try {
			value = anyVisibilityObjectMapper().writeValueAsString(object); 
		} catch (IOException e) {
			logger.error("Error on serialize", e);
			Throwables.propagate(e);
		}
		return value;
	}

	private ObjectMapper anyVisibilityObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDefaultTyping(StdTypeResolverBuilder.noTypeInfoBuilder());
		objectMapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(Visibility.ANY));
		objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
		objectMapper.registerModule(new GuavaModule());
		objectMapper.registerModule(new JSONModule());
		return objectMapper;
	}

}
