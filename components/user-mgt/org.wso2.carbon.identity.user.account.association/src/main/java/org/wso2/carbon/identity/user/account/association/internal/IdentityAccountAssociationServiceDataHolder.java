/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.user.account.association.internal;

import org.osgi.framework.BundleContext;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.listener.UserStoreManagerListener;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collection;
import java.util.Map;

public class IdentityAccountAssociationServiceDataHolder {

    private BundleContext bundleContext;
    private RealmService realmService;
    private Collection<UserStoreManagerListener> userStoreManagerListenerCollection;
    private Collection<UserOperationEventListener> userOperationEventListenerCollection;
    private Map<Integer, UserStoreManagerListener> userStoreManagerListeners;
    private Map<Integer, UserOperationEventListener> userOperationEventListeners;

    private static IdentityAccountAssociationServiceDataHolder instance = new
            IdentityAccountAssociationServiceDataHolder();

    private IdentityAccountAssociationServiceDataHolder() {

    }

    public static IdentityAccountAssociationServiceDataHolder getInstance() {

        return instance;
    }

    public void setBundleContext(BundleContext bundleContext) {

        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {

        return this.bundleContext;
    }

    public void setRealmService(RealmService service) {

        this.realmService = service;
    }

    public RealmService getRealmService() {

        return this.realmService;
    }

    public Collection<UserStoreManagerListener> getUserStoreManagerListenerCollection() {

        return userStoreManagerListenerCollection;
    }

    public void setUserStoreManagerListenerCollection(Collection<UserStoreManagerListener>
                                                               userStoreManagerListenerCollection) {

        this.userStoreManagerListenerCollection = userStoreManagerListenerCollection;
    }

    public Collection<UserOperationEventListener> getUserOperationEventListenerCollection() {

        return userOperationEventListenerCollection;
    }

    public void putUserOperationEventListener(Integer id, UserOperationEventListener userOperationEventListener) {

        this.userOperationEventListeners.put(id, userOperationEventListener);
    }

    public UserOperationEventListener removeUserOperationEventListener(Integer id) {

        return this.userOperationEventListeners.remove(id);

    }

    public void setUserOperationEventListenerCollection(Collection<UserOperationEventListener>
                                                                 userOperationEventListenerCollection) {

        this.userOperationEventListenerCollection = userOperationEventListenerCollection;
    }

    public void putUserStoreManagerListener(Integer id, UserStoreManagerListener listener) {

        this.userStoreManagerListeners.put(id, listener);
    }

    public void removeUserStoreManagerListener(Integer id) {

        this.userStoreManagerListeners.remove(id);
    }

    public Map<Integer, UserStoreManagerListener> getUserStoreManagerListeners() {

        return userStoreManagerListeners;
    }

    public void setUserStoreManagerListeners(Map<Integer, UserStoreManagerListener> userStoreManagerListeners) {

        this.userStoreManagerListeners = userStoreManagerListeners;
    }

    public Map<Integer, UserOperationEventListener> getUserOperationEventListeners() {

        return userOperationEventListeners;
    }

    public void setUserOperationEventListeners(Map<Integer, UserOperationEventListener> userOperationEventListeners) {

        this.userOperationEventListeners = userOperationEventListeners;
    }

}
