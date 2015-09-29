/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.authenticator.saml2.sso.ui;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.SessionIndex;
import org.opensaml.saml2.core.Subject;
import org.opensaml.xml.XMLObject;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.authenticator.saml2.sso.common.FederatedSSOToken;
import org.wso2.carbon.identity.authenticator.saml2.sso.common.SAML2SSOAuthenticatorConstants;
import org.wso2.carbon.identity.authenticator.saml2.sso.common.SAML2SSOUIAuthenticatorException;
import org.wso2.carbon.identity.authenticator.saml2.sso.common.SAMLConstants;
import org.wso2.carbon.identity.authenticator.saml2.sso.common.Util;
import org.wso2.carbon.identity.authenticator.saml2.sso.ui.authenticator.SAML2SSOUIAuthenticator;
import org.wso2.carbon.identity.authenticator.saml2.sso.ui.client.SAMLSSOServiceClient;
import org.wso2.carbon.identity.authenticator.saml2.sso.ui.session.SSOSessionManager;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSORespDTO;
import org.wso2.carbon.ui.CarbonSecuredHttpContext;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.List;

/**
 *
 */
public class SSOAssertionConsumerService extends HttpServlet {

    public static final Log log = LogFactory.getLog(SSOAssertionConsumerService.class);
    public static final String SSO_TOKEN_ID = "ssoTokenId";
    /**
     *
     */
    private static final long serialVersionUID = 5451353570561170887L;
    /**
     * session timeout happens in 10 hours
     */
    private static final int SSO_SESSION_EXPIRE = 36000;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String samlRespString = req.getParameter(
                SAML2SSOAuthenticatorConstants.HTTP_POST_PARAM_SAML2_RESP);

        if (log.isDebugEnabled()) {
            log.debug("Processing SAML Response");

            Enumeration headerNames = req.getHeaderNames();
            log.debug("[Request Headers] :");
            while (headerNames.hasMoreElements()) {
                String headerName = (String) headerNames.nextElement();
                log.debug(">> " + headerName + ":" + req.getHeader(headerName));
            }

            Enumeration params = req.getParameterNames();
            log.debug("[Request Parameters] :");
            while (params.hasMoreElements()) {
                String paramName = (String) params.nextElement();
                log.debug(">> " + paramName + ":" + req.getParameter(paramName));
            }
        }

        // Handle single logout requests
        if (req.getParameter(SAML2SSOAuthenticatorConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ) != null) {
            handleSingleLogoutRequest(req, resp);
            return;
        }

        // If SAML Response is not present in the redirected req, send the user to an error page.
        if (samlRespString == null) {
            log.error("SAML Response is not present in the request.");
            handleMalformedResponses(req, resp,
                    SAML2SSOAuthenticatorConstants.ErrorMessageConstants.RESPONSE_NOT_PRESENT);
            return;
        }

//        // If RELAY-STATE is invalid, redirect the users to an error page.
//        if (!SSOSessionManager.isValidResponse(relayState)) {
//            handleMalformedResponses(req, resp,
//                                     SAML2SSOAuthenticatorConstants.ErrorMessageConstants.RESPONSE_INVALID);
//            return;
//        }

        // Handle valid messages, either SAML Responses or LogoutRequests
        try {
            XMLObject samlObject = Util.unmarshall(Util.decode(samlRespString));
            if (samlObject instanceof LogoutResponse) {   // if it is a logout response, redirect it to login page.
                String externalLogoutPage = Util.getExternalLogoutPage();
                if(externalLogoutPage != null && !externalLogoutPage.isEmpty()){
                    handleExternalLogout(req, resp, externalLogoutPage);
                } else {
                    resp.sendRedirect(getAdminConsoleURL(req) + "admin/logout_action.jsp?logoutcomplete=true");
                }
            } else if (samlObject instanceof Response) {    // if it is a SAML Response
                handleSAMLResponses(req, resp, samlObject);
            }
        } catch (SAML2SSOUIAuthenticatorException e) {
            log.error("Error when processing the SAML Assertion in the request.", e);
            handleMalformedResponses(req, resp, SAML2SSOAuthenticatorConstants.ErrorMessageConstants.RESPONSE_MALFORMED);
        }
    }

    /**
     * Handle SAML Responses and authenticate.
     *
     * @param req        HttpServletRequest
     * @param resp       HttpServletResponse
     * @param samlObject SAML Response object
     * @throws ServletException  Error when redirecting
     * @throws IOException       Error when redirecting
     */
    private void handleSAMLResponses(HttpServletRequest req, HttpServletResponse resp,
                                     XMLObject samlObject)
            throws ServletException, IOException, SAML2SSOUIAuthenticatorException {
        Response samlResponse;
        samlResponse = (Response) samlObject;
        List<Assertion> assertions = samlResponse.getAssertions();
        Assertion assertion = null;
        if (assertions != null && assertions.size() > 0) {
            assertion = assertions.get(0);
        }

        if (assertion == null) {

            // This condition would succeed if Passive Login Request was sent because SP session has timed out
            if (samlResponse.getStatus() != null &&
                    samlResponse.getStatus().getStatusCode() != null &&
                    samlResponse.getStatus().getStatusCode().getValue().equals("urn:oasis:names:tc:SAML:2.0:status:Responder") &&
                    samlResponse.getStatus().getStatusCode().getStatusCode() != null &&
                    samlResponse.getStatus().getStatusCode().getStatusCode().getValue().equals("urn:oasis:names:tc:SAML:2.0:status:NoPassive")) {

                RequestDispatcher requestDispatcher = req.getRequestDispatcher("/carbon/admin/login.jsp");
                requestDispatcher.forward(req, resp);
                return;

            }

            if (samlResponse.getStatus() != null &&
                    samlResponse.getStatus().getStatusMessage() != null) {
                log.error(samlResponse.getStatus().getStatusMessage().getMessage());
            } else {
                log.error("SAML Assertion not found in the Response.");
            }
            throw new SAML2SSOUIAuthenticatorException("SAML Authentication Failed.");
        }

        // Get the subject name from the Response Object and forward it to login_action.jsp
        String username = null;
        if (assertion.getSubject() != null && assertion.getSubject().getNameID() != null) {
            username = Util.getUsernameFromResponse(samlResponse);
        }

        if (log.isDebugEnabled()) {
            log.debug("A username is extracted from the response. : " + username);
        }

        if (username == null) {
            log.error("SAMLResponse does not contain the name of the subject");
            throw new SAML2SSOUIAuthenticatorException("SAMLResponse does not contain the name of the subject");
        }

        String relayState = req.getParameter(SAMLConstants.RELAY_STATE);
        boolean isFederated = false;

        if (relayState != null) {
            FederatedSSOToken federatedSSOToken = org.wso2.carbon.identity.authenticator.saml2.sso.common.SSOSessionManager
                    .getFederatedToken(relayState);
            if (federatedSSOToken != null) {
                isFederated = true;
                HttpServletRequest fedRequest = federatedSSOToken.getHttpServletRequest();

                String samlRequest = fedRequest.getParameter("SAMLRequest");
                String authMode = SAMLConstants.AuthnModes.USERNAME_PASSWORD;
                String fedRelayState = fedRequest.getParameter(SAMLConstants.RELAY_STATE);
                String rpSessionId = fedRequest.getParameter(MultitenantConstants.SSO_AUTH_SESSION_ID);

                Enumeration<String> e = fedRequest.getAttributeNames();

                while (e.hasMoreElements()) {
                    String name = e.nextElement();
                    req.setAttribute(name, fedRequest.getAttribute(name));
                }

                Cookie[] cookies = fedRequest.getCookies();

                if (cookies != null) {
                    for (int i = 0; i < cookies.length; i++) {
                        resp.addCookie(cookies[i]);
                    }
                }

                HttpSession session = fedRequest.getSession();

                // Use sessionID as the tokenID, if cookie is not set.
                String ssoTokenID = session.getId();
                Cookie tokenCookie = getSSOTokenCookie(fedRequest);
                if (tokenCookie != null) {
                    ssoTokenID = tokenCookie.getValue();
                }

                handleFederatedSAMLRequest(req, resp, ssoTokenID, samlRequest, fedRelayState, authMode, assertion.getSubject(), rpSessionId);
            }
        }

        if (!isFederated) {
            // Set the SAML2 Response as a HTTP Attribute, so it is not required to build the
            // assertion again.
            req.setAttribute(SAML2SSOAuthenticatorConstants.HTTP_ATTR_SAML2_RESP_TOKEN,
                    samlResponse);


            String sessionIndex = null;
            List<AuthnStatement> authnStatements = assertion.getAuthnStatements();
            if (authnStatements != null && authnStatements.size() > 0) {
                // There can be only one authentication stmt inside the SAML assertion of a SAML Response
                AuthnStatement authStmt = authnStatements.get(0);
                sessionIndex = authStmt.getSessionIndex();
            }

            String url = req.getRequestURI();
            url = url.replace("acs","carbon/admin/login_action.jsp?username=" + URLEncoder.encode(username, "UTF-8"));

            if(sessionIndex != null) {
                url += "&" + SAML2SSOAuthenticatorConstants.IDP_SESSION_INDEX + "=" + URLEncoder.encode(sessionIndex, "UTF-8");
            }

            if(log.isDebugEnabled()) {
                log.debug("Forwarding to path : " + url);
            }

            RequestDispatcher reqDispatcher = req.getRequestDispatcher(url);
            req.getSession().setAttribute("CarbonAuthenticator", new SAML2SSOUIAuthenticator());
            reqDispatcher.forward(req, resp);
        }
    }

    /**
     * Handle malformed Responses.
     *
     * @param req      HttpServletRequest
     * @param resp     HttpServletResponse
     * @param errorMsg Error message to be displayed in HttpServletResponse.jsp
     * @throws IOException Error when redirecting
     */
    private void handleMalformedResponses(HttpServletRequest req, HttpServletResponse resp,
                                          String errorMsg) throws IOException {
        HttpSession session = req.getSession();
        session.setAttribute(SAML2SSOAuthenticatorConstants.NOTIFICATIONS_ERROR_MSG, errorMsg);
        resp.sendRedirect(getAdminConsoleURL(req) + "sso-acs/notifications.jsp");
        return;
    }


    /**
     * This method is used to handle the single logout requests sent by the Identity Provider
     *
     * @param req  Corresponding HttpServletRequest
     * @param resp Corresponding HttpServletResponse
     */
    private void handleSingleLogoutRequest(HttpServletRequest req, HttpServletResponse resp) {
        String logoutReqStr = decodeHTMLCharacters(req.getParameter(
                SAML2SSOAuthenticatorConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ));

        XMLObject samlObject = null;

        try {
            samlObject = Util.unmarshall(Util.decode(logoutReqStr));
        } catch (SAML2SSOUIAuthenticatorException e) {
            log.error("Error handling the single logout request", e);
        }

        if (samlObject instanceof LogoutRequest) {
            LogoutRequest logoutRequest = (LogoutRequest) samlObject;
            //  There can be only one session index entry.
            List<SessionIndex> sessionIndexList = logoutRequest.getSessionIndexes();
            if (sessionIndexList.size() > 0) {
                SSOSessionManager.getInstance().handleLogout(
                        sessionIndexList.get(0).getSessionIndex());
            }
        }
    }

    /**
     * Get the admin console url from the request.
     *
     * @param request httpServletReq that hits the ACS Servlet
     * @return Admin Console URL       https://10.100.1.221:8443/acs/carbon/
     */
    private String getAdminConsoleURL(HttpServletRequest request) {
        String url = CarbonUIUtil.getAdminConsoleURL(request);
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        if (url.indexOf("/acs") != -1) {
            url = url.replace("/acs", "");
        }
        return url;
    }

    /**
     * A utility method to decode an HTML encoded string
     *
     * @param encodedStr encoded String
     * @return decoded String
     */
    private String decodeHTMLCharacters(String encodedStr) {
        return encodedStr.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"").replaceAll("&apos;", "'");

    }

    private void handleFederatedSAMLRequest(HttpServletRequest req, HttpServletResponse resp,
                                            String ssoTokenID, String samlRequest,
                                            String relayState, String authMode, Subject subject,
                                            String rpSessionId)
            throws IOException, ServletException, SAML2SSOUIAuthenticatorException {
        // Instantiate the service client.
        HttpSession session = req.getSession();
        String serverURL = CarbonUIUtil.getServerURL(session.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) session.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        SAMLSSOServiceClient ssoServiceClient = new SAMLSSOServiceClient(serverURL, configContext);

        String method = req.getMethod();
        boolean isPost = false;

        if ("post".equalsIgnoreCase(method)) {
            isPost = true;
        }

        SAMLSSOReqValidationResponseDTO signInRespDTO =
                ssoServiceClient.validate(samlRequest,
                        null, ssoTokenID,
                        rpSessionId,
                        authMode, isPost);
        if (signInRespDTO.getValid()) {
            handleRequestFromLoginPage(req, resp, ssoTokenID,
                    signInRespDTO.getAssertionConsumerURL(),
                    signInRespDTO.getId(), signInRespDTO.getIssuer(),
                    subject.getNameID().getValue(), subject.getNameID()
                            .getValue(),
                    signInRespDTO.getRpSessionId(),
                    signInRespDTO.getRequestMessageString(), relayState);
        }
    }

    private void handleRequestFromLoginPage(HttpServletRequest req, HttpServletResponse resp,
                                            String ssoTokenID, String assertionConsumerUrl, String id, String issuer, String userName, String subject,
                                            String rpSession, String requestMsgString, String relayState)
            throws IOException, ServletException, SAML2SSOUIAuthenticatorException {
        HttpSession session = req.getSession();

        // instantiate the service client
        String serverURL = CarbonUIUtil.getServerURL(session.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) session.getServletContext()
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        SAMLSSOServiceClient ssoServiceClient = new SAMLSSOServiceClient(serverURL, configContext);

        // Create SAMLSSOAuthnReqDTO using the request Parameters
        SAMLSSOAuthnReqDTO authnReqDTO = new SAMLSSOAuthnReqDTO();

        authnReqDTO.setAssertionConsumerURL(assertionConsumerUrl);
        authnReqDTO.setId(id);
        authnReqDTO.setIssuer(issuer);
        //TODO FIX NEED TO BE DONE
        authnReqDTO.setUser(null);
        authnReqDTO.setPassword("federated_idp_login");
        authnReqDTO.setSubject(subject);
        authnReqDTO.setRpSessionId(rpSession);
        authnReqDTO.setRequestMessageString(requestMsgString);

        // authenticate the user
        SAMLSSORespDTO authRespDTO = ssoServiceClient.authenticate(authnReqDTO, ssoTokenID);

        if (authRespDTO.getSessionEstablished()) { // authentication is SUCCESSFUL
            // Store the cookie
            storeSSOTokenCookie(ssoTokenID, req, resp);
            // add relay state, assertion string and ACS URL as request parameters.
            req.setAttribute(SAMLConstants.RELAY_STATE, relayState);
            req.setAttribute(SAMLConstants.ASSERTION_STR, authRespDTO.getRespString());
            req.setAttribute(SAMLConstants.ASSRTN_CONSUMER_URL, authRespDTO.getAssertionConsumerURL());
            req.setAttribute(SAMLConstants.SUBJECT, authRespDTO.getSubject());
            RequestDispatcher reqDispatcher = req.getRequestDispatcher("/carbon/sso-acs/federation_ajaxprocessor.jsp");
            reqDispatcher.forward(req, resp);
            return;
        }
    }

    private void storeSSOTokenCookie(String ssoTokenID, HttpServletRequest req,
                                     HttpServletResponse resp) {
        Cookie ssoTokenCookie = getSSOTokenCookie(req);
        if (ssoTokenCookie == null) {
            ssoTokenCookie = new Cookie(SSO_TOKEN_ID, ssoTokenID);
        }
        ssoTokenCookie.setMaxAge(SSO_SESSION_EXPIRE);
        resp.addCookie(ssoTokenCookie);
    }

    private Cookie getSSOTokenCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("ssoTokenId".equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    private void handleExternalLogout(HttpServletRequest req, HttpServletResponse resp, String externalLogoutPage) throws IOException {

        HttpSession currentSession = req.getSession(false);
        if (currentSession != null) {
            // check if current session has expired
            currentSession.removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);
            currentSession.getServletContext().removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);
            try {
                currentSession.invalidate();
                if(log.isDebugEnabled()) {
                    log.debug("Frontend session invalidated");
                }
            } catch (Exception ignored) {
                // Ignore exception when invalidating and invalidated session
            }
        }
        clearCookies(req, resp);

        if (log.isDebugEnabled()) {
            log.debug("Sending to " + externalLogoutPage);
        }
        resp.sendRedirect(externalLogoutPage);

    }

    private void clearCookies(HttpServletRequest req, HttpServletResponse resp) {
        Cookie[] cookies = req.getCookies();

        for (Cookie curCookie : cookies) {
            if (curCookie.getName().equals("requestedURI")) {
                Cookie cookie = new Cookie("requestedURI", null);
                cookie.setPath("/");
                cookie.setMaxAge(0);
                resp.addCookie(cookie);
            } else if (curCookie.getName().equals(CarbonConstants.REMEMBER_ME_COOKE_NAME)) {
                Cookie cookie = new Cookie(CarbonConstants.REMEMBER_ME_COOKE_NAME, null);
                cookie.setPath("/");
                cookie.setMaxAge(0);
                resp.addCookie(cookie);
            }
        }
    }
}