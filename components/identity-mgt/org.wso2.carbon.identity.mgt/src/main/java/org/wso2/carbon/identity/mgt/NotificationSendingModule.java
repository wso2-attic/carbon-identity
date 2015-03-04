/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.identity.mgt;

import org.wso2.carbon.identity.mgt.dto.NotificationDataDTO;
import org.wso2.carbon.identity.mgt.mail.Notification;

/**
 *
 */
public abstract class NotificationSendingModule implements Runnable {

    protected NotificationDataDTO notificationData;

    @Override
    public void run() {
        notifyUser();
    }

    public abstract void init() throws Exception;

    public abstract String getNotificationType();

    public abstract String getNotificationAddress(String userName, int tenantId);

    public abstract void notifyUser();

    public void setNotificationData(NotificationDataDTO notificationData) {
        this.notificationData = notificationData;
    }

    public abstract Notification getNotification();

    public abstract void setNotification(Notification notification);
}
