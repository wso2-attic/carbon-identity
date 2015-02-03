/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth2.token.handlers.grant.saml;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.*;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.internal.OAuthComponentServiceHolder;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AbstractAuthorizationGrantHandler;
import org.wso2.carbon.identity.oauth2.util.CarbonEntityResolver;
import org.wso2.carbon.identity.oauth2.util.X509CredentialImpl;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This implements SAML 2.0 Bearer Assertion Profile for OAuth 2.0 -
 * http://tools.ietf.org/html/draft-ietf-oauth-saml2-bearer-14.
 */
public class SAML2BearerGrantHandler extends AbstractAuthorizationGrantHandler {

    private static Log log = LogFactory.getLog(SAML2BearerGrantHandler.class);

    SAMLSignatureProfileValidator profileValidator = null;

    public void init() throws IdentityOAuth2Exception {

        super.init();

        Thread thread = Thread.currentThread();
        ClassLoader loader = thread.getContextClassLoader();
        thread.setContextClassLoader(this.getClass().getClassLoader());

        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            log.error(e.getMessage(),e);
            throw new IdentityOAuth2Exception("Error in bootstrapping the OpenSAML2 library");
        } finally {
            thread.setContextClassLoader(loader);
        }

        profileValidator =  new SAMLSignatureProfileValidator();
    }

    /**
     * We're validating the SAML token that we receive from the request. Through the assertion parameter is the POST
     * request. A request format that we handle here looks like,
     * <p/>
     * POST /token.oauth2 HTTP/1.1
     * Host: as.example.com
     * Content-Type: application/x-www-form-urlencoded
     * <p/>
     * grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Asaml2-bearer&
     * assertion=PHNhbWxwOl...[omitted for brevity]...ZT4
     *
     * @param tokReqMsgCtx Token message request context
     * @return true if validation is successful, false otherwise
     * @throws org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception
     */
    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        super.validateGrant(tokReqMsgCtx);

        Assertion assertion = null;
        IdentityProvider identityProvider = null;
        String tokenEndpointAlias = null;
        String tenantDomain = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getTenantDomain();
        if (tenantDomain == null || "".equals(tenantDomain)) {
            tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        }

        // Logging the SAML token
        if (log.isDebugEnabled()) {
            log.debug("Received SAML assertion : " +
                    new String(Base64.decodeBase64(tokReqMsgCtx.getOauth2AccessTokenReqDTO().getAssertion()))
            );
        }


        try {
            XMLObject samlObject = unmarshall(new String(Base64.decodeBase64(tokReqMsgCtx.getOauth2AccessTokenReqDTO().getAssertion())));
            assertion = (Assertion) samlObject;
        } catch (IdentityOAuth2Exception e) {
            // fault in the saml token
            log.error(e.getMessage(), e);
            return false;
        }

        if (assertion == null) {
            log.debug("Assertion is null, cannot continue");
            return false;
        }

        /**
         * The Assertion MUST contain a <Subject> element.  The subject MAY identify the resource owner for whom
         * the access token is being requested.  For client authentication, the Subject MUST be the "client_id"
         * of the OAuth client.  When using an Assertion as an authorization grant, the Subject SHOULD identify
         * an authorized accessor for whom the access token is being requested (typically the resource owner, or
         * an authorized delegate).  Additional information identifying the subject/principal of the transaction
         * MAY be included in an <AttributeStatement>.
         */
        if (assertion.getSubject() != null) {
            String resourceOwnerUserName = assertion.getSubject().getNameID().getValue();
            if (resourceOwnerUserName == null || resourceOwnerUserName.equals("")) {
                log.debug("NameID in Assertion cannot be empty");
                return false;
            }
            tokReqMsgCtx.setAuthorizedUser(resourceOwnerUserName);
        } else {
            log.debug("Cannot find a Subject in the Assertion");
            return false;
        }

        if (assertion.getIssuer() == null || assertion.getIssuer().getValue().equals("")) {
            log.debug("Issuer is empty in the SAML assertion");
            return false;
        } else {
            try {
				identityProvider = IdentityProviderManager.getInstance().getIdPByAuthenticatorPropertyValue("IdPEntityId",
				                   assertion.getIssuer().getValue(), tenantDomain, false);
				// IF Federated IDP not found get the resident IDP and check,
				// resident IDP entitiID == issuer
				if (identityProvider != null) {
					if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(identityProvider.getIdentityProviderName())) {
						identityProvider = IdentityProviderManager.getInstance().getResidentIdP(tenantDomain);

						FederatedAuthenticatorConfig[] fedAuthnConfigs = identityProvider.getFederatedAuthenticatorConfigs();
						String idpEntityId = null;

						// Get SAML authenticator
						FederatedAuthenticatorConfig samlAuthenticatorConfig = IdentityApplicationManagementUtil.getFederatedAuthenticator(fedAuthnConfigs,
						                                                                                                                   IdentityApplicationConstants.Authenticator.SAML2SSO.NAME);
						// Get Entity ID from SAML authenticator
						Property samlProperty = IdentityApplicationManagementUtil.getProperty(samlAuthenticatorConfig.getProperties(),
						                                                                      IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID);
						if (samlProperty != null) {
							idpEntityId = samlProperty.getValue();
						}

						if (idpEntityId == null || !assertion.getIssuer().getValue().equals(idpEntityId)) {
							log.debug("SAML Token Issuer verification failed or Issuer not registered");
							return false;
						}

						// Get OpenIDConnect authenticator == OAuth
						// authenticator
						FederatedAuthenticatorConfig oauthAuthenticatorConfig = IdentityApplicationManagementUtil.getFederatedAuthenticator(fedAuthnConfigs,
						                                                                                                                    IdentityApplicationConstants.Authenticator.OIDC.NAME);
						// Get OAuth token endpoint
						Property oauthProperty = IdentityApplicationManagementUtil.getProperty(oauthAuthenticatorConfig.getProperties(),
						                                                                       IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_TOKEN_URL);
						if (oauthProperty != null) {
							tokenEndpointAlias = oauthProperty.getValue();
						}
					} else {
						// Get Alias from Federated IDP
						tokenEndpointAlias = identityProvider.getAlias();
					}
				} else {
					log.debug("SAML Token Issuer verification failed or Issuer not registered");
					return false;
				}
            } catch (IdentityApplicationManagementException e) {
                log.debug("Error while getting Federated Identity Provider ");
            }
        }

        /**
         * The Assertion MUST contain <Conditions> element with an <AudienceRestriction> element with an <Audience>
         * element containing a URI reference that identifies the authorization server, or the service provider
         * SAML entity of its controlling domain, as an intended audience.  The token endpoint URL of the
         * authorization server MAY be used as an acceptable value for an <Audience> element.  The authorization
         * server MUST verify that it is an intended audience for the Assertion.
         */

        if(tokenEndpointAlias == null || tokenEndpointAlias.equals("")){
            String errorMsg = "Token Endpoint alias of the local Identity Provider has not been " +
                    "configured for " + identityProvider.getIdentityProviderName();
            log.debug(errorMsg);
            return false;
        }

        Conditions conditions = assertion.getConditions();
        if (conditions != null) {
            List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
            if (audienceRestrictions != null && !audienceRestrictions.isEmpty()) {
                boolean audienceFound = false;
                for (AudienceRestriction audienceRestriction : audienceRestrictions) {
                    if (audienceRestriction.getAudiences() != null && audienceRestriction.getAudiences().size() > 0) {
                        for(Audience audience: audienceRestriction.getAudiences()){
                            if(audience.getAudienceURI().equals(tokenEndpointAlias)){
                                audienceFound = true;
                                break;
                            }
                        }
                    }
                    if(audienceFound){
                        break;
                    }
                }
                if(!audienceFound){
                    if(log.isDebugEnabled()){
                        String message = "SAML Assertion Audience Restriction validation failed";
                         log.debug(message);
                    }
                    return false;
                }
            } else {
                if(log.isDebugEnabled()){
                    String message = "SAML Assertion doesn't contain AudienceRestrictions";
                    log.debug(message);
                }
                return false;
            }
        } else {
            if(log.isDebugEnabled()){
                String message = "SAML Assertion doesn't contain Conditions";
                log.debug(message);
            }
            return false;
        }


        /**
         * The Assertion MUST have an expiry that limits the time window during which it can be used.  The expiry
         * can be expressed either as the NotOnOrAfter attribute of the <Conditions> element or as the NotOnOrAfter
         * attribute of a suitable <SubjectConfirmationData> element.
         */

        /**
         * The <Subject> element MUST contain at least one <SubjectConfirmation> element that allows the
         * authorization server to confirm it as a Bearer Assertion.  Such a <SubjectConfirmation> element MUST
         * have a Method attribute with a value of "urn:oasis:names:tc:SAML:2.0:cm:bearer".  The
         * <SubjectConfirmation> element MUST contain a <SubjectConfirmationData> element, unless the Assertion
         * has a suitable NotOnOrAfter attribute on the <Conditions> element, in which case the
         * <SubjectConfirmationData> element MAY be omitted. When present, the <SubjectConfirmationData> element
         * MUST have a Recipient attribute with a value indicating the token endpoint URL of the authorization
         * server (or an acceptable alias).  The authorization server MUST verify that the value of the Recipient
         * attribute matches the token endpoint URL (or an acceptable alias) to which the Assertion was delivered.
         * The <SubjectConfirmationData> element MUST have a NotOnOrAfter attribute that limits the window during
         * which the Assertion can be confirmed.  The <SubjectConfirmationData> element MAY also contain an Address
         * attribute limiting the client address from which the Assertion can be delivered.  Verification of the
         * Address is at the discretion of the authorization server.
         */

        DateTime notOnOrAfterFromConditions = null;
        Set<DateTime> notOnOrAfterFromSubjectConfirmations = new HashSet<DateTime>();
        boolean bearerFound = false;
        ArrayList<String> recipientURLS = new ArrayList<String>();

        if (assertion.getConditions() != null && assertion.getConditions().getNotOnOrAfter() != null) {
            notOnOrAfterFromConditions = assertion.getConditions().getNotOnOrAfter();
        }

        List<SubjectConfirmation> subjectConfirmations = assertion.getSubject().getSubjectConfirmations();
        if (subjectConfirmations != null && !subjectConfirmations.isEmpty()) {
            for (SubjectConfirmation s : subjectConfirmations) {
                if(s.getMethod() != null){
                    if (s.getMethod().equals(OAuthConstants.OAUTH_SAML2_BEARER_METHOD)) {
                        bearerFound = true;
                    }
                } else {
                    log.debug("Cannot find Method attribute in SubjectConfirmation " + s.toString());
                    return false;
                }

                if(s.getSubjectConfirmationData() != null) {
                    if(s.getSubjectConfirmationData().getRecipient() != null){
                        recipientURLS.add(s.getSubjectConfirmationData().getRecipient());
                    }
                    if(s.getSubjectConfirmationData().getNotOnOrAfter() != null){
                        notOnOrAfterFromSubjectConfirmations.add(s.getSubjectConfirmationData().getNotOnOrAfter());
                    } else {
                        log.debug("Cannot find NotOnOrAfter attribute in SubjectConfirmationData " +
                                s.getSubjectConfirmationData().toString());
                       return false;
                    }
                } else if (s.getSubjectConfirmationData() == null && notOnOrAfterFromConditions == null) {
                    log.debug("Neither can find NotOnOrAfter attribute in Conditions nor SubjectConfirmationData" +
                            "in SubjectConfirmation " + s.toString());
                    return false;
                }
            }
        } else {
            log.debug("No SubjectConfirmation exist in Assertion");
            return false;
        }

        if (!bearerFound) {
            log.debug("Failed to find a SubjectConfirmation with a Method attribute having : " +
                    OAuthConstants.OAUTH_SAML2_BEARER_METHOD);
            return false;
        }

        if(recipientURLS.size() > 0){
            if(!recipientURLS.contains(tokenEndpointAlias)){
                log.debug("None of the recipient URLs match the token endpoint or an acceptable alias");
                return false;
            }
        }

        /**
         * The authorization server MUST verify that the NotOnOrAfter instant has not passed, subject to allowable
         * clock skew between systems.  An invalid NotOnOrAfter instant on the <Conditions> element invalidates
         * the entire Assertion.  An invalid NotOnOrAfter instant on a <SubjectConfirmationData> element only
         * invalidates the individual <SubjectConfirmation>.  The authorization server MAY reject Assertions with
         * a NotOnOrAfter instant that is unreasonably far in the future.  The authorization server MAY ensure
         * that Bearer Assertions are not replayed, by maintaining the set of used ID values for the length of
         * time for which the Assertion would be considered valid based on the applicable NotOnOrAfter instant.
         */
        if (notOnOrAfterFromConditions != null && notOnOrAfterFromConditions.compareTo(new DateTime()) < 1) {
            // notOnOrAfter is an expired timestamp
            log.debug("NotOnOrAfter is having an expired timestamp in Conditions element");
            return false;
        }
        boolean validSubjectConfirmationDataExists = false;
        if(!notOnOrAfterFromSubjectConfirmations.isEmpty()){
            for(DateTime entry : notOnOrAfterFromSubjectConfirmations){
                if(entry.compareTo(new DateTime()) >= 1){
                    validSubjectConfirmationDataExists = true;
                }
            }
        }
        if(notOnOrAfterFromConditions == null && !validSubjectConfirmationDataExists){
            log.debug("No valid NotOnOrAfter element found in SubjectConfirmations");
            return false;
        }

        /**
         * The Assertion MUST be digitally signed by the issuer and the authorization server MUST verify the
         * signature.
         */

        try {
            profileValidator.validate(assertion.getSignature());
        } catch (ValidationException e) {
            // Indicates signature did not conform to SAML Signature profile
            log.debug(e.getMessage());
            return false;
        }

        X509Certificate x509Certificate = null;
        try {
            x509Certificate = (X509Certificate)IdentityApplicationManagementUtil
                    .decodeCertificate(identityProvider.getCertificate());
        } catch (CertificateException e) {
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error occurred while decoding public certificate of Identity Provider "
                    + identityProvider.getIdentityProviderName() + " for tenant domain " + tenantDomain);
        }

        try {
            X509Credential x509Credential = new X509CredentialImpl(x509Certificate);
            SignatureValidator signatureValidator = new SignatureValidator(x509Credential);
            signatureValidator.validate(assertion.getSignature());
            log.debug("Signature validation successful");
        } catch (ValidationException e) {
            log.debug(e.getMessage(), e);
            return false;
        }


        /**
         * The authorization server MUST verify that the Assertion is valid in all other respects per
         * [OASIS.saml-core-2.0-os], such as (but not limited to) evaluating all content within the Conditions
         * element including the NotOnOrAfter and NotBefore attributes, rejecting unknown condition types, etc.
         *
         * [OASIS.saml-core-2.0-os] - http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
         */

        // TODO: Throw the SAML request through the general SAML2 validation routines

        tokReqMsgCtx.setScope(tokReqMsgCtx.getOauth2AccessTokenReqDTO().getScope());

        // Storing the Assertion. This will be used in OpenID Connect for example
        tokReqMsgCtx.addProperty(OAuthConstants.OAUTH_SAML2_ASSERTION, assertion);

		// Invoking extension
		SAML2TokenCallbackHandler callback =
		                                     OAuthServerConfiguration.getInstance()
		                                                             .getSAML2TokenCallbackHandler();
		if (callback != null) {
			log.debug("Invoking the SAML2 Token callback handler ");
			callback.handleSAML2Token(tokReqMsgCtx);
		}

        return true;
    }

    /**
     * Constructing the SAML or XACML Objects from a String
     * @param xmlString Decoded SAML or XACML String
     * @return SAML or XACML Object
     * @throws org.wso2.carbon.identity.base.IdentityException
     *
     */
    private XMLObject unmarshall(String xmlString) throws IdentityOAuth2Exception{
        Unmarshaller unmarshaller;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setExpandEntityReferences(false);
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(new CarbonEntityResolver());
            Document document = docBuilder.parse(new ByteArrayInputStream(xmlString.trim().getBytes()));
            Element element = document.getDocumentElement();
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            unmarshaller = unmarshallerFactory.getUnmarshaller(element);
            return unmarshaller.unmarshall(element);
        } catch (Exception e) {
            log.error("Error in constructing XML Object from the encoded String", e);
            throw new IdentityOAuth2Exception("Error in constructing XML Object from the encoded String", e);
        }
    }

    /**
     * Helper method to get tenantId from userName
     *
     * @param tenantDomain
     * @return tenantId
     * @throws org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception
     */
    private int getTenantId(String tenantDomain) throws IdentityOAuth2Exception {
        //get tenant domain from user name
        RealmService realmService = OAuthComponentServiceHolder.getRealmService();
        try {
            int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            return tenantId;
        } catch (UserStoreException e) {
            String error = "Error in obtaining tenantId from Domain";
            //do not log
            throw new IdentityOAuth2Exception(error);
        }
    }

}
