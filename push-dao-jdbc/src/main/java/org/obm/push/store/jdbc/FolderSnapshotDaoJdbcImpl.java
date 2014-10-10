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
package org.obm.push.store.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.obm.breakdownduration.bean.Watch;
import org.obm.dbcp.DatabaseConnectionProvider;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.Device;
import org.obm.push.exception.DaoException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.state.FolderSyncKey;
import org.obm.push.store.FolderSnapshotDao;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@Watch(BreakdownGroups.SQL)
public class FolderSnapshotDaoJdbcImpl extends AbstractJdbcImpl implements FolderSnapshotDao {

	@Inject
	/* allow cglib proxy */ FolderSnapshotDaoJdbcImpl(DatabaseConnectionProvider dbcp) {
		super(dbcp);
	}
	
	@Override
	public void createFolderSnapshot(Integer folderSyncStateId, Set<CollectionId> collectionIds) 
			throws DaoException {
		String statement = "INSERT INTO opush_folder_snapshot " +
				"(folder_sync_state_id, collection_id) VALUES (?, ?)";
		
		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) {

			ps.setInt(1, folderSyncStateId);
			for (CollectionId collectionId : collectionIds) {
				ps.setInt(2, collectionId.asInt());
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			throw new DaoException(e);
		}
	}

	@Override
	public List<CollectionId> getFolderSnapshot(Integer folderSyncStateId) throws DaoException {
		String statement = "SELECT collection_id FROM opush_folder_snapshot " +
				"WHERE folder_sync_state_id = ?";

		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) {
 
			ps.setInt(1, folderSyncStateId);

			try (ResultSet rs = ps.executeQuery()) {
				List<CollectionId> collectionIds = Lists.newArrayList();
				while (rs.next()) {
					collectionIds.add(CollectionId.of(rs.getInt("collection_id")));
				}
				return collectionIds;
			}
		} catch (SQLException e) {
			throw new DaoException(e);
		}
	}

	@Override
	public List<CollectionId> getFolderSnapshot(FolderSyncKey folderSyncKey) throws DaoException {
		String statement = "SELECT collection_id FROM opush_folder_snapshot " +
				"INNER JOIN opush_folder_sync_state ON opush_folder_sync_state.id = folder_sync_state_id " +
				"WHERE sync_key = ?";
		
		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) {
			
			ps.setString(1, folderSyncKey.asString());

			try (ResultSet rs = ps.executeQuery()) {
				List<CollectionId> collectionIds = Lists.newArrayList();
				while (rs.next()) {
					collectionIds.add(CollectionId.of(rs.getInt("collection_id")));
				}
				return collectionIds;
			}
		} catch (SQLException e) {
			throw new DaoException(e);
		}
	}

	@Override
	public Integer getFolderSyncStateId(CollectionId collectionId, Device device) throws DaoException {
		String statement = "SELECT folder_sync_state_id FROM opush_folder_snapshot " +
				"INNER JOIN opush_folder_sync_state ON opush_folder_sync_state.id = folder_sync_state_id " +
				"WHERE collection_id = ? " +
				"AND device_id = ?";
		
		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) {
			
			ps.setInt(1, collectionId.asInt());
			ps.setInt(2, device.getDatabaseId());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("folder_sync_state_id");
				}
			}
		} catch (SQLException e) {
			throw new DaoException(e);
		}
		return null;
	}

	@Override
	public String getFolderSyncKey(CollectionId collectionId, Device device) throws DaoException {
		String statement = "SELECT sync_key FROM opush_folder_sync_state " +
				"INNER JOIN opush_folder_snapshot ON opush_folder_snapshot.folder_sync_state_id = opush_folder_sync_state.id " +
				"WHERE collection_id = ? " +
				"AND device_id = ?";
		
		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) { 
		
			ps.setInt(1, collectionId.asInt());
			ps.setInt(2, device.getDatabaseId());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getString("sync_key");
				}
			}
		} catch (SQLException e) {
			throw new DaoException(e);
		}
		return null;
	}

	@Override
	public String getFolderSyncKey(String collection, Device device) throws DaoException {
		String statement = "SELECT sync_key FROM opush_folder_sync_state " +
				"INNER JOIN opush_folder_snapshot ON opush_folder_snapshot.folder_sync_state_id = opush_folder_sync_state.id " +
				"INNER JOIN opush_folder_mapping ON opush_folder_mapping.id = opush_folder_snapshot.collection_id " +
				"WHERE collection = ? " +
				"AND opush_folder_sync_state.device_id = ?";
		
		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) { 
		
			ps.setString(1, collection);
			ps.setInt(2, device.getDatabaseId());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getString("sync_key");
				}
			}
		} catch (SQLException e) {
			throw new DaoException(e);
		}
		return null;
	}
}
