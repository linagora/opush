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
package org.obm.push.cassandra.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.obm.DateUtils.dateUTC;
import static org.obm.push.cassandra.dao.CassandraStructure.Windowing.Columns.CHANGE_TYPE;
import static org.obm.push.cassandra.dao.CassandraStructure.Windowing.Columns.CHANGE_VALUE;

import org.cassandraunit.CassandraCQLUnit;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.obm.push.dao.testsuite.WindowingDaoTest;
import org.obm.push.mail.EmailChanges;
import org.obm.push.mail.EmailChanges.Builder;
import org.obm.push.mail.bean.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableList;

public class WindowingDaoCassandraImplTest extends WindowingDaoTest {

	private static final String KEYSPACE = "opush";
	private static final String WINDOWING_CQL = "windowing.cql";
	@Rule public CassandraCQLUnit cassandraCQLUnit = new CassandraCQLUnit(new ClassPathCQLDataSet(WINDOWING_CQL, KEYSPACE), "cassandra.yaml", "localhost", 9042);
	
	private Logger logger = LoggerFactory.getLogger(SyncedCollectionDaoCassandraImplTest.class);
	private WindowingDaoCassandraImpl testeeImpl;
	private IMocksControl mocks;
	
	@Before
	public void init() {
		testee = new WindowingDaoCassandraImpl(cassandraCQLUnit.session, new PublicJSONService(), logger);
		testeeImpl = (WindowingDaoCassandraImpl)testee;
		mocks = createControl();
	}
	
	@Test
	public void buildEmailChangesWhenNone() {
		EmailChanges emailChanges = testeeImpl.buildEmailChanges(ImmutableList.<Row>of());
		assertThat(emailChanges.sumOfChanges()).isZero();
	}
	
	@Test
	public void buildEmailChangesWhenAdd() {
		Email change = buildChange();
		
		Row row1 = mocks.createMock(Row.class);
		expect(row1.getString(CHANGE_TYPE)).andReturn("ADD");
		expect(row1.getString(CHANGE_VALUE)).andReturn(testeeImpl.jsonService.serialize(change));
		
		mocks.replay();
		EmailChanges emailChanges = testeeImpl.buildEmailChanges(ImmutableList.of(row1));
		mocks.verify();
		
		assertThat(emailChanges.additions()).containsOnly(change);
		assertThat(emailChanges.changes()).isEmpty();
		assertThat(emailChanges.deletions()).isEmpty();
	}
	
	@Test
	public void buildEmailChangesWhenChange() {
		Email change = buildChange();
		
		Row row1 = mocks.createMock(Row.class);
		expect(row1.getString(CHANGE_TYPE)).andReturn("CHANGE");
		expect(row1.getString(CHANGE_VALUE)).andReturn(testeeImpl.jsonService.serialize(change));
		
		mocks.replay();
		EmailChanges emailChanges = testeeImpl.buildEmailChanges(ImmutableList.of(row1));
		mocks.verify();
		
		assertThat(emailChanges.changes()).containsOnly(change);
		assertThat(emailChanges.additions()).isEmpty();
		assertThat(emailChanges.deletions()).isEmpty();
	}
	
	@Test
	public void buildEmailChangesWhenDeletion() {
		Email change = buildChange();
		
		Row row1 = mocks.createMock(Row.class);
		expect(row1.getString(CHANGE_TYPE)).andReturn("DELETE");
		expect(row1.getString(CHANGE_VALUE)).andReturn(testeeImpl.jsonService.serialize(change));
		
		mocks.replay();
		EmailChanges emailChanges = testeeImpl.buildEmailChanges(ImmutableList.of(row1));
		mocks.verify();
		
		assertThat(emailChanges.deletions()).containsOnly(change);
		assertThat(emailChanges.additions()).isEmpty();
		assertThat(emailChanges.changes()).isEmpty();
	}
	
	@Test
	public void buildEmailChangesWhenTwoOfEach() {
		Email add1 = buildChange(1l);
		Email add2 = buildChange(2l);
		Email change1 = buildChange(3l);
		Email change2 = buildChange(4l);
		Email del1 = buildChange(5l);
		Email del2 = buildChange(6l);
		
		Row row1 = mocks.createMock(Row.class);
		expect(row1.getString(CHANGE_TYPE)).andReturn("ADD");
		expect(row1.getString(CHANGE_VALUE)).andReturn(testeeImpl.jsonService.serialize(add1));
		Row row2 = mocks.createMock(Row.class);
		expect(row2.getString(CHANGE_TYPE)).andReturn("ADD");
		expect(row2.getString(CHANGE_VALUE)).andReturn(testeeImpl.jsonService.serialize(add2));
		Row row3 = mocks.createMock(Row.class);
		expect(row3.getString(CHANGE_TYPE)).andReturn("CHANGE");
		expect(row3.getString(CHANGE_VALUE)).andReturn(testeeImpl.jsonService.serialize(change1));
		Row row4 = mocks.createMock(Row.class);
		expect(row4.getString(CHANGE_TYPE)).andReturn("CHANGE");
		expect(row4.getString(CHANGE_VALUE)).andReturn(testeeImpl.jsonService.serialize(change2));
		Row row5 = mocks.createMock(Row.class);
		expect(row5.getString(CHANGE_TYPE)).andReturn("DELETE");
		expect(row5.getString(CHANGE_VALUE)).andReturn(testeeImpl.jsonService.serialize(del1));
		Row row6 = mocks.createMock(Row.class);
		expect(row6.getString(CHANGE_TYPE)).andReturn("DELETE");
		expect(row6.getString(CHANGE_VALUE)).andReturn(testeeImpl.jsonService.serialize(del2));
		
		mocks.replay();
		EmailChanges emailChanges = testeeImpl.buildEmailChanges(ImmutableList.of(row1, row2, row3, row4, row5, row6));
		mocks.verify();
		
		assertThat(emailChanges.additions()).containsOnly(add1, add2);
		assertThat(emailChanges.changes()).containsOnly(change1, change2);
		assertThat(emailChanges.deletions()).containsOnly(del1, del2);
	}
	
	@Test
	public void putChangeWhenNullDoNothing() {
		Email change = buildChange();
		EmailChanges emailChanges = putChangeAs(change, null);
		assertThat(emailChanges.additions()).isEmpty();
		assertThat(emailChanges.changes()).isEmpty();
		assertThat(emailChanges.deletions()).isEmpty();
	}
	
	@Test
	public void putChangeWhenNotExistingDoNothing() {
		Email change = buildChange();
		EmailChanges emailChanges = putChangeAs(change, "ILLEGAL TYPE");
		assertThat(emailChanges.additions()).isEmpty();
		assertThat(emailChanges.changes()).isEmpty();
		assertThat(emailChanges.deletions()).isEmpty();
	}
	
	@Test
	public void putChangeWhenAdd() {
		Email change = buildChange();
		EmailChanges emailChanges = putChangeAs(change, "ADD");
		assertThat(emailChanges.deletions()).isEmpty();
		assertThat(emailChanges.changes()).isEmpty();
		assertThat(emailChanges.additions()).containsOnly(change);
	}
	
	@Test
	public void putChangeWhenChange() {
		Email change = buildChange();
		EmailChanges emailChanges = putChangeAs(change, "CHANGE");
		assertThat(emailChanges.deletions()).isEmpty();
		assertThat(emailChanges.additions()).isEmpty();
		assertThat(emailChanges.changes()).containsOnly(change);
	}
	
	@Test
	public void putChangeWhenDeletion() {
		Email change = buildChange();
		EmailChanges emailChanges = putChangeAs(change, "DELETE");
		assertThat(emailChanges.additions()).isEmpty();
		assertThat(emailChanges.changes()).isEmpty();
		assertThat(emailChanges.deletions()).containsOnly(change);
	}

	private EmailChanges putChangeAs(Email change, String type) {
		Builder emailChangesBuilder = EmailChanges.builder();
		testeeImpl.putChange(emailChangesBuilder, type, testeeImpl.jsonService.serialize(change));
		return emailChangesBuilder.build();
	}

	private Email buildChange() {
		return buildChange(2l);
	}
	
	private Email buildChange(long uid) {
		return Email.builder()
				.read(true)
				.uid(uid)
				.date(dateUTC("2013-04-07T12:09:37"))
				.build();
	}
}
