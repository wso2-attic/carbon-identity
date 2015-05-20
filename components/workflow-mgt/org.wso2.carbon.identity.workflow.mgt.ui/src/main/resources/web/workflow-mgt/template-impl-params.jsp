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

<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateImplDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateParameter" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>

<%
    String workflowName = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_NAME));
    String template = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE));
    String templateImpl = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_TEMPLATE_IMPL));
    String operation =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION));
    WorkflowAdminServiceClient client;
    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    String forwardTo = null;
    TemplateImplDTO templateImplDTO = null;

    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
        templateImplDTO = client.getTemplateImpDTO(template, templateImpl);
    } catch (AxisFault e) {
        String message = resourceBundle.getString("workflow.error.when.initiating.service.client");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/error.jsp";
    }
%>
<%
    if (forwardTo != null) {
%>
<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>
<%
        return;
    }
%>

<fmt:bundle basename="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources">
    <carbon:breadcrumb
            label="workflow.mgt"
            resourceBundle="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">

    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.add'/></h2>

        <div id="workArea">
            <form method="post" name="serviceAdd" action="operation-association.jsp">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ACTION%>"
                       value="<%=WorkflowUIConstants.ACTION_VALUE_ADD%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE%>" value="<%=template%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_TEMPLATE_IMPL%>" value="<%=templateImpl%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_NAME%>" value="<%=workflowName%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION%>" value="<%=operation%>">
                <%
                    Map<String, String[]> parameterMap = request.getParameterMap();
                    for (Map.Entry<String, String[]> paramEntry : parameterMap.entrySet()) {
                        if (paramEntry.getKey() != null && paramEntry.getKey().startsWith("p-") && paramEntry.getValue()
                                != null && paramEntry.getValue().length > 0) {
                            String paramValue = CharacterEncoder.getSafeText(paramEntry.getValue()[0]);
                %>
                    <%--The params will only have one value, hence using 0th element--%>
                <input type="hidden" name="<%=paramEntry.getKey()%>" value="<%=paramValue%>">
                <%
                        }
                    }
                %>
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key="workflow.details"/></th>
                    </tr>
                    </thead>
                    <%
                        for (TemplateParameter parameter : templateImplDTO.getImplementationParams()) {
                            String type = "text";
                            if (WorkflowUIConstants.ParamTypes.BOOLEAN.equals(parameter.getParamType())) {
                                type = "checkbox";
                            } else if (WorkflowUIConstants.ParamTypes.INTEGER.equals(parameter.getParamType())) {
                                type = "number";
                            } else if (WorkflowUIConstants.ParamTypes.PASSWORD.equals(parameter.getParamType())) {
                                type = "password";
                            }
//                            todo:handle 'required' value
                    %>
                    <tr>
                        <td><fmt:message key='<%=parameter.getParamName()%>'/></td>
                            <%--Appending 'imp-' to differentiate dynamic params--%>
                        <td><input name="imp-<%=parameter.getParamName()%>" type="<%=type%>"></td>
                    </tr>
                    <%
                        }

                    %>
                    <tr>
                        <td class="buttonRow">
                            <input class="button" value="<fmt:message key="next"/>" type="submit"/>
                            <input class="button" value="<fmt:message key="cancel"/>" type="button"
                                   onclick="doCancel();"/>
                        </td>
                    </tr>
                </table>
                <br/>
            </form>
        </div>
    </div>
</fmt:bundle>