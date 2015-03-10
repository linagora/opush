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
package org.obm.opush.env;

import org.obm.Configuration;
import org.obm.push.ExpungePolicy;
import org.obm.push.configuration.CassandraRetryPolicy;

import com.datastax.driver.core.SocketOptions;

public class OpushConfigurationFixture extends Configuration {

	public static class Cassandra {
		public String seed = "localhost";
		public String keyspace = "opush";
		public String user = "cassandra";
		public String password = "cassandra";
		public int readTimeoutMs = SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS;
		public CassandraRetryPolicy retryPolicy = CassandraRetryPolicy.ALWAYS_RETRY;
		public int maxRetries = 3;
	}
	
	public static class Mail {
		public boolean activateTls = false;
		public boolean loginWithDomain = true;
		public int timeoutInMilliseconds = 5000;
		public int imapPort = 143;
		public int maxMessageSize = 1024;
		public int fetchBlockSize = 1 << 20;
		public ExpungePolicy expungePolicy = ExpungePolicy.ALWAYS;
	}	
	
	public static class RemoteConsole {
		public boolean enable = true;
		public int port = 0; //random
	}

	public static class SyncPerms {
		public String blacklist = "";
		public boolean allowUnknownDevice = true;
	}

	public SyncPerms syncPerms = new SyncPerms();
	public Mail mail = new Mail();
	public RemoteConsole remoteConsole = new RemoteConsole();
	public Cassandra cassandra = new Cassandra();
	
}
