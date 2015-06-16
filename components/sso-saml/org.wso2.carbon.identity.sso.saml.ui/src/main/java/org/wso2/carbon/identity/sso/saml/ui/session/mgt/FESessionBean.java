/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.sso.saml.ui.session.mgt;

import java.util.Date;

/**
 * This class is used to hold the SSO FE session information.
 */
public class FESessionBean {
    private Object sessionBean;
    private Date creationTime;
    private String status;
    private String statusMsg;
    private String relayState;

    public FESessionBean(Object sessionBean, String relayState) {
        this.sessionBean = sessionBean;
        this.creationTime = new Date();
        this.relayState = relayState;
    }

    public FESessionBean(String status, String statusMsg) {
        this.status = status;
        this.statusMsg = statusMsg;
        this.creationTime = new Date();
    }

    public Object getSessionBean() {
        return sessionBean;
    }

    public Date getCreationTime() {
        if(creationTime == null){
            return null;
        }
        return (Date) creationTime.clone();
    }

    public String getStatus() {
        return status;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public String getRelayState() {
        return relayState;
    }

    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }
}
