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
package org.obm.push;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.resource.HttpClientResource;
import org.obm.push.resource.OpushResourcesHolder;

public class ResourcesFilterTest {

	private IMocksControl mocks;
	private ServletRequest request;
	private ServletResponse response;
	private FilterChain chain;
	private OpushResourcesHolder opushResourcesHolder;

	@Before
	public void setUp() {
		mocks = EasyMock.createControl();
		request = mocks.createMock(ServletRequest.class);
		response = mocks.createMock(ServletResponse.class);
		chain = mocks.createMock(FilterChain.class);
		opushResourcesHolder = mocks.createMock(OpushResourcesHolder.class);
	}

	@Test
	public void filterShouldCloseResourcesWhenChainIsDone() throws Exception {
		chain.doFilter(request, response);
		expectLastCall();
		opushResourcesHolder.put(same(HttpClientResource.class), isA(HttpClientResource.class));
		expectLastCall();
		opushResourcesHolder.close();
		expectLastCall();
		
		mocks.replay();
		new ResourcesFilter(opushResourcesHolder).doFilter(request, response, chain);
		mocks.verify();
	}

	@Test(expected=IllegalStateException.class)
	public void filterShouldCloseResourcesWhenChainTriggersException() throws Exception {
		chain.doFilter(request, response);
		expectLastCall().andThrow(new IllegalStateException("error"));
		opushResourcesHolder.put(same(HttpClientResource.class), isA(HttpClientResource.class));
		expectLastCall();
		opushResourcesHolder.close();
		expectLastCall();

		mocks.replay();
		try {
			new ResourcesFilter(opushResourcesHolder).doFilter(request, response, chain);
		} catch (Exception e) {
			mocks.verify();
			throw e;
		}
	}
}
