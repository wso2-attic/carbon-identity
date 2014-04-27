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
package org.wso2.carbon.identity.application.authenticator.oidc;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.amber.oauth2.client.OAuthClient;
import org.apache.amber.oauth2.client.URLConnectionClient;
import org.apache.amber.oauth2.client.request.OAuthClientRequest;
import org.apache.amber.oauth2.client.response.OAuthAuthzResponse;
import org.apache.amber.oauth2.client.response.OAuthClientResponse;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.ui.CarbonUIUtil;

public class OpenIDConnectAuthenticator extends AbstractApplicationAuthenticator implements FederatedApplicationAuthenticator {
	
	private static final long serialVersionUID = -4154255583070524018L;
	private static final String IDTOKEN_HANDLER = "IDTokenHandler";
	private static final String CLAIMS_RETRIEVER = "ClaimsRetriever";
	
	private static Log log = LogFactory.getLog(OpenIDConnectAuthenticator.class);
	private String authenticatedUser;
	
    public boolean canHandle(HttpServletRequest request) {   
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.canHandle()");
		}
    	
    	// From login page asking for OIDC login
        if (request.getParameter("authenticator") != null &&
                getAuthenticatorName().equalsIgnoreCase(request.getParameter("authenticator"))) {
            return true;
        }
    	
    	// Check commonauth got an OIDC response
    	if (request.getParameter(OIDCAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE) != null 
    			&& request.getParameter(OIDCAuthenticatorConstants.OAUTH2_PARAM_STATE) != null) {
        	return true;
        }
		// TODO : What if IdP failed?

        return false;
    }

	@Override
	public AuthenticatorStatus authenticate(HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.authenticate()");
		}

    	// From login page asking for OIDC login
    	if (request.getParameter("authenticator") != null &&
                getAuthenticatorName().equalsIgnoreCase(request.getParameter("authenticator"))) {
            sendInitialRequest(request, response, context);
    		return AuthenticatorStatus.CONTINUE;
    	}
    	
    	try {
        	ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
    		String clientId = externalIdPConfig.getClientId();
    		String clientSecret = externalIdPConfig.getClientSecret();
    		String tokenEndPoint = externalIdPConfig.getTokenEndpointUrl();
			
			String callbackurl = CarbonUIUtil.getAdminConsoleURL(request);
			callbackurl = callbackurl.replace("commonauth/carbon/", "commonauth");

    		OAuthAuthzResponse authzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
    		String code = authzResponse.getCode();
			
			OAuthClientRequest accessRequest;
			try {
				accessRequest = OAuthClientRequest
						.tokenLocation(tokenEndPoint)
						.setGrantType(GrantType.AUTHORIZATION_CODE)
						.setClientId(clientId).setClientSecret(clientSecret)
						.setRedirectURI(callbackurl)
						.setCode(code)
						.buildBodyMessage();
				
			} catch (OAuthSystemException e) {
	        	if (log.isDebugEnabled()) {
	    			log.debug("Exception while building request for request access token", e);
	    		}
		        return AuthenticatorStatus.FAIL;
			}

			// create OAuth client that uses custom http client under the hood
			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
			OAuthClientResponse oAuthResponse;
			try {
				oAuthResponse = oAuthClient.accessToken(accessRequest);
			} catch (OAuthSystemException e) {
	        	if (log.isDebugEnabled()) {
	    			log.debug("Exception while requesting access token", e);
	    		}
		        return AuthenticatorStatus.FAIL;
			}

			// TODO : return access token and id token to framework
			String accessToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN);
			String idToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ID_TOKEN);

			if (accessToken != null && idToken != null) {
				
				context.setProperty(OIDCAuthenticatorConstants.ACCESS_TOKEN, accessToken);
				context.setProperty(OIDCAuthenticatorConstants.ID_TOKEN, idToken);
				
				// validates the IDToken
				if(!getIDTokenHanlder().handle(idToken, context)) {
					 return AuthenticatorStatus.FAIL;
				}
				return AuthenticatorStatus.PASS;
			}
			
        } catch (OAuthProblemException e) {
        	if (log.isDebugEnabled()) {
    			log.debug("Exception while processing OpenID Connect response", e);
    		}
	    } 
        return AuthenticatorStatus.FAIL;
	}

	@Override
	public AuthenticatorStatus logout(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context,
			AuthenticatorStateInfo stateInfo) {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.logout()");
		}
		// TODO : Add logic
		return AuthenticatorStatus.PASS;
	}

	@Override
	public void sendInitialRequest(HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.sendInitialRequest()");
		}
        
		try {
        	ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
    		String clientId = externalIdPConfig.getClientId();
    		String authorizationEP = externalIdPConfig.getAuthzEndpointUrl();
			
//			String callbackurl = "https://localhost:9444/commonauth";
			String callbackurl = CarbonUIUtil.getAdminConsoleURL(request);
			callbackurl = callbackurl.replace("commonauth/carbon/", "commonauth");
			
			String state = context.getContextIdentifier();

			OAuthClientRequest authzRequest = OAuthClientRequest
		            .authorizationLocation(authorizationEP)
		            .setClientId(clientId)
		            .setRedirectURI(callbackurl)
		            .setResponseType(OIDCAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE)
		            .setScope(OIDCAuthenticatorConstants.OAUTH_OIDC_SCOPE)
		            .setState(state)
		            .buildQueryMessage();
			
		    response.sendRedirect(authzRequest.getLocationUri());
		    
        } catch (IOException e) {
        	log.error("Exception while sending to the login page", e);
        } catch (OAuthSystemException e) {
        	log.error("Exception while building authorization code request", e);
		}
		return;
	}

	@Override
	public String getAuthenticatedSubject(HttpServletRequest request) {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.getAuthenticatedSubject()");
		}
    	
		return authenticatedUser;
	}

	@Override
	public String getContextIdentifier(HttpServletRequest request) {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.getContextIdentifier()");
		}
    	
		return request.getParameter(OIDCAuthenticatorConstants.OAUTH2_PARAM_STATE);
	}
    
	@Override
    public String getAuthenticatorName() {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.getAuthenticatorName()");
		}
    	
	    return OIDCAuthenticatorConstants.AUTHENTICATOR_NAME;
	}

	@Override
	public Map<String, String> getResponseAttributes(HttpServletRequest request, AuthenticationContext context) {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.getResponseAttributes()");
		}
    	
    	String accessToken = (String) context.getProperty(OIDCAuthenticatorConstants.ACCESS_TOKEN);
    	ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
    	
    	ClaimsRetriever retriever = getClaimsRetriever();
    	Map<String, String> claims = retriever.retrieveClaims(accessToken, externalIdPConfig, context);
    	context.setSubjectAttributes(claims);
		return claims;
	}

	@Override
	public AuthenticatorStateInfo getStateInfo(HttpServletRequest request) {
		return null;
	}
	
	private IDTokenHandler getIDTokenHanlder() {
		
		IDTokenHandler handler = null;
		String handlerClassName = getAuthenticatorConfig().getParameterMap().get(IDTOKEN_HANDLER);
		if (handlerClassName != null) {
			try {
				// Bundle class loader will cache the loaded class and returned
				// the already loaded instance, hence calling this method
				// multiple times doesn't cost.
				Class clazz = Thread.currentThread().getContextClassLoader()
						.loadClass(handlerClassName);
				handler = (IDTokenHandler) clazz.newInstance();

			} catch (ClassNotFoundException e) {
				log.error("Error while instantiating the OpenIDManager ", e);
			} catch (InstantiationException e) {
				log.error("Error while instantiating the OpenIDManager ", e);
			} catch (IllegalAccessException e) {
				log.error("Error while instantiating the OpenIDManager ", e);
			}
		} else {
			handler = new DefaultIDTokenHandler();
		}
		
		return handler;
	}
	
	private ClaimsRetriever getClaimsRetriever() {
		
		ClaimsRetriever retriever = null;
		String retrieverClassName = getAuthenticatorConfig().getParameterMap().get(CLAIMS_RETRIEVER);
		if (retrieverClassName != null) {
			try {
				// Bundle class loader will cache the loaded class and returned
				// the already loaded instance, hence calling this method
				// multiple times doesn't cost.
				Class clazz = Thread.currentThread().getContextClassLoader()
						.loadClass(retrieverClassName);
				retriever = (ClaimsRetriever) clazz.newInstance();

			} catch (ClassNotFoundException e) {
				log.error("Error while instantiating the OpenIDManager ", e);
			} catch (InstantiationException e) {
				log.error("Error while instantiating the OpenIDManager ", e);
			} catch (IllegalAccessException e) {
				log.error("Error while instantiating the OpenIDManager ", e);
			}
		} else {
			retriever = new OIDCUserInfoClaimsRetriever();
		}
		
		return retriever;
	}
}
