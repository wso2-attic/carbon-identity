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
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowEventBean" %>
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
    WorkflowEventBean[] workflowEvents = new WorkflowEventBean[0];

    try {
        String template =
                CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_TEMPLATE));
        if (StringUtils.isNotBlank(template)) {
            //coming from a form submission
            String alias = CharacterEncoder.getSafeText(WorkflowUIConstants.PARAM_SERVICE_ALIAS);
            String event = CharacterEncoder.getSafeText(WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT);
            if (StringUtils.isBlank(alias) || StringUtils.isBlank(event)) {
                //check whether the mandatory params are not provided.
                String message = resourceBundle.getString("workflow.error.service.alias.empty");
                CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
                forwardTo = "../admin/error.jsp";
            } else {
                //route to the next page based on template
                if (WorkflowUIConstants.VALUE_EXISTING_SERVICE.equals(template)) {
                    //routing to add existing service page
                    forwardTo = "service-data.jsp?" + WorkflowUIConstants.PARAM_SERVICE_ALIAS + "=" + alias + "&" +
                            WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT + "=" + event + "&" +
                            WorkflowUIConstants.PARAM_ACTION + "=" + WorkflowUIConstants.ACTION_VALUE_ADD;
                } else if (WorkflowUIConstants.TEMPLATE_MAP.containsKey(template)) {
                    //routing to add new service page
                    forwardTo = "template-indep-config.jsp?" + WorkflowUIConstants.PARAM_SERVICE_TEMPLATE + "=" +
                            template + "&" + WorkflowUIConstants.PARAM_SERVICE_ALIAS + "=" + alias + "&" +
                            WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT + "=" + event;
                } else {
                    String message = resourceBundle.getString("workflow.error.non.existing.template");
                    CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
                    forwardTo = "../admin/error.jsp";
                }
            }
        } else {
            //display page, request is not from a form submission
            String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
            String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
            ConfigurationContext configContext =
                    (ConfigurationContext) config.getServletContext()
                            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
            client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
            workflowEvents = client.listWorkflowEvents();
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
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key="workflow.service.details"/></th>
                    </tr>
                    </thead>
                    <tr>
                        <td class="formRow">
                            <table class="normal">
                                <tr>
                                    <td><fmt:message key='workflow.service.alias'/></td>
                                    <td><input type="text" name="<%=WorkflowUIConstants.PARAM_SERVICE_ALIAS%>"/></td>
                                </tr>
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
                                    <td><fmt:message key='workflow.service.template.source'/></td>
                                    <td>
                                        <input type="radio" name="<%=WorkflowUIConstants.PARAM_SERVICE_TEMPLATE%>"
                                               value="<%=WorkflowUIConstants.VALUE_EXISTING_SERVICE%>"/>
                                        Existing Service <br/>
                                        <input id="newBpelRadio" type="radio"
                                               name="<%=WorkflowUIConstants.PARAM_SERVICE_TEMPLATE%>" value=""/>
                                        Deploy new BPEL from template<br/>
                                        <select onchange="updateTemplate(this)">
                                            <%
                                                for (Map.Entry<String, String> eventEntry :
                                                        WorkflowUIConstants.TEMPLATE_MAP.entrySet()) {
                                            %>
                                            <option value="<%=eventEntry.getKey()%>"><%=eventEntry.getValue()%>
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
