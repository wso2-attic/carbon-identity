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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.saml2.core.Audience;
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml2.core.impl.AttributeBuilder;
import org.opensaml.saml2.core.impl.AttributeStatementBuilder;
import org.opensaml.saml2.core.impl.AudienceBuilder;
import org.opensaml.saml2.core.impl.AudienceRestrictionBuilder;
import org.opensaml.saml2.core.impl.AuthnContextBuilder;
import org.opensaml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml2.core.impl.AuthnStatementBuilder;
import org.opensaml.saml2.core.impl.ConditionsBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.core.impl.StatusMessageBuilder;
import org.opensaml.saml2.core.impl.SubjectBuilder;
import org.opensaml.saml2.core.impl.SubjectConfirmationBuilder;
import org.opensaml.saml2.core.impl.SubjectConfirmationDataBuilder;
import org.opensaml.xml.encryption.EncryptionConstants;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.builders.SignKeyDataHolder;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.identity.tools.saml.validator.util.SAMLValidatorUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.Iterator;
import java.util.Map;

public class SAMLResponseBuilder {

    private static Log log = LogFactory.getLog(SAMLResponseBuilder.class);

    /**
     * Build SAML response using IdP configuration & user name
     *
     * @param ssoIdPConfigs
     * @param userName
     * @return SAML Response object
     * @throws IdentityException
     */
    public Response buildSAMLResponse(SAMLSSOServiceProviderDO ssoIdPConfigs, String userName)
            throws IdentityException {
        if (log.isDebugEnabled()) {
            log.debug("Building SAML Response for the consumer '" +
                    ssoIdPConfigs.getAssertionConsumerUrl() + "'");
        }
        Response response = new org.opensaml.saml2.core.impl.ResponseBuilder().buildObject();
        response.setIssuer(SAMLSSOUtil.getIssuer());
        response.setID(SAMLSSOUtil.createID());
        response.setDestination(ssoIdPConfigs.getAssertionConsumerUrl());
        response.setStatus(buildStatus(SAMLSSOConstants.StatusCodes.SUCCESS_CODE, null));
        response.setVersion(SAMLVersion.VERSION_20);
        DateTime issueInstant = new DateTime();
        DateTime notOnOrAfter =
                new DateTime(issueInstant.getMillis() +
                        SAMLSSOUtil.getSAMLResponseValidityPeriod() * 60 *
                                1000);
        response.setIssueInstant(issueInstant);
        Assertion assertion = buildSAMLAssertion(ssoIdPConfigs, notOnOrAfter, userName);
        if (ssoIdPConfigs.isDoEnableEncryptedAssertion()) {
            String domainName = MultitenantUtils.getTenantDomain(userName);
            String alias = ssoIdPConfigs.getCertAlias();
            if (alias != null) {
                EncryptedAssertion encryptedAssertion =
                        SAMLSSOUtil.setEncryptedAssertion(assertion,
                                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256,
                                alias,
                                domainName);
                response.getEncryptedAssertions().add(encryptedAssertion);
            }
        } else {
            response.getAssertions().add(assertion);
        }
        if (ssoIdPConfigs.isDoSignResponse()) {
            SAMLSSOUtil.setSignature(response, ssoIdPConfigs.getSigningAlgorithmUri(), ssoIdPConfigs
                    .getDigestAlgorithmUri(), new SignKeyDataHolder(userName));
        }
        return response;
    }

    /**
     * Build SAML assertion
     *
     * @param ssoIdPConfigs
     * @param notOnOrAfter
     * @param userName
     * @return Assertion object
     * @throws IdentityException
     */
    private Assertion buildSAMLAssertion(SAMLSSOServiceProviderDO ssoIdPConfigs,
                                         DateTime notOnOrAfter, String userName)
            throws IdentityException {
        DateTime currentTime = new DateTime();
        Assertion samlAssertion = new AssertionBuilder().buildObject();
        samlAssertion.setID(SAMLSSOUtil.createID());
        samlAssertion.setVersion(SAMLVersion.VERSION_20);
        samlAssertion.setIssuer(SAMLSSOUtil.getIssuer());
        samlAssertion.setIssueInstant(currentTime);
        Subject subject = new SubjectBuilder().buildObject();
        NameID nameId = new NameIDBuilder().buildObject();
        String claimValue = null;

        if (ssoIdPConfigs.getNameIdClaimUri() != null) {
            Map<String, String> claims =
                    SAMLValidatorUtil.getUserClaimValues(userName,
                            new String[]{ssoIdPConfigs.getNameIdClaimUri()},
                            null);
            claimValue = claims.get(ssoIdPConfigs.getNameIdClaimUri());
            nameId.setValue(claimValue);
        }

        if (claimValue == null) {
            nameId.setValue(userName);
        }

        if (ssoIdPConfigs.getNameIDFormat() != null) {
            nameId.setFormat(ssoIdPConfigs.getNameIDFormat());
        } else {
            nameId.setFormat(NameIdentifier.EMAIL);
        }

        subject.setNameID(nameId);

        SubjectConfirmation subjectConfirmation = new SubjectConfirmationBuilder().buildObject();
        subjectConfirmation.setMethod(SAMLSSOConstants.SUBJECT_CONFIRM_BEARER);

        SubjectConfirmationData subjectConfirmationData =
                new SubjectConfirmationDataBuilder().buildObject();
        subjectConfirmationData.setRecipient(ssoIdPConfigs.getAssertionConsumerUrl());
        subjectConfirmationData.setNotOnOrAfter(notOnOrAfter);

        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        samlAssertion.setSubject(subject);

        AuthnStatement authStmt = new AuthnStatementBuilder().buildObject();
        authStmt.setAuthnInstant(new DateTime());

        AuthnContext authContext = new AuthnContextBuilder().buildObject();
        AuthnContextClassRef authCtxClassRef = new AuthnContextClassRefBuilder().buildObject();
        authCtxClassRef.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);
        authContext.setAuthnContextClassRef(authCtxClassRef);
        authStmt.setAuthnContext(authContext);
        samlAssertion.getAuthnStatements().add(authStmt);

        Map<String, String> claims = getAttributes(ssoIdPConfigs, userName);
        if (claims != null) {
            samlAssertion.getAttributeStatements().add(buildAttributeStatement(claims));
        }

        AudienceRestriction audienceRestriction = new AudienceRestrictionBuilder().buildObject();
        Audience issuerAudience = new AudienceBuilder().buildObject();
        issuerAudience.setAudienceURI(ssoIdPConfigs.getIssuer());
        audienceRestriction.getAudiences().add(issuerAudience);
        if (ssoIdPConfigs.getRequestedAudiences() != null) {
            for (String requestedAudience : ssoIdPConfigs.getRequestedAudiences()) {
                Audience audience = new AudienceBuilder().buildObject();
                audience.setAudienceURI(requestedAudience);
                audienceRestriction.getAudiences().add(audience);
            }
        }

        Conditions conditions = new ConditionsBuilder().buildObject();
        conditions.setNotBefore(currentTime);
        conditions.setNotOnOrAfter(notOnOrAfter);
        conditions.getAudienceRestrictions().add(audienceRestriction);
        samlAssertion.setConditions(conditions);

        if (ssoIdPConfigs.isDoSignAssertions()) {
            SAMLSSOUtil.setSignature(samlAssertion, ssoIdPConfigs.getSigningAlgorithmUri(), ssoIdPConfigs
                    .getDigestAlgorithmUri(), new SignKeyDataHolder(userName));
        }

        return samlAssertion;
    }

    /**
     * Get attributes
     *
     * @param ssoIdPConfigs
     * @param userName
     * @return attributes
     * @throws IdentityException
     */
    private Map<String, String> getAttributes(SAMLSSOServiceProviderDO ssoIdPConfigs,
                                              String userName) throws IdentityException {
        String[] requestedClaims = ssoIdPConfigs.getRequestedClaims();

        if (requestedClaims == null) {
            return null;
        }
        return SAMLValidatorUtil.getUserClaimValues(userName, requestedClaims, null);
    }

    /**
     * Get status
     *
     * @param status
     * @param statMsg
     * @return Status object
     */
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

    /**
     * Build Attribute Statement
     *
     * @param claims
     * @return AttributeStatement
     */
    private AttributeStatement buildAttributeStatement(Map<String, String> claims) {
        AttributeStatement attStmt = null;
        if (claims != null) {
            attStmt = new AttributeStatementBuilder().buildObject();
            Iterator<String> ite = claims.keySet().iterator();

            for (int i = 0; i < claims.size(); i++) {
                Attribute attrib = new AttributeBuilder().buildObject();
                String claimUri = ite.next();
                attrib.setName(claimUri);
                // look
                // https://wiki.shibboleth.net/confluence/display/OpenSAML/OSTwoUsrManJavaAnyTypes
                XSStringBuilder stringBuilder =
                        (XSStringBuilder) Configuration.getBuilderFactory()
                                .getBuilder(XSString.TYPE_NAME);
                XSString stringValue =
                        stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME,
                                XSString.TYPE_NAME);
                stringValue.setValue(claims.get(claimUri));
                attrib.getAttributeValues().add(stringValue);
                attStmt.getAttributes().add(attrib);
            }
        }
        return attStmt;
    }

}
