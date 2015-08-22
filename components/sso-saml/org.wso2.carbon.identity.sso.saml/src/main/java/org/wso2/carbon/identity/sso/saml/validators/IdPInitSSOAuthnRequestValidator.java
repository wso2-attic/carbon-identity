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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.dto.QueryParamDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;


public class IdPInitSSOAuthnRequestValidator {

    private static Log log = LogFactory.getLog(IdPInitSSOAuthnRequestValidator.class);

    private String spEntityID;
    private String acs;


    public IdPInitSSOAuthnRequestValidator(QueryParamDTO[] queryParamDTOs, String relayState) throws IdentityException {

        init(queryParamDTOs);
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
            if (StringUtils.isNotBlank(spEntityID)) {
                validationResponse.setIssuer(spEntityID);
            } else {
                validationResponse.setValid(false);
                String errorResp = SAMLSSOUtil.buildErrorResponse(SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                        "spEntityID parameter not found in request", null);
                log.debug("spEntityID parameter not found in request");
                validationResponse.setResponse(errorResp);
                validationResponse.setValid(false);
                return validationResponse;
            }

            // If SP has multiple ACS
            if (StringUtils.isNotBlank(acs)) {
                validationResponse.setAssertionConsumerURL(acs);
            }

            validationResponse.setValid(true);

            if (log.isDebugEnabled()) {
                log.debug("IdP Initiated SSO request validation is successful");
            }
            return validationResponse;
        } catch (Exception e) {
            throw new IdentityException("Error validating the IdP Initiated SSO request", e);
        }
    }

    private void init(QueryParamDTO[] queryParamDTOs) {

        for (QueryParamDTO queryParamDTO : queryParamDTOs) {
            if (SAMLSSOConstants.QueryParameter.SP_ENTITY_ID.toString().equals(queryParamDTO.getKey())) {
                this.spEntityID = queryParamDTO.getValue();
            } else if (SAMLSSOConstants.QueryParameter.ACS.toString().equals(queryParamDTO.getKey())) {
                this.acs = queryParamDTO.getValue();
            }
        }
    }

}
