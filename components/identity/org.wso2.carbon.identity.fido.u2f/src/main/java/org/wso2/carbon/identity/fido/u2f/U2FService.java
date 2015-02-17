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

package org.wso2.carbon.identity.fido.u2f;

import com.yubico.u2f.U2F;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.AuthenticateRequestData;
import com.yubico.u2f.data.messages.AuthenticateResponse;
import com.yubico.u2f.data.messages.RegisterRequestData;
import com.yubico.u2f.data.messages.RegisterResponse;
import com.yubico.u2f.exceptions.U2fException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.fido.dao.DeviceStoreDAO;

import java.util.*;

/**
 * Created by ananthaneshan on 12/11/14.
 */
public class U2FService {

	private static final Log LOG = LogFactory.getLog(U2FService.class);
	public static boolean isTraceEnabled = LOG.isTraceEnabled();

	private static U2FService instance = new U2FService();
	private final U2F u2f = new U2F();
	private static Map<String, String> requestStorage = new HashMap<String, String>();
	private DeviceStoreDAO deviceStoreDAO = new DeviceStoreDAO();

	private Iterable<DeviceRegistration> getRegistrations(String username) {
		logAtTraceLevel("Executing {getRegistrations} method.");
		//		Collection<String> serializedRegistrations = Storage.retrieveFromUserStorage(username);
		Collection<String> serializedRegistrations = null;
		try {
			serializedRegistrations = deviceStoreDAO.getDeviceRegistration(username);
		} catch (IdentityException e) {
			LOG.error("Error retrieving device registration from store", e);
		}
		List<DeviceRegistration> registrations = new ArrayList<DeviceRegistration>();
		for (String serialized : serializedRegistrations) {
			registrations.add(DeviceRegistration.fromJson(serialized));
		}
		logAtTraceLevel("Completed {getRegistrations} method.");
		return registrations;
	}

	/**
	 * Initiate FIDO authentication.
	 *
	 * @param username the username.
	 * @param appID    the U2F AppID. Set this to the Web Origin of the login page, unless you need to
	 *                 support logging in from multiple Web Origins.
	 * @return AuthenticateRequestData.
	 * @throws AuthenticationFailedException when U2F can not generate the challenge
	 */
	public String startAuthentication(String username, String appID)
			throws AuthenticationFailedException {
		logAtTraceLevel("Executing {startAuthentication} method");
		AuthenticateRequestData authenticateRequestData = null;

		try {
			authenticateRequestData =
					u2f.startAuthentication(appID, getRegistrations(username));
		} catch (U2fException e) {
			throw new AuthenticationFailedException("Could not start FIDO authentication", e);
		}

		requestStorage.put(authenticateRequestData.getRequestId(), authenticateRequestData.toJson());
		logAtTraceLevel("Completed {startAuthentication} method");
		return authenticateRequestData.toJson();
	}

	/**
	 * Finish FIDO authentication.
	 *
	 * @param response The tokenResponse
	 * @param username The username
	 * @throws AuthenticationFailedException when validation fails.
	 */
	public void finishAuthentication(String response, String username) throws AuthenticationFailedException {
		logAtTraceLevel("Executing {finishAuthentication} method");
		AuthenticateResponse authenticateResponse = AuthenticateResponse.fromJson(response);

		AuthenticateRequestData authenticateRequest;
		try {
			authenticateRequest = AuthenticateRequestData
					.fromJson(requestStorage.remove(authenticateResponse.getRequestId()));

			u2f.finishAuthentication(authenticateRequest, authenticateResponse,
			                         getRegistrations(username));

		} catch (U2fException e) {
			throw new AuthenticationFailedException("Could not complete FIDO authentication", e);
		}
		logAtTraceLevel("Completed {finishAuthentication} method");
	}

	/**
	 * Initiate FIDO Device Registration.
	 *
	 * @param username The username.
	 * @param appID    the U2F AppID. Set this to the Web Origin of the login page, unless you need to
	 *                 support logging in from multiple Web Origins.
	 * @return String RegisterRequestData.
	 * @throws IdentityException when U2F can not generate the challenge.
	 */
	public String startRegistration(String username, String appID) throws IdentityException {
		logAtTraceLevel("Executing {startRegistration} method");
		String response;

		RegisterRequestData registerRequestData = u2f.startRegistration(appID, getRegistrations(username));
		requestStorage.put(registerRequestData.getRequestId(), registerRequestData.toJson());
		response = registerRequestData.toJson();

		logAtTraceLevel("Completed {startRegistration} method");
		return response;
	}

	/**
	 * Finish FIDO Device registration
	 *
	 * @param response The tokenResponse.
	 * @param username The username.
	 * @return success or failure.
	 * @throws IdentityException when validation fails.
	 */
	public String finishRegistration(String response, String username)
			throws IdentityException {
		logAtTraceLevel("Executing {finishRegistration} method");
		String status = "FAILED";
		try {
			RegisterResponse registerResponse = RegisterResponse.fromJson(response);

			RegisterRequestData registerRequestData = RegisterRequestData
					.fromJson(requestStorage.remove(registerResponse.getRequestId()));

			DeviceRegistration registration = u2f.finishRegistration(registerRequestData, registerResponse);

			addRegistration(username, registration);
			//			requestStorage.remove(registerResponse.getRequestId());
			status = "SUCCESS";

		} catch (U2fException e) {
			throw new IdentityException("Could not complete FIDO registration", e);
		}
		logAtTraceLevel("Completed {finishRegistration} method");
		return status;
	}

	private void addRegistration(String username, DeviceRegistration registration) throws IdentityException {
		//		Storage.storeToUserStorage(username, registration.toJsonWithAttestationCert());
		deviceStoreDAO.addDeviceRegistration(username, registration);
	}

	/*private void addRegistration(String username, DeviceRegistration registration, String appID)
			throws IdentityException {
		addRegistration(username, registration);
	}*/

	private void logAtTraceLevel(String message) {
		if (isTraceEnabled) {
			LOG.trace(message);
		}
	}

	/**
	 * Gets a U2FService instance.
	 *
	 * @return a U2FService.
	 */
	public static U2FService getInstance() {
		return instance;
	}
}
