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
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDeploymentDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDeploymentParameter" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%

    String workflowName = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_NAME));
    String action = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ACTION));
    String operation =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION));
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    WorkflowAdminServiceClient client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
    String forwardTo = "list-services.jsp";


    if (WorkflowUIConstants.ACTION_VALUE_ADD.equals(action)) {
        String templateName =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE));
        String templateImplName =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_TEMPLATE_IMPL));
        String condition =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATION_CONDITION));
        Map<String, String[]> parameterMap = request.getParameterMap();
        List<TemplateDeploymentParameter> templateParams = new ArrayList<TemplateDeploymentParameter>();
        List<TemplateDeploymentParameter> templateImplParams = new ArrayList<TemplateDeploymentParameter>();
        for (Map.Entry<String, String[]> paramEntry : parameterMap.entrySet()) {

            if (paramEntry.getKey() != null && paramEntry.getValue().length > 0) {
                if (paramEntry.getKey().startsWith("p-")) {
                    TemplateDeploymentParameter parameter = new TemplateDeploymentParameter();
                    parameter.setParamName(paramEntry.getKey().substring(2));   //length of "p-"
                    parameter.setParamValue(paramEntry.getValue()[0]);
                    templateParams.add(parameter);
                }
                if (paramEntry.getKey().startsWith("imp-")) {
                    TemplateDeploymentParameter parameter = new TemplateDeploymentParameter();
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
        deploymentDTO.setTemplateParameters(
                templateParams.toArray(new TemplateDeploymentParameter[templateParams.size()]));
        deploymentDTO.setTemplateImplParameters(templateImplParams.toArray(new
                TemplateDeploymentParameter[templateImplParams.size()]));
        deploymentDTO.setCondition(condition);
        deploymentDTO.setAssociatedEvent(operation);
        client.deployTemplate(deploymentDTO);

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
