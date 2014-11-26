/*
*
*   Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*   WSO2 Inc. licenses this file to you under the Apache License,
*   Version 2.0 (the "License"); you may not use this file except
*   in compliance with the License.
*   You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*
*/

package org.wso2.carbon.identity.entitlement.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.entitlement.model.PEPEndpointInfo;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.*;
import java.util.Properties;

/**
 * configurations for user operation notification module
 *
 */
public class UserNotificationConfig {

	private static final Log log = LogFactory.getLog(UserNotificationConfig.class);

	private static final String NOTIFICATION_CONFIG = "notification-mgt.properties";
	private static final String ENDPOINT = "endpoint";
	private static final String NOTIFICATION_TYPE = "notification.type";
	private static final String EMAIL_ENABLED = "enable.email.notification";
	private static final String EMAIL_USERNAME = "email.target.username";
	private static final String EMAIL_GROUP = "email.sending.group";
	private static final String IS_NOTIFICATION_ENABLED="enable.notification";

	private PEPEndpointInfo targetEndpoint;
	private String notificationType;
	private boolean emailEnabled;
	private String emailUsername;
	private String emailGroup;

	//only if notifications are enabled, UserOperationListener is registered
	private boolean notificationEnabled = false;

	public UserNotificationConfig() {

		Properties properties = new Properties();
		InputStream inStream = null;

		File notificationConfig = new File(
                CarbonUtils.getCarbonSecurityConfigDirPath(), NOTIFICATION_CONFIG);
		if (notificationConfig.exists()) {
			try {
				inStream = new FileInputStream(notificationConfig);
				properties.load(inStream);
			} catch (FileNotFoundException e) {
				log.error("File not found: " + NOTIFICATION_CONFIG, e);
			} catch (IOException e) {
				log.error("Can not load " + NOTIFICATION_CONFIG + " file", e);
			} finally {
				if (inStream != null) {
					try {
						inStream.close();
					} catch (IOException e) {
						log.error("Error while closing stream ", e);
					}
				}
			}
		}

		String notificationsEnabledProp = properties.getProperty(IS_NOTIFICATION_ENABLED);

		if(notificationsEnabledProp != null && !notificationsEnabledProp.trim().isEmpty()) {
			this.notificationEnabled = Boolean.parseBoolean(notificationsEnabledProp);
		} else {
			this.notificationEnabled = false;
		}
		//if the this module is not enabled, will return without further proceeding
		if(!notificationEnabled){
			return;
		}

		String endpoint = properties.getProperty(ENDPOINT);
		if(endpoint != null && !endpoint.trim().isEmpty()){
			String[] endpointInfo = endpoint.split(",");
			if(endpointInfo.length == 3){
				targetEndpoint = new PEPEndpointInfo();
				targetEndpoint.setEndpoint(endpointInfo[0]);
				targetEndpoint.setUsername(endpointInfo[1]);
				targetEndpoint.setPassword(endpointInfo[2]);
			} else {
                log.error("Notification endpoint configuration error. Only " +
                        endpointInfo.length + " parameters were found");
            }
		}
		this.notificationType = properties.getProperty(NOTIFICATION_TYPE);
		if(this.notificationType == null) {
			this.notificationType = "JSON";
		}

		String emailEnabled = properties.getProperty(EMAIL_ENABLED);
		if(emailEnabled != null) {
			this.emailEnabled = Boolean.parseBoolean(emailEnabled);
		} else {
			this.emailEnabled =false;
		}

		this.emailGroup = properties.getProperty(EMAIL_GROUP);
		if(this.emailGroup == null) {
			this.emailGroup = "notifications@default.com";
		}
		this.emailUsername = properties.getProperty(EMAIL_USERNAME);
		if(this.emailUsername == null) {
			this.emailUsername = "Admin";
		}

		if(log.isDebugEnabled() && isNotificationEnabled()) {

			if(targetEndpoint != null){
				log.debug("loading " + ENDPOINT +" : " + targetEndpoint.getEndpoint());
			}
			log.debug("loading " + NOTIFICATION_TYPE +" : " + getNotificationType());
			log.debug("loading " + EMAIL_ENABLED +" : " + getEmailEnabled());
			log.debug("loading " + EMAIL_GROUP +" : " + getEmailGroup());
			log.debug("loading " + EMAIL_USERNAME +" : " + getEmailUsername());
		}

	}

	public PEPEndpointInfo getTargetEndpoint() {
		return targetEndpoint;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public boolean getEmailEnabled() {
		return emailEnabled;
	}

	public String getEmailUsername() {
		return emailUsername;
	}

	public String getEmailGroup() {
		return emailGroup;
	}

	public boolean isNotificationEnabled() {
		return notificationEnabled;
	}



}
