package org.wso2.carbon.identity.application.authenticator.facebook.v2;

public class AuthenticatorException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AuthenticatorException(String message){
        super(message);
    }

    public AuthenticatorException(Throwable e){
        super(e);
    }

    public AuthenticatorException(String message, Throwable e){
        super(message, e);
    }

}
