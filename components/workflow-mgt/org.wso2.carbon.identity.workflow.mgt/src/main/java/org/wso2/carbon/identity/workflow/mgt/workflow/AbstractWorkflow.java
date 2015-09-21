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

package org.wso2.carbon.identity.workflow.mgt.workflow;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.bean.metadata.ParametersMetaData;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowRuntimeException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;

import javax.xml.bind.JAXBException;
import java.util.List;

public abstract class AbstractWorkflow {

    private Log log = LogFactory.getLog(AbstractWorkflow.class);

    private ParametersMetaData parametersMetaData = null ;
    private String templateId ;
    /**
     *
     *
     * @param metaDataXML Parameter Metadata XML string
     * @throws WorkflowRuntimeException
     */
    public AbstractWorkflow(String metaDataXML) throws WorkflowRuntimeException {
        try {
            this.parametersMetaData = WorkflowManagementUtil.unmarshalXML(metaDataXML, ParametersMetaData.class);
        } catch (JAXBException e) {
            String errorMsg = "Error occured while converting workflow parameter data to object : " + e.getMessage();
            log.error(errorMsg);
            throw new WorkflowRuntimeException(errorMsg, e);
        }
    }


    @Deprecated
    public void activate(List<Parameter> parameterList) throws WorkflowException {

        TemplateInitializer initializer = getInitializer();
        if (initializer != null && initializer.initNeededAtStartUp()) {
            deploy(parameterList);
        }
    }

    public void initializeExecutor(List<Parameter> parameterList) throws WorkflowException {

        WorkFlowExecutor executor = getExecutor();
        if (executor != null) {
            executor.initialize(parameterList);
        }
    }

    public void deploy(List< Parameter> parameterList) throws WorkflowException {

        TemplateInitializer initializer = getInitializer();
        if (initializer != null) {
            initializer.initialize(parameterList);
        }
    }

    public void execute(WorkflowRequest workFlowRequest) throws WorkflowException {

        WorkFlowExecutor executor = getExecutor();
        if (executor != null) {
            executor.execute(workFlowRequest);
        }
    }

    public ParametersMetaData getParametersMetaData() {
        return parametersMetaData;
    }

    public void setParametersMetaData(ParametersMetaData parametersMetaData) {
        this.parametersMetaData = parametersMetaData;
    }
/*
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof AbstractWorkflow)) {
            return false;
        }
        AbstractWorkflow that = (AbstractWorkflow) o;
        return StringUtils.equals(getTemplateId(), that.getTemplateId()) &&
                StringUtils.equals(getImplementationId(), that
                        .getImplementationId());

    }

    @Override
    public int hashCode() {

        int result = getTemplateId() != null ? getTemplateId().hashCode() : 0;
        result = 31 * result + (getImplementationId() != null ? getImplementationId().hashCode() : 0);
        return result;
    }
*/

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    protected abstract TemplateInitializer getInitializer();
    protected abstract WorkFlowExecutor getExecutor();

    public abstract String getWorkflowImplId();
    public abstract String getWorkflowImplName();
}
