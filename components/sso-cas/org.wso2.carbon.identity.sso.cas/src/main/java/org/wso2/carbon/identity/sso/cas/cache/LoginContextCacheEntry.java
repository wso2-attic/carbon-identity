package org.wso2.carbon.identity.sso.cas.cache;

import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.sso.cas.ticket.LoginContext;

public class LoginContextCacheEntry extends CacheEntry {

	private static final long serialVersionUID = 8433390024465099261L;
	
	private LoginContext loginContext;

    public LoginContext getLoginContext() {
        return loginContext;
    }

    public void setLoginContext(LoginContext loginContext) {
        this.loginContext = loginContext;
    }
	
}
