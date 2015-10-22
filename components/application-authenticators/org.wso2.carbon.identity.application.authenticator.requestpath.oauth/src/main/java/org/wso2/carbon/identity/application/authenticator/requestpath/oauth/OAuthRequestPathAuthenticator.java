/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.application.authenticator.requestpath.oauth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.common.OAuth;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.RequestPathApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.oauth2.OAuth2TokenValidationService;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO.OAuth2AccessToken;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


public class OAuthRequestPathAuthenticator extends AbstractApplicationAuthenticator implements RequestPathApplicationAuthenticator {


    private static final long serialVersionUID = 634910519658720780L;
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_SCHEMA = "Bearer";
    private static final String AUTHENTICATOR_NAME = "OAuthRequestPathAuthenticator";
    private static Log log = LogFactory.getLog(OAuthRequestPathAuthenticator.class);

    @Override
    public boolean canHandle(HttpServletRequest request) {

        if (log.isTraceEnabled()) {
            log.trace("Inside canHandle()");
        }

        String headerValue = (String) request.getSession().getAttribute(AUTHORIZATION_HEADER_NAME);

        if (headerValue != null && !"".equals(headerValue.trim())) {
            String[] headerPart = headerValue.trim().split(" ");
            if (BEARER_SCHEMA.equals(headerPart[0])) {
                return true;
            }
        } else if (request.getParameter(OAuth.OAUTH_ACCESS_TOKEN) != null) {
            return true;
        }

        return false;
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        String headerValue = (String) request.getSession().getAttribute(AUTHORIZATION_HEADER_NAME);

        String token = null;
        if (headerValue != null) {
            token = headerValue.trim().split(" ")[1];
        } else {
            token = request.getParameter(OAuth.OAUTH_ACCESS_TOKEN);
        }


        try {
            OAuth2TokenValidationService validationService = new OAuth2TokenValidationService();
            OAuth2TokenValidationRequestDTO validationReqDTO = new OAuth2TokenValidationRequestDTO();
            OAuth2AccessToken accessToken = validationReqDTO.new OAuth2AccessToken();
            accessToken.setIdentifier(token);
            accessToken.setTokenType("bearer");
            validationReqDTO.setAccessToken(accessToken);
            OAuth2TokenValidationResponseDTO validationResponse = validationService.validate(validationReqDTO);

            if (!validationResponse.isValid()) {
                log.error("RequestPath OAuth authentication failed");
                throw new AuthenticationFailedException("Authentication Failed");
            }

            String user = validationResponse.getAuthorizedUser();
            String tenantDomain = MultitenantUtils.getTenantDomain(user);

            if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                user = MultitenantUtils.getTenantAwareUsername(user);
            }

            Map<String, Object> authProperties = context.getProperties();

            if (authProperties == null) {
                authProperties = new HashMap<String, Object>();
                context.setProperties(authProperties);
            }

            // TODO: user tenant domain has to be an attribute in the
            // AuthenticationContext
            authProperties.put("user-tenant-domain", tenantDomain);

            if (log.isDebugEnabled()) {
                log.debug("Authenticated user " + user);
            }

            context.setSubject(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier(user));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {
        return null;
    }

    @Override
    public String getFriendlyName() {
        return "oauth-bearer";
    }

    @Override
    public String getName() {
        return AUTHENTICATOR_NAME;
    }
}
