/*
 * Copyright (c) 2005-2008 , WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provider.openid.handlers;

import org.openid4java.message.AuthRequest;

/**
 * Parameters sent from an OpenID consumer are mapped to an instance of
 * OpenIDAuthenticationRequest and corresponding extensions are invoked.
 */
public class OpenIDAuthenticationRequest {

    private boolean phishingResistanceLogin;

    private boolean multifactorLogin;

    private String extensionAlias;

    private AuthRequest authRequest;

    public boolean isPhishingResistanceLogin() {
        return phishingResistanceLogin;
    }

    public void setPhishingResistanceLogin(boolean phishingResistanceLogin) {
        this.phishingResistanceLogin = phishingResistanceLogin;
    }

    public boolean isMultifactorLogin() {
        return multifactorLogin;
    }

    public void setMultifactorLogin(boolean multifactorLogin) {
        this.multifactorLogin = multifactorLogin;
    }

    public String getExtensionAlias() {
        return extensionAlias;
    }

    public void setExtensionAlias(String extensionAlias) {
        this.extensionAlias = extensionAlias;
    }

    public AuthRequest getAuthRequest() {
        return authRequest;
    }

    public void setAuthRequest(AuthRequest authRequest) {
        this.authRequest = authRequest;
    }
}