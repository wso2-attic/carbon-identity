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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.authenticator.fido.dto.FIDOUser;
import org.wso2.carbon.identity.application.authenticator.fido.exception.FIDOAuthenticatorClientException;
import org.wso2.carbon.identity.application.authenticator.fido.exception.FIDOAuthenticatorServerException;
import org.wso2.carbon.identity.application.authenticator.fido.u2f.U2FService;
import org.wso2.carbon.identity.application.authenticator.fido.util.FIDOUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.ArrayList;

/**
 * FIDO service class for FIDO registration.
 */
public class FIDOAdminService {

    private U2FService u2FService = U2FService.getInstance();
    private static Log log = LogFactory.getLog(U2FService.class);

    /**
     * Initiate FIDO registration.
     *
     * @param appID Application ID.
     * @return RegisterRequestData.
     * @throws IdentityException when U2F can not generate challenge.
     */
    public String startRegistration(String appID) throws FIDOAuthenticatorClientException {

        FIDOUser user = getUser();
        user.setAppID(appID);
        try {
            return u2FService.startRegistration(user).toJson();
        } catch (FIDOAuthenticatorServerException e) {
          log.error("Error occurred while initiating device registration for User : " + user.getUserName(), e);
            throw new FIDOAuthenticatorClientException("Error occurred while initiating device registration");
        }

    }

    /**
     * Complete FIDO registration.
     *
     * @param response response from client.
     * @throws IdentityException when U2F validation fails.
     */
    public void finishRegistration(String response) throws FIDOAuthenticatorClientException {

        FIDOUser user = getUser();
        user.setRegisterResponse(RegisterResponse.fromJson(response));
        try {
            u2FService.finishRegistration(user);
        } catch (FIDOAuthenticatorServerException e) {
            log.error("Error occurred while finishing device registration for User : " + user.getUserName(), e);
            throw new FIDOAuthenticatorClientException("Error occurred while finishing device registration");
        }
    }

    /**
     * Remove registrations for logged in user
     *
     * @throws UserStoreException
     * @throws IdentityException
     */
    public void removeAllRegistrations() throws FIDOAuthenticatorClientException {
        FIDOUser user = getUser();
        try {
            u2FService.removeAllRegistrations(user);
        } catch (FIDOAuthenticatorServerException e) {
            log.error("Error occurred while deleting all registered device for User : " + user.getUserName(), e);
            throw new FIDOAuthenticatorClientException("Error occurred while deleting all registered device for user");
        }
    }

    public void removeRegistration(String deviceRemarks) throws FIDOAuthenticatorClientException {
        FIDOUser user = getUser();
        try {
            u2FService.removeRegistration(user, deviceRemarks);
        } catch (FIDOAuthenticatorServerException e) {
            log.error("Error occurred while deleting registered device for User : " + user.getUserName(), e);
            throw new FIDOAuthenticatorClientException("Error occurred while deleting registered device");
        }
    }

    /**
     * Check device registrations for logged in user
     *
     * @return
     * @throws UserStoreException
     * @throws IdentityException
     */
    public boolean isDeviceRegistered() throws FIDOAuthenticatorClientException {
        FIDOUser user = getUser();
        try {
            return u2FService.isDeviceRegistered(user);
        } catch (FIDOAuthenticatorServerException e) {
            log.error("Error occurred while getting device registration status for User : " + user.getUserName(), e);
            throw new FIDOAuthenticatorClientException("Error occurred while getting device registration status");
        }
    }

    public String[] getDeviceMetadataList() throws FIDOAuthenticatorClientException{
        FIDOUser user = getUser();
        ArrayList<String> deviceMetadataList ;
        try {
            deviceMetadataList =  u2FService.getDeviceMetadata(user);
            if (deviceMetadataList.size() > 0) {
                return deviceMetadataList.toArray(new String[deviceMetadataList.size()]);
            }
            return new String[0];
        }catch (FIDOAuthenticatorServerException e){

            log.error("Error occurred while getting registered device metadata list for User : " + user.getUserName(), e);
            throw  new FIDOAuthenticatorClientException("Error occurred while getting registered device metadata list");
        }
    }
    /**
     * Get logged in user details
     * @return
     */
    private FIDOUser getUser() {
        String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String loggedInDomain = FIDOUtil.getDomainName(loggedInUser);
        String domainAwareUser = FIDOUtil.getUsernameWithoutDomain(loggedInUser);
        String loggedInTenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        FIDOUser user = new FIDOUser(domainAwareUser, loggedInTenant, loggedInDomain);
        return user;


    }
}
