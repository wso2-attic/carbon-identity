package org.wso2.carbon.identity.application.authentication.framework.exception;

public class ApplicationAuthenticatorException extends Exception {
	
	public ApplicationAuthenticatorException() {
        super();
    }

    public ApplicationAuthenticatorException(String message) {
        super(message);
    }

    public ApplicationAuthenticatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
