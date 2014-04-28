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

package org.wso2.carbon.identity.sso.agent;

import org.wso2.carbon.identity.sso.agent.bean.SSOAgentSessionBean;
import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.oauth2.SAML2GrantAccessTokenRequestor;
import org.wso2.carbon.identity.sso.agent.openid.OpenIDManager;
import org.wso2.carbon.identity.sso.agent.saml.SAML2SSOManager;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConfigs;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConstants;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet Filter implementation class SSOAgentFilter
 */
public class SSOAgentFilter implements Filter {

    private static Logger LOGGER = Logger.getLogger("InfoLogging");

    private SAML2SSOManager samlSSOManager;
    private OpenIDManager openIdManager;

    /**
     * @see Filter#init(FilterConfig)
     */
    public void init(FilterConfig fConfig) throws ServletException {
        try {
            //Initialize the configurations as 1st step.
            SSOAgentConfigs.initConfig(fConfig);
            SSOAgentConfigs.initCheck();
            // initialize ssomanager and openidmanager
            samlSSOManager = new SAML2SSOManager();
            openIdManager = new OpenIDManager();
 //           fConfig.getServletContext().addListener("org.wso2.carbon.identity.sso.agent.saml.SSOAgentHttpSessionListener");
        }  catch (SSOAgentException e) {
            LOGGER.log(Level.SEVERE, "An error has occurred", e);
            throw e;
        }
    }

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        try {

            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            // This should be SLO SAML Request from IdP
            String samlRequest = request.getParameter(SSOAgentConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ);

            // This could be either SAML Response for a SSO SAML Request by the client application or
            // a SAML Response for a SLO SAML Request from a SP
            String samlResponse = request.getParameter(SSOAgentConstants.HTTP_POST_PARAM_SAML2_RESP);

            String openid_mode = request.getParameter(SSOAgentConstants.OPENID_MODE);

            String claimed_id = request.getParameter(SSOAgentConfigs.getClaimedIdParameterName());

            if(SSOAgentConfigs.isSAMLSSOLoginEnabled() && samlRequest != null){
                samlSSOManager.doSLO(request);
            } else if(SSOAgentConfigs.isSAMLSSOLoginEnabled() && samlResponse != null){
                try {
                    samlSSOManager.processResponse(request);
                } catch (SSOAgentException e) {
                    if(request.getSession(false) != null){
                        request.getSession(false).removeAttribute(SSOAgentConfigs.getSessionBeanName());
                    }
                    throw e;
                }
            } else if(SSOAgentConfigs.isOpenIDLoginEnabled() && openid_mode != null &&
                    !openid_mode.equals("") && !openid_mode.equals("null")){
                try {
                    openIdManager.processOpenIDLoginResponse(request, response);
                } catch (SSOAgentException e){
                    if(request.getSession(false) != null){
                        request.getSession(false).removeAttribute(SSOAgentConfigs.getSessionBeanName());
                    }
                    throw e;
                }
            } else if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.isSLOEnabled() &&
                    request.getRequestURI().endsWith(SSOAgentConfigs.getLogoutUrl())){
                if(request.getSession(false) != null){
                    response.sendRedirect(samlSSOManager.buildRequest(request, true, false));
                    return;
                }
            } else if(SSOAgentConfigs.isSAMLSSOLoginEnabled() &&
                    request.getRequestURI().endsWith(SSOAgentConfigs.getSAMLSSOUrl())){
                response.sendRedirect(samlSSOManager.buildRequest(request, false, false));
                return;
            } else if(SSOAgentConfigs.isOpenIDLoginEnabled() &&
                    request.getRequestURI().endsWith(SSOAgentConfigs.getOpenIdUrl()) &&
                    claimed_id != null && !claimed_id.equals("") && !claimed_id.equals("null")){
                response.sendRedirect(openIdManager.doOpenIDLogin(request, response));
                return;
            } else if ((SSOAgentConfigs.isSAMLSSOLoginEnabled() || SSOAgentConfigs.isOpenIDLoginEnabled()) &&
                    !request.getRequestURI().endsWith(SSOAgentConfigs.getLoginUrl()) &&
                    (request.getSession(false) == null ||
                    request.getSession(false).getAttribute(SSOAgentConfigs.getSessionBeanName()) == null)) {
                response.sendRedirect(samlSSOManager.buildRequest(request, false, true));
                return;
            } else if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.isSAML2GrantEnabled() &&
                    request.getRequestURI().endsWith(SSOAgentConfigs.getSAML2GrantUrl()) &&
                    request.getSession(false) != null &&
                    request.getSession(false).getAttribute(SSOAgentConfigs.getSessionBeanName()) != null &&
                    ((SSOAgentSessionBean)request.getSession().getAttribute(
                            SSOAgentConfigs.getSessionBeanName())).getSAMLSSOSessionBean() != null &&
                    ((SSOAgentSessionBean)request.getSession(false).getAttribute(
                    SSOAgentConfigs.getSessionBeanName())).getSAMLSSOSessionBean().getSAMLAssertion() != null) {
                SAML2GrantAccessTokenRequestor.getAccessToken(request);
            }
            // pass the request along the filter chain
            chain.doFilter(request, response);

        } catch (SSOAgentException e){
            LOGGER.log(Level.SEVERE, "An error has occurred", e);
            throw e;
        }
	}


    /**
     * @see Filter#destroy()
     */
    public void destroy() {

    }

}
