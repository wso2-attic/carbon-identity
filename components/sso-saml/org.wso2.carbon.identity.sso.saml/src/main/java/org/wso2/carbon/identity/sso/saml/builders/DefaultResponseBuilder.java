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
package org.wso2.carbon.identity.sso.saml.builders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.core.impl.StatusMessageBuilder;
import org.opensaml.xml.encryption.EncryptionConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

public class DefaultResponseBuilder implements ResponseBuilder {

    private static Log log = LogFactory.getLog(DefaultResponseBuilder.class);

    static {
        SAMLSSOUtil.doBootstrap();
    }

    @Override
    public Response buildResponse(SAMLSSOAuthnReqDTO authReqDTO, String sessionId)
            throws IdentityException {

        if (log.isDebugEnabled()) {
            log.debug("Building SAML Response for the consumer '"
                    + authReqDTO.getAssertionConsumerURL() + "'");
        }
        Response response = new org.opensaml.saml2.core.impl.ResponseBuilder().buildObject();
        response.setIssuer(SAMLSSOUtil.getIssuer());
        response.setID(SAMLSSOUtil.createID());
        if (!authReqDTO.isIdPInitSSOEnabled()) {
            response.setInResponseTo(authReqDTO.getId());
        }
        response.setDestination(authReqDTO.getAssertionConsumerURL());
        response.setStatus(buildStatus(SAMLSSOConstants.StatusCodes.SUCCESS_CODE, null));
        response.setVersion(SAMLVersion.VERSION_20);
        DateTime issueInstant = new DateTime();
        DateTime notOnOrAfter = new DateTime(issueInstant.getMillis()
                + SAMLSSOUtil.getSAMLResponseValidityPeriod() * 60 * 1000L);
        response.setIssueInstant(issueInstant);
        Assertion assertion = SAMLSSOUtil.buildSAMLAssertion(authReqDTO, notOnOrAfter, sessionId);

        if (authReqDTO.isDoEnableEncryptedAssertion()) {

            String domainName = authReqDTO.getTenantDomain();
            String alias = authReqDTO.getCertAlias();
            if (alias != null) {
                EncryptedAssertion encryptedAssertion = SAMLSSOUtil.setEncryptedAssertion(assertion,
                        EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256, alias, domainName);
                response.getEncryptedAssertions().add(encryptedAssertion);
            }
        } else {
            response.getAssertions().add(assertion);
        }

        if (authReqDTO.isDoSignResponse()) {
            SAMLSSOUtil.setSignature(response, authReqDTO.getSigningAlgorithmUri(), authReqDTO.getDigestAlgorithmUri
                    (), new SignKeyDataHolder(authReqDTO.getUser().getAuthenticatedSubjectIdentifier()));
        }
        return response;
    }

    public Response buildResponse(SAMLSSOAuthnReqDTO authReqDTO, Assertion assertion)
            throws IdentityException {

        if (log.isDebugEnabled()) {
            log.debug("Building SAML Response for the consumer '"
                    + authReqDTO.getAssertionConsumerURL() + "'");
        }
        Response response = new org.opensaml.saml2.core.impl.ResponseBuilder().buildObject();
        response.setIssuer(SAMLSSOUtil.getIssuer());
        response.setID(SAMLSSOUtil.createID());
        response.setInResponseTo(authReqDTO.getId());
        response.setDestination(authReqDTO.getAssertionConsumerURL());
        response.setStatus(buildStatus(SAMLSSOConstants.StatusCodes.SUCCESS_CODE, null));
        response.setVersion(SAMLVersion.VERSION_20);
        DateTime issueInstant = new DateTime();
        response.setIssueInstant(issueInstant);
        response.getAssertions().add(assertion);
        if (authReqDTO.isDoSignResponse()) {
            SAMLSSOUtil.setSignature(response, authReqDTO.getSigningAlgorithmUri(), authReqDTO.getDigestAlgorithmUri
                    (), new SignKeyDataHolder(authReqDTO.getUser().getAuthenticatedSubjectIdentifier()));
        }
        return response;
    }

    private Status buildStatus(String status, String statMsg) {

        Status stat = new StatusBuilder().buildObject();

        // Set the status code
        StatusCode statCode = new StatusCodeBuilder().buildObject();
        statCode.setValue(status);
        stat.setStatusCode(statCode);

        // Set the status Message
        if (statMsg != null) {
            StatusMessage statMesssage = new StatusMessageBuilder().buildObject();
            statMesssage.setMessage(statMsg);
            stat.setStatusMessage(statMesssage);
        }

        return stat;
    }

}
