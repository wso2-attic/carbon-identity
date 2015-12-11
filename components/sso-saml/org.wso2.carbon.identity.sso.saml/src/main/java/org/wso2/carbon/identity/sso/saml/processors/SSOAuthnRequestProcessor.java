/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.sso.saml.processors;

import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSORespDTO;

/**
 * Interface to implement in order to process a received SAML SSO authentication request.
 * Implementation class should be defined in <>identity.xml</> under <>SSOService</> element as
 * <IdPInitSSOAuthnRequestProcessor></IdPInitSSOAuthnRequestProcessor> for IDP initiated authentication request flow,
 * <SPInitSSOAuthnRequestProcessor></SPInitSSOAuthnRequestProcessor> for SP initiated authentication request flow.
 */
public interface SSOAuthnRequestProcessor {

    /**
     * Processes the authentication request according to SAML SSO Web Browser Specification
     *
     * @return SAMLSSOSignInResponseDTO : includes validation outputs
     * @throws IdentityException
     */
    SAMLSSORespDTO process(SAMLSSOAuthnReqDTO authnReqDTO, String sessionId,
                           boolean isAuthenticated, String authenticators, String authMode) throws Exception;
}
