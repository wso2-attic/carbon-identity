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
package org.wso2.carbon.identity.sso.saml.dto;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;

import java.io.Serializable;
import java.util.Map;

public class SAMLSSOAuthnReqDTO implements Serializable {

    private static final long serialVersionUID = -8883458443469019318L;

    private AuthenticatedUser user;
    private String password;
    private String issuer;
    private String subject;
    private String assertionConsumerURL;
    private String[] assertionConsumerURLs;
    private String id;
    private String claim;
    private String audience;
    private String recipient;
    private String nameIDFormat;
    private String sloResponseURL;
    private String sloRequestURL;
    private String loginPageURL;
    private String rpSessionId;
    private String requestMessageString;
    private String queryString;
    private String destination;
    private String[] requestedClaims;
    private String[] requestedAudiences;
    private String[] requestedRecipients;
    private boolean doSingleLogout;
    private boolean doSignResponse;
    private boolean doSignAssertions;
    private boolean isStratosDeployment = false;
    private int attributeConsumingServiceIndex;
    private String nameIdClaimUri;
    private boolean idPInitSSOEnabled;
    private boolean idPInitSLOEnabled;
    private String[] idpInitSLOReturnToURLs;
    private boolean doEnableEncryptedAssertion;
    private boolean doValidateSignatureInRequests;
    private Map<String, String> claimMapping = null;
    private String tenantDomain;
    private String certAlias;
    private String signingAlgorithmUri;
    private String digestAlgorithmUri;

    public String getDigestAlgorithmUri() {
        return digestAlgorithmUri;
    }

    public void setDigestAlgorithmUri(String digestAlgorithmUri) {
        if (StringUtils.isNotBlank(digestAlgorithmUri)) {
            this.digestAlgorithmUri = digestAlgorithmUri;
        }
    }

    public String getSigningAlgorithmUri() {
        return signingAlgorithmUri;
    }

    public void setSigningAlgorithmUri(String signingAlgorithmUri) {
        if (StringUtils.isNotBlank(signingAlgorithmUri)) {
            this.signingAlgorithmUri = signingAlgorithmUri;
        }
    }

    public String getNameIdClaimUri() {
        return nameIdClaimUri;
    }

    public void setNameIdClaimUri(String nameIdClaimUri) {
        this.nameIdClaimUri = nameIdClaimUri;
    }

    public int getAttributeConsumingServiceIndex() {
        return attributeConsumingServiceIndex;
    }

    public void setAttributeConsumingServiceIndex(
            int attributeConsumingServiceIndex) {
        this.attributeConsumingServiceIndex = attributeConsumingServiceIndex;
    }

    public String getCertAlias() {
        return certAlias;
    }

    public void setCertAlias(String certAlias) {
        this.certAlias = certAlias;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIssuer() {
        if (issuer.contains("@")) {
            String[] splitIssuer = issuer.split("@");
            return splitIssuer[0];
        }
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getIssuerWithDomain() {
        return issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAssertionConsumerURL() {
        return assertionConsumerURL;
    }

    public void setAssertionConsumerURL(String assertionConsumerURL) {
        this.assertionConsumerURL = assertionConsumerURL;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNameIDFormat() {
        return nameIDFormat;
    }

    public void setNameIDFormat(String nameIDFormat) {
        this.nameIDFormat = nameIDFormat;
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSloResponseURL() {
        return sloResponseURL;
    }

    public void setSloResponseURL(String sloResponseURL) {
        this.sloResponseURL = sloResponseURL;
    }

    public boolean isDoSingleLogout() {
        return doSingleLogout;
    }

    public void setDoSingleLogout(boolean doSingleLogout) {
        this.doSingleLogout = doSingleLogout;
    }

    public String getLoginPageURL() {
        return loginPageURL;
    }

    public void setLoginPageURL(String loginPageURL) {
        this.loginPageURL = loginPageURL;
    }

    public String getRpSessionId() {
        return rpSessionId;
    }

    public void setRpSessionId(String rpSessionId) {
        this.rpSessionId = rpSessionId;
    }

    public boolean getDoSignAssertions() {
        return doSignAssertions;
    }

    public void setDoSignAssertions(boolean doSignAssertions) {
        this.doSignAssertions = doSignAssertions;
    }

    /**
     * @return
     */
    public String getRequestMessageString() {
        return requestMessageString;
    }

    /**
     * @param requestMessageString
     */
    public void setRequestMessageString(String requestMessageString) {
        this.requestMessageString = requestMessageString;
    }

    public String[] getRequestedClaims() {
        if (requestedClaims == null) {
            return new String[0];
        }
        return requestedClaims.clone();
    }

    public void setRequestedClaims(String[] requestedClaims) {
        if (requestedClaims == null) {
            this.requestedClaims = new String[0];
        } else {
            this.requestedClaims = requestedClaims.clone();
        }
    }

    public String[] getRequestedAudiences() {
        if (requestedAudiences == null) {
            return new String[0];
        }
        return requestedAudiences.clone();
    }

    public void setRequestedAudiences(String[] requestedAudiences) {
        if (requestedAudiences == null) {
            this.requestedAudiences = new String[0];
        } else {
            this.requestedAudiences = requestedAudiences.clone();
        }
    }

    public String[] getRequestedRecipients() {
        if (requestedRecipients == null) {
            return new String[0];
        }
        return requestedRecipients.clone();
    }

    public void setRequestedRecipients(String[] requestedRecipients) {
        if (requestedRecipients == null) {
            this.requestedRecipients = new String[0];
        } else {
            this.requestedRecipients = requestedRecipients.clone();
        }
    }

    public boolean isStratosDeployment() {
        return isStratosDeployment;
    }

    public void setStratosDeployment(boolean isStratosDeployment) {
        this.isStratosDeployment = isStratosDeployment;
    }

    /**
     * @return the queryString
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * @param queryString the queryString to set
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**
     * @return the doSignResponse
     */
    public boolean isDoSignResponse() {
        return doSignResponse;
    }

    /**
     * @param doSignResponse the doSignResponse to set
     */
    public void setDoSignResponse(boolean doSignResponse) {
        this.doSignResponse = doSignResponse;
    }

    /**
     * @return the 'destination' attribute of the SAML request
     */
    public String getDestination() {
        return destination;
    }

    /**
     * @param destination Set the SAML request's 'destination' attribute
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isIdPInitSSOEnabled() {
        return idPInitSSOEnabled;
    }

    public void setIdPInitSSOEnabled(boolean isIdPInitSSO) {
        this.idPInitSSOEnabled = isIdPInitSSO;
    }

    public boolean isDoEnableEncryptedAssertion() {
        return doEnableEncryptedAssertion;
    }

    public void setDoEnableEncryptedAssertion(boolean doEnableEncryptedAssertion) {
        this.doEnableEncryptedAssertion = doEnableEncryptedAssertion;
    }

    public boolean isDoValidateSignatureInRequests() {
        return doValidateSignatureInRequests;
    }

    public void setDoValidateSignatureInRequests(
            boolean doValidateSignatureInRequests) {
        this.doValidateSignatureInRequests = doValidateSignatureInRequests;
    }

    public Map<String, String> getClaimMapping() {
        return claimMapping;
    }

    public void setClaimMapping(Map<String, String> claimMapping) {
        this.claimMapping = claimMapping;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public boolean isIdPInitSLOEnabled() {
        return idPInitSLOEnabled;
    }

    public void setIdPInitSLOEnabled(boolean idPInitSLOEnabled) {
        this.idPInitSLOEnabled = idPInitSLOEnabled;
    }

    public String[] getAssertionConsumerURLs() {
        return assertionConsumerURLs;
    }

    public void setAssertionConsumerURLs(String[] assertionConsumerURLs) {
        this.assertionConsumerURLs = assertionConsumerURLs;
    }

    public String[] getIdpInitSLOReturnToURLs() {
        return idpInitSLOReturnToURLs;
    }

    public void setIdpInitSLOReturnToURLs(String[] idpInitSLOReturnToURLs) {
        this.idpInitSLOReturnToURLs = idpInitSLOReturnToURLs;
    }

    public String getSloRequestURL() {
        return sloRequestURL;
    }

    public void setSloRequestURL(String sloRequestURL) {
        this.sloRequestURL = sloRequestURL;
    }
}
