/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authenticator.social.yahoo;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authenticator.openid.OpenIDAuthenticator;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Deprecated
public class YahooOpenIDAuthenticator extends OpenIDAuthenticator {

    private static final long serialVersionUID = -782801773114711699L;
    private String openIDServerUrl;

    @Override
    public String getFriendlyName() {
        return YahooOpenIDAuthenticatorConstants.YAHOO_AUTHENTICATOR_FRIENDLY_NAME;
    }

    @Override
    public String getName() {
        return YahooOpenIDAuthenticatorConstants.YAHOO_AUTHENTICATOR_NAME;
    }

    /**
     * Get Authorization Server Endpoint
     *
     * @param authenticatorProperties
     * @return
     */
    @Override
    protected void setOpenIDServerUrl(
            Map<String, String> authenticatorProperties) {
        this.openIDServerUrl = authenticatorProperties.get(YahooOpenIDAuthenticatorConstants.YAHOO_AUTHZ_URL);
    }

    @Override
    protected String getOpenIDServerUrl() {
        return this.openIDServerUrl;
    }

    /**
     * Get Configuration Properties
     *
     * @return
     */
    @Override
    public List<Property> getConfigurationProperties() {

        List<Property> configProperties = new ArrayList<Property>();

        Property oauthEndpoint = new Property();
        oauthEndpoint.setDisplayName("Yahoo Authentication Endpoint");
        oauthEndpoint.setName(YahooOpenIDAuthenticatorConstants.YAHOO_AUTHZ_URL);
        oauthEndpoint.setValue(IdentityApplicationConstants.YAHOO_AUTHZ_URL);
        oauthEndpoint.setDescription("Enter value corresponding to yahoo oauth endpoint.");
        configProperties.add(oauthEndpoint);

        return configProperties;
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {
        super.processAuthenticationResponse(request, response, context);

        String subject = super.getSubjectFromUserIDClaimURI(context);
        if (subject != null) {
            context.getSubject().setAuthenticatedSubjectIdentifier(subject);
        }
    }
}
