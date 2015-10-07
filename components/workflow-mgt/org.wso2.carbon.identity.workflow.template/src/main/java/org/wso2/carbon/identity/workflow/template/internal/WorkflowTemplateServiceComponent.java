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

package org.wso2.carbon.identity.workflow.template.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowRuntimeException;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractTemplate;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;
import org.wso2.carbon.identity.workflow.template.TemplateConstant;
import org.wso2.carbon.identity.workflow.template.MultiStepApprovalTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @scr.component name="org.wso2.carbon.identity.workflow.template" immediate="true"
 */
public class WorkflowTemplateServiceComponent {

    private static Log log = LogFactory.getLog(WorkflowTemplateServiceComponent.class);


    protected void activate(ComponentContext context) {

        try {
            BundleContext bundleContext = context.getBundleContext();

            String templateParamMetaDataXML = readTemplateParamMetaDataXML(TemplateConstant.TEMPLATE_PARAMETER_METADATA_FILE_NAME);
            bundleContext.registerService(AbstractTemplate.class, new MultiStepApprovalTemplate(templateParamMetaDataXML),null);

        }catch(Throwable e){
            log.error("Error occurred while activating WorkflowTemplateServiceComponent bundle, " + e.getMessage());
        }
    }


    private String readTemplateParamMetaDataXML(String fileName) throws WorkflowRuntimeException{
        String content = null ;
        try {
            InputStream resourceAsStream = this.getClass().getClassLoader()
                    .getResourceAsStream(fileName);
            content = WorkflowManagementUtil.readFileFromResource(resourceAsStream);
        } catch (URISyntaxException e) {
            String errorMsg = "Error occurred while reading file from class path, " + e.getMessage() ;
            log.error(errorMsg);
            throw new WorkflowRuntimeException(errorMsg,e);
        } catch (IOException e) {
            String errorMsg = "Error occurred while reading file from class path, " + e.getMessage() ;
            log.error(errorMsg);
            throw new WorkflowRuntimeException(errorMsg,e);
        }
        return content ;
    }



}
