/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2013 Linagora
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
package org.obm.provisioning;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.obm.provisioning.bean.LdapGroup;
import org.obm.provisioning.bean.LdapUser;
import org.obm.provisioning.bean.LdapUserMembership;
import org.obm.provisioning.exception.ConnectionException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import com.google.inject.Inject;
import com.google.inject.Singleton;

public class ConnectionImpl implements Connection {

	@Singleton
	public static class Factory {
		private final Configuration configuration;
		
		@Inject
		private Factory(Configuration configuration) {
			this.configuration = configuration;
		}
	
		public ConnectionImpl create() {
			return new ConnectionImpl(configuration);
		}
	}
	
	public static final int NO_LIMIT = 0;
	private final Configuration configuration;
	
	private LdapConnection connection;
	private AtomicInteger requestCounter;

	protected ConnectionImpl(Configuration configuration) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		this.configuration = configuration;
		requestCounter = new AtomicInteger();
		initializeConnection();
	}
	
	private void initializeConnection() throws org.obm.provisioning.exception.LdapException, ConnectionException {
		try {
			requestCounter.set(0);
			connection = new LdapNetworkConnection(configuration.getNetworkConfiguration());
			connection.bind(configuration.getBindDn(), configuration.getBindPassword());
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		} catch (IOException e) {
			throw new org.obm.provisioning.exception.ConnectionException(e);
		}
	}

	private void incrementAndCheckRequestCounter() {
		if (configuration.maxRequests() == NO_LIMIT) {
			return;
		}
		if (requestCounter.incrementAndGet() >= configuration.maxRequests()) {
			synchronized (requestCounter) {
				shutdown();
				initializeConnection();
			}
		}
	}

	@Override
	public void createUser(LdapUser ldapUser) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		try {
			createEntry(ldapUser.buildEntry());
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}
	
	@Override
	public void createGroup(LdapGroup ldapGroup) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		try {
			createEntry(ldapGroup.buildEntry());
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}
	
	private void createEntry(Entry entry) throws org.obm.provisioning.exception.LdapException {
		try {
			connection.add(entry);
			incrementAndCheckRequestCounter();
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}
	
	@Override
	public void deleteUser(LdapUser.Id ldapUserId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		try {
			connection.delete(getUserDnFromUserId(ldapUserId));
			incrementAndCheckRequestCounter();
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}
	
	@VisibleForTesting Dn getUserDnFromUserId(LdapUser.Id ldapUserId) throws LdapException {
		return getDn(configuration.getUserBaseDn(), configuration.buildUserFilter(ldapUserId), configuration.getUserSearchScope());
	}

	@Override
	public void deleteGroup(LdapGroup.Id ldapGroupId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		try {
			connection.delete(getGroupDnFromGroupId(ldapGroupId));
			incrementAndCheckRequestCounter();
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}
	
	@VisibleForTesting Dn getGroupDnFromGroupId(LdapGroup.Id ldapGroupId) throws LdapException {
		return getDn(configuration.getGroupBaseDn(), configuration.buildGroupFilter(ldapGroupId), configuration.getGroupSearchScope());
	}
	
	private Dn getDn(Dn baseDn, String filter, SearchScope scope) throws LdapException {
		return getEntry(baseDn, filter, scope).getDn();
	}
	
	@VisibleForTesting Entry getEntry(Dn baseDn, String filter, SearchScope scope) throws LdapException {
		Cursor<Entry> entries = connection.search(baseDn, filter, scope, SchemaConstants.ALL_ATTRIBUTES_ARRAY);
		incrementAndCheckRequestCounter();
		ImmutableList<Entry> entriesList = FluentIterable.from(entries).toList();
		Preconditions.checkState(entriesList.size() == 1,
				"Entry has not been found or too many group found for the filter " + filter + " in the base " + baseDn.getName() + " with scope " + scope);
		return entriesList.get(0);
	}

	@Override
	public void addUserToGroup(LdapUserMembership ldapUserMembership, LdapGroup.Id ldapGroupId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		modifyGroup(ldapGroupId, ldapUserMembership.buildAddModifications());
	}
	
	@Override
	public void addUsersToGroup(List<LdapUserMembership> ldapUserMemberships, LdapGroup.Id ldapGroupId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		Modification[] modifications = new Modification[0];
		for (LdapUserMembership ldapUserMembership: ldapUserMemberships) {
			modifications = ObjectArrays.concat(modifications, ldapUserMembership.buildAddModifications(), Modification.class);
		}
		modifyGroup(ldapGroupId, modifications);
	}
	
	private void modifyGroup(LdapGroup.Id ldapGroupId, Modification[] modifications) throws org.obm.provisioning.exception.LdapException {
		try {
			Dn group = getGroupDnFromGroupId(ldapGroupId);
			connection.modify(group, modifications);
			incrementAndCheckRequestCounter();
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}

	@Override
	public void removeUserFromGroup(LdapUserMembership ldapUserMembership, LdapGroup.Id ldapGroupId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		modifyGroup(ldapGroupId, ldapUserMembership.buildRemoveModifications());
	}

	@Override
	public void removeUsersFromGroup(List<LdapUserMembership> ldapUserMemberships, LdapGroup.Id ldapGroupId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		Modification[] modifications = new Modification[0];
		for (LdapUserMembership ldapUserMembership: ldapUserMemberships) {
			modifications = ObjectArrays.concat(modifications, ldapUserMembership.buildRemoveModifications(), Modification.class);
		}
		modifyGroup(ldapGroupId, modifications);
	}

	@Override
	public void shutdown() throws ConnectionException {
		try {
			connection.close();
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
	}

	@Override
	public void addGroupToGroup(LdapGroup.Id ldapGroupId, LdapGroup.Id toLdapGroupId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		try {
			Entry entry = getGroupEntry(ldapGroupId);
			Entry toEntry = getGroupEntry(toLdapGroupId);
			
			modifyGroupByGroup(toEntry, entry, ModificationOperation.ADD_ATTRIBUTE);
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}

	@Override
	public void removeGroupFromGroup(LdapGroup.Id ldapGroupId, LdapGroup.Id fromLdapGroupId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		try {
			Entry entry = getGroupEntry(ldapGroupId);
			Entry fromEntry = getGroupEntry(fromLdapGroupId);
			
			modifyGroupByGroup(fromEntry, entry, ModificationOperation.REMOVE_ATTRIBUTE);
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}
	
	private void modifyGroupByGroup(Entry entry, Entry byEntry, ModificationOperation modificationOperation) throws LdapException {
		List<Modification> modifications = memberModificationsFromEntry(byEntry, modificationOperation);
		
		connection.modify(entry.getDn(), modifications.toArray(new Modification[] {}));
		incrementAndCheckRequestCounter();
	}

	private List<Modification> memberModificationsFromEntry(Entry entry, ModificationOperation modificationOperation) {
		List<Modification> modifications = Lists.newArrayList();
		for (Value<?> value : entry.get(new AttributeType("member"))) {
			modifications.add(new DefaultModification(modificationOperation, "member", value));
		}
		return modifications;
	}
	
	@Override
	public void addGroupsToGroup(List<LdapGroup.Id> ldapGroupIds, LdapGroup.Id toLdapGroupId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		try {
			modifyMembersFromGroups(ldapGroupIds, toLdapGroupId, ModificationOperation.ADD_ATTRIBUTE);
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}

	@Override
	public void removeGroupsFromGroup(List<LdapGroup.Id> ldapGroupIds, LdapGroup.Id fromLdapGroupId) throws org.obm.provisioning.exception.LdapException, ConnectionException {
		try {
			modifyMembersFromGroups(ldapGroupIds, fromLdapGroupId, ModificationOperation.REMOVE_ATTRIBUTE);
		} catch (LdapException e) {
			throw new org.obm.provisioning.exception.LdapException(e);
		}
	}
	
	private void modifyMembersFromGroups(List<LdapGroup.Id> ldapGroupIds, LdapGroup.Id modifiedLdapGroupId, 
			ModificationOperation modificationOperation) throws LdapException {
		
		List<Modification> modifications = Lists.newArrayList();
		for (LdapGroup.Id ldapGroupId : ldapGroupIds) {
			Entry entry = getGroupEntry(ldapGroupId);
			
			modifications.addAll(memberModificationsFromEntry(entry, modificationOperation));
		}
		
		Entry fromEntry = getGroupEntry(modifiedLdapGroupId);
		connection.modify(fromEntry.getDn(), modifications.toArray(new Modification[] {}));
		incrementAndCheckRequestCounter();
	}
	
	private Entry getGroupEntry(LdapGroup.Id ldapGroupId) throws LdapException {
		return getEntry(configuration.getGroupBaseDn(),
				configuration.buildGroupFilter(ldapGroupId), 
				configuration.getGroupSearchScope());
	}
}