package org.wso2.carbon.identity.certificateauthority.data;

import java.util.Date;

public class CertificateMetaInfo {
    String serialNo;
    Date issuedDate;
    Date expiryDate;
    String username;
    String status;

    public CertificateMetaInfo(String serialNo, Date issuedDate, Date expiryDate, String username, String status) {
        this.serialNo = serialNo;
        this.issuedDate = issuedDate;
        this.expiryDate = expiryDate;
        this.username = username;
        this.expiryDate = expiryDate;
        this.status = status;
    }

    public CertificateMetaInfo() {

    }

    public Date getExpiryDate() {

        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(Date issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }


}
