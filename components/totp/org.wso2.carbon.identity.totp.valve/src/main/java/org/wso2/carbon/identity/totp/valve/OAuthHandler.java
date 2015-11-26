/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.totp.valve;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.catalina.connector.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth2.OAuth2TokenValidationService;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;

import java.rmi.RemoteException;
import java.util.Map;

/**
 * OAuthHandler class.
 */
public class OAuthHandler implements TOTPAuthenticationHandler {
	private static Log log = LogFactory.getLog(OAuthHandler.class);
	private String remoteServiceURL;
	private String userName;
	private String password;
	private int priority;

	private final int DEFAULT_PRIORITY = 10;

	private Map<String, String> properties;


	/**
	 * Set default AuthzServer.
	 */
	public void setDefaultAuthzServer() {
		this.remoteServiceURL = Constants.LOCAL_AUTH_SERVER;
	}

	/**
	 * Set properties.
	 *
	 * @param authenticatorProperties
	 */
	@Override
	public void setProperties(Map<String, String> authenticatorProperties) {
		this.properties = authenticatorProperties;
		String priorityString = properties.get(Constants.PROPERTY_NAME_PRIORITY);
		if (priorityString != null) {
			priority = Integer.parseInt(priorityString);
		} else {
			priority = DEFAULT_PRIORITY;
		}
		String remoteURLString = properties.get(Constants.PROPERTY_NAME_AUTH_SERVER);
		if (remoteURLString != null) {
			remoteServiceURL = remoteURLString;
		} else {
			remoteServiceURL = Constants.LOCAL_AUTH_SERVER;
		}
		userName = properties.get(Constants.PROPERTY_NAME_USERNAME);
		password = properties.get(Constants.PROPERTY_NAME_PASSWORD);
	}

	/**
	 * get the OAuthzServerURL.
	 *
	 * @return
	 */
	private String getOAuthAuthzServerURL() {
		if (remoteServiceURL != null) {
			if (!remoteServiceURL.endsWith("/")) {
				remoteServiceURL += "/";
			}
		}
		return remoteServiceURL;
	}

	/**
	 * extended can handle method.
	 *
	 * @param request
	 * @return
	 */
	@Override
	public boolean canHandler(Request request) {

		String authheader = request.getHeader(Constants.AUTHORIZATION_HEADER);
		if (authheader != null && authheader.startsWith(Constants.BEARER_AUTH_HEADER)) {
			return true;
		}
		return false;
	}

	/**
	 * Check whether a given credentials in the request is authenticated or not.
	 *
	 * @param request request from the client
	 * @return true if authenticated, false otherwise
	 */
	@Override
	public boolean isAuthenticated(Request request) {

		String authheader = request.getHeader(Constants.AUTHORIZATION_HEADER);
		if (authheader != null && authheader.startsWith(Constants.BEARER_AUTH_HEADER)) {

			String accessToken = authheader.substring(Constants.BEARER_AUTH_HEADER.length()).trim();

			try {
				OAuth2ClientApplicationDTO validationApp = validateAccessToken(accessToken);
				OAuth2TokenValidationResponseDTO validationResponse = null;

				if (validationApp != null) {
					validationResponse = validationApp.getAccessTokenValidationResponse();
				}

				if (validationResponse != null) {
					if (validationResponse.isValid()) {
						String userName = validationResponse.getAuthorizedUser();
						return true;
					}
				}
			} catch (RemoteException e) {
                log.error("Error when calling the remote service", e);
            }
        }

		return false;
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Validate the access token sent.
	 *
	 * @param accessTokenIdentifier
	 * @return
	 * @throws Exception
	 */
	private OAuth2ClientApplicationDTO validateAccessToken(String accessTokenIdentifier)
            throws RemoteException {

		// if it is specified to use local authz server (i.e: local://services)
		if (remoteServiceURL.startsWith(Constants.LOCAL_PREFIX)) {
			OAuth2TokenValidationRequestDTO oauthValidationRequest = new OAuth2TokenValidationRequestDTO();
			OAuth2TokenValidationRequestDTO.OAuth2AccessToken accessToken = oauthValidationRequest.new 
					OAuth2AccessToken();
			accessToken.setTokenType(OAuthServiceClient.BEARER_TOKEN_TYPE);
			accessToken.setIdentifier(accessTokenIdentifier);
			oauthValidationRequest.setAccessToken(accessToken);

			OAuth2TokenValidationService oauthValidationService = new OAuth2TokenValidationService();
			OAuth2ClientApplicationDTO oauthValidationResponse = oauthValidationService
					.findOAuthConsumerIfTokenIsValid(oauthValidationRequest);

			return oauthValidationResponse;
		}

		// else do a web service call to the remote authz server
		try {
			ConfigurationContext configContext = ConfigurationContextFactory
					.createConfigurationContextFromFileSystem(null, null);
			OAuthServiceClient oauthClient = new OAuthServiceClient(getOAuthAuthzServerURL(),
			                                                        userName, password, configContext);
			org.wso2.carbon.identity.oauth2.stub.dto.OAuth2ClientApplicationDTO validationResponse;
			validationResponse = oauthClient.findOAuthConsumerIfTokenIsValid(accessTokenIdentifier);

			OAuth2ClientApplicationDTO appDTO = new OAuth2ClientApplicationDTO();
			appDTO.setConsumerKey(validationResponse.getConsumerKey());

			OAuth2TokenValidationResponseDTO validationDto = new OAuth2TokenValidationResponseDTO();
			validationDto.setAuthorizedUser(validationResponse.getAccessTokenValidationResponse().getAuthorizedUser());
			validationDto.setValid(validationResponse.getAccessTokenValidationResponse().getValid());
			appDTO.setAccessTokenValidationResponse(validationDto);
			return appDTO;
		} catch (AxisFault axisFault) {
			throw axisFault;
		} catch (RemoteException e) {
            throw e;
        }
    }

	public void setDefaultPriority() {
		priority = DEFAULT_PRIORITY;
	}
}
