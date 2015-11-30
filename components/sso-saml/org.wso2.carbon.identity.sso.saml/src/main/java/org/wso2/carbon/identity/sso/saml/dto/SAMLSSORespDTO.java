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
package org.wso2.carbon.identity.sso.saml.dto;

import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;

import java.io.Serializable;

public class SAMLSSORespDTO implements Serializable {

    private static final long serialVersionUID = 5848839581755256822L;

    private String respString;
    private boolean isSessionEstablished;
    private String assertionConsumerURL;
    private String loginPageURL;
    private String errorMsg;
    private AuthenticatedUser subject;

    public String getRespString() {
        return respString;
    }

    public void setRespString(String respString) {
        this.respString = respString;
    }

    public boolean isSessionEstablished() {
        return isSessionEstablished;
    }

    public void setSessionEstablished(boolean sessionEstablished) {
        isSessionEstablished = sessionEstablished;
    }

    public String getAssertionConsumerURL() {
        return assertionConsumerURL;
    }

    public void setAssertionConsumerURL(String assertionConsumerURL) {
        this.assertionConsumerURL = assertionConsumerURL;
    }

    public String getLoginPageURL() {
        return loginPageURL;
    }

    public void setLoginPageURL(String loginPageURL) {
        this.loginPageURL = loginPageURL;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public AuthenticatedUser getSubject() {
        return subject;
    }

    public void setSubject(AuthenticatedUser subject) {
        this.subject = subject;
    }
}
