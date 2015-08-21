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
    public static final String ACTION_VALUE_BACK = "back";
    public static final String ACTION_VALUE_FINISH = "finishAdd";
    public static final String ACTION_VALUE_EDIT = "edit";
    public static final String ACTION_VALUE_DEPLOY = "deploy";
    public static final String ACTION_VALUE_DELETE = "delete";
    public static final String ACTION_VALUE_UPDATE = "update";
    public static final String ACTION_VALUE_ENABLE = "enable";
    public static final String ACTION_VALUE_DISABLE = "disable";
    public static final String ACTION_VALUE_DELETE_ASSOCIATION = "deleteAssociation";
    public static final String ACTION_VALUE_ADD_ASSOCIATION = "addAssociation";

    public static final String ATTRIB_WORKFLOW_WIZARD = "workflowAddWizard";

    public static final String PARAM_WORKFLOW_NAME = "workflowName";
    public static final String PARAM_WORKFLOW_ID = "workflowId";
    public static final String PARAM_ASSOCIATION_ID = "associationId";
    public static final String PARAM_ASSOCIATION_NAME = "associationName";
    public static final String PARAM_WORKFLOW_DESCRIPTION = "workflowDescription";
    public static final String PARAM_TEMPLATE_IMPL = "templateImpl";
    public static final String PARAM_WORKFLOW_TEMPLATE = "workflowTemplate";
    public static final String VALUE_EXISTING_SERVICE = "EXISTING";
    public static final String PARAM_OPERATION = "operation";
    public static final String PARAM_OPERATION_CATEGORY = "opertaionCategory";

    public static final String PARAM_BPS_PROFILE_NAME = "bpsProfileName";
    public static final String PARAM_BPS_HOST = "bpsHost";
    public static final String PARAM_BPS_AUTH_USER = "bpsUser";
    public static final String PARAM_BPS_AUTH_PASSWORD = "bpsPassword";
    public static final String PARAM_SERVICE_ACTION = "serviceAction";
    public static final String PARAM_SERVICE_AUTH_USERNAME = "serviceUser";
    public static final String PARAM_SERVICE_AUTH_PASSWORD = "serviceUserPassword";

    public static final String PARAM_SERVICE_ASSOCIATION_PRIORITY = "priority";
    public static final String PARAM_ASSOCIATION_CONDITION = "condition";

    public static final String PARAM_PROCESS_NAME = "processName";
    public static final String PARAM_CARBON_HOST = "carbonHost";
    public static final String PARAM_CARBON_AUTH_USER = "carbonUser";
    public static final String PARAM_CARBON_AUTH_PASSWORD = "carbonUserPassword";

    public static final String PARAM_REQUEST_ID = "requestId";

    public static final String PARAM_PAGE_NUMBER = "pageNumber";
    public static final int RESULTS_PER_PAGE = 20;

    public static class ParamTypes {
        public static final String INTEGER = "INTEGER";
        public static final String DOUBLE = "DOUBLE";
        public static final String STRING = "STRING";
        public static final String LONG_STRING = "TEXT";
        public static final String BOOLEAN = "BOOLEAN";
        public static final String PASSWORD = "PASSWORD";
        public static final String USER_ROLE = "USER_ROLE";
        public static final String USER_NAME = "USER_NAME";
        public static final String BPS_PROFILE = "BPS_PROFILE";
        public static final String USER_NAME_OR_USER_ROLE = "USER_NAME_OR_USER_ROLE";
    }
}
