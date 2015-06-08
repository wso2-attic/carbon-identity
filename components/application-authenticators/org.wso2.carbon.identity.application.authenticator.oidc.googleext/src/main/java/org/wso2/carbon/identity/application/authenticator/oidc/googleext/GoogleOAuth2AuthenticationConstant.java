/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.application.authenticator.oidc.googleext;

public class GoogleOAuth2AuthenticationConstant {
    private GoogleOAuth2AuthenticationConstant() {
    }

    public static final String GOOGLE_OAUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/auth";
    public static final String GOOGLE_TOKEN_ENDPOINT = "https://accounts.google.com/o/oauth2/token";
    public static final String GOOGLE_USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo?schema=openid";
    public static final String GOOGLE_CONNECTOR_FRIENDLY_NAME = "Google OAuth2/OpenID Connect";
    public static final String GOOGLE_CONNECTOR_NAME = "GoogleOAUth2OpenIDAuthenticator";
    public static final String QUERY_STRING = "scope=openid%20email%20profile";
    public static final String CALLBACK_URL = "Google-callback-url";
}
