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
package org.wso2.carbon.identity.application.authenticator.requestpath.oauth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.RequestPathApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.oauth2.OAuth2TokenValidationService;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO.OAuth2AccessToken;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Map;

public class OAuthRequestPathAuthenticator extends AbstractApplicationAuthenticator implements RequestPathApplicationAuthenticator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	private static final String BEARER_SCHEMA = "Bearer";
	private static final String AUTHENTICATOR_NAME = "OAuthRequestPathAuthenticator";
	private static Log log = LogFactory.getLog(OAuthRequestPathAuthenticator.class);

	@Override
	public boolean canHandle(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
    		log.trace("Inside canHandle()");
    	}
		
		Enumeration<String> authHeader = request.getHeaders(AUTHORIZATION_HEADER_NAME);
		if(authHeader != null && authHeader.hasMoreElements()) {
			String headerValue = authHeader.nextElement();
			if(headerValue != null && "".equals(headerValue.trim())) {
				String[] headerPart = headerValue.trim().split(" ");
				if(BEARER_SCHEMA.equals(headerPart[0])) {
					return true;
				}
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
		
		Enumeration<String> authHeader = request.getHeaders(AUTHORIZATION_HEADER_NAME);
		String headerValue = authHeader.nextElement().trim();
		String token = headerValue.trim().split(" ")[1];
		
		try {
			
			OAuth2TokenValidationService validationService = new OAuth2TokenValidationService();
			OAuth2TokenValidationRequestDTO validationReqDTO = new OAuth2TokenValidationRequestDTO();
			OAuth2AccessToken accessToken = validationReqDTO.new OAuth2AccessToken();
			accessToken.setIdentifier(token);
			accessToken.setTokenType("bearer");
			validationReqDTO.setAccessToken(accessToken);
			OAuth2TokenValidationResponseDTO validationResponse = validationService.validate(validationReqDTO);
			
			if(validationResponse.isValid()) {
				log.error("RequestPath OAuth authentication failed");
				return AuthenticatorStatus.CONTINUE;
			}
			
			String user = validationResponse.getAuthorizedUser();
			context.setSubject(user);
			
			if(log.isDebugEnabled()) {
				log.debug("Authenticated user " + user);
			}
			
			request.getSession().setAttribute("username", user);
			
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
	public String getAuthenticatedSubject(HttpServletRequest request) {
		if (log.isTraceEnabled()) {
    		log.trace("Inside getAuthenticatedSubject()");
    	}
		
		return (String)request.getSession().getAttribute("username");
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
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getClaimDialectURIIfStandard() {
		// TODO Auto-generated method stub
		return null;
	}

}
