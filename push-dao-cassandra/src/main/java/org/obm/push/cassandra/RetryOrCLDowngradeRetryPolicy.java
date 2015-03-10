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
package org.obm.push.cassandra;

import org.obm.push.configuration.CassandraConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.WriteType;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.RetryPolicy;

public class RetryOrCLDowngradeRetryPolicy implements RetryPolicy {

	private static final Logger LOGGER = LoggerFactory.getLogger(RetryOrCLDowngradeRetryPolicy.class);
	
	private final int maxNbRetry;
	private final RetryPolicy readAndWriteRetryPolicy;

	public RetryOrCLDowngradeRetryPolicy(CassandraConfiguration cassandraConfiguration, RetryPolicy readAndWriteRetryPolicy) {
		this.maxNbRetry = cassandraConfiguration.maxRetries();
		this.readAndWriteRetryPolicy = readAndWriteRetryPolicy;
	}

	@Override
	public RetryDecision onReadTimeout(Statement statement, ConsistencyLevel cl, int requiredResponses, int receivedResponses, boolean dataRetrieved, int nbRetry) {
		return readAndWriteRetryPolicy.onReadTimeout(statement, cl, requiredResponses, receivedResponses, dataRetrieved, nbRetry);
	}

	@Override
	public RetryDecision onWriteTimeout(Statement statement, ConsistencyLevel cl, WriteType writeType, int requiredAcks, int receivedAcks, int nbRetry) {
		return readAndWriteRetryPolicy.onWriteTimeout(statement, cl, writeType, requiredAcks, receivedAcks, nbRetry);
	}

	@Override
	public RetryDecision onUnavailable(Statement statement, ConsistencyLevel cl, int requiredReplica, int aliveReplica, int nbRetry) {
		if (nbRetry >= maxNbRetry) {
			return RetryDecision.rethrow();
		}
		LOGGER.warn("Opush is downgrading the consistency-level of a Cassandra request as it cannot reach enough replicas to get the QORUM." + 
				"A 'nodetool repair' might be done on each node when your cluster gets back to a healthy state. Read http://docs.obm.org/opush/ for more information");
		return DowngradingConsistencyRetryPolicy.INSTANCE.onUnavailable(statement, cl, requiredReplica, aliveReplica, 0);
	}
}
