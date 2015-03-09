/* ***** BEGIN LICENSE BLOCK *****
 * 
<<<<<<< HEAD
 * Copyright (C) 2014  Linagora
=======
 * Copyright (C) 2014 Linagora
>>>>>>> 6cce91c... OBMFULL-5830 Add status command
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
package org.obm.push.bean.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.obm.push.bean.migration.Version;

public class VersionTest {

	@Test(expected=IllegalArgumentException.class)
	public void testZeroVersionThrowsException() {
		Version.of(0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNegativeVersionThrowsException() {
		Version.of(-1);
	}

	@Test
	public void testVersion() {
		assertThat(Version.of(1).get()).isEqualTo(1);
	}
	
	@Test
	public void isLessThanWhenLess() {
		assertThat(Version.of(4).isLessThan(Version.of(5))).isTrue();
	}
	
	@Test
	public void isLessThanWhenEquals() {
		assertThat(Version.of(5).isLessThan(Version.of(5))).isFalse();
	}
	
	@Test
	public void isLessThanWhenGreater() {
		assertThat(Version.of(5).isLessThan(Version.of(4))).isFalse();
	}
	
	@Test
	public void isGreaterThanOrEqualWhenLess() {
		assertThat(Version.of(4).isGreaterThanOrEqual(Version.of(5))).isFalse();
	}
	
	@Test
	public void isGreaterThanOrEqualWhenEquals() {
		assertThat(Version.of(5).isGreaterThanOrEqual(Version.of(5))).isTrue();
	}
	
	@Test
	public void isGreaterThanOrEqualWhenGreater() {
		assertThat(Version.of(5).isGreaterThanOrEqual(Version.of(4))).isTrue();
	}
}
