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

package org.wso2.carbon.identity.oauth.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.event.OauthEventListener;
import org.wso2.carbon.registry.api.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.List;

public class OAuthComponentServiceHolder {

    private static OAuthComponentServiceHolder instance = new OAuthComponentServiceHolder();
    private RegistryService registryService;
    private RealmService realmService;
    private List<OauthEventListener> oauthEventListeners;
    private static Log log = LogFactory.getLog(OAuthComponentServiceHolder.class);

    private OAuthComponentServiceHolder(){

    }

    public static OAuthComponentServiceHolder getInstance() {

        return instance;
    }

    public RegistryService getRegistryService() {

        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {

        this.registryService = registryService;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    public void addOauthEventListener(OauthEventListener oauthEventListener) {

        if (oauthEventListeners == null) {
            oauthEventListeners = new ArrayList<>();
        }
        oauthEventListeners.add(oauthEventListener);
    }

    public void removeOauthEventListener(OauthEventListener oauthEventListener) {

        if (oauthEventListeners != null && oauthEventListener != null) {
            boolean isRemoved = oauthEventListeners.remove(oauthEventListener);
            if (!isRemoved) {
                log.warn(oauthEventListener.getClass().getName() + " had not been registered as a listener");
            }
        }
    }

    public List<OauthEventListener> getOauthEventListeners() {

        if (oauthEventListeners == null) {
            oauthEventListeners = new ArrayList<>();
        }
        return oauthEventListeners;
    }
}
