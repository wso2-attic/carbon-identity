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

package org.wso2.carbon.identity.application.mgt.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.dao.SAMLApplicationDAO;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.registry.core.Registry;

public class SAMLApplicationDAOImpl implements SAMLApplicationDAO {

    Log log = LogFactory.getLog(SAMLApplicationDAOImpl.class);

    /*SAMLSSOConfigService samlService = new SAMLSSOConfigService();*/

    public void removeServiceProviderConfiguration(String issuer) throws IdentityApplicationManagementException {
        try {
            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
            Registry configSystemRegistry = (Registry) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                    getRegistry(RegistryType.SYSTEM_CONFIGURATION);
            persistenceManager.removeServiceProvider(configSystemRegistry, issuer);
        } catch (IdentityException e) {
            throw new IdentityApplicationManagementException("Error while deleting SAML issuer " + e.getMessage());
        }
    }

}
