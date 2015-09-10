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
import org.wso2.carbon.identity.workflow.mgt.WorkflowService;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplate;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.extension.WorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.exception.RuntimeWorkflowException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkflowServiceDataHolder {

    private static WorkflowServiceDataHolder instance = new WorkflowServiceDataHolder();
    private static Log log = LogFactory.getLog(WorkflowServiceDataHolder.class);

    private RealmService realmService;
    private ConfigurationContextService configurationContextService;
    private BundleContext bundleContext;

    private Map<String, WorkflowRequestHandler> workflowRequestHandlers;
    private Map<String, AbstractWorkflowTemplate> templates;
    private Set<AbstractWorkflowTemplateImpl> unresolvedImpls;

    private WorkflowService workflowService = null ;

    private WorkflowServiceDataHolder() {

        workflowRequestHandlers = new HashMap<>();
        templates = new HashMap<>();
        unresolvedImpls = new HashSet<>();
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
            for (Iterator<AbstractWorkflowTemplateImpl> iterator = unresolvedImpls.iterator(); iterator.hasNext(); ) {
                AbstractWorkflowTemplateImpl unresolvedImpl = iterator.next();
                if (StringUtils.equals(unresolvedImpl.getTemplateId(), template.getTemplateId())) {
                    try {
                        template.addImplementation(unresolvedImpl);
                    } catch (RuntimeWorkflowException e) {
                        log.error("The workflow template implementation with id:" +
                                unresolvedImpl.getImplementationId() + " is already registered.");
                    }
                    iterator.remove();
                }
            }
            templates.put(template.getTemplateId(), template);
        }
    }

    public void removeTemplate(AbstractWorkflowTemplate template) {

        if (template != null && template.getTemplateId() != null && templates.containsKey(template.getTemplateId())) {
            templates.remove(template.getTemplateId());
        }
    }

    public void addTemplateImpl(AbstractWorkflowTemplateImpl impl) {

        if (impl != null) {
            String templateId = impl.getTemplateId();
            if (StringUtils.isNotBlank(templateId)) {
                try {
                    AbstractWorkflowTemplate template = getTemplate(templateId);
                    if (template == null) {
                        unresolvedImpls.add(impl);
                        log.warn("No such workflow template with id:" + templateId + ". The implementation " + impl
                                .getImplementationId() + " might not be usable.");
                        return;
                    }
                    template.addImplementation(impl);
                } catch (RuntimeWorkflowException e) {
                    log.error("The workflow template implementation with id:" + impl.getImplementationId() +
                            " is already" +
                            " registered.");
                }
            }
        }
    }

    public void removeTemplateImpl(AbstractWorkflowTemplateImpl impl) {

        if (impl != null && impl.getTemplateId() != null) {
            if (templates.containsKey(impl.getTemplateId())) {
                AbstractWorkflowTemplate workflowTemplate = templates.get(impl.getTemplateId());
                if (workflowTemplate != null) {
                    workflowTemplate.removeImpl(impl.getImplementationId());
                }
            } else {
                for (Iterator<AbstractWorkflowTemplateImpl> iterator = unresolvedImpls.iterator();
                     iterator.hasNext(); ) {
                    AbstractWorkflowTemplateImpl unresolvedImpl = iterator.next();
                    if (impl.equals(unresolvedImpl)) {
                        iterator.remove();
                    }
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

    public List<AbstractWorkflowTemplate> listTemplates() {

        return new ArrayList<>(templates.values());
    }

    public WorkflowService getWorkflowService() {
        return workflowService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
}

