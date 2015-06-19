/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authenticator.iwa;

import waffle.servlet.spi.SecurityFilterProviderCollection;
import waffle.windows.auth.IWindowsAuthProvider;
import waffle.windows.auth.PrincipalFormat;

public class IWAServiceDataHolder {
    private IWAServiceDataHolder() {
        setPrincipalFormat(PrincipalFormat.both.fqn);
        setRoleFormat(PrincipalFormat.fqn);
    }
    private static IWAServiceDataHolder instance = new IWAServiceDataHolder();
    private SecurityFilterProviderCollection providers;
    private IWindowsAuthProvider auth;
    private boolean allowGuestLogin;
    private boolean impersonate;
    private PrincipalFormat principalFormat;
    private PrincipalFormat roleFormat;


    public static IWAServiceDataHolder getInstance() {
        return instance;
    }

    public PrincipalFormat getPrincipalFormat() {
        return principalFormat;
    }

    public void setPrincipalFormat(PrincipalFormat principalFormat) {
        this.principalFormat = principalFormat;
    }

    public PrincipalFormat getRoleFormat() {
        return roleFormat;
    }

    public void setRoleFormat(PrincipalFormat roleFormat) {
        this.roleFormat = roleFormat;
    }

    public IWindowsAuthProvider getAuth() {
        return auth;
    }

    public void setAuth(IWindowsAuthProvider auth) {
        this.auth = auth;
    }

    public boolean isAllowGuestLogin() {
        return allowGuestLogin;
    }

    public void setAllowGuestLogin(boolean allowGuestLogin) {
        this.allowGuestLogin = allowGuestLogin;
    }

    public boolean isImpersonate() {
        return impersonate;
    }

    public void setImpersonate(boolean impersonate) {
        this.impersonate = impersonate;
    }

    public SecurityFilterProviderCollection getProviders() {
        return providers;
    }

    public void setProviders(SecurityFilterProviderCollection providers) {
        this.providers = providers;
    }

}
