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

import java.io.Serializable;
import java.util.Iterator;

public class InboundProvisioningConfig implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7268191162277514923L;

    private String provisioningUserStore;
    private boolean provisioningEnabled;

    /*
     * <InboundProvisioningConfig> <ProvisioningUserStore></ProvisioningUserStore>
     * <IsProvisioningEnabled></IsProvisioningEnabled> </InboundProvisioningConfig>
     */
    public static InboundProvisioningConfig build(OMElement inboundProvisioningConfigOM) {
        InboundProvisioningConfig inboundProvisioningConfig = new InboundProvisioningConfig();

        if (inboundProvisioningConfigOM == null) {
            return inboundProvisioningConfig;
        }

        Iterator<?> iter = inboundProvisioningConfigOM.getChildElements();

        while (iter.hasNext()) {
            OMElement element = (OMElement) (iter.next());
            String elementName = element.getLocalName();

            if (elementName.equals("ProvisioningUserStore")) {
                inboundProvisioningConfig.setProvisioningUserStore(element.getText());
            } else if (elementName.equals("IsProvisioningEnabled")) {
                if (element.getText() != null) {
                    inboundProvisioningConfig.setProvisioningEnabled(Boolean.parseBoolean(element
                            .getText()));
                }
            }
        }

        return inboundProvisioningConfig;
    }

    /**
     * @return
     */
    public String getProvisioningUserStore() {
        return provisioningUserStore;
    }

    /**
     * @param provisioningUserStore
     */
    public void setProvisioningUserStore(String provisioningUserStore) {
        this.provisioningUserStore = provisioningUserStore;
    }

    /**
     * @return
     */
    public boolean isProvisioningEnabled() {
        return provisioningEnabled;
    }

    /**
     * @param provisioningEnabled
     */
    public void setProvisioningEnabled(boolean provisioningEnabled) {
        this.provisioningEnabled = provisioningEnabled;
    }

}
