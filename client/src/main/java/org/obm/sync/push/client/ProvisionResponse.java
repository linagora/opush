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
package org.obm.sync.push.client;

import javax.xml.transform.TransformerException;

import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Element;

import com.google.common.base.Objects;

public class ProvisionResponse {
	
	public static Builder builder(org.obm.push.protocol.bean.ProvisionResponse.Builder protocolBuilder) {
		return new Builder(protocolBuilder);
	}
	
	public static class Builder {
		
		private final org.obm.push.protocol.bean.ProvisionResponse.Builder protocolBuilder;
		private Element policyDataEl;

		private Builder(org.obm.push.protocol.bean.ProvisionResponse.Builder protocolBuilder) {
			super();
			this.protocolBuilder = protocolBuilder;
		}
		
		public Builder policyData(Element policyDataEl) {
			this.policyDataEl = policyDataEl;
			return this;
		}
		
		public ProvisionResponse build() throws TransformerException {
			String policyData = serializePolicy();
			return new ProvisionResponse(protocolBuilder.build(), policyDataEl, policyData);
		}

		private String serializePolicy() throws TransformerException {
			if (policyDataEl != null) {
				return DOMUtils.prettySerialize(policyDataEl);
			}
			return null;
		}
		
	}

	private final org.obm.push.protocol.bean.ProvisionResponse response;
	private final String policyData;
	private final Element policyDataEl;
	
	private ProvisionResponse(org.obm.push.protocol.bean.ProvisionResponse response ,Element policyDataEl, String policyData) {
		this.response = response;
		this.policyDataEl = policyDataEl;
		this.policyData = policyData;
	}
	
	public org.obm.push.protocol.bean.ProvisionResponse getResponse() {
		return response;
	}

	public boolean hasPolicyData() {
		return policyData != null;
	}

	public String policyData() {
		return policyData;
	}
	
	public Element getPolicyDataEl() {
		return policyDataEl;
	}

	@Override
	public final int hashCode() {
		return Objects.hashCode(response, policyData);
	}
	
	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof ProvisionResponse) {
			ProvisionResponse other = (ProvisionResponse) obj;
			return Objects.equal(response, other.response)
				&& Objects.equal(policyData, other.policyData);
		}
		return false;
	}

}
