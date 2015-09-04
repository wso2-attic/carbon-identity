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

package org.wso2.carbon.identity.workflow.mgt.util;

import java.util.HashSet;
import java.util.Set;

public class WorkFlowConstants {

    public static final String WF_STATUS_NO_MATCHING_EXECUTORS = "NO_MATCHING_EXECUTOR";
    public static final String DEFAULT_BPS_PROFILE = "embeded_bps";
    public static final String PROFILE = "profile";
    public static final String DEFAULT_CARBON_PROFILE = "default";
    public static final String WORKFLOW_PROFILE = "workflow" ;

    public static final String WORKFLOW_DOMAIN = "Workflow";

    public static final Set<Class> NUMERIC_CLASSES;

    static {
        NUMERIC_CLASSES = new HashSet<>();
        NUMERIC_CLASSES.add(Integer.class);
        NUMERIC_CLASSES.add(Long.class);
        NUMERIC_CLASSES.add(Short.class);
        NUMERIC_CLASSES.add(Character.class);
        NUMERIC_CLASSES.add(Byte.class);
        NUMERIC_CLASSES.add(Float.class);
        NUMERIC_CLASSES.add(Double.class);
    }

    public static class TemplateConstants {

        public static final String APPROVAL_TEMPLATE_ID = "SimpleApproval";
        public static final String SIMPLE_APPROVAL_USER_OR_ROLE_NAME = "UserNameOrRoleName";
        public static final String SIMPLE_APPROVAL_USERS_OR_ROLES_DISPLAY_NAME = "Users or Roles to Authorize the Request";

        public static final String IMMEDIATE_DENY_TEMPLATE_ID = "ImmediateDeny";

        public static final String BPEL_IMPL_BPS_PROFILE = "BPELEngineProfile";

        public static final String HOST = "Host";

        public static final String WORKFLOW_NAME = "WorkflowName";
        public static final String AUTH_USER = "User";
        public static final String AUTH_USER_PASSWORD = "UserPassword";

        public static final String CALLBACK_USER = "CarbonUser";
        public static final String CALLBACK_USER_PASSWORD = "CarbonUserPassword";

        public static final String HT_SUBJECT = "HTSubject";
        public static final String HT_DESCRIPTION = "HTDescription";
        public static final String SERVICE_ACTION = "ServiceAction";
        public static final String SERVICE_SUFFIX = "Service";

        public static final String DEFAULT_APPROVAL_BPEL_SOAP_ACTION = "http://bpel.mgt.workflow.carbon.wso2" +
                ".org/approvalProcess/initiate";
    }
}
