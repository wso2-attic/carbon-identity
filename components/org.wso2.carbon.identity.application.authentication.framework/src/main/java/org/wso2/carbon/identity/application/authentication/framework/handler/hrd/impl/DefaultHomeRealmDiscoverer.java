package org.wso2.carbon.identity.application.authentication.framework.handler.hrd.impl;

import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.hrd.HomeRealmDiscoverer;

public class DefaultHomeRealmDiscoverer implements HomeRealmDiscoverer {
	
	private static volatile DefaultHomeRealmDiscoverer instance;
	
	public static DefaultHomeRealmDiscoverer getInstance() {
		if (instance == null) {
			synchronized (DefaultHomeRealmDiscoverer.class) {
				if (instance == null) {
					instance = new DefaultHomeRealmDiscoverer();
				}
			}
		}
		return instance;
	}
	
	public String discover(String value) throws FrameworkException {
		return value;
	}
}
