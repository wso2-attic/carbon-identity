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

package org.wso2.carbon.identity.notification.mgt.sms.bean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.notification.mgt.NotificationManagementException;
import org.wso2.carbon.identity.notification.mgt.NotificationManagementUtils;
import org.wso2.carbon.identity.notification.mgt.bean.Subscription;
import org.wso2.carbon.identity.notification.mgt.sms.SMSModuleConstants;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Event class is a bean class for a registered event. ie registered events for JSON message sending module. All
 * event related data are stored in this class
 */
@SuppressWarnings("unused")
public class SMSSubscription extends Subscription {

    private static final Log log = LogFactory.getLog(SMSSubscription.class);
    /**
     * Template of the json as defined in the configuration or given at the run time
     */
    private String jsonContent;
    /**
     * List of endpoints which this event should fire notifications.
     */
    private List<SMSReciever> endpointInfoList;

    /**
     * Passes a generic subscription object and this builds specific json subscription object
     *
     * @param subscription A generic type subscription object
     */
    public SMSSubscription(Subscription subscription) throws NotificationManagementException {

        super(subscription.getSubscriptionName(), subscription.getSubscriptionProperties());
        endpointInfoList = new ArrayList<SMSReciever>();
        // Build the json subscription object with parsed properties from management component
        build(getSubscriptionName(), getSubscriptionProperties());
        setSubscriptionProperties(NotificationManagementUtils.buildSingleWordKeyProperties( SMSModuleConstants
                .Config.SUBSCRIPTION_NS + "." + getSubscriptionName(), getSubscriptionProperties()));
    }

    /**
     * Takes all properties which has json.subscribe.eventName as key prefix and build an event out of it
     *
     * @param eventName              name of the event.
     * @param subscriptionProperties properties which has json.subscribe.eventName as prefix
     * @throws NotificationManagementException
     */
    private void build(String eventName, Properties subscriptionProperties) throws
            NotificationManagementException {
        // Subscription properties will never be null, Therefore no need to check
        if (StringUtils.isEmpty(eventName)) {
            throw new NotificationManagementException("No valid event name found. Cannot proceed with event building");
        }
        String eventKey = SMSModuleConstants.Config.SUBSCRIPTION_NS + "." + eventName;

        if (log.isDebugEnabled()) {
            log.debug("Building Event object for event " + eventName + " from json notification sending module");
        }

        // Reading json template for event
        String templatePath = (String) subscriptionProperties.remove(eventKey + "." + SMSModuleConstants.Config
                .JSON_CONTENT_QNAME);
        if (templatePath != null) {
            String template = NotificationManagementUtils.readMessageTemplate(templatePath);
            this.setJsonContent(template);
        } else {
            log.warn("No default json template found for event " + eventName);
        }

        // Sets endpoint information for the event
        String endpointKey = eventKey + "." + SMSModuleConstants.Config.ENDPOINT_QNAME;
        setEndpoints(endpointKey, NotificationManagementUtils.getPropertiesWithPrefix(endpointKey,
                subscriptionProperties));
    }

    /**
     * Set endpoints to the json subscription object
     *
     * @param prefix              Prefix of the endpoint properties key. ie json.subscribe.eventName.endpoint.endpointName
     * @param endpointsProperties Properties which are relevant to endpoint.
     */
    private void setEndpoints(String prefix, Properties endpointsProperties) {

        Properties endpointNames = NotificationManagementUtils.getSubProperties(prefix, endpointsProperties);
        Enumeration endpointNameSet = endpointNames.propertyNames();

        while (endpointNameSet.hasMoreElements()) {
            String key = (String) endpointNameSet.nextElement();
            String endpointName = (String) endpointNames.remove(key);

            String endpointKey = prefix + "." + endpointName;
            Properties endpointProperties = NotificationManagementUtils.getPropertiesWithPrefix(endpointKey,
                    endpointsProperties);

            try {
                endpointInfoList.add(buildEndpoint(endpointKey, endpointProperties));
            } catch (NotificationManagementException e) {
                log.error("Error while building endpoint object with key " + endpointKey, e);
            }
        }
    }

    /**
     * Build JsonEndpointInfo  objects from properties which has the prefix json.subscribe.eventName.endpoint
     * .endpointName
     *
     * @param prefix             prefix of the endpoint properties key. ie json.subscribe.eventName
     *                           .endpoint.endpointName
     * @param endpointProperties properties which has key with prefix. json.subscribe.eventName
     *                           .endpoint.endpointName
     * @return JsonEndpointInfo object build out of properties
     * @throws NotificationManagementException
     */
    private SMSReciever buildEndpoint(String prefix, Properties endpointProperties) throws
            NotificationManagementException {

        SMSReciever jsonEndpointInfo = new SMSReciever();
        String url = (String) endpointProperties.remove(prefix + "." + SMSModuleConstants.Config.ADDRESS_QNAME);
        // If there is no configured json url address, stop building endpoint, throw an exception
        if (StringUtils.isNotEmpty(url)) {
            url = url.trim();
            if (log.isDebugEnabled()) {
                log.debug("Registering json endpoint with address " + url);
            }
            jsonEndpointInfo.setEndpoint(url);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Registering json endpoint with prefix " + prefix + " without url. Expecting the " +
                        "email url at event time");
            }
        }
        jsonEndpointInfo.setEndpoint(url);
        String template = (String) endpointProperties.remove(prefix + "." + SMSModuleConstants.Config
                .JSON_CONTENT_QNAME);

        if (template != null) {
            //Set the json template configured for endpoint.
            jsonEndpointInfo.setJsonConfigString(NotificationManagementUtils.readMessageTemplate(template.trim()));
        } else {
            // If template is null, subscription level template will be used on the time of
            // sending email
            if (log.isDebugEnabled()) {
                log.debug("No template configured for endpoint" + url);
            }
        }
        jsonEndpointInfo.setEndpointProperties(NotificationManagementUtils.buildSingleWordKeyProperties(prefix,
                endpointProperties));
        return jsonEndpointInfo;
    }

    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public List<SMSReciever> getEndpointInfoList() {
        return endpointInfoList;
    }

    public void setEndpointInfoList(List<SMSReciever> endpointInfoList) {
        this.endpointInfoList = endpointInfoList;
    }

}