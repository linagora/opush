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
package org.obm.push.spushnik.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.obm.push.spushnik.resources.Scenario.DEVICE_ID;
import static org.obm.push.spushnik.resources.Scenario.DEV_TYPE;
import static org.obm.push.spushnik.resources.Scenario.USER_AGENT;

import org.apache.http.impl.client.CloseableHttpClient;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.push.spushnik.bean.CheckResult;
import org.obm.push.spushnik.bean.CheckStatus;
import org.obm.push.spushnik.bean.Credentials;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.WBXMLOPClient;

import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

@GuiceModule(ScenarioTest.Env.class)
@RunWith(GuiceRunner.class)
public class ScenarioTest {

	public static class Env extends AbstractModule {

		@Override
		protected void configure() {
			IMocksControl mocks = createControl();
			bind(IMocksControl.class).toInstance(mocks);
			bind(WBXMLOPClient.Factory.class).toInstance(mocks.createMock(WBXMLOPClient.Factory.class));
			
			bind(Scenario.class).annotatedWith(Names.named("regular")).toInstance(new Scenario(){
				
				@Override
				protected CheckResult scenarii(OPClient client) throws Exception {
					return CheckResult.builder().checkStatus(CheckStatus.OK).build();
				}
			});
			bind(Scenario.class).annotatedWith(Names.named("failing")).toInstance(new Scenario(){
				
				@Override
				protected CheckResult scenarii(OPClient client) throws Exception {
					throw new IllegalStateException("expected message");
				}
			});
		}
	}
	
	String loginAtDomain;
	char[] password;
	String serverUrl;
	WBXMLOPClient opushClient;
	Credentials noCertificateCredentials;
	
	@Inject IMocksControl mocks;
	@Inject WBXMLOPClient.Factory opushClientFactory;
	@Inject @Named("regular") Scenario testee;
	@Inject @Named("failing") Scenario testeeThrowingException;

	@Before
	public void setUp() {
		serverUrl = "http://localhost";
		loginAtDomain = "user@domain";
		password = "pwd".toCharArray();
		noCertificateCredentials = Credentials.builder().loginAtDomain(loginAtDomain).password(password).build();

		opushClient = mocks.createMock(WBXMLOPClient.class);
		expect(opushClientFactory.create(anyObject(CloseableHttpClient.class),
				eq(loginAtDomain), aryEq(password), eq(DEVICE_ID), eq(DEV_TYPE), eq(USER_AGENT), eq(serverUrl)))
			.andReturn(opushClient);
	}

	@Test(expected=NullPointerException.class)
	public void testChooseHttpClientWhenNullCredentials() throws Exception {
		Credentials credentials = null;
		testee.chooseHttpClient(credentials, serverUrl);
	}
	
	@Test(expected=NullPointerException.class)
	public void testChooseHttpClientWhenNullUrl() throws Exception {
		String serviceUrl = null;
		testee.chooseHttpClient(noCertificateCredentials, serviceUrl);
	}

	@Test
	public void runShouldCloseHttpClientAndReturnOKWhenSuccess() {
		opushClient.close();
		expectLastCall();
		
		mocks.replay();
		CheckResult result = testee.run(serverUrl, noCertificateCredentials);
		mocks.verify();
		
		assertThat(result).isEqualTo(CheckResult.builder().checkStatus(CheckStatus.OK).build());
	}

	@Test
	public void runShouldCloseHttpClientAndReturnERRORWhenException() {
		opushClient.close();
		expectLastCall();
		
		mocks.replay();
		CheckResult result = testeeThrowingException.run(serverUrl, noCertificateCredentials);
		mocks.verify();
		
		assertThat(result.getStatus()).isEqualTo(CheckStatus.ERROR.asSpecificationValue());
		assertThat(result.getMessages()).hasSize(1);
		assertThat(Iterables.getLast(result.getMessages())).startsWith(IllegalStateException.class.getName() + ": expected message");
	}

}
