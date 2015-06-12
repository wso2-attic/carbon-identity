/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sts.mgt;

/**
 * This class defines the constants used throughout this component
 */
public class STSMgtConstants {

    public static class TokenType {
        public static final String SAML10 = "SAML10";
        public static final String SAML11 = "SAML11";
        public static final String SAML20 = "SAML20";
        public static final String OpenID = "OpenID";

        private TokenType() {
        }
    }

    public static class Policy {
        public static final String POLICY_SCENARIO18 = "scenario18";
        public static final String POLICY_SCENARIO19 = "scenario19";

        private Policy() {
        }
    }

    public static class ServerConfigProperty {
        public static final String SECURITY_KEYSTORE_LOCATION = "Security.KeyStore.Location";
        public static final String STS_TIME_TO_LIVE = "STSTimeToLive";

        private ServerConfigProperty() {
        }
    }

    private STSMgtConstants() {
    }
}
