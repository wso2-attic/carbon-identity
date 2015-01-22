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

import java.util.Properties;

/**
 * A bean class which keeps information about email endpoints. ie information about email
 * recipients and the content to the particular recipient.
 */
public class EmailEndpointInfo {
    /**
     * Properties which are related to endpoints.
     */
    private Properties properties;
    /**
     * Email template
     */
    private String template;
    /**
     * Email address which email should be sent to.
     */
    private String emailAddress;

    /**
     * Return the template of the email
     * @return template of the email
     */
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Set of properties which are specific for endpoint level
     * @return Map of properties.
     */
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

}
