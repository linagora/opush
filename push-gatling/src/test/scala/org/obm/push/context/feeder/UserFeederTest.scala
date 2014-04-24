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
package org.obm.push.context.feeder

import scala.collection.Iterator
import scala.collection.TraversableOnce.flattenTraversableOnce
import org.junit.runner.RunWith
import org.obm.push.context.UserKey
import org.scalatest.Finders
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UserFeederTest extends FunSuite {
	
	test("Feeder gives right number of items when the rootItems is infinite") {
		val rootItems = Iterator.continually(Array(1, 2, 3)).flatten
		val generateSessionData = (a: UserKey, b: Int) => Map(a.key + "transformed" -> b)
		val feeder = new ItemFeeder(rootItems, generateSessionData, new UserKey("keya"), new UserKey("keyb")).build
		
		val firstIteration = feeder.next
		assert(firstIteration === Map("keyatransformed" -> 1, "keybtransformed" -> 2))
		val secondIteration = feeder.next
		assert(secondIteration === Map("keyatransformed" -> 3, "keybtransformed" -> 1))
		val thirdIteration = feeder.next
		assert(thirdIteration === Map("keyatransformed" -> 2, "keybtransformed" -> 3))
	}
	
	test("Feeder gives only remainging items when the rootItems is not infinite") {
		val rootItems = Array(1, 2, 3).iterator
		val generateSessionData = (a: UserKey, b: Int) => Map(a.key + "transformed" -> b)
		val feeder = new ItemFeeder(rootItems, generateSessionData, new UserKey("keya"), new UserKey("keyb")).build
		
		val firstIteration = feeder.next
		assert(firstIteration === Map("keyatransformed" -> 1, "keybtransformed" -> 2))
		val secondIteration = feeder.next
		assert(secondIteration === Map("keyatransformed" -> 3))
	}

}