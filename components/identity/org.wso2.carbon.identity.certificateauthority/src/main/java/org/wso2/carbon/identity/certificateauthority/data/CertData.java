package org.wso2.carbon.identity.certificateauthority.data;

import java.security.cert.X509Certificate;
import java.util.Date;

public class CertData {
    private String SerialNo;
    private X509Certificate[] certificateChain;
    private Date issuedDate;
    private Date expiaryDate;

    public CertData(String serialNo, X509Certificate[] certificateChain, Date issuedDate, Date expiaryDate) {
        SerialNo = serialNo;
        this.certificateChain = certificateChain;
        this.issuedDate = issuedDate;
        this.expiaryDate = expiaryDate;
    }

    public String getSerialNo() {
        return SerialNo;
    }

    public X509Certificate[] getCertificateChain() {
        return certificateChain;
    }

    public Date getIssuedDate() {
        return issuedDate;
    }

    public Date getExpiaryDate() {
        return expiaryDate;
    }
}
