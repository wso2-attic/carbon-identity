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
package org.wso2.carbon.identity.application.authenticator.fido.util;

import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.user.core.UserCoreConstants;

import javax.servlet.http.HttpServletRequest;

/**
 * FIDOUtil class for FIDO authentication component.
 */
public class FIDOUtil {
    private FIDOUtil() {
    }

	public static String getOrigin(HttpServletRequest request) {

		return request.getScheme() + "://" + request.getServerName() + ":" +
		       request.getServerPort();
	}

    public static String getUniqueUsername(HttpServletRequest request, String username) {
        return request.getServerName() + "/" + username;
    }

    public static String getDomainName(String username) {
        int index = username.indexOf(CarbonConstants.DOMAIN_SEPARATOR);
        if (index < 0) {
            return UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
        }
        return username.substring(0, index);
    }

    public static String getUsernameWithoutDomain(String username) {
        int index = username.indexOf(CarbonConstants.DOMAIN_SEPARATOR);
        if (index < 0) {
            return username;
        }
        return username.substring(index + 1, username.length());
    }
}
