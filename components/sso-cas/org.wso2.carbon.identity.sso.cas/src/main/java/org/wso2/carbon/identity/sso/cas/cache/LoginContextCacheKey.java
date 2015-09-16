package org.wso2.carbon.identity.sso.cas.cache;

import org.wso2.carbon.identity.application.common.cache.CacheKey;

public class LoginContextCacheKey extends CacheKey {

	private static final long serialVersionUID = -5560706455102133373L;
	
	private String sessionDataKey;

    public LoginContextCacheKey(String sessionDataKey) {
        this.sessionDataKey = sessionDataKey;
    }

    public String getSessionDataKey() {
        return sessionDataKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sessionDataKey == null) ? 0 : sessionDataKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LoginContextCacheKey other = (LoginContextCacheKey) obj;
        if (sessionDataKey == null) {
            if (other.sessionDataKey != null)
                return false;
        } else if (!sessionDataKey.equals(other.sessionDataKey))
            return false;
        return true;
    }
}
