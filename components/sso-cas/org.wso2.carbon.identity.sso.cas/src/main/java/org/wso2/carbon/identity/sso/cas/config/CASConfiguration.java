package org.wso2.carbon.identity.sso.cas.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;

public class CASConfiguration {
	private static final String CAS_CONTEXT_PATH = "CAS.ContextPath";
	private static final String CAS_CACHE_TIMEOUT = "CAS.CacheTimeout";
	private static final String CAS_PROXY_RETRY_LIMIT = "CAS.ProxyRetryLimit";
	private static final String CAS_PROXY_RETRY_INTERVAL = "CAS.ProxyRetryInterval";
	private static final String CAS_LOGOUT_RETRY_LIMIT = "CAS.LogoutRetryLimit";
	private static final String CAS_LOGOUT_RETRY_INTERVAL = "CAS.LogoutRetryInterval";
	
    private static Log log = LogFactory.getLog(CASConfiguration.class);
    private static String basePath = "/cas"; // Default context path
    private static int cacheTimeout = 157680; // Default matches SAML SSO default timeout
    private static int proxyRetryInterval = 30; // 30 second default
	private static int proxyRetryLimit = 3; // 3 retry default
	private static int logoutRetryInterval = 30; // 30 second default
	private static int logoutRetryLimit = 3; // 3 retry default
	private static long validityPeriod = 300000; // 5 minute default (300,000 ms)
    
    static {
		try {
			String casBasePath = IdentityUtil.getProperty(CASConfiguration.CAS_CONTEXT_PATH);

			if( casBasePath == null || casBasePath.trim().length() == 0) {
				throw new Exception();
			} else {
				basePath = casBasePath;
			}
		} catch(Exception ex) {
			log.info("CAS base path not found. Using default value.");
		}
    	
		try {
			String casCacheTimeout = IdentityUtil.getProperty(CASConfiguration.CAS_CACHE_TIMEOUT);
			
			cacheTimeout = Integer.parseInt(casCacheTimeout);			
		} catch(Exception ex) {
			log.info("CAS retry count not found. Using default value.");
		}
		
		try {
			String casRetryInterval = IdentityUtil.getProperty(CASConfiguration.CAS_PROXY_RETRY_INTERVAL);
			
			proxyRetryInterval = Integer.parseInt(casRetryInterval);			
		} catch(Exception ex) {
			log.info("CAS proxy retry interval not found. Using default value.");
		}
		
		try {
			String casRetryLimit = IdentityUtil.getProperty(CASConfiguration.CAS_PROXY_RETRY_LIMIT);
			
			proxyRetryLimit = Integer.parseInt(casRetryLimit);			
		} catch(Exception ex) {
			log.info("CAS proxy retry limit not found. Using default value.");
		}
		
		try {
			String casRetryInterval = IdentityUtil.getProperty(CASConfiguration.CAS_LOGOUT_RETRY_INTERVAL);
			
			logoutRetryInterval = Integer.parseInt(casRetryInterval);			
		} catch(Exception ex) {
			log.info("CAS logout retry interval not found. Using default value.");
		}
		
		try {
			String casRetryLimit = IdentityUtil.getProperty(CASConfiguration.CAS_LOGOUT_RETRY_LIMIT);
			
			logoutRetryLimit = Integer.parseInt(casRetryLimit);			
		} catch(Exception ex) {
			log.info("CAS logout retry limit not found. Using default value.");
		}
		
		try {
	        validityPeriod = Integer.parseInt(IdentityUtil.getProperty(
	                    IdentityConstants.ServerConfig.SAML_RESPONSE_VALIDITY_PERIOD).trim()) * 60000;
		} catch (Exception ex) {
			log.info("CAS SAML validity period not found. Using default value.");
		}
    }
    
    public static String buildTenantRelativePath(String tenantDomain, String path) {
    	if( tenantDomain != null && tenantDomain.trim().length() > 0 ) {
    		return "/" + tenantDomain + buildRelativePath(path);	
    	} else {
    		return buildRelativePath(path);
    	}
    	
    }
    
	public static String buildRelativePath(String path) {
		if( path == null ) {
			throw new NullPointerException("Relative path cannot be null");
		}
		
		String casContextPath = getBasePath();
		
		if( path != null && !path.startsWith("/") ) {
			casContextPath += "/";
		}
		
		casContextPath += path;
		
		return casContextPath;
	}
	
	public static String getTenantBasePath(String tenantDomain) {
    	if( tenantDomain != null && tenantDomain.trim().length() > 0 ) {
    		return "/" + tenantDomain + getBasePath();	
    	} else {
    		return getBasePath();
    	}
	}
	
	public static String getBasePath() {
		return basePath;
	}
	
	public static int getCacheTimeout() {		
		return cacheTimeout * 1000;
	}
	
	public static int getProxyRetryInterval() {		
		return proxyRetryInterval * 1000;
	}
	
	public static int getProxyRetryLimit() {		
		return proxyRetryLimit;
	}
	public static int getLogoutRetryInterval() {
		return logoutRetryInterval * 1000;
	}
	
	public static int getLogoutRetryLimit() {
		return logoutRetryLimit;
	}
	
	public static long getSAMLResponseValidityPeriod() {
		return validityPeriod;
	}
}