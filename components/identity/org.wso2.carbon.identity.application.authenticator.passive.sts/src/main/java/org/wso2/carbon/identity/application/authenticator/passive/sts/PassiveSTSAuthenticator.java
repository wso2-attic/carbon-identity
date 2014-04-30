/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.application.authenticator.passive.sts;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authenticator.passive.sts.exception.PassiveSTSException;
import org.wso2.carbon.identity.application.authenticator.passive.sts.manager.PassiveSTSManager;
import org.wso2.carbon.identity.application.authenticator.passive.sts.util.PassiveSTSConstants;

public class PassiveSTSAuthenticator extends AbstractApplicationAuthenticator {

	private static final long serialVersionUID = -8097512332218044090L;
	
	private static Log log = LogFactory.getLog(PassiveSTSAuthenticator.class);
	
    public boolean canHandle(HttpServletRequest request) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside canHandle()");
		}

        if ((request.getParameter(PassiveSTSConstants.HTTP_PARAM_PASSIVE_STS_RESULT) != null) ||
        		(request.getParameter("authenticator") != null 
        		&& getAuthenticatorName().equalsIgnoreCase(request.getParameter("authenticator")))) {
        	return true;
        }

        return false;
    }
    
    public AuthenticatorStatus authenticate(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside authenticate()");
		}
    	
    	if (request.getParameter("authenticator") != null 
        		&& getAuthenticatorName().equalsIgnoreCase(request.getParameter("authenticator"))) {
    		sendInitialRequest(request, response, context);
    		return AuthenticatorStatus.CONTINUE;
    	}
    	
    	ExternalIdPConfig externalIdPConfig = context.getExternalIdP();

        if(request.getParameter(PassiveSTSConstants.HTTP_PARAM_PASSIVE_STS_RESULT) != null){
            try {
                new PassiveSTSManager(externalIdPConfig).processResponse(request, externalIdPConfig);
            } catch (PassiveSTSException e) {
                log.error("Exception while processing WS-Federation response", e);
                return AuthenticatorStatus.FAIL;
            }
        } else {
            log.error("wresult can not be found in request");
            return AuthenticatorStatus.FAIL;
        }
    	return AuthenticatorStatus.PASS;
    }
    
    @Override
	public void sendInitialRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside sendInitialRequest()");
		}
    	
    	ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
    	
		String idpURL = externalIdPConfig.getPassiveSTSUrl();
		String loginPage = "";

        try {
	        loginPage = new PassiveSTSManager(externalIdPConfig).buildRequest(request, idpURL, externalIdPConfig, context.getContextIdentifier());
        } catch (PassiveSTSException e) {
        	log.error("Exception while building the WS-Federation request", e);
        }
        
		try {
	        response.sendRedirect(response.encodeRedirectURL(loginPage));
        } catch (IOException e) {
        	log.error("Exception while sending to the login page", e);
        }
		return;
	}
    
	@Override
    public String getAuthenticatorName() {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getAuthenticatorName()");
		}
		
	    return PassiveSTSConstants.AUTHENTICATOR_NAME;
	}

	@Override
    public AuthenticatorStatus logout(HttpServletRequest request, HttpServletResponse response, 
    		AuthenticationContext context, AuthenticatorStateInfo stateInfo) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside logout()");
		}
	    return AuthenticatorStatus.PASS;
    }
	
	@Override
	public String getAuthenticatedSubject(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getAuthenticatedSubject()");
		}
		
		return (String)request.getSession().getAttribute("username");
	}
	
	@Override
	public Map<String, String> getResponseAttributes(HttpServletRequest arg0, AuthenticationContext context) {
		return null;
	}
	
	@Override
	public String getContextIdentifier(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getContextIdentifier()");
		}
		
		String identifier = request.getParameter("sessionDataKey");
		
		if (identifier == null) {
			identifier = request.getParameter("wctx");
			
			if (identifier != null) {
				// TODO SHOULD ensure that the value has not been tampered with by using a checksum, a pseudo-random value, or similar means.
				try {
					return URLDecoder.decode(identifier, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("Exception while URL decoding the Relay State", e);
				}
			}
		}
		
		return identifier;
	}

	@Override
	public AuthenticatorStateInfo getStateInfo(HttpServletRequest request) {
		return null;
	}
}
