/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.tools.xacml.validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.entitlement.dto.PolicyDTO;
import org.wso2.carbon.identity.tools.xacml.validator.bean.ValidationResult;
import org.wso2.carbon.identity.tools.xacml.validator.processors.XACMLPolicyValidator;

/**
 * XACMLPolicyValidatorService class which exposes xacml policy processors
 * service
 */

public class XACMLPolicyValidatorService extends AbstractAdmin {

    private static Log log = LogFactory.getLog(XACMLPolicyValidatorService.class);

    /**
     * validate policy according to XACML schema
     * 
     * @param policyDTO
     *            include policy content
     * @return ValidationResult array
     */
    public ValidationResult[] validateXACMLPolicy(PolicyDTO policyDTO) {//change ValidationResult to ValidationResult
        if (log.isDebugEnabled()) {
            log.debug("XACML Policy Content :" + policyDTO.getPolicy());
        }
        return XACMLPolicyValidator.validatePolicy(policyDTO);
    }

}
