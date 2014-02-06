package org.wso2.carbon.identity.scim.common.utils;

public class IdentitySCIMException extends Exception {
    public IdentitySCIMException(String error) {
        super(error);
    }
    
    public IdentitySCIMException(String message, Throwable cause) {
        super(message, cause);
    }
}