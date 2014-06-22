package org.wso2.carbon.identity.application.authentication.framework.exception;

public class LogoutFailedException extends Exception {

	public LogoutFailedException() {
        super();
    }

    public LogoutFailedException(String message) {
        super(message);
    }

    public LogoutFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
