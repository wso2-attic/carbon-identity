/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.sso.saml.builders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.signature.XMLSignature;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.*;
import org.opensaml.xml.encryption.EncryptionConstants;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.Iterator;
import java.util.Map;

public class DefaultResponseBuilder implements ResponseBuilder{

    private static Log log = LogFactory.getLog(DefaultResponseBuilder.class);

    static {
        SAMLSSOUtil.doBootstrap();
    }

    public Response buildResponse(SAMLSSOAuthnReqDTO authReqDTO, String sessionId)
            throws IdentityException {

        if (log.isDebugEnabled()) {
            log.debug("Building SAML Response for the consumer '"
                    + authReqDTO.getAssertionConsumerURL() + "'");
        }
        Response response = new org.opensaml.saml2.core.impl.ResponseBuilder().buildObject();
        response.setIssuer(SAMLSSOUtil.getIssuer());
        response.setID(SAMLSSOUtil.createID());
        if(!authReqDTO.isIdPInitSSO()){
            response.setInResponseTo(authReqDTO.getId());
        }
        response.setDestination(authReqDTO.getAssertionConsumerURL());
        response.setStatus(buildStatus(SAMLSSOConstants.StatusCodes.SUCCESS_CODE, null));
        response.setVersion(SAMLVersion.VERSION_20);
        DateTime issueInstant = new DateTime();
        DateTime notOnOrAfter = new DateTime(issueInstant.getMillis()
                + SAMLSSOUtil.getSAMLResponseValidityPeriod() * 60 * 1000);
        response.setIssueInstant(issueInstant);
        Assertion assertion = buildSAMLAssertion(authReqDTO, notOnOrAfter, sessionId);
        if (authReqDTO.isDoEnableEncryptedAssertion()) {

            String domainName = MultitenantUtils.getTenantDomain(authReqDTO.getUsername());
            String alias = authReqDTO.getCertAlias();
            if(alias != null)   {
            EncryptedAssertion encryptedAssertion = SAMLSSOUtil.setEncryptedAssertion(assertion,
                    EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256, alias, domainName);
            response.getEncryptedAssertions().add(encryptedAssertion);
            }
        } else {
            response.getAssertions().add(assertion);
        }

        if (authReqDTO.isDoSignResponse()) {
            SAMLSSOUtil.setSignature(response, XMLSignature.ALGO_ID_SIGNATURE_RSA,
                    new SignKeyDataHolder(authReqDTO.getUsername()));
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
			SAMLSSOUtil.setSignature(response, XMLSignature.ALGO_ID_SIGNATURE_RSA,
					new SignKeyDataHolder(authReqDTO.getUsername()));
		}
		return response;
	}

    private Assertion buildSAMLAssertion(SAMLSSOAuthnReqDTO authReqDTO, DateTime notOnOrAfter,
                                         String sessionId) throws IdentityException {
        try {
            DateTime currentTime = new DateTime();
            Assertion samlAssertion = new AssertionBuilder().buildObject();
            samlAssertion.setID(SAMLSSOUtil.createID());
            samlAssertion.setVersion(SAMLVersion.VERSION_20);
            samlAssertion.setIssuer(SAMLSSOUtil.getIssuer());
            samlAssertion.setIssueInstant(currentTime);
            Subject subject = new SubjectBuilder().buildObject();

            NameID nameId = new NameIDBuilder().buildObject();

            if (authReqDTO.getUseFullyQualifiedUsernameAsSubject()) {
                nameId.setValue(authReqDTO.getUsername());
                if (authReqDTO.getNameIDFormat() != null) {
                    nameId.setFormat(authReqDTO.getNameIDFormat());
                } else {
                    nameId.setFormat(NameIdentifier.EMAIL);
                }
            } else {
                nameId.setValue(MultitenantUtils.getTenantAwareUsername(authReqDTO
                        .getUsername()));
                nameId.setFormat(authReqDTO.getNameIDFormat());
            }

            subject.setNameID(nameId);

            SubjectConfirmation subjectConfirmation = new SubjectConfirmationBuilder()
                    .buildObject();
            subjectConfirmation.setMethod(SAMLSSOConstants.SUBJECT_CONFIRM_BEARER);
            SubjectConfirmationData scData = new SubjectConfirmationDataBuilder().buildObject();
            scData.setRecipient(authReqDTO.getAssertionConsumerURL());
            scData.setNotOnOrAfter(notOnOrAfter);
            if(!authReqDTO.isIdPInitSSO()){
                scData.setInResponseTo(authReqDTO.getId());
            }
            subjectConfirmation.setSubjectConfirmationData(scData);
            subject.getSubjectConfirmations().add(subjectConfirmation);

            if(authReqDTO.getRequestedRecipients() != null && authReqDTO.getRequestedRecipients().length > 0){
                for(String recipient : authReqDTO.getRequestedRecipients()){
                    subjectConfirmation = new SubjectConfirmationBuilder()
                            .buildObject();
                    subjectConfirmation.setMethod(SAMLSSOConstants.SUBJECT_CONFIRM_BEARER);
                    scData = new SubjectConfirmationDataBuilder().buildObject();
                    scData.setRecipient(recipient);
                    scData.setNotOnOrAfter(notOnOrAfter);
                    if(!authReqDTO.isIdPInitSSO()){
                        scData.setInResponseTo(authReqDTO.getId());
                    }
                    subjectConfirmation.setSubjectConfirmationData(scData);
                    subject.getSubjectConfirmations().add(subjectConfirmation);
                }
            }

            samlAssertion.setSubject(subject);

            AuthnStatement authStmt = new AuthnStatementBuilder().buildObject();
            authStmt.setAuthnInstant(new DateTime());

            AuthnContext authContext = new AuthnContextBuilder().buildObject();
            AuthnContextClassRef authCtxClassRef = new AuthnContextClassRefBuilder().buildObject();
            authCtxClassRef.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);
            authContext.setAuthnContextClassRef(authCtxClassRef);
            authStmt.setAuthnContext(authContext);
            if (authReqDTO.isDoSingleLogout()) {
                authStmt.setSessionIndex(sessionId);
            }
            samlAssertion.getAuthnStatements().add(authStmt);

			/*
			 * If <AttributeConsumingServiceIndex> element is in the <AuthnRequest> and according to
			 * the spec 2.0 the subject MUST be in the assertion
			 */
            Map<String, String> claims = SAMLSSOUtil.getAttributes(authReqDTO);
            if (claims != null) {
                samlAssertion.getAttributeStatements().add(buildAttributeStatement(claims));
            }

            AudienceRestriction audienceRestriction = new AudienceRestrictionBuilder()
                    .buildObject();
            Audience issuerAudience = new AudienceBuilder().buildObject();
            issuerAudience.setAudienceURI(authReqDTO.getIssuerWithDomain());
            audienceRestriction.getAudiences().add(issuerAudience);
            if (authReqDTO.getRequestedAudiences() != null) {
                for (String requestedAudience : authReqDTO.getRequestedAudiences()) {
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

            if (authReqDTO.getDoSignAssertions()) {
                SAMLSSOUtil.setSignature(samlAssertion, XMLSignature.ALGO_ID_SIGNATURE_RSA,
                        new SignKeyDataHolder(authReqDTO.getUsername()));
            }

            return samlAssertion;
        } catch (Exception e) {
            log.error("Error when reading claim values for generating SAML Response", e);
            throw new IdentityException(
                    "Error when reading claim values for generating SAML Response", e);
        }
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

    private AttributeStatement buildAttributeStatement(Map<String, String> claims) {
        AttributeStatement attStmt = null;
        if (claims != null) {
            attStmt = new AttributeStatementBuilder().buildObject();
            Iterator<String> ite = claims.keySet().iterator();

            for (int i = 0; i < claims.size(); i++) {
                Attribute attrib = new AttributeBuilder().buildObject();
                String claimUri = ite.next();
                attrib.setName(claimUri);
		//setting NAMEFORMAT attribute value to basic attribute profile
                attrib.setNameFormat(SAMLSSOConstants.NAME_FORMAT_BASIC);
                // look
                // https://wiki.shibboleth.net/confluence/display/OpenSAML/OSTwoUsrManJavaAnyTypes
                XSStringBuilder stringBuilder = (XSStringBuilder) Configuration.getBuilderFactory()
                        .getBuilder(XSString.TYPE_NAME);
                XSString stringValue = stringBuilder.buildObject(
                        AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
                stringValue.setValue(claims.get(claimUri));
                attrib.getAttributeValues().add(stringValue);
                attStmt.getAttributes().add(attrib);
            }
        }
        return attStmt;
    }

}
