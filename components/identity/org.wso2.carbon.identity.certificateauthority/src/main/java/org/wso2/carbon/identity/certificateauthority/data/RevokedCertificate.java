package org.wso2.carbon.identity.certificateauthority.data;

import java.util.Date;

public class RevokedCertificate {

    String serialNo;
    Date revokedDate;
    int reason;

    public RevokedCertificate(String serialNo, Date revokedDate, int reason) {
        this.serialNo = serialNo;
        this.revokedDate = revokedDate;
        this.reason = reason;
    }

    public String getSerialNo() {
        return serialNo;

    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }


    public Date getRevokedDate() {
        return revokedDate;
    }

    public void setRevokedDate(Date revokedDate) {
        this.revokedDate = revokedDate;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }


}
