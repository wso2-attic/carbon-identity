/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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
package org.wso2.carbon.identity.sso.saml.validators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class IdPInitSSOAuthnRequestValidator {

    private static Log log = LogFactory.getLog(IdPInitSSOAuthnRequestValidator.class);

    private String spEntityID;


    public IdPInitSSOAuthnRequestValidator(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                           String spEntityID, String relayState) throws IdentityException {
        this.spEntityID = spEntityID;
    }

    /**
     * Validates the authentication request according to IdP Initiated SAML SSO Web Browser Specification
     *
     * @return SAMLSSOSignInResponseDTO
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    public SAMLSSOReqValidationResponseDTO validate() throws IdentityException {

        SAMLSSOReqValidationResponseDTO validationResponse = new SAMLSSOReqValidationResponseDTO();
        try {

            // spEntityID MUST NOT be null
            if (spEntityID != null) {
                validationResponse.setIssuer(spEntityID);
            } else {
                validationResponse.setValid(false);
                String errorResp = SAMLSSOUtil.buildErrorResponse(
                        SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                        "spEntityID parameter not found in request",
                        null);
                log.debug("spEntityID parameter not found in request");
                validationResponse.setResponse(errorResp);
                validationResponse.setValid(false);
                return validationResponse;
            }

            SSOServiceProviderConfigManager spConfigManager = SSOServiceProviderConfigManager.getInstance();
            SAMLSSOServiceProviderDO spDO = spConfigManager.getServiceProvider(spEntityID);
            String spAcsUrl = null;
            if (spDO != null) {
                spAcsUrl = spDO.getAssertionConsumerUrl();
            }
            validationResponse.setAssertionConsumerURL(spAcsUrl);
            validationResponse.setValid(true);

            if (log.isDebugEnabled()) {
                log.debug("IdP Initiated SSO request validation is successful..");
            }
            return validationResponse;
        } catch (Exception e) {
            throw new IdentityException("Error validating the IdP Initiated SSO request", e);
        }
    }

}
