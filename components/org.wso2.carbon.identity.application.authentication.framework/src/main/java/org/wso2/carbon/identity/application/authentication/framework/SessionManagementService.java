/*
 *Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.application.authentication.framework.cache.SessionContextCache;
import org.wso2.carbon.identity.application.authentication.framework.exception.SessionManagementException;
import org.wso2.carbon.identity.application.authentication.framework.model.UserSessionInfo;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.ArrayList;

public class SessionManagementService extends AbstractAdmin {
    private static Log log = LogFactory.getLog(SessionManagementService.class);

    /**
     * Get All Session Details.
     *
     * @return sessionInfo object arraylist.
     */
    public ArrayList<UserSessionInfo> getAllSessionInfo() throws SessionManagementException {
        ArrayList<UserSessionInfo> userSessionInfoList = null;
        try {
            userSessionInfoList = SessionContextCache.getInstance(0).getSessionDetails();
        } catch (Exception e) {
            String errorMsg = "Error is occurred while getting session details ";
            log.error(errorMsg, e);
            throw new SessionManagementException(errorMsg, e);
        }
        return userSessionInfoList;
    }

    /**
     * Remove sessions for a given user name.
     *
     * @param userName         String.
     * @param userStoreDomain  String.
     * @param tenantDomainName String.
     */
    public void removeSession(String userName, String userStoreDomain, String tenantDomainName)
            throws SessionManagementException {
        try {
            SessionContextCache.getInstance(0).removeSessionDetailsFromDbAndCache(
                    userName, userStoreDomain, tenantDomainName);
        } catch (Exception e) {
            String errorMsg = "Error is occurred while killing sessions ";
            log.error(errorMsg, e);
            throw new SessionManagementException(errorMsg, e);
        }
    }
}
