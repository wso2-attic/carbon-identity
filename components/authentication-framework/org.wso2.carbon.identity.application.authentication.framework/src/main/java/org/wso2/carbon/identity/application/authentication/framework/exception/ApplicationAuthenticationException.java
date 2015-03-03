package org.wso2.carbon.identity.application.authentication.framework.exception;

public class ApplicationAuthenticationException extends Exception {

    public ApplicationAuthenticationException() {
        super();
    }

    public ApplicationAuthenticationException(String message) {
        super(message);
    }

    public ApplicationAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}