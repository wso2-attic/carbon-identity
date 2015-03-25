package org.wso2.carbon.identity.application.authentication.framework.model;

import org.apache.abdera.model.DateTime;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;

import java.sql.Timestamp;

/**
 * Created by lakshani on 3/20/15.
 */
public class SessionInfo {
    private String userName = null;
    private String applicationId = null;
    private Timestamp loggedInTimeStamp= null;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public Timestamp getLoggedInTimeStamp() {
        return loggedInTimeStamp;
    }

    public void setLoggedInTimeStamp(Timestamp loggedInTimeStamp) {
        this.loggedInTimeStamp = loggedInTimeStamp;
    }
}
