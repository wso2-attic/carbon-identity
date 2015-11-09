/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authenticator.social.live;

public class WindowsLiveOAuth2AuthenticatorConstants {

    public static final String AUTHENTICATOR_NAME = "MicrosoftWindowsLiveAuthenticator";
    public static final String AUTHENTICATOR_FRIENDLY_NAME = "Microsoft (Hotmail, MSN, Live)";

    public static final String CALLBACK_URL = "windows-live-callback-url";
    public static final String WINDOWS_LIVE_AUTHZ_URL = "AuthnEndpoint";
    public static final String WINDOWS_LIVE_TOKEN_URL = "AuthTokenEndpoint";
    public static final String WINDOWS_LIVE_USER_INFO_URL = "UserInfoEndpoint";
    public static final String USER_ID = "user_id";

    public static final String EMAIL_ADD_CLAIM_URI = "http://wso2.org/claims/emailaddress";
    public static final String GIVEN_NAME_CLAIM_URI = "http://wso2.org/claims/givenname";
    public static final String LAST_NAME_CLAIM_URI = "http://wso2.org/claims/lastname";
    public static final String GENDER_CLAIM_URI = "http://wso2.org/claims/gender";
    public static final String LOCALITY_CLAIM_URI = "http://wso2.org/claims/locality";

    private WindowsLiveOAuth2AuthenticatorConstants() {
    }
}
