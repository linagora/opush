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
package org.obm.push.bean.change.hierarchy;

import java.util.List;

import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;

public class MailboxPath implements Comparable<MailboxPath>, BackendId {

	public static final char DEFAULT_SEPARATOR = '/';
	
	public static MailboxPath of(String path) {
		return new MailboxPath(path, DEFAULT_SEPARATOR);
	}
	
	public static MailboxPath of(String path, char separator) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(path));
		return new MailboxPath(path, separator);
	}
	
	private final String path;
	private final char separator;
	
	private MailboxPath(String path, char separator) {
		this.path = path;
		this.separator = separator;
	}

	@Override
	public String asString() {
		return path;
	}

	public String getPath() {
		return path;
	}

	public char getSeparator() {
		return separator;
	}

	public Iterable<MailboxPath> reducingPaths() {
		Builder<MailboxPath> paths = ImmutableList.builder();
		List<String> pieces = Splitter.on(separator).omitEmptyStrings().splitToList(path);
		for (int index = pieces.size() - 1 ; index > 0; index--) {
			String path = Joiner.on(separator).join(Iterables.limit(pieces, index));
			paths.add(MailboxPath.of(path, separator));
		}
		return paths.build();
	}

	@Override
	public int compareTo(MailboxPath o) {
		return asString().compareToIgnoreCase(o.asString());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(path, separator);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MailboxPath) {
			MailboxPath that = (MailboxPath)obj;
			return Objects.equal(path, that.path)
				&& Objects.equal(separator, that.separator);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("path", path)
			.add("separator", separator)
			.toString();
	}
}
