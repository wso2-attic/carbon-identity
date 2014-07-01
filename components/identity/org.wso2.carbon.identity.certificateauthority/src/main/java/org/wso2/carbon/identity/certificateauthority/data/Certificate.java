package org.wso2.carbon.identity.certificateauthority.data;

import java.util.Date;

public class Certificate {
    private CertificateMetaInfo certificateMetaInfo;
    private String publicCertificate;
    private int tenantID;
    private int userStoreId;

    public Certificate(String serialNo, String publicCertificate, String status, int tenantID, String username, Date issuedDate, Date expiaryDate, int userStoreid) {
        this.certificateMetaInfo = new CertificateMetaInfo(serialNo, issuedDate, expiaryDate, username, status);
        this.publicCertificate = publicCertificate;
        this.tenantID = tenantID;
        this.userStoreId = userStoreid;
    }

    public Date getExpiryDate() {
        return certificateMetaInfo.getExpiryDate();
    }

    public void setExpiryDate(Date expiryDate) {
        certificateMetaInfo.setExpiryDate(expiryDate);
    }

    public int getUserStoreId() {
        return userStoreId;
    }

    public void setUserStoreId(int userStoreId) {
        this.userStoreId = userStoreId;
    }

    public Date getIssuedDate() {
        return certificateMetaInfo.getIssuedDate();
    }

    public void setIssuedDate(Date issuedDate) {
        certificateMetaInfo.getIssuedDate();
    }

    public String getPublicCertificate() {
        return publicCertificate;
    }

    public void setPublicCertificate(String publicCertificate) {
        this.publicCertificate = publicCertificate;
    }


    public int getTenantID() {
        return tenantID;
    }

    public void setTenantID(int tenantID) {
        this.tenantID = tenantID;
    }

    public String getUsername() {
        return certificateMetaInfo.getUsername();
    }

    public void setUsername(String username) {
        certificateMetaInfo.setUsername(username);
    }

    public Date getExpiaryDate() {
        return certificateMetaInfo.getExpiryDate();
    }

    public void setExpiaryDate(Date expiaryDate) {
        certificateMetaInfo.setExpiryDate(expiaryDate);
    }

    public String getStatus() {
        return certificateMetaInfo.getStatus();
    }

    public void setStatus(String status) {
        certificateMetaInfo.setStatus(status);
    }

    public String getSerialNo() {
        return certificateMetaInfo.getSerialNo();
    }

    public void setSerialNo(String serialNo) {
        certificateMetaInfo.setSerialNo(serialNo);
    }

}
