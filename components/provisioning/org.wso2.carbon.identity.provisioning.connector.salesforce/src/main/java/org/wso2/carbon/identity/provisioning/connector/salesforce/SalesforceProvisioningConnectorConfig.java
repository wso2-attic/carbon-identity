/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provisioning.connector.salesforce;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SalesforceProvisioningConnectorConfig implements Serializable {

    private static final long serialVersionUID = -4476579393653814433L;

    private static final Log log = LogFactory.getLog(SalesforceProvisioningConnectorConfig.class);
    private Properties configs;

    /**
     * @param configs
     */
    public SalesforceProvisioningConnectorConfig(Properties configs) {
        this.configs = configs;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<String> getRequiredAttributeNames() {
        List<String> requiredAttributeList = new ArrayList<>();
        String requiredAttributes = this.configs
                .getProperty(SalesforceConnectorConstants.PropertyConfig.REQUIRED_FIELDS);
        if (StringUtils.isNotBlank(requiredAttributes)) {
            requiredAttributeList = Arrays.asList(requiredAttributes
                    .split(IdentityProvisioningConstants.PropertyConfig.DELIMATOR));
        }
        return requiredAttributeList;
    }

    /**
     * @return
     * @throws IdentityProvisioningException
     */
    public String getUserIdClaim() throws IdentityProvisioningException {
        String userIDClaim = this.configs
                .getProperty(SalesforceConnectorConstants.PropertyConfig.USER_ID_CLAIM);
        if (StringUtils.isBlank(userIDClaim)) {
            log.error("Required claim for user id is not defined in config");
            throw new IdentityProvisioningException(
                    "Required claim for user id is not defined in config");
        }
        if (log.isDebugEnabled()) {
            log.debug("Mapped claim for UserId is : " + userIDClaim);
        }
        return userIDClaim;
    }

    /**
     * @param key
     * @return
     */
    public String getValue(String key) {
        return this.configs.getProperty(key);
    }
}
