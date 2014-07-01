package org.wso2.carbon.identity.certificateauthority.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.certificateauthority.Constants;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.certificateauthority.data.CsrFile;
import org.wso2.carbon.identity.certificateauthority.data.CsrMetaInfo;
import org.wso2.carbon.identity.certificateauthority.data.CsrStatus;
import org.wso2.carbon.identity.certificateauthority.utils.CsrUtils;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class CsrDAO {
    private static Log log = LogFactory.getLog(CsrDAO.class);

    /**
     * to add a csr to the database
     *
     * @param csrContent base64 encoded csr request
     * @param userName   username of the user logged in
     * @param tenantID   Tenant which the userr is assigned or the request is pointed
     * @return
     */

    public String addCsr(String csrContent, String userName, int tenantID, int userStoreId) throws CertAuthException {

        String csrSerialNo = new BigInteger(32, new SecureRandom()).toString();
        Connection connection = null;
        Date requestDate = new Date();
        String sql = null;
        PreparedStatement prepStmt = null;
        RDN[] orgRdNs = CsrUtils.getCRfromEncodedCsr(csrContent).getSubject().getRDNs(BCStyle.O);
        String organization = "";
        if (orgRdNs.length > 0) {
            organization = orgRdNs[0].getFirst().getValue().toString();
        }
        RDN[] cnRdNs = CsrUtils.getCRfromEncodedCsr(csrContent).getSubject().getRDNs(BCStyle.CN);
        String commonName = "";
        if (cnRdNs.length > 0) {
            commonName = cnRdNs[0].getFirst().getValue().toString();
        }
//        Vector vector = CsrUtils.getCRfromEncodedCsr(csrContent).getCertificationRequestInfo().getSubject().getValues();
//        String organization = vector.get(3).toString();
//        String commonName = CsrUtils.getSubjectInfo(CsrUtils.getCRfromEncodedCsr(csrContent)).get("CN");
//        PKCS10CertificationRequest csr = CsrUtils.getCRfromEncodedCsr(csrContent);
//        HashMap map = CsrUtils.getSubjectInfo(csr);
        try {
            log.debug("adding csr file to database");
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "INSERT INTO CA_CSR_STORE (CSR_CONTENT, STATUS, USER_NAME, REQUESTED_DATE, SERIAL_NO, TENANT_ID,COMMON_NAME,ORGANIZATION,USER_STORE_ID) VALUES (?,?,?,?,?,?,?,?,?) ";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setBlob(1, new ByteArrayInputStream(csrContent.getBytes()));
            prepStmt.setString(2, CsrStatus.PENDING.toString());
            prepStmt.setString(3, userName);
            prepStmt.setTimestamp(4, new Timestamp(requestDate.getTime()));
            prepStmt.setString(5, csrSerialNo);
            prepStmt.setInt(6, tenantID);
            prepStmt.setString(7, commonName);
            prepStmt.setString(8, organization);
            prepStmt.setInt(9, userStoreId);
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
        return csrSerialNo;
    }

    /**
     * query csr  information from database using serial number
     *
     * @param serialNo serial number of the csr request which is stored against in DB
     * @return CSR file
     */
    public CsrFile getCSR(String serialNo, int userStoreId, String userName, int tenantId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving csr information from serial :" + serialNo);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CSR_STORE WHERE SERIAL_NO = ? and USER_NAME= ? AND TENANT_ID =? AND USER_STORE_ID =?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, serialNo);
            prepStmt.setString(2, userName);
            prepStmt.setInt(3, tenantId);
            prepStmt.setInt(4, userStoreId);
            resultSet = prepStmt.executeQuery();
            CsrFile[] csrFiles = getCsrArray(resultSet);
            if (csrFiles != null && csrFiles.length > 0) {
                return csrFiles[0];
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
     * @param serialNo
     * @param tenantId
     * @return
     * @throws CertAuthException
     */
    public CsrFile getCSR(String serialNo, int tenantId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving csr information from serial :" + serialNo);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CSR_STORE WHERE SERIAL_NO = ? and TENANT_ID =? ";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, serialNo);
            prepStmt.setInt(2, tenantId);
            resultSet = prepStmt.executeQuery();
            CsrFile[] csrFiles = getCsrArray(resultSet);
            if (csrFiles != null && csrFiles.length > 0) {
                return csrFiles[0];
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
     * get csr from serial number
     *
     * @param serialNo serial number of the csr
     * @return csr file with relevent serial number if not returns false
     * @throws CertAuthException
     */
    public CsrFile getCSR(String serialNo) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving csr information from serial :" + serialNo);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CSR_STORE WHERE SERIAL_NO = ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, serialNo);
            resultSet = prepStmt.executeQuery();
            CsrFile[] csrFiles = getCsrArray(resultSet);
            if (csrFiles != null && csrFiles.length > 0) {
                return csrFiles[0];
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
     * update the status of csr to the given status
     *
     * @param serialNo serial number of the csr request
     * @param status   status of the csr
     * @return returns 1 if the update is successful, 0 if unsuccessful
     */
    public int updateStatus(String serialNo, CsrStatus status, int tenantID) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        int result = 0;
        try {
            log.debug("updating csr with serial number :" + serialNo);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "UPDATE CA_CSR_STORE SET STATUS= ? WHERE SERIAL_NO= ? AND TENANT_ID= ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, status.toString());
            prepStmt.setString(2, serialNo);
            prepStmt.setInt(3, tenantID);
            result = prepStmt.executeUpdate();
            connection.commit();
            if (result == 1) {
                log.debug("csr with serial number " + serialNo + " status updated to " + status);
            } else {
                log.debug("error while updating csr with serial number " + serialNo);
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
     * delete the csr with given serial number
     *
     * @param serialNo serial number of the csr request
     * @return 1 if the update is successful, 0 if unsuccessful
     */
    public int deleteCSR(String serialNo, int tenantId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        int result = 0;
        String sql = null;
        try {
            log.debug("deleting csr with serial number :" + serialNo);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "DELETE FROM CA_CSR_STORE WHERE SERIAL_NO= ? AND TENANT_ID = ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, serialNo);
            prepStmt.setInt(2, tenantId);
            result = prepStmt.executeUpdate();
            connection.commit();
            if (result == 1) {
                log.debug("csr with serial number " + serialNo + "is deleted");
            } else {
                log.debug("error while deleting csr with serial number " + serialNo);
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
     * given a tenant id, returns the list of csr requests to the tenant
     *
     * @param tenantID tenant id
     * @return array of csr files
     */

    public CsrMetaInfo[] getCSRList(int tenantID) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving csr information for tenantID :" + tenantID);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CSR_STORE WHERE TENANT_ID = ?";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setInt(1, tenantID);
            resultSet = prepStmt.executeQuery();
            return getCsrMetaInfoArray(resultSet);
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

    public CsrMetaInfo[] getCSRListWithStatus(int tenantID, String status) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving csr information for tenantID :" + tenantID);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CSR_STORE WHERE TENANT_ID = ? AND STATUS = ?";
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setInt(1, tenantID);
            prepStmt.setString(2, status);
            resultSet = prepStmt.executeQuery();
            return getCsrMetaInfoArray(resultSet);
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
     * given a username, returns the list of csr requests from the user
     *
     * @param username tenant id
     * @return array of csr files
     */
    public CsrMetaInfo[] getCSRList(String username, int tenantID) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving csr information for tenantID :" + username);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CSR_STORE WHERE USER_NAME = ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, username);
            resultSet = prepStmt.executeQuery();
            return getCsrMetaInfoArray(resultSet);

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
     * constructs and returns a CsrFile array from a resultSet
     *
     * @param resultSet result set
     * @return array of CsrFiles
     */
    private CsrFile[] getCsrArray(ResultSet resultSet) {
        ArrayList<CsrFile> csrList = new ArrayList<CsrFile>();
        int count = 0;

        try {
            while (resultSet.next()) {
                String serialNo = resultSet.getString(Constants.SERIAL_NO_LABEL);
                String status = resultSet.getString(Constants.CSR_STATUS_LABEL);
                String commonName = resultSet.getString(Constants.CSR_COMMON_NAME_LABEL);
                String organization = resultSet.getString(Constants.CSR_ORGANIZATION_LABEL);
                CsrFile csrFile;
                String country = null;
                String department = null;
                String city = null;
                String state = null;
                Blob csrBlob = resultSet.getBlob(Constants.CSR_CONTENT_LABEL);
                String csrContent = new String(csrBlob.getBytes(1, (int) csrBlob.length()));
                Date requestedDate = resultSet.getTimestamp(Constants.CSR_REQUESTED_DATE);
                String username = resultSet.getString(Constants.CSR_REQUESTER_USERNAME_LABEL);
                int tenantID = resultSet.getInt(Constants.TENANT_ID_LABEL);
                int userStoreId = resultSet.getInt(Constants.USER_STORE_ID_LABEL);
                PKCS10CertificationRequest csr = CsrUtils.getCRfromEncodedCsr(csrContent);
                HashMap decodedContent = CsrUtils.getSubjectInfo(csr);
                if (decodedContent.containsKey("C")) {
                    country = decodedContent.get("C").toString();
                }
                if (decodedContent.containsKey("L")) {
                    city = decodedContent.get("L").toString();
                }
                if (decodedContent.containsKey("OU")) {
                    department = decodedContent.get("OU").toString();
                }
                if (decodedContent.containsKey("ST")) {
                    state = decodedContent.get("ST").toString();
                }
                csrFile = new CsrFile(commonName, department, organization, city, state, country, csrContent, serialNo, status, username, tenantID, userStoreId, requestedDate);
                csrList.add(csrFile);
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        CsrFile[] csrFiles = new CsrFile[csrList.size()];
        csrFiles = csrList.toArray(csrFiles);
        return csrFiles;
    }


    private CsrMetaInfo[] getCsrMetaInfoArray(ResultSet resultSet) {
        ArrayList<CsrMetaInfo> csrMetaInfoList = new ArrayList<CsrMetaInfo>();
        int count = 0;
        try {
            while (resultSet.next()) {
                String serialNo = resultSet.getString(Constants.SERIAL_NO_LABEL);
                String status = resultSet.getString(Constants.CSR_STATUS_LABEL);
                String commonName = resultSet.getString(Constants.CSR_COMMON_NAME_LABEL);
                String organization = resultSet.getString(Constants.CSR_ORGANIZATION_LABEL);
                CsrFile csrFile;
                Date requestedDate = resultSet.getTimestamp(Constants.CSR_REQUESTED_DATE);
                String username = resultSet.getString(Constants.CSR_REQUESTER_USERNAME_LABEL);
                CsrMetaInfo csrMetaInfo = new CsrMetaInfo(serialNo, commonName, organization, status, requestedDate, username);
                csrMetaInfoList.add(csrMetaInfo);
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        CsrMetaInfo[] csrMetaFiles = new CsrMetaInfo[csrMetaInfoList.size()];
        csrMetaFiles = csrMetaInfoList.toArray(csrMetaFiles);
        return csrMetaFiles;
    }

    /**
     * get CsrFromTenantID and username
     *
     * @param tenantID
     * @param username
     * @return
     */
    public CsrMetaInfo[] getCsrList(int tenantID, String username, int userStoreId) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving csr information for tenantID :" + username);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CSR_STORE WHERE USER_NAME = ? and TENANT_ID= ? and USER_STORE_ID = ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, username);
            prepStmt.setInt(2, tenantID);
            prepStmt.setInt(3, userStoreId);
            resultSet = prepStmt.executeQuery();
            return getCsrMetaInfoArray(resultSet);
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
     * get csr list which contains given cn isssued for a given tenant
     *
     * @param cn       common name
     * @param tenantID id of the tenant
     * @return CsrMetaInfos for given cn and tenant id
     * @throws CertAuthException
     */
    public CsrMetaInfo[] getCsrListfromCN(String cn, int tenantID) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;
        try {
            log.debug("retriving csr information for tenantID :" + tenantID);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CSR_STORE WHERE COMMON_NAME LIKE ? and TENANT_ID= ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, cn);
            prepStmt.setInt(2, tenantID);
            resultSet = prepStmt.executeQuery();
            return getCsrMetaInfoArray(resultSet);

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
     * get a csr list wich contains the given string as a part of string in Organization
     *
     * @param org      organization name
     * @param tenantID tenant id
     * @return list of CsrMetaInfo
     * @throws CertAuthException
     */
    public CsrMetaInfo[] getCsrListfromOrg(String org, int tenantID) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving csr information for tenantID :");
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM CA_CSR_STORE WHERE COMMON_NAME LIKE ? and TENANT_ID= ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, org);
            prepStmt.setInt(2, tenantID);
            resultSet = prepStmt.executeQuery();
            return getCsrMetaInfoArray(resultSet);

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
