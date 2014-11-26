/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sso.agent.bean;

import org.opensaml.common.xml.SAMLConstants;
import org.wso2.carbon.identity.sso.agent.SSOAgentConstants;
import org.wso2.carbon.identity.sso.agent.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.openid.AttributesRequestor;
import org.wso2.carbon.identity.sso.agent.saml.SSOAgentCarbonX509Credential;
import org.wso2.carbon.identity.sso.agent.saml.SSOAgentX509Credential;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SSOAgentConfig {

    private static Logger LOGGER = Logger.getLogger(SSOAgentConstants.LOGGER_NAME);

    private Boolean isSAML2SSOLoginEnabled = false;
    private Boolean isOpenIdLoginEnabled = false;
    private Boolean isOAuth2SAML2GrantEnabled = false;

    private String saml2SSOURL = null;
    private String openIdURL = null;
    private String oauth2SAML2GrantURL = null;
    private Set<String> skipURIs = new HashSet<String>();

    private Map<String, String[]> queryParams = new HashMap<String, String[]>();

    private SAML2 saml2 = new SAML2();
    private OpenID openId = new OpenID();
    private OAuth2 oauth2 = new OAuth2();

    public Boolean isSAML2SSOLoginEnabled() {
        return isSAML2SSOLoginEnabled;
    }

    public Boolean isOpenIdLoginEnabled() {
        return isOpenIdLoginEnabled;
    }

    public Boolean isOAuth2SAML2GrantEnabled() {
        return isOAuth2SAML2GrantEnabled;
    }

    public String getSAML2SSOURL() {
        return saml2SSOURL;
    }

    public String getOpenIdURL() {
        return openIdURL;
    }

    public String getOAuth2SAML2GrantURL() {
        return oauth2SAML2GrantURL;
    }

    public Set<String> getSkipURIs(){
        return skipURIs;
    }

    public Map<String,String[]> getQueryParams(){
        return queryParams;
    }

    public SAML2 getSAML2() {
        return saml2;
    }

    public OAuth2 getOAuth2() {
        return oauth2;
    }

    public OpenID getOpenId() {
        return openId;
    }

    public void setSAML2SSOLoginEnabled(Boolean isSAML2SSOLoginEnabled) {
        this.isSAML2SSOLoginEnabled = isSAML2SSOLoginEnabled;
    }

    public void setOpenIdLoginEnabled(Boolean isOpenIdLoginEnabled) {
        this.isOpenIdLoginEnabled = isOpenIdLoginEnabled;
    }

    public void setOAuth2SAML2GrantEnabled(Boolean isOAuth2SAML2GrantEnabled) {
        this.isOAuth2SAML2GrantEnabled = isOAuth2SAML2GrantEnabled;
    }

    public void setSAML2SSOURL(String saml2SSOURL) {
        this.saml2SSOURL = saml2SSOURL;
    }

    public void setOpenIdURL(String openIdURL) {
        this.openIdURL = openIdURL;
    }

    public void setOAuth2SAML2GrantURL(String oauth2SAML2GrantURL) {
        this.oauth2SAML2GrantURL = oauth2SAML2GrantURL;
    }

    public void setSkipURIs(Set<String> skipURIs){
        this.skipURIs = skipURIs;
    }

    public void setQueryParams(Map<String,String[]> queryParams) {
        this.queryParams = queryParams;
    }

    public class SAML2 {

        private String httpBinding = null;
        private String spEntityId = null;
        private String acsURL = null;
        private String idPEntityId = null;
        private String idPURL = null;
        private Boolean isSLOEnabled = false;
        private String sloURL = null;
        private String attributeConsumingServiceIndex = null;
        private SSOAgentX509Credential ssoAgentX509Credential = null;
        private Boolean isAssertionSigned = false;
        private Boolean isAssertionEncrypted = false;
        private Boolean isResponseSigned = false;
        private Boolean isRequestSigned = false;
        private Boolean isPassiveAuthn = false;
        private Boolean isForceAuthn = false;
        private String relayState = null;
        /**
         *
         * The html page that will auto-submit the SAML2 to the IdP.
         * This should be in valid HTML syntax, with following section within the
         * auto-submit form.
         * "&lt;!--$saml_params--&gt;"
         * This section will be replaced by the SAML2 parameters.
         * <p/>
         * If the parameter value is empty, null or doesn't have the above
         * section, the default page will be shown
         */
        private String postBindingRequestHTMLPayload = null;

        public String getHttpBinding() {
            return httpBinding;
        }

        public String getSPEntityId() {
            return spEntityId;
        }

        public String getACSURL() {
            return acsURL;
        }

        public String getIdPEntityId() {
            return idPEntityId;
        }

        public String getIdPURL() {
            return idPURL;
        }

        public Boolean isSLOEnabled() {
            return isSLOEnabled;
        }

        public String getSLOURL() {
            return sloURL;
        }

        public String getAttributeConsumingServiceIndex() {
            return attributeConsumingServiceIndex;
        }

        public SSOAgentX509Credential getSSOAgentX509Credential() {
            return ssoAgentX509Credential;
        }

        public Boolean isAssertionSigned() {
            return isAssertionSigned;
        }

        public Boolean isAssertionEncrypted() {
            return isAssertionEncrypted;
        }

        public Boolean isResponseSigned() {
            return isResponseSigned;
        }

        public Boolean isRequestSigned() {
            return isRequestSigned;
        }

        public Boolean isPassiveAuthn() {
            return isPassiveAuthn;
        }

        public Boolean isForceAuthn() {
            return isForceAuthn;
        }

        public String getRelayState() {
            return relayState;
        }

        public String getPostBindingRequestHTMLPayload() {
            return postBindingRequestHTMLPayload;
        }

        public void setHttpBinding(String httpBinding) {
            this.httpBinding = httpBinding;
        }

        public void setSPEntityId(String spEntityId) {
            this.spEntityId = spEntityId;
        }

        public void setACSURL(String acsURL) {
            this.acsURL = acsURL;
        }

        public void setIdPEntityId(String idPEntityId) {
            this.idPEntityId = idPEntityId;
        }

        public void setIdPURL(String idPURL) {
            this.idPURL = idPURL;
        }

        public void setSLOEnabled(Boolean isSLOEnabled) {
            this.isSLOEnabled = isSLOEnabled;
        }

        public void setSLOURL(String sloURL) {
            this.sloURL = sloURL;
        }

        public void setAttributeConsumingServiceIndex(String attributeConsumingServiceIndex) {
            this.attributeConsumingServiceIndex = attributeConsumingServiceIndex;
        }

        public void setSSOAgentX509Credential(SSOAgentX509Credential ssoAgentX509Credential) {
            this.ssoAgentX509Credential = ssoAgentX509Credential;
        }

        public void setAssertionSigned(Boolean isAssertionSigned) {
            this.isAssertionSigned = isAssertionSigned;
        }

        public void setAssertionEncrypted(Boolean isAssertionEncrypted) {
            this.isAssertionEncrypted = isAssertionEncrypted;
        }

        public void setResponseSigned(Boolean isResponseSigned) {
            this.isResponseSigned = isResponseSigned;
        }

        public void setRequestSigned(Boolean isRequestSigned) {
            this.isRequestSigned = isRequestSigned;
        }

        public void setPassiveAuthn(Boolean isPassiveAuthn) {
            this.isPassiveAuthn = isPassiveAuthn;
        }

        public void setForceAuthn(Boolean isForceAuthn) {
            this.isForceAuthn = isForceAuthn;
        }

        public void setRelayState(String relayState) {
            this.relayState = relayState;
        }

        public void setPostBindingRequestHTMLPayload(String postBindingRequestHTMLPayload) {
            this.postBindingRequestHTMLPayload = postBindingRequestHTMLPayload;
        }
    }

    public class OpenID {

        private String mode = null;
        private String providerURL = null;
        private String returnToURL = null;
        private String claimedId = null;
        private AttributesRequestor attributesRequestor = null;
        private boolean isAttributeExchangeEnabled = false;
        private boolean isDumbModeEnabled = false;

        public String getMode(){
            return mode;
        }

        public String getProviderURL() {
            return providerURL;
        }

        public String getReturnToURL() {
            return returnToURL;
        }

        public String getClaimedId(){
            return claimedId;
        }

        public AttributesRequestor getAttributesRequestor() {
            return attributesRequestor;
        }

        public boolean isAttributeExchangeEnabled() {
            return isAttributeExchangeEnabled;
        }

        public boolean isDumbModeEnabled() {
            return isDumbModeEnabled;
        }

        public void setMode(String mode){
            this.mode = mode;
        }

        public void setProviderURL(String providerURL) {
            this.providerURL = providerURL;
        }

        public void setReturnToURL(String returnToURL) {
            this.returnToURL = returnToURL;
        }

        public void setClaimedId(String claimedId) {
            this.claimedId = claimedId;
        }

        public void setAttributesRequestor(AttributesRequestor attributesRequestor) {
            this.attributesRequestor = attributesRequestor;
        }

        public void setAttributeExchangeEnabled(boolean isAttributeExchangeEnabled) {
            this.isAttributeExchangeEnabled = isAttributeExchangeEnabled;
        }

        public void setDumbModeEnabled(boolean isDumbModeEnabled) {
            this.isDumbModeEnabled = isDumbModeEnabled;
        }
    }

    public class OAuth2 {

        private String tokenURL = null;
        private String clientId = null;
        private String clientSecret = null;

        public String getClientSecret() {
            return clientSecret;
        }

        public String getTokenURL() {
            return tokenURL;
        }

        public String getClientId() {
            return clientId;
        }

        public void setTokenURL(String tokenURL) {
            this.tokenURL = tokenURL;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    public void initConfig(Properties properties) throws SSOAgentException{

        String isSAML2SSOLoginEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.ENABLE_SAML2_SSO_LOGIN);
        if(isSAML2SSOLoginEnabledString != null){
            isSAML2SSOLoginEnabled = Boolean.parseBoolean(isSAML2SSOLoginEnabledString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.ENABLE_SAML2_SSO_LOGIN +
                    " not configured. Defaulting to \'false\'");
            isSAML2SSOLoginEnabled = false;
        }

        String isOpenIdLoginEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.ENABLE_OPENID_SSO_LOGIN);
        if(isOpenIdLoginEnabledString != null){
            isOpenIdLoginEnabled = Boolean.parseBoolean(isOpenIdLoginEnabledString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.ENABLE_OPENID_SSO_LOGIN +
                    " not configured. Defaulting to \'false\'");
            isOpenIdLoginEnabled = false;
        }

        String isSAML2OAuth2GrantEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.ENABLE_OAUTH2_SAML2_OAUTH2_GRANT);
        if(isSAML2OAuth2GrantEnabledString != null){
            isOAuth2SAML2GrantEnabled = Boolean.parseBoolean(isSAML2OAuth2GrantEnabledString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.ENABLE_OAUTH2_SAML2_OAUTH2_GRANT +
                    " not configured. Defaulting to \'false\'");
            isOAuth2SAML2GrantEnabled = false;
        }

        saml2SSOURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2_SSO_URL);
        openIdURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.OPENID_URL);
        oauth2SAML2GrantURL = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OAUTH2_SAML2_GRANT_URL);

        String skipURIsString = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SKIP_URIS);
        if(skipURIsString != null && !skipURIsString.isEmpty()){
            String[] skipURIArray = skipURIsString.split(",");
            for(String skipURI : skipURIArray){
                skipURIs.add(skipURI);
            }
        }

        String queryParamsString = properties.getProperty(SSOAgentConstants.SSOAgentConfig.QUERY_PARAMS);
        if(queryParamsString != null && !queryParamsString.isEmpty()){
            String[] queryParamsArray = queryParamsString.split("&");
            Map<String, List<String>> queryParamMap = new HashMap<String, List<String>>();
            if(queryParamsArray.length > 0){
                for(String queryParam : queryParamsArray) {
                    String[] splitParam = queryParam.split("=");
                    if(splitParam.length == 2){
                        if (queryParamMap.get(splitParam[0]) != null) {
                            queryParamMap.get(splitParam[0]).add(splitParam[1]);
                        } else {
                            List<String> newList = new ArrayList<String>();
                            newList.add(splitParam[1]);
                            queryParamMap.put(splitParam[0], newList);
                        }
                    }

                }
                for(Map.Entry<String, List<String>> entry : queryParamMap.entrySet()){
                    String[] valueArray = entry.getValue().toArray(new String[entry.getValue().size()]);
                    queryParams.put(entry.getKey(), valueArray);
                }
            }
        }

        saml2.httpBinding = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.HTTP_BINDING);
        if(saml2.httpBinding == null || saml2.httpBinding.isEmpty()){
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.HTTP_BINDING +
                    " not configured. Defaulting to \'" + SAMLConstants.SAML2_POST_BINDING_URI + "\'");
            saml2.httpBinding = SAMLConstants.SAML2_POST_BINDING_URI;
        }
        saml2.spEntityId = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.SP_ENTITY_ID);
        saml2.acsURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.ACS_URL);
        saml2.idPEntityId = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.IDP_ENTITY_ID);
        saml2.idPURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.IDP_URL);
        saml2.attributeConsumingServiceIndex = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ATTRIBUTE_CONSUMING_SERVICE_INDEX);

        String isSLOEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_SLO);
        if(isSLOEnabledString != null){
            saml2.isSLOEnabled = Boolean.parseBoolean(isSLOEnabledString);
        } else {
            LOGGER.info("\'" + SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_SLO +
                    "\' not configured. Defaulting to \'false\'");
            saml2.isSLOEnabled = false;
        }
        saml2.sloURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.SLO_URL);

        String isAssertionSignedString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_ASSERTION_SIGNING);
        if(isAssertionSignedString != null){
            saml2.isAssertionSigned = Boolean.parseBoolean(isAssertionSignedString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_ASSERTION_SIGNING +
                    " not configured. Defaulting to \'false\'");
            saml2.isAssertionSigned = false;
        }

        String isAssertionEncryptedString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_ASSERTION_ENCRYPTION);
        if(isAssertionEncryptedString != null){
            saml2.isAssertionEncrypted = Boolean.parseBoolean(isAssertionEncryptedString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_ASSERTION_ENCRYPTION +
                    " not configured. Defaulting to \'false\'");
            saml2.isAssertionEncrypted = false;
        }

        String isResponseSignedString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_RESPONSE_SIGNING);
        if(isResponseSignedString != null){
            saml2.isResponseSigned = Boolean.parseBoolean(isResponseSignedString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_RESPONSE_SIGNING +
                    " not configured. Defaulting to \'false\'");
            saml2.isResponseSigned = false;
        }

        String isRequestSignedString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_REQUEST_SIGNING);
        if(isRequestSignedString != null){
            saml2.isRequestSigned = Boolean.parseBoolean(isRequestSignedString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_REQUEST_SIGNING +
                    " not configured. Defaulting to \'false\'");
            saml2.isRequestSigned = false;
        }

        String isPassiveAuthnString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.IS_PASSIVE_AUTHN);
        if(isPassiveAuthnString != null){
            saml2.isPassiveAuthn = Boolean.parseBoolean(isPassiveAuthnString);
        } else {
            LOGGER.log(Level.FINE, "\'" + SSOAgentConstants.SSOAgentConfig.SAML2.IS_PASSIVE_AUTHN +
                    "\' not configured. Defaulting to \'false\'");
            saml2.isPassiveAuthn = false;
        }

        String isForceAuthnString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.IS_FORCE_AUTHN);
        if(isForceAuthnString != null){
            saml2.isForceAuthn = Boolean.parseBoolean(properties.getProperty(isForceAuthnString));
        } else {
            LOGGER.log(Level.FINE, "\'" + SSOAgentConstants.SSOAgentConfig.SAML2.IS_FORCE_AUTHN +
                    "\' not configured. Defaulting to \'false\'");
            saml2.isForceAuthn = false;
        }

        saml2.relayState = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.RELAY_STATE);
        saml2.postBindingRequestHTMLPayload = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.POST_BINDING_REQUEST_HTML_PAYLOAD);

        oauth2.tokenURL = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OAuth2.TOKEN_URL);
        oauth2.clientId = properties.getProperty(SSOAgentConstants.SSOAgentConfig.OAuth2.CLIENT_ID);
        oauth2.clientSecret = properties.getProperty(SSOAgentConstants.SSOAgentConfig.OAuth2.CLIENT_SECRET);

        openId.providerURL = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OpenID.PROVIDER_URL);
        openId.returnToURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.OpenID.RETURN_TO_URL);

        String isAttributeExchangeEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OpenID.ENABLE_ATTRIBUTE_EXCHANGE);
        if (isAttributeExchangeEnabledString != null){
            openId.isAttributeExchangeEnabled = Boolean.parseBoolean(isAttributeExchangeEnabledString);
        } else {
            LOGGER.log(Level.FINE, "\'" + SSOAgentConstants.SSOAgentConfig.OpenID.ENABLE_ATTRIBUTE_EXCHANGE +
                    "\' not configured. Defaulting to \'true\'");
            openId.isAttributeExchangeEnabled = true;
        }

        String isDumbModeEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OpenID.ENABLE_DUMB_MODE);
        if (isAttributeExchangeEnabledString != null){
            openId.isDumbModeEnabled = Boolean.parseBoolean(isDumbModeEnabledString);
        } else {
            LOGGER.log(Level.FINE, "\'" + SSOAgentConstants.SSOAgentConfig.OpenID.ENABLE_DUMB_MODE +
                    "\' not configured. Defaulting to \'false\'");
            openId.isDumbModeEnabled = false;
        }
    }

    public void verifyConfig() throws SSOAgentException {

        if(isSAML2SSOLoginEnabled && saml2SSOURL == null){
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2_SSO_URL + "\' not configured");
        }

        if(isOpenIdLoginEnabled && openIdURL == null){
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.OPENID_URL + "\' not configured");
        }

        if(!isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled) {
            throw new SSOAgentException(
                    "SAML2 SSO Login is disabled. Cannot use SAML2 Bearer Grant type for OAuth2");
        }

        if(isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled && oauth2SAML2GrantURL == null){
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.OAUTH2_SAML2_GRANT_URL + "\' not configured");
        }

        if(isSAML2SSOLoginEnabled && saml2.spEntityId == null){
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2.SP_ENTITY_ID + "\' not configured");
        }

        if(isSAML2SSOLoginEnabled && saml2.acsURL == null){
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2.ACS_URL + "\' not configured");
        }

        if(isSAML2SSOLoginEnabled && saml2.idPEntityId == null){
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2.IDP_ENTITY_ID + "\' not configured");
        }

        if(isSAML2SSOLoginEnabled && saml2.idPURL == null){
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2.IDP_URL + "\' not configured");
        }

        if(isSAML2SSOLoginEnabled && saml2.attributeConsumingServiceIndex == null){
            LOGGER.log(Level.FINE,
                    "\'" + SSOAgentConstants.SSOAgentConfig.SAML2.ATTRIBUTE_CONSUMING_SERVICE_INDEX +
                            "\' not configured. " + "No attributes of the Subject will be requested");
        }

        if(isSAML2SSOLoginEnabled && saml2.isSLOEnabled && saml2.sloURL == null){
            throw new SSOAgentException("Single Logout enabled, but SLO URL not configured");
        }

        if(isSAML2SSOLoginEnabled &&
                (saml2.isAssertionSigned || saml2.isAssertionEncrypted || saml2.isResponseSigned ||
                        saml2.isRequestSigned) && saml2.ssoAgentX509Credential == null){
            LOGGER.log(Level.FINE,
                    "\'SSOAgentX509Credential\' not configured. Defaulting to " +
                            SSOAgentCarbonX509Credential.class.getName());
        }

        if(isSAML2SSOLoginEnabled &&
                (saml2.isAssertionSigned || saml2.isResponseSigned) &&
                saml2.ssoAgentX509Credential.getEntityCertificate() == null){
            throw  new SSOAgentException("Public certificate of IdP not configured");
        }

        if(isSAML2SSOLoginEnabled &&
                (saml2.isRequestSigned || saml2.isAssertionEncrypted) &&
                saml2.ssoAgentX509Credential.getPrivateKey() == null){
            throw new SSOAgentException("Private key of SP not configured");
        }

        if(isOpenIdLoginEnabled && openId.providerURL == null){
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.OpenID.PROVIDER_URL + "\' not configured");
        }

        if(isOpenIdLoginEnabled && openId.returnToURL == null){
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.OpenID.RETURN_TO_URL + "\' not configured");
        }

        if(isOpenIdLoginEnabled && openId.attributesRequestor == null){
            LOGGER.log(Level.FINE, "\'" +
                    SSOAgentConstants.SSOAgentConfig.OpenID.PROVIDER_URL +
                    "\' not configured. " + "No attributes of the Subject will be fetched");
        }

        if(isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled && oauth2.tokenURL == null) {
            throw new SSOAgentException("OAuth2 Token endpoint not configured");
        }

        if(isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled && oauth2.clientId == null) {
            throw new SSOAgentException("OAuth2 Client Id not configured");
        }

        if(isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled && oauth2.clientSecret == null) {
            throw new SSOAgentException("OAuth2 Client Secret not configured");
        }

    }
}
