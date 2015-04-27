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

import org.obm.push.bean.Device;
import org.obm.push.protocol.bean.ClientSyncRequest;
import org.obm.push.protocol.bean.SyncResponse;
import org.obm.push.protocol.data.SyncDecoder;
import org.obm.push.protocol.data.SyncEncoder;
import org.obm.sync.push.client.ResponseTransformer;
import org.obm.sync.push.client.beans.AccountInfos;
import org.obm.sync.push.client.beans.NS;
import org.w3c.dom.Document;

import com.google.inject.Inject;

public class Sync extends AbstractCommand<SyncResponse> {

	public static class Builder {
		
		private final SyncDecoder syncDecoder;
		private final SyncEncoder syncEncoder;
		
		private Device device;
		private ClientSyncRequest request;

		@Inject
		protected Builder(SyncDecoder syncDecoder, SyncEncoder syncEncoder) {
			this.syncDecoder = syncDecoder;
			this.syncEncoder = syncEncoder;
		}

		public Builder request(ClientSyncRequest request) {
			this.request = request;
			return this;
		}
		
		public Builder device(Device device) {
			this.device = device;
			return this;
		}
		
		public Sync build() {
			return new Sync(syncDecoder, new SimpleSyncDocumentProvider(syncEncoder, device, request));
		}

	}

	private final SyncDecoder syncDecoder;

	public Sync(SyncDecoder syncDecoder, DocumentProvider documentProvider) {
		super(NS.AirSync, "Sync", documentProvider);
		this.syncDecoder = syncDecoder;
	}

	@Override
	protected SyncResponse parseResponse(Document responseDocument) {
		return syncDecoder.decodeSyncResponse(responseDocument);
	}

	@Override
	protected ResponseTransformer<SyncResponse> responseTransformer() {
		return new SyncResponseTransformer();
	}
	
	private static class SimpleSyncDocumentProvider implements DocumentProvider {

		private final SyncEncoder syncEncoder;
		private final ClientSyncRequest request;
		private final Device device;

		private SimpleSyncDocumentProvider(SyncEncoder syncEncoder, Device device, ClientSyncRequest request) {
			this.syncEncoder = syncEncoder;
			this.request = request;
			this.device = device;
		}

		@Override
		public Document get(AccountInfos accountInfos) {
			return syncEncoder.encodeSync(request, device);
		}
		
	}

	private class SyncResponseTransformer implements ResponseTransformer<SyncResponse> {

		@Override
		public SyncResponse parse(Document document) {
			return parseResponse(document);
		}
	}
}
