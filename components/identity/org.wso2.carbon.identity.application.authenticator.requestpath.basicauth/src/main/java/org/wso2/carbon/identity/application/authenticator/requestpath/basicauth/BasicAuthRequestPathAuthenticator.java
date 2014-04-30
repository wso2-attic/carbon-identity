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
package org.wso2.carbon.identity.application.authenticator.requestpath.basicauth;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.RequestPathApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authenticator.requestpath.basicauth.internal.BasicAuthRequestPathAuthenticatorServiceComponent;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class BasicAuthRequestPathAuthenticator extends AbstractApplicationAuthenticator implements RequestPathApplicationAuthenticator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	private static final String BASIC_AUTH_SCHEMA = "Basic";
	private static final String AUTHENTICATOR_NAME = "BasicAuthRequestPathAuthenticator";
	private static Log log = LogFactory.getLog(BasicAuthRequestPathAuthenticator.class);

	@Override
	public boolean canHandle(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
    		log.trace("Inside canHandle()");
    	}
		
		String headerValue = (String) request.getSession().getAttribute(AUTHORIZATION_HEADER_NAME);
		
		if (headerValue != null && !"".equals(headerValue.trim())) {
			String[] headerPart = headerValue.trim().split(" ");
			if (BASIC_AUTH_SCHEMA.equals(headerPart[0])) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public AuthenticatorStatus authenticate(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context) {
		
		if (log.isTraceEnabled()) {
    		log.trace("Inside authenticate()");
    	}
		
		String headerValue =(String) request.getSession().getAttribute(AUTHORIZATION_HEADER_NAME);
		String credential = headerValue.trim().split(" ")[1];
		
		try {
			String[] cred = new String(Base64.decode(credential)).split(":");
			int tenantId = IdentityUtil.getTenantIdOFUser(cred[0]);
			UserStoreManager userStoreManager = (UserStoreManager) BasicAuthRequestPathAuthenticatorServiceComponent.getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
			boolean isAuthenticated = userStoreManager.authenticate(MultitenantUtils.getTenantAwareUsername(cred[0]), cred[1]);
			
			if(!isAuthenticated) {
				log.error("Authentication failed for user " + cred[0]);
				return AuthenticatorStatus.FAIL;
			}
			if(log.isDebugEnabled()) {
				log.debug("Authenticated user " + cred[0]);
			}
			
			request.getSession().setAttribute("username", cred[0]);
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return AuthenticatorStatus.FAIL;
		}
		
		return AuthenticatorStatus.PASS;
	}

	@Override
	public AuthenticatorStatus logout(HttpServletRequest request, HttpServletResponse response,
			AuthenticationContext context, AuthenticatorStateInfo stateInfo) {
		if (log.isTraceEnabled()) {
    		log.trace("Inside logout()");
    	}
    	
    	// We cannot invalidate the session in case session is used by the calling servlet
        return AuthenticatorStatus.PASS;
	}

	@Override
	public void sendInitialRequest(HttpServletRequest request, HttpServletResponse response,
			AuthenticationContext context) {
		
		if (log.isTraceEnabled()) {
    		log.trace("Inside sendInitialRequest()");
    	}

	}


	@Override
	public Map<String, String> getResponseAttributes(HttpServletRequest request,
			AuthenticationContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AuthenticatorStateInfo getStateInfo(HttpServletRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContextIdentifier(HttpServletRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAuthenticatorName() {
		
		if (log.isTraceEnabled()) {
    		log.trace("Inside getAuthenticatorName()");
    	}
    	
		return AUTHENTICATOR_NAME;
	}

	@Override
	public String getAuthenticatedSubject(HttpServletRequest request) {
		if (log.isTraceEnabled()) {
    		log.trace("Inside getAuthenticatedSubject()");
    	}
		
		return (String)request.getSession().getAttribute("username");
	}

}
