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


package org.wso2.carbon.identity.sso.saml.tomcat.agent;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.wso2.carbon.identity.sso.saml.tomcat.agent.authenticator.Authenticator;
import org.wso2.carbon.identity.sso.saml.tomcat.agent.authenticator.SimpleAuthenticator;

/**
 * Servlet Filter implementation class SSOAgentFilter
 */
public class SSOAgentFilter implements Filter {

	private SSOManager ssoManager;
	private Authenticator authenticator;

	public static final Log log = LogFactory.getLog(SSOAgentFilter.class);
	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {



		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
        if(log.isDebugEnabled()){
            log.debug("JSESSION ID " + request.getSession().getId());
            log.debug("REQUESTD URI  " + request.getRequestURI());
        }
		String requestWithoutParams = request.getRequestURI();
		if(requestWithoutParams.indexOf("?")>-1){
			requestWithoutParams = requestWithoutParams.substring(0, requestWithoutParams.indexOf("?"));
		}
		if(requestWithoutParams.endsWith(SSOConfigs.getSsoLoginPage())){
			doSSO(request, response);
			return;
		}
		else if(request.getRequestURI().endsWith(SSOConfigs.getLogoutPage())){
			if(request.getSession().getAttribute(SSOConstants.AUTHENTICATED)!=null){
				response.sendRedirect(SSOConfigs.getSsoLoginPage()+"?logout=1");
				return;
			}else{
				chain.doFilter(request, response);
				return;
			}
		}
		else if (request.getSession().getAttribute(SSOConstants.AUTHENTICATED) == null) {
			response.sendRedirect(SSOConfigs.getSsoLoginPage());
			return;
		} else {
			Date lastAccessed = (Date) request.getSession().getAttribute(
					SSOConstants.LAST_ACCESSED_TIME);
			Date now = new Date();
			if (now.getTime() - lastAccessed.getTime() > SSOConstants.SESSION_EXPIRE_TIME) {
				SSOSessionManager.invalidateSession(request.getSession());
				response.sendRedirect(SSOConfigs.getSsoLoginPage());
			} else {
				request.setAttribute(SSOConstants.LAST_ACCESSED_TIME, now);
			}
		}
		// pass the request along the filter chain
		chain.doFilter(request, response);
	}
	
	/**
	 * This method handles the login and logout requests from the user or IdP
	 * Any request for the defined login/logout URL is handled here
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void doSSO(HttpServletRequest request, HttpServletResponse response)
                                                            throws ServletException, IOException {

		String samlRequest = request.getParameter(SSOConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ);
        if (samlRequest != null) {
            //This is a single logout request from the IdP
			try {

                // if identity server version is 4.X.X, response is Base64 encoded. Therefore decoding
                // is needed
                if(SSOConfigs.getServerVersion() == 4){
                    samlRequest = Util.decode(samlRequest);
                }
				XMLObject samlObject = ssoManager.unmarshall(samlRequest);
				if (samlObject instanceof LogoutRequest) {
                    if(log.isDebugEnabled()){
                        log.debug("SAML LOGOUT REQUEST IS RECEIVED. REQUEST : " +  samlRequest);
                    }
					LogoutRequest logoutRequest = (LogoutRequest) samlObject;
					String sessionIndex = logoutRequest.getSessionIndexes()
							.get(0).getSessionIndex();
					SSOSessionManager.invalidateSessionByIdPSId(sessionIndex);
				}
			} catch (SSOAgentException e) {
                log.error("Invalid SAML request", e);
                handleMalformedResponses(request, response, "Invalid SAML request");
			}
			return;
		}

		String samlResponse = request.getParameter(SSOConstants.HTTP_POST_PARAM_SAML2_RESP);
        if (samlResponse != null) {
            XMLObject samlObject = null;
            try {
                // if identity server version is 4.X.X, response is Base64 encoded. Therefore decoding
                // is needed
                if(SSOConfigs.getServerVersion() == 4){
                    samlResponse = Util.decode(samlResponse);
                }
                samlObject = ssoManager.unmarshall(samlResponse);
            } catch (SSOAgentException e) {
                log.error("Invalid SAML response", e);
                handleMalformedResponses(request, response, "Invalid SAML response");
                return;
            }

            if (samlObject instanceof LogoutResponse) {
                if(log.isDebugEnabled()){
                    log.debug("SAML LOGOUT RESPONSE IS RECEIVED. RESPONSE : " +  samlResponse);
                }
                //This is a SAML response for a single logout request from the SP
                SSOSessionManager.invalidateSession(request.getSession());
                response.sendRedirect(SSOConfigs.getLogoutPage());
                return;
            } else {
                if(log.isDebugEnabled()){
                    log.debug("SAML RESPONSE IS RECEIVED. RESPONSE : " +  samlResponse);
                }
                Response samlResponseObject = (Response) samlObject;
                List<Assertion> assertions = samlResponseObject.getAssertions();
                Assertion assertion = null;
                if (assertions != null && assertions.size() > 0) {
                    assertion = assertions.get(0);
                }

                if (assertion == null) {
                    if (samlResponseObject.getStatus() != null &&
                            samlResponseObject.getStatus().getStatusMessage() != null) {
                        log.error(samlResponseObject.getStatus().getStatusMessage().getMessage());
                    } else {
                        log.error("SAML Assertion not found in the Response");
                    }
                    handleMalformedResponses(request, response, "SAML Assertion can not be null");
                    return;
                }

                // Get the subject name from the Response Object and forward it to login_action.jsp
                String username = null;
                if(assertion.getSubject() != null && assertion.getSubject().getNameID() != null){
                    username = assertion.getSubject().getNameID().getValue();
                }

                if(username == null){
                    log.error("SAMLResponse does not contain the name of the subject");
                    handleMalformedResponses(request, response,
                            "SAMLResponse does not contain the name of the subject");
                    return;
                }

                // validate audience restriction
                if(!ssoManager.validateAudienceRestriction(assertion)){
                    handleMalformedResponses(request, response,
                            "SAML Assertion is not valid");
                    return;
                }

                // validate signature this SP only looking for assertion signature
                if(!ssoManager.validateSignature(samlResponseObject)){
                    handleMalformedResponses(request, response,
                            "SAML Assertion is not valid");
                    return;                    
                }

                //This is a SSO response, user need to be sign in
                Map<String, String> samlAttributeMap = ssoManager.getAssertionStatements(assertion);


                String sessionId = assertion.getAuthnStatements().get(0).getSessionIndex();

                //For removing the session when the single sign out request made by the SP itself
                request.getSession().setAttribute(SSOConstants.IDP_SESSION, sessionId);
                SSOSessionManager.addAuthenticatedSession(sessionId, request.getSession());
                request.getSession().setAttribute(SSOConstants.LAST_ACCESSED_TIME, new Date());

                try {
                    authenticator.authenticate(request, response, samlResponse, samlAttributeMap);
                } catch (Exception e) {
                    log.error(e);
                    handleMalformedResponses(request, response, e.getMessage());
                }
            }
			return;
		}

		try {
			//Request initiated by the user, may be a login or logout request
			String requestMessage = ssoManager.buildRequestMessage(request);
			response.sendRedirect(requestMessage);
		} catch (IOException e) {
            log.error(e);
            handleMalformedResponses(request, response, e.getMessage());
		}
	}

    /**
     * Handle malformed Responses.
     *
     * @param req   HttpServletRequest
     * @param resp  HttpServletResponse
     * @param errorMsg  Error message to be displayed in HttpServletResponse.jsp
     * @throws IOException  Error when redirecting
     */
    private void handleMalformedResponses(HttpServletRequest req, HttpServletResponse resp,
                                          String errorMsg) throws IOException {
        HttpSession session = req.getSession();
        session.setAttribute("error", errorMsg);
        resp.sendRedirect(SSOConfigs.getErrorPage());
    }    

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		try {
			//Initialize the configurations as 1st step.
			SSOConfigs.initConfigs(fConfig);
            // Initialize 
			ssoManager = new SSOManager();
			authenticator = new SimpleAuthenticator();
		} catch (ConfigurationException e) {
			throw new ServletException(
					"Error while configuring SAMLConsumerManager", e);
		}
	}

}
