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

package org.wso2.carbon.identity.workflow.impl.listener;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.base.IdentityValidationUtil;
import org.wso2.carbon.identity.workflow.impl.WFImplConstant;
import org.wso2.carbon.identity.workflow.impl.WorkflowImplException;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;

public class WorkflowImplValidationListener extends AbstractWorkflowImplServiceListener {

    @Override
    public void doPreAddBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException {

        if ((!IdentityValidationUtil.isValidOverWhiteListPatterns(bpsProfileDTO.getProfileName(), new
                String[]{IdentityValidationUtil.ValidatorPattern.ALPHANUMERICS_ONLY.toString()}) && !StringUtils
                .equals(bpsProfileDTO.getProfileName(), WFImplConstant.DEFAULT_BPS_PROFILE_NAME)) || StringUtils
                .isBlank(bpsProfileDTO.getProfileName())) {
            throw new WorkflowImplException("Profile name should be a not null alpha numeric string, if its not the " +
                    "default embedded BPS.");
        }
    }


}
