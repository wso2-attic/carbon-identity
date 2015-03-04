/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.identity.application.common.model;

import org.apache.axiom.om.OMElement;

import java.util.ArrayList;
import java.util.Iterator;

public class RequestPathAuthenticatorConfig extends LocalAuthenticatorConfig {

    /**
     *
     */
    private static final long serialVersionUID = -753652473009612026L;

    /*
     * <RequestPathAuthenticatorConfig> <Name></Name> <DisplayName></DisplayName>
     * <IsEnabled></IsEnabled> <Properties></Properties> </RequestPathAuthenticatorConfig>
     */
    public static RequestPathAuthenticatorConfig build(OMElement requestPathAuthenticatorConfigOM) {
        RequestPathAuthenticatorConfig requestPathAuthenticatorConfig = new RequestPathAuthenticatorConfig();

        Iterator<?> members = requestPathAuthenticatorConfigOM.getChildElements();

        while (members.hasNext()) {
            OMElement member = (OMElement) members.next();

            if (member.getLocalName().equals("Name")) {
                requestPathAuthenticatorConfig.setName(member.getText());
            } else if (member.getLocalName().equals("DisplayName")) {
                requestPathAuthenticatorConfig.setDisplayName(member.getText());
            } else if (member.getLocalName().equals("IsEnabled")) {
                requestPathAuthenticatorConfig.setEnabled(Boolean.parseBoolean(member.getText()));
            } else if (member.getLocalName().equals("Properties")) {
                Iterator<?> propertiesIter = member.getChildElements();
                ArrayList<Property> propertiesArrList = new ArrayList<Property>();

                if (propertiesIter != null) {
                    while (propertiesIter.hasNext()) {
                        OMElement propertiesElement = (OMElement) (propertiesIter.next());
                        Property prop = Property.build(propertiesElement);
                        propertiesArrList.add(prop);
                    }
                }

                if (propertiesArrList.size() > 0) {
                    Property[] propertiesArr = propertiesArrList.toArray(new Property[0]);
                    requestPathAuthenticatorConfig.setProperties(propertiesArr);
                }
            }
        }
        return requestPathAuthenticatorConfig;
    }

}
