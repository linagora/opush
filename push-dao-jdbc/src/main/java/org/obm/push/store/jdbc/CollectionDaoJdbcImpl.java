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
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.obm.breakdownduration.bean.Watch;
import org.obm.dbcp.DatabaseConnectionProvider;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.bean.Device;
import org.obm.push.bean.ItemSyncState;
import org.obm.push.bean.SyncKey;
import org.obm.push.exception.DaoException;
import org.obm.push.protocol.bean.CollectionId;
import org.obm.push.store.CollectionDao;
import org.obm.push.utils.JDBCUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@Watch(BreakdownGroups.SQL)
public class CollectionDaoJdbcImpl extends AbstractJdbcImpl implements CollectionDao {

	private static final String SYNC_STATE_ITEM_TABLE = "opush_sync_state";
	private static final String SYNC_STATE_FIELDS = 
			Joiner.on(',').join("id", "last_sync", "sync_key");
	
	@Inject
	/* allow cglib proxy */ CollectionDaoJdbcImpl(DatabaseConnectionProvider dbcp) {
		super(dbcp);
	}

	@Override
	public void resetCollection(Device device, CollectionId collectionId) throws DaoException {
		String statement = "DELETE FROM opush_sync_state WHERE device_id=? AND collection_id=?";
		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) {
			ps.setInt(1, device.getDatabaseId());
			ps.setInt(2, collectionId.asInt());
			Stopwatch stopwatch = Stopwatch.createStarted();
			ps.executeUpdate();

			logger.warn("mappings & states cleared for sync of collection {} of device {}",
					collectionId, device.getDevId());
			logger.warn("Deletion time: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
		} catch (SQLException e) {
			throw new DaoException(e);
		}
	}

	@Override
	public ItemSyncState updateState(Device device, CollectionId collectionId, SyncKey syncKey, Date syncDate) throws DaoException {
		String statement = "INSERT INTO opush_sync_state (sync_key, device_id, last_sync, collection_id) VALUES (?, ?, ?, ?)";
		
		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) {
		
			ps.setString(1, syncKey.getSyncKey());
			ps.setInt(2, device.getDatabaseId());
			ps.setTimestamp(3, new Timestamp(syncDate.getTime()));
			ps.setInt(4, collectionId.asInt());
			
			if (ps.executeUpdate() == 0) {
				throw new DaoException("No SyncState inserted");
			} else {
				return ItemSyncState.builder()
						.syncDate(syncDate)
						.syncKey(syncKey)
						.id(dbcp.lastInsertId(con))
						.build();
			}
		} catch (SQLException e) {
			throw new DaoException(e);
		}
	}
	
	@Override
	public ItemSyncState findItemStateForKey(SyncKey syncKey) throws DaoException {
		String statement = "SELECT " + SYNC_STATE_FIELDS + " FROM " + SYNC_STATE_ITEM_TABLE + " WHERE sync_key=?";

		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) {
			
			ps.setString(1, syncKey.getSyncKey());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return buildItemSyncState(rs);
				}
			}
		} catch (SQLException e) {
			throw new DaoException(e);
		}
		return null;
	}

	@Override
	public ItemSyncState lastKnownState(Device device, CollectionId collectionId) throws DaoException {
		String statement = "SELECT " + SYNC_STATE_FIELDS + " FROM opush_sync_state " +
				"WHERE device_id=? AND collection_id=? ORDER BY last_sync DESC LIMIT 1";
		
		try (Connection con = dbcp.getConnection();
				PreparedStatement ps = con.prepareStatement(statement)) {
			
			ps.setInt(1, device.getDatabaseId());
			ps.setInt(2, collectionId.asInt());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return buildItemSyncState(rs);
				}
			}
		} catch (SQLException e) {
			throw new DaoException(e);
		}
		return null;
	}
	
	private ItemSyncState buildItemSyncState(ResultSet rs) throws SQLException {
		Date lastSync = JDBCUtils.getDate(rs, "last_sync");
		SyncKey syncKey = new SyncKey(rs.getString("sync_key"));
		ItemSyncState syncState = ItemSyncState.builder()
				.syncKey(syncKey)
				.syncDate(lastSync)
				.id(rs.getInt("id"))
				.build();
		return syncState;
	}
}
