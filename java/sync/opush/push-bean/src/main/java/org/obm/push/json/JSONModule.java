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
package org.obm.push.json;

import java.nio.charset.Charset;

import org.obm.push.utils.SerializableInputStream;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.MapType;

public class JSONModule extends Module {

	private final static String JSON_MODULE_NAME = "JSONModule";
	
	@Override
	public String getModuleName() {
		return JSON_MODULE_NAME;
	}

	@Override
	public Version version() {
		return PackageVersion.VERSION;
	}

	@Override
	public void setupModule(SetupContext context) {
		context.addSerializers(new JSONSerializers());
		context.addDeserializers(new JSONDeserializers());
	}
	
	private class JSONSerializers implements Serializers {

		@Override
		public JsonSerializer<?> findSerializer(SerializationConfig config,
				JavaType type, BeanDescription beanDesc) {
			Class<?> clazz = type.getRawClass();
			if (Charset.class.isAssignableFrom(clazz)) {
				return new CharsetSerializer();
			}
			if (SerializableInputStream.class.isAssignableFrom(clazz)) {
				return new SerializableInputStreamSerializer();
			}
			return null;
		}

		@Override
		public JsonSerializer<?> findArraySerializer(
				SerializationConfig config, ArrayType type,
				BeanDescription beanDesc, TypeSerializer elementTypeSerializer,
				JsonSerializer<Object> elementValueSerializer) {
			return null;
		}

		@Override
		public JsonSerializer<?> findCollectionSerializer(
				SerializationConfig config, CollectionType type,
				BeanDescription beanDesc, TypeSerializer elementTypeSerializer,
				JsonSerializer<Object> elementValueSerializer) {
			return null;
		}

		@Override
		public JsonSerializer<?> findCollectionLikeSerializer(
				SerializationConfig config, CollectionLikeType type,
				BeanDescription beanDesc, TypeSerializer elementTypeSerializer,
				JsonSerializer<Object> elementValueSerializer) {
			return null;
		}

		@Override
		public JsonSerializer<?> findMapSerializer(SerializationConfig config,
				MapType type, BeanDescription beanDesc,
				JsonSerializer<Object> keySerializer,
				TypeSerializer elementTypeSerializer,
				JsonSerializer<Object> elementValueSerializer) {
			return null;
		}

		@Override
		public JsonSerializer<?> findMapLikeSerializer(
				SerializationConfig config, MapLikeType type,
				BeanDescription beanDesc, JsonSerializer<Object> keySerializer,
				TypeSerializer elementTypeSerializer,
				JsonSerializer<Object> elementValueSerializer) {
			return null;
		}
		
	}
	
	private class JSONDeserializers implements Deserializers {

		@Override
		public JsonDeserializer<?> findArrayDeserializer(ArrayType type,
				DeserializationConfig config, BeanDescription beanDesc,
				TypeDeserializer elementTypeDeserializer,
				JsonDeserializer<?> elementDeserializer)
				throws JsonMappingException {
			return null;
		}

		@Override
		public JsonDeserializer<?> findCollectionDeserializer(
				CollectionType type, DeserializationConfig config,
				BeanDescription beanDesc,
				TypeDeserializer elementTypeDeserializer,
				JsonDeserializer<?> elementDeserializer)
				throws JsonMappingException {
			return null;
		}

		@Override
		public JsonDeserializer<?> findCollectionLikeDeserializer(
				CollectionLikeType type, DeserializationConfig config,
				BeanDescription beanDesc,
				TypeDeserializer elementTypeDeserializer,
				JsonDeserializer<?> elementDeserializer)
				throws JsonMappingException {
			return null;
		}

		@Override
		public JsonDeserializer<?> findEnumDeserializer(Class<?> type,
				DeserializationConfig config, BeanDescription beanDesc)
				throws JsonMappingException {
			return null;
		}

		@Override
		public JsonDeserializer<?> findMapDeserializer(MapType type,
				DeserializationConfig config, BeanDescription beanDesc,
				KeyDeserializer keyDeserializer,
				TypeDeserializer elementTypeDeserializer,
				JsonDeserializer<?> elementDeserializer)
				throws JsonMappingException {
			return null;
		}

		@Override
		public JsonDeserializer<?> findMapLikeDeserializer(MapLikeType type,
				DeserializationConfig config, BeanDescription beanDesc,
				KeyDeserializer keyDeserializer,
				TypeDeserializer elementTypeDeserializer,
				JsonDeserializer<?> elementDeserializer)
				throws JsonMappingException {
			return null;
		}

		@Override
		public JsonDeserializer<?> findTreeNodeDeserializer(
				Class<? extends JsonNode> nodeType,
				DeserializationConfig config, BeanDescription beanDesc)
				throws JsonMappingException {
			return null;
		}

		@Override
		public JsonDeserializer<?> findBeanDeserializer(JavaType type,
				DeserializationConfig config, BeanDescription beanDesc)
				throws JsonMappingException {
			Class<?> clazz = type.getRawClass();
			if (Charset.class.isAssignableFrom(clazz)) {
				return new CharsetDeserializer();
			}
			if (SerializableInputStream.class.isAssignableFrom(clazz)) {
				return new SerializableInputStreamDeserializer();
			}
			return null;
		}
		
	}
}
