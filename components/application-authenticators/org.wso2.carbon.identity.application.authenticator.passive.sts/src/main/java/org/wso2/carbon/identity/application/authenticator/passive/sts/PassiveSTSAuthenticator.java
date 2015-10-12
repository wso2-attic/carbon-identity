/*
 * Copyright (c) 2005, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authenticator.passive.sts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authenticator.passive.sts.exception.PassiveSTSException;
import org.wso2.carbon.identity.application.authenticator.passive.sts.manager.PassiveSTSManager;
import org.wso2.carbon.identity.application.authenticator.passive.sts.util.PassiveSTSConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

public class PassiveSTSAuthenticator extends AbstractApplicationAuthenticator {

    private static final long serialVersionUID = -8097512332218044090L;

    private static Log log = LogFactory.getLog(PassiveSTSAuthenticator.class);

    @Override
    public boolean canHandle(HttpServletRequest request) {

        if (log.isTraceEnabled()) {
            log.trace("Inside canHandle()");
        }

        if (request.getParameter(PassiveSTSConstants.HTTP_PARAM_PASSIVE_STS_RESULT) != null) {
            return true;
        }

        return false;
    }

    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {


        ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
        String idpURL = context.getAuthenticatorProperties().get(IdentityApplicationConstants.Authenticator.PassiveSTS.IDENTITY_PROVIDER_URL);
        String loginPage;

        try {
            loginPage = new PassiveSTSManager(externalIdPConfig).buildRequest(request, idpURL, context.getContextIdentifier(), context.getAuthenticatorProperties());
        } catch (PassiveSTSException e) {
            log.error("Exception while building the WS-Federation request", e);
            throw new AuthenticationFailedException(e.getMessage(), e);
        }

        try {
            String domain = request.getParameter("domain");

            if (domain != null) {
                loginPage = loginPage + "&fidp=" + domain;
            }

            Map<String, String> authenticatorProperties = context
                    .getAuthenticatorProperties();

            if (authenticatorProperties != null) {
                String queryString = authenticatorProperties
                        .get(FrameworkConstants.QUERY_PARAMS);
                if (queryString != null) {
                    if (!queryString.startsWith("&")) {
                        loginPage = loginPage + "&" + queryString;
                    } else {
                        loginPage = loginPage + queryString;
                    }
                }
            }

            response.sendRedirect(loginPage);
        } catch (IOException e) {
            log.error("Exception while sending to the login page", e);
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
        return;
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        ExternalIdPConfig externalIdPConfig = context.getExternalIdP();

        if (request.getParameter(PassiveSTSConstants.HTTP_PARAM_PASSIVE_STS_RESULT) != null) {
            try {
                new PassiveSTSManager(externalIdPConfig).processResponse(request, context);
            } catch (PassiveSTSException e) {
                log.error("Exception while processing WS-Federation response", e);
                throw new AuthenticationFailedException(e.getMessage(), e);
            }
        } else {
            log.error("wresult can not be found in request");
            throw new AuthenticationFailedException("wresult can not be found in request");
        }

    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {

        if (log.isTraceEnabled()) {
            log.trace("Inside getContextIdentifier()");
        }

        String identifier = request.getParameter("sessionDataKey");

        if (identifier == null) {
            identifier = request.getParameter("wctx");

            if (identifier != null) {
                // TODO SHOULD ensure that the value has not been tampered with by using a checksum, a pseudo-random value, or similar means.
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
        return "passivests";
    }

    @Override
    public String getName() {
        return PassiveSTSConstants.AUTHENTICATOR_NAME;
    }
}
