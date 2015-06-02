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

package org.wso2.carbon.identity.workflow.mgt;

import org.wso2.carbon.identity.workflow.mgt.bean.BPSProfileBean;
import org.wso2.carbon.identity.workflow.mgt.bean.EventBean;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateBean;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateDeploymentDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateImplDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowBean;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class WorkflowAdminService {

    WorkflowService service = new WorkflowService();

    public EventBean[] listWorkflowEvents() {
        List<EventBean> events = service.listWorkflowEvents();
        return events.toArray(new EventBean[events.size()]);
    }

    public TemplateBean[] listWorkflowTemplates() {
        List<TemplateBean> templates = service.listWorkflowTemplates();
        return templates.toArray(new TemplateBean[templates.size()]);
    }

    public TemplateDTO getTemplateDTO(String templateName) {
        return service.getTemplateDTO(templateName);
    }

    public TemplateImplDTO getTemplateImplDTO(String template, String implName) {
        return service.getTemplateImplDTO(template, implName);
    }

    public void deployTemplate(TemplateDeploymentDTO deploymentDTO) {
        String uuid = UUID.randomUUID().toString();
        service.addWorkflow(uuid, deploymentDTO.getWorkflowName(), deploymentDTO.getWorkflowDescription(), deploymentDTO
                .getTemplateName(), deploymentDTO.getTemplateImplName(), deploymentDTO.getParameters(), deploymentDTO
                .getTemplateImplParameters());
        try {
            service.addAssociation(uuid, deploymentDTO.getAssociatedEvent(), deploymentDTO.getCondition());
        } catch (WorkflowException e) {
            //todo
        }

    }

    public void addBPSProfile(String profileName, String host, String user, char[] password) {
        service.addBPSProfile(profileName, host, user, password);
    }

    public BPSProfileBean[] listBPSProfiles() {
        List<BPSProfileBean> bpsProfiles = service.listBPSProfiles();
        return bpsProfiles.toArray(new BPSProfileBean[bpsProfiles.size()]);
    }

    public void removeBPSProfile(String profileName) {
        service.removeBPSProfile(profileName);
    }

    public void addWorkflow(String id, String name, String description, String templateId, String templateImpl,
                            Parameter[] templateParams, Parameter[] implParams) {
        service.addWorkflow(id, name, description, templateId, templateImpl, templateParams, implParams);
    }

    public void addAssociation(String workflowId, String eventId, String condition) throws WorkflowException {
        service.addAssociation(workflowId, eventId, condition);
    }

    public WorkflowBean[] listWorkflows() {
        List<WorkflowBean> workflows = service.listWorkflows();
        return workflows.toArray(new WorkflowBean[workflows.size()]);
    }

    public void removeWorkflow(String id) {
        service.removeWorkflow(id);
    }

    public void removeAssociation(String associationId) {
        service.removeAssociation(associationId);
    }
}
