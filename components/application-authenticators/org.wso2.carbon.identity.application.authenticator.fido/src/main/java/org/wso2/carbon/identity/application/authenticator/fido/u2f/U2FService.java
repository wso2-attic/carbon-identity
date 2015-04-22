/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.identity.application.authenticator.fido.u2f;


import com.yubico.u2f.U2F;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.AuthenticateRequestData;
import com.yubico.u2f.data.messages.RegisterRequestData;
import com.yubico.u2f.exceptions.U2fBadInputException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authenticator.fido.dao.DeviceStoreDAO;
import org.wso2.carbon.identity.application.authenticator.fido.dto.FIDOUser;
import org.wso2.carbon.identity.application.authenticator.fido.util.Util;
import org.wso2.carbon.identity.base.IdentityException;

import java.util.*;

public class U2FService {

	private static Log log = LogFactory.getLog(U2FService.class);

	private static volatile U2FService u2FService ;
//    static {
//        instance = new U2FService();
//    }
	private final U2F u2f = new U2F();
	private static Map<String, String> requestStorage = new HashMap<String, String>();
	private DeviceStoreDAO deviceStoreDAO = new DeviceStoreDAO();

	private Iterable<DeviceRegistration> getRegistrations(final FIDOUser user) {
		Util.logTrace("Executing {getRegistrations} method.", log);
		//		Collection<String> serializedRegistrations = Storage.retrieveFromUserStorage(username);
		Collection<String> serializedRegistrations = null;
		try {
			serializedRegistrations = deviceStoreDAO.getDeviceRegistration(user.getUsername());
		} catch (IdentityException e) {
			log.error("Error retrieving device registration from store", e);
		}
		List<DeviceRegistration> registrations = new ArrayList<DeviceRegistration>();
		for (String serialized : serializedRegistrations) {
			registrations.add(DeviceRegistration.fromJson(serialized));
		}
		Util.logTrace("Completed {getRegistrations} method.", log);
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
		Util.logTrace("Executing {startAuthentication} method", log);
		AuthenticateRequestData authenticateRequestData = null;

		try {
			authenticateRequestData =
					u2f.startAuthentication(user.getAppID(), getRegistrations(user));
		} catch (Exception e) {
			throw new AuthenticationFailedException("Could not start FIDO authentication", e);
		}

		requestStorage.put(authenticateRequestData.getRequestId(), authenticateRequestData.toJson());
		Util.logTrace("Completed {startAuthentication} method", log);
		return authenticateRequestData;
	}

	/**
	 * Finish FIDO authentication.
	 *
	 * @param user the FIDO user.
	 * @throws AuthenticationFailedException when validation fails.
	 */
	public void finishAuthentication(final FIDOUser user) throws AuthenticationFailedException {
		Util.logTrace("Executing {finishAuthentication} method", log);
		//AuthenticateResponse authenticateResponse = AuthenticateResponse.fromJson(response);

		AuthenticateRequestData authenticateRequest;
		try {
			authenticateRequest = AuthenticateRequestData
					.fromJson(requestStorage.remove(user.getAuthenticateResponse().getRequestId()));

			u2f.finishAuthentication(authenticateRequest, user.getAuthenticateResponse(),
			                         getRegistrations(user));

		} catch (Exception e) {
			throw new AuthenticationFailedException("Could not complete FIDO authentication", e);
		}
		Util.logTrace("Completed {finishAuthentication} method", log);
	}

	/**
	 * Initiate FIDO Device Registration.
	 *
	 * @param user the FIDO user.
	 * @throws IdentityException when U2F can not generate the challenge.
	 */
	public RegisterRequestData startRegistration(final FIDOUser user) throws IdentityException {
		Util.logTrace("Executing {startRegistration} method", log);

		RegisterRequestData registerRequestData = u2f.startRegistration(user.getAppID(), getRegistrations(user));
		requestStorage.put(registerRequestData.getRequestId(), registerRequestData.toJson());

		Util.logTrace("Completed {startRegistration} method", log);
		return registerRequestData;
	}

	/**
	 * Finish FIDO Device registration
	 *
	 * @param user the FIDO user.
	 * @return success or failure.
	 * @throws IdentityException when validation fails.
	 */
	public void finishRegistration(final FIDOUser user) throws IdentityException {
		Util.logTrace("Executing {finishRegistration} method", log);
		try {
			//RegisterResponse registerResponse = RegisterResponse.fromJson(response);

			RegisterRequestData registerRequestData = RegisterRequestData
					.fromJson(requestStorage.remove(user.getRegisterResponse().getRequestId()));

			//DeviceRegistration registration = u2f.finishRegistration(registerRequestData, user.getRegisterResponse());
			user.setDeviceRegistration(u2f.finishRegistration(registerRequestData, user.getRegisterResponse()));

			addRegistration(user);
			//			requestStorage.remove(registerResponse.getRequestId());

		} catch (Exception e) {
			throw new IdentityException("Could not complete FIDO registration", e);
		}
		Util.logTrace("Completed {finishRegistration} method", log);
	}

	private void addRegistration(FIDOUser user) throws IdentityException {
		//		Storage.storeToUserStorage(username, registration.toJsonWithAttestationCert());
		deviceStoreDAO.addDeviceRegistration(user.getUsername(), user.getDeviceRegistration());
	}

	/**
	 * Gets a U2FService instance.
	 *
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
}