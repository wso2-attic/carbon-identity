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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.axiom.om.OMElement;

public class InboundAuthenticationConfig implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8966626233502458748L;

    private InboundAuthenticationRequestConfig[] inboundAuthenticationRequestConfigs = new InboundAuthenticationRequestConfig[0];

    /**
     * 
     * @return
     */
    public InboundAuthenticationRequestConfig[] getInboundAuthenticationRequestConfigs() {
        return inboundAuthenticationRequestConfigs;
    }

    /**
     * 
     * @param inboundAuthenticationRequest
     */
    public void setInboundAuthenticationRequestConfigs(
            InboundAuthenticationRequestConfig[] inboundAuthenticationRequestConfigs) {
        this.inboundAuthenticationRequestConfigs = inboundAuthenticationRequestConfigs;
    }

    /*
     * <InboundAuthenticationConfig>
     * <InboundAuthenticationRequestConfigs></InboundAuthenticationRequestConfigs>
     * </InboundAuthenticationConfig>
     */
    public static InboundAuthenticationConfig build(OMElement inboundAuthenticationConfigOM) {

        InboundAuthenticationConfig inboundAuthenticationConfig = new InboundAuthenticationConfig();

        if (inboundAuthenticationConfigOM == null) {
            return inboundAuthenticationConfig;
        }

        Iterator<?> iter = inboundAuthenticationConfigOM.getChildElements();

        while (iter.hasNext()) {

            OMElement element = (OMElement) (iter.next());
            String elementName = element.getLocalName();

            if (elementName.equals("InboundAuthenticationRequestConfigs")) {

                Iterator<?> inboundAuthenticationRequestConfigsIter = element.getChildElements();

                ArrayList<InboundAuthenticationRequestConfig> inboundAuthenticationRequestConfigsArrList;
                inboundAuthenticationRequestConfigsArrList = new ArrayList<InboundAuthenticationRequestConfig>();

                if (inboundAuthenticationRequestConfigsIter != null) {

                    while (inboundAuthenticationRequestConfigsIter.hasNext()) {
                        OMElement inboundAuthenticationRequestConfigsElement;
                        inboundAuthenticationRequestConfigsElement = (OMElement) inboundAuthenticationRequestConfigsIter
                                .next();
                        InboundAuthenticationRequestConfig authReqConfig;
                        authReqConfig = InboundAuthenticationRequestConfig
                                .build(inboundAuthenticationRequestConfigsElement);
                        if (authReqConfig != null) {
                            inboundAuthenticationRequestConfigsArrList.add(authReqConfig);
                        }
                    }
                }

                if (inboundAuthenticationRequestConfigsArrList.size() > 0) {
                    InboundAuthenticationRequestConfig[] inboundAuthenticationRequestConfigsArr = inboundAuthenticationRequestConfigsArrList
                            .toArray(new InboundAuthenticationRequestConfig[0]);
                    inboundAuthenticationConfig
                            .setInboundAuthenticationRequestConfigs(inboundAuthenticationRequestConfigsArr);
                }
            }
        }

        return inboundAuthenticationConfig;

    }
}
