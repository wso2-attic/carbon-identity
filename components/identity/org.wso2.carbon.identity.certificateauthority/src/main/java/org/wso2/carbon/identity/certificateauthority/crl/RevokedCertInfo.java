package org.wso2.carbon.identity.certificateauthority.crl;


public class RevokedCertInfo {

    public static final int NOT_REVOKED = -1;
    public static final int REVOCATION_REASON_UNSPECIFIED = 0;
    public static final int REVOCATION_REASON_KEYCOMPROMISE = 1;
    public static final int REVOCATION_REASON_CACOMPROMISE = 2;
    public static final int REVOCATION_REASON_AFFILIATIONCHANGED = 3;
    public static final int REVOCATION_REASON_SUPERSEDED = 4;
    public static final int REVOCATION_REASON_CESSATIONOFOPERATION = 5;
    public static final int REVOCATION_REASON_CERTIFICATEHOLD = 6;
    // Value 7 is not used, see RFC5280
    public static final int REVOCATION_REASON_REMOVEFROMCRL = 8;
    public static final int REVOCATION_REASON_PRIVILEGESWITHDRAWN = 9;
    public static final int REVOCATION_REASON_AACOMPROMISE = 10;
}
