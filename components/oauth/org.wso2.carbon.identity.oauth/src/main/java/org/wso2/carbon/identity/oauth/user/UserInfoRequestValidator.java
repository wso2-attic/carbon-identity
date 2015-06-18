/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

/**
 *
 */
package org.wso2.carbon.identity.oauth.user;

import javax.servlet.http.HttpServletRequest;

/**
 * Validates the User Info request against the specification
 */
public interface UserInfoRequestValidator {

    /**
     * Validates the UserInfo request and return the access token if validation
     * succeed. If validation failed, an {@code UserInfoEndpointException} will be
     * thrown. Scope validation must be done at the access token validation
     *
     * @param request
     * @return
     * @throws UserInfoEndpointException
     */
    public String validateRequest(HttpServletRequest request) throws UserInfoEndpointException;

}
