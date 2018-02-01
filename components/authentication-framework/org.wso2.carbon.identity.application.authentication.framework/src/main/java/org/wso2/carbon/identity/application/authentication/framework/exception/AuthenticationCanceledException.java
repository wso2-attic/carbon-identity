package org.wso2.carbon.identity.application.authentication.framework.exception;

import org.wso2.carbon.identity.base.IdentityException;

import java.rmi.server.UID;

/**
 * Created by nl26303 on 12/7/16.
 */
public class AuthenticationCanceledException extends IdentityException {

    private static final long serialVersionUID = 7383842128705809075L;

    public AuthenticationCanceledException(String message) {
        super(message);
    }

    public AuthenticationCanceledException(String message, Throwable cause) {
        super(message, cause);
    }
}
