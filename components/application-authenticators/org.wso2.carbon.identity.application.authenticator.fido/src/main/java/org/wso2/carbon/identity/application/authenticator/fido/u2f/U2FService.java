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

package org.wso2.carbon.identity.application.authenticator.fido.u2f;


import com.yubico.u2f.U2F;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.AuthenticateRequestData;
import com.yubico.u2f.data.messages.RegisterRequestData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authenticator.fido.dao.DeviceStoreDAO;
import org.wso2.carbon.identity.application.authenticator.fido.dto.FIDOUser;
import org.wso2.carbon.identity.application.authenticator.fido.exception.FIDOAuthenticatorServerException;
import org.wso2.carbon.identity.base.IdentityException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class U2FService {

    private static Log log = LogFactory.getLog(U2FService.class);

    private static volatile U2FService u2FService;
    private final U2F u2f = new U2F();
    private static Map<String, String> requestStorage = new HashMap<String, String>();

    /**
     * Gets a U2FService instance.
     * @return a U2FService.
     */
    public static U2FService getInstance() {

        if (u2FService == null) {
            synchronized (U2FService.class) {
                if (u2FService == null) {
                    u2FService = new U2FService();
                    return u2FService;
                } else {
                    return u2FService;
                }
            }
        } else {
            return u2FService;
        }
    }

    private U2FService() {

    }

    private Iterable<DeviceRegistration> getRegistrations(final FIDOUser user)
            throws FIDOAuthenticatorServerException {

        Collection<String> serializedRegistrations = null;
        serializedRegistrations = DeviceStoreDAO.getInstance().getDeviceRegistration(user.getUserName(),
                user.getTenantDomain(), user.getUserStoreDomain());

        List<DeviceRegistration> registrations = new ArrayList<DeviceRegistration>();
        for (String serialized : serializedRegistrations) {
            registrations.add(DeviceRegistration.fromJson(serialized));
        }

        return registrations;
    }

    /**
     * Initiate FIDO authentication.
     *
     * @param user the FIDO user.
     * @return AuthenticateRequestData.
     * @throws AuthenticationFailedException when U2F can not generate the challenge
     */
    public AuthenticateRequestData startAuthentication(final FIDOUser user)
            throws AuthenticationFailedException {
        AuthenticateRequestData authenticateRequestData = null;
        int numberOfRegistereddevice = 0;

        Iterable<DeviceRegistration> registeredDeviceList = null;
        try {
            registeredDeviceList = getRegistrations(user);
        } catch (Exception e) {
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
        if (registeredDeviceList instanceof Collection<?>) {
            numberOfRegistereddevice = ((Collection<?>) registeredDeviceList).size();
        }
        if (numberOfRegistereddevice > 0) {
            try {
                authenticateRequestData =
                        u2f.startAuthentication(user.getAppID(), registeredDeviceList);
            } catch (Exception e) {
                throw new AuthenticationFailedException("Could not start FIDO authentication", e);
            }

            requestStorage.put(authenticateRequestData.getRequestId(), authenticateRequestData.toJson());
        }
        return authenticateRequestData;
    }

    /**
     * Finish FIDO authentication.
     *
     * @param user the FIDO user.
     * @throws AuthenticationFailedException when validation fails.
     */
    public void finishAuthentication(final FIDOUser user) throws AuthenticationFailedException {

        AuthenticateRequestData authenticateRequest;
        try {
            authenticateRequest = AuthenticateRequestData
                    .fromJson(requestStorage.remove(user.getAuthenticateResponse().getRequestId()));

            u2f.finishAuthentication(authenticateRequest, user.getAuthenticateResponse(),
                    getRegistrations(user));

        } catch (Exception e) {
            throw new AuthenticationFailedException("Could not complete FIDO authentication", e);
        }
    }

    /**
     * Initiate FIDO Device Registration.
     *
     * @param user the FIDO user.
     * @throws IdentityException when U2F can not generate the challenge.
     */
    public RegisterRequestData startRegistration(final FIDOUser user)
            throws FIDOAuthenticatorServerException {

        RegisterRequestData registerRequestData = u2f.startRegistration(user.getAppID(), getRegistrations(user));
        requestStorage.put(registerRequestData.getRequestId(), registerRequestData.toJson());
        return registerRequestData;
    }

    /**
     * Finish FIDO Device registration
     *
     * @param user the FIDO user.
     * @return success or failure.
     * @throws IdentityException when validation fails.
     */
    public void finishRegistration(final FIDOUser user) throws FIDOAuthenticatorServerException {

        RegisterRequestData registerRequestData = RegisterRequestData
                .fromJson(requestStorage.remove(user.getRegisterResponse().getRequestId()));

        user.setDeviceRegistration(u2f.finishRegistration(registerRequestData, user.getRegisterResponse()));
        addRegistration(user);
    }

    private void addRegistration(FIDOUser user) throws FIDOAuthenticatorServerException {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        DeviceStoreDAO.getInstance().addDeviceRegistration(user.getUserName(), user.getDeviceRegistration(),
                user.getTenantDomain(), user.getUserStoreDomain(), timestamp);
    }

    public boolean isDeviceRegistered(FIDOUser user) throws FIDOAuthenticatorServerException {
        Collection<String> registrations = DeviceStoreDAO.getInstance().getDeviceRegistration(user.getUserName(),
                user.getTenantDomain(), user.getUserStoreDomain());
        if (!registrations.isEmpty()) {
            return true;
        } else {
            return false;
        }

    }

    public ArrayList<String> getDeviceMetadata(FIDOUser user) throws FIDOAuthenticatorServerException{
        return DeviceStoreDAO.getInstance().getDeviceMetadata(user.getUserName(), user.getTenantDomain(),
                user.getUserStoreDomain());

    }

    public void removeAllRegistrations(FIDOUser user) throws FIDOAuthenticatorServerException {
        DeviceStoreDAO.getInstance().removeAllRegistrations(user.getUserName(), user.getTenantDomain(),
                user.getUserStoreDomain());
    }

    public void removeRegistration(FIDOUser user, String deviceRemarks)
            throws FIDOAuthenticatorServerException {
        DeviceStoreDAO.getInstance().removeRegistration(user.getUserName(), user.getTenantDomain(),
                user.getUserStoreDomain(), Timestamp.valueOf(deviceRemarks));

    }
}