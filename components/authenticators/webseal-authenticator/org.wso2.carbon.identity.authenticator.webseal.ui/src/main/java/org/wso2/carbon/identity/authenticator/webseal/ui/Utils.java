/*
*  Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.authenticator.webseal.ui;

import java.util.Map;
import org.wso2.carbon.core.security.AuthenticatorsConfiguration;

/**
 * .
 */
public class Utils {

    public static final String LOG_OUT_URL = "LogOutUrl";

    public static final String Filter_PAGE = "FilterPage";

    private static String  logOutUrl;

    private static String  filterPage;

    public static void initConfig(){

        AuthenticatorsConfiguration authenticatorsConfiguration = AuthenticatorsConfiguration.getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig =
                authenticatorsConfiguration.getAuthenticatorConfig(WebSealUIAuthenticator.AUTHENTICATOR_NAME);
        Map<String, String> parameters = null;
        if (authenticatorConfig != null) {
            parameters = authenticatorConfig.getParameters();
            logOutUrl = parameters.get(LOG_OUT_URL);
            filterPage = parameters.get(Filter_PAGE);
        }
    }

    public static  String getLogOutUrl() {
        return logOutUrl;
    }

    public static  String getLogOutPage() {
        return filterPage;
    }
}
