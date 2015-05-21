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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowEventDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    //    String username = CharacterEncoder.getSafeText(request.getParameter("username"));

    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    WorkflowAdminServiceClient client;
    String forwardTo = null;
    WorkflowEventDTO[] workflowEvents = new WorkflowEventDTO[0];
    String action = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ACTION));

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);


    String workflowName = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_NAME));
    String event =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION));
    String condition = null;
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


        function doValidation() {
            if (isEmpty("<%=WorkflowUIConstants.PARAM_WORKFLOW_NAME%>")) {
                CARBON.showWarningDialog("<fmt:message key="workflow.error.service.alias.empty"/>");
                return false;
            }
//            todo:validate other params
        }

        function doCancel() {
            location.href = 'list-services.jsp';
        }

        function updateTemplate(sel) {
            if (sel.options[sel.selectedIndex].value != null) {
                $("#newBpelRadio").value = sel.options[sel.selectedIndex].value;
            }
        }
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.add'/></h2>

        <div id="workArea">
            <form method="post" name="serviceAdd" onsubmit="return doValidation();" action="add-workflow-finish.jsp">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_NAME%>" value="<%=workflowName%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION%>" value="<%=event%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ACTION%>" value="<%=action%>">
                <%
                    if (WorkflowUIConstants.ACTION_VALUE_ADD.equals(action)) {
                        String template = CharacterEncoder
                                        .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE));
                        String templateImpl = CharacterEncoder
                                        .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_TEMPLATE_IMPL));
                %>
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE%>" value="<%=template%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_TEMPLATE_IMPL%>" value="<%=templateImpl%>">
                <%
                    Map<String, String[]> parameterMap = request.getParameterMap();
                    for (Map.Entry<String, String[]> paramEntry : parameterMap.entrySet()) {
                        if (paramEntry.getKey() != null && (paramEntry.getKey().startsWith("p-") ||
                                paramEntry.getKey().startsWith("imp-")) && paramEntry.getValue().length > 0) {
                            String paramValue = CharacterEncoder.getSafeText(paramEntry.getValue()[0]);
                %>
                <input type="hidden" name="<%=paramEntry.getKey()%>" value="<%=paramValue%>">
                <%
                            }
                        }
                    }
                %>
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th>
                            <fmt:message key="workflow.service.association.details">
                                <fmt:param value="<%=workflowName%>"/>
                                <fmt:param value="<%=event%>"/>
                            </fmt:message>
                        </th>
                    </tr>
                    </thead>
                    <tr>
                        <td class="formRow">
                            <table class="normal">
                                <tr>
                                    <td><fmt:message key='workflow.service.associate.condition'/></td>
                                    <td>
                                        <input type="text"
                                               name="<%=WorkflowUIConstants.PARAM_ASSOCIATION_CONDITION%>"/>
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
