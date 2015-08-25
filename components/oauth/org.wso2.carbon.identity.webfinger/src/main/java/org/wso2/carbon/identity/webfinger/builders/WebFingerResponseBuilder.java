/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */
package org.wso2.carbon.identity.webfinger.builders;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.webfinger.WebFingerConstants;
import org.wso2.carbon.identity.webfinger.WebFingerEndPointException;
import org.wso2.carbon.identity.webfinger.WebFingerRequest;
import org.wso2.carbon.identity.webfinger.WebFingerResponse;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * Build the WebFingerResponse only with the OpenID Provider Issuer.
 * Add other information when needed.
 */
public class WebFingerResponseBuilder {


    public WebFingerResponse buildWebFingerResponse(WebFingerRequest request) throws WebFingerEndPointException,
            ServerConfigurationException {
        WebFingerResponse response = this.getLinkFromIdentityConfig(request.getRel(), request.getResource());
        if (response == null) {
            try {
                response = this.getIssuerFromServerURL(request.getResource());
            } catch (IdentityException e) {
                throw new WebFingerEndPointException(WebFingerConstants.ERROR_CODE_NO_WEBFINGER_CONFIG, "Error in " +
                        "getting server url.\n" + e.getMessage());
            }
        }
        if (response == null) {
            throw new WebFingerEndPointException(WebFingerConstants.ERROR_CODE_NO_WEBFINGER_CONFIG, "No WebFinger " +
                    "settings set at the server.");
        }
        return response;
    }

    /**
     * This is a generic method.
     *
     * @return WebFingerResponse with the respective Rel,Href added to the WebLink list.
     * @rel rel parameter in the request
     * @resource resource parameter in the request
     */
    private WebFingerResponse getLinkFromIdentityConfig(String rel, String resource) throws
            WebFingerEndPointException, ServerConfigurationException {
        WebFingerResponse response = null;
        IdentityConfigParser configParser = IdentityConfigParser.getInstance();
        OMElement webFingerElement = configParser.getConfigElement(WebFingerConstants.CONFIG_WEBFINGER_TAG);
        if (webFingerElement != null) {
            Iterator<OMElement> links = webFingerElement.getChildrenWithName(configParser.getQNameWithIdentityNS
                    (WebFingerConstants.CONFIG_LINK));
            while (links.hasNext()) {
                OMElement link = links.next();
                String linkRel = link.getAttributeValue(new QName(WebFingerConstants.REL));
                if (linkRel.equals(rel)) {
                    response = new WebFingerResponse();
                    response.setSubject(resource);
                    response.addLink(rel, link.getText());
                    break;
                }
            }
        }
        return response;
    }

    /**
     * This is not generic. This is intended to get the web finger issuer only.
     *
     * @return WebFingerResponse with http://openid.net/specs/connect/1.0/issuer, ServerUrl added ti the WebLink list.
     * @resource resource parameter in the web finger request
     */
    private WebFingerResponse getIssuerFromServerURL(String resource) throws IdentityException {
        String issuer = IdentityUtil.getServerURL("");
        if (issuer == null || issuer.isEmpty()) {
            return null;
        }
        issuer = issuer + WebFingerConstants.OPENID_CONNECT_ENDPOINT;
        //TODO
        //get the tenant using the user and append from here.
        WebFingerResponse response = new WebFingerResponse();
        response.setSubject(resource);
        response.addLink(WebFingerConstants.OPENID_CONNETCT_ISSUER_REL, issuer);
        return response;
    }
}
