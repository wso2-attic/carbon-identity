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

package org.wso2.carbon.identity.application.authenticator.samlsso;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authenticator.samlsso.manager.DefaultSAML2SSOManager;
import org.wso2.carbon.identity.application.authenticator.samlsso.manager.SAML2SSOManager;
import org.wso2.carbon.identity.application.authenticator.samlsso.model.StateInfo;
import org.wso2.carbon.identity.application.authenticator.samlsso.exception.SAMLSSOException;
import org.wso2.carbon.identity.application.authenticator.samlsso.util.SSOConstants;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.utils.CarbonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Scanner;

public class SAMLSSOAuthenticator extends AbstractApplicationAuthenticator implements FederatedApplicationAuthenticator {

	private static final long serialVersionUID = -8097512332218044859L;
	
	private static Log log = LogFactory.getLog(SAMLSSOAuthenticator.class);

	@Override
    public boolean canHandle(HttpServletRequest request) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside canHandle()");
		}
    	
        if (request.getParameter("SAMLResponse") != null) {
        	return true;
        }

        return false;
    }
    
    @Override
	protected void initiateAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context)
			throws AuthenticationFailedException {
		
		String idpURL = context.getAuthenticatorProperties().get(
                IdentityApplicationConstants.Authenticator.SAML2SSO.SSO_URL);
		String ssoUrl = "";

        try {
            if (getAuthenticatorConfig().getParameterMap() != null &&
                    Boolean.parseBoolean(getAuthenticatorConfig().getParameterMap().
                            get("HttpPostBindingEnabled"))) {

                sendPostAuthRequest(request, response, false, false, idpURL, context);
                return;

            } else {
                SAML2SSOManager saml2SSOManager = getSAML2SSOManagerInstance();
                saml2SSOManager.init(context.getTenantDomain(), context.getAuthenticatorProperties(),
                        context.getExternalIdP().getIdentityProvider());
                ssoUrl = saml2SSOManager.buildRequest(request, false, false, idpURL, context);
            }
        } catch (SAMLSSOException e) {
            throw new AuthenticationFailedException(e.getMessage(), e);
        }

        try {
            String domain = request.getParameter("domain");

            if (domain != null) {
                ssoUrl = ssoUrl + "&fidp=" + domain;
            }

            Map<String, String> authenticatorProperties = context
                    .getAuthenticatorProperties();


                if (authenticatorProperties != null) {
				String queryString = authenticatorProperties
						.get(FrameworkConstants.QUERY_PARAMS);
				if (queryString != null) {
					if (!queryString.startsWith("&")) {
						ssoUrl = ssoUrl + "&" + queryString;
					} else {
						ssoUrl = ssoUrl + queryString;
					}
				}
			}
			
	        response.sendRedirect(ssoUrl);
        } catch (IOException e) {
        	throw new AuthenticationFailedException(e.getMessage(), e);
        }
		return;
	}
    
    @Override
	protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context)
			throws AuthenticationFailedException {

    	try {
            SAML2SSOManager saml2SSOManager = getSAML2SSOManagerInstance();
            saml2SSOManager.init(context.getTenantDomain(), context.getAuthenticatorProperties(),
                    context.getExternalIdP().getIdentityProvider());
            saml2SSOManager.processResponse(request);
	        String subject = (String)request.getSession().getAttribute("username");
            Map<ClaimMapping, String> receivedClaims = (Map<ClaimMapping, String>) request
                    .getSession(false).getAttribute("samlssoAttributes");
	        
	        context.setSubjectAttributes(receivedClaims);
	        
	        Object sessionIndexObj = request.getSession(false).getAttribute(SSOConstants.IDP_SESSION);
			String sessionIndex = null;
			
			if (sessionIndexObj != null) {
				sessionIndex = (String)sessionIndexObj;
			}
			
			StateInfo stateInfoDO = new StateInfo();
			stateInfoDO.setSessionIndex(sessionIndex);
			stateInfoDO.setSubject(subject);
			context.setStateInfo(stateInfoDO);
			context.setSubject(subject);
        } catch (SAMLSSOException e) {
	        throw new AuthenticationFailedException(e.getMessage(), e);
        }
	}
    
	@Override
	public String getContextIdentifier(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getContextIdentifier()");
		}
		
		String identifier = request.getParameter("sessionDataKey");
		
		if (identifier == null) {
			identifier = request.getParameter("RelayState");
			
			if (identifier != null) {
				// TODO: SHOULD ensure that the value has not been tampered with by using a checksum, a pseudo-random value, or similar means.
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
	public String getFriendlyName() {
		return SSOConstants.AUTHENTICATOR_FRIENDLY_NAME;
	}

	@Override
	public String getName() {
		return SSOConstants.AUTHENTICATOR_NAME;
	}
	
	@Override
	protected void initiateLogoutRequest(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context)
			throws LogoutFailedException {
		
	    boolean logoutEnabled = false;
	    String logoutEnabledProp = context.getAuthenticatorProperties().get(
                IdentityApplicationConstants.Authenticator.SAML2SSO.IS_LOGOUT_ENABLED);
	    
	    if (logoutEnabledProp != null && "true".equalsIgnoreCase(logoutEnabledProp)) {
	        logoutEnabled = true;
	    }
	    
	    if (logoutEnabled) {
	      //send logout request to external idp
	        String idpLogoutURL = context.getAuthenticatorProperties().get(
	                IdentityApplicationConstants.Authenticator.SAML2SSO.LOGOUT_REQ_URL);
	        
            if (idpLogoutURL == null || idpLogoutURL.trim().length() == 0) {
                idpLogoutURL = context.getAuthenticatorProperties().get(
                        IdentityApplicationConstants.Authenticator.SAML2SSO.SSO_URL);
            }
            
            if (idpLogoutURL == null || idpLogoutURL.trim().length() == 0) {
                throw new LogoutFailedException(
                        "Logout is enabled for the IdP but Logout URL is not configured");
            }

	        AuthenticatorStateInfo stateInfo = context.getStateInfo();
	        
	        if (stateInfo instanceof StateInfo) {
	            request.getSession().setAttribute("logoutSessionIndex", ((StateInfo)stateInfo).getSessionIndex());
	            request.getSession().setAttribute("logoutUsername", ((StateInfo)stateInfo).getSubject());
	        }
	        
	        try {
	            SAML2SSOManager saml2SSOManager = getSAML2SSOManagerInstance();
                saml2SSOManager.init(context.getTenantDomain(), context
                        .getAuthenticatorProperties(), context.getExternalIdP()
                        .getIdentityProvider());
	            String logoutURL = saml2SSOManager.buildRequest(request, true, false, idpLogoutURL, context);
	            response.sendRedirect(logoutURL);
	        } catch (IOException e) {
	            throw new LogoutFailedException(e.getMessage(), e);
	        } catch (SAMLSSOException e) {
	            throw new LogoutFailedException(e.getMessage(), e);
            }
	    } else {
	        throw new UnsupportedOperationException();
	    }
	}
	
	@Override
	protected void processLogoutResponse(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context)
			throws LogoutFailedException {
	}

    private void sendPostAuthRequest(HttpServletRequest request, HttpServletResponse response,
                                     boolean isLogout, boolean isPassive,
                                     String loginPage, AuthenticationContext context) throws SAMLSSOException {

        SAML2SSOManager saml2SSOManager = getSAML2SSOManagerInstance();
        saml2SSOManager.init(context.getTenantDomain(), context.getAuthenticatorProperties(),
                context.getExternalIdP().getIdentityProvider());

        if (!(saml2SSOManager instanceof DefaultSAML2SSOManager)) {
            throw new SAMLSSOException("HTTP-POST is not supported");
        }

        String encodedRequest = ((DefaultSAML2SSOManager) saml2SSOManager).buildPostRequest(request,
                isLogout, isPassive, loginPage);

        String relayState = context.getContextIdentifier();

        try {
            if (getAuthenticatorConfig().getParameterMap() != null &&
                    Boolean.parseBoolean(getAuthenticatorConfig().getParameterMap().get("HttpPostRedirectPageEnabled"))) {

                String redirectHtmlPath = CarbonUtils.getCarbonHome() + File.separator + "repository"
                        + File.separator + "resources" + File.separator + "security" + File.separator + "sso_redirect.html";
                FileInputStream fis = new FileInputStream(new File(redirectHtmlPath));
                String ssoRedirectPage = new Scanner(fis, "UTF-8").useDelimiter("\\A").next();
                log.debug("sso_redirect.html " + ssoRedirectPage);

                if (ssoRedirectPage != null) {
                    String finalPage;

                    String pageWithAcs = ssoRedirectPage.replace("$acUrl", loginPage);
                    String pageWithAcsResponse = pageWithAcs.replace("$response", encodedRequest);
                    if (relayState != null) {
                        finalPage = pageWithAcsResponse.replace("$relayState", relayState);
                    } else {
                        finalPage = pageWithAcsResponse.replace("$relayState", "");
                    }

                    PrintWriter out = response.getWriter();
                    out.print(finalPage);

                    if (log.isDebugEnabled()) {
                        log.debug("sso_redirect.html " + finalPage);
                    }
                } else {
                    throw new SAMLSSOException("POST redirect page is not defined");
                }
            } else {
                PrintWriter out = response.getWriter();
                out.println("<html>");
                out.println("<body>");
                out.println("<p>You are now redirected back to " + loginPage);
                out.println(" If the redirection fails, please click the post button.</p>");
                out.println("<form method='post' action='" + loginPage + "'>");
                out.println("<p>");
                out.println("<input type='hidden' name='SAMLRequest' value='" + encodedRequest + "'>");
                if (relayState != null) {
                    out.println("<input type='hidden' name='RelayState' value='" + relayState + "'>");
                }
                out.println("<button type='submit'>POST</button>");
                out.println("</p>");
                out.println("</form>");
                out.println("<script type='text/javascript'>");
                out.println("document.forms[0].submit();");
                out.println("</script>");
                out.println("</body>");
                out.println("</html>");
            }
        } catch (Exception e) {
            throw new SAMLSSOException(e.getMessage(), e);
        }
    }

    private SAML2SSOManager getSAML2SSOManagerInstance() throws SAMLSSOException {

        String managerClassName = getAuthenticatorConfig().getParameterMap()
                .get(SSOConstants.ServerConfig.SAML2_SSO_MANAGER);
        if (managerClassName != null) {
            try {
                Class clazz = Class.forName(managerClassName);
                return (SAML2SSOManager) clazz.newInstance();
            } catch (ClassNotFoundException e) {
                throw new SAMLSSOException(e.getMessage(), e);
            } catch (InstantiationException e) {
                throw new SAMLSSOException(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new SAMLSSOException(e.getMessage(), e);
            }
        } else {
            return new DefaultSAML2SSOManager();
        }
    }
}
