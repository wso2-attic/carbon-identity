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

package org.wso2.carbon.identity.application.authenticator.iwa;

public class IWAConstants {

    public static final String OS_NAME_PROPERTY = "os.name";
    public static final String WINDOWS_OS_MATCH_STRING = "win";
    public static final String COMMON_AUTH_EP = "commonauth";
    public static final String IWA_AUTH_EP = "iwa";
    public static final String UTF_8 = "UTF-8";
    public static final String IWA_URL = "/iwa";
    public static final String IWA_PARAM_STATE = "state";
    public static final String SUBJECT_ATTRIBUTE = "javax.security.auth.subject";
    public static final String HTTP_CONNECTION_HEADER = "Connection";
    public static final String CONNECTION_CLOSE = "close";
    public static final String CONNECTION_KEEP_ALIVE = "keep-alive";
    public static final String AUTHENTICATE_HEADER = "WWW-Authenticate";
    public static final String NEGOTIATE_HEADER = "Negotiate";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String NTLM_PROLOG = "TlRMTVNT";
    public static final String SPN_NAME = "SPNName";
    public static final String SPN_PASSWORD = "SPNPassword";
    public static final String SERVER = "Server";
    public static final String USER_NAME = "user.name";
    public static final String OID = "1.3.6.1.5.5.2";

    private IWAConstants() {
    }
}
