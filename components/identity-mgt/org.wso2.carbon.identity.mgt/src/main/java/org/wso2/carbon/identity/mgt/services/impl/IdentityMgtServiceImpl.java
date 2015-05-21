package org.wso2.carbon.identity.mgt.services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.mgt.beans.TenantConfigBean;
import org.wso2.carbon.identity.mgt.cache.CacheBackedConfig;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.identity.mgt.handler.EventHandler;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.identity.mgt.services.IdentityMgtService;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.*;
import java.util.*;

public class IdentityMgtServiceImpl implements IdentityMgtService{

    Log log = LogFactory.getLog(IdentityMgtServiceImpl.class);

    @Override
    public Properties addConfigurations(int tenantId) {

        Properties properties = new Properties();
        InputStream inStream = null;

        // key - property name
        // Value - property value
        HashMap<String, String> configurationDetails = new HashMap<String, String>();


        File pipConfigXml = new File(CarbonUtils.getCarbonSecurityConfigDirPath(),
                IdentityMgtConstants.PropertyConfig.CONFIG_FILE_NAME);
        if (pipConfigXml.exists()) {
            try {
                inStream = new FileInputStream(pipConfigXml);
                properties.load(inStream);

                Enumeration enuKeys = properties.keys();
                while (enuKeys.hasMoreElements()) {
                    String key = (String) enuKeys.nextElement();
                    String value = properties.getProperty(key);
                    configurationDetails.put(key, value);
                }

                TenantConfigBean tenantConfigBean = new TenantConfigBean(tenantId, configurationDetails);

                CacheBackedConfig cacheBackedConfig = new CacheBackedConfig();
                cacheBackedConfig.addConfig(tenantConfigBean);

            } catch (FileNotFoundException e) {
                log.error("Can not load identity-mgt properties file ", e);
            } catch (IOException e) {
                log.error("Can not load identity-mgt properties file ", e);
            } finally {
                if(inStream != null){
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        log.error("Error while closing stream ", e);
                    }
                }
            }
        }

        return  properties;

    }

    @Override
    public Properties getConfigurations(int tenantId) {

        Properties properties = new Properties();
        InputStream inStream = null;
        HashMap<String, String> configMap = new HashMap<String, String>();

        CacheBackedConfig cacheBackedConfig = new CacheBackedConfig();
        configMap = cacheBackedConfig.getConfig(tenantId);

        Iterator<Map.Entry<String, String>> iterator = configMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> pair = iterator.next();
            properties.setProperty(pair.getKey(), pair.getValue());
            System.out.println(pair.getKey() + " = " + pair.getValue());
/*            iterator.remove();*/
        }

        return properties;
    }

    @Override
    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws UserStoreException {

        //get the event handlers registered for the event name

        List<EventHandler> eventHandlerList = IdentityMgtServiceComponent.eventHandlerList;
        boolean returnValue = true;
        for (final EventHandler handler : eventHandlerList) {
            //called the isRegistered for each registered handlers by the event name
            //so in each handler it checks the list with event names
            if (handler.isRegistered(identityMgtEvent)) {
                //if the event name exists(if isRegistered() returns true), called the handleEvent of that handler by passing the IdentityMgtEvent object
                returnValue = handler.handleEvent(identityMgtEvent);
            }
        }
        return returnValue;
    }
}
