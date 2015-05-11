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
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowEventBean" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
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
    WorkflowEventBean[] workflowEvents = new WorkflowEventBean[0];
    String action = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ACTION));

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);

    String alias = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ALIAS));
    String event =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT));
    String serviceEP = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_EPR));
    String authUser =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_AUTH_USERNAME));
    String authPassword =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_AUTH_PASSWORD));
    String serviceAction = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ACTION));
    String priority = null;
    String condition = null;

    if (!WorkflowUIConstants.ACTION_VALUE_ADD.equals(action)) {
        //should be edit or finish, and following should be available for both
        priority = CharacterEncoder
                .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_PRIORITY));
        condition = CharacterEncoder.getSafeText(
                request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_CONDITION));
    }
    if (WorkflowUIConstants.ACTION_VALUE_FINISH.equals(action)) {
        //todo : validate?
        try {
            client.addExistingService(alias, serviceEP, serviceAction, authUser, authPassword);
            client.associateServiceToEvent(alias, event, condition, Integer.parseInt(priority));
            forwardTo = "list-services.jsp";
        } catch (WorkflowAdminServiceWorkflowException e) {
            String message = resourceBundle.getString("workflow.error.when.adding.service");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "../admin/error.jsp";
        }
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


        function doValidation() {
            if (isEmpty("<%=WorkflowUIConstants.PARAM_SERVICE_ALIAS%>")) {
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
        <h2><fmt:message key='workflow.service.add'/></h2>

        <div id="workArea">
            <form method="post" name="serviceAdd" onsubmit="return doValidation();">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ALIAS%>" value="<%=alias%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT%>" value="<%=event%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_EPR%>" value="<%=serviceEP%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ACTION%>" value="<%=serviceAction%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_AUTH_USERNAME%>" value="<%=authUser%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_AUTH_PASSWORD%>"
                       value="<%=authPassword%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ACTION%>"
                       value="<%=WorkflowUIConstants.ACTION_VALUE_FINISH%>">
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th>
                            <fmt:message key="workflow.service.association.details">
                                <fmt:param value="<%=alias%>"/>
                                <fmt:param value="<%=event%>"/>
                            </fmt:message>
                        </th>
                    </tr>
                    </thead>
                    <tr>
                        <td class="formRow">
                            <table class="normal">
                                <tr>
                                    <td><fmt:message key='workflow.service.associate.priority'/></td>
                                    <td>
                                        <input type="number"
                                               name="<%=WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_PRIORITY%>"
                                               value="10"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.service.associate.condition'/></td>
                                    <td>
                                        <input type="text"
                                               name="<%=WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_CONDITION%>"/>
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
