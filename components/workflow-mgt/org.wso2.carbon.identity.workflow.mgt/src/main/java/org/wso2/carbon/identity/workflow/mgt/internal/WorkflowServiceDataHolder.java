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

package org.wso2.carbon.identity.workflow.mgt.internal;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.identity.workflow.mgt.AbstractWorkflowTemplate;
import org.wso2.carbon.identity.workflow.mgt.AbstractWorkflowTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.WorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowServiceDataHolder {

    private static WorkflowServiceDataHolder instance = new WorkflowServiceDataHolder();
    private static Log log = LogFactory.getLog(WorkflowServiceDataHolder.class);

    private RealmService realmService;
    private ConfigurationContextService configurationContextService;
    private BundleContext bundleContext;

    private Map<String, WorkflowRequestHandler> workflowRequestHandlers;
    private Map<String, AbstractWorkflowTemplate> templates;

    private WorkflowServiceDataHolder() {
        workflowRequestHandlers = new HashMap<>();
        templates = new HashMap<>();
    }

    public ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }

    public void setConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        this.configurationContextService = configurationContextService;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public static WorkflowServiceDataHolder getInstance() {
        return instance;
    }

    public void addTemplate(AbstractWorkflowTemplate template) {
        if (template != null) {
            templates.put(template.getTemplateId(), template);
        }
    }

    public void addTemplateImpl(AbstractWorkflowTemplateImpl impl) {
        if (impl != null) {
            String templateId = impl.getImplementationId();
            if (StringUtils.isNotBlank(templateId)) {
                try {
                    AbstractWorkflowTemplate template = getTemplate(templateId);
                    if (template == null) {
                        log.error("No such template with id:" + templateId);
                        return;
                    }
                    template.addImplementation(impl);
                } catch (WorkflowException e) {
                    log.error("Error when adding implementation '" + impl.getImplementationId() + "' for " +
                            "templateId '" + templateId + "'", e);
                }
            }
        }
    }

    public AbstractWorkflowTemplate getTemplate(String id) {
        return templates.get(id);
    }

    public AbstractWorkflowTemplateImpl getTemplateImplementation(String templateId, String implId) {
        AbstractWorkflowTemplate template = getTemplate(templateId);
        if (template != null) {
            return template.getImplementation(implId);
        }
        return null;
    }

    public void addWorkflowRequestHandler(WorkflowRequestHandler requestHandler) {
        if (requestHandler != null) {
            workflowRequestHandlers.put(requestHandler.getEventId(), requestHandler);
        }
    }

    public void removeWorkflowRequestHandler(WorkflowRequestHandler requestHandler) {
        if (requestHandler != null) {
            workflowRequestHandlers.remove(requestHandler.getEventId());
        }
    }

    public WorkflowRequestHandler getRequestHandler(String handlerId) {
        return workflowRequestHandlers.get(handlerId);
    }

    public List<WorkflowRequestHandler> listRequestHandlers() {
        return new ArrayList<>(workflowRequestHandlers.values());
    }

    public List<AbstractWorkflowTemplate> listTemplates(){
        return new ArrayList<>(templates.values());
    }
}

