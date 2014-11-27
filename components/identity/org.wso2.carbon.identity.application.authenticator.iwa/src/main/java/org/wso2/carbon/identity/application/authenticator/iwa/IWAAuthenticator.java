/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authenticator.iwa;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.LocalApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.iwa.servlet.IWAServelet;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * Username Password based Authenticator
 *
 */
public class IWAAuthenticator extends AbstractApplicationAuthenticator implements
        LocalApplicationAuthenticator {
	
	private static final long serialVersionUID = -713445365200141399L;
	
	private static Log log = LogFactory.getLog(IWAAuthenticator.class);

    public static final String IWA_PARAM_STATE = "state";

    //the following param of the request will be set once the request is processed by the IWAServlet
    public static final String IWA_PROCESSED = "iwaauth";

    /*
     * (non-Javadoc)
     *
     * @see org.wso2.carbon.identity.application.authentication.framework.
     * ApplicationAuthenticator#canHandle(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public boolean canHandle(HttpServletRequest request) {

    	if (log.isTraceEnabled()) {
    		log.trace("Inside canHandle()");
    	}
    	

        //check whether the OS is windows. IWA works only with windows
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win") && request.getParameter(IWA_PROCESSED)!=null;
    }
    
    @Override
	protected void initiateAuthenticationRequest(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context)
			throws AuthenticationFailedException {
		sendToLoginPage(request, response, context.getContextIdentifier());
	}
    
    @Override
	protected void processAuthenticationResponse(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context)
			throws AuthenticationFailedException {
		
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                principal = (Principal) session
                        .getAttribute(IWAServelet.PRINCIPAL_SESSION_KEY);
            }
        }

        if (principal == null || principal.getName() == null) {
            throw new AuthenticationFailedException("Authentication Failed");
        }

        String username = principal.getName();
        username = username.substring(username.indexOf("\\") + 1);

        if (log.isDebugEnabled()) {
            log.debug("Authenticate request received : Authtype - " + request.getAuthType() +
                      ", User - " + username);
        }

        boolean isAuthenticated;

        UserStoreManager userStoreManager;

        // Check the authentication
         try {
         	userStoreManager = (UserStoreManager) CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager();
 	        isAuthenticated = userStoreManager.isExistingUser(MultitenantUtils.getTenantAwareUsername(username));
         } catch (org.wso2.carbon.user.api.UserStoreException e) {
         	log.error("IWAAuthenticator failed while trying to find user existence", e);
         	throw new AuthenticationFailedException("Failed to find user existence");
         }
         
         if (!isAuthenticated) {
             if (log.isDebugEnabled()) {
                 log.debug("user authentication failed");
             }
             
             throw new AuthenticationFailedException("Authentication Failed");
         }
         username = FrameworkUtils.prependUserStoreDomainToName(username);
         context.setSubject(username);
	}
    
    public void sendToLoginPage(HttpServletRequest request, HttpServletResponse response, String ctx) {

        if (log.isTraceEnabled()) {
            log.trace("Inside sendToLoginPage()");
        }
        try {
            String iwaURL = CarbonUIUtil.getAdminConsoleURL(request).replace("commonauth", "iwa") + "?" + IWA_PARAM_STATE + "=" + URLEncoder.encode(ctx, "UTF-8");
            response.sendRedirect(response.encodeRedirectURL(iwaURL));
        } catch (IOException e) {
            log.error("Error when sending to the login page", e);
        }
        return;
    }

	@Override
	public String getContextIdentifier(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
    		log.trace("Inside getContextIdentifier()");
    	}

        return request.getParameter(IWA_PARAM_STATE);
    }

	@Override
	public String getFriendlyName() {
		return "iwa";	}

	@Override
	public String getName() {
		 return "IWAAuthenticator";
	}
}
