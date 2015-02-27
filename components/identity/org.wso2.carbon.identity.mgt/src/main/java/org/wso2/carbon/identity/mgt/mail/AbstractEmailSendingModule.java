/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.mgt.mail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.mgt.NotificationSendingModule;
import org.wso2.carbon.identity.mgt.config.Config;
import org.wso2.carbon.identity.mgt.config.ConfigBuilder;
import org.wso2.carbon.identity.mgt.config.ConfigType;
import org.wso2.carbon.identity.mgt.config.StorageType;
import org.wso2.carbon.identity.mgt.util.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * email sending abstract module. This implements the runnable task for sending
 * email.
 * So implementation of this module does not want to worry about threads.
 */
public abstract class AbstractEmailSendingModule extends NotificationSendingModule {

    private static final Log log = LogFactory.getLog(AbstractEmailSendingModule.class);
    protected String NOTIFICATION_TYPE = "EMAIL";
    private Map<String, EmailConfig> emailConfigs = new HashMap<String, EmailConfig>();

    private Config emailConfig;
//
//    public Config getEmailConfig() {
//		return this.emailConfig;
//	}

    @Override
    public String getNotificationType() {
        return NOTIFICATION_TYPE;
    }

    @Override
    public void init() throws Exception {
        loadEmailConfigurations();
    }

    @Override
    public String getNotificationAddress(String userName, int tenantId) {
        return Utils.getEmailAddressForUser(userName, tenantId);
    }

    @Override
    public void notifyUser() {

        EmailConfig emailConfig = null;

        String notification = notificationData.getNotification();

        if (notification != null && notification.trim().length() > 0) {
            emailConfig = emailConfigs.get(notification);
        }

        if (emailConfig == null) {
            emailConfig = new EmailConfig();
        }

//        sendEmail(emailConfig);
        sendEmail();
    }

//    public abstract void sendEmail(EmailConfig emailConfig);

    public abstract void sendEmail();

    /**
     * method to load the adminManagementConfig
     */
    public void loadEmailConfigurations() {

        //Load the configuration
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();

        ConfigBuilder configBuilder = ConfigBuilder.getInstance();
        try {
            this.emailConfig = configBuilder.loadConfiguration(ConfigType.EMAIL, StorageType.REGISTRY, tenantId);
        } catch (Exception e) {
            log.error("Error occurred while getting configuration data", e);
        }

//        String confXml = CarbonUtils.getCarbonConfigDirPath() + File.separator +
//                IdentityMgtConstants.EMAIL_CONF_DIRECTORY + File.separator +
//                IdentityMgtConstants.EMAIL_ADMIN_CONF_FILE;
//
//        File configfile = new File(confXml);
//        if (!configfile.exists()) {
//            log.warn("Email Configuration File is not present at: " + confXml);
//        }
//
//        XMLStreamReader parser = null;
//        InputStream stream = null;
//
//        try {
//            stream = new FileInputStream(configfile);
//            parser = XMLInputFactory.newInstance().createXMLStreamReader(stream);
//            StAXOMBuilder builder = new StAXOMBuilder(parser);
//            OMElement documentElement = builder.getDocumentElement();
//            Iterator iterator = documentElement.getChildElements();
//            while(iterator.hasNext()){
//                OMElement omElement = (OMElement) iterator.next();
//                String configType = omElement.getAttributeValue(new QName("type"));
//                if(configType != null && configType.trim().length() > 0){
//                    emailConfigs.put(configType, loadEmailConfig(omElement));
//                }
//
//            }
//        } catch (XMLStreamException e) {
//            log.warn("Error while loading email config. using default configuration");
//        } catch (FileNotFoundException e) {
//            log.warn("Error while loading email config. using default configuration");
//        } finally {
//            try {
//                if(parser != null){
//                    parser.close();
//                }
//                if(stream != null){
//                    stream.close();
//                }
//            } catch (XMLStreamException e) {
//                log.error("Error while closing XML stream");
//            } catch (IOException e) {
//                log.error("Error while closing input stream");
//            }
//        }

    }

    /**
     * Loading the EmailConfig details from the given config file,
     *
     * @param configElement
     * @return - admin management config
     */
//    private EmailConfig loadEmailConfig(OMElement configElement) {
//        EmailConfig config = new EmailConfig();
//        Iterator it = configElement.getChildElements();
//        while (it.hasNext()) {
//            OMElement element = (OMElement) it.next();
//            if ("subject".equals(element.getLocalName())) {
//                config.setSubject(element.getText());
//            } else if ("body".equals(element.getLocalName())) {
//                config.setEmailBody(element.getText());
//            } else if ("footer".equals(element.getLocalName())) {
//                config.setEmailFooter(element.getText());
//            } else if ("targetEpr".equals(element.getLocalName())) {
//                config.setTargetEpr(element.getText());
//            } else if ("redirectPath".equals(element.getLocalName())) {
//                config.setRedirectPath(element.getText());
//            }
//        }
//        return config;
//    }
}
