package org.wso2.carbon.identity.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.mgt.policy.PolicyEnforcer;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.identity.mgt.store.UserRecoveryDataStore;
import org.wso2.carbon.identity.mgt.policy.PolicyRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * encapsulates global config data belongs to all the tenants
 */
public class IdentityMgtConfigGlobal {

    private static final Log log = LogFactory.getLog(IdentityMgtConfigGlobal.class);
    private static IdentityMgtConfigGlobal identityMgtConfigGlobal;
    private UserIdentityDataStore userIdentityDataStore;
    private UserRecoveryDataStore userRecoveryDataStore;
    private PolicyRegistry policyRegistry = new PolicyRegistry();

    /*
     * Define the pattern of the configuration file. Assume following
     * pattern in config.
     * Eg. Password.policy.extensions.1.min.length=6
     */
    private Pattern propertyPattern = Pattern.compile("(\\.\\d\\.)");

    public static IdentityMgtConfigGlobal getInstance() {

        if (identityMgtConfigGlobal == null) {
            identityMgtConfigGlobal = new IdentityMgtConfigGlobal();
        }
        return identityMgtConfigGlobal;
    }

    public PolicyRegistry getPolicyRegistry() {
        return policyRegistry;
    }

    public void setPolicyRegistry(PolicyRegistry policyRegistry) {
        this.policyRegistry = policyRegistry;
    }

    public UserIdentityDataStore getUserIdentityDataStore() {
        return userIdentityDataStore;
    }

    public void setUserIdentityDataStore(UserIdentityDataStore userIdentityDataStore) {
        this.userIdentityDataStore = userIdentityDataStore;
    }

    public UserRecoveryDataStore getUserRecoveryDataStore() {
        return userRecoveryDataStore;
    }

    public void setUserRecoveryDataStore(UserRecoveryDataStore userRecoveryDataStore) {
        this.userRecoveryDataStore = userRecoveryDataStore;
    }


    public void setGlobalConfiguration(Properties properties) {
        // Load the configuration for Password.policy.extensions.
        loadPolicyExtensions(properties, IdentityMgtConstants.PropertyConfig.PASSWORD_POLICY_EXTENSIONS);
        setUserIdentityDataStore(properties);
        setUserRecoveryDataStore(properties);
    }

    /**
     * This method is used to load the policies declared in the configuration.
     *
     * @param properties    Loaded properties
     * @param extensionType Type of extension
     */
    private void loadPolicyExtensions(Properties properties, String extensionType) {

        // First property must start with 1.
        int count = 1;
        String className = null;
        int size = 0;
        if (properties != null) {
            size = properties.size();
        }
        while (size > 0) {
            className = properties.getProperty(extensionType + "." + count);
            if (className == null) {
                count++;
                size--;
                continue;
            }
            try {
                Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);

                PolicyEnforcer policy = (PolicyEnforcer) clazz.newInstance();
                policy.init(getParameters(properties, extensionType, count));

                this.policyRegistry.addPolicy((PolicyEnforcer) policy);
                count++;
                size--;
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SecurityException e) {
                log.error("Error while loading password policies " + className, e);
            }
        }

    }

    private void setUserIdentityDataStore(Properties properties) {
        String dataPersistModule = properties.
                getProperty(IdentityMgtConstants.PropertyConfig.EXTENSION_USER_DATA_STORE);
        if (dataPersistModule != null && dataPersistModule.trim().length() > 0) {
            try {
                Class clazz = Thread.currentThread().getContextClassLoader().loadClass(dataPersistModule);
                setUserIdentityDataStore((UserIdentityDataStore) clazz.newInstance());
            } catch (Exception e) {
                log.error("Error while loading user identity data persist class. " + dataPersistModule +
                        " Default module would be used", e);
            }
        }
    }

    private void setUserRecoveryDataStore(Properties properties) {
        String dataPersistModule = properties.
                getProperty(IdentityMgtConstants.PropertyConfig.EXTENSION_USER_DATA_STORE);
        String recoveryPersistModule = properties.
                getProperty(IdentityMgtConstants.PropertyConfig.EXTENSION_USER_RECOVERY_DATA_STORE);
        if (dataPersistModule != null && dataPersistModule.trim().length() > 0) {
            try {
                Class clazz = Thread.currentThread().getContextClassLoader().loadClass(recoveryPersistModule);
                setUserRecoveryDataStore((UserRecoveryDataStore) clazz.newInstance());
            } catch (Exception e) {
                log.error("Error while loading user recovery data persist class. " + dataPersistModule +
                        " Default module would be used", e);
            }
        }
    }

    /**
     * This utility method is used to get the parameters from the configuration
     * file for a given policy extension.
     *
     * @param prop         - properties
     * @param extensionKey - extension key which is defined in the
     *                     IdentityMgtConstants
     * @param sequence     - property sequence number in the file
     * @return Map of parameters with key and value from the configuration file.
     */
    private Map<String, String> getParameters(Properties prop, String extensionKey, int sequence) {

        Set<String> keys = prop.stringPropertyNames();

        Map<String, String> keyValues = new HashMap<String, String>();

        for (String key : keys) {
            // Get only the provided extensions.
            // Eg.
            // Password.policy.extensions.1
            if (key.contains(extensionKey + "." + String.valueOf(sequence))) {

                Matcher m = propertyPattern.matcher(key);

                // Find the .1. pattern in the property key.
                if (m.find()) {
                    int searchIndex = m.end();

					/*
                     * Key length is > matched pattern's end index if it has
					 * parameters
					 * in the config file.
					 */
                    if (key.length() > searchIndex) {
                        String propKey = key.substring(searchIndex);
                        String propValue = prop.getProperty(key);
                        keyValues.put(propKey, propValue);
                    }
                }

            }
        }

        return keyValues;
    }

}
