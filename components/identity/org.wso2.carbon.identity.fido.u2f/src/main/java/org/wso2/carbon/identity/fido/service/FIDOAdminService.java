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

package org.wso2.carbon.identity.fido.service;

import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.fido.u2f.U2FService;

/**
 * Created by ananthaneshan on 12/11/14.
 */

/**
 * FIDO service class for FIDO registration.
 */
public class FIDOAdminService {
	private static final U2FService U_2_F_SERVICE = new U2FService();

	/**
	 * Initiate FIDO registration.
	 *
	 * @param username username.
	 * @param appID    Application ID.
	 * @return RegisterRequestData.
	 * @throws IdentityException when U2F can not generate challenge.
	 */
	public String startRegistration(String username, String appID)
			throws IdentityException {
		return U_2_F_SERVICE.startRegistration(username, appID);
	}

	/**
	 * Complete FIDO registration.
	 *
	 * @param response response from client.
	 * @param username username associated with initiate request.
	 * @param appID    Application ID associated with the initiate.
	 * @return String "success or failure".
	 * @throws IdentityException when U2F validation fails.
	 */
	public String finishRegistration(String response, String username, String appID)
			throws IdentityException {
		return U_2_F_SERVICE.finishRegistration(response, username);
	}
}
