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

package org.wso2.carbon.identity.provisioning.connector.google;

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

public class GoogleProvisioningConnectorConfig implements Serializable {

    private static final long serialVersionUID = -3057146255884741487L;

    private static final Log log = LogFactory.getLog(GoogleProvisioningConnectorConfig.class);
    private Properties configs;

    public GoogleProvisioningConnectorConfig(Properties configs) {
        this.configs = configs;
    }

    List<String> getRequiredAttributeNames() {
        List<String> requiredAttributeList = new ArrayList<>();
        String requiredAttributes = this.configs.getProperty(GoogleConnectorConstants.PropertyConfig.REQUIRED_FIELDS);
        if (StringUtils.isNotBlank(requiredAttributes)) {
            requiredAttributeList = Arrays.asList(requiredAttributes.split(IdentityProvisioningConstants.PropertyConfig.DELIMATOR));
        }
        return requiredAttributeList;
    }

    String getUserIdClaim() throws IdentityProvisioningException {
        String userIDClaim = this.configs.getProperty(GoogleConnectorConstants.PropertyConfig.USER_ID_CLAIM);
        if (StringUtils.isBlank(userIDClaim)) {
            log.warn("Claim for user id is not defined in config. Using " + GoogleConnectorConstants.ATTRIBUTE_PRIMARYEMAIL + "'s claim instead");

            userIDClaim = this.configs.getProperty(GoogleConnectorConstants.ATTRIBUTE_PRIMARYEMAIL);
        }
        if (StringUtils.isBlank(userIDClaim)) {
            log.warn("Claim for user id is set to default value : " + "http://wso2.org/claims/streetaddress");

            //TODO make userIDClaim read from UI\DB
            userIDClaim = "http://wso2.org/claims/streetaddress";
        }


        if (StringUtils.isBlank(userIDClaim)) {
            log.error("UserId cannot mapped to a claim");
            throw new IdentityProvisioningException("UserId cannot mapped to a claim");
        }

        if (log.isDebugEnabled()) {
            log.debug("Mapped claim for UserId is : " + userIDClaim);
        }
        return userIDClaim;
    }

    public String getValue(String key) {
        return this.configs.getProperty(key);
    }
}
