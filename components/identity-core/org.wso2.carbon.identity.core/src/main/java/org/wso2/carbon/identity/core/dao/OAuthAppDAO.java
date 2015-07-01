/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.core.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.IdentityRegistryResources;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.registry.core.Association;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.utils.Transaction;

import java.util.ArrayList;
import java.util.List;

public class OAuthAppDAO extends AbstractDAO<OAuthAppDO> {

    protected Log log = LogFactory.getLog(OAuthAppDAO.class);

    /**
     * @param registry
     */
    public OAuthAppDAO(Registry registry) {
        this.registry = registry;
    }

    /**
     * @param card
     * @throws IdentityException
     */
    public void createOrUpdateOAuthApp(OAuthAppDO app) throws IdentityException {
        String path = null;
        Resource resource = null;
        Collection userResource = null;

        try {

            path = IdentityRegistryResources.OAUTH_APP_PATH + app.getUserName() + "/"
                    + app.getApplicationName();

            if (registry.resourceExists(path)) {
                resource = registry.get(path);
                resource.removeProperty(IdentityRegistryResources.OAUTH_APP_CALLBACK);
                resource.removeProperty(IdentityRegistryResources.OAUTH_APP_CONSUMER_KEY);
                resource.removeProperty(IdentityRegistryResources.OAUTH_APP_CONSUMER_SECRET);
                resource.removeProperty(IdentityRegistryResources.OAUTH_APP_NAME);
            } else {
                resource = registry.newCollection();
            }

            resource
                    .addProperty(IdentityRegistryResources.OAUTH_APP_CALLBACK, app.getCallbackUrl());
            resource.addProperty(IdentityRegistryResources.OAUTH_APP_CONSUMER_KEY, app
                    .getOauthConsumerKey());
            resource.addProperty(IdentityRegistryResources.OAUTH_APP_CONSUMER_SECRET, app
                    .getOauthConsumerSecret());
            resource
                    .addProperty(IdentityRegistryResources.OAUTH_APP_NAME, app.getApplicationName());

            boolean transactionStarted = Transaction.isStarted();
            try {

                if (!transactionStarted) {
                    registry.beginTransaction();
                }
                registry.put(path, resource);

                if (!registry.resourceExists(RegistryConstants.PROFILES_PATH + app.getUserName())) {
                    userResource = registry.newCollection();
                    registry.put(RegistryConstants.PROFILES_PATH + app.getUserName(), userResource);
                } else {
                    //userResource = (Collection) registry.get(RegistryConstants.PROFILES_PATH
                    //        + app.getUserName());
                }

                registry.addAssociation(RegistryConstants.PROFILES_PATH + app.getUserName(), path,
                        IdentityRegistryResources.ASSOCIATION_USER_OAUTH_APP);
                if (!transactionStarted) {
                    registry.commitTransaction();
                }
            } catch (Exception e) {
                if (!transactionStarted) {
                    registry.rollbackTransaction();
                }
                if (e instanceof RegistryException) {
                    throw (RegistryException) e;
                } else {
                    throw new IdentityException(
                            "Error occured while creating new oauth application", e);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("New oauth application added successfully, " + app.getApplicationName());
            }

        } catch (RegistryException e) {
            log.error("Error occured while creating new oauth application", e);
            throw new IdentityException("Error occured while creating new oauth application", e);
        }
    }

    /**
     * @param card
     * @throws IdentityException
     */
    public void deleteOAuthApp(String userName, String appName) throws IdentityException {
        String path = null;

        try {
            path = IdentityRegistryResources.OAUTH_APP_PATH + userName + "/"
                    + appName;

            if (!registry.resourceExists(path)) {
                if (log.isDebugEnabled()) {
                    String message = "Resource doese not exist for the oauth application being deleted";
                    log.debug(message + appName);
                }
                return;
            }
            boolean transactionStarted = Transaction.isStarted();
            try {

                if (!transactionStarted) {
                    registry.beginTransaction();
                }
                registry.removeAssociation(RegistryConstants.PROFILES_PATH + appName,
                        path, IdentityRegistryResources.ASSOCIATION_USER_OAUTH_APP);
                registry.delete(path);
                if (!transactionStarted) {
                    registry.commitTransaction();
                }
            } catch (Exception e) {
                if (!transactionStarted) {
                    registry.rollbackTransaction();
                }
                if (e instanceof RegistryException) {
                    throw (RegistryException) e;
                } else {
                    throw new IdentityException("Error occured while deleting infromation card", e);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("OAuth app deleted successfully, " + appName);
            }

        } catch (RegistryException e) {
            log.error("Error occured while deleting OAuth app", e);
            throw new IdentityException("Error occured while deleting OAuth app", e);
        }
    }

    /**
     * @return
     * @throws IdentityException
     */
    public OAuthAppDO[] getAllOAuthAppByUser(String userName) throws IdentityException {
        List<OAuthAppDO> apps = null;
        Association[] assoc = null;
        OAuthAppDO oauthApp = null;
        String path = null;

        if (log.isDebugEnabled()) {
            log.debug("Retrieving oauth applications for user " + userName);
        }

        apps = new ArrayList<OAuthAppDO>();
        path = IdentityRegistryResources.OAUTH_APP_PATH + userName;

        try {
            if (registry.resourceExists(path)) {
                assoc = registry.getAssociations(RegistryConstants.PROFILES_PATH + userName,
                        IdentityRegistryResources.ASSOCIATION_USER_OAUTH_APP);
                for (Association association : assoc) {
                    oauthApp = resourceToObject(registry.get(association.getDestinationPath()));
                    oauthApp.setUserName(userName);
                    apps.add(oauthApp);
                }
            }
        } catch (RegistryException e) {
            log.error("Error occured while retreiving oauth applications for" + userName, e);
            throw new IdentityException("Error occured while retreiving oauth applications for "
                    + userName, e);
        }

        return apps.toArray(new OAuthAppDO[apps.size()]);
    }

    /**
     * @param cardId
     * @return
     * @throws IdentityException
     */
    public OAuthAppDO getOAuthApp(String userName, String applicationName) throws IdentityException {
        String path = null;

        if (log.isDebugEnabled()) {
            log.debug("Retrieving OAuth App" + applicationName);
        }

        if (applicationName == null || userName == null) {
            if (log.isDebugEnabled()) {
                log.debug("Null card id provided for retriving oauth application");
            }
            return null;
        }

        path = IdentityRegistryResources.OAUTH_APP_PATH + userName + "/" + applicationName;
        try {
            if (registry.resourceExists(path)) {
                return resourceToObject(registry.get(path));
            }
        } catch (RegistryException e) {
            log.error("Error occured while retreiving oauth application", e);
            throw new IdentityException("Error occured while retreiving oauth application", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    protected OAuthAppDO resourceToObject(Resource resource) {
        OAuthAppDO app = null;

        if (resource != null) {
            app = new OAuthAppDO();
            app.setApplicationName(resource.getProperty(IdentityRegistryResources.OAUTH_APP_NAME));
            app.setCallbackUrl(resource.getProperty(IdentityRegistryResources.OAUTH_APP_CALLBACK));
            app.setOauthConsumerKey(resource
                    .getProperty(IdentityRegistryResources.OAUTH_APP_CONSUMER_KEY));
            app.setOauthConsumerSecret(resource
                    .getProperty(IdentityRegistryResources.OAUTH_APP_CONSUMER_SECRET));
        }

        return app;
    }
}