/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.wso2.carbon.identity.sso.agent.bean;

import com.google.gson.Gson;
import org.openid4java.discovery.DiscoveryInformation;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;

import java.util.List;
import java.util.Map;

public class SSOAgentSessionBean {

    private OpenIDSessionBean openIDSessionBean;

    private SAMLSSOSessionBean samlssoSessionBean;

    public SAMLSSOSessionBean getSAMLSSOSessionBean() {
        return samlssoSessionBean;
    }

    public void setSAMLSSOSessionBean(SAMLSSOSessionBean samlssoSessionBean) {
        this.samlssoSessionBean = samlssoSessionBean;
    }

    public OpenIDSessionBean getOpenIDSessionBean() {
        return openIDSessionBean;
    }

    public void setOpenIDSessionBean(OpenIDSessionBean openIDSessionBean) {
        this.openIDSessionBean = openIDSessionBean;
    }

    public static class AccessTokenResponseBean {

        private String access_token;

        private String refresh_token;

        private String token_type;

        private String expires_in;

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }

        public String getRefresh_token() {
            return refresh_token;
        }

        public void setRefresh_token(String refresh_token) {
            this.refresh_token = refresh_token;
        }

        public String getToken_type() {
            return token_type;
        }

        public void setToken_type(String token_type) {
            this.token_type = token_type;
        }

        public String getExpires_in() {
            return expires_in;
        }

        public void setExpires_in(String expires_in) {
            this.expires_in = expires_in;
        }

       @Override
       public String toString() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    public class OpenIDSessionBean {

        private DiscoveryInformation discoveryInformation;

        private String claimedId;

        private Map<String, List<String>> openIdAttributes;

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

        public Map<String, List<String>> getOpenIdAttributes() {
            return openIdAttributes;
        }

        public void setOpenIdAttributes(Map<String, List<String>> openIdAttributes) {
            this.openIdAttributes = openIdAttributes;
        }
    }

    public class SAMLSSOSessionBean {

        private String subjectId;

        private Response samlResponse;

        private String samlResponseString;

        private Assertion samlAssertion;

        private String samlAssertionString;

        private AccessTokenResponseBean accessTokenResponseBean;

        private String idPSessionIndex;

        private Map<String, String> samlSSOAttributes;

        public String getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(String subjectId) {
            this.subjectId = subjectId;
        }

        public Map<String, String> getSAMLSSOAttributes() {
            return samlSSOAttributes;
        }

        public void setSAMLSSOAttributes(Map<String, String> samlSSOAttributes) {
            this.samlSSOAttributes = samlSSOAttributes;
        }

        public String getIdPSessionIndex() {
            return idPSessionIndex;
        }

        public void setIdPSessionIndex(String idPSessionIndex) {
            this.idPSessionIndex = idPSessionIndex;
        }

        public Response getSAMLResponse() {
            return samlResponse;
        }

        public void setSAMLResponse(Response samlResponse) {
            this.samlResponse = samlResponse;
        }

        public String getSAMLResponseString() {
            return samlResponseString;
        }

        public void setSAMLResponseString(String samlResponseString) {
            this.samlResponseString = samlResponseString;
        }

        public Assertion getSAMLAssertion() {
            return samlAssertion;
        }

        public void setSAMLAssertion(Assertion samlAssertion) {
            this.samlAssertion = samlAssertion;
        }

        public String getSAMLAssertionString() {
            return samlAssertionString;
        }

        public void setSAMLAssertionString(String samlAssertionString) {
            this.samlAssertionString = samlAssertionString;
        }

        public AccessTokenResponseBean getAccessTokenResponseBean() {
            return accessTokenResponseBean;
        }

        public void setAccessTokenResponseBean(AccessTokenResponseBean accessTokenResponseBean) {
            this.accessTokenResponseBean = accessTokenResponseBean;
        }
    }
}
