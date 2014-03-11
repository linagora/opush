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
package org.obm.push.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;

import java.util.Collection;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.configuration.utils.IniFile;

public class CassandraConfigurationFileImplTest {

	private IMocksControl control;
	private IniFile iniFile;
	
	@Before
	public void setup() {
		control = createControl();
		iniFile = control.createMock(IniFile.class);
	}
	
	@Test(expected=NullPointerException.class)
	public void testSeedsNotDefined() {
		try {
			expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_SEEDS))
				.andReturn(null);
			
			control.replay();
			CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
			cassandraConfigurationFileImpl.seeds();
		} catch (NullPointerException e) {
			control.verify();
			throw e;
		}
	}
	
	@Test
	public void testOneSeed() {
		String expectedSeed = "localhost";
		expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_SEEDS))
			.andReturn(expectedSeed);
		
		control.replay();
		CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
		Collection<String> seed = cassandraConfigurationFileImpl.seeds();
		control.verify();
		
		assertThat(seed).containsOnly(expectedSeed);
	}
	
	@Test
	public void testThreeSeeds() {
		String expectedSeeds = "localhost,192.0.0.1,0.0.0.0";
		expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_SEEDS))
			.andReturn(expectedSeeds);
		
		control.replay();
		CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
		Collection<String> seed = cassandraConfigurationFileImpl.seeds();
		control.verify();
		
		assertThat(seed).containsOnly("localhost", "192.0.0.1", "0.0.0.0");
	}
	
	@Test
	public void testThreeSeedsWithSpace() {
		String expectedSeeds = " localhost, 192.0.0.1 , 0.0.0.0 ";
		expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_SEEDS))
			.andReturn(expectedSeeds);
		
		control.replay();
		CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
		Collection<String> seed = cassandraConfigurationFileImpl.seeds();
		control.verify();
		
		assertThat(seed).containsOnly("localhost", "192.0.0.1", "0.0.0.0");
	}

	@Test
	public void testThreeSeedsWithUnexpectedComma() {
		String expectedSeeds = " localhost,, 192.0.0.1, , 0.0.0.0 ";
		expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_SEEDS))
		.andReturn(expectedSeeds);
		
		control.replay();
		CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
		Collection<String> seed = cassandraConfigurationFileImpl.seeds();
		control.verify();
		
		assertThat(seed).containsOnly("localhost", "192.0.0.1", "0.0.0.0");
	}
	
	@Test(expected=NullPointerException.class)
	public void testKeyspaceNotDefined() {
		try {
			expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_KEYSPACE))
				.andReturn(null);
			
			control.replay();
			CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
			cassandraConfigurationFileImpl.keyspace();
		} catch(NullPointerException e) {
			control.verify();
			throw e;
		}
	}
	
	@Test
	public void testKeyspace() {
		String expectedKeyspace = "opush";
		expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_KEYSPACE))
			.andReturn(expectedKeyspace);
		
		control.replay();
		CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
		String keyspace = cassandraConfigurationFileImpl.keyspace();
		control.verify();
		
		assertThat(keyspace).isEqualTo(expectedKeyspace);
	}
	
	@Test(expected=NullPointerException.class)
	public void testUserNotDefined() {
		try {
			expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_USER))
				.andReturn(null);
			
			control.replay();
			CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
			cassandraConfigurationFileImpl.user();
		} catch (NullPointerException e) {
			control.verify();
			throw e;
		}
	}
	
	@Test
	public void testUser() {
		String expectedUser = "user";
		expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_USER))
			.andReturn(expectedUser);
		
		control.replay();
		CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
		String user = cassandraConfigurationFileImpl.user();
		control.verify();
		
		assertThat(user).isEqualTo(expectedUser);
	}

	@Test(expected=NullPointerException.class)
	public void testPasswordNotDefined() {
		try {
			expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_PASSWORD))
				.andReturn(null);

			control.replay();
			CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
			cassandraConfigurationFileImpl.password();
		} catch (NullPointerException e) {
			control.verify();
			throw e;
		}
	}

	@Test
	public void testPassword() {
		String expectedPassword = "password";
		expect(iniFile.getStringValue(CassandraConfigurationFileImpl.CASSANDRA_PASSWORD))
			.andReturn(expectedPassword);
		
		control.replay();
		CassandraConfigurationFileImpl cassandraConfigurationFileImpl = new CassandraConfigurationFileImpl(iniFile);
		String password = cassandraConfigurationFileImpl.password();
		control.verify();
		
		assertThat(password).isEqualTo(expectedPassword);
	}
}
