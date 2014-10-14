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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.obm.push.spushnik.SpushnikTestUtils.buildServiceUrl;

import java.util.Properties;

import org.assertj.core.util.Files;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.ConfigurationModule.PolicyConfigurationProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.opush.IntegrationUserAccessUtils;
import org.obm.opush.Users;
import org.obm.opush.env.CassandraServer;
import org.obm.push.OpushServer;
import org.obm.push.bean.Device;
import org.obm.push.bean.DeviceId;
import org.obm.push.bean.FolderSyncState;
import org.obm.push.bean.User;
import org.obm.push.spushnik.SpushnikScenarioTestUtils;
import org.obm.push.spushnik.bean.CheckResult;
import org.obm.push.spushnik.bean.CheckStatus;
import org.obm.push.spushnik.bean.Credentials;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.state.FolderSyncKeyFactory;
import org.obm.push.store.CollectionDao;
import org.obm.push.store.DeviceDao;
import org.obm.push.store.DeviceDao.PolicyStatus;

import com.google.inject.Inject;

@RunWith(GuiceRunner.class)
@GuiceModule(ScenarioTestModule.class)
public class FolderSyncScenarioMockTest {

	@Inject private Users users;
	@Inject private OpushServer opushServer;
	@Inject private IMocksControl mocksControl;
	@Inject private Configuration configuration;
	@Inject private FolderSyncScenario folderSyncScenario;
	@Inject private PolicyConfigurationProvider policyConfigurationProvider;
	@Inject private CassandraServer cassandraServer;
	@Inject private IntegrationUserAccessUtils userAccessUtils;
	@Inject private DeviceDao deviceDao;
	@Inject private CollectionDao collectionDao;
	@Inject private SpushnikScenarioTestUtils spushnikScenarioTestUtils;
	@Inject private FolderSyncKeyFactory folderSyncKeyFactory;
	
	@Before
	public void setup() throws Exception {
		cassandraServer.start();
		expect(policyConfigurationProvider.get()).andReturn("fakeConfiguration").anyTimes();
	}
	
	@After
	public void shutdown() throws Exception {
		opushServer.stop();
		cassandraServer.stop();
		Files.delete(configuration.dataDir);
	}

	@Test
	public void testScenarii() throws Exception {
		spushnikScenarioTestUtils.mockWorkingFolderSync(users.jaures);
		mocksControl.replay();
		opushServer.start();

		CheckResult checkResult = folderSyncScenario.run(
				buildServiceUrl(opushServer.getHttpPort()),
				Credentials.builder()
					.loginAtDomain(users.jaures.user.getLoginAtDomain())
					.password(users.jaures.password)
					.build());
		
		assertThat(checkResult.getStatus()).isEqualTo(CheckStatus.OK.asSpecificationValue());
	}

	@Test
	public void testErrorInBackend() throws Exception {
		userAccessUtils.expectUserLoginFromOpush(users.jaures);
		User user = users.jaures.user;
		DeviceId deviceId = new DeviceId("spushnik");
		Device device = new Device(user.hashCode(), 
				"spushnikProbe", 
				deviceId, 
				new Properties(), 
				users.jaures.deviceProtocolVersion);
		// First provisionning
		expect(deviceDao.getDevice(user, 
				deviceId, 
				"spushnikAgent",
				users.jaures.deviceProtocolVersion))
			.andReturn(device).anyTimes();
		expect(deviceDao.getPolicyKey(user, deviceId, PolicyStatus.PENDING))
			.andReturn(null).once();
		Long policyKey = new Long(1);
		expect(deviceDao.allocateNewPolicyKey(user, deviceId, PolicyStatus.PENDING))
			.andReturn(policyKey).once();
		
		// Second provisionning
		expect(deviceDao.getPolicyKey(user, deviceId, PolicyStatus.PENDING))
			.andReturn(policyKey).once();
		deviceDao.removePolicyKey(user, device);
		expectLastCall().once();
		expect(deviceDao.allocateNewPolicyKey(user, deviceId, PolicyStatus.ACCEPTED))
			.andReturn(policyKey).once();
		expect(deviceDao.getPolicyKey(user, deviceId, PolicyStatus.ACCEPTED))
			.andReturn(policyKey).once();
		
		// FolderSync
		FolderSyncKey syncKey = new FolderSyncKey("123");
		expect(folderSyncKeyFactory.randomSyncKey())
			.andReturn(syncKey).once();
		FolderSyncState syncState = FolderSyncState.builder()
				.syncKey(syncKey)
				.id(1)
				.build();
		expect(collectionDao.allocateNewFolderSyncState(device, syncKey))
			.andReturn(syncState).once();
		
		mocksControl.replay();
		opushServer.start();

		CheckResult checkResult = folderSyncScenario.run(
				buildServiceUrl(opushServer.getHttpPort()),
				Credentials.builder()
					.loginAtDomain(user.getLoginAtDomain())
					.password(users.jaures.password)
					.build());
		
		assertThat(checkResult.getStatus()).isEqualTo(CheckStatus.ERROR.asSpecificationValue());
	}

	@Test
	public void testBadOpushPort() throws Exception {
		userAccessUtils.expectUserLoginFromOpush(users.jaures);
		
		mocksControl.replay();
		opushServer.start();

		CheckResult checkResult = folderSyncScenario.run(
				buildServiceUrl(opushServer.getHttpPort() +1),
				Credentials.builder()
					.loginAtDomain(users.jaures.user.getLoginAtDomain())
					.password(users.jaures.password)
					.build());
		
		assertThat(checkResult.getStatus()).isEqualTo(CheckStatus.ERROR.asSpecificationValue());
	}

	@Test
	public void testBadOpushAddress() throws Exception {
		userAccessUtils.expectUserLoginFromOpush(users.jaures);
		
		mocksControl.replay();
		opushServer.start();

		CheckResult checkResult = folderSyncScenario.run(
				buildServiceUrl("123.456.0.1", opushServer.getHttpPort()),
				Credentials.builder()
					.loginAtDomain(users.jaures.user.getLoginAtDomain())
					.password(users.jaures.password)
					.build()); 
		
		assertThat(checkResult.getStatus()).isEqualTo(CheckStatus.ERROR.asSpecificationValue());
	}

	@Test
	public void testBadWebApp() throws Exception {
		userAccessUtils.expectUserLoginFromOpush(users.jaures);
		
		mocksControl.replay();
		opushServer.start();

		CheckResult checkResult = folderSyncScenario.run(
				buildServiceUrl("/VeryBad/", "127.0.0.1", opushServer.getHttpPort()),
				Credentials.builder()
					.loginAtDomain(users.jaures.user.getLoginAtDomain())
					.password(users.jaures.password)
					.build());  
				
		assertThat(checkResult.getStatus()).isEqualTo(CheckStatus.ERROR.asSpecificationValue());
	}
}
