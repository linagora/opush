/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015  Linagora
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

import org.obm.configuration.EmailConfigurationImpl;
import org.obm.configuration.utils.IniFile;
import org.obm.push.ExpungePolicy;

import com.google.inject.Singleton;

@Singleton
public class OpushEmailConfigurationImpl extends EmailConfigurationImpl implements OpushEmailConfiguration {
	
	private static final String BACKEND_CONF_FILE = "/etc/opush/mail_conf.ini";
	
	private static final String IMAP_EXPUNGE_POLICY = "imap.expungePolicy";
	private static final String MESSAGE_MAX_SIZE = "message.maxSize";
	
	public static class Factory {
		
		protected IniFile.Factory iniFileFactory;

		public Factory() {
			iniFileFactory = new IniFile.Factory();
		}
		
		public OpushEmailConfigurationImpl create() {
			return new OpushEmailConfigurationImpl(iniFileFactory.build(BACKEND_CONF_FILE));
		}
	}
	
	private OpushEmailConfigurationImpl(IniFile iniFile) {
		super(iniFile);
	}

	@Override
	public ExpungePolicy expungePolicy() {
		String stringValue = iniFile.getStringValue(IMAP_EXPUNGE_POLICY);
		if (stringValue == null) {
			return IMAP_EXPUNGE_POLICY_DEFAULT_VALUE;
		}
		return ExpungePolicy.valueOf(stringValue.toUpperCase());
	}

	@Override
	public int getMessageMaxSize() {
		return iniFile.getIntValue(MESSAGE_MAX_SIZE, MESSAGE_MAX_SIZE_DEFAULT_VALUE);
	}
}
