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

package org.wso2.carbon.identity.workflow.mgt.ws;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.workflow.mgt.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.bean.WSServiceBean;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowServicesDAO;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class WSWorkflowAdminService {

    private static Log log = LogFactory.getLog(WSWorkflowAdminService.class);

    WorkflowServicesDAO servicesDAO = new WorkflowServicesDAO();

    public void addService(WSServiceBean service) throws WorkflowException {
        if (service != null) {
            if (StringUtils.isBlank(service.getAlias())) {
                throw new WorkflowException("Service alias cannot be null or empty");
            }
            if (StringUtils.isBlank(service.getServiceEndpoint())) {
                throw new WorkflowException("Service endpoint cannot be null or empty");
            }
        }
        try {
            servicesDAO.addWorkflowService(service);
        } catch (WorkflowException e) {
            log.error("Error while adding service.", e);
            throw new WorkflowException("Error occurred when adding the service.");
        }
    }

    public void removeService(String alias) throws WorkflowException {
        if (StringUtils.isBlank(alias)) {
            throw new WorkflowException("Service alias cannot be null or empty");
        }
        try {
            servicesDAO.removeWorkflowService(alias);
        } catch (WorkflowException e) {
            log.error("Error while adding service.", e);
            throw new WorkflowException("Error occurred when removing service");
        }
    }

    public void associateServiceToEvent(String serviceAlias, String eventType, String condition, int priority)
            throws WorkflowException {
        if (StringUtils.isBlank(serviceAlias)) {
            throw new WorkflowException("Service alias cannot be null");
        }
        if (StringUtils.isBlank(eventType)) {
            throw new WorkflowException("Event type cannot be null");
        }

        if (StringUtils.isBlank(condition)) {
            throw new WorkflowException("Condition cannot be null");
        }

        //check for xpath syntax errors
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            xpath.compile(condition);
        } catch (XPathExpressionException e) {
            throw new WorkflowException("The condition:" + condition + " is not an valid xpath expression.", e);
        }
        servicesDAO.associateServiceWithEvent(serviceAlias, eventType, condition, priority);
    }
}
