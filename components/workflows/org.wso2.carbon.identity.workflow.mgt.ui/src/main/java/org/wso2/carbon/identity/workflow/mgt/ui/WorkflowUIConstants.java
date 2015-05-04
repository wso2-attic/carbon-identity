/*
 * Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.workflow.mgt.ui;

import java.util.HashMap;
import java.util.Map;

public class WorkflowUIConstants {
    public static final String PARAM_ACTION = "action";
    public static final String ACTION_VALUE_ADD = "addNew";
    public static final String ACTION_VALUE_FINISH = "finishAdd";
    public static final String ACTION_VALUE_EDIT = "edit";
    public static final String ACTION_VALUE_DEPLOY = "deploy";

    public static final String PARAM_SERVICE_ALIAS = "serviceAlias";
    public static final String PARAM_SERVICE_TEMPLATE = "serviceTemplate";
    public static final String VALUE_EXISTING_SERVICE = "EXISTING";
    public static final String PARAM_SERVICE_ASSOCIATION_EVENT = "event";

    public static final String PARAM_SERVICE_EPR = "serviceEPR";
    public static final String PARAM_SERVICE_ACTION = "serviceAction";
    public static final String PARAM_SERVICE_AUTH_USERNAME = "serviceUser";
    public static final String PARAM_SERVICE_AUTH_PASSWORD = "serviceUserPassword";

    public static final String PARAM_SERVICE_ASSOCIATION_PRIORITY = "priority";
    public static final String PARAM_SERVICE_ASSOCIATION_CONDITION = "condition";

    public static final String PARAM_PROCESS_NAME = "processName";
    public static final String PARAM_BPS_HOST = "bpsHost";
    public static final String PARAM_CARBON_HOST = "carbonHost";
    public static final String PARAM_CARBON_AUTH_USER = "carbonUser";
    public static final String PARAM_CARBON_AUTH_PASSWORD = "carbonUserPassword";


    public static final String PARAM_PAGE_NUMBER = "pageNumber";
    public static final int SERVICES_PER_PAGE = 20;

    public static final Map<String,String> TEMPLATE_MAP;

    static {
        TEMPLATE_MAP = new HashMap<>();
        TEMPLATE_MAP.put("simple-approval", "Simple Approval");
    }
}
