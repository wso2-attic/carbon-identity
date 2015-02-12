/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.identity.application.authenticator.fido;

import com.yubico.u2f.data.messages.AuthenticateRequestData;
import com.yubico.u2f.data.messages.AuthenticateResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.LocalApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authenticator.fido.dto.FIDOUser;
import org.wso2.carbon.identity.application.authenticator.fido.u2f.U2FService;
import org.wso2.carbon.identity.application.authenticator.fido.util.FIDOAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.fido.util.Util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * FIDO U2F Specification based authenticator.
 */
public class FIDOAuthenticator extends AbstractApplicationAuthenticator implements LocalApplicationAuthenticator {
	private static Log log = LogFactory.getLog(FIDOAuthenticator.class);
	private static FIDOAuthenticator instance = new FIDOAuthenticator();
	@Override
	public AuthenticatorFlowStatus process(HttpServletRequest request, HttpServletResponse response,
	                                       AuthenticationContext context)
			throws AuthenticationFailedException, LogoutFailedException {
		return super.process(request, response, context);
	}

	@Override
	protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
			AuthenticationContext context) throws AuthenticationFailedException {

		String tokenResponse = request.getParameter("tokenResponse");
		String appID = Util.getOrigin(request);
		//		String appID = request.getServerName();
		String username = getUsername(context);
		//		String username = request.getParameter("username");

		U2FService u2FService = U2FService.getInstance();
		//TODO
		FIDOUser fidoUser = new FIDOUser(username, "", "", AuthenticateResponse.fromJson(tokenResponse));
		fidoUser.setAppID(appID);
		u2FService.finishAuthentication(fidoUser);

	}

	@Override
	public boolean canHandle(javax.servlet.http.HttpServletRequest httpServletRequest) {
		String tokenResponse = httpServletRequest.getParameter("tokenResponse");
		return null != tokenResponse;

	}

	@Override
	public String getContextIdentifier(
			javax.servlet.http.HttpServletRequest httpServletRequest) {
		return httpServletRequest.getParameter("sessionDataKey");
	}

	@Override
	public String getName() {
		return FIDOAuthenticatorConstants.AUTHENTICATOR_NAME;
	}

	@Override public String getFriendlyName() {
		return FIDOAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
	}

	@Override
	protected void initiateAuthenticationRequest(HttpServletRequest request,
	                                             HttpServletResponse response,
	                                             AuthenticationContext context)
			throws AuthenticationFailedException {
		//FIDO BE service component
		U2FService u2FService = U2FService.getInstance();
		try {
			//authentication page's URL.
			String loginPage = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();
			//username from basic authenticator.
			String user = getUsername(context);
			//origin as appID eg.: http://example.com:8080
			String appID = Util.getOrigin(request);
			//calls BE service method to generate challenge.
			//TODO
			FIDOUser fidoUser = new FIDOUser(user, "", "", appID);
			AuthenticateRequestData data = u2FService.startAuthentication(fidoUser);
			//redirect to FIDO login page
			response.sendRedirect(response.encodeRedirectURL(loginPage + ("?"))
			                      + "&authenticators=" + getName() + "&type=fido&sessionDataKey=" +
			                      request.getParameter("sessionDataKey") +
			                      "&data=" + data.toJson());

		} catch (IOException e) {
			throw new AuthenticationFailedException(
					"Could not initiate FIDO authentication request", e);
		}
	}

	@Override
	protected boolean retryAuthenticationEnabled() {
		//retry disabled
		return false;
	}

	private String getUsername(AuthenticationContext context) {
		//username from authentication context.
		String username = "";
		for (int i = 1; i <= context.getSequenceConfig().getStepMap().size(); i++) {
			System.out.println("i:" + i + " size:" + context.getSequenceConfig().getStepMap().size());
			if (context.getSequenceConfig().getStepMap().get(i).getAuthenticatedUser() != null &&
			    context.getSequenceConfig().getStepMap().get(i).getAuthenticatedAutenticator()
			           .getApplicationAuthenticator() instanceof LocalApplicationAuthenticator) {
				username = context.getSequenceConfig().getStepMap().get(i).getAuthenticatedUser();
				if (log.isDebugEnabled()) {
					log.debug("username :" + username);
				}
				break;
			}
		}
		return username;
	}


	/**
	 * Gets a FIDOAuthenticator instance.
	 *
	 * @return a FIDOAuthenticator.
	 */
	public static FIDOAuthenticator getInstance() {
		return instance;
	}

}

