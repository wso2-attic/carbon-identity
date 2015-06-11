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
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.EventBean" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateBean" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    //    String username = CharacterEncoder.getSafeText(request.getParameter("username"));

    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    String forwardTo = null;
    WorkflowAdminServiceClient client;
    EventBean[] workflowEvents;
    Map<String, List<EventBean>> events = new HashMap<String, List<EventBean>>();
    TemplateBean[] templateList = null;
    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
        workflowEvents = client.listWorkflowEvents();
        for (EventBean event : workflowEvents) {
            String category = event.getCategory();
            if (events.containsKey(category)) {
                events.get(category).add(event);
            } else {
                events.put(category, new ArrayList<EventBean>());
            }
        }
        templateList = client.listTemplates();
        if (templateList == null) {
            templateList = new TemplateBean[0];
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
        eventsObj = {};
        <%
            for (Map.Entry<String,List<EventBean>> eventCategory : events.entrySet()) {
            %>
        eventsObj["<%=eventCategory.getKey()%>"] = [];
        <%
            for (EventBean event : eventCategory.getValue()) {
                %>
        var eventObj = {};
        eventObj.displayName = "<%=event.getFriendlyName()%>";
        eventObj.value = "<%=event.getId()%>";
        eventObj.title = "<%=event.getDescription()!=null?event.getDescription():""%>";
        eventsObj["<%=eventCategory.getKey()%>"].push(eventObj);
        <%
                    }
            }
        %>

        function updateActions() {
            var categoryDropdown = document.getElementById("categoryDropdown");
            var actionDropdown = document.getElementById("actionDropdown");
            var selectedCategory = categoryDropdown.options[categoryDropdown.selectedIndex].value;
            var eventsOfCategory = eventsObj[selectedCategory];
            for (var i = 0; i < eventsOfCategory.length; i++) {
                var opt = document.createElement("option");
                opt.text = eventsOfCategory[i].displayName;
                opt.value = eventsOfCategory[i].value;
                opt.title = eventsOfCategory[i].title;
                actionDropdown.options.add(opt);
            }
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
                                    <td><fmt:message key='workflow.name'/></td>
                                    <td><input type="text" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_NAME%>"/></td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.description'/></td>
                                    <td><textarea name="<%=WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION%>"></textarea>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.template'/></td>
                                    <td>
                                        <select name="<%=WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE%>">
                                            <%
                                                for (TemplateBean template : templateList) {
                                            %>
                                            <option value="<%=template.getId()%>"><%=template.getName()%>
                                            </option>
                                            <%
                                                }

                                            %>
                                        </select>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.operation.category'/></td>
                                    <td>
                                        <select id="categoryDropdown" onchange="updateActions();">
                                            <%
                                                for (String key : events.keySet()) {
                                            %>
                                            <option value="<%=key%>"><%=key%>
                                            </option>
                                            <%
                                                }
                                            %>
                                        </select>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.operation.name'/></td>
                                    <td><select id="actionDropdown"
                                                name="<%=WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION%>"></select>
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
    <script type="text/javascript">
        updateActions();
    </script>
</fmt:bundle>
