package org.wso2.carbon.identity.mgt;

public class IdentityMgtConfigException extends Exception {

    private static final long serialVersionUID = 7808577688733705284L;

    public IdentityMgtConfigException() {
        super();
    }

    public IdentityMgtConfigException(String message, Throwable t) {
        super(message, t);
    }

    public IdentityMgtConfigException(String message) {
        super(message);
    }

    public IdentityMgtConfigException(Throwable cause) {
        super(cause);
    }
}
