/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.identity.notification.mgt.email;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.mail.MailConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.CarbonConfigurationContextFactory;
import org.wso2.carbon.identity.notification.mgt.AbstractNotificationSendingModule;
import org.wso2.carbon.identity.notification.mgt.NotificationManagementException;
import org.wso2.carbon.identity.notification.mgt.NotificationManagementUtils;
import org.wso2.carbon.identity.notification.mgt.bean.ModuleConfiguration;
import org.wso2.carbon.identity.notification.mgt.bean.PublisherEvent;
import org.wso2.carbon.identity.notification.mgt.bean.Subscription;
import org.wso2.carbon.identity.notification.mgt.email.bean.EmailEndpointInfo;
import org.wso2.carbon.identity.notification.mgt.email.bean.EmailSubscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * This class will be the registering class as a service for email sending module on message sending component. This
 * is responsible for sending email messages on published events taking care of configured information and dynamic
 * information which are sent by the publisher
 */
@SuppressWarnings("unused")
public class EmailSendingModule extends AbstractNotificationSendingModule {
    private static final Log log = LogFactory.getLog(EmailSendingModule.class);
    /**
     * Subscription map which has all the subscriptions by email module
     */
    private Map<String, EmailSubscription> subscriptionMap;

    /**
     * Logic for sending email on publisher event from Notification Management component
     *
     * @param publisherEvent Publisher event from publisher. Includes event name and properties
     * @throws NotificationManagementException
     */
    @Override
    public void sendMessage(PublisherEvent publisherEvent) throws NotificationManagementException {
        // publisher event will not be null, since it is handled by the mgt component
        EmailSubscription subscription = subscriptionMap.get(publisherEvent.getEventName());

        // Message sending will only be done if there is a subscription on this module
        if (subscription != null) {
            List<EmailEndpointInfo> endpointInfoList = new ArrayList<EmailEndpointInfo>(subscription.getEmailEndpointInfoList());

            PrivilegedCarbonContext.startTenantFlow();
            // Send mails for each and every subscribed endpoint of the subscription
            for (EmailEndpointInfo endpointInfo : endpointInfoList) {
                Map<String, String> headerMap = new HashMap<String, String>();

                headerMap.put(MailConstants.MAIL_HEADER_SUBJECT,
                        getSubject(subscription.getSubscriptionProperties(),
                                endpointInfo.getProperties(),
                                publisherEvent.getEventProperties()));
                // Read the template configured in endpoint information.
                String template = endpointInfo.getTemplate();
                // If there is no template defined in the endpoint. use default template for which is configured for
                // subscription.
                if (StringUtils.isEmpty(template)) {
                    template = subscription.getMailTemplate();
                }
                // If still no template found. The message sending will be aborted to that
                // particular endpoint.
                if (template == null) {
                    log.error("No template found to send email to " + endpointInfo.getEmailAddress() + "on event " +
                            publisherEvent.getEventName() + ", sending aborted");
                    continue;
                }
                // Get the message which the place holders are replaced by configurations and
                // subscription properties.
                String message = getMessage(template,
                        subscription.getSubscriptionProperties(),
                        endpointInfo.getProperties(), publisherEvent.getEventProperties());
                OMElement payload = OMAbstractFactory.getOMFactory().createOMElement(BaseConstants
                        .DEFAULT_TEXT_WRAPPER, null);
                payload.setText(message);
                ServiceClient serviceClient;
                ConfigurationContext configContext = CarbonConfigurationContextFactory.getConfigurationContext();
                try {
                    if (configContext != null) {
                        serviceClient = new ServiceClient(configContext, null);
                    } else {
                        serviceClient = new ServiceClient();
                    }
                    //setting properties for axis2 client
                    Options options = new Options();
                    options.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
                    options.setProperty(MessageContext.TRANSPORT_HEADERS, headerMap);
                    options.setProperty(MailConstants.TRANSPORT_MAIL_FORMAT,
                            MailConstants.TRANSPORT_FORMAT_TEXT);
                    options.setTo(new EndpointReference(EmailModuleConstants.MAILTO_LABEL +
                            endpointInfo.getEmailAddress()));
                    serviceClient.setOptions(options);
                    serviceClient.fireAndForget(payload);
                    if (log.isDebugEnabled()) {
                        log.debug("Email has been sent to " + endpointInfo.getEmailAddress() + ", " +
                                "on event " + publisherEvent.getEventName());
                    }
                } catch (AxisFault axisFault) {
                    log.error("Error while sending email notification to address " + endpointInfo.
                            getEmailAddress() + "on event " + publisherEvent.getEventName(), axisFault);
                }
            }
            // Ultimately close tenant flow.
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * returns name of the message sending module
     *
     * @return Name of the message sending module. i.e EMAIL
     */
    @Override
    public String getModuleName() {
        return EmailModuleConstants.MODULE_NAME;
    }

    /**
     * Initialize Email Module with configurations.
     *
     * @param configurations Configurations which are relevant to this module. Passed by Notification Management
     *                       Component
     */
    @Override
    public void init(ModuleConfiguration configurations) {
        List<Subscription> subscriptions = configurations.getSubscriptions();
        subscriptionMap = new HashMap<String, EmailSubscription>();
        // Create Email Subscription from generic Subscriptions and add to map
        for (Subscription subscription : subscriptions) {
            subscriptionMap.put(subscription.getSubscriptionName(), new EmailSubscription(subscription));
        }
    }

    /**
     * Checks whether this message sending module is registered for the given name of event
     *
     * @return true if subscribed. false if not.
     */
    @Override
    public boolean isSubscribed(PublisherEvent publisherEvent) throws NotificationManagementException {
        return publisherEvent != null && subscriptionMap.containsKey(publisherEvent.getEventName());
    }

    /**
     * Message whose place holders are replaced by configurations and dynamic properties.
     *
     * @param mailContent            Original content with place holders
     * @param subscriptionProperties Generic properties which are defined in Event level
     * @param endpointProperties     Configured Properties which are in endpoint level
     * @param eventProperties        Dynamic properties which are coming from the event publisher
     * @return Message whose place holders are replaced.
     */
    private String getMessage(String mailContent, Properties subscriptionProperties, Properties endpointProperties,
                              Properties eventProperties) {
        // First replace place holders with configured endpoint properties
        mailContent = NotificationManagementUtils.replacePlaceHolders(mailContent, "\\{", "\\}", endpointProperties);
        // Secondly replace place holders with dynamic properties which comes form publisher
        mailContent = NotificationManagementUtils.replacePlaceHolders(mailContent, "\\{", "\\}", eventProperties);
        // Thirdly replace place holders with generic properties which are configured in event level.
        mailContent = NotificationManagementUtils.replacePlaceHolders(mailContent, "\\{", "\\}",
                subscriptionProperties);
        return mailContent;
    }

    /**
     * Read the subject of the mail from either configurations or dynamic information
     *
     * @param subscriptionProperties Module level event properties
     * @param endpointProperties     endpoint level properties
     * @param eventProperties        dynamic properties which are sent from the publisher
     * @return Subject of the mail
     */
    private String getSubject(Properties subscriptionProperties, Properties endpointProperties,
                              Properties eventProperties) {
        String subject = "";
        // First priority is given to the endpoint level subject
        if (endpointProperties != null && endpointProperties.get(EmailModuleConstants.SUBJECT_PROPERTY_LABEL) != null) {
            subject = endpointProperties.getProperty(EmailModuleConstants.SUBJECT_PROPERTY_LABEL);
            // If the subject is not found at endpoint level configuration then search in dynamic configurations.
        } else if (eventProperties != null && StringUtils.isNotEmpty(eventProperties.getProperty(EmailModuleConstants
                .SUBJECT_PROPERTY_LABEL))) {
            subject = eventProperties.get(EmailModuleConstants.SUBJECT_PROPERTY_LABEL).toString();
            // If subject is not found at any of the above levels. use the default subject which is configured at
            // subscription level.
        } else if (subscriptionProperties != null && StringUtils.isNotEmpty(subscriptionProperties.getProperty
                (EmailModuleConstants.SUBJECT_PROPERTY_LABEL))) {
            subject = subscriptionProperties.getProperty(EmailModuleConstants.SUBJECT_PROPERTY_LABEL);
        }
        return subject;
    }
}

