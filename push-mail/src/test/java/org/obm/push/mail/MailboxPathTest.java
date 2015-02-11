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
package org.obm.push.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TreeSet;

import org.junit.Test;

import com.google.common.collect.Sets;


public class MailboxPathTest {

	private static final char SLASH = '/';

	@SuppressWarnings("unchecked")
	@Test
	public void ordering() {
		TreeSet<Comparable<MailboxPath>> set = Sets.newTreeSet();
		set.add(MailboxPath.of("custom"));
		set.add(MailboxPath.of("INBOX"));
		set.add(MailboxPath.of("Sent/sub/sub/sub"));
		set.add(MailboxPath.of("Trash"));
		set.add(MailboxPath.of("Sent"));
		set.add(MailboxPath.of("Sent/sub"));
		set.add(MailboxPath.of("Sent/sub/sub"));
		
		assertThat(set).containsExactly(
			MailboxPath.of("custom"),
			MailboxPath.of("INBOX"),
			MailboxPath.of("Sent"),
			MailboxPath.of("Sent/sub"),
			MailboxPath.of("Sent/sub/sub"),
			MailboxPath.of("Sent/sub/sub/sub"),
			MailboxPath.of("Trash")
		);
	}
	
	@Test
	public void reducedPathShouldReturnEmptyWhenNoSeparatorFound() {
		assertThat(MailboxPath.of("INBOX", SLASH).reducingPaths()).isEmpty();
	}
	
	@Test
	public void reducedPathShouldReturnOneElementWhenOneSeparatorFound() {
		assertThat(MailboxPath.of("INBOX/sub", SLASH).reducingPaths())
			.containsOnly(MailboxPath.of("INBOX", SLASH));
	}
	
	@Test
	public void reducedPathShouldReturnElementWithTailingAndLeadingSpaces() {
		assertThat(MailboxPath.of(" INBOX /sub", SLASH).reducingPaths())
			.containsOnly(MailboxPath.of(" INBOX ", SLASH));
	}
	
	@Test
	public void reducedPathShouldReturnOneElementWhenOneSeparatorFoundAndSpacesInLast() {
		assertThat(MailboxPath.of("INBOX/sub with space", SLASH).reducingPaths())
			.containsOnly(MailboxPath.of("INBOX", SLASH));
	}
	
	@Test
	public void reducedPathShouldReturnElementsWithLeadingAndTailingWithSpacesInParent() {
		assertThat(MailboxPath.of("INBOX/ subs with spaces /sub", SLASH).reducingPaths())
			.containsExactly(
				MailboxPath.of("INBOX/ subs with spaces ", SLASH),
				MailboxPath.of("INBOX", SLASH)
			);
	}
	
	@Test
	public void reducedPathShouldReturnOneElementWhenTwoSeparatorFoundButOneAsTail() {
		assertThat(MailboxPath.of("INBOX/sub/", SLASH).reducingPaths())
			.containsExactly(MailboxPath.of("INBOX", SLASH));
	}
	
	@Test
	public void reducedPathShouldReturnOneElementWhenTwoSeparatorFoundButOneEmpty() {
		assertThat(MailboxPath.of("INBOX//sub/", SLASH).reducingPaths())
			.containsOnly(MailboxPath.of("INBOX", SLASH));
	}
	
	@Test
	public void reducedPathShouldReturnAllElementsWithDESCOrdering() {
		assertThat(MailboxPath.of("INBOX/sub/sub/sub/sub/sub", SLASH).reducingPaths())
			.containsExactly(
				MailboxPath.of("INBOX/sub/sub/sub/sub", SLASH),
				MailboxPath.of("INBOX/sub/sub/sub", SLASH),
				MailboxPath.of("INBOX/sub/sub", SLASH),
				MailboxPath.of("INBOX/sub", SLASH),
				MailboxPath.of("INBOX", SLASH)
			);
	}
}
