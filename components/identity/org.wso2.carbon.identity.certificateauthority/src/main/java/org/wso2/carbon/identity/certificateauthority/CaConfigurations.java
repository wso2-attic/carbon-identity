package org.wso2.carbon.identity.certificateauthority;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CaConfigurations {

    private static Log log = LogFactory.getLog(CaConfigurations.class);

    public static String getKeyStoreName(int tenantId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving tenant configurations from tenantID :" + tenantId);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CONFIGURATIONS WHERE TENANT_ID= ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setInt(1, tenantId);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(Constants.PC_KEY_STORE);
            }

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new CertAuthException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + sql);
            log.error(e.getMessage(), e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return null;
    }

    public static String getAlias(int tenantId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving csr information for tenantID :");
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CONFIGURATIONS WHERE TENANT_ID= ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setInt(1, tenantId);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(Constants.PC_ALIAS);
            }

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new CertAuthException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + sql);
            log.error(e.getMessage(), e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return null;
    }

    public static void setKeyStoreNameAndAlias(int tenantId, String keyStoreName, String alias) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving tenant configurations from tenantID :" + tenantId);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CONFIGURATIONS WHERE TENANT_ID= ?";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setInt(1, tenantId);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                sql = "UPDATE CA_CONFIGURATIONS SET KEY_STORE_NAME= ? WHERE TENANT_ID= ?";
                prepStmt = connection.prepareStatement(sql);
                prepStmt.setString(1, keyStoreName);
                prepStmt.setInt(2, tenantId);
                prepStmt.executeUpdate();
            } else {
                sql = "INSERT INTO CA_CONFIGURATIONS (TENANT_ID, KEY_STORE , ALIAS) VALUES (?,?,?) ";
                prepStmt = connection.prepareStatement(sql);

                prepStmt.setInt(1, tenantId);
                prepStmt.setString(2, keyStoreName);
                prepStmt.setString(3, alias);
                prepStmt.execute();
            }

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new CertAuthException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + sql);
            log.error(e.getMessage(), e);
            throw new CertAuthException(e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

    }
}
