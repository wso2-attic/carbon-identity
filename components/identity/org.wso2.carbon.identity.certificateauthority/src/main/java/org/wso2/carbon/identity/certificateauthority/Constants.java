package org.wso2.carbon.identity.certificateauthority;

public class Constants {

    //database labels
    public static final String CSR_CONTENT_LABEL = "CSR_CONTENT";
    public static final String CSR_STATUS_LABEL = "STATUS";
    public static final String CSR_REQUESTER_USERNAME_LABEL = "USER_NAME";
    public static final String CSR_REQUESTED_DATE = "REQUESTED_DATE";
    public static final String CSR_DEPARTMENT_LABEL = "DEPARTMENT";
    public static final String CSR_ORGANIZATION_LABEL = "ORGANIZATION";
    public static final String CSR_CITY_LABEL = "CITY";
    public static final String CSR_STATE_LABEL = "STATE";
    public static final String CSR_COUNTRY_LABEL = "COUNTRY";
    public static final String CSR_COMMON_NAME_LABEL = "COMMON_NAME";

    public static final String DEFAULT_LABEL = "default";

    public static final String SERIAL_NO_LABEL = "SERIAL_NO";
    public static final String TENANT_ID_LABEL = "TENANT_ID";
    public static final String USER_STORE_ID_LABEL = "USER_STORE_ID";
    public static final String PC_CONTENT_LABEL = "PUBLIC_CERTIFICATE";
    public static final String PC_ISSUER_LABEL = "USER_NAME";
    public static final String PC_STATUS_LABEL = "STATUS";
    public static final String PC_ISSUDED_DATE = "ISSUED_DATE";
    public static final String PC_EXPIRY_DATE = "EXPIRY_DATE";
    public static final String PC_KEY_STORE = "KEY_STORE";
    public static final String PC_ALIAS = "ALIAS";

    public static final String CRL_CONTENT_LABEL = "BASE64CRL";
    public static final String THIS_UPDATE_LABEL = "THIS_UPDATE";
    public static final String NEXT__UPDATE_LABEL = "NEXT_UPDATE";
    public static final String CRL_NUMBER_LABEL = "CRL_NUMBER";
    public static final String DELTA_INDICATOR_LABEL = "DELTA_CRL_INDICATOR";


    public static final String RC_REV_DATE_LABEL = "REVOKED_DATE";
    public static final String RC_REV_REASON = "REASON";

    public static final String CRL_COMMAND = "cmd";
    public static final String REQUEST_TYPE_CRL = "crl";
    public static final String REQUEST_TYPE_DELTA_CRL = "deltacrl";

    public static final int DEFAULT_HTTP_PORT = 9763;
    public static final String PORT_OFFSET = "Ports.Offset";
    public static final String HOST_NAME = "HostName";
}
