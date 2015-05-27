/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.mgt.constants;

/**
 * Identity management related constants
 */
public class IdentityMgtConstants {

    public class PropertyConfig {

        public static final String CONFIG_FILE_NAME = "identity-mgt.properties";

        public static final String ACCOUNT_LOCK_ENABLE = "Account.Lock.Enable";

        public static final String  AUTH_POLICY_ENABLE = "Authentication.Policy.Enable";

        public static final String  AUTH_POLICY_ACCOUNT_EXIST = "Authentication.Policy.Check.Account.Exist";

        public static final String  AUTH_POLICY_ACCOUNT_LOCKING_FAIL_ATTEMPTS = "Authentication.Policy.Account.Lock.On.Failure.Max.Attempts";

    }

    public class Event {

        public static final String PRE_AUTHENTICATION = "PRE_AUTHENTICATION";

    }


    public class EventProperty {

        public static final String MODULE = "module";

        public static final String USER_NAME = "userName";

        public static final String USER_STORE_MANAGER = "userStoreManager";

        public static final String IDENTITY_MGT_CONFIG = "identityMgtConfig";

    }
}
