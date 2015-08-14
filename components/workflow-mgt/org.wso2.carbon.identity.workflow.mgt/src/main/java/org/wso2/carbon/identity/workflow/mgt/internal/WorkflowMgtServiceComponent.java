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

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplate;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.template.impl.AlwaysDenyTemplate;
import org.wso2.carbon.identity.workflow.mgt.template.impl.BPELApprovalTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.template.impl.DefaultImmediateDenyImpl;
import org.wso2.carbon.identity.workflow.mgt.template.impl.SimpleApprovalTemplate;
import org.wso2.carbon.identity.workflow.mgt.extension.WorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.WorkflowService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="identity.workflow" immediate="true"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="workflow.request.handler.service"
 * interface="org.wso2.carbon.identity.workflow.mgt.extension.WorkflowRequestHandler"
 * cardinality="0..n" policy="dynamic"
 * bind="setWorkflowRequestHandler"
 * unbind="unsetWorkflowRequestHandler"
 * @scr.reference name="workflow.template.service"
 * interface="org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplate"
 * cardinality="0..n" policy="dynamic"
 * bind="setWorkflowTemplate"
 * unbind="unsetWorkflowTemplate"
 * @scr.reference name="workflow.template.impl.service"
 * interface="org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplateImpl"
 * cardinality="0..n" policy="dynamic"
 * bind="setTemplateImplementation"
 * unbind="unsetTemplateImplementation"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"  bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 */
public class WorkflowMgtServiceComponent {

    protected void setRealmService(RealmService realmService) {

        WorkflowServiceDataHolder.getInstance().setRealmService(realmService);
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {

        WorkflowServiceDataHolder.getInstance().setConfigurationContextService(contextService);
    }

    protected void activate(ComponentContext context) {

        BundleContext bundleContext = context.getBundleContext();
        WorkflowService workflowService = new WorkflowService();
        bundleContext.registerService(WorkflowService.class, workflowService, null);
        WorkflowServiceDataHolder.getInstance().setBundleContext(bundleContext);
        setWorkflowTemplate(new SimpleApprovalTemplate());
        setTemplateImplementation(new BPELApprovalTemplateImpl());
        setWorkflowTemplate(new AlwaysDenyTemplate());
        setTemplateImplementation(new DefaultImmediateDenyImpl());
    }

    protected void unsetRealmService(RealmService realmService) {

        WorkflowServiceDataHolder.getInstance().setRealmService(null);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {

        WorkflowServiceDataHolder.getInstance().setConfigurationContextService(null);
    }

    protected void setWorkflowRequestHandler(WorkflowRequestHandler workflowRequestHandler) {

        WorkflowServiceDataHolder.getInstance().addWorkflowRequestHandler(workflowRequestHandler);
    }

    protected void unsetWorkflowRequestHandler(WorkflowRequestHandler workflowRequestHandler) {

        WorkflowServiceDataHolder.getInstance().removeWorkflowRequestHandler(workflowRequestHandler);
    }

    protected void setWorkflowTemplate(AbstractWorkflowTemplate template) {

        WorkflowServiceDataHolder.getInstance().addTemplate(template);
    }

    protected void unsetWorkflowTemplate(AbstractWorkflowTemplate template) {

        WorkflowServiceDataHolder.getInstance().removeTemplate(template);
    }

    protected void setTemplateImplementation(AbstractWorkflowTemplateImpl templateImpl) {

        WorkflowServiceDataHolder.getInstance().addTemplateImpl(templateImpl);
    }

    protected void unsetTemplateImplementation(AbstractWorkflowTemplateImpl templateImpl) {

        WorkflowServiceDataHolder.getInstance().removeTemplateImpl(templateImpl);
    }
}
