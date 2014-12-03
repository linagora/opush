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
package org.obm.sync.push.client.commands;

import java.io.IOException;
import java.util.List;

import org.obm.push.bean.Device;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.SyncCollectionCommandRequest;
import org.obm.push.bean.SyncCollectionOptions;
import org.obm.push.protocol.bean.SyncCollection;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.EncoderFactory;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.protocol.data.SyncRequestFields;
import org.obm.push.utils.DOMUtils;
import org.obm.sync.push.client.ResponseTransformer;
import org.obm.sync.push.client.beans.AccountInfos;
import org.obm.sync.push.client.beans.NS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

/**
 * Performs a Sync AS command for the given folders with 0 as syncKey
 */
public class Sync extends AbstractCommand<SyncResponse> {

	public static Builder builder(SyncDecoder decoder) {
		return new Builder(decoder);
	}
	
	public static class Builder {
		
		private final SyncDecoder decoder;
		private final ImmutableList.Builder<SyncCollection> collections;

		private EncoderFactory encoderFactory;
		private Device device;

		private Builder(SyncDecoder decoder) {
			this.decoder = decoder;
			this.collections = ImmutableList.builder();
		}
		
		public Builder encoder(EncoderFactory encoderFactory) {
			this.encoderFactory = encoderFactory;
			return this;
		}
		
		public Builder collection(SyncCollection collection) {
			this.collections.add(collection);
			return this;
		}
		
		public Builder device(Device device) {
			this.device = device;
			return this;
		}
		
		public Sync build() {
			ImmutableList<SyncCollection> collections = this.collections.build();
			return new Sync(decoder, new SimpleSyncDocumentProvider(encoderFactory, device, collections));
		}

	}

	private final SyncDecoder decoder;

	public Sync(SyncDecoder decoder, DocumentProvider documentProvider) {
		super(NS.AirSync, "Sync", documentProvider);
		this.decoder = decoder;
	}

	@Override
	protected SyncResponse parseResponse(Document responseDocument) {
		return decoder.decodeSyncResponse(responseDocument);
	}

	@Override
	protected ResponseTransformer<SyncResponse> responseTransformer() {
		return new SyncResponseTransformer();
	}
	
	private static class SimpleSyncDocumentProvider implements DocumentProvider {

		private final List<SyncCollection> collections;
		private Device device;
		private EncoderFactory encoders;

		private SimpleSyncDocumentProvider(EncoderFactory encoders, Device device, List<SyncCollection> collections) {
			this.collections = collections;
			this.encoders = encoders;
			this.device = device;
		}

		@Override
		public Document get(AccountInfos accountInfos) {
			Document document = DOMUtils.createDoc(null, "Sync");
			Element root = document.getDocumentElement();

			try {
				final Element cols = DOMUtils.createElement(root, SyncRequestFields.COLLECTIONS.getName());
				for (SyncCollection collection: collections) {
					Element col = DOMUtils.createElement(cols, SyncRequestFields.COLLECTION.getName());
					DOMUtils.createElementAndText(col, SyncRequestFields.SYNC_KEY.getName(), collection.getSyncKey().getSyncKey());
					DOMUtils.createElementAndText(col, SyncRequestFields.COLLECTION_ID.getName(), collection.getCollectionId().asString());
					Integer windowSize = collection.getWindowSize();
					if (windowSize != null) {
						DOMUtils.createElementAndText(col, SyncRequestFields.WINDOW_SIZE.getName(), String.valueOf(windowSize));
					}
					SyncCollectionOptions options = collection.getOptions();
					if (options != null) {
						FilterType filterType = options.getFilterType();
						if (filterType != null) {
							DOMUtils.createElementAndText(col, SyncRequestFields.FILTER_TYPE.getName(), filterType.asSpecificationValue());
						}
					}
					appendDataClass(col, collection);

					Element commandsEl = DOMUtils.createElement(col, SyncRequestFields.COMMANDS.getName());
					for (SyncCollectionCommandRequest command: collection.getCommands()) {
						Element commandEl = DOMUtils.createElement(commandsEl, command.getType().asSpecificationValue());
						if (command.getServerId() != null) {
							DOMUtils.createElementAndText(commandEl, SyncRequestFields.SERVER_ID.getName(), command.getServerId().asString());
						}
						if (!Strings.isNullOrEmpty(command.getClientId())) {
							DOMUtils.createElementAndText(commandEl, SyncRequestFields.CLIENT_ID.getName(), command.getClientId());
						}
						if (command.getApplicationData() != null) {
							Element applicationDataEl = DOMUtils.createElement(commandEl, SyncRequestFields.APPLICATION_DATA.getName());
							encoders.encode(device, applicationDataEl, command.getApplicationData(), false);
						}
					}
				}
			} catch (IOException e) {
				Throwables.propagate(e);
			}
			return document;
		}
		
		private void appendDataClass(Element collectionEl, SyncCollection collection) {
			if (collection.getDataType() != null) {
				String xmlValue = collection.getDataType().asXmlValue();
				if (xmlValue != null) {
					DOMUtils.createElementAndText(collectionEl, "Class", xmlValue);
				}
			}
		}
	}

	private class SyncResponseTransformer implements ResponseTransformer<SyncResponse> {

		@Override
		public SyncResponse parse(Document document) {
			return parseResponse(document);
		}
	}
}
