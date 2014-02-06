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
package org.wso2.carbon.identity.sso.saml.validators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.Response;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.builders.ErrorResponseBuilder;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;


public class IdPInitSSOAuthnRequestValidator {

    private static Log log = LogFactory.getLog(IdPInitSSOAuthnRequestValidator.class);

    private String spEntityID;
    private String relayState;


    public IdPInitSSOAuthnRequestValidator(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                           String spEntityID, String relayState) throws IdentityException {
        this.spEntityID = spEntityID;
        this.relayState = relayState;
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
                String errorResp =
                        buildErrorResponse(SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                                "spEntityID parameter not found in request");
                log.debug("spEntityID parameter not found in request");
                validationResponse.setResponse(errorResp);
                validationResponse.setValid(false);
                return validationResponse;
            }

            SSOServiceProviderConfigManager spConfigManager = SSOServiceProviderConfigManager.getInstance();
            SAMLSSOServiceProviderDO spDO = spConfigManager.getServiceProvider(spEntityID);
            String spAcsUrl = null;
            if(spDO != null){
                spAcsUrl = spDO.getAssertionConsumerUrl();
            }

            if (relayState != null && !relayState.equals("")) {
                validationResponse.setAssertionConsumerURL(relayState);
            } else {
                if(spAcsUrl != null){
                    log.debug("RelayState parameter not found in IdP Initiated SSO request. " +
                            "Using " + spAcsUrl + " as default Assertion Consumer URL for " + spEntityID);
                    validationResponse.setAssertionConsumerURL(spAcsUrl);
                }
//              else {
//                    String errorMsg = "RelayState parameter not found in IdP Initiated SSO request and " +
//                            "Assertion Consumer URL not found in service provider configuration for " + spEntityID + ". " +
//                            "Cannot proceed with request";
//                    log.debug(errorMsg);
//                    String errorResp = buildErrorResponse(SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, errorMsg);
//                    validationResponse.setResponse(errorResp);
//                }
            }

            validationResponse.setValid(true);

            if (log.isDebugEnabled()) {
                log.debug("IdP Initiated SSO request validation is successful..");
            }
            return validationResponse;
        } catch (Exception e) {
            throw new IdentityException("Error validating the IdP Initiated SSO request", e);
        }
    }

    /**
     * build the error response
     * @param status
     * @param message
     * @return decoded response
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    private String buildErrorResponse(String status, String message) throws Exception {
        ErrorResponseBuilder respBuilder = new ErrorResponseBuilder();
        List<String> statusCodeList = new ArrayList<String>();
        statusCodeList.add(status);
        Response response = respBuilder.buildResponse(null, statusCodeList, message);
        return SAMLSSOUtil.encode(SAMLSSOUtil.marshall(response));
    }
}
