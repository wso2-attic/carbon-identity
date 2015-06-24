/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.core.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.IdentityRegistryResources;
import org.wso2.carbon.identity.core.model.OpenIDUserRPDO;
import org.wso2.carbon.registry.core.Association;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.utils.Transaction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class OpenIDUserRPDAO extends AbstractDAO<OpenIDUserRPDO> {

    protected Log log = LogFactory.getLog(OpenIDUserRPDAO.class);

    /**
     * @param registry
     */
    public OpenIDUserRPDAO(Registry registry) {
        this.registry = registry;
    }

    /**
     * Creates a Relying Party and asscociates it with the User
     *
     * @param oprp
     * @throws IdentityException
     */
    public void create(OpenIDUserRPDO oprp) throws IdentityException {
        String path = null;
        Resource resource = null;
        Collection userResource = null;

        if (log.isDebugEnabled()) {
            log.debug("Creating an OpenID user relying party");
        }

        try {
            path = IdentityRegistryResources.OPENID_USER_RP_ROOT + oprp.getUuid();
            if (registry.resourceExists(path)) {
                log.info("OpenID user RP trying to create already exists");
                return;
            }
            /*
			 * rp = getFirstObjectWithPropertyValue(IdentityRegistryResources.
			 * OPENID_USER_RP_ROOT,
			 * IdentityRegistryResources.PROP_RP_URL, oprp.getRpUrl());
			 * 
			 * if (rp != null) {
			 * log.info("OpenID user RP trying to create already exists");
			 * return;
			 * }
			 */

            resource = registry.newResource();
            resource.setProperty(IdentityRegistryResources.PROP_RP_URL, oprp.getRpUrl());
            resource.setProperty(IdentityRegistryResources.PROP_IS_TRUSTED_ALWAYS,
                    Boolean.toString(oprp.isTrustedAlways()));
            resource.setProperty(IdentityRegistryResources.PROP_VISIT_COUNT,
                    Integer.toString(oprp.getVisitCount()));
            resource.setProperty(IdentityRegistryResources.PROP_LAST_VISIT,
                    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(oprp.getLastVisit()));
            resource.setProperty(IdentityRegistryResources.PROP_DEFAULT_PROFILE_NAME,
                    oprp.getDefaultProfileName());
            resource.setProperty(IdentityRegistryResources.PROP_USER_ID, oprp.getUserName());

            boolean transactionStarted = Transaction.isStarted();

            try {
                if (!transactionStarted) {
                    registry.beginTransaction();
                }
                registry.put(path, resource);
                if (!registry.resourceExists(RegistryConstants.PROFILES_PATH + oprp.getUserName())) {
                    userResource = registry.newCollection();
                    registry.put(RegistryConstants.PROFILES_PATH + oprp.getUserName(), userResource);
                } else {
                    userResource =
                            (Collection) registry.get(RegistryConstants.PROFILES_PATH +
                                    oprp.getUserName());
                }
                registry.addAssociation(RegistryConstants.PROFILES_PATH + oprp.getUserName(), path,
                        IdentityRegistryResources.ASSOCIATION_USER_OPENID_RP);
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
                            "Error occured while creating an OpenID user relying party",
                            e);
                }
            }

        } catch (RegistryException e) {
            log.error("Error occured while creating an OpenID user relying party", e);
            throw new IdentityException(
                    "Error occured while creating an OpenID user relying party",
                    e);
        }
    }

    /**
     * Updates the Relying Party if exists, if not, then creates a new Relying
     * Party
     *
     * @param oprp
     * @throws IdentityException
     */
    public void update(OpenIDUserRPDO oprp) throws IdentityException {
        String path = null;
        Resource resource = null;

        if (log.isDebugEnabled()) {
            log.debug("Updating an OpenID user relying party");
        }

        try {
            path = IdentityRegistryResources.OPENID_USER_RP_ROOT + oprp.getUuid();
            if (!registry.resourceExists(path)) {
                log.info("OpenID user RP trying to update does not exist");
                return;
            }

			/*
			 * rp =
			 * getFirstObjectWithPropertyValue(IdentityRegistryResources.
			 * OPENID_USER_RP_ROOT,
			 * IdentityRegistryResources.PROP_RP_URL,
			 * oprp.getRpUrl());
			 * 
			 * if (rp == null) {
			 * log.info("OpenID user RP trying to update does not exist");
			 * return;
			 * }
			 */

            resource = registry.get(path);
            resource.setProperty(IdentityRegistryResources.PROP_RP_URL, oprp.getRpUrl());
            resource.setProperty(IdentityRegistryResources.PROP_IS_TRUSTED_ALWAYS,
                    Boolean.toString(oprp.isTrustedAlways()));
            resource.setProperty(IdentityRegistryResources.PROP_VISIT_COUNT,
                    Integer.toString(oprp.getVisitCount()));
            resource.setProperty(IdentityRegistryResources.PROP_LAST_VISIT,
                    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(oprp.getLastVisit()));
            resource.setProperty(IdentityRegistryResources.PROP_DEFAULT_PROFILE_NAME,
                    oprp.getDefaultProfileName());
            resource.setProperty(IdentityRegistryResources.PROP_USER_ID, oprp.getUserName());
            registry.put(path, resource);

        } catch (RegistryException e) {
            log.error("Error occured while updating an OpenID user relying party", e);
            throw new IdentityException(
                    "Error occured while updating an OpenID user relying party",
                    e);
        }
    }

    /**
     * @param oprp
     * @throws IdentityException
     */
    public void delete(OpenIDUserRPDO oprp) throws IdentityException {
        String path = null;

        if (log.isDebugEnabled()) {
            log.debug("Deleting an OpenID user relying party");
        }

        try {
            path = IdentityRegistryResources.OPENID_USER_RP_ROOT + oprp.getUuid();
            boolean transactionStarted = Transaction.isStarted();
            try {
                if (!transactionStarted) {
                    registry.beginTransaction();
                }
                registry.removeAssociation(RegistryConstants.PROFILES_PATH + oprp.getUserName(),
                        path,
                        IdentityRegistryResources.ASSOCIATION_USER_OPENID_RP);
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
                    throw new IdentityException(
                            "Error occured while deleting an OpenID user relying party",
                            e);
                }
            }

        } catch (RegistryException e) {
            log.error("Error occured while deleting an OpenID user relying party", e);
            throw new IdentityException(
                    "Error occured while deleting an OpenID user relying party",
                    e);
        }
    }

    /**
     * Returns relying party user settings corresponding to a given user name.
     *
     * @param userName Unique user name
     * @param rpUrl    Relying party urlupdateOpenIDUserRPInfo
     * @return A set of OpenIDUserRPDO, corresponding to the provided user name
     * and RP url.
     * @throws IdentityException
     */
    public OpenIDUserRPDO getOpenIDUserRP(String userName, String rpUrl) throws IdentityException {
        OpenIDUserRPDO rp = null;
        Association[] assoc = null;

        if (log.isDebugEnabled()) {
            log.debug("Retreiving OpenID user relying party");
        }

        try {
            if (registry.resourceExists(RegistryConstants.PROFILES_PATH + userName)) {
                assoc =
                        registry.getAssociations(RegistryConstants.PROFILES_PATH + userName,
                                IdentityRegistryResources.ASSOCIATION_USER_OPENID_RP);
                for (Association association : assoc) {
                    rp = resourceToObject(registry.get(association.getDestinationPath()));
                    if (rp.getRpUrl().equals(rpUrl)) {
                        return rp;
                    }
                }
            }
        } catch (RegistryException e) {
            log.error("Error occured while retreiving OpenID user relying party", e);
            throw new IdentityException("Error occured while retreiving OpenID user relying party",
                    e);
        }
        return rp;
    }

    /**
     * @return
     * @throws IdentityException
     */
    public OpenIDUserRPDO[] getAllOpenIDUserRP() throws IdentityException {
        List<OpenIDUserRPDO> rpdos = null;
        Collection rps = null;
        String[] children = null;

        if (log.isDebugEnabled()) {
            log.debug("Retrieving all OP RPs");
        }

        try {
            rps = (Collection) registry.get(IdentityRegistryResources.OPENID_USER_RP_ROOT);
            rpdos = new ArrayList<OpenIDUserRPDO>();

            if (rps != null && rps.getChildCount() > 0) {
                children = rps.getChildren();
                for (String child : children) {
                    rpdos.add(resourceToObject(registry.get(child)));
                }
            }

        } catch (RegistryException e) {
            log.error("Error occured while retreiving all OP RPs", e);
            throw new IdentityException("Error occured while retreiving all OP RPs", e);
        }

        return rpdos.toArray(new OpenIDUserRPDO[rpdos.size()]);
    }

    /**
     * Returns relying party user settings corresponding to a given user name.
     *
     * @param userName Unique user name
     * @return OpenIDUserRPDO, corresponding to the provided user name and RP
     * url.
     * @throws IdentityException
     */
    public OpenIDUserRPDO[] getOpenIDUserRPs(String userName) throws IdentityException {
        List<OpenIDUserRPDO> lst = null;
        Association[] assoc = null;
        OpenIDUserRPDO rp = null;

        if (log.isDebugEnabled()) {
            log.debug("Retreiving OpenID user relying parties");
        }

        try {
            lst = new ArrayList<OpenIDUserRPDO>();

            if (registry.resourceExists(RegistryConstants.PROFILES_PATH + userName)) {
                assoc =
                        registry.getAssociations(RegistryConstants.PROFILES_PATH + userName,
                                IdentityRegistryResources.ASSOCIATION_USER_OPENID_RP);
                for (Association association : assoc) {
                    rp = resourceToObject(registry.get(association.getDestinationPath()));
                    rp.setUserName(userName);
                    lst.add(rp);
                }
            }
        } catch (RegistryException e) {
            log.error("Error occured while retreiving OpenID user relying parties", e);
            throw new IdentityException(
                    "Error occured while retreiving OpenID user relying parties",
                    e);
        }
        return lst.toArray(new OpenIDUserRPDO[lst.size()]);
    }

    /**
     * Returns the default user profile corresponding to the given user name and
     * the RP URL.
     *
     * @param userName Unique user name
     * @param rpUrl    Relying party URL
     * @return Default user profile
     * @throws IdentityException
     */
    public String getOpenIDDefaultUserProfile(String userName, String rpUrl)
            throws IdentityException {
        OpenIDUserRPDO oprp = null;

        if (log.isDebugEnabled()) {
            log.debug("Retreiving OpenID default user profile for user " + userName);
        }

        oprp = getOpenIDUserRP(userName, rpUrl);
        return oprp.getDefaultProfileName();
    }

    /**
     * Returns user name,number of total visits, last login time and OpenID, of
     * all the users who at
     * least used his OpenID once.
     *
     * @return user data
     */
    protected OpenIDUserRPDO resourceToObject(Resource resource) {
        OpenIDUserRPDO rp = null;

        if (resource != null) {
            rp = new OpenIDUserRPDO();
            String path = resource.getPath();
            String[] values = path.split("/");
            String uuid = values[values.length - 1];

            rp.setUuid(uuid);
            rp.setRpUrl(resource.getProperty(IdentityRegistryResources.PROP_RP_URL));
            rp.setTrustedAlways(Boolean.parseBoolean(resource.getProperty(IdentityRegistryResources.PROP_IS_TRUSTED_ALWAYS)));
            rp.setVisitCount(Integer.parseInt(resource.getProperty(IdentityRegistryResources.PROP_VISIT_COUNT)));
            try {
                rp.setLastVisit(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(resource.getProperty(IdentityRegistryResources.PROP_LAST_VISIT)));
            } catch (ParseException e) {
                if (log.isDebugEnabled()) {
                    log.error("Error while parsing resourceToObject", e);
                }
            }
            rp.setDefaultProfileName(resource.getProperty(IdentityRegistryResources.PROP_DEFAULT_PROFILE_NAME));
            rp.setUserName(resource.getProperty(IdentityRegistryResources.PROP_USER_ID));
        }
        return rp;
    }
}