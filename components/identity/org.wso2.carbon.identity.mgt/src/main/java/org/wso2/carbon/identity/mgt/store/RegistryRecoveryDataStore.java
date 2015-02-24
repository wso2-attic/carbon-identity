package org.wso2.carbon.identity.mgt.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.dto.UserRecoveryDataDO;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.Properties;

/**
 *
 */
public class RegistryRecoveryDataStore implements UserRecoveryDataStore {

    private static final Log log = LogFactory.getLog(RegistryRecoveryDataStore.class);

    @Override
    public void store(UserRecoveryDataDO recoveryDataDO) throws IdentityException {

        try {
            Registry registry = IdentityMgtServiceComponent.getRegistryService().
                    getConfigSystemRegistry(PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId());
            Resource resource = registry.newResource();
            resource.setProperty(SECRET_KEY, recoveryDataDO.getSecret());
            resource.setProperty(USER_ID, recoveryDataDO.getUserName());
            resource.setProperty(EXPIRE_TIME, recoveryDataDO.getExpireTime());
            resource.setVersionableChange(false);
            String confirmationKeyPath = IdentityMgtConstants.IDENTITY_MANAGEMENT_DATA + "/" + recoveryDataDO.getCode();
            registry.put(confirmationKeyPath, resource);
        } catch (RegistryException e) {
            log.error(e);
            throw new IdentityException("Error while persisting user recovery data for user : " +
                    recoveryDataDO.getUserName());
        }

    }

    @Override
    public void store(UserRecoveryDataDO[] recoveryDataDOs) throws IdentityException {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public UserRecoveryDataDO load(String code) throws IdentityException {

        Registry registry = null;
        UserRecoveryDataDO dataDO = new UserRecoveryDataDO();

        try {

            registry = IdentityMgtServiceComponent.getRegistryService().
                    getConfigSystemRegistry(PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId());

            registry.beginTransaction();

            String secretKeyPath = IdentityMgtConstants.IDENTITY_MANAGEMENT_DATA +
                    RegistryConstants.PATH_SEPARATOR + code;
            if (registry.resourceExists(secretKeyPath)) {
                Resource resource = registry.get(secretKeyPath);
                Properties props = resource.getProperties();
                for (Object o : props.keySet()) {
                    String key = (String) o;
                    if (key.equals(USER_ID)) {
                        dataDO.setUserName(resource.getProperty(key));
                    } else if (key.equals(SECRET_KEY)) {
                        dataDO.setSecret(resource.getProperty(key));
                    } else if (key.equals(EXPIRE_TIME)) {
                        String time = resource.getProperty(key);

                        if (System.currentTimeMillis() > Long.parseLong(time)) {
                            dataDO.setValid(false);
                            break;
                        } else {
                            dataDO.setValid(true);
                        }
                    }
                }
                registry.delete(resource.getPath());
            } else {
                return null;
            }
        } catch (RegistryException e) {
            log.error(e);
            throw new IdentityException("Error while loading user recovery data for code : " + code);
        } finally {
            if (registry != null) {
                try {
                    registry.commitTransaction();
                } catch (RegistryException e) {
                    log.error("Error while processing registry transaction", e);
                }
            }
        }

        return dataDO;
    }

    @Override
    public void invalidate(UserRecoveryDataDO recoveryDataDO) throws IdentityException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void invalidate(String userId, int tenantId) throws IdentityException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserRecoveryDataDO[] load(String userName, int tenantId) throws IdentityException {
        return new UserRecoveryDataDO[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
