/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.oauth2.model;

import org.apache.amber.oauth2.as.request.OAuthTokenRequest;
import org.apache.amber.oauth2.common.OAuth;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.utils.OAuthUtils;
import org.apache.amber.oauth2.common.validators.OAuthValidator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * CarbonOAuthTokenRequest holds all OAuth token request parameters.
 */
public class CarbonOAuthTokenRequest extends OAuthTokenRequest {
    private static final String ASSERTION = "assertion";
    private static final String CREDENTIAL_TYPE = "credentialType";
    private static final String WINDOWS_TOKEN = "windows_token";
    private static final String TENANT_DOMAIN = "tenantDomain";
    private static Log log = LogFactory.getLog(CarbonOAuthTokenRequest.class);
    private String assertion;
    private String credentialType;
    private String windows_token;
    private String tenantDomain;
    private RequestParameter[] requestParameters;

    /**
     * Constructs CarbonOAuthTokenRequest from the given HttpServletRequest
     *
     * @param request an instance of HttpServletRequest that represents an OAuth token request
     * @throws OAuthSystemException
     * @throws OAuthProblemException
     */
    public CarbonOAuthTokenRequest(HttpServletRequest request) throws OAuthSystemException,
            OAuthProblemException {

        super(request);
        assertion = request.getParameter(ASSERTION);
        credentialType = request.getParameter(CREDENTIAL_TYPE);
        tenantDomain = request.getParameter(TENANT_DOMAIN);
        if (tenantDomain == null){
            tenantDomain = "carbon.super";
        }
        windows_token = request.getParameter(WINDOWS_TOKEN);

        // Store all request parameters
        if (request.getParameterNames() != null) {
            List<RequestParameter> requestParameterList = new ArrayList<RequestParameter>();
            while (request.getParameterNames().hasMoreElements()) {
                String key = (String) request.getParameterNames().nextElement();
                String value = request.getParameter(key);
                requestParameterList.add(new RequestParameter(key, value));
            }
            requestParameters =
                    requestParameterList.toArray(new RequestParameter[requestParameterList.size()]);
        }
    }

    /**
     * Initialize a grant type validator
     *
     * @return an instance of OAuthValidator
     * @throws OAuthProblemException
     * @throws OAuthSystemException
     */
    @Override
    protected OAuthValidator<HttpServletRequest> initValidator()
            throws OAuthProblemException, OAuthSystemException {

        String requestTypeValue = getParam(OAuth.OAUTH_GRANT_TYPE);
        if (OAuthUtils.isEmpty(requestTypeValue)) {
            throw OAuthUtils.handleOAuthProblemException("Missing grant_type parameter value");
        }

        Class<? extends OAuthValidator<HttpServletRequest>> clazz = OAuthServerConfiguration
                .getInstance().getSupportedGrantTypeValidators().get(requestTypeValue);

        if (clazz == null) {
            if (log.isDebugEnabled()) {
                //Do not change this log format as these logs use by external applications
                log.debug("Unsupported Grant Type : " + requestTypeValue +
                        " for client id : " + getClientId());
            }
            throw OAuthUtils.handleOAuthProblemException("Invalid grant_type parameter value");
        }

        return OAuthUtils.instantiateClass(clazz);
    }

    /**
     * Returns the assertion
     *
     * @return assertion
     */
    public String getAssertion() {
        return assertion;
    }

    /**
     * Sets the assertion
     *
     * @param assertion assertion as a string
     */
    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    /**
     * Returns the credential type
     *
     * @return credential type
     */
    public String getCredentialType() {
        return credentialType;
    }

    /**
     * Sets the credential type
     *
     * @param credentialType credential type as a string
     */
    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    /**
     * Returns the windows token
     *
     * @return window token
     */
    public String getWindowsToken() {
        return windows_token;
    }

    /**
     * Sets window token
     *
     * @param windowsToken window token as a string
     */
    public void setWindowsToken(String windowsToken) {
        this.windows_token = windowsToken;
    }

    /**
     * Returns tenant domain
     *
     * @return tenant domain
     */
    public String getTenantDomain() {
        return tenantDomain;
    }

    /**
     * Sets tenant domain
     *
     * @param tenantDomain tenant domain as a string
     */
    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    /**
     * Get all request parameters as an array of RequestParameter objects
     *
     * @return array of RequestParameter objects
     */
    public RequestParameter[] getRequestParameters() {
        return requestParameters;
    }
}
