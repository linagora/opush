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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

import org.obm.push.backend.WindowingContact;
import org.obm.push.backend.WindowingEvent;
import org.obm.push.bean.AbstractSyncCollection;
import org.obm.push.bean.AnalysedSyncCollection;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.Credentials;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.MSAddress;
import org.obm.push.bean.MSEmailHeader;
import org.obm.push.bean.MSEventExtId;
import org.obm.push.bean.MSEventUid;
import org.obm.push.bean.SyncCollectionCommandRequest;
import org.obm.push.bean.SyncCollectionCommandResponse;
import org.obm.push.bean.SyncCollectionCommands;
import org.obm.push.bean.SyncCollectionCommandsRequest;
import org.obm.push.bean.SyncCollectionCommandsResponse;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.bean.SyncCollectionRequest;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;
import org.obm.push.bean.change.item.ItemChange;
import org.obm.push.bean.change.item.ItemDeletion;
import org.obm.push.bean.ms.MSEmail;
import org.obm.push.bean.ms.MSEmailBody;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequest;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequestCategory;
import org.obm.push.bean.msmeetingrequest.MSMeetingRequestRecurrence;
import org.obm.push.json.mixin.AbstractSyncCollectionMixIn;
import org.obm.push.json.mixin.AnalysedSyncCollectionMixIn;
import org.obm.push.json.mixin.BodyPreferenceMixIn;
import org.obm.push.json.mixin.CredentialsMixIn;
import org.obm.push.json.mixin.DeviceIdMixIn;
import org.obm.push.json.mixin.DeviceMixIn;
import org.obm.push.json.mixin.EmailChangesMixIn;
import org.obm.push.json.mixin.EmailMixIn;
import org.obm.push.json.mixin.IApplicationDataMixIn;
import org.obm.push.json.mixin.ItemChangeMixIn;
import org.obm.push.json.mixin.ItemDeletionMixIn;
import org.obm.push.json.mixin.MSAddressMixIn;
import org.obm.push.json.mixin.MSEmailBodyMixIn;
import org.obm.push.json.mixin.MSEmailHeaderMixIn;
import org.obm.push.json.mixin.MSEmailMixIn;
import org.obm.push.json.mixin.MSEventExtIdMixIn;
import org.obm.push.json.mixin.MSEventUidMixIn;
import org.obm.push.json.mixin.MSMeetingRequestCategoryMixIn;
import org.obm.push.json.mixin.MSMeetingRequestMixIn;
import org.obm.push.json.mixin.MSMeetingRequestRecurrenceMixIn;
import org.obm.push.json.mixin.SnapshotMixIn;
import org.obm.push.json.mixin.SyncCollectionCommandRequestMixIn;
import org.obm.push.json.mixin.SyncCollectionCommandResponseMixIn;
import org.obm.push.json.mixin.SyncCollectionCommandsMixIn;
import org.obm.push.json.mixin.SyncCollectionCommandsRequestMixIn;
import org.obm.push.json.mixin.SyncCollectionCommandsResponseMixIn;
import org.obm.push.json.mixin.SyncCollectionOptionsMixIn;
import org.obm.push.json.mixin.SyncCollectionRequestMixIn;
import org.obm.push.json.mixin.SyncKeyMixIn;
import org.obm.push.json.mixin.UserMixIn;
import org.obm.push.json.mixin.WindowingContactMixIn;
import org.obm.push.json.mixin.WindowingEventMixIn;
import org.obm.push.json.serializer.CharsetDeserializer;
import org.obm.push.json.serializer.CharsetSerializer;
import org.obm.push.json.serializer.SerializableInputStreamDeserializer;
import org.obm.push.json.serializer.SerializableInputStreamSerializer;
import org.obm.push.mail.EmailChanges;
import org.obm.push.mail.bean.Email;
import org.obm.push.mail.bean.Snapshot;
import org.obm.push.utils.SerializableInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JSONService {
	
	private final static Logger logger = LoggerFactory.getLogger(JSONService.class);
	
	@Inject
	protected JSONService() {}
	
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
	
	public <T> Set<T> deserializeSet(final Class<T> t, Set<String> jsons) {
		return FluentIterable.from(jsons)
				.transform(new Function<String, T>() {

					@Override
					public T apply(String input) {
						return deserialize(t, input);
					}
				})
				.toSet();
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
	
	public <T> Set<String> serializeSet(Set<T> objects) {
		return FluentIterable.from(objects)
			.transform(new Function<T, String>() {

				@Override
				public String apply(T input) {
					return serialize(input);
				}
			})
			.toSet();
	}

	private ObjectMapper anyVisibilityObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(Visibility.ANY));
		return objectMapper
				.setDefaultTyping(StdTypeResolverBuilder.noTypeInfoBuilder())
				.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
				.registerModules(
					new GuavaModule(), 
					buildOpushBeansSerializers(), 
					buildOpushBeansMixIns());
	}

	private SimpleModule buildOpushBeansMixIns() {
		return new SimpleModule("OpushBeansMixIns", PackageVersion.VERSION)
			.setMixInAnnotation(IApplicationData.class, IApplicationDataMixIn.class)
			.setMixInAnnotation(ItemChange.class, ItemChangeMixIn.class)
			.setMixInAnnotation(ItemDeletion.class, ItemDeletionMixIn.class)
			.setMixInAnnotation(Credentials.class, CredentialsMixIn.class)
			.setMixInAnnotation(Device.class, DeviceMixIn.class)
			.setMixInAnnotation(DeviceId.class, DeviceIdMixIn.class)
			.setMixInAnnotation(AbstractSyncCollection.class, AbstractSyncCollectionMixIn.class)
			.setMixInAnnotation(AnalysedSyncCollection.class, AnalysedSyncCollectionMixIn.class)
			.setMixInAnnotation(BodyPreference.class, BodyPreferenceMixIn.class)
			.setMixInAnnotation(Email.class, EmailMixIn.class)
			.setMixInAnnotation(EmailChanges.class, EmailChangesMixIn.class)
			.setMixInAnnotation(MSAddress.class, MSAddressMixIn.class)
			.setMixInAnnotation(MSEmailBody.class, MSEmailBodyMixIn.class)
			.setMixInAnnotation(MSEmailHeader.class, MSEmailHeaderMixIn.class)
			.setMixInAnnotation(MSEmail.class, MSEmailMixIn.class)
			.setMixInAnnotation(MSEventExtId.class, MSEventExtIdMixIn.class)
			.setMixInAnnotation(MSEventUid.class, MSEventUidMixIn.class)
			.setMixInAnnotation(MSMeetingRequestCategory.class, MSMeetingRequestCategoryMixIn.class)
			.setMixInAnnotation(MSMeetingRequest.class, MSMeetingRequestMixIn.class)
			.setMixInAnnotation(MSMeetingRequestRecurrence.class, MSMeetingRequestRecurrenceMixIn.class)
			.setMixInAnnotation(Snapshot.class, SnapshotMixIn.class)
			.setMixInAnnotation(SyncCollectionCommandRequest.class, SyncCollectionCommandRequestMixIn.class)
			.setMixInAnnotation(SyncCollectionCommandResponse.class, SyncCollectionCommandResponseMixIn.class)
			.setMixInAnnotation(SyncCollectionCommands.class, SyncCollectionCommandsMixIn.class)
			.setMixInAnnotation(SyncCollectionCommandsRequest.class, SyncCollectionCommandsRequestMixIn.class)
			.setMixInAnnotation(SyncCollectionCommandsResponse.class, SyncCollectionCommandsResponseMixIn.class)
			.setMixInAnnotation(SyncCollectionCommandsResponse.class, SyncCollectionCommandsResponseMixIn.class)
			.setMixInAnnotation(SyncCollectionOptions.class, SyncCollectionOptionsMixIn.class)
			.setMixInAnnotation(SyncCollectionRequest.class, SyncCollectionRequestMixIn.class)
			.setMixInAnnotation(SyncKey.class, SyncKeyMixIn.class)
			.setMixInAnnotation(User.class, UserMixIn.class)
			.setMixInAnnotation(WindowingContact.class, WindowingContactMixIn.class)
			.setMixInAnnotation(WindowingEvent.class, WindowingEventMixIn.class);
	}

	private SimpleModule buildOpushBeansSerializers() {
		return new SimpleModule("OpushBeansSerializers", PackageVersion.VERSION)
			.addSerializer(Charset.class, new CharsetSerializer())
			.addDeserializer(Charset.class, new CharsetDeserializer())
			.addSerializer(SerializableInputStream.class, new SerializableInputStreamSerializer())
			.addDeserializer(SerializableInputStream.class, new SerializableInputStreamDeserializer());
	}

}
