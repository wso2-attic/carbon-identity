/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.application.authenticator.fido.service;

import com.yubico.u2f.data.messages.RegisterResponse;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.authenticator.fido.dto.FIDOUser;
import org.wso2.carbon.identity.application.authenticator.fido.u2f.U2FService;
import org.wso2.carbon.identity.application.authenticator.fido.util.FIDOAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.fido.util.FIDOUtil;
import org.wso2.carbon.identity.base.IdentityException;

/**
 * FIDO service class for FIDO registration.
 */
public class FIDOAdminService {

    private U2FService u2FService = U2FService.getInstance();

    /**
     * Initiate FIDO registration.
     *
     * @param appID Application ID.
     * @return RegisterRequestData.
     * @throws IdentityException when U2F can not generate challenge.
     */
    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public String startRegistration(String appID) throws IdentityException {

        FIDOUser user = getUser();
        user.setAppID(appID);
        return u2FService.startRegistration(user).toJson();

    }

    /**
     * Complete FIDO registration.
     *
     * @param response response from client.
     * @throws IdentityException when U2F validation fails.
     */
    @SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
    public void finishRegistration(String response) throws IdentityException {

        FIDOUser user = getUser();
        user.setRegisterResponse(RegisterResponse.fromJson(response));
        u2FService.finishRegistration(user);
    }

    private FIDOUser getUser() {
        String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String loggedInDomain = FIDOUtil.getDomainName(loggedInUser);
        String domainAwareUser = FIDOUtil.getUsernameWithoutDomain(loggedInUser);
        String loggedInTenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        return new FIDOUser(domainAwareUser, loggedInTenant, loggedInDomain);


    }
}
