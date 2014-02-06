package org.wso2.carbon.identity.authorization.core.jdbc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.dao.GenericDAO;
import org.wso2.carbon.identity.authorization.core.dao.ModuleResourceDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.util.DatabaseUtil;

public class JDBCModuleResourceDAO extends ModuleResourceDAO {

	private static Log log = LogFactory.getLog(JDBCModuleResourceDAO.class);

	@Override
	protected void insert(PreparedStatement stmt, ResultSet res, Connection connection)
	                                                                                   throws SQLException,
	                                                                                   UserStoreException {
		String sql = "INSERT INTO UM_MODULE_RESOURCE (UM_RESOURCE, UM_MODULE_ID) VALUES(?, ?) ";

		stmt = connection.prepareStatement(sql);
		byte count = 0;
		stmt.setString(++count, getResource());
		stmt.setInt(++count, getModuleId());

		int resCount = stmt.executeUpdate();
		if (resCount == 0) {
			String error = "Insertion faild for the module " + getResource();
			log.error(error);
			throw new UserStoreException(error);
		}
	}

	@Override
	protected void update(Connection connection, boolean commit) throws UserStoreException {
		String sql = " UPDATE UM_MODULE_RESOURCE SET UM_RESOURCE = ? WHERE UM_ID = ?";
		DatabaseUtil.updateDatabase(connection, sql, getResource(), getIdentifier());

		if (commit) {
			try {
				connection.commit();
			} catch (SQLException e) {
				log.error(e);
				throw new UserStoreException(e);
			}
		}

	}

	@Override
	protected void delete(Connection connection, boolean commit) throws UserStoreException {
		String sql = " DELETE FROM UM_MODULE_RESOURCE WHERE UM_ID = ?";
		DatabaseUtil.updateDatabase(connection, sql, getIdentifier());

		if (commit) {
			try {
				connection.commit();
			} catch (SQLException e) {
				log.error(e);
				throw new UserStoreException(e);
			}
		}

	}

	@Override
	protected void saveDependentModules(Connection connection, boolean commit)
	                                                                          throws UserStoreException {

	}

	@Override
	public List<? extends GenericDAO> load(Connection connection, boolean closeConnection)
	                                                                                      throws UserStoreException {
		PreparedStatement stmt = null;
		ResultSet res = null;
		resetAppendTxt();
		try {
			StringBuilder sql =
			                    new StringBuilder(
			                                      "SELECT UM_ID, UM_RESOURCE, UM_MODULE_ID FROM UM_MODULE_RESOURCE ");
			if (getModuleId() > 0) {
				sql.append(" WHERE UM_MODULE_ID = ? ");
			}

			stmt = connection.prepareStatement(sql.toString());
			byte count = 0;
			if (getModuleId() > 0) {
				stmt.setInt(++count, getModuleId());
			}
			res = stmt.executeQuery();
			List<ModuleResourceDAO> dataList = new ArrayList<ModuleResourceDAO>();
			ModuleResourceDAO moduel = null;
			while (res.next()) {
				moduel = new JDBCModuleResourceDAO();
				dataList.add(moduel);
				moduel.setModuleId(res.getInt("UM_MODULE_ID"));
				moduel.setResource(res.getString("UM_RESOURCE"));
				moduel.setId(res.getInt("UM_ID"));
			}
			return dataList;

		} catch (SQLException e) {
			log.error("Error while loading resources for the id: " + getModuleId(), e);
			throw new UserStoreException("Error while loading module resources ", e);
		} finally {
			if (closeConnection) {
				DatabaseUtil.closeAllConnections(connection, res, stmt);
			} else {
				DatabaseUtil.closeAllConnections(null, res, stmt);
			}
		}
	}

	@Override
	public List<? extends GenericDAO> load(Connection connection) throws UserStoreException {
		return load(connection, true);
	}

	@Override
	public int getIdentifier() {
		return getId();
	}

}
