package org.wso2.carbon.identity.certificateauthority.data;

import java.util.Date;

public class CertificateDTO {

    private CertificateMetaInfo certificateMetaInfo;
    private String encodedCertificate;
    private int tenantID;
    private String userStoreDomain;

    public CertificateDTO(String serialNo, String encodedCertificate, String status, int tenantID, String username, Date issuedDate, Date expiaryDate, String userStoreDomain) {
        this.certificateMetaInfo = new CertificateMetaInfo(serialNo, issuedDate, expiaryDate, username, status);
        this.encodedCertificate = encodedCertificate;
        this.tenantID = tenantID;
        this.userStoreDomain = userStoreDomain;
    }

    public CertificateDTO(CertificateMetaInfo metaInfo, String encodedCertificate, int tenantID, String userStoreDomain) {
        this.certificateMetaInfo = metaInfo;
        this.encodedCertificate = encodedCertificate;
        this.tenantID = tenantID;
        this.userStoreDomain = userStoreDomain;
    }

    public String getEncodedCertificate() {
        return encodedCertificate;
    }

    public void setEncodedCertificate(String encodedCertificate) {
        this.encodedCertificate = encodedCertificate;
    }

    public int getTenantID() {
        return tenantID;
    }

    public void setTenantID(int tenantID) {
        this.tenantID = tenantID;
    }

    public String getUserStoreDomain() {
        return userStoreDomain;
    }

    public void setUserStoreDomain(String userStoreDomain) {
        this.userStoreDomain = userStoreDomain;
    }

    public Date getExpiryDate() {
        return certificateMetaInfo.getExpiryDate();
    }

    public void setExpiryDate(Date expiryDate) {
        certificateMetaInfo.setExpiryDate(expiryDate);
    }

    public Date getIssuedDate() {
        return certificateMetaInfo.getIssuedDate();
    }

    public void setIssuedDate(Date issuedDate) {
        certificateMetaInfo.getIssuedDate();
    }

    public String getUsername() {
        return certificateMetaInfo.getUsername();
    }

    public void setUsername(String username) {
        certificateMetaInfo.setUsername(username);
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

    public CertificateMetaInfo getCertificateMetaInfo() {
        return certificateMetaInfo;
    }


}
