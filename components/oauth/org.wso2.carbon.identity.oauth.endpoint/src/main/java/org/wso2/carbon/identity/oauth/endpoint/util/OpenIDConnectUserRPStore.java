/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth.endpoint.util;

import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.wso2.carbon.identity.core.dao.OpenIDUserRPDAO;
import org.wso2.carbon.identity.core.model.OpenIDUserRPDO;

/**
 * Stores user consent on applications
 */
public class OpenIDConnectUserRPStore {

    private static final String DEFAULT_PROFILE_NAME = "default";
    private static OpenIDConnectUserRPStore store = new OpenIDConnectUserRPStore();

    private OpenIDConnectUserRPStore() {

    }

    public static OpenIDConnectUserRPStore getInstance() {
        return store;
    }

    /**
     * @param username
     * @param appName
     * @throws OAuthSystemException
     */
    public void putUserRPToStore(String username, String appName, boolean trustedAlways) throws OAuthSystemException {
        OpenIDUserRPDO repDO = new OpenIDUserRPDO();
        repDO.setDefaultProfileName(DEFAULT_PROFILE_NAME);
        repDO.setRpUrl(appName);
        repDO.setUserName(username);
        repDO.setTrustedAlways(trustedAlways);

        OpenIDUserRPDAO dao = new OpenIDUserRPDAO();
        dao.createOrUpdate(repDO);
    }

    /**
     * @param username
     * @param appName
     * @return
     * @throws OAuthSystemException
     */
    public synchronized boolean hasUserApproved(String username, String appName) throws OAuthSystemException {
        OpenIDUserRPDAO dao = new OpenIDUserRPDAO();
        OpenIDUserRPDO rpDO = dao.getOpenIDUserRP(username, appName);
        if (rpDO != null && rpDO.isTrustedAlways()) {
            return true;
        }
        return false;
    }
}
