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

package org.wso2.carbon.identity.application.authenticator.fido.service;

import com.yubico.u2f.data.messages.RegisterResponse;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authenticator.fido.dto.FIDOUser;
import org.wso2.carbon.identity.application.authenticator.fido.u2f.U2FService;
import org.wso2.carbon.identity.application.authenticator.fido.util.FIDOAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.fido.util.FIDOUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;

/**
 * FIDO service class for FIDO registration.
 */
public class FIDOAdminService {

    private U2FService u2FService = U2FService.getInstance();

	/**
	 * Initiate FIDO registration.
	 *
	 * @param username username.
	 * @param appID    Application ID.
	 * @return RegisterRequestData.
	 * @throws IdentityException when U2F can not generate challenge.
	 */
	@SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
	public String startRegistration(String username, String appID) throws IdentityException {

//        String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
//        String loggedInDomain = FIDOUtil.getDomainName(loggedInUser);
//        String domainAwareUser = FIDOUtil.getUsernameWithoutDomain(loggedInUser);
//        String loggedInTenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
//
////        U2FService u2FService = U2FService.getInstance();
////        IdentityUtil.getTenantIdOFUser(username);
//
//		FIDOUser user = new FIDOUser(domainAwareUser, loggedInTenant, loggedInDomain, appID);
        FIDOUser user = getUser();
        user.setAppID(appID);
        return u2FService.startRegistration(user).toJson();

	}

	/**
	 * Complete FIDO registration.
	 *
	 * @param response response from client.
	 * @param username username associated with initiate request.
	 * @throws IdentityException when U2F validation fails.
	 */
	@SuppressWarnings(FIDOAuthenticatorConstants.UNUSED)
	public void finishRegistration(String response, String username) throws IdentityException {

        FIDOUser user = getUser();
        user.setRegisterResponse(RegisterResponse.fromJson(response));
//
//        U2FService u2FService = U2FService.getInstance();
//        //TODO enhancement: tenant domain, user store domain
//		FIDOUser user = new FIDOUser(username, "", "", RegisterResponse.fromJson(response));
        u2FService.finishRegistration(user);
	}

    private FIDOUser getUser(){
        String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String loggedInDomain = FIDOUtil.getDomainName(loggedInUser);
        String domainAwareUser = FIDOUtil.getUsernameWithoutDomain(loggedInUser);
        String loggedInTenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        FIDOUser user = new FIDOUser(domainAwareUser, loggedInTenant, loggedInDomain);
        return user;


    }
}
