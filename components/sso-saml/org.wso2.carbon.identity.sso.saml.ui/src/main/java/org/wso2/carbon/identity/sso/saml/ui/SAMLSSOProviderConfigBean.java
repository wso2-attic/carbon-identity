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
package org.wso2.carbon.identity.sso.saml.ui;

import java.util.ArrayList;
import java.util.List;

public class SAMLSSOProviderConfigBean {

    private String issuer = "";
    private String assertionConsumerUrl = "";
    private String enableAssertionSigning = "false";
    private String enableSignatureValidation = "false";
    private String certificateAlias = "";
    private String enableSingleLogout = "false";
    private String singleLogoutUrl = "";
    private String enableClaims = "false";
    private String enableAudiences = "false";
    private String enableRecipients = "false";
    private List<String> selectedClaims = new ArrayList<String>();
    private List<String> selectedAudiences = new ArrayList<String>();
    private List<String> selectedRecipients = new ArrayList<String>();
    private String attributeConsumingServiceIndex = null;
    private String isIdPInitSSOEnabled;

    /**
     * clears the values in bean
     */
    public void clearBean() {
        issuer = "";
        assertionConsumerUrl = "";
        enableAssertionSigning = "false";
        enableSignatureValidation = "false";
        certificateAlias = "";
        enableSingleLogout = "false";
        singleLogoutUrl = "";
        enableClaims = "false";
        enableAudiences = "false";
        enableRecipients = "false";
        attributeConsumingServiceIndex = "";
        selectedClaims.clear();
    }

    /**
     * @return the singleLogoutUrl
     */
    public String getSingleLogoutUrl() {
        return singleLogoutUrl;
    }

    /**
     * @param singleLogoutUrl the singleLogoutUrl to set
     */
    public void setSingleLogoutUrl(String singleLogoutUrl) {
        this.singleLogoutUrl = singleLogoutUrl;
    }

    /**
     * @return the enableSingleLogout
     */
    public String getEnableSingleLogout() {
        return enableSingleLogout;
    }

    /**
     * @param enableSingleLogout the enableSingleLogout to set
     */
    public void setEnableSingleLogout(String enableSingleLogout) {
        this.enableSingleLogout = enableSingleLogout;
    }

    /**
     * @return the certificateAlias
     */
    public String getCertificateAlias() {
        return certificateAlias;
    }

    /**
     * @param certificateAlias the certificateAlias to set
     */
    public void setCertificateAlias(String certificateAlias) {
        this.certificateAlias = certificateAlias;
    }

    /**
     * @return the enableSignatureValidation
     */
    public String getEnableSignatureValidation() {
        return enableSignatureValidation;
    }

    /**
     * @param enableSignatureValidation the enableSignatureValidation to set
     */
    public void setEnableSignatureValidation(String enableSignatureValidation) {
        this.enableSignatureValidation = enableSignatureValidation;
    }

    /**
     * @return the enableAssertionSigning
     */
    public String getEnableAssertionSigning() {
        return enableAssertionSigning;
    }

    /**
     * @param enableAssertionSigning the enableAssertionSigning to set
     */
    public void setEnableAssertionSigning(String enableAssertionSigning) {
        this.enableAssertionSigning = enableAssertionSigning;
    }

    /**
     * @return the assertionConsumerUrl
     */
    public String getAssertionConsumerUrl() {
        return assertionConsumerUrl;
    }

    /**
     * @param assertionConsumerUrl the assertionConsumerUrl to set
     */
    public void setAssertionConsumerUrl(String assertionConsumerUrl) {
        this.assertionConsumerUrl = assertionConsumerUrl;
    }

    /**
     * @return the issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * @param issuer the issuer to set
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * @return the enableClaims
     */
    public String getEnableClaims() {
        return enableClaims;
    }

    /**
     * @param enableClaims the enableClaims to set
     */
    public void setEnableClaims(String enableClaims) {
        this.enableClaims = enableClaims;
    }

    /**
     * @return the selectedClaims
     */
    public List<String> getSelectedClaims() {
        return selectedClaims;
    }

    public String[] getSelectedClaimsAttay() {
        return selectedClaims.toArray(new String[selectedClaims.size()]);
    }

    /**
     * @param selectedClaims the selected claims to set
     */
    public void setSelectedAttributes(List<String> selectedClaims) {
        this.selectedClaims = selectedClaims;
    }

    /**
     * add an claim to the required cliams list
     *
     * @param claim
     * @return
     */
    public boolean addClaimToList(String claim) {
        if (selectedClaims.contains(claim)) {
            return false;
        }
        selectedClaims.add(claim);
        return true;
    }

    /**
     * remove a claim from the required claims list
     *
     * @param claim
     */
    public void removeClaimFromList(String claim) {
        selectedClaims.remove(claim);
    }

    /**
     * @return the enableAudiences
     */
    public String getEnableAudiences() {
        return enableAudiences;
    }

    /**
     * @param enableAudiences the enableAudiences to set
     */
    public void setEnableAudiences(String enableAudiences) {
        this.enableAudiences = enableAudiences;
    }

    /**
     * @return the selectedAudiences
     */
    public List<String> getSelectedAudiences() {
        return selectedAudiences;
    }

    public String[] getSelectedAudiencesArray() {
        return selectedAudiences.toArray(new String[selectedAudiences.size()]);
    }

    /**
     * add an audience to the required audiences list
     *
     * @param audience
     * @return
     */
    public boolean addAudienceToList(String audience) {
        if (selectedAudiences.contains(audience)) {
            return false;
        }
        selectedAudiences.add(audience);
        return true;
    }

    /**
     * remove a audience from the required audiences list
     *
     * @param audience
     */
    public void removeAudienceFromList(String audience) {
        selectedAudiences.remove(audience);
    }

    /**
     * @return the enableRecipients
     */
    public String getEnableRecipients() {
        return enableRecipients;
    }

    /**
     * @param enableRecipients the enableRecipients to set
     */
    public void setEnableRecipients(String enableRecipients) {
        this.enableRecipients = enableRecipients;
    }

    /**
     * @return the selectedRecipients
     */
    public List<String> getSelectedRecipients() {
        return selectedRecipients;
    }

    public String[] getSelectedRecipientsArray() {
        return selectedRecipients.toArray(new String[selectedRecipients.size()]);
    }

    /**
     * add an recipient to the required recipients list
     *
     * @param recipient
     * @return
     */
    public boolean addRecipientToList(String recipient) {
        if (selectedRecipients.contains(recipient)) {
            return false;
        }
        selectedRecipients.add(recipient);
        return true;
    }

    /**
     * remove a recipient from the required recipients list
     *
     * @param recipient
     */
    public void removeRecipientFromList(String recipient) {
        selectedRecipients.remove(recipient);
    }

    /**
     * @return the attributeConsumingServiceIndex
     */
    public String getAttributeConsumingServiceIndex() {
        return attributeConsumingServiceIndex;
    }

    /**
     * @param attributeConsumingServiceIndex the issuer to set
     */
    public void setAttributeConsumingServiceIndex(String attributeConsumingServiceIndex) {
        this.attributeConsumingServiceIndex = attributeConsumingServiceIndex;
    }

    public String isIdPInitSSOEnabled() {
        return isIdPInitSSOEnabled;
    }

    public void setIdPInitSSOEnabled(String idPInitSSOEnabled) {
        isIdPInitSSOEnabled = idPInitSSOEnabled;
    }
}
