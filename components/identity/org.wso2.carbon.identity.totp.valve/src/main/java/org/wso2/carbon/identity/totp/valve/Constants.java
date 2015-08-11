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

package org.wso2.carbon.identity.totp.valve;

public class Constants {

	public static final String CONTEXT_PATH = "/totp";
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BASIC_AUTH_HEADER = "Basic";
	public static final String BEARER_AUTH_HEADER = "Bearer";
	public static final String LOCAL_AUTH_SERVER = "local://services";
	public static final String LOCAL_PREFIX = "local";

	public static final String PROPERTY_NAME_PRIORITY = "Priority";
	public static final String PROPERTY_NAME_AUTH_SERVER = "AuthorizationServer";
	public static final String PROPERTY_NAME_USERNAME = "UserName";
	public static final String PROPERTY_NAME_PASSWORD = "Password";
}
