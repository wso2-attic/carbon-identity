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

import com.google.gson.Gson;
import org.openid4java.discovery.DiscoveryInformation;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;

import java.util.List;
import java.util.Map;

public class LoggedInSessionBean {

    private OpenID openId;

    private SAML2SSO saml2SSO;

    public SAML2SSO getSAML2SSO() {
        return saml2SSO;
    }

    public void setSAML2SSO(SAML2SSO saml2SSO) {
        this.saml2SSO = saml2SSO;
    }

    public OpenID getOpenId() {
        return openId;
    }

    public void setOpenId(OpenID openId) {
        this.openId = openId;
    }

    public class OpenID {

        private DiscoveryInformation discoveryInformation;

        private String claimedId;

        private Map<String,List<String>> subjectAttributes;

        public DiscoveryInformation getDiscoveryInformation() {
            return discoveryInformation;
        }

        public void setDiscoveryInformation(DiscoveryInformation discoveryInformation) {
            this.discoveryInformation = discoveryInformation;
        }

        public String getClaimedId() {
            return claimedId;
        }

        public void setClaimedId(String claimedId) {
            this.claimedId = claimedId;
        }

        public Map<String, List<String>> getSubjectAttributes() {
            return subjectAttributes;
        }

        public void setSubjectAttributes(Map<String, List<String>> subjectAttributes) {
            this.subjectAttributes = subjectAttributes;
        }
    }

    public class SAML2SSO {

        private String subjectId;

        private Response response;

        private String responseString;

        private Assertion assertion;

        private String assertionString;

        private AccessTokenResponseBean accessTokenResponseBean;

        private String sessionIndex;

        private Map<String,String> subjectAttributes;

        public String getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(String subjectId) {
            this.subjectId = subjectId;
        }

        public Map<String, String> getSubjectAttributes() {
            return subjectAttributes;
        }

        public void setSubjectAttributes(Map<String, String> samlSSOAttributes) {
            this.subjectAttributes = samlSSOAttributes;
        }

        public String getSessionIndex() {
            return sessionIndex;
        }

        public void setSessionIndex(String sessionIndex) {
            this.sessionIndex = sessionIndex;
        }

        public Response getSAMLResponse() {
            return response;
        }

        public void setSAMLResponse(Response samlResponse) {
            this.response = samlResponse;
        }

        public String getResponseString() {
            return responseString;
        }

        public void setResponseString(String responseString) {
            this.responseString = responseString;
        }

        public Assertion getAssertion() {
            return assertion;
        }

        public void setAssertion(Assertion samlAssertion) {
            this.assertion = samlAssertion;
        }

        public String getAssertionString() {
            return assertionString;
        }

        public void setAssertionString(String samlAssertionString) {
            this.assertionString = samlAssertionString;
        }

        public AccessTokenResponseBean getAccessTokenResponseBean() {
            return accessTokenResponseBean;
        }

        public void setAccessTokenResponseBean(AccessTokenResponseBean accessTokenResponseBean) {
            this.accessTokenResponseBean = accessTokenResponseBean;
        }
    }

    public static class AccessTokenResponseBean {

        private String accessToken;

        private String refreshToken;

        private String tokenType;

        private String expiresIn;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public String getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(String expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String toString () {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }
}
