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

import org.obm.push.utils.jvm.VMArgumentsUtils;

import com.google.common.base.Objects;

public class ServerConfiguration {

	public static ServerConfiguration defaultConfiguration() {
		return builder().build();
	}
	
	public static ServerConfigurationBuilder builder() {
		return new ServerConfigurationBuilder();
	}
	
	public static class ServerConfigurationBuilder {

		private static final int DEFAULT_THREADPOOL_SIZE = Objects.firstNonNull( 
				VMArgumentsUtils.integerArgumentValue("threadPoolSize"), 200);
		private static final int DEFAULT_SELECTOR_COUNT = 10;
		private static final int JETTY_SELECTED_PORT = 0;
		
		private int port = JETTY_SELECTED_PORT;
		private int threadPoolSize = DEFAULT_THREADPOOL_SIZE;
		private int selectorCount = DEFAULT_SELECTOR_COUNT;
		
		private ServerConfigurationBuilder() {
		}
		
		public ServerConfigurationBuilder jettySelectorPort() {
			this.port = JETTY_SELECTED_PORT;
			return this;
		}
		
		public ServerConfigurationBuilder port(int port) {
			this.port = port;
			return this;
		}
		
		public ServerConfigurationBuilder threadPoolSize(int size) {
			this.threadPoolSize = size;
			return this;
		}
		
		public ServerConfigurationBuilder selectorCount(int count) {
			this.selectorCount = count;
			return this;
		}
		
		public ServerConfiguration build() {
			return new ServerConfiguration(port, threadPoolSize, selectorCount);
		}
	}
	
	private final int port;
	private final int threadPoolSize;
	private final int selectorCount;
	
	private ServerConfiguration(int port, int threadPoolSize, int selectorCount) {
		super();
		this.port = port;
		this.threadPoolSize = threadPoolSize;
		this.selectorCount = selectorCount;
	}

	public int selectorCount() {
		return selectorCount;
	}
	
	public int port() {
		return port;
	}
	
	public int threadPoolSize() {
		return threadPoolSize;
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(port, threadPoolSize, selectorCount);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof ServerConfiguration) {
			ServerConfiguration that = (ServerConfiguration) object;
			return Objects.equal(this.port, that.port)
				&& Objects.equal(this.threadPoolSize, that.threadPoolSize)
				&& Objects.equal(this.selectorCount, that.selectorCount);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("port", port)
			.add("threadPoolSize", threadPoolSize)
			.add("selectorCount", selectorCount)
			.toString();
	}
}