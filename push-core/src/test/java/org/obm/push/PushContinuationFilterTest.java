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

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.push.impl.PushContinuation;

public class PushContinuationFilterTest {

	IMocksControl control;
	LoggerService loggerService;
	PushContinuation.Factory continuationFactory;

	HttpServletRequest request;
	HttpServletResponse response;
	FilterChain chain;
	PushContinuation continuation;
	
	PushContinuationFilter testee;

	@Before
	public void setUp() {
		control = createControl();
		loggerService = control.createMock(LoggerService.class);
		continuationFactory = control.createMock(PushContinuation.Factory.class);

		request = control.createMock(HttpServletRequest.class);
		response = control.createMock(HttpServletResponse.class);
		chain = control.createMock(FilterChain.class);
		continuation = control.createMock(PushContinuation.class);
		
		testee = new PushContinuationFilter(loggerService, continuationFactory);
	}

	@Test
	public void doFilterShouldCloseSession() throws Exception {
		int requestId = 5;
		expect(continuation.getReqId()).andReturn(requestId);
		
		request.setAttribute(RequestProperties.CONTINUATION, continuation);
		expectLastCall();
		
		expect(continuationFactory.createContinuation(request)).andReturn(continuation);
		
		loggerService.startSession();
		expectLastCall();
		
		loggerService.defineRequestId(requestId);
		expectLastCall();
		
		chain.doFilter(request, response);
		expectLastCall();
		
		loggerService.closeSession();
		expectLastCall();
		
		control.replay();
		testee.doFilter(request, response, chain);
		control.verify();
	}
	
	@Test(expected=IllegalStateException.class)
	public void doFilterShouldCloseSessionWhenExceptionByChain() throws Exception {
		int requestId = 5;
		expect(continuation.getReqId()).andReturn(requestId);
		
		request.setAttribute(RequestProperties.CONTINUATION, continuation);
		expectLastCall();
		
		expect(continuationFactory.createContinuation(request)).andReturn(continuation);
		
		loggerService.startSession();
		expectLastCall();
		
		loggerService.defineRequestId(requestId);
		expectLastCall();
		
		chain.doFilter(request, response);
		expectLastCall().andThrow(new IllegalStateException("Might be exception"));
		
		loggerService.closeSession();
		expectLastCall();
		
		control.replay();
		try {
			testee.doFilter(request, response, chain);
		} catch (Exception e) {
			control.verify();
			throw e;
		}
	}

	@Test(expected=IllegalStateException.class)
	public void doFilterShouldCloseSessionWhenExceptionByFactory() throws Exception {
		expect(continuationFactory.createContinuation(request)).andThrow(new IllegalStateException());
		
		loggerService.closeSession();
		expectLastCall();
		
		control.replay();
		try {
			testee.doFilter(request, response, chain);
		} catch (Exception e) {
			control.verify();
			throw e;
		}
	}
}
