package org.wso2.carbon.identity.certificateauthority.ui;

/**
 * Created by thilini on 6/7/14.
 */
public class CAConstants {
    public static final String CA_ADMIN_CLIENT = "CAAdminServiceClient";
    public static final int DEFAULT_ITEMS_PER_PAGE = 10;

    public static final String REVOCATION_REASON_UNSPECIFIED = "Unspecified";
    public static final String REVOCATION_REASON_KEYCOMPROMISE = "Key Compromise";
    public static final String REVOCATION_REASON_CACOMPROMISE = "CA Compromise";
    public static final String REVOCATION_REASON_AFFILIATIONCHANGED = "Affiliation Changed";
    public static final String REVOCATION_REASON_SUPERSEDED = "Superseded";
    public static final String REVOCATION_REASON_CESSATIONOFOPERATION = "Cessation of Opearation";
    public static final String REVOCATION_REASON_CERTIFICATEHOLD = "Certificate Hold";
    // Value 7 is not used, see RFC5280
    public static final String REVOCATION_REASON_REMOVEFROMCRL = "Remove From CRL";
    public static final String REVOCATION_REASON_PRIVILEGESWITHDRAWN = "Privileges Withdrawn";
    public static final String REVOCATION_REASON_AACOMPROMISE = "AACompromise";

    public static final int REVOCATION_REASON_UNSPECIFIED_VAL = 0;
    public static final int REVOCATION_REASON_KEYCOMPROMISE_VAL = 1;
    public static final int REVOCATION_REASON_CACOMPROMISE_VAL = 2;
    public static final int REVOCATION_REASON_AFFILIATIONCHANGED_VAL = 3;
    public static final int REVOCATION_REASON_SUPERSEDED_VAL = 4;
    public static final int REVOCATION_REASON_CESSATIONOFOPERATION_VAL = 5;
    public static final int REVOCATION_REASON_CERTIFICATEHOLD_VAL = 6;
    // Value 7 is not used, see RFC5280
    public static final int REVOCATION_REASON_REMOVEFROMCRL_VAL = 8;
    public static final int REVOCATION_REASON_PRIVILEGESWITHDRAWN_VAL = 9;
    public static final int REVOCATION_REASON_AACOMPROMISE_VAL = 10;
}
