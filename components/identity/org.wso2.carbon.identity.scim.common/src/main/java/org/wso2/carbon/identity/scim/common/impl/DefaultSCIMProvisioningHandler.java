package org.wso2.carbon.identity.scim.common.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.scim.common.config.SCIMProvisioningConfigManager;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.charon.core.config.SCIMConsumer;
import org.wso2.charon.core.config.SCIMProvider;
import org.wso2.charon.core.objects.Group;
import org.wso2.charon.core.objects.SCIMObject;
import org.wso2.charon.core.objects.User;
import org.wso2.charon.core.schema.SCIMConstants;
import org.wso2.charon.core.provisioning.ProvisioningHandler;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultSCIMProvisioningHandler implements ProvisioningHandler {

    private static Log logger = LogFactory.getLog(DefaultSCIMProvisioningHandler.class.getName());
    protected SCIMProvisioningConfigManager provisioningManager;

    //to make provisioning requests sent to different providers parallel.
    private ExecutorService provisioningThreadPool = Executors.newCachedThreadPool();

    private int objectType;
    //variables used in runnable's run method in a particular instance of the object:
    private String consumerName;
    private SCIMObject objectToBeProvisioned;
    private int provisioningMethod;
    private Map<String, Object> additionalProvisioningInformation;

    public DefaultSCIMProvisioningHandler(String consumerId, User user, int httpMethod,
                                          Map<String, Object> additionalInformation) {
        initConfigManager();
        objectType = SCIMConstants.USER_INT;
        consumerName = consumerId;
        objectToBeProvisioned = user;
        provisioningMethod = httpMethod;
        additionalProvisioningInformation = additionalInformation;
    }

    public DefaultSCIMProvisioningHandler(String consumerId, Group group, int httpMethod,
                                          Map<String, Object> additionalInformation) {
        initConfigManager();
        objectType = SCIMConstants.GROUP_INT;
        consumerName = consumerId;
        objectToBeProvisioned = group;
        provisioningMethod = httpMethod;
        additionalProvisioningInformation = additionalInformation;
    }

    public void initConfigManager() {
        this.provisioningManager = SCIMProvisioningConfigManager.getInstance();
    }

    public void provision() {

        //get all the providers to whom the provisioning should be done, for this particular consumer
        //SCIMConsumer consumer = provisioningManager.getSCIMConfig().getConsumerProcessed(consumerName);

        SCIMConsumer consumer = null;
        try {
            consumer = provisioningManager.getSCIMConsumerConfig(consumerName);
        } catch (IdentitySCIMException e) {
            //if exception occurred, log the error and return
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            return;
        }

        //iterate through all the providers and do provisioning
        Map<String, SCIMProvider> scimProviders = consumer.getScimProviders();

        for (SCIMProvider scimProviderEntry : scimProviders.values()) {
            if (SCIMConstants.USER_INT == objectType) {
                provisioningThreadPool.submit(new ProvisioningClient(
                        scimProviderEntry, (User) objectToBeProvisioned, provisioningMethod,
                        additionalProvisioningInformation));
            } else if (SCIMConstants.GROUP_INT == objectType) {
                provisioningThreadPool.submit(new ProvisioningClient(
                        scimProviderEntry, (Group) objectToBeProvisioned, provisioningMethod,
                        additionalProvisioningInformation));
            }
        }
    }

    public void setProperties(Map<String, Object> propertiesMap) {
        additionalProvisioningInformation = propertiesMap;
    }

    public void setProvisioningObjectType(int objectType) {
        this.objectType = objectType;
    }

    public void setProvisioningObject(Object object) {
        objectToBeProvisioned = (SCIMObject) object;
    }

    public void setProvisioningMethod(int provisioningMethod) {
        this.provisioningMethod = provisioningMethod; 
    }

    public void setProvisioningConsumer(String consumerName) {
        this.consumerName = consumerName;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public void run() {
        this.provision();
    }
}