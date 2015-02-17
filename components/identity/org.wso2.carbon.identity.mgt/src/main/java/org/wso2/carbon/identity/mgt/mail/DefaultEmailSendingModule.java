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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.mail.MailConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.CarbonConfigurationContextFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Default email sending implementation
 */
public class DefaultEmailSendingModule extends AbstractEmailSendingModule {

    public static final String CONF_STRING = "confirmation";
    private static Log log = LogFactory.getLog(DefaultEmailSendingModule.class);
    private Notification notification;

    /**
     * Replace the {user-parameters} in the config file with the respective
     * values
     *
     * @param text           the initial text
     * @param userParameters mapping of the key and its value
     * @return the final text to be sent in the email
     */
    public static String replacePlaceHolders(String text, Map<String, String> userParameters) {
        if (userParameters != null) {
            for (Map.Entry<String, String> entry : userParameters.entrySet()) {
                String key = entry.getKey();
                if (key != null && entry.getValue() != null) {
                    text = text.replaceAll("\\{" + key + "\\}", entry.getValue());
                }
            }
        }
        return text;
    }

    public void sendEmail() {

        Map<String, String> headerMap = new HashMap<String, String>();

        try {
            if (this.notification == null) {
                throw new NullPointerException("Notification not set. Please set the notification before sending messages");
            }
            PrivilegedCarbonContext.startTenantFlow();

            headerMap.put(MailConstants.MAIL_HEADER_SUBJECT, this.notification.getSubject());

            OMElement payload = OMAbstractFactory.getOMFactory().createOMElement(
                    BaseConstants.DEFAULT_TEXT_WRAPPER, null);
            StringBuilder contents = new StringBuilder();
            contents.append(this.notification.getBody())
                    .append(System.getProperty("line.separator"))
                    .append(System.getProperty("line.separator"))
                    .append(this.notification.getFooter());
            payload.setText(contents.toString());
            ServiceClient serviceClient;
            ConfigurationContext configContext = CarbonConfigurationContextFactory
                    .getConfigurationContext();
            if (configContext != null) {
                serviceClient = new ServiceClient(configContext, null);
            } else {
                serviceClient = new ServiceClient();
            }
            Options options = new Options();
            options.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
            options.setProperty(MessageContext.TRANSPORT_HEADERS, headerMap);
            options.setProperty(MailConstants.TRANSPORT_MAIL_FORMAT,
                    MailConstants.TRANSPORT_FORMAT_TEXT);
            options.setTo(new EndpointReference("mailto:" + this.notification.getSendTo()));
            serviceClient.setOptions(options);
            log.debug("Sending " + "user credentials configuration mail to " + this.notification.getSendTo());
            serviceClient.fireAndForget(payload);

            log.debug("Email content : " + this.notification.getBody());
            log.info("User credentials configuration mail has been sent to " + this.notification.getSendTo());
        } catch (Exception e) {
            log.error("Failed Sending Email" + e.getMessage());
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }

    /**
     * @param userParameters
     * @return
     */
//	private String getEmailMessage(Map<String, String> userParameters) {
//		StringBuffer message = new StringBuffer();
//		for (Map.Entry<String, String> entry : userParameters.entrySet()) {
//			message.append("\n" + entry.getKey() + " : " + entry.getValue());
//		}
//		return message.toString();
//	}
    public String getRequestMessage(EmailConfig emailConfig) {

        String msg;
        String targetEpr = emailConfig.getTargetEpr();
        if (emailConfig.getEmailBody().length() == 0) {
            msg = EmailConfig.DEFAULT_VALUE_MESSAGE + "\n";
            if (notificationData.getNotificationCode() != null) {
                msg =
                        msg + targetEpr + "?" + CONF_STRING + "=" + notificationData.getNotificationCode() +
                                "\n";
            }
        } else {
            msg = emailConfig.getEmailBody() + "\n";
            if (notificationData.getNotificationCode() != null) {
//				msg =
//				      msg + targetEpr + "?" + CONF_STRING + "=" + notificationData.getNotificationCode() +
//				              "\n";
            }
        }
        if (emailConfig.getEmailFooter() != null) {
            msg = msg + "\n" + emailConfig.getEmailFooter();
        }
        return msg;
    }

    @Override
    public Notification getNotification() {
        return this.notification;
    }

    @Override
    public void setNotification(Notification notification) {
        this.notification = notification;
    }

}
