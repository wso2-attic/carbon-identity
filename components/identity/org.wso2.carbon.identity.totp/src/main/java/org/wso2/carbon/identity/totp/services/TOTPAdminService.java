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
package org.wso2.carbon.identity.totp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.totp.TOTPDTO;
import org.wso2.carbon.identity.totp.TOTPKeyGenerator;
import org.wso2.carbon.identity.totp.exception.TOTPException;

public class TOTPAdminService {

	private static Log log = LogFactory.getLog(TOTPAdminService.class);

	/**
	 * Generate TOTP Token for the give user
	 *
	 * @param username username of the user
	 * @return
	 * @throws TOTPException
	 */
	public String initTOTP(String username) throws TOTPException {
		TOTPDTO totpdto = null;
		try {
			totpdto = TOTPKeyGenerator.getInstance().generateTOTPKeyLocal(username);
			return totpdto.getQRCodeURL();
		} catch (TOTPException e) {
			log.error("TOTPAdminService failed to generateTOTP key for the user : " + username, e);
			throw new TOTPException("TOTPAdminService failed to generateTOTP key for the user : " + username, e);
		}
	}

	/**
	 * reset TOTP credentials of the user
	 *
	 * @param username of the user
	 * @return
	 * @throws TOTPException
	 */
	public boolean resetTOTP(String username) throws TOTPException {
		return TOTPKeyGenerator.getInstance().resetLocal(username);
	}

}
