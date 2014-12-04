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
package org.obm.push.protocol.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.EncodedApplicationData;
import org.obm.push.bean.EncodedSyncCollectionCommandRequest;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.MSContact;
import org.obm.push.bean.MSEmailBodyType;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncCollectionCommandResponse;
import org.obm.push.bean.SyncCollectionResponse;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.exception.activesync.ASRequestBooleanFieldException;
import org.obm.push.exception.activesync.ASRequestIntegerFieldException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.protocol.bean.SyncCollection;
import org.obm.push.protocol.bean.SyncRequest;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.ms.MSEmailDecoder;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Iterables;
import com.google.inject.Provider;


public class SyncDecoderTest {

	static final int DEFAULT_WINDOW_SIZE = 100;
	
	Provider<CalendarDecoder> calendarProvider;
	Provider<ContactDecoder> contactProvider;
	Provider<TaskDecoder> taskProvider;
	Provider<MSEmailDecoder> emailProvider;
	DecoderFactory decoder;
	
	@Before
	public void setUp() {
		Provider<ContactDecoder> contactProvider = new Provider<ContactDecoder>() {

			@Override
			public ContactDecoder get() {
				return new ContactDecoder(null, null);
			}
		};
		decoder = new DecoderFactory(calendarProvider, contactProvider, taskProvider, emailProvider);
	}
	
	@Test(expected=ASRequestIntegerFieldException.class)
	public void testGetWaitWhenNotANumber() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
				"<Sync>" +
					"<Wait>a10</Wait>" + 
					"<Collections>" +
						"<Collection>" + 
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" + 
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		new SyncDecoder(null).getWait(request.getDocumentElement());
	}

	@Test
	public void testGetWaitWhenJustTagReturnNull() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Wait/>" + 
					"<Collections>" + 
						"<Collection>" + 
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" + 
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");

		Integer wait = new SyncDecoder(null).getWait(request.getDocumentElement());
		
		assertThat(wait).isNull();
	}

	@Test(expected=ASRequestIntegerFieldException.class)
	public void testGetWaitWhenEmpty() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Wait> </Wait>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		new SyncDecoder(null).getWait(request.getDocumentElement());
	}
	
	@Test
	public void testGetWaitWhenNotReturnNull() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		Integer wait = new SyncDecoder(null).getWait(request.getDocumentElement());

		assertThat(wait).isNull();
	}

	@Test
	public void testGetWaitWhen0() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Wait>0</Wait>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		int wait = new SyncDecoder(null).getWait(request.getDocumentElement());

		assertThat(wait).isEqualTo(0);
	}

	@Test
	public void testGetWaitWhen1000() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Wait>1000</Wait>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		int wait = new SyncDecoder(null).getWait(request.getDocumentElement());

		assertThat(wait).isEqualTo(1000);
	}

	@Test(expected=ASRequestBooleanFieldException.class)
	public void testPartialWhenNotBoolean() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Partial>yeah</Partial>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		new SyncDecoder(null).isPartial(request.getDocumentElement());
	}

	@Test
	public void testPartialWhenNotPresent() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		Boolean isPartial = new SyncDecoder(null).isPartial(request.getDocumentElement());

		assertThat(isPartial).isNull();
	}

	@Test
	public void testPartialWhenEmptyIsTrue() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Partial/>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		Boolean isPartial = new SyncDecoder(null).isPartial(request.getDocumentElement());

		assertThat(isPartial).isTrue();
	}

	@Test
	public void testPartialFalse() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Partial>0</Partial>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		Boolean isPartial = new SyncDecoder(null).isPartial(request.getDocumentElement());

		assertThat(isPartial).isFalse();
	}

	@Test
	public void testPartialTrue() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Partial>1</Partial>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		Boolean isPartial = new SyncDecoder(null).isPartial(request.getDocumentElement());

		assertThat(isPartial).isTrue();
	}

	@Test
	public void testWindowSizeByDefault() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		Integer windowSize = new SyncDecoder(null).getWindowSize(request.getDocumentElement());

		assertThat(windowSize).isNull();
	}

	@Test(expected=ASRequestIntegerFieldException.class)
	public void testWindowSizeWhenNotANumber() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<WindowSize>a1</WindowSize>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		Integer windowSize = new SyncDecoder(null).getWindowSize(request.getDocumentElement());

		assertThat(windowSize).isNull();
	}

	@Test
	public void testWindowSize() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<WindowSize>150</WindowSize>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		Integer windowSize = new SyncDecoder(null).getWindowSize(request.getDocumentElement());

		assertThat(windowSize).isEqualTo(150);
	}

	@Test
	public void testCollectionWhenNotPresent() throws Exception {
		Document request = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<Sync>" +
					"<Collections>" +
					"</Collections>" +
				"</Sync>");
		
		SyncRequest syncRequest = new SyncDecoder(null).decodeSync(request);

		assertThat(syncRequest.getCollections()).isEmpty();
	}

	@Test
	public void testCollectionNoOptionNoCommand() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<Class>Email</Class>" +
					"<WindowSize>150</WindowSize>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());

		assertThat(collection.getSyncKey()).isEqualTo(new SyncKey("ddcf2e35-9834-49de-96ff-09979c7e2aa0"));
		assertThat(collection.getCollectionId()).isEqualTo(CollectionId.of(2));
		assertThat(collection.getDataType()).isEqualTo(PIMDataType.EMAIL);
		assertThat(collection.getWindowSize()).isEqualTo(150);
	}

	@Test
	public void testCollectionWhenNoClass() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<WindowSize>150</WindowSize>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());

		assertThat(collection.getDataType()).isNull();
	}

	@Test
	public void testCollectionWhenUnkownClass() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<Class>Music</Class>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<WindowSize>150</WindowSize>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());
		
		assertThat(collection.getDataType()).isNull();
	}

	@Test
	public void testCollectionWhenEmailClass() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<Class>Email</Class>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<WindowSize>150</WindowSize>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());
		
		assertThat(collection.getDataType()).isEqualTo(PIMDataType.EMAIL);
	}

	@Test
	public void testResponseCollectionWhenNoClass() throws Exception {
		Document request = DOMUtils.parse(
				"<Sync>" +
					"<Collections>" +
						"<Collection>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
							"<WindowSize>150</WindowSize>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		SyncResponse response = new SyncDecoder(null).decodeSyncResponse(request);
		
		SyncCollectionResponse collectionResponse = Iterables.getOnlyElement(response.getCollectionResponses());
		assertThat(collectionResponse.getDataType()).isNull();
	}

	@Test
	public void testResponseCollectionWhenUnkonwClass() throws Exception {
		Document request = DOMUtils.parse(
				"<Sync>" +
					"<Collections>" +
						"<Collection>" +
							"<Class>Music</Class>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
							"<WindowSize>150</WindowSize>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		SyncResponse response = new SyncDecoder(null).decodeSyncResponse(request);
		
		SyncCollectionResponse collectionResponse = Iterables.getOnlyElement(response.getCollectionResponses());
		assertThat(collectionResponse.getDataType()).isEqualTo(PIMDataType.UNKNOWN);
	}

	@Test
	public void testResponseCollectionWhenEmailClass() throws Exception {
		Document request = DOMUtils.parse(
				"<Sync>" +
					"<Collections>" +
						"<Collection>" +
							"<Class>Email</Class>" +
							"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
							"<CollectionId>2</CollectionId>" +
							"<WindowSize>150</WindowSize>" +
						"</Collection>" +
					"</Collections>" +
				"</Sync>");
		
		SyncResponse response = new SyncDecoder(null).decodeSyncResponse(request);
		
		SyncCollectionResponse collectionResponse = Iterables.getOnlyElement(response.getCollectionResponses());
		assertThat(collectionResponse.getDataType()).isEqualTo(PIMDataType.EMAIL);
	}

	@Test
	public void testCollectionWhenNoWindowSize() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<Class>Email</Class>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());
		
		assertThat(collection.getWindowSize()).isNull();
	}

	@Test
	public void testCollectionOptionsWhenNot() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());

		assertThat(collection.getOptions()).isNull();
		assertThat(collection.hasOptions()).isFalse();
	}

	@Test
	public void testCollectionOptions() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<Options>" +
						"<FilterType>5</FilterType>" +
						"<Conflict>1</Conflict>" +
						"<MIMETruncation>8</MIMETruncation>" +
						"<MIMESupport>2</MIMESupport>" +
						"<BodyPreference>" +
							"<Type>4</Type>" +
							"<TruncationSize>1000</TruncationSize>" +
							"<AllOrNone>1</AllOrNone>" +
						"</BodyPreference>" +
						"<BodyPreference>" +
							"<Type>2</Type>" +
							"<TruncationSize>5000</TruncationSize>" +
							"<AllOrNone>0</AllOrNone>" +
						"</BodyPreference>" +
					"</Options>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());

		assertThat(collection.getOptions().getFilterType()).isEqualTo(FilterType.ONE_MONTHS_BACK);
		assertThat(collection.getOptions().getConflict()).isEqualTo(1);
		assertThat(collection.getOptions().getMimeTruncation()).isEqualTo(8);
		assertThat(collection.getOptions().getMimeSupport()).isEqualTo(2);
		assertThat(collection.getOptions().getBodyPreferences()).containsOnly(
				BodyPreference.builder().bodyType(MSEmailBodyType.MIME).truncationSize(1000).allOrNone(true).build(),
				BodyPreference.builder().bodyType(MSEmailBodyType.HTML).truncationSize(5000).allOrNone(false).build());
	}

	@Test
	public void testCollectionOptionsFilterTypeDefaultValue() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<Options>" +
						"<Conflict>1</Conflict>" +
						"<MIMETruncation>8</MIMETruncation>" +
						"<MIMESupport>2</MIMESupport>" +
						"<BodyPreference>" +
							"<Type>4</Type>" +
							"<TruncationSize>1000</TruncationSize>" +
							"<AllOrNone>1</AllOrNone>" +
						"</BodyPreference>" +
					"</Options>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());

		assertThat(collection.getOptions().getFilterType()).isEqualTo(FilterType.THREE_DAYS_BACK);
	}

	@Test
	public void testCollectionOptionsConflictDefaultValue() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<Options>" +
						"<FilterType>5</FilterType>" +
						"<MIMETruncation>8</MIMETruncation>" +
						"<MIMESupport>2</MIMESupport>" +
						"<BodyPreference>" +
							"<Type>4</Type>" +
							"<TruncationSize>1000</TruncationSize>" +
							"<AllOrNone>1</AllOrNone>" +
						"</BodyPreference>" +
					"</Options>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());

		assertThat(collection.getOptions().getConflict()).isEqualTo(1);
	}

	@Test
	public void testCollectionOptionsMimeTruncationIsNotRequired() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<Options>" +
						"<FilterType>5</FilterType>" +
						"<Conflict>1</Conflict>" +
						"<MIMESupport>2</MIMESupport>" +
						"<BodyPreference>" +
							"<Type>4</Type>" +
							"<TruncationSize>1000</TruncationSize>" +
							"<AllOrNone>1</AllOrNone>" +
						"</BodyPreference>" +
					"</Options>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());

		assertThat(collection.getOptions().getMimeTruncation()).isNull();
	}

	@Test
	public void testCollectionOptionsMimeSupportIsNotRequired() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<Options>" +
						"<FilterType>5</FilterType>" +
						"<Conflict>1</Conflict>" +
						"<MIMETruncation>8</MIMETruncation>" +
						"<BodyPreference>" +
							"<Type>4</Type>" +
							"<TruncationSize>1000</TruncationSize>" +
							"<AllOrNone>1</AllOrNone>" +
						"</BodyPreference>" +
					"</Options>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());

		assertThat(collection.getOptions().getMimeSupport()).isNull();
	}

	@Test
	public void testCollectionOptionsBodyPreferenceIsNotRequired() throws Exception {
		Document request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>2</CollectionId>" +
					"<Options>" +
						"<FilterType>5</FilterType>" +
						"<Conflict>1</Conflict>" +
						"<MIMETruncation>8</MIMETruncation>" +
						"<MIMESupport>2</MIMESupport>" +
					"</Options>" +
				"</Collection>");
		
		SyncCollection collection = new SyncDecoder(null).getCollection(request.getDocumentElement());

		assertThat(collection.getOptions().getBodyPreferences()).isEmpty();
	}

	@Test
	public void testCollectionCommands() throws Exception {
		CollectionId collectionId = CollectionId.of(2);
		String contactApplicationData = 
				"<ApplicationData>" +
					"<Email1Address>opush@obm.org</Email1Address>" +
					"<FileAs>Dobney, JoLynn Julie</FileAs>" +
					"<FirstName>JoLynn</FirstName>" +
				"</ApplicationData>";

		MSContact contact = new MSContact();
		contact.setEmail1Address("opush@obm.org");
		contact.setFileAs("Dobney, JoLynn Julie");
		contact.setFirstName("JoLynn");
		
		Element request = DOMUtils.parse(
				"<Collection>" +
					"<SyncKey>ddcf2e35-9834-49de-96ff-09979c7e2aa0</SyncKey>" +
					"<CollectionId>"+ collectionId.asString() +"</CollectionId>" +
					"<Class>Contacts</Class>" +
					"<Commands>" +
						"<Add>" +
							"<ServerId>2:12</ServerId>" +
							"<ClientId>120</ClientId>" +
							contactApplicationData +
						"</Add>" +
						"<Change>" +
							"<ServerId>2:35</ServerId>" +
							"<ClientId>350</ClientId>" +
							contactApplicationData +
						"</Change>" +
						"<Fetch>" +
							"<ServerId>2:56</ServerId>" +
						"</Fetch>" +
						"<Delete>" +
							"<ServerId>2:79</ServerId>" +
						"</Delete>" +
					"</Commands>" +
				"</Collection>").getDocumentElement();
		
		SyncCollection collection = new SyncDecoder(decoder).getCollection(request);

		assertThat(collection.getCommands()).containsOnly(
				EncodedSyncCollectionCommandRequest.builder()
					.type(SyncCommand.ADD).serverId(collectionId.serverId(12)).clientId("120").applicationData(encodeContact(contact)).build(),
				EncodedSyncCollectionCommandRequest.builder()
					.type(SyncCommand.CHANGE).serverId(collectionId.serverId(35)).clientId("350").applicationData(encodeContact(contact)).build(),
				EncodedSyncCollectionCommandRequest.builder().type(SyncCommand.FETCH).serverId(collectionId.serverId(56)).build(),
				EncodedSyncCollectionCommandRequest.builder().type(SyncCommand.DELETE).serverId(collectionId.serverId(79)).build());
	}

	private EncodedApplicationData encodeContact(MSContact contact) {
		return new EncodedApplicationData(new ContactEncoder(){}.encodedApplicationData(null, contact));
	}

	@Test
	public void testCollectionCommandsServerIdIsNotRequired() throws Exception {
		Document request = DOMUtils.parse(
						"<Add>" +
							"<ClientId>120</ClientId>" +
							"<ApplicationData>" +
								"<Email1Address>\"opush@obm.org\"&lt;opush@obm.org&gt;</Email1Address>" +
								"<FileAs>Dobney, JoLynn Julie</FileAs>" +
								"<FirstName>JoLynn</FirstName>" +
							"</ApplicationData>" +
						"</Add>");
		
		EncodedSyncCollectionCommandRequest command = new SyncDecoder(decoder).getCommandRequest(request.getDocumentElement());
		
		assertThat(command.getServerId()).isNull();
	}

	@Test
	public void testCollectionCommandsClientIdIsNotRequired() throws Exception {
		Document request = DOMUtils.parse(
						"<Add>" +
							"<ServerId>12</ServerId>" +
							"<ApplicationData>" +
								"<Email1Address>\"opush@obm.org\"&lt;opush@obm.org&gt;</Email1Address>" +
								"<FileAs>Dobney, JoLynn Julie</FileAs>" +
								"<FirstName>JoLynn</FirstName>" +
							"</ApplicationData>" +
						"</Add>");
		
		EncodedSyncCollectionCommandRequest command = new SyncDecoder(decoder).getCommandRequest(request.getDocumentElement());
		
		assertThat(command.getClientId()).isNull();
	}

	@Test
	public void testCollectionCommandsApplicationDataIsNotRequired() throws Exception {
		Document request = DOMUtils.parse(
						"<Add>" +
							"<ServerId>12</ServerId>" +
							"<ClientId>120</ClientId>" +
						"</Add>");
		
		SyncCollectionCommandResponse command = new SyncDecoder(decoder).getCommandResponse(request.getDocumentElement(), PIMDataType.CONTACTS);
		
		assertThat(command.getApplicationData()).isNull();
	}
}
