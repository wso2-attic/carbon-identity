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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.certificateauthority.Constants;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.certificateauthority.data.Certificate;
import org.wso2.carbon.identity.certificateauthority.data.CertificateMetaInfo;
import org.wso2.carbon.identity.certificateauthority.data.CertificateStatus;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

public class CertificateDAO {
    Log log = LogFactory.getLog(CertificateDAO.class);

    /**
     * adds a public certificate to the database
     *
     * @param serial   serial number of the certificate
     * @param tenantID id of the tenant tenant who issued the certificate
     * @return
     */
    public void addPublicCertificate(String serial, X509Certificate certificate, int tenantID, String username, int userStoreId) throws CertAuthException {
        Connection connection = null;
        Date requestDate = new Date();
        String sql = null;
        PreparedStatement prepStmt = null;
        try {
            Date expiryDate = certificate.getNotAfter();

            log.debug("adding public certificate file to database");
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "INSERT INTO CA_CERTIFICATE_STORE (SERIAL_NO,PUBLIC_CERTIFICATE,STATUS,ISSUED_DATE,EXPIRY_DATE,TENANT_ID,USER_NAME,USER_STORE_ID) VALUES (?,?,?,?,?,?,?,?) ";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, serial);
            prepStmt.setBlob(2, new ByteArrayInputStream(certificate.getEncoded()));
            prepStmt.setString(3, CertificateStatus.ACTIVE.toString());
            prepStmt.setTimestamp(4, new Timestamp(requestDate.getTime()));
            prepStmt.setTimestamp(5, new Timestamp(expiryDate.getTime()));
            prepStmt.setInt(6, tenantID);
            prepStmt.setString(7, username);
            prepStmt.setInt(8, userStoreId);
            prepStmt.execute();
            connection.commit();
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new CertAuthException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + sql);
            log.error(e.getMessage(), e);
        } catch (CertificateEncodingException e) {
            log.error("Error encoding certificate");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    /**
     * update pc status to a given string
     *
     * @param serialNo serial number of the PC
     * @param status   Status of the PC
     * @return 1 if the update is successfull, 0 if not
     */
    public int updatePCStatus(String serialNo, String status) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String sql = null;
        int result = 0;
        try {
            log.debug("updating PC with serial number :" + serialNo);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "UPDATE CA_CERTIFICATE_STORE SET STATUS= ? WHERE SERIAL_NO= ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, status);
            prepStmt.setString(2, serialNo);
            result = prepStmt.executeUpdate();
            connection.commit();
            if (result == 1) {
                log.debug("PC with serial number " + serialNo + " status updated to " + status);
            } else {
                log.debug("error while updating PC with serial number " + serialNo);
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
        return result;
    }

    /**
     * get CSR
     *
     * @return returns an array of revoked certificates
     */

    public CertificateMetaInfo[] getRevokedPCList() throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving revoked certificates");
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CERTIFICATE_STORE WHERE STATUS = ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, CertificateStatus.REVOKED.toString());
            resultSet = prepStmt.executeQuery();
            return getPublicCertificateMetaInfoArray(resultSet);

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
     * convert result set to an array of publicCertificates
     *
     * @param resultSet resultSet
     * @return an Array of PublicCertificates
     */

    public Certificate[] getPublicCertificateArray(ResultSet resultSet) {
        ArrayList<Certificate> pcList = new ArrayList<Certificate>();
        int count = 0;
        try {
            while (resultSet.next()) {
                Certificate pc = null;
                String serialNo = resultSet.getString(Constants.SERIAL_NO_LABEL);
                String status = resultSet.getString(Constants.PC_STATUS_LABEL);
                Date expiryDate = resultSet.getTimestamp(Constants.PC_EXPIRY_DATE);


                Blob pcBlob = resultSet.getBlob(Constants.PC_CONTENT_LABEL);
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(pcBlob.getBinaryStream());
                String pcContent = new String(pcBlob.getBytes(1, (int) pcBlob.length()));
                Date issuedDate = resultSet.getTimestamp(Constants.PC_ISSUDED_DATE);
                String username = resultSet.getString(Constants.PC_ISSUER_LABEL);
                int tenantID = resultSet.getInt(Constants.TENANT_ID_LABEL);
                int userStoreId = resultSet.getInt(Constants.USER_STORE_ID_LABEL);
                pc = new Certificate(serialNo, certificate, status, tenantID, username, issuedDate, expiryDate, userStoreId);


                pcList.add(pc);
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        Certificate[] pcFiles = new Certificate[pcList.size()];
        pcFiles = pcList.toArray(pcFiles);
        return pcFiles;
    }

    /**
     * converts a result set to a CertificateMetaInfo Array
     *
     * @param resultSet result set
     * @return CertificateMetaInfoArray
     */
    public CertificateMetaInfo[] getPublicCertificateMetaInfoArray(ResultSet resultSet) {
        ArrayList<CertificateMetaInfo> pcList = new ArrayList<CertificateMetaInfo>();
        int count = 0;
        try {
            while (resultSet.next()) {
                CertificateMetaInfo pcMeta = null;
                String serialNo = resultSet.getString(Constants.SERIAL_NO_LABEL);
                String status = resultSet.getString(Constants.PC_STATUS_LABEL);
                Date expiryDate = resultSet.getTimestamp(Constants.PC_EXPIRY_DATE);
                Date issuedDate = resultSet.getTimestamp(Constants.PC_ISSUDED_DATE);
                String username = resultSet.getString(Constants.PC_ISSUER_LABEL);
                pcMeta = new CertificateMetaInfo(serialNo, issuedDate, expiryDate, username, status);
                pcList.add(pcMeta);
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        CertificateMetaInfo[] pcFiles = new CertificateMetaInfo[pcList.size()];
        pcFiles = pcList.toArray(pcFiles);
        return pcFiles;
    }

    /**
     * to get public certificate from a serial number
     *
     * @param serialNo serial number of the certificate
     * @return Certificate
     */
    public Certificate getPubCert(String serialNo, int tenantID) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving PC information from serial :" + serialNo);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CERTIFICATE_STORE WHERE SERIAL_NO = ? AND TENANT_ID = ?";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, serialNo);
            prepStmt.setInt(2, tenantID);
            resultSet = prepStmt.executeQuery();
            Certificate[] pcs = getPublicCertificateArray(resultSet);
            if (pcs != null && pcs.length > 0) {
                return pcs[0];
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
     * get public certificate from serial number
     *
     * @param serialNo serial number of the requested certificate
     * @return public certificate with requested serial number
     * @throws CertAuthException
     */
    public Certificate getPubCert(String serialNo) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving PC information from serial :" + serialNo);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CERTIFICATE_STORE WHERE SERIAL_NO = ? ";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, serialNo);
            resultSet = prepStmt.executeQuery();
            Certificate[] pcs = getPublicCertificateArray(resultSet);
            if (pcs != null && pcs.length > 0) {
                return pcs[0];
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
     * get the public certificate with given serial number for a user
     *
     * @param serialNo    serial number of the certificate
     * @param tenantID    tenant
     * @param username    username
     * @param userStoreId user store id of the user
     * @return certificate with requested details
     * @throws CertAuthException
     */
    public Certificate getPubCert(String serialNo, int tenantID, String username, int userStoreId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving PC information from serial :" + serialNo);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CERTIFICATE_STORE WHERE SERIAL_NO = ? AND TENANT_ID = ? AND USER_STORE_ID =? AND USER_NAME = ?";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, serialNo);
            prepStmt.setInt(2, tenantID);
            prepStmt.setInt(3, userStoreId);
            prepStmt.setString(4, username);
            resultSet = prepStmt.executeQuery();
            Certificate[] pcs = getPublicCertificateArray(resultSet);
            if (pcs != null && pcs.length > 0) {
                return (Certificate) pcs[0];
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
     * get list of abstract certificates from tenantID
     *
     * @param tenantID
     * @return
     * @throws CertAuthException
     */

    public Certificate[] getRevokedPubCertsDecoded(String tenantID) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving revoked certificates");
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CERTIFICATE_STORE WHERE STATUS = ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, CertificateStatus.REVOKED.toString());
            resultSet = prepStmt.executeQuery();
            return getPublicCertificateArray(resultSet);

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
     * get all certificates issued by a tenant
     *
     * @param tenantId id of the tenant
     * @return set of certificate meta infos of all the certificates issued by the given tenant
     * @throws CertAuthException
     */
    public CertificateMetaInfo[] getCertificates(int tenantId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving certificates issued by :" + tenantId);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CERTIFICATE_STORE WHERE TENANT_ID = ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setInt(1, tenantId);
            resultSet = prepStmt.executeQuery();
            return getPublicCertificateMetaInfoArray(resultSet);

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
     * get all the certificates of a tenant with given status
     *
     * @param status   status of the certificate
     * @param tenantId tenant Id
     * @return
     * @throws CertAuthException
     */
    public CertificateMetaInfo[] getCerts(String status, int tenantId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving revoked certificates");
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CERTIFICATE_STORE WHERE STATUS = ? AND TENANT_ID =?";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, CertificateStatus.valueOf(status).toString());
            prepStmt.setInt(2, tenantId);
            resultSet = prepStmt.executeQuery();
            return getPublicCertificateMetaInfoArray(resultSet);
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
     * get all the certificates issued for a particular user
     *
     * @param username username of the user
     * @param tenantId tenant id
     * @return
     * @throws CertAuthException
     */
    public CertificateMetaInfo[] getCertsFromUsername(String username, int tenantId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving revoked certificates");
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CERTIFICATE_STORE WHERE USER_NAME = ? AND TENANT_ID =?";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, username);
            prepStmt.setInt(2, tenantId);
            resultSet = prepStmt.executeQuery();
            return getPublicCertificateMetaInfoArray(resultSet);
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


}
