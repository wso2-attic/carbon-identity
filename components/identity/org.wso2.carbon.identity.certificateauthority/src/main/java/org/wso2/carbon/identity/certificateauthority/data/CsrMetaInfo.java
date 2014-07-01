package org.wso2.carbon.identity.certificateauthority.data;

import java.util.Date;

public class CsrMetaInfo {
    String serialNo;
    String commonName;
    String organization;
    String status;
    String userName;
    Date requestedDate;

    public CsrMetaInfo(String serialNo, String commonName, String organization, String status, Date requestedDate, String userName) {
        this.serialNo = serialNo;
        this.commonName = commonName;
        this.organization = organization;
        this.status = status;
        this.requestedDate = requestedDate;
        this.userName = userName;
    }

    public CsrMetaInfo() {

    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(Date requestedDate) {
        this.requestedDate = requestedDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }


}
