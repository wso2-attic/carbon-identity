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

public class XMPPSettingsDTO {

    private String xmppServer;
    private String xmppUserName;
    private String userCode;
    private String userId;
    private boolean isXmppEnabled;
    private boolean isPINEnabled;

    public boolean isPINEnabled() {
        return isPINEnabled;
    }

    public void setPINEnabled(boolean PINEnabled) {
        isPINEnabled = PINEnabled;
    }

    /**
     * @return whether the user has enabled XMPP Settings.
     */
    public boolean isXmppEnabled() {
        return isXmppEnabled;
    }

    /**
     * @param xmppEnabled
     */
    public void setXmppEnabled(boolean xmppEnabled) {
        this.isXmppEnabled = xmppEnabled;
    }

    /**
     * @return userId of a user
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @param userId
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * @return XMPP Server Name
     */
    public String getXmppServer() {
        return xmppServer;
    }

    /**
     * @param xmppServer
     */
    public void setXmppServer(String xmppServer) {
        this.xmppServer = xmppServer;
    }

    /**
     * @return XMPP UserName
     */
    public String getXmppUserName() {
        return xmppUserName;
    }

    /**
     * @param xmppUserName
     */
    public void setXmppUserName(String xmppUserName) {
        this.xmppUserName = xmppUserName;
    }

    /**
     * @return xmppUserCode
     */
    public String getUserCode() {
        return userCode;
    }

    /**
     * @param userCode
     */
    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }
}
