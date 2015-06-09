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

package org.wso2.carbon.identity.provisioning.connector.scim;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.provisioning.AbstractProvisioningConnectorFactory;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;

/**
 * @author
 */
public class SCIMProvisioningConnectorFactory extends AbstractProvisioningConnectorFactory {

    public static final String SCIM = "scim";
    private static final Log log = LogFactory.getLog(SCIMProvisioningConnectorFactory.class);

    @Override
    /**
     * @throws IdentityProvisioningException
     */
    protected SCIMProvisioningConnector buildConnector(Property[] provisioningProperties)
            throws IdentityProvisioningException {
        SCIMProvisioningConnector scimProvisioningConnector = new SCIMProvisioningConnector();
        scimProvisioningConnector.init(provisioningProperties);

        if (log.isDebugEnabled()) {
            log.debug("Created new connector of type : " + SCIM);
        }
        return scimProvisioningConnector;
    }

    @Override
    public String getConnectorType() {
        return SCIM;
    }

}
