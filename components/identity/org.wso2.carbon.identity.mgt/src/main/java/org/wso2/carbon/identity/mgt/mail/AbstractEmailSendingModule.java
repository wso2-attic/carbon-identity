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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.mgt.NotificationSendingModule;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.util.Utils;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * email sending abstract module. This implements the runnable task for sending
 * email.
 * So implementation of this module does not want to worry about threads.
 * 
 */
public abstract class AbstractEmailSendingModule extends NotificationSendingModule {

    protected String NOTIFICATION_TYPE = "EMAIL";

    private static final Log log = LogFactory.getLog(AbstractEmailSendingModule.class);

    private Map<String, EmailConfig> emailConfigs = new HashMap<String, EmailConfig>();

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

        if(notification != null && notification.trim().length() > 0){
            emailConfig = emailConfigs.get(notification);
        }

        if(emailConfig == null){
            emailConfig = new EmailConfig();
        }

        sendEmail(emailConfig);
    }

    public abstract void sendEmail(EmailConfig emailConfig);



    /**
     * method to load the adminManagementConfig
     */
    public  void loadEmailConfigurations() {

        String confXml = CarbonUtils.getCarbonConfigDirPath() + File.separator +
                IdentityMgtConstants.EMAIL_CONF_DIRECTORY + File.separator +
                IdentityMgtConstants.EMAIL_ADMIN_CONF_FILE;

        File configfile = new File(confXml);
        if (!configfile.exists()) {
            log.warn("Email Configuration File is not present at: " + confXml);
        }

        XMLStreamReader parser = null;
        InputStream stream = null;

        try {
            stream = new FileInputStream(configfile);
            parser = XMLInputFactory.newInstance().createXMLStreamReader(stream);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            OMElement documentElement = builder.getDocumentElement();
            Iterator iterator = documentElement.getChildElements();
            while(iterator.hasNext()){
                OMElement omElement = (OMElement) iterator.next();
                String configType = omElement.getAttributeValue(new QName("type"));
                if(configType != null && configType.trim().length() > 0){
                    emailConfigs.put(configType, loadEmailConfig(omElement));
                }

            }
        } catch (XMLStreamException e) {
            log.warn("Error while loading email config. using default configuration");
        } catch (FileNotFoundException e) {
            log.warn("Error while loading email config. using default configuration");
        } finally {
            try {
                if(parser != null){
                    parser.close();
                }
                if(stream != null){
                    stream.close();
                }
            } catch (XMLStreamException e) {
                log.error("Error while closing XML stream");
            } catch (IOException e) {
                log.error("Error while closing input stream");
            }
        }

    }

    /**
     * Loading the EmailConfig details from the given config file,
     *
     * @param configElement
     * @return - admin management config
     */
    private EmailConfig loadEmailConfig(OMElement configElement) {
        EmailConfig config = new EmailConfig();
        Iterator it = configElement.getChildElements();
        while (it.hasNext()) {
            OMElement element = (OMElement) it.next();
            if ("subject".equals(element.getLocalName())) {
                config.setSubject(element.getText());
            } else if ("body".equals(element.getLocalName())) {
                config.setEmailBody(element.getText());
            } else if ("footer".equals(element.getLocalName())) {
                config.setEmailFooter(element.getText());
            } else if ("targetEpr".equals(element.getLocalName())) {
                config.setTargetEpr(element.getText());
            } else if ("redirectPath".equals(element.getLocalName())) {
                config.setRedirectPath(element.getText());
            }
        }
        return config;
    }
}
