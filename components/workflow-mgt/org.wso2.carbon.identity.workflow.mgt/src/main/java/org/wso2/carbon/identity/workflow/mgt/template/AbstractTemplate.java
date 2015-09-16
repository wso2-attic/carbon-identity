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

package org.wso2.carbon.identity.workflow.mgt.template;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.workflow.mgt.bean.metadata.ParametersMetaData;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowRuntimeException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;

import javax.xml.bind.JAXBException;

/**
 * Abstract Template class can be extend to create concrete
 * template that is providing its own details and metadata
 *
 */
public abstract class AbstractTemplate {

    private Log log = LogFactory.getLog(AbstractTemplate.class);
    private ParametersMetaData parameterMetaDatas = null ;

    /**
     * AbstractTemplate Constructor with metadata xml string parameter
     *
     * @param metaDataXML metadata xml string that is validated against ParameterMetaData.xsd
     * @throws WorkflowRuntimeException
     */
    public AbstractTemplate(String metaDataXML) throws WorkflowRuntimeException {
        try {
            this.parameterMetaDatas = WorkflowManagementUtil.unmarshalXML(metaDataXML, ParametersMetaData.class);
        } catch (JAXBException e) {
            String errorMsg = "Error occured while converting template parameter data to object : " + e.getMessage();
            log.error(errorMsg);
            throw new WorkflowRuntimeException(errorMsg, e);
        }
    }

    /**
     * Retrieve template specific metadata
     *
     * @return ParametersMetaData object that is contain all the template specific parameter metadata.
     */
    public ParametersMetaData getParameterMetaDatas() {
        return parameterMetaDatas;
    }

    /**
     * Template Id is unique representation of the template
     * @return String templateId
     */
    public abstract String getTemplateId();

    /**
     * Template Name
     * @return String Template Name
     */
    public abstract String getName();

    /**
     * Template Description
     *
     * @return String description
     */
    public abstract String getDescription();
}
