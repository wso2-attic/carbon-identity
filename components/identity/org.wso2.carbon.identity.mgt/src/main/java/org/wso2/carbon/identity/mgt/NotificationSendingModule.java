package org.wso2.carbon.identity.mgt;

import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.mgt.dto.NotificationDataDTO;
import org.wso2.carbon.identity.mgt.dto.UserRecoveryDTO;

/**
 *
 */
public abstract class NotificationSendingModule implements Runnable  {

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
}
