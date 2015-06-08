/*
<<<<<<< HEAD
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
=======
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
>>>>>>> 9c014534b0589aff73f9b83eb9122ca5a111918f
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
<<<<<<< HEAD
 * http://www.apache.org/licenses/LICENSE-2.0
=======
 *      http://www.apache.org/licenses/LICENSE-2.0
>>>>>>> 9c014534b0589aff73f9b83eb9122ca5a111918f
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.scim.common.config;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonUtils;

import java.util.List;

/**
 * This is the service class for SCIMConfigAdminService which exposes the
 * functionality of
 * managing SCIM Configuration -both globally(per tenant) and individually(per
 * user account)
 */
public class SCIMConfigAdminService {

    public static final Log log = LogFactory.getLog(SCIMConfigAdminService.class);

	/* Global Providers Operations.. */

    public SCIMProviderDTO[] getAllGlobalProviders(String consumerId) throws IdentitySCIMException {
        SCIMProviderDTO[] scimProviderDTOs = new SCIMProviderDTO[0];

        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            List<SCIMProviderDTO> globalProviders =
                    providerDAO.getAllProviders(SCIMCommonUtils.getGlobalConsumerId());
<<<<<<< HEAD
            if (CollectionUtils.isNotEmpty(globalProviders)) {
=======
            if (globalProviders != null && !CollectionUtils.isEmpty(globalProviders)) {
>>>>>>> 9c014534b0589aff73f9b83eb9122ca5a111918f
                scimProviderDTOs = new SCIMProviderDTO[globalProviders.size()];
                int i = 0;
                for (SCIMProviderDTO globalProvider : globalProviders) {
                    scimProviderDTOs[i] = globalProvider;
                    i++;
                }
                return scimProviderDTOs;
            }

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId +
                            " is trying to obtain " +
                            "provisioning configuration of :" +
                            SCIMCommonUtils.getGlobalConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
        return scimProviderDTOs;
    }

    public void addGlobalProvider(String consumerId, SCIMProviderDTO scimProviderDTO)
            throws IdentitySCIMException {

        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            providerDAO.addProvider(SCIMCommonUtils.getGlobalConsumerId(), scimProviderDTO);

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId + " is trying to add " +
                            "provisioning configuration to :" +
                            SCIMCommonUtils.getGlobalConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
    }

    public SCIMProviderDTO getGlobalProvider(String consumerId, String providerId)
            throws IdentitySCIMException {

        SCIMProviderDTO scimProviderDTO = null;
        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            scimProviderDTO =
                    providerDAO.getProvider(SCIMCommonUtils.getGlobalConsumerId(),
                            providerId);

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId +
                            " is trying to obtain " +
                            "provisioning configuration of :" +
                            SCIMCommonUtils.getGlobalConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
        return scimProviderDTO;
    }

    public void updateGlobalProvider(String consumerId, SCIMProviderDTO scimProviderDTO)
            throws IdentitySCIMException {

        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            providerDAO.updateProvider(SCIMCommonUtils.getGlobalConsumerId(), scimProviderDTO);

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId +
                            " is trying to update " +
                            "provisioning configuration to :" +
                            SCIMCommonUtils.getGlobalConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
    }

    public void deleteGlobalProvider(String consumerId, String providerId)
            throws IdentitySCIMException {
        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            providerDAO.deleteProvider(SCIMCommonUtils.getGlobalConsumerId(), providerId);

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId +
                            " is trying to delete " +
                            "provisioning configuration to :" +
                            SCIMCommonUtils.getGlobalConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
    }

    public SCIMProviderDTO[] getAllUserProviders(String consumerId) throws IdentitySCIMException {

        SCIMProviderDTO[] scimProviderDTOs = new SCIMProviderDTO[0];
        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            List<SCIMProviderDTO> globalProviders =
                    providerDAO.getAllProviders(SCIMCommonUtils.getUserConsumerId());
<<<<<<< HEAD
            if (CollectionUtils.isNotEmpty(globalProviders)) {
=======
            if (globalProviders != null && !CollectionUtils.isEmpty(globalProviders)) {
>>>>>>> 9c014534b0589aff73f9b83eb9122ca5a111918f
                scimProviderDTOs = new SCIMProviderDTO[globalProviders.size()];
                int i = 0;
                for (SCIMProviderDTO globalProvider : globalProviders) {
                    scimProviderDTOs[i] = globalProvider;
                    i++;
                }
                return scimProviderDTOs;
            }

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId +
                            " is trying to obtain " +
                            "provisioning configuration of :" +
                            SCIMCommonUtils.getUserConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
        return scimProviderDTOs;
    }

    public void addUserProvider(String consumerId, SCIMProviderDTO scimProviderDTO)
            throws IdentitySCIMException {

        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            providerDAO.addProvider(SCIMCommonUtils.getUserConsumerId(), scimProviderDTO);

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId + " is trying to add " +
                            "provisioning configuration to :" +
                            SCIMCommonUtils.getUserConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
    }

    public SCIMProviderDTO getUserProvider(String consumerId, String providerId)
            throws IdentitySCIMException {

        SCIMProviderDTO scimProviderDTO = null;
        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            scimProviderDTO =
                    providerDAO.getProvider(SCIMCommonUtils.getUserConsumerId(),
                            providerId);

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId +
                            " is trying to obtain " +
                            "provisioning configuration of :" +
                            SCIMCommonUtils.getUserConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
        return scimProviderDTO;
    }

    public void updateUserProvider(String consumerId, SCIMProviderDTO scimProviderDTO)
            throws IdentitySCIMException {

        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            providerDAO.updateProvider(SCIMCommonUtils.getUserConsumerId(), scimProviderDTO);

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId +
                            " is trying to update " +
                            "provisioning configuration to :" +
                            SCIMCommonUtils.getUserConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
    }

    public void deleteUserProvider(String consumerId, String providerId)
            throws IdentitySCIMException {

        if (StringUtils.isEmpty(consumerId) ||
                StringUtils.equals(consumerId, SCIMCommonUtils.getGlobalConsumerId())) {
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            providerDAO.deleteProvider(SCIMCommonUtils.getUserConsumerId(), providerId);

        } else {
            String errorMessage =
                    "Security error: consumer:" + consumerId +
                            " is trying to delete " +
                            "provisioning configuration to :" +
                            SCIMCommonUtils.getUserConsumerId();
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
    }
}
