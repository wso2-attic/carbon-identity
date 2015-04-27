<%--
  ~ Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@page import="org.apache.axis2.AxisFault" %>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException" %>
<%@page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowEventBean" %>
<%@page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
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
    String serviceAlias = null;
    WorkflowEventBean[] workflowEvents = new WorkflowEventBean[0];

    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);

        if (CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.ASSOCIATE_SERVICE)) != null) {
            //This block handles self submission of the form
            String event = CharacterEncoder
                    .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT));
            String priority = CharacterEncoder
                    .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_PRIORITY));
            String condition = CharacterEncoder
                    .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_CONDITION));
            serviceAlias = CharacterEncoder
                    .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ALIAS));
            try {
                client.associateServiceToEvent(serviceAlias, event, condition, Integer.parseInt(priority));
                forwardTo = "list-services.jsp?from=associateService";
            } catch (WorkflowAdminServiceWorkflowException e) {
                String message = resourceBundle.getString("workflow.error.when.associating.service");
                CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
                forwardTo = "../admin/error.jsp";
            }

        } else {
            serviceAlias = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ALIAS));
            workflowEvents = client.listWorkflowEvents();
//        todo:logic here

            if (serviceAlias == null) {
                String message = resourceBundle.getString("workflow.error.with.service.null.alias");
                CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
                forwardTo = "../admin/error.jsp";
            }
            if (workflowEvents == null || workflowEvents.length == 0) {
                String message = resourceBundle.getString("workflow.error.empty.event.list");
                CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
                forwardTo = "../admin/error.jsp";
            }
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
            label="workflow.mgt"
            resourceBundle="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">
        function doCancel() {
            location.href = 'list-services.jsp';
        }
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.service.associate'/></h2>

        <div id="workArea">
            <form method="post" name="serviceAssociate">
                <input type="hidden" name="<%=WorkflowUIConstants.ASSOCIATE_SERVICE %>"
                       value="true">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ALIAS %>"
                       value="<%=serviceAlias %>">

                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th>
                            <fmt:message key="workflow.service.association.details">
                                <fmt:param value="<%=serviceAlias%>"/>
                            </fmt:message>
                        </th>
                    </tr>
                    </thead>
                    <tr>
                        <td class="formRow">
                            <table class="normal">
                                <tr>
                                    <td><fmt:message key='workflow.service.associate.event'/></td>
                                    <td>
                                        <select name="<%=WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT%>">
                                            <%
                                                for (WorkflowEventBean event : workflowEvents) {
                                            %>
                                            <option value="<%=event.getEventName()%>"><%=event.getEventName()%>
                                            </option>
                                            <%
                                                }

                                            %>
                                        </select>
                                    </td>
                                </tr>
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