/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.application.authentication.framework.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.cache.*;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.handler.claims.ClaimHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.claims.impl.DefaultClaimHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.hrd.HomeRealmDiscoverer;
import org.wso2.carbon.identity.application.authentication.framework.handler.hrd.impl.DefaultHomeRealmDiscoverer;
import org.wso2.carbon.identity.application.authentication.framework.handler.provisioning.ProvisioningHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.provisioning.impl.DefaultProvisioningHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.AuthenticationRequestHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.LogoutRequestHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.impl.DefaultAuthenticationRequestHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.impl.DefaultLogoutRequestHandler;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceComponent;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationResult;
import org.wso2.carbon.ui.CarbonUIUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FrameworkUtils {

    private static Log log = LogFactory.getLog(FrameworkUtils.class);

	public static ApplicationAuthenticator getAppAuthenticatorByName(String name) {

		for (ApplicationAuthenticator authenticator : FrameworkServiceComponent.authenticators) {

			if (name.equals(authenticator.getAuthenticatorName())) {
				return authenticator;
			}
		}

		return null;
	}
	
	public static AuthenticationContext getContextData(HttpServletRequest request) {
	    
	    AuthenticationContext context = null;
	    
	    for (ApplicationAuthenticator authenticator : FrameworkServiceComponent.authenticators) {
	    	try {
	    		String contextIdentifier = authenticator.getContextIdentifier(request);
	            
	            if (contextIdentifier != null && !contextIdentifier.isEmpty()) {
	                // TODO extension point here? to get the request data from the session, cache, database or etc.
	            	//context = (AuthenticationContext)request.getSession().getAttribute(contextIdentifier);	
            		context = FrameworkUtils.getAuthenticationContextFromCache(contextIdentifier);
            		
            		if (context != null) {
            			break;
            		}
	            }
	    	} catch (UnsupportedOperationException e) {
	    		continue;
	    	}
	    }
	    
	    return context;
	}
	
	public static AuthenticationRequestHandler getAuthenticationRequestHandler() {
		
		AuthenticationRequestHandler authenticationRequestHandler = null;
		Object obj = ConfigurationFacade.getInstance().getExtensions().get(FrameworkConstants.Config.QNAME_EXT_AUTH_REQ_HANDLER);
		
		if (obj instanceof AuthenticationRequestHandler) {
			authenticationRequestHandler = (AuthenticationRequestHandler)obj;
		} else {
			authenticationRequestHandler = DefaultAuthenticationRequestHandler.getInstance();
		}
		
		return authenticationRequestHandler;
	}
	
	public static LogoutRequestHandler getLogoutRequestHandler() {
		
		LogoutRequestHandler logoutRequestHandler = null;
		Object obj = ConfigurationFacade.getInstance().getExtensions().get(FrameworkConstants.Config.QNAME_EXT_LOGOUT_REQ_HANDLER);
		
		if (obj instanceof AuthenticationRequestHandler) {
			logoutRequestHandler = (LogoutRequestHandler)obj;
		} else {
			logoutRequestHandler = DefaultLogoutRequestHandler.getInstance();
		}
		
		return logoutRequestHandler;
	}
	
	public static HomeRealmDiscoverer getHomeRealmDiscoverer() {
		
		HomeRealmDiscoverer homeRealmDiscoverer = null;
		Object obj = ConfigurationFacade.getInstance().getExtensions().get(FrameworkConstants.Config.QNAME_EXT_HRD);
		
		if (obj instanceof HomeRealmDiscoverer) {
			homeRealmDiscoverer = (HomeRealmDiscoverer)obj;
		} else {
			homeRealmDiscoverer = DefaultHomeRealmDiscoverer.getInstance();
		}
		
		return homeRealmDiscoverer;
	}
	
	public static ClaimHandler getClaimHandler() {
		
		ClaimHandler claimHandler = null;
		Object obj = ConfigurationFacade.getInstance().getExtensions().get(FrameworkConstants.Config.QNAME_EXT_CLAIM_HANDLER);
		
		if (obj instanceof ClaimHandler) {
			claimHandler = (ClaimHandler)obj;
		} else {
			claimHandler = DefaultClaimHandler.getInstance();
		}
		
		return claimHandler;
	}
	
	public static ProvisioningHandler getProvisioningHandler() {
		
		ProvisioningHandler provisioningHandler = null;
		Object obj = ConfigurationFacade.getInstance().getExtensions().get(FrameworkConstants.Config.QNAME_EXT_PROVISIONING_HANDLER);
		
		if (obj instanceof ProvisioningHandler) {
			provisioningHandler = (ProvisioningHandler)obj;
		} else {
			provisioningHandler = DefaultProvisioningHandler.getInstance();
		}
		
		return provisioningHandler;
	}
	
	public static void sendToRetryPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
		//TODO read the URL from framework config file rather than carbon.xml
		String redirectURL = CarbonUIUtil.getAdminConsoleURL(request);
        redirectURL = redirectURL.replace("commonauth/carbon/", "authenticationendpoint/retry.do");
        response.sendRedirect(redirectURL);
	}
	
	public static void removeAuthCookie(HttpServletRequest req, HttpServletResponse resp) {
		
		Cookie[] cookies = req.getCookies();

        if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(FrameworkConstants.COMMONAUTH_COOKIE)) {
					cookie.setMaxAge(0);
					resp.addCookie(cookie);
					break;
				}
			}
        }
	}
	
	public static void storeAuthCookie(HttpServletRequest req, HttpServletResponse resp, String id) {
		
        Cookie authCookie = new Cookie(FrameworkConstants.COMMONAUTH_COOKIE, id);
        resp.addCookie(authCookie);
    }
	
	public static Cookie getAuthCookie(HttpServletRequest req) {
		
        Cookie[] cookies = req.getCookies();
        
        if (cookies != null) {
        	
        	for (Cookie cookie : cookies) {
        		
                if (cookie.getName().equals(FrameworkConstants.COMMONAUTH_COOKIE)) {
                    return cookie;
                }
            }
        }
        
        return null;
    }
	
	public static void addAuthenticationContextToCache(String key, AuthenticationContext context, int cacheTimeout) {
		
		AuthenticationContextCacheKey cacheKey = new AuthenticationContextCacheKey(key);
		AuthenticationContextCacheEntry cacheEntry = new AuthenticationContextCacheEntry();
		cacheEntry.setContext(context);
		AuthenticationContextCache.getInstance(cacheTimeout).addToCache(cacheKey, cacheEntry);
	}
	
	public static void addAuthenticationResultToCache(String key, AuthenticationResult authenticationResult, int cacheTimeout) {
		
        AuthenticationResultCacheKey cacheKey = new AuthenticationResultCacheKey(key);
        AuthenticationResultCacheEntry cacheEntry = new AuthenticationResultCacheEntry();
        cacheEntry.setResult(authenticationResult);
        AuthenticationResultCache.getInstance(cacheTimeout).addToCache(cacheKey, cacheEntry);
	}
	
	public static void addSessionContextToCache(String key, SessionContext sessionContext, int cacheTimeout) {
		
		SessionContextCacheKey cacheKey = new SessionContextCacheKey(key);
		SessionContextCacheEntry cacheEntry = new SessionContextCacheEntry();
		cacheEntry.setContext(sessionContext);
		SessionContextCache.getInstance(cacheTimeout).addToCache(cacheKey, cacheEntry);
	}
	
	public static SessionContext getSessionContextFromCache(String key) {
		
		SessionContext sessionContext = null;
		SessionContextCacheKey cacheKey = new SessionContextCacheKey(key);
		Object cacheEntryObj = SessionContextCache.getInstance(0).getValueFromCache(cacheKey);
		
		if (cacheEntryObj != null) {
			sessionContext = ((SessionContextCacheEntry)cacheEntryObj).getContext();
    	}
		
		return sessionContext;
    }
	
	public static void removeSessionContextFromCache(String key) {
		
		SessionContextCacheKey cacheKey = new SessionContextCacheKey(key);
		SessionContextCache.getInstance(0).clearCacheEntry(cacheKey);
	}
	
	public static AuthenticationContext getAuthenticationContextFromCache(String key) {
		
		AuthenticationContext authnContext = null;
		AuthenticationContextCacheKey cacheKey = new AuthenticationContextCacheKey(key);
    	Object cacheEntryObj = AuthenticationContextCache.getInstance(0).getValueFromCache(cacheKey);
    	
    	if (cacheEntryObj != null) {
    		authnContext = ((AuthenticationContextCacheEntry)cacheEntryObj).getContext();
    	}
		
		return authnContext;
    }

    public static void setRequestPathCredentials(HttpServletRequest req) {
        // reading the authorization header for request path authentication
        String reqPathCred = req.getHeader("Authorization");
        if (reqPathCred == null) {
            reqPathCred = req.getParameter("ReqPathCredential");
        }
        if (reqPathCred != null) {
            log.debug("A Request path credential found");
            req.getSession().setAttribute("Authorization", reqPathCred);
        }
    }
}
