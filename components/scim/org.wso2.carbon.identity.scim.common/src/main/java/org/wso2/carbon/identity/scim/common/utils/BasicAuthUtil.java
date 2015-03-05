package org.wso2.carbon.identity.scim.common.utils;

import org.apache.axiom.om.util.Base64;

public class BasicAuthUtil {
    public static String getBase64EncodedBasicAuthHeader(String userName, String password) {
        String concatenatedCredential = userName + ":" + password;
        byte[] byteValue = concatenatedCredential.getBytes();
        String encodedAuthHeader = Base64.encode(byteValue);
        encodedAuthHeader = "Basic " + encodedAuthHeader;
        return encodedAuthHeader;
    }
}