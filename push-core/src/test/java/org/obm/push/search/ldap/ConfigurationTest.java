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
package org.obm.push.search.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.obm.push.search.ldap.Configuration.DEFAULT_SEARCH_LDAP_LIMIT;
import static org.obm.push.search.ldap.Configuration.LDAP_CONF_FILE;
import static org.obm.push.search.ldap.Configuration.SEARCH_LDAP_BASE;
import static org.obm.push.search.ldap.Configuration.SEARCH_LDAP_FILTER;
import static org.obm.push.search.ldap.Configuration.SEARCH_LDAP_LIMIT;
import static org.obm.push.search.ldap.Configuration.SEARCH_LDAP_URL;

import javax.naming.directory.DirContext;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.obm.configuration.utils.IniFile;
import org.obm.configuration.utils.IniFile.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConfigurationTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private IMocksControl mocks;
	private Factory iniFileFactory;
	private IniFile iniFile;

	@Before
	public void setUp() {
		mocks = createControl();
		iniFileFactory = mocks.createMock(IniFile.Factory.class);
		iniFile = mocks.createMock(IniFile.class);
		expect(iniFileFactory.build(LDAP_CONF_FILE)).andReturn(iniFile);
	}
	
	@Test
	public void testUrlNone() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();
		
		assertThat(configuration.getUrl()).isNull();
		assertThat(configuration.isValidConfiguration()).isFalse();
	}
	
	@Test
	public void testUrlEmpty() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();
		
		assertThat(configuration.getUrl()).isNull();
		assertThat(configuration.isValidConfiguration()).isFalse();
	}
	
	@Test
	public void testUrlNoProtocol() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("127.0.0.1");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();
		
		assertThat(configuration.getUrl()).isEqualTo("ldap://127.0.0.1");
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testUrlBadProtocol() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("http://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();
		
		assertThat(configuration.getUrl()).isNull();
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testUrlNoIp() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldap://");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();
		
		assertThat(configuration.getUrl()).isNull();
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testUrlLDAP() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldap://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();
		
		assertThat(configuration.getUrl()).isEqualTo("ldap://ldapserver");
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testUrlLDAPS() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();

		assertThat(configuration.getUrl()).isEqualTo("ldaps://ldapserver");
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testBaseNone() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();

		assertThat(configuration.getBaseDn()).isNull();
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testBaseEmpty() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();

		assertThat(configuration.getBaseDn()).isNull();
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testBase() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();

		assertThat(configuration.getBaseDn()).isEqualTo("%d,dc=local");
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testFilterNone() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();

		assertThat(configuration.getFilter()).isNull();
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testFilterEmpty() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn("");
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();

		assertThat(configuration.getFilter()).isNull();
		assertThat(configuration.isValidConfiguration()).isFalse();
	}

	@Test
	public void testFilter() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn("filter");
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();

		assertThat(configuration.getFilter()).isEqualTo("filter");
		assertThat(configuration.isValidConfiguration()).isTrue();
	}

	@SuppressWarnings("unused")
	@Test(expected=RuntimeException.class)
	public void limitShouldTriggerExceptionWhenLessThanZero() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn("filter");
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(-5);
		
		mocks.replay();
		try {
			new Configuration(iniFileFactory, logger);
		} catch (Exception e) {
			mocks.verify();
			throw e;
		}
	}

	@Test
	public void limitShouldBeZeroWhenZero() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn("filter");
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(0);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();

		assertThat(configuration.getLimit()).isEqualTo(0);
		assertThat(configuration.isValidConfiguration()).isTrue();
	}
	
	@Test
	public void limitShouldNotBeDefaultWhenFive() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn("filter");
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();

		assertThat(configuration.getLimit()).isEqualTo(5);
		assertThat(configuration.isValidConfiguration()).isTrue();
	}
	
	@Test
	public void limitShouldBeDefaultWhenNotDefined() {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn("filter");
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(null);
		
		mocks.replay();
		Configuration configuration = new Configuration(iniFileFactory, logger);
		mocks.verify();
		
		assertThat(configuration.getLimit()).isEqualTo(DEFAULT_SEARCH_LDAP_LIMIT);
		assertThat(configuration.isValidConfiguration()).isTrue();
	}
	
	@Test(expected=IllegalStateException.class)
	public void testBuildContextConnectionFailsIfUrlIsMissing() throws Exception {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn("filter");
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		new Configuration(iniFileFactory, logger).buildContextConnection();
	}

	@Test(expected=IllegalStateException.class)
	public void testBuildContextConnectionFailsIfBaseIsMissing() throws Exception {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn(null);
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn("filter");
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		new Configuration(iniFileFactory, logger).buildContextConnection();
	}

	@Test(expected=IllegalStateException.class)
	public void testBuildContextConnectionFailsIfFilterIsMissing() throws Exception {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn(null);
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		new Configuration(iniFileFactory, logger).buildContextConnection();
	}

	@Ignore("A connection toward the url tries to be done")
	@Test
	public void testBuildContextConnection() throws Exception {
		expect(iniFile.getStringValue(SEARCH_LDAP_URL)).andReturn("ldaps://ldapserver");
		expect(iniFile.getStringValue(SEARCH_LDAP_BASE)).andReturn("%d,dc=local");
		expect(iniFile.getStringValue(SEARCH_LDAP_FILTER)).andReturn("filter");
		expect(iniFile.getIntegerValue(SEARCH_LDAP_LIMIT, null)).andReturn(5);
		
		mocks.replay();
		DirContext connection = new Configuration(iniFileFactory, logger).buildContextConnection();
		mocks.verify();

		assertThat(connection).isNotNull();
	}
}
