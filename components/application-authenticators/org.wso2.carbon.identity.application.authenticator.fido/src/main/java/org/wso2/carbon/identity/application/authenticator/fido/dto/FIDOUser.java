/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authenticator.fido.dto;

import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.AuthenticateResponse;
import com.yubico.u2f.data.messages.RegisterResponse;
import org.wso2.carbon.identity.application.authenticator.fido.util.FIDOAuthenticatorConstants;
import org.wso2.carbon.identity.application.common.model.User;

public class FIDOUser extends User {

    /**
     * The U2F AppID. Set this to the Web Origin of the login page, unless you need to support logging in from multiple
     * Web Origins.
     */
    private String appID;

    /**
     * A list of valid facets to verify against.
     */
    private String facets;

    /**
     * Response from the device/client for the RegisterRequest created by calling startRegistration.
     */
    private RegisterResponse registerResponse;

    /**
     * The devices currently registered to the user.
     */
    private DeviceRegistration deviceRegistration;

    /**
     * The response from the device/client for the AuthenticateRequestData created by calling startAuthentication.
     */
    private AuthenticateResponse authenticateResponse;

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public FIDOUser(final String username) {
        this.userName = username;
    }

    public FIDOUser(RegisterResponse registerResponse) {
        this.registerResponse = registerResponse;
    }

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public FIDOUser(final String appID, final String username) {
        this.appID = appID;
        this.userName = username;
    }

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public FIDOUser(final String username, final String tenantDomain,
                    final String userStoreDomain) {
        this.userName = username;
        this.tenantDomain = tenantDomain;
        this.userStoreDomain = userStoreDomain;
    }

    public FIDOUser(final String username, final String tenantDomain, final String userStoreDomain,
                    final RegisterResponse registerResponse) {
        this.userName = username;
        this.tenantDomain = tenantDomain;
        this.userStoreDomain = userStoreDomain;
        this.registerResponse = registerResponse;
    }

    public FIDOUser(String username, String tenantDomain, String userStoreDomain,
                    AuthenticateResponse authenticateResponse) {
        this.userName = username;
        this.tenantDomain = tenantDomain;
        this.userStoreDomain = userStoreDomain;
        this.authenticateResponse = authenticateResponse;
    }

    public FIDOUser(String username, String tenantDomain, String userStoreDomain, String appID) {
        this.userName = username;
        this.tenantDomain = tenantDomain;
        this.userStoreDomain = userStoreDomain;
        this.appID = appID;
    }

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public DeviceRegistration getDeviceRegistration() {
        return deviceRegistration;
    }

    public void setDeviceRegistration(final DeviceRegistration deviceRegistration) {
        this.deviceRegistration = deviceRegistration;
    }

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public AuthenticateResponse getAuthenticateResponse() {
        return authenticateResponse;
    }

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public void setAuthenticateResponse(final AuthenticateResponse authenticateResponse) {
        this.authenticateResponse = authenticateResponse;
    }

    public String getAppID() {
        return appID;
    }

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public void setAppID(final String appID) {
        this.appID = appID;
    }

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public String getFacets() {
        return facets;
    }

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public void setFacets(final String facets) {
        this.facets = facets;
    }

    public RegisterResponse getRegisterResponse() {
        return registerResponse;
    }

    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public void setRegisterResponse(RegisterResponse registerResponse) {
        this.registerResponse = registerResponse;
    }
}
