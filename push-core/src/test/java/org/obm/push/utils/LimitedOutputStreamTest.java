/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2015 Linagora
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
package org.obm.push.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;


public class LimitedOutputStreamTest {

	private static final int LIMIT = 5;
	
	private LimitedOutputStream testee;
	
	@Before
	public void setUp() {
		testee = new LimitedOutputStream(LIMIT);
	}
	
	@Test
	public void streamShouldHoldAllDataWhenWriteByteDontReachTheLimit() {
		int size = LIMIT -1;
		
		for (int index = 0 ; index < size ; index++) {
			testee.write(0x0);
		}
		
		assertThat(testee.size()).isEqualTo(size);
		assertThat(testee.toByteArray()).isEqualTo(new byte[size]);
	}
	
	@Test
	public void streamShouldHoldAllDataWhenWriteByteJustReachTheLimit() {
		for (int index = 0 ; index < LIMIT ; index++) {
			testee.write(0x0);
		}
		
		assertThat(testee.size()).isEqualTo(LIMIT);
		assertThat(testee.toByteArray()).isEqualTo(new byte[LIMIT]);
	}

	
	@Test
	public void streamShouldDiscardsExceedingDataWhenWriteByteGoBeyondTheLimit() {
		for (int index = 0 ; index < LIMIT +1 ; index++) {
			testee.write(0x0);
		}
		
		assertThat(testee.size()).isEqualTo(LIMIT);
		assertThat(testee.toByteArray()).isEqualTo(new byte[LIMIT]);
	}
	
	@Test
	public void streamShouldHoldAllDataWhenWriteByteArrayDontReachTheLimit() throws IOException {
		int size = LIMIT -1;
		
		testee.write(new byte[size]);
		
		assertThat(testee.size()).isEqualTo(size);
		assertThat(testee.toByteArray()).isEqualTo(new byte[size]);
	}
	
	@Test
	public void streamShouldHoldAllDataWhenWriteByteArrayJustReachTheLimit() throws IOException {
		testee.write(new byte[LIMIT]);
		
		assertThat(testee.size()).isEqualTo(LIMIT);
		assertThat(testee.toByteArray()).isEqualTo(new byte[LIMIT]);
	}

	
	@Test
	public void streamShouldDiscardsExceedingDataWhenWriteArrayByteGoBeyondTheLimit() throws IOException {
		testee.write(new byte[LIMIT +1]);
		
		assertThat(testee.size()).isEqualTo(LIMIT);
		assertThat(testee.toByteArray()).isEqualTo(new byte[LIMIT]);
	}
	
	@Test
	public void streamShouldHoldAllDataWhenWriteByRangeDontReachTheLimit() {
		int count = LIMIT-1;
		int offset = 2;
		testee.write(new byte[LIMIT+100], offset, count);
		
		assertThat(testee.size()).isEqualTo(count);
		assertThat(testee.toByteArray()).isEqualTo(new byte[count]);
	}
	
	@Test
	public void streamShouldHoldAllDataWhenWriteByRangeJustReachTheLimit() {
		int count = LIMIT;
		int offset = 2;
		testee.write(new byte[LIMIT+100], offset, count);
		
		assertThat(testee.size()).isEqualTo(LIMIT);
		assertThat(testee.toByteArray()).isEqualTo(new byte[LIMIT]);
	}
	
	@Test
	public void streamShouldDiscardsExceedingDataWhenWriteByRangeGoBeyondTheLimit() {
		int count = LIMIT +1;
		int offset = 2;
		testee.write(new byte[LIMIT+100], offset, count);
		
		assertThat(testee.size()).isEqualTo(LIMIT);
		assertThat(testee.toByteArray()).isEqualTo(new byte[LIMIT]);
	}
	
	@Test
	public void streamShouldDiscardsExceedingDataWhenDifferentWritesAreUsed() throws IOException {
		int count = 1;
		int offset = 0;

		testee.write(new byte[LIMIT+100], offset, count);
		testee.write(0x0);
		testee.write(new byte[2]);
		testee.write(0x0);
		testee.write(new byte[LIMIT+100], offset, count);
		
		assertThat(testee.size()).isEqualTo(LIMIT);
		assertThat(testee.toByteArray()).isEqualTo(new byte[LIMIT]);
	}
}
