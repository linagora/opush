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
import org.obm.opush.IntegrationTestUtils;
import org.obm.opush.env.CassandraServer;
import org.obm.opush.env.DefaultOpushModule;
import org.obm.push.OpushServer;
import org.obm.sync.push.client.OPClient;
import org.obm.sync.push.client.OptionsResponse;

import com.google.inject.Inject;

@RunWith(GuiceRunner.class)
@GuiceModule(DefaultOpushModule.class)
public class ServletContextTest {

	@Inject OpushServer opushServer;
	@Inject CassandraServer cassandraServer;
	@Inject	Users users;
	@Inject IMocksControl control;
	@Inject PolicyConfigurationProvider policyConfigurationProvider;
	
	private CloseableHttpClient httpClient;
	private OpushUser user;
	
	@Before
	public void setUp() {
		httpClient = HttpClientBuilder.create().build();
		user = users.jaures;
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration").anyTimes();
	}
	
	@After
	public void tearDown() throws Exception {
		httpClient.close();
		cassandraServer.stop();
		opushServer.stop();
	}

	@Test
	public void opushHttpServerIsUp() throws Exception {
		cassandraServer.start();
		control.replay();
		opushServer.start();
		OPClient opClient = IntegrationTestUtils.buildWBXMLOpushClient(user, opushServer.getHttpPort(), httpClient);
		OptionsResponse options = opClient.options();
		assertThat(options.getStatusLine().getStatusCode()).isEqualTo(200);
	}
	
	@Test(expected=IllegalStateException.class)
	public void opushNoCassandraSchemaIsUp() throws Exception {
		cassandraServer.startWithoutSchema();
		control.replay();
		opushServer.start();
		opushServer.getHttpPort();
	}
}
