package org.wso2.carbon.identity.application.authentication.framework.exception;

public class AuthenticationFailedException extends Exception {

    private static final long serialVersionUID = -7390290583990926490L;

    public AuthenticationFailedException() {
        super();
    }

    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
