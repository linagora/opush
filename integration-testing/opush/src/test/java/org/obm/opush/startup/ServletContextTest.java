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
package org.obm.opush.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.obm.opush.IntegrationTestUtils.buildWBXMLOpushClient;

import java.io.IOException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.Users;
import org.obm.opush.Users.OpushUser;
import org.obm.opush.env.NoCassandraOpushModule;
import org.obm.push.OpushContainerModule.OpushHttpCapability;
import org.obm.push.OpushServer;
import org.obm.sync.LifecycleListenerHelper;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.OptionsResponse;

import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;

@RunWith(GuiceRunner.class)
@GuiceModule(NoCassandraOpushModule.class)
public class ServletContextTest {

	@Inject	Users users;
	@Inject PolicyConfigurationProvider policyConfigurationProvider;
	@Inject Injector injector;
	@Inject IMocksControl mocksControl;
	
	private CloseableHttpClient httpClient;
	private OpushUser user;
	
	@Before
	public void setUp() {
		httpClient = HttpClientBuilder.create().build();
		user = users.jaures;
	}
	
	@After
	public void tearDown() throws IOException {
		httpClient.close();
		LifecycleListenerHelper.shutdownListeners(injector);
	}
	
	@Test(expected=ConfigurationException.class)
	public void noOpushServerByDefault() {
		assertThat(injector.getInstance(OpushHttpCapability.class)).isNotNull();
		injector.getInstance(OpushServer.class);
	}
	
	@Test
	public void isOpushServerManagedByExtendedInjector() {
		OpushHttpCapability httpCapability = injector.getInstance(OpushHttpCapability.class);
		assertThat(httpCapability.enableByExtendingInjector().getInstance(OpushServer.class)).isNotNull();
	}

	@Test
	public void hasServletContextAfterEnablingIt() throws Exception {
		expect(policyConfigurationProvider.get()).andReturn("");
		mocksControl.replay();
		OpushHttpCapability httpCapability = injector.getInstance(OpushHttpCapability.class);
		OpushServer opushServer = httpCapability.enableByExtendingInjector().getInstance(OpushServer.class);
		opushServer.start();

		try {
			OPClient opClient = buildWBXMLOpushClient(user, opushServer.getPort(), httpClient);
			OptionsResponse options = opClient.options();
			assertThat(options.getStatusLine().getStatusCode()).isEqualTo(200);
		} finally {
			opushServer.stop();
			mocksControl.verify();
		}
	}
}
