/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.certificateauthority.dao;

import com.hazelcast.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.certificateauthority.Constants;
import org.wso2.carbon.identity.certificateauthority.data.CRLDataHolder;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;

import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

public class CrlDataHolderDao {
    Log log = LogFactory.getLog(CrlDataHolderDao.class);

    /**
     * add crl data into database table CA_CRL_STORE
     *
     * @param crl               x509 crl
     * @param tenantID          issuer of the crl
     * @param thisUpdate        time of this update
     * @param nextUpdate        time of next crl will be released
     * @param crlNumber         contiuolusly increasing number for a tenant
     * @param deltaCrlIndicator
     * @throws CRLException
     * @throws CertAuthException
     */
    public void addCRL(X509CRL crl, int tenantID, Date thisUpdate, Date nextUpdate, int crlNumber, int deltaCrlIndicator) throws CRLException, CertAuthException {
        Connection connection = null;
        String sql = null;
        PreparedStatement prepStmt = null;
        try {
            log.debug("adding CRL to database");
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            Date date = new Date();
            sql = "INSERT INTO CA_CRL_STORE (BASE64CRL, THIS_UPDATE, NEXT_UPDATE, CRL_NUMBER, DELTA_CRL_INDICATOR, TENANT_ID) VALUES (?,?,?,?,?,?) ";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, new String(Base64.encode((crl).getEncoded())));
            prepStmt.setTimestamp(2, new Timestamp(thisUpdate.getTime()));
            prepStmt.setTimestamp(3, new Timestamp(nextUpdate.getTime()));
            prepStmt.setInt(4, crlNumber);
            prepStmt.setInt(5, deltaCrlIndicator);
            prepStmt.setInt(6, tenantID);
            prepStmt.execute();
            connection.commit();
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
    }

    /**
     * get the latest crl constructed for a tenant
     *
     * @param tenantId id of the tenant
     * @param deltaCrl if delta crl is requested, true and if full crl is requested false
     * @return the latest crl or delta crl
     * @throws CertificateException
     * @throws CertAuthException
     */
    public CRLDataHolder getLatestCRL(int tenantId, boolean deltaCrl) throws CertificateException, CertAuthException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving CRL information from serial :" + tenantId);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            if (deltaCrl) {
                sql = "SELECT * FROM CA_CRL_STORE WHERE TENANT_ID = ?  AND CRL_NUMBER = SELECT MAX(CRL_NUMBER) FROM CA_CRL_STORE WHERE DELTA_CRL_INDICATOR > 0 AND TENANT_ID =?";
            } else {
                sql = "SELECT * FROM CA_CRL_STORE WHERE TENANT_ID = ?  AND CRL_NUMBER = SELECT MAX(CRL_NUMBER) FROM CA_CRL_STORE WHERE DELTA_CRL_INDICATOR = -1 AND TENANT_ID =?";
            }
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setInt(1, tenantId);
            prepStmt.setInt(2, tenantId);
            resultSet = prepStmt.executeQuery();
            CRLDataHolder[] crls = getCrl(resultSet);
            if (crls != null && crls.length > 0) {
                return crls[0];
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


    /**
     * returns an array of CRLDataholder from result set
     *
     * @param resultSet result set from the db query
     * @return CRKDataHolder Array
     * @throws CertificateException
     */
    private CRLDataHolder[] getCrl(ResultSet resultSet) throws CertificateException {
        ArrayList<CRLDataHolder> crlList = new ArrayList<CRLDataHolder>();
        int count = 0;
        try {
            while (resultSet.next()) {
                CRLDataHolder crlDataHolder = null;
                String base64crl = resultSet.getString(Constants.CRL_CONTENT_LABEL);
                Date thisUpdate = resultSet.getTimestamp(Constants.THIS_UPDATE_LABEL);
                Date nextUpdate = resultSet.getTimestamp(Constants.NEXT__UPDATE_LABEL);
                int tenantID = resultSet.getInt(Constants.TENANT_ID_LABEL);
                int crlNumber = resultSet.getInt(Constants.CRL_NUMBER_LABEL);
                int deltaCrlIndicator = resultSet.getInt(Constants.DELTA_INDICATOR_LABEL);
                crlDataHolder = new CRLDataHolder(thisUpdate, nextUpdate, base64crl, tenantID, crlNumber, deltaCrlIndicator);
                crlList.add(crlDataHolder);
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        CRLDataHolder[] crls = new CRLDataHolder[crlList.size()];
        crls = crlList.toArray(crls);
        return crls;
    }

    /**
     * find the highest number out of the clrs from a given tenant
     *
     * @param tenantId id of the tenant
     * @param deltaCrl true if the required number is for delta crl, false unless
     * @return current highest number of the crl
     * @throws CertificateException
     * @throws CertAuthException
     */
    public int findHighestCrlNumber(int tenantId, boolean deltaCrl) throws CertificateException, CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving maximum crl number for tenantDomain :" + tenantId);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            if (deltaCrl) {
                sql = "SELECT MAX(CRL_NUMBER) AS CRL FROM CA_CRL_STORE WHERE TENANT_ID = ?  AND DELTA_CRL_INDICATOR >0 ";
            } else {
                sql = "SELECT MAX(CRL_NUMBER) AS CRL FROM CA_CRL_STORE WHERE TENANT_ID = ?  AND DELTA_CRL_INDICATOR =-1 ";
            }

            prepStmt = connection.prepareStatement(sql);
            prepStmt.setInt(1, tenantId);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("CRL");
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
        return 0;
    }


}
