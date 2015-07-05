/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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

package org.wso2.carbon.identity.core.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SAMLSSOServiceProviderDO implements Serializable {

    private static final long serialVersionUID = -1213957008659821807L;
    String tenantDomain;
    private String issuer;
    private String assertionConsumerUrl;
    private String certAlias;
    private String logoutURL;
    private boolean doSingleLogout;
    private String loginPageURL;
    private boolean doSignResponse;
    private boolean doSignAssertions;
    private String attributeConsumingServiceIndex;
    private String[] requestedClaims;
    private List<String> requestedClaimsList;
    private String[] requestedAudiences;
    private List<String> requestedAudiencesList;
    private String[] requestedRecipients;
    private List<String> requestedRecipientsList;
    private boolean enableAttributesByDefault;
    private String nameIdClaimUri;
    private String nameIDFormat;
    private boolean isIdPInitSSOEnabled;
    private boolean doEnableEncryptedAssertion;
    private boolean doValidateSignatureInRequests;

    public String getNameIDFormat() {
        return nameIDFormat;
    }

    public void setNameIDFormat(String nameIDFormat) {
        this.nameIDFormat = nameIDFormat;
    }

    public String getNameIdClaimUri() {
        return nameIdClaimUri;
    }

    public void setNameIdClaimUri(String nameIdClaimUri) {
        this.nameIdClaimUri = nameIdClaimUri;
    }

    public boolean isEnableAttributesByDefault() {
        return enableAttributesByDefault;
    }

    public void setEnableAttributesByDefault(boolean enableAttributesByDefault) {
        this.enableAttributesByDefault = enableAttributesByDefault;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        if (issuer != null) {
            this.issuer = issuer.replaceAll("[\n\r]", "").trim();
        }
    }

    public String getAssertionConsumerUrl() {
        return assertionConsumerUrl;
    }

    public void setAssertionConsumerUrl(String assertionConsumerUrl) {
        if (assertionConsumerUrl != null) {
            this.assertionConsumerUrl = assertionConsumerUrl.replaceAll("[\n\r]", "").trim();
        }
    }

    public String getCertAlias() {
        return certAlias;
    }

    public void setCertAlias(String certAlias) {
        this.certAlias = certAlias;
    }

    public String getLogoutURL() {
        return logoutURL;
    }

    public void setLogoutURL(String logoutURL) {
        if (logoutURL != null) {
            this.logoutURL = logoutURL.replaceAll("[\n\r]", "").trim();
        }
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
        if (loginPageURL != null) {
            this.loginPageURL = loginPageURL.replaceAll("[\n\r]", "").trim();
        }
    }

    public boolean isDoSignAssertions() {
        return doSignAssertions;
    }

    public void setDoSignAssertions(boolean doSignAssertions) {
        this.doSignAssertions = doSignAssertions;
    }

    public String getAttributeConsumingServiceIndex() {
        return attributeConsumingServiceIndex;
    }

    public void setAttributeConsumingServiceIndex(String attributeConsumingServiceIndex) {
        this.attributeConsumingServiceIndex = attributeConsumingServiceIndex;
    }

    /**
     * @return the requestedClaims
     */
    public String[] getRequestedClaims() {
        if (requestedClaims != null) {
            return requestedClaims.clone();
        } else {
            return new String[0];
        }
    }

    /**
     * @param requestedClaims the requestedClaims to set
     */
    public void setRequestedClaims(List<String> requestedClaims) {
        if (requestedClaims != null) {
            this.requestedClaimsList = requestedClaims;
            this.requestedClaims = requestedClaims.toArray(new String[requestedClaims.size()]);
        }
    }

    /**
     * @param requestedClaims the requestedClaims to set
     */
    public void setRequestedClaims(String[] requestedClaims) {
        if (requestedClaims != null) {
            this.requestedClaims = requestedClaims.clone();
            this.requestedClaimsList = Arrays.asList(requestedClaims);
        }
    }

    /**
     * @return the requestedClaims
     */
    public List<String> getRequestedClaimsList() {
        if (requestedClaimsList != null) {
            return requestedClaimsList;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @return the requestedAudiences
     */
    public String[] getRequestedAudiences() {
        if (requestedAudiences != null) {
            return requestedAudiences.clone();
        } else {
            return new String[0];
        }
    }

    /**
     * @param requestedAudiences the requestedAudiences to set
     */
    public void setRequestedAudiences(List<String> requestedAudiences) {
        if (requestedAudiences != null) {
            this.requestedAudiencesList = requestedAudiences;
            this.requestedAudiences = requestedAudiences.toArray(new String[requestedAudiencesList.size()]);
        }
    }

    /**
     * @param requestedAudiences the requestedAudiences to set
     */
    public void setRequestedAudiences(String[] requestedAudiences) {
        if (requestedAudiences != null) {
            this.requestedAudiences = requestedAudiences.clone();
            this.requestedAudiencesList = Arrays.asList(requestedAudiences);
        }
    }

    /**
     * @return the requestedAudiences
     */
    public List<String> getRequestedAudiencesList() {
        if (requestedAudiencesList != null) {
            return requestedAudiencesList;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @return the requestedRecipients
     */
    public String[] getRequestedRecipients() {
        if (requestedRecipients != null) {
            return requestedRecipients.clone();
        } else {
            return new String[0];
        }
    }

    /**
     * @param requestedRecipients the requestedRecipients to set
     */
    public void setRequestedRecipients(List<String> requestedRecipients) {
        if (requestedRecipients != null) {
            this.requestedRecipientsList = requestedRecipients;
            this.requestedRecipients = requestedRecipients.toArray(new String[requestedRecipientsList.size()]);
        }
    }

    /**
     * @param requestedRecipients the requestedRecipients to set
     */
    public void setRequestedRecipients(String[] requestedRecipients) {
        if (requestedRecipients != null) {
            this.requestedRecipients = requestedRecipients.clone();
            this.requestedRecipientsList = Arrays.asList(requestedRecipients);
        }
    }

    /**
     * @return the requestedRecipients
     */
    public List<String> getRequestedRecipientsList() {
        if (requestedRecipientsList != null) {
            return requestedRecipientsList;
        } else {
            return Collections.emptyList();
        }
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

    public boolean isIdPInitSSOEnabled() {
        return isIdPInitSSOEnabled;
    }

    public void setIdPInitSSOEnabled(boolean idPInitSSOEnabled) {
        isIdPInitSSOEnabled = idPInitSSOEnabled;
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

    public void setDoValidateSignatureInRequests(boolean doValidateSignatureInRequests) {
        this.doValidateSignatureInRequests = doValidateSignatureInRequests;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }
}
