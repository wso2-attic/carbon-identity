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
package org.wso2.carbon.identity.relyingparty.ui.openid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.OpenIDException;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.Message;
import org.openid4java.message.ParameterList;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.relyingparty.stub.dto.ClaimDTO;
import org.wso2.carbon.identity.relyingparty.stub.dto.OpenIDDTO;
import org.wso2.carbon.identity.relyingparty.ui.openid.extensions.OpenIDExtension;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.CarbonUtils;

import com.google.step2.AuthRequestHelper;
import com.google.step2.AuthResponseHelper;
import com.google.step2.ConsumerHelper;
import com.google.step2.Step2.AxSchema;
import com.google.step2.discovery.IdpIdentifier;
import com.google.step2.openid.ui.UiMessageRequest;

public class OpenIDConsumer {

    private volatile static OpenIDConsumer consumer;
    private volatile static ConsumerHelper consumerHelper;
    private static Log log = LogFactory.getLog(OpenIDConsumer.class);

    /**
     * Creates an instance of OpenIDConsumer
     *
     * @throws RelyingPartyException
     */
    public OpenIDConsumer() throws IdentityException {
        try {
            ConsumerFactory factory = new ConsumerFactory(new InMemoryConsumerAssociationStore());
            consumerHelper = factory.getConsumerHelper();
        } catch (Exception e) {
            log.error("Relying Party initialization failed", e);
            // Present error to the user
            throw new IdentityException("Relying Party initialization failed", e);
        }

    }

    /**
     * @return An instance of OpenIDConsumer
     * @throws RelyingPartyException
     */
    public static OpenIDConsumer getInstance() throws IdentityException {
        // Double-checked locking to make sure thread safety
        if (consumer == null) {
            synchronized (OpenIDConsumer.class) {
                if (consumer == null) {
                    consumer = new OpenIDConsumer();
                }
            }
        }
        return consumer;
    }

    /**
     * Initiates OpenID authentication.
     *
     * @param request OpenID authentication request.
     */
    public String doOpenIDAuthentication(OpenIDAuthenticationRequest request)
            throws IdentityException {

        if (request == null)
            throw new IdentityException("Invalid OpenID authentication request");

        if (request.getReturnUrl() == null)
            request.setReturnUrl((String) request.getRequest().getParameter("returnUrl"));

        if (log.isDebugEnabled()) {
            log.debug("Return_to url :" + request.getReturnUrl());
        }

        return authRequest(request);
    }

    /**
     * @param request
     * @param openidResp
     * @param data
     * @return
     * @throws OpenIDException
     * @throws RelyingPartyException
     */
    public Message verifyOpenID(HttpServletRequest request, ParameterList openidResp, String receivingURL)
            throws OpenIDException, IdentityException {

        DiscoveryInformation discovered = null;
        String queryString = null;
        HttpSession session = null;

        session = request.getSession();

        // Retrieve the previously stored discovery information
        discovered = (DiscoveryInformation) session.getAttribute(IdentityConstants.OpenId.DISC);

        // Getting stored meta-data
        String cssLocation = request.getParameter("css");
        if ("null".equals(cssLocation)) {
            cssLocation = null;
        }

        String pageTitle = request.getParameter("title");
        String forwardPage = request.getParameter("forwardPage");
        
        if (receivingURL == null) {
            receivingURL = getAdminConsoleURL(request) + "relyingparty/openid_accept.jsp";
            if (cssLocation != null) {
                receivingURL = receivingURL + "?forwardPage=" + forwardPage + "&css=" + cssLocation
                    + "&title=" + pageTitle;
            }
        }

        queryString = request.getQueryString();

        if (log.isDebugEnabled()) {
            log.debug("OpenID receiving url from the reponse: " + receivingURL);
            log.debug("OpenID query string from the reponse: " + queryString);
        }

        AuthResponseHelper authResponse = consumerHelper.verify(receivingURL, openidResp,
                discovered);
        if (authResponse.getAuthResultType() == AuthResponseHelper.ResultType.AUTH_SUCCESS) {
            return authResponse.getAuthResponse();
        } else {
            log.error("OpenID verification failed");
            throw new IdentityException("OpenID verification failed");
        }

    }

    /**
     * Authenticates the OpenID url.
     *
     * @param request OpenID authentication request.
     * @throws RelyingPartyException
     */
    protected String authRequest(OpenIDAuthenticationRequest request) throws IdentityException {

        try {
            IdpIdentifier openId = new IdpIdentifier(request.getOpenIDUrl());

            AuthRequestHelper helper = consumerHelper.getAuthRequestHelper(openId,
                    request.getReturnUrl());
            addAttributes(helper, request);

            HttpSession session = request.getRequest().getSession();
            AuthRequest authReq = helper.generateRequest();

            UiMessageRequest uiExtension = new UiMessageRequest();
            uiExtension.setIconRequest(true);
            authReq.addExtension(uiExtension);

            session.setAttribute(IdentityConstants.OpenId.DISC, helper.getDiscoveryInformation());

            if (request.getRealm() != null && request.getRealm().trim().length() != 0) {
                authReq.setRealm(request.getRealm());
            }

            for (String type : request.getRequestTypes()) {
                authReq.addExtension(OpenIDExtensionFactory.getInstance().getExtension(type)
                        .getMessageExtension(request));
            }

            // Redirect to the OpenID provider server for authentication.
            String value = authReq.getDestinationUrl(true);
            return value;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // Present error to the user
            throw new IdentityException(e.getMessage(), e);
        }
    }

    /**
     * Adds the requested AX attributes to the request
     *
     * @param helper Request builder
     */
    void addAttributes(AuthRequestHelper helper, OpenIDAuthenticationRequest request) {
        if (request.getRequestClaimsFromIdP()) {
            List<String> claims = request.getRequiredClaimURIs();
            Iterator<String> ite = claims.iterator();
            while (ite.hasNext()) {
                helper.requestAxAttribute(AxSchema.ofTypeUri(ite.next()), true);
            }
        }
    }

    /**
     * @param request
     * @return
     * @throws IdentityException
     */
    public OpenIDDTO validateOpenIDAuthentication(HttpServletRequest request, String requestURL)
            throws IdentityException {

        ParameterList response = null;
        AuthSuccess authSuccess = null;
        String mode = null;
        OpenIDExtension extension = null;
        List<ClaimDTO> claimList = null;
        OpenIDDTO openID = null;

        try {
            // Extract the parameters from the authentication response
            // (which comes in as a HTTP request from the OpenID provider)
            response = new ParameterList(request.getParameterMap());

            mode = response.getParameterValue(IdentityConstants.OpenId.ATTR_MODE);

            if (mode != null && IdentityConstants.OpenId.CANCEL.equals(mode)) {
                // User has denied sending his profile info :(
                throw new IdentityException("User has denied sending his profile info");
            }

            authSuccess = (AuthSuccess) verifyOpenID(request, response, requestURL);

            claimList = new ArrayList<ClaimDTO>();

            for (Object alias : authSuccess.getExtensions()) {
                extension = OpenIDExtensionFactory.getInstance().getExtension((String) alias,
                        authSuccess);
                if (extension != null)
                    extension.setSessionAttributes(claimList);
            }

            openID = new OpenIDDTO();
            openID.setOpenID(authSuccess.getIdentity());
            openID.setClaims(claimList.toArray(new ClaimDTO[claimList.size()]));

            return openID;

        } catch (OpenIDException e) {
            log.error(e.getMessage());
            // Present error to the user.
            throw new IdentityException("OpenID authentication failed", e);
        }
    }

    public String getAdminConsoleURL(HttpServletRequest request) {
        //Sometime context comes as /carbon in that case admin console URL becomes /carbon/carbon
        //hence we need to check if context == /carbon and if so we need to set it to /
        //@nuwan
        String webContextRoot = CarbonUtils.getServerConfiguration().getFirstProperty("WebContextRoot");
        return CarbonUIUtil.getAdminConsoleURL(webContextRoot);
    }
}