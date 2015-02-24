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
package org.obm.push.mail;

import org.easymock.EasyMock;
import org.obm.configuration.EmailConfiguration;
import org.obm.push.ExpungePolicy;
import org.obm.push.LinagoraImapClientModule;
import org.obm.push.bean.ICollectionPathHelper;
import org.obm.push.configuration.OpushEmailConfiguration;
import org.obm.push.exception.OpushLocatorException;
import org.obm.push.mail.greenmail.GreenMailPortProvider;
import org.obm.push.mail.greenmail.GreenMailProviderModule;
import org.obm.push.mail.greenmail.GreenMailSmtpProvider;
import org.obm.push.mail.imap.LinagoraMailboxService;
import org.obm.push.mail.imap.MinigStoreClient;
import org.obm.push.mail.imap.MinigStoreClientImpl;
import org.obm.push.mail.imap.idle.IdleClient;
import org.obm.push.mail.smtp.SmtpProvider;
import org.obm.push.mail.transformer.Identity;
import org.obm.push.mail.transformer.Transformer;
import org.obm.push.minig.imap.IdleClientImpl;
import org.obm.push.resource.ResourcesHolder;
import org.obm.push.service.EventService;
import org.obm.push.service.OpushLocatorService;
import org.obm.sync.client.login.LoginService;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.util.Modules;

public class MailEnvModule extends AbstractModule {

	private final int imapTimeout;

	public MailEnvModule() {
		this(3600000);
	}
	
	public MailEnvModule(int imapTimeout) {
		this.imapTimeout = imapTimeout; 
	}

	@Override
	protected void configure() {
		install(new OpushGreenMailProviderModule());
		bind(OpushEmailConfiguration.class).to(OpushGreenMailEmailConfiguration.class);
		bind(SmtpProvider.class).to(GreenMailSmtpProvider.class);

		install(new LinagoraImapClientModule());
		final ResourcesHolder resourcesHolder = new ResourcesHolder();
		bindScope(RequestScoped.class, new Scope() {
			
			@Override
			public <T> Provider<T> scope(final Key<T> key, Provider<T> unscoped) {
				return new Provider<T>() {

					@SuppressWarnings("unchecked")
					@Override
					public T get() {
						if (key.getTypeLiteral().getRawType().equals(ResourcesHolder.class)) {
							return (T) resourcesHolder;
						}
						return null;
					}
				};
			}
		});
		bind(MailboxService.class).to(LinagoraMailboxService.class);
		bind(MinigStoreClient.Factory.class).to(MinigStoreClientImpl.Factory.class);
		bind(IdleClient.Factory.class).to(IdleClientImpl.Factory.class);
		
		bind(ICollectionPathHelper.class).to(TestingCollectionPathHelper.class);
		bind(EventService.class).toInstance(EasyMock.createMock(EventService.class));
		bind(LoginService.class).toInstance(EasyMock.createMock(LoginService.class));
		bind(OpushLocatorService.class).toInstance(new OpushLocatorService() {
			
			@Override
			public String getServiceLocation(String serviceSlashProperty,
					String loginAtDomain) throws OpushLocatorException {
				return "127.0.0.1";
			}
		});

		bind(Integer.class).annotatedWith(Names.named("imapTimeout")).toInstance(imapTimeout);

		bind(MailViewToMSEmailConverter.class).to(MailViewToMSEmailConverterImpl.class);
		Multibinder<Transformer.Factory> transformers = 
				Multibinder.newSetBinder(binder(), Transformer.Factory.class);
		transformers.addBinding().to(Identity.Factory.class);
	}
	
	private static class OpushGreenMailProviderModule extends AbstractModule {

		@Override
		protected void configure() {
			install(Modules.override(new GreenMailProviderModule())
				.with(new AbstractModule() {
					
					@Override
					protected void configure() {
						bind(EmailConfiguration.class).to(OpushGreenMailEmailConfiguration.class);
					}
				}));
		}
	}
	
	@Singleton
	private static class OpushGreenMailEmailConfiguration implements OpushEmailConfiguration {

		private GreenMailPortProvider greenMailPortProvider;
		private int imapTimeout;

		@Inject
		private OpushGreenMailEmailConfiguration(GreenMailPortProvider greenMailPortProvider, @Named("imapTimeout")int imapTimeout) {
			this.greenMailPortProvider = greenMailPortProvider;
			this.imapTimeout = imapTimeout;
		}

		@Override
		public boolean activateTls() {
			return false;
		}

		@Override
		public boolean loginWithDomain() {
			return true;
		}

		@Override
		public int imapPort() {
			return greenMailPortProvider.imapPort();
		}

		@Override
		public int imapTimeoutInMilliseconds() {
			return imapTimeout;
		}

		@Override
		public String imapMailboxDraft() {
			return IMAP_DRAFTS_NAME;
		}

		@Override
		public String imapMailboxSent() {
			return IMAP_SENT_NAME;
		}

		@Override
		public String imapMailboxTrash() {
			return IMAP_TRASH_NAME;
		}

		@Override
		public MailboxNameCheckPolicy mailboxNameCheckPolicy() {
			return MailboxNameCheckPolicy.ALWAYS;
		}
		
		@Override
		public ExpungePolicy expungePolicy() {
			return OpushEmailConfiguration.IMAP_EXPUNGE_POLICY_DEFAULT_VALUE;
		}

		@Override
		public int getMessageMaxSize() {
			return OpushEmailConfiguration.MESSAGE_MAX_SIZE_DEFAULT_VALUE;
		}
	}
}
