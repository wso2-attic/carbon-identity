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
package org.wso2.carbon.identity.sso.saml.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.xml.XMLObject;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.SAMLSSOService;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSORespDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOSessionDTO;
import org.wso2.carbon.identity.sso.saml.logout.LogoutRequestSender;
import org.wso2.carbon.identity.sso.saml.processors.LogoutRequestProcessor;
import org.wso2.carbon.identity.sso.saml.session.SSOSessionPersistenceManager;
import org.wso2.carbon.identity.sso.saml.session.SessionInfoData;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * This is the entry point for authentication process in an SSO scenario. This servlet is registered
 * with the URL pattern /samlsso and act as the control servlet. The message flow of an SSO scenario
 * is as follows.
 * <ol>
 * <li>SP sends a SAML Request via HTTP POST to the https://<ip>:<port>/samlsso endpoint.</li>
 * <li>IdP validates the SAML Request and checks whether this user is already authenticated.</li>
 * <li>If the user is authenticated, it will generate a SAML Response and send it back the SP via
 * the samlsso_redirect_ajaxprocessor.jsp.</li>
 * <li>If the user is not authenticated, it will send him to the login page and prompts user to
 * enter his credentials.</li>
 * <li>If these credentials are valid, then the user will be redirected back the SP with a valid
 * SAML Assertion. If not, he will be prompted again for credentials.</li>
 * </ol>
 */
public class SAMLSSOProviderServlet extends HttpServlet {

    private static final long serialVersionUID = -5182312441482721905L;
    private static Log log = LogFactory.getLog(SAMLSSOProviderServlet.class);

    private SAMLSSOService samlSsoService = new SAMLSSOService();

    @Override
    protected void doGet(HttpServletRequest httpServletRequest,
                         HttpServletResponse httpServletResponse) throws ServletException, IOException {
        handleRequest(httpServletRequest, httpServletResponse, false);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp, true);
    }

    /**
     * All requests are handled by this handleRequest method. In case of SAMLRequest the user
     * will be redirected to commonAuth servlet for authentication. Based on successful
     * authentication of the user a SAMLResponse is sent back to service provider.
     * In case of logout requests, the IDP will send logout requests
     * to the other session participants and then send the logout response back to the initiator.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    private void handleRequest(HttpServletRequest req, HttpServletResponse resp, boolean isPost)
            throws ServletException, IOException {

        String sessionId = null;
        Cookie ssoTokenIdCookie = getTokenIdCookie(req);

        if (ssoTokenIdCookie != null){
            sessionId = ssoTokenIdCookie.getValue();
        }

        Cookie rememberMeCookie = getRememberMeCookie(req);
        if (rememberMeCookie != null) {
            sessionId = rememberMeCookie.getValue();
        }

        String queryString = req.getQueryString();
        if (log.isDebugEnabled()) {
            log.debug("Query string : " + queryString);
        }
        // if an openid authentication or password authentication
        String authMode = req.getParameter("authMode");
        if (!SAMLSSOConstants.AuthnModes.OPENID.equals(authMode)) {
            authMode = SAMLSSOConstants.AuthnModes.USERNAME_PASSWORD;
        }
        String relayState = req.getParameter(SAMLSSOConstants.RELAY_STATE);
        String spEntityID = req.getParameter("spEntityID");
        String samlRequest = req.getParameter("SAMLRequest");

        try {
            if (req.getAttribute("commonAuthAuthenticated") != null) { //Response from common authentication framework.
                sessionId = UUIDGenerator.generateUUID();
                handleRequestFromLoginPage(req, resp, sessionId,
                        (String)req.getAttribute(SAMLSSOConstants.SESSION_DATA_KEY));
            } else if (req.getAttribute("commonAuthLoggedOut") != null) {
            	handleLogoutReponseFromAuthenFramework(req, resp);
            } else if (spEntityID != null) { // idp initiated SSO
                handleIdPInitSSO(req, resp, spEntityID, relayState, queryString, authMode, sessionId);
            } else if (samlRequest != null) {// SAMLRequest received. SP initiated SSO
                handleSPInitSSO(req, resp, queryString, relayState, authMode, samlRequest, sessionId, isPost);
            } else {
                log.debug("Invalid request message or single logout message ");
                // Non-SAML request are assumed to be logout requests
                sendToAuthenFrameworkForLogout(req, resp, null, null, sessionId);
                sendNotification(SAMLSSOConstants.Notification.INVALID_MESSAGE_STATUS,
                        SAMLSSOConstants.Notification.INVALID_MESSAGE_MESSAGE, req,
                        resp);
                return;
            }
        } catch (IdentityException e) {
            log.error("Error when processing the authentication request!", e);
            sendNotification(SAMLSSOConstants.Notification.EXCEPTION_STATUS
            		,
                    SAMLSSOConstants.Notification.EXCEPTION_MESSAGE, req, resp);
        }
    }

    /**
     * Prompts user a notification with the status and message
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    private void sendNotification(String status, String message, HttpServletRequest req,
                                  HttpServletResponse resp) throws ServletException, IOException {
        String redirectURL = CarbonUIUtil.getAdminConsoleURL(req);
        redirectURL = redirectURL.replace("samlsso/carbon/",
                "authenticationendpoint/samlsso_notification.do");
        //TODO Send status codes rather than full messages in the GET request
        String queryParams = "?" + SAMLSSOConstants.STATUS + "=" + status + "&" +
                SAMLSSOConstants.STATUS_MSG + "=" + message;
        resp.sendRedirect(redirectURL + queryParams);
    }

    private void handleIdPInitSSO(HttpServletRequest req, HttpServletResponse resp, String spEntityID, String relayState,
                                  String queryString, String authMode, String sessionId)
            throws IdentityException, IOException, ServletException {

        String rpSessionId = req.getParameter(MultitenantConstants.SSO_AUTH_SESSION_ID);
        SAMLSSOService samlSSOService = new SAMLSSOService();

        SAMLSSOReqValidationResponseDTO signInRespDTO = samlSSOService.validateIdPInitSSORequest(req, resp,
                spEntityID, relayState, queryString, sessionId, rpSessionId, authMode);

        if (signInRespDTO.isValid() && signInRespDTO.getResponse() != null) {
            // user already has an existing SSO session, redirect
            if (SAMLSSOConstants.AuthnModes.OPENID.equals(authMode)) {

                storeRememberMeCookie(sessionId, req, resp, samlSSOService.getSSOSessionTimeout());
            }
            if(samlSSOService.isSAMLSSOLoginAccepted()){
                req.getSession().setAttribute("authenticatedOpenID", SAMLSSOUtil.getOpenID(signInRespDTO.getSubject()));
                req.getSession().setAttribute("openId",SAMLSSOUtil.getOpenID(signInRespDTO.getSubject()));
            }
            sendResponse(req, resp, relayState, signInRespDTO.getResponse(),
                    signInRespDTO.getAssertionConsumerURL(), signInRespDTO.getSubject());
        } else if (signInRespDTO.isValid() && samlSsoService.isOpenIDLoginAccepted() &&
                req.getSession().getAttribute("authenticatedOpenID") != null){
            handleRequestWithOpenIDLogin(req,resp,signInRespDTO,relayState,sessionId);
        } else if (signInRespDTO.isValid() && signInRespDTO.getResponse() == null) {
            // user doesn't have an existing SSO session, so authenticate
            sendToAuthenticate(req, resp, signInRespDTO, relayState);
        } else {
            log.debug("Invalid SAML SSO Request");
            throw new IdentityException("Invalid SAML SSO Request");
        }
    }

    /**
     * If the SAMLRequest is a Logout request then IDP will send logout requests to other session
     * participants and then sends the logout Response back to the initiator. In case of
     * authentication request, check if there is a valid session for the user, if there is, the user
     * will be redirected directly to the Service Provider, if not the user will be redirected to
     * the login page.
     *
     * @param req
     * @param resp
     * @param sessionId
     * @param samlRequest
     * @param relayState
     * @param authMode
     * @throws IdentityException
     * @throws IOException
     * @throws ServletException
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    private void handleSPInitSSO(HttpServletRequest req, HttpServletResponse resp,
                                 String queryString, String relayState, String authMode,
                                 String samlRequest, String sessionId, boolean isPost)
            throws IdentityException, IOException, ServletException {

        String rpSessionId = req.getParameter(MultitenantConstants.SSO_AUTH_SESSION_ID);
        SAMLSSOService samlSSOService = new SAMLSSOService();

        SAMLSSOReqValidationResponseDTO signInRespDTO = samlSSOService.validateSPInitSSORequest(
                samlRequest, queryString, sessionId, rpSessionId, authMode, isPost);
        if (!signInRespDTO.isLogOutReq()) { // an <AuthnRequest> received
            if (signInRespDTO.isValid() && signInRespDTO.getResponse() != null && !signInRespDTO.isPassive()) {
                // user already has an existing SSO session, redirect
                if (SAMLSSOConstants.AuthnModes.OPENID.equals(authMode)) {

                    storeRememberMeCookie(sessionId, req, resp, samlSSOService.getSSOSessionTimeout());
                }
                if(samlSSOService.isSAMLSSOLoginAccepted()){
                    req.getSession().setAttribute("authenticatedOpenID",SAMLSSOUtil.getOpenID(signInRespDTO.getSubject()));
                    req.getSession().setAttribute("openId",SAMLSSOUtil.getOpenID(signInRespDTO.getSubject()));
                }
                sendResponse(req, resp, relayState, signInRespDTO.getResponse(),
                        signInRespDTO.getAssertionConsumerURL(), signInRespDTO.getSubject());
            } else if (signInRespDTO.isValid() && samlSsoService.isOpenIDLoginAccepted() &&
                    req.getSession().getAttribute("authenticatedOpenID") != null){
                handleRequestWithOpenIDLogin(req,resp,signInRespDTO,relayState,sessionId);
            } else if(signInRespDTO.isValid() && signInRespDTO.getResponse() != null && signInRespDTO.isPassive()){
                sendResponse(req, resp, relayState, signInRespDTO.getResponse(),
                        signInRespDTO.getAssertionConsumerURL(), signInRespDTO.getSubject());
            } else if (signInRespDTO.isValid() && signInRespDTO.getResponse() == null && !signInRespDTO.isPassive()) {
                // user doesn't have an existing SSO session, so authenticate
                sendToAuthenticate(req, resp, signInRespDTO, relayState);
            } else {
                log.debug("Invalid SAML SSO Request");
                throw new IdentityException("Invalid SAML SSO Request");
            }
        } else { // a <LogoutRequest> received
        	sendToAuthenFrameworkForLogout(req, resp, signInRespDTO, relayState, sessionId);
        }
    }

    /**
     * Sends the user for authentication to the login page
     *
     * @param req
     * @param resp
     * @param signInRespDTO
     * @param relayState
     * @throws ServletException
     * @throws IOException
     */
    private void sendToAuthenticate(HttpServletRequest req, HttpServletResponse resp,
                                    SAMLSSOReqValidationResponseDTO signInRespDTO, String relayState)
            throws ServletException, IOException {

        SAMLSSOSessionDTO sessionDTO = new SAMLSSOSessionDTO();
        sessionDTO.setHttpQueryString(req.getQueryString());
        sessionDTO.setDestination(signInRespDTO.getDestination());
        sessionDTO.setRelayState(relayState);
        sessionDTO.setRequestMessageString(signInRespDTO.getRequestMessageString());
        sessionDTO.setIssuer(signInRespDTO.getIssuer());
        sessionDTO.setRequestID(signInRespDTO.getId());
        sessionDTO.setSubject(signInRespDTO.getSubject());
        sessionDTO.setRelyingPartySessionId(signInRespDTO.getRpSessionId());
        sessionDTO.setAssertionConsumerURL(signInRespDTO.getAssertionConsumerURL());
        if(signInRespDTO.isIdPInitSSO()) {
            sessionDTO.setIdPInitSSO(true);
        } else {
            sessionDTO.setIdPInitSSO(false);
        }

        String sessionDataKey = UUIDGenerator.generateUUID();
        HttpSession session = req.getSession();
        session.setAttribute(sessionDataKey, sessionDTO);

        String commonAuthURL = CarbonUIUtil.getAdminConsoleURL(req);
        commonAuthURL = commonAuthURL.replace("samlsso/carbon/", "commonauth");

        String selfPath = URLEncoder.encode("../../samlsso","UTF-8");

        String queryParams = "?" + req.getQueryString() + "&issuer=" + signInRespDTO.getIssuer() +
                "&" + SAMLSSOConstants.SESSION_DATA_KEY + "=" + sessionDataKey +
                "&type=samlsso" +
                "&commonAuthCallerPath=" + selfPath +
                "&forceAuthenticate=false";

        resp.sendRedirect(commonAuthURL + queryParams);
    }
    
    private void sendToAuthenFrameworkForLogout(HttpServletRequest request, HttpServletResponse response, 
    								SAMLSSOReqValidationResponseDTO signInRespDTO, String relayState, String sessionId) 
    										throws ServletException, IOException {
    	
        if (sessionId != null) {
            
            SAMLSSOSessionDTO sessionDTO = new SAMLSSOSessionDTO();
            sessionDTO.setHttpQueryString(request.getQueryString());
            sessionDTO.setRelayState(relayState);
            sessionDTO.setSessionId(sessionId);
            
            if (signInRespDTO != null) {
            	sessionDTO.setDestination(signInRespDTO.getDestination());
                sessionDTO.setRequestMessageString(signInRespDTO.getRequestMessageString());
                sessionDTO.setIssuer(signInRespDTO.getIssuer());
                sessionDTO.setRequestID(signInRespDTO.getId());
                sessionDTO.setSubject(signInRespDTO.getSubject());
                sessionDTO.setRelyingPartySessionId(signInRespDTO.getRpSessionId());
                sessionDTO.setAssertionConsumerURL(signInRespDTO.getAssertionConsumerURL());
                sessionDTO.setValidationRespDTO(signInRespDTO);
            }
            
        	String sessionDataKey = UUIDGenerator.generateUUID();
            request.getSession().setAttribute(sessionDataKey, sessionDTO);

            String commonAuthURL = CarbonUIUtil.getAdminConsoleURL(request);
            commonAuthURL = commonAuthURL.replace("samlsso/carbon/", "commonauth");

            String selfPath = URLEncoder.encode("../../samlsso","UTF-8");
            
            SSOSessionPersistenceManager sessionPersistenceManager = SSOSessionPersistenceManager.getPersistenceManager();
            SessionInfoData sessionInfo = sessionPersistenceManager.
                    getSessionInfo(sessionPersistenceManager.getSessionIndexFromTokenId(sessionId));
            String authenticators = sessionInfo.getAuthenticators();
            
            String queryParams = "?" + request.getQueryString() +
                    "&" + SAMLSSOConstants.SESSION_DATA_KEY + "=" + sessionDataKey +
                    "&type=samlsso" +
                    "&commonAuthCallerPath=" + selfPath +
                    "&commonAuthLogout=true" + 
                    "&authenticatedAuthenticators=" + authenticators;
            
            if (signInRespDTO != null) {
            	queryParams = queryParams + "&issuer=" + signInRespDTO.getIssuer();
            }

            response.sendRedirect(commonAuthURL + queryParams);
        }
    }

    /**
     * Sends the Response message back to the Service Provider.
     *
     * @param req
     * @param resp
     * @param relayState
     * @param response
     * @param acUrl
     * @param subject
     * @throws ServletException
     * @throws IOException
     */
    private void sendResponse(HttpServletRequest req, HttpServletResponse resp, String relayState,
                              String response, String acUrl, String subject) throws ServletException, IOException {

        HttpSession session = req.getSession();
        session.removeAttribute(SAMLSSOConstants.SESSION_DATA_KEY);

        if(relayState != null){
            relayState = URLDecoder.decode(relayState, "UTF-8");
            relayState = relayState.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;").
                    replaceAll("<", "&lt;").replaceAll(">", "&gt;").replace("\n", "");
        }

        acUrl = getACSUrlWithTenantPartitioning(acUrl, subject);

        PrintWriter out = resp.getWriter();
        out.println("<html>");
        out.println("<body>");
        out.println("<p>You are now redirected back to " + acUrl);
        out.println(" If the redirection fails, please click the post button.</p>");
        out.println("<form method='post' action='" + acUrl + "'>");
        out.println("<p>");
        out.println("<input type='hidden' name='SAMLResponse' value='" + response + "'>");
        out.println("<input type='hidden' name='RelayState' value='" + relayState + "'>");
        out.println("<button type='submit'>POST</button>");
        out.println("</p>");
        out.println("</form>");
        out.println("<script type='text/javascript'>");
        out.println("document.forms[0].submit();");
        out.println("</script>");
        out.println("</body>");
        out.println("</html>");
    }

    /**
     * This method handles authentication and sends authentication Response message back to the
     * Service Provider after successful authentication. In case of authentication failure the user
     * is prompted back for authentication.
     *
     * @param req
     * @param resp
     * @param sessionId
     * @throws IdentityException
     * @throws IOException
     * @throws ServletException
     */
    private void handleRequestFromLoginPage(HttpServletRequest req, HttpServletResponse resp,
                                            String sessionId, String sessionDataKey) throws IdentityException, IOException, ServletException {

    	if (req.getAttribute("commonAuthAuthenticated") != null &&
    			!((Boolean)req.getAttribute("commonAuthAuthenticated")).booleanValue()) {
    		if (log.isDebugEnabled()) {
    			log.debug("Unauthenticated User");
    		}
    		//TODO send a saml response with a status message.
    		sendNotification(SAMLSSOConstants.Notification.EXCEPTION_STATUS,
                    SAMLSSOConstants.Notification.EXCEPTION_MESSAGE, req, resp);
    		return;
    	}
    	
        SAMLSSOSessionDTO sessionDTO = (SAMLSSOSessionDTO)req.getSession().getAttribute(sessionDataKey);

        if (sessionDTO==null){
            sendNotification(SAMLSSOConstants.Notification.EXCEPTION_STATUS,
                    SAMLSSOConstants.Notification.INVALID_MESSAGE_MESSAGE, req, resp);
            log.error("The value of sessionDTO is null. This could be due to the hostname settings");
            return;
        }

        String relayState = null;

        if (req.getParameter(SAMLSSOConstants.RELAY_STATE) != null){
            relayState = req.getParameter(SAMLSSOConstants.RELAY_STATE);
        } else {
            relayState = sessionDTO.getRelayState();
        }

        SAMLSSOAuthnReqDTO authnReqDTO = new SAMLSSOAuthnReqDTO();

        populateAuthnReqDTO(req, authnReqDTO, sessionDTO);

        SAMLSSOService samlSSOService = new SAMLSSOService();
        SAMLSSORespDTO authRespDTO = null;
        if(samlSSOService.isOpenIDLoginAccepted() && req.getSession().getAttribute("authenticatedOpenID") != null){
            authnReqDTO.setUsername(SAMLSSOUtil.getUserNameFromOpenID(
                    (String)req.getSession().getAttribute("authenticatedOpenID")));
            authRespDTO = samlSSOService.authenticate(authnReqDTO, sessionId, true, null, SAMLSSOConstants.AuthnModes.OPENID);
        } else {
            authRespDTO = samlSSOService.authenticate(authnReqDTO, sessionId, (Boolean)req.getAttribute("commonAuthAuthenticated"), 
            		(String)req.getAttribute("authenticatedAuthenticators"), SAMLSSOConstants.AuthnModes.USERNAME_PASSWORD);
        }

        if (authRespDTO.isSessionEstablished()) { // authenticated
            if(req.getParameter("chkRemember") != null && req.getParameter("chkRemember").equals("on")){
                storeRememberMeCookie(sessionId, req, resp, samlSSOService.getSSOSessionTimeout());
            }

            storeTokenIdCookie(sessionId, req, resp);

            if(samlSSOService.isSAMLSSOLoginAccepted()){
                req.getSession().setAttribute("authenticatedOpenID",SAMLSSOUtil.getOpenID(authRespDTO.getSubject()));
                req.getSession().setAttribute("openId",SAMLSSOUtil.getOpenID(authRespDTO.getSubject()));
            }
            sendResponse(req, resp, relayState, authRespDTO.getRespString(),
                    authRespDTO.getAssertionConsumerURL(), authRespDTO.getSubject());
        } else { // authentication FAILURE
            sendNotification(SAMLSSOConstants.Notification.EXCEPTION_STATUS,
                    SAMLSSOConstants.Notification.EXCEPTION_MESSAGE, req, resp);
        }
    }
    
    private void handleLogoutReponseFromAuthenFramework(HttpServletRequest request, HttpServletResponse response) 
    					throws ServletException, IOException{
    	
    	String sessionDataKey = (String)request.getAttribute(SAMLSSOConstants.SESSION_DATA_KEY);
    	SAMLSSOSessionDTO sessionDTO = (SAMLSSOSessionDTO)request.getSession().getAttribute(sessionDataKey);
    	
    	if (sessionDTO == null){
            log.error("The value of sessionDTO is null. This could be due to the hostname settings");
            sendNotification(SAMLSSOConstants.Notification.EXCEPTION_STATUS,
                    SAMLSSOConstants.Notification.INVALID_MESSAGE_MESSAGE, request, response);
            return;
        }
    	
    	SAMLSSOReqValidationResponseDTO validatonResponseDTO = sessionDTO.getValidationRespDTO();
    	
    	if (validatonResponseDTO != null) {
    		// sending LogoutRequests to other session participants
            LogoutRequestSender.getInstance().sendLogoutRequests(validatonResponseDTO.getLogoutRespDTO());
            
            SAMLSSOService samlSSOService = new SAMLSSOService();
            
            if(samlSSOService.isSAMLSSOLoginAccepted()){
            	request.getSession().removeAttribute("authenticatedOpenID");
            	request.getSession().removeAttribute("openId");
            }
            
            new LogoutRequestProcessor().removeSession(sessionDTO.getSessionId(), validatonResponseDTO.getIssuer());
            
            // sending LogoutResponse back to the initiator
            sendResponse(request, response, sessionDTO.getRelayState(), validatonResponseDTO.getLogoutResponse(),
            		validatonResponseDTO.getAssertionConsumerURL(), validatonResponseDTO.getSubject());
    	} else {
    		try {
				samlSsoService.doSingleLogout(request.getSession().getId());
			} catch (IdentityException e) {
				log.error("Error when processing the logout request!", e);
	            sendNotification(SAMLSSOConstants.Notification.EXCEPTION_STATUS,
	                    SAMLSSOConstants.Notification.EXCEPTION_MESSAGE, request, response);
			}
    		
    		sendNotification(SAMLSSOConstants.Notification.INVALID_MESSAGE_STATUS,
                    SAMLSSOConstants.Notification.INVALID_MESSAGE_MESSAGE, request,
                    response);
    	}
    }

    /**
     *
     * @param req
     * @param authnReqDTO
     */
    private void populateAuthnReqDTO(HttpServletRequest req, SAMLSSOAuthnReqDTO authnReqDTO,
                                     SAMLSSOSessionDTO sessionDTO) {
        authnReqDTO.setAssertionConsumerURL(sessionDTO.getAssertionConsumerURL());
        authnReqDTO.setId(sessionDTO.getRequestID());
        authnReqDTO.setIssuer(sessionDTO.getIssuer());
        authnReqDTO.setSubject(sessionDTO.getSubject());
        authnReqDTO.setRpSessionId(sessionDTO.getRelyingPartySessionId());
        authnReqDTO.setRequestMessageString(sessionDTO.getRequestMessageString());
        authnReqDTO.setQueryString(sessionDTO.getHttpQueryString());
        authnReqDTO.setDestination(sessionDTO.getDestination());
        authnReqDTO.setUsername((String)req.getAttribute("authenticatedUser"));
        authnReqDTO.setIdPInitSSO(sessionDTO.isIdPInitSSO());
    }

    /**
     *
     * @param req
     * @return
     */
    private Cookie getRememberMeCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("samlssoRememberMe")) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     *
     * @param sessionId
     * @param req
     * @param resp
     */
    private void storeRememberMeCookie(String sessionId, HttpServletRequest req, HttpServletResponse resp,
                                       int sessionTimeout) {
        Cookie rememberMeCookie = getRememberMeCookie(req);
        if (rememberMeCookie == null) {
            rememberMeCookie = new Cookie("samlssoRememberMe", sessionId);
        }
        rememberMeCookie.setMaxAge(sessionTimeout);
        resp.addCookie(rememberMeCookie);
    }

    private Cookie getTokenIdCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("samlssoTokenId")) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     *
     * @param sessionId
     * @param req
     * @param resp
     */
    private void storeTokenIdCookie(String sessionId, HttpServletRequest req, HttpServletResponse resp) {
        Cookie rememberMeCookie = getRememberMeCookie(req);
        if (rememberMeCookie == null) {
            rememberMeCookie = new Cookie("samlssoTokenId", sessionId);
        }
        resp.addCookie(rememberMeCookie);
    }

    /**
     *
     * @param customLoginPage
     * @return
     */
    private String getLoginPage(String customLoginPage) {
        if (customLoginPage != null && customLoginPage.length() != 0) {
            return "/carbon/" + customLoginPage.trim();
        } else {
            return "authenticationendpoint/" + "samlsso/samlsso_auth_ajaxprocessor.jsp";
        }
    }

    /**
     *
     * @param req
     * @param paramName
     * @return
     */
    private String getRequestParameter(HttpServletRequest req, String paramName) {
        // This is to handle "null" values coming as the parameter values from the JSP.
        if(req.getParameter(paramName) != null && !req.getParameter(paramName).equals("null")){
            return req.getParameter(paramName);
        } else if (req.getAttribute(paramName) != null && !req.getAttribute(paramName).equals("null")) {
            return (String)req.getAttribute(paramName);
        }
        return null;
    }

    private void handleRequestWithOpenIDLogin(HttpServletRequest req, HttpServletResponse resp,
                                              SAMLSSOReqValidationResponseDTO signInRespDTO, String relayState, String sessionId)
            throws ServletException, IOException, IdentityException {

        SAMLSSOSessionDTO sessionDTO = new SAMLSSOSessionDTO();

        sessionDTO.setHttpQueryString(req.getQueryString());
        sessionDTO.setDestination(signInRespDTO.getDestination());
        sessionDTO.setRelayState(relayState);
        sessionDTO.setRequestMessageString(signInRespDTO.getRequestMessageString());
        sessionDTO.setIssuer(signInRespDTO.getIssuer());
        sessionDTO.setRequestID(signInRespDTO.getId());
        sessionDTO.setSubject(signInRespDTO.getSubject());
        sessionDTO.setRelyingPartySessionId(signInRespDTO.getRpSessionId());
        sessionDTO.setAssertionConsumerURL(signInRespDTO.getAssertionConsumerURL());

        String sessionDataKey = UUIDGenerator.generateUUID();
        HttpSession session = req.getSession();
        session.setAttribute(sessionDataKey, sessionDTO);

        handleRequestFromLoginPage(req,resp,sessionId, sessionDataKey);
    }

    private String getACSUrlWithTenantPartitioning(String acsUrl, String subject) {
        String domain = null;
        String acsUrlWithTenantDomain = acsUrl;
        if (subject != null && MultitenantUtils.getTenantDomain(subject) != null) {
            domain = MultitenantUtils.getTenantDomain(subject);
        }
        if (domain != null &&
                "true".equals(IdentityUtil.getProperty((IdentityConstants.ServerConfig.SSO_TENANT_PARTITIONING_ENABLED)))) {
            acsUrlWithTenantDomain =
                    acsUrlWithTenantDomain + "?" +
                            MultitenantConstants.TENANT_DOMAIN + "=" + domain;
        }
        return acsUrlWithTenantDomain;
    }
}
