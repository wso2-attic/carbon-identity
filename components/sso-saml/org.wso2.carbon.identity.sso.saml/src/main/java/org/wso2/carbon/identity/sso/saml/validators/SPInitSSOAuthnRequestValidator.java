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
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Subject;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;


public class SPInitSSOAuthnRequestValidator implements SSOAuthnRequestValidator{

    private static Log log = LogFactory.getLog(SPInitSSOAuthnRequestValidator.class);
    AuthnRequest authnReq;


    public SPInitSSOAuthnRequestValidator(AuthnRequest authnReq) throws IdentityException {
        this.authnReq = authnReq;
    }

    /**
     * Validates the authentication request according to SAML SSO Web Browser Specification
     *
     * @return SAMLSSOSignInResponseDTO
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    public SAMLSSOReqValidationResponseDTO validate() throws IdentityException {

        try {
            SAMLSSOReqValidationResponseDTO validationResponse = new SAMLSSOReqValidationResponseDTO();
            Issuer issuer = authnReq.getIssuer();
            Subject subject = authnReq.getSubject();

            // Validate the version
            if (!(SAMLVersion.VERSION_20.equals(authnReq.getVersion()))) {
                String errorResp = SAMLSSOUtil.buildErrorResponse(
                        SAMLSSOConstants.StatusCodes.VERSION_MISMATCH,
                        "Invalid SAML Version in Authentication Request. SAML Version should be equal to 2.0",
                        authnReq.getAssertionConsumerServiceURL());
                if (log.isDebugEnabled()) {
                    log.debug("Invalid version in the SAMLRequest" + authnReq.getVersion());
                }
                validationResponse.setResponse(errorResp);
                validationResponse.setValid(false);
                return validationResponse;
            }

            // Issuer MUST NOT be null
            if (StringUtils.isNotBlank(issuer.getValue())) {
                validationResponse.setIssuer(issuer.getValue());
            } else if (StringUtils.isNotBlank(issuer.getSPProvidedID())) {
                validationResponse.setIssuer(issuer.getSPProvidedID());
            } else {
                validationResponse.setValid(false);
                String errorResp = SAMLSSOUtil.buildErrorResponse(
                        SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                        "Issuer/ProviderName should not be empty in the Authentication Request.",
                        authnReq.getAssertionConsumerServiceURL());
                log.debug("SAML Request issuer validation failed. Issuer should not be empty");
                validationResponse.setResponse(errorResp);
                validationResponse.setValid(false);
                return validationResponse;
            }

            if (!SAMLSSOUtil.isSAMLIssuerExists(splitAppendedTenantDomain(validationResponse.getIssuer()),
                                                SAMLSSOUtil.getTenantDomainFromThreadLocal())) {
                String message = "A Service Provider with the Issuer '" + validationResponse.getIssuer()
                                 + "' is not registered. Service Provider should be registered in advance";
                log.error(message);
                String errorResp = SAMLSSOUtil.buildErrorResponse(SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                                                                  message, null);
                validationResponse.setResponse(errorResp);
                validationResponse.setValid(false);
                return validationResponse;
            }

            // Issuer Format attribute
            if ((StringUtils.isNotBlank(issuer.getFormat())) &&
                    !(issuer.getFormat().equals(SAMLSSOConstants.Attribute.ISSUER_FORMAT))) {
                validationResponse.setValid(false);
                String errorResp = SAMLSSOUtil.buildErrorResponse(
                        SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                        "Issuer Format attribute value is invalid",
                        authnReq.getAssertionConsumerServiceURL());
                if (log.isDebugEnabled()) {
                    log.debug("Invalid Issuer Format attribute value " + issuer.getFormat());
                }
                validationResponse.setResponse(errorResp);
                validationResponse.setValid(false);
                return validationResponse;
            }

            //TODO : REMOVE THIS UNNECESSARY CHECK
            // set the custom login page URL and ACS URL if available
            SSOServiceProviderConfigManager spConfigManager = SSOServiceProviderConfigManager.getInstance();
            SAMLSSOServiceProviderDO spDO = spConfigManager.getServiceProvider(issuer.getValue());
            String spAcsUrl = null;
            if (spDO != null) {
                validationResponse.setLoginPageURL(spDO.getLoginPageURL());
                spAcsUrl = spDO.getAssertionConsumerUrl();
            }
            // Check for a Spoofing attack
            String acsUrl = authnReq.getAssertionConsumerServiceURL();
            if ( StringUtils.isNotBlank(spAcsUrl) && StringUtils.isNotBlank(acsUrl) && !acsUrl.equals(spAcsUrl)) {
                log.error("Invalid ACS URL value " + acsUrl + " in the AuthnRequest message from " +
                        spDO.getIssuer() + "\n" +
                        "Possibly an attempt for a spoofing attack from Provider " +
                        authnReq.getIssuer().getValue());

                String errorResp = SAMLSSOUtil.buildErrorResponse(
                        SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                        "Invalid Assertion Consumer Service URL in the Authentication Request.",
                        acsUrl);
                validationResponse.setResponse(errorResp);
                validationResponse.setValid(false);
                return validationResponse;
            }

            //TODO : Validate the NameID Format
            if (subject != null && subject.getNameID() != null) {
                validationResponse.setSubject(subject.getNameID().getValue());
            }

            // subject confirmation should not exist
            if (subject != null && subject.getSubjectConfirmations() != null &&
                    !subject.getSubjectConfirmations().isEmpty()) {
                validationResponse.setValid(false);
                String errorResp = SAMLSSOUtil.buildErrorResponse(
                        SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                        "Subject Confirmation methods should NOT be in the request.",
                        authnReq.getAssertionConsumerServiceURL());
                if (log.isDebugEnabled()) {
                    log.debug("Invalid Request message. A Subject confirmation method found " +
                            subject.getSubjectConfirmations().get(0));
                }
                validationResponse.setResponse(errorResp);
                validationResponse.setValid(false);
                return validationResponse;
            }
            validationResponse.setId(authnReq.getID());
            validationResponse.setAssertionConsumerURL(authnReq.getAssertionConsumerServiceURL());
            validationResponse.setDestination(authnReq.getDestination());
            validationResponse.setValid(true);
            validationResponse.setPassive(authnReq.isPassive());
            validationResponse.setForceAuthn(authnReq.isForceAuthn());
            Integer index = authnReq.getAttributeConsumingServiceIndex();
            if (index !=null && !(index < 1)){              //according the spec, should be an unsigned short
                validationResponse.setAttributeConsumingServiceIndex(index);
            }
            if (log.isDebugEnabled()) {
                log.debug("Authentication Request Validation is successful..");
            }
            return validationResponse;
        } catch (Exception e) {
            throw IdentityException.error("Error validating the authentication request", e);
        }
    }

    protected String splitAppendedTenantDomain(String issuer) throws UserStoreException, IdentityException {

        if(IdentityUtil.isBlank(SAMLSSOUtil.getTenantDomainFromThreadLocal())) {
            if (issuer.contains("@")) {
                String tenantDomain = issuer.substring(issuer.lastIndexOf('@') + 1);
                issuer = issuer.substring(0, issuer.lastIndexOf('@'));
                if (StringUtils.isNotBlank(tenantDomain) && StringUtils.isNotBlank(issuer)) {
                    SAMLSSOUtil.setTenantDomainInThreadLocal(tenantDomain);
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant Domain: " + tenantDomain + " & Issuer name: " + issuer + "has been " +
                                "split");
                    }
                }
            }
        }
        if(IdentityUtil.isBlank(SAMLSSOUtil.getTenantDomainFromThreadLocal())){
            SAMLSSOUtil.setTenantDomainInThreadLocal(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        }
        return issuer;
    }

}
