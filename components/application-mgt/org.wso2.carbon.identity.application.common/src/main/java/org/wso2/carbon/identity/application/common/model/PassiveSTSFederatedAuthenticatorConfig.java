/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.common.model;

import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;

public class PassiveSTSFederatedAuthenticatorConfig extends FederatedAuthenticatorConfig {

    /**
     *
     */
    private static final long serialVersionUID = 2265391150645470497L;
    private String idpEntityId;

    public PassiveSTSFederatedAuthenticatorConfig(FederatedAuthenticatorConfig federatedAuthenticatorConfig) {
        for (Property property : federatedAuthenticatorConfig.getProperties()) {
            if (IdentityApplicationConstants.Authenticator.PassiveSTS.IDENTITY_PROVIDER_ENTITY_ID.equals(property.getName())) {
                idpEntityId = property.getValue();
            }
        }
    }

    @Override
    public boolean isValid() {

        if (IdentityApplicationManagementUtil.getProperty(properties,
                IdentityApplicationConstants.Authenticator.PassiveSTS.IDENTITY_PROVIDER_URL) != null
                && !"".equals(IdentityApplicationManagementUtil.getProperty(properties,
                IdentityApplicationConstants.Authenticator.PassiveSTS.IDENTITY_PROVIDER_URL))) {
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return IdentityApplicationConstants.Authenticator.PassiveSTS.NAME;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public void setIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }
}
