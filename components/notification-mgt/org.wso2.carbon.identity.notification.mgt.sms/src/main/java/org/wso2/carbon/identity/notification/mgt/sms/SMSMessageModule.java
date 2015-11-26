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

package org.wso2.carbon.identity.notification.mgt.sms;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.wso2.carbon.identity.notification.mgt.AbstractNotificationSendingModule;
import org.wso2.carbon.identity.notification.mgt.NotificationManagementException;
import org.wso2.carbon.identity.notification.mgt.NotificationManagementUtils;
import org.wso2.carbon.identity.notification.mgt.bean.ModuleConfiguration;
import org.wso2.carbon.identity.notification.mgt.bean.PublisherEvent;
import org.wso2.carbon.identity.notification.mgt.bean.Subscription;
import org.wso2.carbon.identity.notification.mgt.sms.bean.SMSReciever;
import org.wso2.carbon.identity.notification.mgt.sms.bean.SMSSubscription;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This is a specific implementation of MessageSendingModule which is used for sending JSON messages on an event.
 * This is exposed as the service class and will register this module as a message sending module
 */
public class SMSMessageModule extends AbstractNotificationSendingModule {

    private static final Log log = LogFactory.getLog(SMSMessageModule.class);
    /**
     * Map of JsonSubscriptions which are registered
     */
    private Map<String, SMSSubscription> subscriptionMap;

    /**
     * Overridden method for rest json message sending.
     *
     * @param publisherEvent Published event name
     * @throws NotificationManagementException
     */
    @Override
    public void sendMessage(PublisherEvent publisherEvent) throws NotificationManagementException {
        // Publisher event will not be null since it is handled by the mgt component.
        // Read the relevant subscription from configurations.
        SMSSubscription jsonSubscription = subscriptionMap.get(publisherEvent.getEventName());
        // New list for aggregated set of endpoints. ie configured and dynamic
        List<SMSReciever> endpoints;

        if (jsonSubscription != null) {
            endpoints = jsonSubscription.getEndpointInfoList();
            HttpClient client = new DefaultHttpClient();
            // Get all configured endpoints for message sending
            // Send messages to each endpoint in endpoints list
            for (SMSReciever endpoint : endpoints) {
                String jsonContent = endpoint.getJsonConfigString();
                String smsRestCall = endpoint.getEndpoint() + jsonContent;
                smsRestCall = NotificationManagementUtils.replacePlaceHolders(smsRestCall, "\\(", "\\)",
                        publisherEvent.getEventProperties());
                smsRestCall = smsRestCall.trim();
                HttpGet post = new HttpGet(smsRestCall);
                HttpResponse response = null;
                StringEntity entity;

                try {
                    response = client.execute(post);
                    log.info("Notification message has been posted to " + endpoint.getEndpoint() + " on event " +
                            publisherEvent.getEventName());
                    if (log.isDebugEnabled() && response != null) {
                        log.debug("Response of HTTP post is " + response.getStatusLine() + "on post operation to " +
                                "endpoint " + endpoint.getEndpoint() + " on event " + publisherEvent.getEventName());
                    }
                } catch (UnsupportedEncodingException e) {
                    log.error("Error while creating StringEntry from given JSON string on event " + publisherEvent.
                            getEventName() + "to endpoint " + endpoint.getEndpoint() + " JSON string : ", e);

                } catch (ClientProtocolException e) {
                    log.error("Error in HTTP protocol, error while executing POST operation to endpoint " + endpoint
                            .getEndpoint() + "on event " + publisherEvent.getEventName(), e);

                } catch (IOException e) {
                    log.error("Error while executing POST operation to endpoint " + endpoint.getEndpoint() + "on " +
                            "event " + publisherEvent.getEventName(), e);

                } finally {
                    // Finally closing connections.
                    post.abort();
                }
            }
        }
    }

    @Override
    public String getModuleName() {
        return SMSModuleConstants.MODULE_NAME;
    }

    /**
     * Initialize REST JSON Module with configurations.
     *
     * @param moduleConfigurations Configurations which are relevant to this module. Passed by Notification Management
     *                             Component
     */
    @Override
    public void init(ModuleConfiguration moduleConfigurations) throws NotificationManagementException {

        this.subscriptionMap = new HashMap<String, SMSSubscription>();
        for (Subscription subscription : moduleConfigurations.getSubscriptions()) {
            try {
                subscriptionMap.put(subscription.getSubscriptionName(), new SMSSubscription(subscription));
            } catch (NotificationManagementException e) {
                // Will log an error at the server startup time if JSON subscription object building fails. Will
                // continue with building rest of the subscriptions if one fails.
                log.error("Error while building JSON subscription from Subscription for : " +
                        subscription.getSubscriptionName(), e);
            }
        }
    }

    @Override
    public boolean isSubscribed(PublisherEvent publisherEvent) throws NotificationManagementException {

        return publisherEvent != null && subscriptionMap.containsKey(publisherEvent.getEventName());
    }
    /**
     * Build the JSON string which is to be sent to the endpoint
     *
     * @return A string which contains JSON message to be sent to the endpoint
     */
    private String getJSONData(String jsonContent, Properties eventParams, Properties endpointParams,
                               Properties dynamicParams) {

        if (StringUtils.isNotEmpty(jsonContent)) {
            // Replaced using one property map at once. May improve efficiency if no replacements are needed.
            jsonContent = NotificationManagementUtils.replacePlaceHolders(jsonContent, "\\(", "\\)", endpointParams);
            jsonContent = NotificationManagementUtils.replacePlaceHolders(jsonContent, "\\(", "\\)", dynamicParams);
            jsonContent = NotificationManagementUtils.replacePlaceHolders(jsonContent, "\\(", "\\)", eventParams);
        }
        return jsonContent;
    }
}
