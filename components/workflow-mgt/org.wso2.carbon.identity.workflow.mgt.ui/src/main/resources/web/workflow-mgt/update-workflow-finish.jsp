<%--
  ~ Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 Inc. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.AddAssociationDO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.Parameter" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDeploymentDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>

<%
    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());

    String action = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ACTION));
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    WorkflowAdminServiceClient client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
    String forwardTo = "list-workflows.jsp";


    if (WorkflowUIConstants.ACTION_VALUE_ADD.equals(action)) {
        String workflowName =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_NAME));
        String operation =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION));
        String templateName =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE));
        String templateImplName =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_TEMPLATE_IMPL));
        String condition =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATION_CONDITION));
        Map<String, String[]> parameterMap = request.getParameterMap();
        List<Parameter> templateParams = new ArrayList<Parameter>();
        List<Parameter> templateImplParams = new ArrayList<Parameter>();
        for (Map.Entry<String, String[]> paramEntry : parameterMap.entrySet()) {

            if (paramEntry.getKey() != null && paramEntry.getValue().length > 0) {
                if (paramEntry.getKey().startsWith("p-")) {
                    Parameter parameter = new Parameter();
                    parameter.setParamName(paramEntry.getKey().substring(2));   //length of "p-"
                    parameter.setParamValue(paramEntry.getValue()[0]);
                    templateParams.add(parameter);
                }
                if (paramEntry.getKey().startsWith("imp-")) {
                    Parameter parameter = new Parameter();
                    parameter.setParamName(paramEntry.getKey().substring(4));   //length of "imp-"
                    parameter.setParamValue(paramEntry.getValue()[0]);
                    templateImplParams.add(parameter);
                }
            }
        }
        TemplateDeploymentDTO deploymentDTO = new TemplateDeploymentDTO();
        deploymentDTO.setWorkflowName(workflowName);
        deploymentDTO.setTemplateName(templateName);
        deploymentDTO.setTemplateImplName(templateImplName);
        deploymentDTO.setParameters(
                templateParams.toArray(new Parameter[templateParams.size()]));
        deploymentDTO.setTemplateImplParameters(templateImplParams.toArray(new
                Parameter[templateImplParams.size()]));
        AddAssociationDO association = new AddAssociationDO();
        association.setCondition(condition);
        association.setEventId(operation);
        try {
            client.deployTemplate(deploymentDTO);
        } catch (WorkflowAdminServiceWorkflowException e) {
            String message = resourceBundle.getString("workflow.error.when.adding.workflow");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "../admin/error.jsp";
        }

    } else if (WorkflowUIConstants.ACTION_VALUE_DELETE.equals(action)) {
        String workflowId = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_ID));
        if (StringUtils.isNotBlank(workflowId)) {
            try {
                client.deleteWorkflow(workflowId);
            } catch (WorkflowAdminServiceWorkflowException e) {
                String message = resourceBundle.getString("workflow.error.when.deleting.workflow");
                CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
                forwardTo = "../admin/error.jsp";
            }
        }
    } else if (WorkflowUIConstants.ACTION_VALUE_DELETE_ASSOCIATION.equals(action)) {
        String associationId =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATION_ID));
        String workflowId = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_ID));
        try {
            client.deleteAssociation(associationId);
            forwardTo = "view-workflow.jsp?" + WorkflowUIConstants.PARAM_WORKFLOW_ID + "=" + workflowId;
        } catch (WorkflowAdminServiceWorkflowException e) {
            String message = resourceBundle.getString("workflow.error.when.deleting.association");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "../admin/error.jsp";
        }
    } else if (WorkflowUIConstants.ACTION_VALUE_ADD_ASSOCIATION.equals(action)) {
        String workflowId =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_ID));
        String operation =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION));
        String condition =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATION_CONDITION));
        try {
            client.addAssociation(workflowId, operation, condition);
            forwardTo = "view-workflow.jsp?" + WorkflowUIConstants.PARAM_WORKFLOW_ID + "=" + workflowId;
        } catch (WorkflowAdminServiceWorkflowException e) {
            String message = resourceBundle.getString("workflow.error.when.deleting.association");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "../admin/error.jsp";
        }
    }
%>
<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>
