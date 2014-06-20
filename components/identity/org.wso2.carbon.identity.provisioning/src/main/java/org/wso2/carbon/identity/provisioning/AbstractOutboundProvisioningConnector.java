/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.provisioning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;

public abstract class AbstractOutboundProvisioningConnector implements Serializable {

    private static final long serialVersionUID = 2196864101772627178L;
    
    protected boolean jitProvisioningEnabled;

    /**
     * 
     * 
     * @param provisioningProperties
     * @throws IdentityProvisioningException
     */
    public abstract void init(Property[] provisioningProperties)
            throws IdentityProvisioningException;

    /**
     * 
     * @param provisioningEntity
     * @throws IdentityProvisioningException
     */
    public abstract ProvisionedIdentifier provision(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException;

    /**
     * override only if needed - if claims are controlled by the identity provider, this will return
     * null. If it is connector specific this must return the corresponding claim dialect.
     * 
     * @return
     * @throws IdentityProvisioningException
     */
    public String getClaimDialectUri() throws IdentityProvisioningException {
        return null;
    }
    
    /**
     * 
     * @return
     * @throws IdentityProvisioningException
     */
    protected boolean isJitProvisioningEnabled() throws IdentityProvisioningException {
        return jitProvisioningEnabled;
    }

    /**
     * 
     * @param attributeMap
     * @return
     */
    protected List<String> getUserNames(Map<ClaimMapping, List<String>> attributeMap) {
        return ProvisioningUtil.getClaimValues(attributeMap,
                IdentityProvisioningConstants.USERNAME_CLAIM_URI, getUserStoreDomainName());
    }

    /**
     * 
     * @param attributeMap
     * @return
     */
    protected List<String> getGroupNames(Map<ClaimMapping, List<String>> attributeMap) {
        return ProvisioningUtil.getClaimValues(attributeMap,
                IdentityProvisioningConstants.GROUP_CLAIM_URI, getUserStoreDomainName());
    }

    /**
     * 
     * @param attributeMap
     * @return
     */
    protected String getPassword(Map<ClaimMapping, List<String>> attributeMap) {
        List<String> claimValue = ProvisioningUtil.getClaimValues(attributeMap,
                IdentityProvisioningConstants.PASSWORD_CLAIM_URI, getUserStoreDomainName());

        if (claimValue != null && claimValue.size() > 0 && claimValue.get(0) != null) {
            return claimValue.get(0);
        }

        return UUID.randomUUID().toString();

    }

    /**
     * 
     * @param attributeMap
     * @param claimUri
     * @return
     */
    protected Map<String, String> getSingleValuedClaims(Map<ClaimMapping, List<String>> attributeMap) {

        Map<String, String> claimValues = new HashMap<String, String>();

        for (Map.Entry<ClaimMapping, List<String>> entry : attributeMap.entrySet()) {
            ClaimMapping mapping = entry.getKey();
            if (mapping.getRemoteClaim() != null && mapping.getRemoteClaim().getClaimUri() != null) {
                String claimUri = mapping.getRemoteClaim().getClaimUri();

                if (!(IdentityProvisioningConstants.GROUP_CLAIM_URI.equals(claimUri)
                        || IdentityProvisioningConstants.PASSWORD_CLAIM_URI.equals(claimUri) || IdentityProvisioningConstants.USERNAME_CLAIM_URI
                            .equals(claimUri))) {
                    if (entry.getValue() != null && entry.getValue().get(0) != null) {
                        claimValues.put(claimUri, entry.getValue().get(0));
                    } else {
                        claimValues.put(claimUri, mapping.getDefaultValue());
                    }
                }
            }
        }

        return claimValues;
    }

    /**
     * 
     * @return
     */
    protected String getUserStoreDomainName() {
        // return null by default. concrete implementations can override this value whenever
        // required.
        return null;
    }

}
