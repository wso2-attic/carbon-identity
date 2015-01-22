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

package org.wso2.carbon.identity.notification.mgt.email.bean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.notification.mgt.NotificationManagementException;
import org.wso2.carbon.identity.notification.mgt.NotificationManagementUtils;
import org.wso2.carbon.identity.notification.mgt.bean.Subscription;
import org.wso2.carbon.identity.notification.mgt.email.EmailModuleConstants;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * A bean class which keeps information about Subscriptions which are registered in the email message sending module.
 * This is built using a generic Subscription object which is being passed from Notification Mgt component.
 */
@SuppressWarnings("unused")
public class EmailSubscription extends Subscription {

    private static final Log log = LogFactory.getLog(EmailSubscription.class);
    /**
     * Subscription level template.
     */
    private String mailTemplate;
    /**
     * List of email endpoints which is registered with the subscription.
     */
    private List<EmailEndpointInfo> emailEndpointInfoList;

    /**
     * Constructor of EmailSubscription. Will initiate lists and build specific type subscription object out of
     * generic Subscription object
     *
     * @param subscription A subscription passed from Notification Mgt component
     */
    public EmailSubscription(Subscription subscription) {

        super(subscription.getSubscriptionName(), subscription.getSubscriptionProperties());
        emailEndpointInfoList = new ArrayList<EmailEndpointInfo>();
        // Build specific subscription objects out of generic subscription objects
        buildEmailSubscription();
    }

    /**
     * Builds an EmailSubscription object from general Subscription properties which are passed
     * from Notification Mgt component.
     */
    private void buildEmailSubscription() {
        // Constructing subscription key. eg - email.subscription.subscriptionName
        String subscriptionKey = EmailModuleConstants.Config.SUBSCRIPTION_NS + "." + getSubscriptionName();

        // Reading email template for event
        String templatePath = (String) getSubscriptionProperties().remove(subscriptionKey + "." +
                EmailModuleConstants.Config.MAIL_TEMPLATE_QNAME);

        if (StringUtils.isNotEmpty(templatePath)) {
            String template = NotificationManagementUtils.readMessageTemplate(templatePath);
            this.setMailTemplate(template);
        } else {
            log.warn("No default email template found for event " + getSubscriptionName());
        }
        //Sets endpoint information for the event
        String endpointKey = subscriptionKey + "." + EmailModuleConstants.Config.ENDPOINT_QNAME;
        setEndpoints(endpointKey, NotificationManagementUtils.getPropertiesWithPrefix(endpointKey,
                getSubscriptionProperties()));
    }

    /**
     * Set endpoints to the email subscription
     *
     * @param prefix              prefix of the subscription
     * @param endpointsProperties Properties which are related to endpoints
     */
    private void setEndpoints(String prefix, Properties endpointsProperties) {

        Properties endpointNames = NotificationManagementUtils.getSubProperties(prefix, endpointsProperties);
        Enumeration endpointNameSet = endpointNames.propertyNames();
        // Build all the endpoints by iterating through properties
        while (endpointNameSet.hasMoreElements()) {
            String key = (String) endpointNameSet.nextElement();
            String endpointName = (String) endpointNames.remove(key);
            // Build endpoint key using endpoint name
            String endpointKey = prefix + "." + endpointName;
            Properties endpointProperties = NotificationManagementUtils.getPropertiesWithPrefix(endpointKey,
                    endpointsProperties);
            // Build and add email endpoint object to subscription
            try {
                emailEndpointInfoList.add(buildEndpoint(endpointKey, endpointProperties));
            } catch (NotificationManagementException e) {
                // If the particular endpoint building fails, An error message will be printed at the startup time.
                // And continue with building other endpoints
                log.error("Error while building endpoint object for endpoint with key " + endpointKey, e);
            }
        }
    }

    /**
     * Build EmailEndpointInfo from the properties relevant to endpoints
     *
     * @param prefix             Prefix of the endpoint. Eg email.subscription.subscriptionName.endpoint
     * @param endpointProperties Properties which are specific to endpoint. eg email.subscription
     *                           .subscriptionName.endpoint.property
     * @return EmailEndpointInfo object which consists of email endpoint information
     * @throws NotificationManagementException
     */
    private EmailEndpointInfo buildEndpoint(String prefix, Properties endpointProperties) throws
            NotificationManagementException {

        EmailEndpointInfo emailEndpointInfo = new EmailEndpointInfo();
        String emailAddress = (String) endpointProperties.remove(prefix + "." + EmailModuleConstants.Config.
                ADDRESS_QNAME);
        // If no configured email address is found, check in event properties for an email address.
        if (StringUtils.isEmpty(emailAddress)) {
            emailAddress = (String) endpointProperties.remove(EmailModuleConstants.Config.ADDRESS_QNAME);
        }
        // If there is no configured email address, stop building endpoint, throw an exception
        if (StringUtils.isEmpty(emailAddress)) {
            throw new NotificationManagementException("No email address configured for endpoint");
        }

        emailAddress = emailAddress.trim();
        if (log.isDebugEnabled()) {
            log.debug("Registering email endpoint with address " + emailAddress);
        }
        emailEndpointInfo.setEmailAddress(emailAddress);

        String template = (String) endpointProperties.remove(prefix + "." + EmailModuleConstants.Config.
                MAIL_TEMPLATE_QNAME);
        if (template != null) {
            //Set the email template configured for endpoint.
            emailEndpointInfo.setTemplate(NotificationManagementUtils.readMessageTemplate(template.trim()));
        } else {
            // If template is null, event level template will be used
            if (log.isDebugEnabled()) {
                log.debug("No template configured for endpoint" + emailAddress);
            }
        }
        // Set endpoint specific properties
        emailEndpointInfo.setProperties(NotificationManagementUtils.buildSingleWordKeyProperties(prefix,
                endpointProperties));
        return emailEndpointInfo;
    }

    public String getMailTemplate() {
        return mailTemplate;
    }

    public void setMailTemplate(String mailTemplate) {
        this.mailTemplate = mailTemplate;
    }

    public List<EmailEndpointInfo> getEmailEndpointInfoList() {
        return emailEndpointInfoList;
    }

    public void setEmailEndpointInfoList(List<EmailEndpointInfo> emailEndpointInfoList) {
        this.emailEndpointInfoList = emailEndpointInfoList;
    }

}
