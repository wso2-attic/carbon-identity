/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.carbon.identity.oauth.common;

import org.apache.amber.oauth2.as.request.OAuthTokenRequest;

import org.apache.amber.oauth2.as.validator.*;

import org.apache.amber.oauth2.common.OAuth;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.amber.oauth2.common.utils.OAuthUtils;
import org.apache.amber.oauth2.common.validators.OAuthValidator;

import javax.servlet.http.HttpServletRequest;

public class CarbonOAuthTokenRequest extends OAuthTokenRequest {

    private String assertion;

    private String credentialType;
    private String windows_token;

    private String idp;

    public CarbonOAuthTokenRequest(HttpServletRequest request) throws OAuthSystemException, OAuthProblemException {
        super(request);
        assertion = request.getParameter("assertion");
        credentialType = request.getParameter("credentialType");
        idp = request.getParameter("idp");
        windows_token = request.getParameter("windows_token");
    }

    @Override
    protected OAuthValidator<HttpServletRequest> initValidator() throws OAuthProblemException, OAuthSystemException {
        validators.put(GrantType.PASSWORD.toString(), PasswordValidator.class);
        validators.put(GrantType.CLIENT_CREDENTIALS.toString(), ClientCredentialValidator.class);
        validators.put(GrantType.AUTHORIZATION_CODE.toString(), AuthorizationCodeValidator.class);
        validators.put(GrantType.REFRESH_TOKEN.toString(), RefreshTokenValidator.class);
        validators.put(org.wso2.carbon.identity.oauth.common.GrantType.SAML20_BEARER.toString(), SAML2GrantValidator.class);
        validators.put(org.wso2.carbon.identity.oauth.common.GrantType.IWA_NTLM.toString(), NTLMAuthenticationValidator.class);
        String requestTypeValue = getParam(OAuth.OAUTH_GRANT_TYPE);
        if (OAuthUtils.isEmpty(requestTypeValue)) {
            throw OAuthUtils.handleOAuthProblemException("Missing grant_type parameter value");
        }
        Class<? extends OAuthValidator<HttpServletRequest>> clazz = validators.get(requestTypeValue);
        if (clazz == null) {
            throw OAuthUtils.handleOAuthProblemException("Invalid grant_type parameter value");
        }
        return OAuthUtils.instantiateClass(clazz);
    }

    public String getAssertion() {
        return assertion;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    public String getWindowsToken() {
        return windows_token;
    }

    public void setWindowsToken(String windowsToken) {
        this.windows_token = windowsToken;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getIdP() {
        return idp;
    }

    public void setIdP(String idp) {
        this.idp = idp;
    }

}
