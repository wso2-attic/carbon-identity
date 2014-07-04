package org.wso2.carbon.identity.certificateauthority.endpoint.scep;

public class ScepConstants {

    public static final String OP_PARAM = "operation";
    public static final String MESSAGE_PARAM = "message";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String CA_CAPS = "POSTPKIOperation\n" +
            "SHA-1";
    //Not included in java 1.6
    public static final int METHOD_NOT_ALLOWED = 405;

}
