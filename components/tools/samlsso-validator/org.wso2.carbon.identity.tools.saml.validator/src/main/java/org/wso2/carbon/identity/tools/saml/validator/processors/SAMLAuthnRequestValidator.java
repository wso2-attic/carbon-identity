/*
 * Copyright 2005-2014 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.tools.saml.validator.processors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Subject;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.identity.tools.saml.validator.dto.ValidatedItemDTO;
import org.wso2.carbon.identity.tools.saml.validator.util.SAMLValidatorConstants;
import org.wso2.carbon.identity.tools.saml.validator.util.SAMLValidatorUtil;

import java.util.List;

public class SAMLAuthnRequestValidator {

    private static Log log = LogFactory.getLog(SAMLAuthnRequestValidator.class);
    private AuthnRequest authnRequest;
    private boolean isPost = false;
    private String queryString = null;
    private String issuerStr = null;

    public SAMLAuthnRequestValidator(AuthnRequest authnRequest) {
        this.setAuthnRequest(authnRequest);
    }

    /**
     * Validate SP initiated SAML AuthnRequest
     *
     * @param validatedItems
     * @throws IdentityException
     */
    public void validate(List<ValidatedItemDTO> validatedItems) throws IdentityException {
        // Validate version - version must be SAML 2.0
        if (authnRequest.getVersion().equals(SAMLVersion.VERSION_20)) {
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_VERSION,
                    true,
                    SAMLValidatorConstants.ValidationMessage.VAL_VERSION_SUCCESS));
        } else {
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_VERSION,
                    false,
                    String.format(SAMLValidatorConstants.ValidationMessage.VAL_VERSION_FAIL,
                            authnRequest.getVersion())));
            throw IdentityException.error(SAMLValidatorConstants.ValidationMessage.EXIT_WITH_ERROR);
        }

        Issuer issuer = authnRequest.getIssuer();
        Subject subject = authnRequest.getSubject();

        // Validate Issuer/ProviderName - at least one should not be null
        if (issuer.getValue() == null && issuer.getSPProvidedID() == null) {
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_ISSUER,
                    false,
                    SAMLValidatorConstants.ValidationMessage.VAL_ISSUER_FAIL));
            throw IdentityException.error(SAMLValidatorConstants.ValidationMessage.EXIT_WITH_ERROR);
        } else {
            issuerStr = issuer.getValue() != null ? issuer.getValue() : issuer.getSPProvidedID();
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_ISSUER,
                    true,
                    SAMLValidatorConstants.ValidationMessage.VAL_ISSUER_SUCCESS));
        }

        if (issuer.getFormat() != null) {
            if (issuer.getFormat().equals(SAMLValidatorConstants.Attribute.ISSUER_FORMAT)) {
                validatedItems.add(new ValidatedItemDTO(
                        SAMLValidatorConstants.ValidationType.VAL_ISSUER_FORMAT,
                        true,
                        SAMLValidatorConstants.ValidationMessage.VAL_ISSUER_FMT_SUCCESS));
            } else {
                validatedItems.add(new ValidatedItemDTO(
                        SAMLValidatorConstants.ValidationType.VAL_ISSUER_FORMAT,
                        false,
                        SAMLValidatorConstants.ValidationMessage.VAL_ISSUER_FMT_FAIL));
                throw IdentityException.error(
                        SAMLValidatorConstants.ValidationMessage.EXIT_WITH_ERROR);
            }
        }

        // Load SSO IdP configuration for issuer
        SAMLSSOServiceProviderDO ssoIdPConfigs = null;
        try {
            ssoIdPConfigs = SAMLValidatorUtil.getServiceProviderConfig(issuer.getValue());
        } catch (IdentityException e) {
            log.error(e.getMessage());
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_IDP_CONFIGS,
                    false,
                    String.format(SAMLValidatorConstants.ValidationMessage.VAL_IDP_CONFIGS_FAIL,
                            authnRequest.getIssuer()
                                    .getValue())));
            throw IdentityException.error(SAMLValidatorConstants.ValidationMessage.EXIT_WITH_ERROR);
        }
        if (ssoIdPConfigs == null) {
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_IDP_CONFIGS,
                    false,
                    String.format(SAMLValidatorConstants.ValidationMessage.VAL_IDP_CONFIGS_FAIL,
                            authnRequest.getIssuer()
                                    .getValue())));
            throw IdentityException.error(SAMLValidatorConstants.ValidationMessage.EXIT_WITH_ERROR);
        } else {
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_IDP_CONFIGS,
                    true,
                    String.format(SAMLValidatorConstants.ValidationMessage.VAL_IDP_CONFIGS_SUCCESS,
                            authnRequest.getIssuer()
                                    .getValue())));
        }

        // Validating Assertion Consumer URL
        String consumerServiceURL = authnRequest.getAssertionConsumerServiceURL();
        if (consumerServiceURL != null &&
                ssoIdPConfigs.getAssertionConsumerUrl().equals(consumerServiceURL)) {
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_CONSUM_URL,
                    true,
                    String.format(SAMLValidatorConstants.ValidationMessage.VAL_CONSUM_URL_SUCCESS,
                            consumerServiceURL)));
        } else {
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_CONSUM_URL,
                    false,
                    String.format(SAMLValidatorConstants.ValidationMessage.VAL_CONSUM_URL_FAIL,
                            consumerServiceURL,
                            ssoIdPConfigs.getAssertionConsumerUrl())));
            throw IdentityException.error(SAMLValidatorConstants.ValidationMessage.EXIT_WITH_ERROR);
        }

        // Validating SubjectID format
        if (subject != null && subject.getNameID() != null &&
                subject.getNameID().getFormat() != null && ssoIdPConfigs.getNameIDFormat() != null &&
                subject.getNameID().getFormat().equals(ssoIdPConfigs.getNameIDFormat())) {
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_SUB_NAMEID_FMT,
                    true,
                    SAMLValidatorConstants.ValidationMessage.VAL_SUB_NAMEID_SUCCESS));
        }

        // Subject Confirmation methods should NOT be available
        if (subject != null && subject.getSubjectConfirmations() != null) {
            validatedItems.add(new ValidatedItemDTO(
                    SAMLValidatorConstants.ValidationType.VAL_SUB_CONF_MTHD,
                    false,
                    SAMLValidatorConstants.ValidationMessage.VAL_SUB_CONF_MTHD_FAIL));
        }

        if (ssoIdPConfigs.isDoValidateSignatureInRequests()) {

            // Validate Destination
            String idPUrl = IdentityUtil.getProperty(IdentityConstants.ServerConfig.SSO_IDP_URL);
            if(StringUtils.isBlank(idPUrl)) {
                idPUrl = IdentityUtil.getServerURL(SAMLSSOConstants.SAMLSSO_URL, true, true);
            }

            if (authnRequest.getDestination() != null &&
                    idPUrl.equals(authnRequest.getDestination())) {
                validatedItems.add(new ValidatedItemDTO(
                        SAMLValidatorConstants.ValidationType.VAL_DESTINATION,
                        true,
                        String.format(SAMLValidatorConstants.ValidationMessage.VAL_DESTINATION_SUCCESS,
                                authnRequest.getDestination())));
            } else {
                validatedItems.add(new ValidatedItemDTO(
                        SAMLValidatorConstants.ValidationType.VAL_DESTINATION,
                        false,
                        String.format(SAMLValidatorConstants.ValidationMessage.VAL_DESTINATION_FAIL,
                                authnRequest.getDestination(),
                                idPUrl)));
                throw IdentityException.error(
                        SAMLValidatorConstants.ValidationMessage.EXIT_WITH_ERROR);
            }

            // Validate Signature
            String alias = ssoIdPConfigs.getCertAlias();
            String domainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            try {
                boolean isValid = false;

                if (isPost) {
                    isValid =
                            SAMLSSOUtil.validateXMLSignature((RequestAbstractType) authnRequest,
                                    alias, domainName);
                } else {
                    isValid =
                            SAMLSSOUtil.validateDeflateSignature(queryString, issuerStr, alias,
                                    domainName);
                }

                if (isValid) {
                    validatedItems.add(new ValidatedItemDTO(
                            SAMLValidatorConstants.ValidationType.VAL_SIGNATURE,
                            true,
                            SAMLValidatorConstants.ValidationMessage.VAL_SIGNATURE_SUCCESS));
                } else {
                    validatedItems.add(new ValidatedItemDTO(
                            SAMLValidatorConstants.ValidationType.VAL_SIGNATURE,
                            false,
                            SAMLValidatorConstants.ValidationMessage.VAL_SIGNATURE_FAIL));
                }
            } catch (IdentityException e) {
                validatedItems.add(new ValidatedItemDTO(
                        SAMLValidatorConstants.ValidationType.VAL_SIGNATURE,
                        false,
                        String.format(SAMLValidatorConstants.ValidationMessage.VAL_SIGNATURE_ERROR,
                                e.getMessage())));
                throw IdentityException.error(
                        SAMLValidatorConstants.ValidationMessage.EXIT_WITH_ERROR);
            }
        }
    }

    public AuthnRequest getAuthnRequest() {
        return authnRequest;
    }

    public void setAuthnRequest(AuthnRequest authnRequest) {
        this.authnRequest = authnRequest;
    }

    public void setPost(boolean isPost) {
        this.isPost = isPost;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
}
