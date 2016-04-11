/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2016 Linagora
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

import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.obm.push.bean.RecurrenceDayOfWeek;
import org.obm.sync.calendar.RecurrenceDay;
import org.obm.sync.calendar.RecurrenceDays;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class RecurrenceDayOfWeekConverter {
	public static Set<RecurrenceDayOfWeek> fromRecurrenceDays(RecurrenceDays recurrenceDays) {
		Set<RecurrenceDayOfWeek> recurrenceDaysOfWeek = Sets.newHashSet();
		for (RecurrenceDay recurrenceDay : recurrenceDays) {
			RecurrenceDayOfWeek recurrenceDayOfWeek = RecurrenceDayOfWeek.getByIndex(recurrenceDay
					.ordinal());
			recurrenceDaysOfWeek.add(recurrenceDayOfWeek);
		}
		return recurrenceDaysOfWeek;
	}

	public static RecurrenceDays toRecurrenceDays(
			Set<RecurrenceDayOfWeek> recurrenceDaysOfWeek) {
		EnumSet<RecurrenceDay> recurrenceDays = EnumSet.noneOf(RecurrenceDay.class);
		for (RecurrenceDayOfWeek recurrenceDayOfWeek : recurrenceDaysOfWeek) {
			RecurrenceDay recurrenceDay = RecurrenceDay.getByIndex(recurrenceDayOfWeek.ordinal());
			recurrenceDays.add(recurrenceDay);
		}
		return new RecurrenceDays(recurrenceDays);
	}

	public static Map<Integer, RecurrenceDays> JODA_TO_DAYS = ImmutableMap.<Integer, RecurrenceDays>builder()
			.put(DateTimeConstants.MONDAY, new RecurrenceDays(RecurrenceDay.Monday))
			.put(DateTimeConstants.TUESDAY, new RecurrenceDays(RecurrenceDay.Tuesday))
			.put(DateTimeConstants.WEDNESDAY, new RecurrenceDays(RecurrenceDay.Wednesday))
			.put(DateTimeConstants.THURSDAY, new RecurrenceDays(RecurrenceDay.Thursday))
			.put(DateTimeConstants.FRIDAY, new RecurrenceDays(RecurrenceDay.Friday))
			.put(DateTimeConstants.SATURDAY, new RecurrenceDays(RecurrenceDay.Saturday))
			.put(DateTimeConstants.SUNDAY, new RecurrenceDays(RecurrenceDay.Sunday))
			.build();
	
	public static RecurrenceDays byUTCDate(Date date, TimeZone tz) {
		int jodaDateOfWeek = new DateTime(date, DateTimeZone.UTC)
			.withZone(DateTimeZone.forTimeZone(tz))
			.getDayOfWeek();
		
		return JODA_TO_DAYS.get(jodaDateOfWeek);
	}
}
