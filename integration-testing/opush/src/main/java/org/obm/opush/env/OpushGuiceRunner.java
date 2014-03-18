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

import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.guice.GuiceRunnerDelegation;
import org.obm.push.OpushContainerModule.OpushHttpCapability;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class OpushGuiceRunner extends GuiceRunner {

	public OpushGuiceRunner(Class<?> klass) throws InitializationError {
		super(klass, new OpushGuiceRunnerDelegation());
	}

	public static class OpushGuiceRunnerDelegation extends GuiceRunnerDelegation {

		@Override
		protected GuiceStatement buildGuiceStatement(Statement base, Object target, GuiceModule moduleAnnotation) {
			return new OpushGuiceStatement(moduleAnnotation.value(), target, base);
		}

		public static class OpushGuiceStatement extends GuiceStatement { 
			
			public OpushGuiceStatement(Class<? extends Module> module, Object target, Statement next) {
				super(module, target, next);
			}

			@Override
			public void evaluate() throws Throwable {
				Injector baseInjector = Guice.createInjector(instantiateModule());
				Injector httpInjector = baseInjector.getInstance(OpushHttpCapability.class).enableByExtendingInjector();
				httpInjector.injectMembers(target);
				next.evaluate();
			}

			protected Module instantiateModule() throws InstantiationException, IllegalAccessException {
				return module.newInstance();
			}
		}

	}
}
