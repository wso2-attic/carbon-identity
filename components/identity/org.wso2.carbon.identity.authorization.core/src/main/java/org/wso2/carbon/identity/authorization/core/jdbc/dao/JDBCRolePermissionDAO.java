/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.authorization.core.jdbc.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.AuthorizationKey;
import org.wso2.carbon.identity.authorization.core.dao.RolePermissionDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JDBCRolePermissionDAO extends RolePermissionDAO {

    private static Log log = LogFactory.getLog(JDBCRolePermissionDAO.class);

    @Override
    protected void insert(PreparedStatement stmt, ResultSet res, Connection connection)
            throws SQLException,
            UserStoreException {
        String sql =
                "INSERT INTO UM_ROLE_PERMISSION (UM_PERMISSION_ID, UM_ROLE_NAME, UM_IS_ALLOWED, UM_TENANT_ID) VALUES(?,?,?,?) ";

        stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        byte count = 0;
        stmt.setInt(++count, getPermissionId());
        stmt.setString(++count, getRoleName());
        stmt.setBoolean(++count, isAuthorized());
        stmt.setInt(++count, getTenantId());

        int resCount = stmt.executeUpdate();
        if (resCount == 0) {
            String error = "Insertion faild for the permission";
            log.error(error);
            throw new UserStoreException(error);
        }
        res = stmt.getGeneratedKeys();
        if (res.next()) {
            setId(res.getInt(1));
        }

    }

    @Override
    protected void update(Connection connection, boolean commit) throws UserStoreException {
        StringBuilder sql =
                new StringBuilder(
                        "UPDATE UM_ROLE_PERMISSION SET UM_IS_ALLOWED = ? WHERE ");
        if (getId() > 0) {
            sql.append(" UM_ID = ? ");
            DatabaseUtil.updateDatabase(connection, sql.toString(), isAuthorized(), getId());
        } else {
            sql.append(" UM_PERMISSION_ID = ? AND UM_ROLE_NAME = ? AND UM_TENANT_ID = ? ");
            DatabaseUtil.updateDatabase(connection, sql.toString(), isAuthorized(),
                    getPermissionId(), getRoleName(), getTenantId());
        }

    }

    @Override
    protected void delete(Connection connection, boolean commit) throws UserStoreException {
        StringBuilder sql = new StringBuilder("DELETE FROM UM_ROLE_PERMISSION WHERE ");
        if (getId() > 0) {
            sql.append(" UM_ID = ? ");
            DatabaseUtil.updateDatabase(connection, sql.toString(), getId());
        } else {
            sql.append(" UM_PERMISSION_ID = ? AND UM_ROLE_NAME = ? AND UM_TENANT_ID = ? ");
            DatabaseUtil.updateDatabase(connection, sql.toString(), getPermissionId(),
                    getRoleName(), getTenantId());
        }

    }

    @Override
    public Map<AuthorizationKey, Boolean> createCacheEntry(Connection connection)
            throws UserStoreException {
        JDBCPermissionDAO permission = new JDBCPermissionDAO();
        permission.setPermissionId(getPermissionId());
        List<JDBCPermissionDAO> permissions = (List<JDBCPermissionDAO>) permission.load(connection);
        Map<AuthorizationKey, Boolean> cacheEntry = null;
        if (permissions != null && !permissions.isEmpty()) {

            permission = permissions.get(0);
            AuthorizationKey key =
                    new AuthorizationKey(null, permission.getTenantId(), null,
                            permission.getResourceId(),
                            permission.getAction(),
                            permission.getModuleId(), getRoleName());
            cacheEntry = new HashMap<AuthorizationKey, Boolean>();
            cacheEntry.put(key, isAuthorized());

        }
        return cacheEntry;
    }
}
