/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authenticator.facebook;

public class FacebookAuthenticatorConstants {

    public static final String AUTHENTICATOR_NAME = "FacebookAuthenticator";
    // TODO : Change login type
    public static final String FACEBOOK_LOGIN_TYPE = "facebook";

    public static final String LOGIN_TYPE = "loginType";

    public static final String OAUTH2_GRANT_TYPE_CODE = "code";
    public static final String OAUTH2_PARAM_STATE = "state";
    public static final String USERNAME = "id";

    public static final String FB_AUTHZ_URL = "http://www.facebook.com/dialog/oauth";
    public static final String FB_TOKEN_URL = "https://graph.facebook.com/oauth/access_token";
    public static final String FB_USER_INFO_URL = "https://graph.facebook.com/me";
    public static final String SCOPE = "email";

    public static final String CLIENT_ID = "ClientId";
    public static final String CLIENT_SECRET = "ClientSecret";

    private FacebookAuthenticatorConstants() {
    }
}