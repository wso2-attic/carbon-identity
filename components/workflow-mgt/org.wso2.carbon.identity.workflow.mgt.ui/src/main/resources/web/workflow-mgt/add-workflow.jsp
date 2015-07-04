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

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateBean" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    //    String username = CharacterEncoder.getSafeText(request.getParameter("username"));

    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    String forwardTo = null;
    WorkflowAdminServiceClient client;
    TemplateBean[] templateList = null;
    String workflowName = null;
    String workflowDescription = null;
    String workflowTemplate = null;
    String action = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ACTION));
    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
        templateList = client.listTemplates();
        if (templateList == null) {
            templateList = new TemplateBean[0];
        }

        if (session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD) != null &&
                session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD) instanceof Map &&
                WorkflowUIConstants.ACTION_VALUE_BACK.equals(action)) {
            Map<String, String> attribMap =
                    (Map<String, String>) session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD);
            workflowName = attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_NAME);
            workflowDescription = attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION);
            workflowTemplate = attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE);
        } else {
            session.setAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD, new HashMap<String, String>());
        }
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
            label="workflow.add"
            resourceBundle="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">

        function doCancel() {
            function cancel() {
                location.href = "list-workflows.jsp";
            }

            CARBON.showConfirmationDialog('<fmt:message key="confirmation.workflow.add.abort"/> ' + name + '?',
                    cancel, null);
        }
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.add'/></h2>

        <div id="workArea">
            <form method="post" name="serviceAdd" action="template-params.jsp">
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key="workflow.details"/></th>
                    </tr>
                    </thead>
                    <tr>
                        <td class="formRow">
                            <table class="normal">
                                <tr>
                                    <td width="30%"><fmt:message key='workflow.name'/></td>
                                    <td><input type="text" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_NAME%>"
                                               value="<%=workflowName != null ? workflowName : ""%>"
                                               style="min-width: 30%"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td width="30%"><fmt:message key='workflow.description'/></td>
                                    <td><textarea name="<%=WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION%>"
                                                  style="min-width: 30%"
                                            ><%=workflowDescription != null ? workflowDescription : ""%></textarea>
                                            <%--do not leave any space/newlines between textarea start and close tags,
                                            it will display those also if any--%>
                                    </td>
                                </tr>
                                <tr>
                                    <td width="30%"><fmt:message key='workflow.template'/></td>
                                    <td>
                                        <select name="<%=WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE%>"
                                                style="min-width: 30%">
                                            <option value="" disabled selected><fmt:message key="select"/></option>
                                            <%
                                                for (TemplateBean template : templateList) {
                                            %>
                                            <option value="<%=template.getId()%>"
                                                    <%=template.getId().equals(workflowTemplate) ? "selected" : ""%>>
                                                <%=template.getName()%>
                                            </option>
                                            <%
                                                }
                                            %>
                                        </select>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
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
