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
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.ParameterDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>

<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowDTO" %>


<%
    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());

    String action = request.getParameter(WorkflowUIConstants.PARAM_ACTION);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    WorkflowAdminServiceClient client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
    String workflowName = request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_NAME);

    String forwardTo = "list-workflows.jsp";

    if(request.getParameter("path") != null && !request.getParameter("path").isEmpty()){
        forwardTo = request.getParameter("path") + ".jsp?wizard=finish&" + WorkflowUIConstants.PARAM_WORKFLOW_NAME + "=" + workflowName;
    }

    if (WorkflowUIConstants.ACTION_VALUE_ADD.equals(action)) {

        Map<String, String> attribMap =
                (Map<String, String>) session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD);

        workflowName =
                attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_NAME);
        String description = attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION);
        String templateName = attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE);
        String templateImplName = attribMap.get(WorkflowUIConstants.PARAM_TEMPLATE_IMPL);

        Map<String, String[]> parameterMap = request.getParameterMap();
        List<ParameterDTO> templateParams = new ArrayList<ParameterDTO>();
        List<ParameterDTO> templateImplParams = new ArrayList<ParameterDTO>();
        for (Map.Entry<String, String[]> paramEntry : parameterMap.entrySet()) {

            if (paramEntry.getKey() != null && paramEntry.getValue().length > 0) {
                if (paramEntry.getKey().startsWith("imp-")) {
                    ParameterDTO parameter = new ParameterDTO();
                    parameter.setParamName(paramEntry.getKey().substring(4));   //length of "imp-"
                    parameter.setParamValue(paramEntry.getValue()[0]);
                    templateImplParams.add(parameter);
                }
            }
        }
        for (Map.Entry<String, String> paramEntry : attribMap.entrySet()) {

            if (paramEntry.getKey() != null && paramEntry.getValue()!=null) {
                if (paramEntry.getKey().startsWith("p-")) {
                    ParameterDTO parameter = new ParameterDTO();
                    parameter.setParamName(paramEntry.getKey().substring(2));   //length of "p-"
                    parameter.setParamValue(paramEntry.getValue());
                    templateParams.add(parameter);
                }
            }
        }

        try {
            WorkflowDTO workflowDTO = new WorkflowDTO();
            workflowDTO.setWorkflowName(workflowName);
            workflowDTO.setWorkflowDescription(description);
            workflowDTO.setTemplateName(templateName);
            workflowDTO.setImplementationName(templateImplName);
            client.addWorkflow(workflowDTO, templateParams,templateImplParams);

        } catch (WorkflowAdminServiceWorkflowException e) {
            String message = resourceBundle.getString("workflow.error.when.adding.workflow");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "../admin/error.jsp";
        }

    } else if (WorkflowUIConstants.ACTION_VALUE_DELETE.equals(action)) {
        String workflowId = request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_ID);
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
        String associationId = request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATION_ID);
        String workflowId = request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_ID);
        try {
            client.deleteAssociation(associationId);
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
