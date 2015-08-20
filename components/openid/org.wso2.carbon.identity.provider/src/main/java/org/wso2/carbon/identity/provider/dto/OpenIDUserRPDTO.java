/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.wso2.carbon.identity.provider.dto;

import org.wso2.carbon.identity.core.model.OpenIDUserRPDO;

import java.util.Date;

public class OpenIDUserRPDTO {

    private String defaultProfileName;
    private String rpUrl;
    private String userName;
    private boolean trustedAlways;
    private int visitCount;
    private Date lastVisit;
    private String openID;

    public OpenIDUserRPDTO() {

    }

    public OpenIDUserRPDTO(OpenIDUserRPDO rpdo) {
        defaultProfileName = rpdo.getDefaultProfileName();
        rpUrl = rpdo.getRpUrl();
        userName = rpdo.getUserName();
        trustedAlways = rpdo.isTrustedAlways();
        visitCount = rpdo.getVisitCount();
        lastVisit = rpdo.getLastVisit();
        openID = null;
    }

    public String getRpUrl() {
        return rpUrl;
    }

    public void setRpUrl(String rpUrl) {
        this.rpUrl = rpUrl;
    }

    public String getDefaultProfileName() {
        return defaultProfileName;
    }

    public void setDefaultProfileName(String defaultProfileName) {
        this.defaultProfileName = defaultProfileName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isTrustedAlways() {
        return trustedAlways;
    }

    public void setTrustedAlways(boolean isTrustedAlways) {
        this.trustedAlways = isTrustedAlways;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public Date getLastVisit() {
        return lastVisit;
    }

    public void setLastVisit(Date lastVisit) {
        this.lastVisit = lastVisit;
    }

    public String getOpenID() {
        return openID;
    }

    public void setOpenID(String openID) {
        this.openID = openID;
    }
}
