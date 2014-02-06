/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.scim.provider.auth;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.scim.provider.util.SCIMProviderConstants;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SCIMAuthConfigReader {

    private static Log logger = LogFactory.getLog(SCIMAuthConfigReader.class);

    public List<SCIMAuthenticationHandler> buildSCIMAuthenticators() {
        try {
            IdentityConfigParser identityConfig = IdentityConfigParser.getInstance();
            OMElement SCIMAuthElement = identityConfig.getConfigElement(SCIMProviderConstants.ELEMENT_NAME_SCIM_AUTHENTICATORS);
            //iterate through authenticators and build authenticators list
            Iterator<OMElement> authenticators = SCIMAuthElement.getChildrenWithName(new QName(
                    SCIMProviderConstants.ELEMENT_NAME_AUTHENTICATOR));
            List<SCIMAuthenticationHandler> SCIMAuthHandlers = new ArrayList<SCIMAuthenticationHandler>();
            if (authenticators != null) {

                while (authenticators.hasNext()) {
                    OMElement authenticatorElement = authenticators.next();
                    //read the authenticator class name
                    String authenticatorClassName = authenticatorElement.getAttributeValue(new QName(
                            SCIMProviderConstants.ATTRIBUTE_NAME_CLASS));
                    //initialize the authenticatorElement
                    Class authenticatorClass = Class.forName(authenticatorClassName);
                    SCIMAuthenticationHandler authHandler = (SCIMAuthenticationHandler)
                            authenticatorClass.newInstance();

                    //read the properties in the authenticator element and set them in the authenticator.
                    Iterator<OMElement> propertyElements = authenticatorElement.getChildrenWithName(new QName(
                            SCIMProviderConstants.ELEMENT_NAME_PROPERTY));
                    if (propertyElements != null) {
                        Map<String, String> properties = new HashMap<String, String>();
                        while (propertyElements.hasNext()) {
                            OMElement propertyElement = propertyElements.next();
                            String attributeName = propertyElement.getAttributeValue(new QName(
                                    SCIMProviderConstants.ATTRIBUTE_NAME_NAME));
                            String attributeValue = propertyElement.getText();
                            properties.put(attributeName, attributeValue);
                        }
                        authHandler.setProperties(properties);
                    }
                    SCIMAuthHandlers.add(authHandler);
                }
                return SCIMAuthHandlers;
            }

        } catch (ServerConfigurationException e) {
            logger.error("Error in reading authenticator config from " +
                         "identity.xml when initializing the SCIM webapp...");
        } catch (ClassNotFoundException e) {
            //we just log the exception and continue loading other authenticators.
            logger.error("Error in loading the authenticator class...", e);
        } catch (InstantiationException e) {
            logger.error("Error while instantiating the authenticator..", e);
        } catch (IllegalAccessException e) {
            logger.error("Error while instantiating the authenticator..", e);
        }
        return null;
    }
}
