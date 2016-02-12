/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.oauth.endpoint.auth;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * This reads the identity config for OAuthAuthenticators
 */
public class OAuthAuthenticatorConfigReader {

    private static Log logger = LogFactory.getLog(OAuthAuthenticatorConfigReader.class);
    private static final String ELEMENT_NAME_OAUTH = "OAuth";
    private static final String ELEMENT_NAME_OAUTH_AUTHENTICATORS = "OAuthAuthenticators";
    private static final String ELEMENT_NAME_AUTHENTICATOR = "Authenticator";
    public static final String ATTRIBUTE_NAME_CLASS = "class";
    public static final String ATTRIBUTE_NAME_NAME = "name";
    private static final String ELEMENT_NAME_PROPERTY = "Property";

    public List<OAuthAuthenticationHandler> buildOAuthAuthenticators() {

        IdentityConfigParser identityConfig = IdentityConfigParser.getInstance();
        try {
            OMElement oauthElem = identityConfig.getConfigElement(ELEMENT_NAME_OAUTH);
            if (oauthElem != null) {
                OMElement oauthAuthElement = oauthElem.getFirstChildWithName(
                        new QName(IdentityCoreConstants.IDENTITY_DEFAULT_NAMESPACE, ELEMENT_NAME_OAUTH_AUTHENTICATORS));
                //iterate through authenticators and build authenticators list
                Iterator<OMElement> authenticators = oauthAuthElement
                        .getChildrenWithName(new QName(ELEMENT_NAME_AUTHENTICATOR));
                List<OAuthAuthenticationHandler> OAuthAuthHandlers = new ArrayList<OAuthAuthenticationHandler>();
                if (authenticators != null) {
                    while (authenticators.hasNext()) {
                        OMElement authenticatorElement = authenticators.next();
                        //read the authenticator class name
                        String authenticatorClassName = authenticatorElement
                                .getAttributeValue(new QName(ATTRIBUTE_NAME_CLASS));
                        //initialize the authenticatorElement
                        Class authenticatorClass = Class.forName(authenticatorClassName);
                        OAuthAuthenticationHandler authHandler = (OAuthAuthenticationHandler) authenticatorClass
                                .newInstance();

                        //read the properties in the authenticator element and set them in the authenticator.
                        Iterator<OMElement> propertyElements = authenticatorElement
                                .getChildrenWithName(new QName(ELEMENT_NAME_PROPERTY));
                        if (propertyElements != null) {
                            Map<String, String> properties = new HashMap<String, String>();
                            while (propertyElements.hasNext()) {
                                OMElement propertyElement = propertyElements.next();
                                String attributeName = propertyElement
                                        .getAttributeValue(new QName(ATTRIBUTE_NAME_NAME));
                                String attributeValue = propertyElement.getText();
                                properties.put(attributeName, attributeValue);
                            }
                            authHandler.setProperties(properties);
                        }
                        OAuthAuthHandlers.add(authHandler);
                    }
                    return OAuthAuthHandlers;
                }
            }

        } catch (ClassNotFoundException e) {
            //we just log the exception and continue loading other authenticators.
            logger.error("Error in loading the authenticator class...", e);
        } catch (InstantiationException e) {
            logger.error("Error while instantiating the authenticator..", e);
        } catch (IllegalAccessException e) {
            logger.error("Error while instantiating the authenticator..", e);
        }
        return Collections.emptyList();
    }
}
