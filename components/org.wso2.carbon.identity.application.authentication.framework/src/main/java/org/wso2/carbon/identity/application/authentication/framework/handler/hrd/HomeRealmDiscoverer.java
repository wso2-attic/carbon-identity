package org.wso2.carbon.identity.application.authentication.framework.handler.hrd;

import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;

public interface HomeRealmDiscoverer {
	
	public String discover(String value) throws FrameworkException;
}
