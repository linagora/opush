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
import org.obm.push.bean.IApplicationData;
import org.obm.push.bean.ServerId;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.change.SyncCommand;
import org.obm.push.protocol.bean.CollectionId;
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
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Performs a Sync AS command for the given folders with 0 as syncKey
 */
public class Sync extends AbstractCommand<SyncResponse> {

	public static Builder builder(SyncDecoder decoder) {
		return new Builder(decoder);
	}
	
	public static class Builder {
		
		private final SyncDecoder decoder;
		private EncoderFactory encoderFactory;
		
		private SyncKey syncKey;
		private ImmutableList.Builder<CollectionId> collectionIds;
		private SyncCommand command;
		private ServerId serverId;
		private String clientId;
		private IApplicationData data;

		private Device device;

		private Builder(SyncDecoder decoder) {
			this.decoder = decoder;
			this.collectionIds = ImmutableList.builder();
		}
		
		public Builder encoder(EncoderFactory encoderFactory) {
			this.encoderFactory = encoderFactory;
			return this;
		}
		
		public Builder syncKey(SyncKey syncKey) {
			this.syncKey = syncKey;
			return this;
		}
		
		public Builder collectionId(CollectionId collectionId) {
			this.collectionIds.add(collectionId);
			return this;
		}
		
		public Builder command(SyncCommand command) {
			this.command = command;
			return this;
		}
		
		public Builder serverId(ServerId serverId) {
			this.serverId = serverId;
			return this;
		}
		
		public Builder clientId(String clientId) {
			this.clientId = clientId;
			return this;
		}
		
		public Builder data(IApplicationData data) {
			this.data = data;
			return this;
		}
		
		public Builder device(Device device) {
			this.device = device;
			return this;
		}
		
		public Sync build() throws SAXException, IOException {
			ImmutableList<CollectionId> collectionIds = this.collectionIds.build();
			if (command == null) {
				return new Sync(decoder, new SimpleSyncTemplate(syncKey, collectionIds));
			}
			Preconditions.checkState(collectionIds.size() == 1);
			if (data != null) {
				return new Sync(decoder, new SyncWithCommandDataTemplate(syncKey, Iterables.getOnlyElement(collectionIds), command, serverId, clientId, data, encoderFactory, device));
			} else {
				return new Sync(decoder, new SyncWithCommandTemplate(syncKey, Iterables.getOnlyElement(collectionIds), command, serverId, clientId));
			}
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
	
	private static class SimpleSyncTemplate extends TemplateDocument {

		private final SyncKey syncKey;
		private final List<CollectionId> collectionIds;

		private SimpleSyncTemplate(SyncKey syncKey, List<CollectionId> collectionIds) throws SAXException, IOException {
			super("SyncRequest.xml");
			this.syncKey = syncKey;
			this.collectionIds = collectionIds;
		}

		@Override
		protected void customize(Document document, AccountInfos accountInfos) {
			Element cols = DOMUtils.getUniqueElement(document.getDocumentElement(), "Collections");
			for (CollectionId collectionId : collectionIds) {
				Element col = DOMUtils.createElement(cols, "Collection");
				DOMUtils.createElementAndText(col, "SyncKey", syncKey.getSyncKey());
				DOMUtils.createElementAndText(col, "CollectionId", collectionId.asString());
			}				
		}
	}

	public static class SyncWithCommandTemplate extends TemplateDocument {

		protected final SyncKey syncKey;
		protected final CollectionId collectionId;
		protected final SyncCommand command;
		protected final ServerId serverId;
		protected final String clientId;

		protected SyncWithCommandTemplate(SyncKey syncKey, CollectionId collectionId, SyncCommand command,
				ServerId serverId, String clientId) throws SAXException, IOException {
			super("SyncWithCommandRequest.xml");
			this.syncKey = syncKey;
			this.collectionId = collectionId;
			this.command = command;
			this.serverId = serverId;
			this.clientId = clientId;
		}

		@Override
		protected void customize(Document document, AccountInfos accountInfos) {
			Element sk = DOMUtils.getUniqueElement(document.getDocumentElement(), SyncRequestFields.SYNC_KEY.getName());
			sk.setTextContent(syncKey.getSyncKey());
			Element collection = DOMUtils.getUniqueElement(document.getDocumentElement(), SyncRequestFields.COLLECTION_ID.getName());
			collection.setTextContent(collectionId.asString());
			
			Element commandsEl = DOMUtils.getUniqueElement(document.getDocumentElement(), SyncRequestFields.COMMANDS.getName());
			Element commandEl = DOMUtils.createElement(commandsEl, command.asSpecificationValue());
			if (serverId != null) {
				DOMUtils.createElementAndText(commandEl, SyncRequestFields.SERVER_ID.getName(), serverId.asString());
			}
			if (!Strings.isNullOrEmpty(clientId)) {
				DOMUtils.createElementAndText(commandEl, SyncRequestFields.CLIENT_ID.getName(), clientId);
			}
		}
	}
	
	public static class SyncWithCommandDataTemplate extends SyncWithCommandTemplate {

		private final IApplicationData data;
		private final EncoderFactory encoders;
		private final Device device;

		protected SyncWithCommandDataTemplate(SyncKey syncKey, CollectionId collectionId, SyncCommand command,
				ServerId serverId, String clientId, IApplicationData data, EncoderFactory encoders, Device device)
				throws SAXException, IOException {
			super(syncKey, collectionId, command, serverId, clientId);
			this.data = data;
			this.encoders = encoders;
			this.device = device;
		}

		@Override
		protected void customize(Document document, AccountInfos accountInfos) {
			try {
				super.customize(document, accountInfos);
				Element commandsEl = DOMUtils.getUniqueElement(document.getDocumentElement(), SyncRequestFields.COMMANDS.getName());
				Element commandEl = DOMUtils.getUniqueElement(commandsEl, command.asSpecificationValue());
				Element applicationDataEl = DOMUtils.createElement(commandEl, SyncRequestFields.APPLICATION_DATA.getName());
				encoders.encode(device, applicationDataEl, data, false);
			} catch (IOException e) {
				Throwables.propagate(e);
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
