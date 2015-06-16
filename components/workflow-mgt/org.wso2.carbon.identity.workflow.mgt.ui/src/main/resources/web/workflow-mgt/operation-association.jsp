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
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.Parameter" %>
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
    String action = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ACTION));
    String workflowName = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_NAME));
    String event =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION));
    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    WorkflowAdminServiceClient client;
    String forwardTo = null;
    WorkflowEventDTO eventDTO = null;
    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
        eventDTO = client.getEvent(event);
    } catch (Exception e) {
        String message = resourceBundle.getString("workflow.error.when.listing.services");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/error.jsp";
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
        var paramDefs = {};
        var operations = {
            "INTEGER": ["equals", "less than", "greater than"],
            "DOUBLE": ["equals", "less than", "greater than"],
            "STRING": ["equals", "contains"],
            "STRING_LIST": ["has value"],
            "STRING_STRING_MAP": ["contains key"]
        };
        <%
        if (eventDTO != null) {
            for (Parameter parameter : eventDTO.getParameters()) {
            %>
        paramDefs["<%=parameter.getParamName()%>"] = "<%=parameter.getParamValue()%>";
        <%
            }
        }
        %>

        function updateParams() {
            var paramDropDown = document.getElementById("paramSelect");
            var headOption = document.createElement("option");
            headOption.text = "--- Select ---";
            headOption.value = "";
            headOption.selected = true;
            headOption.disabled = "disabled";
            paramDropDown.options.add(headOption);
            for (var key in paramDefs) {
                if (paramDefs.hasOwnProperty(key)) {
                    var opt = document.createElement("option");
                    opt.text = key;
                    opt.value = key;
                    paramDropDown.options.add(opt);
                }
            }
        }

        function updateOperator() {
            var paramDropDown = document.getElementById("paramSelect");
            var operationDropdown = document.getElementById("operationSelect");
            $("#operationSelect").empty();
            var selectedParam = paramDropDown.options[paramDropDown.selectedIndex].value;
            var operationsForParam = operations[paramDefs[selectedParam]];
            var headOption = document.createElement("option");
            headOption.text = "--- Select ---";
            headOption.value = "";
            headOption.selected = true;
            headOption.disabled = "disabled";
            operationDropdown.options.add(headOption);
            for (var i = 0; i < operationsForParam.length; i++) {
                var opt = document.createElement("option");
                opt.text = operationsForParam[i];
                opt.value = operationsForParam[i];
                operationDropdown.options.add(opt);
            }
            operationDropdown.disabled = false;
            var val1 = document.getElementById("val1");
            val1.disabled = false;
        }

        function generateXpath() {
            var paramDropDown = document.getElementById("paramSelect");
            var operationDropdown = document.getElementById("operationSelect");
            var selectedParam = paramDropDown.options[paramDropDown.selectedIndex].value;
            var selectedOperation = operationDropdown.options[operationDropdown.selectedIndex].value;
            var condition = "boolean(1)";
            var val1 = document.getElementById("val1").value;
            switch (selectedOperation) {
                case "contains":
                    var template =
                            "boolean(//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value']/*[local-name()='itemValue'][contains(text(),'{{value}}')])";
                    condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                    break;
                case "less than":
                    var template =
                            "boolean(//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value']/*[local-name()='itemValue']/text()<{{value}})";
                    condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                    break;
                case "greater than":
                    var template =
                            "boolean(//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value']/*[local-name()='itemValue']/text()>{{value}})";
                    condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                    break;
                case "equals"://both equals and has value has same condition, but works differently because of string and list
                case "has value":
                    var template =
                            "//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value']/*[local-name()='itemValue']/text()='{{value}}'";
                    condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                    break;
                case "contains key":
                    var template =
                            "boolean(//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value'][@itemName='{{value}}']/*[local-name()='itemValue'])";
                    condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                    break;
                default :
                    CARBON.showErrorDialog('<fmt:message key="condition.error"/>', null, null);
                    return false;
            }
            document.getElementsByName("<%=WorkflowUIConstants.PARAM_ASSOCIATION_CONDITION%>")[0].value = condition;
            return true;
        }

        function doValidation() {

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
            <form method="post" name="serviceAdd" onsubmit="return generateXpath();" action="update-workflow-finish.jsp">
                <%
                    if (WorkflowUIConstants.ACTION_VALUE_ADD.equals(action)) {
                        String template = CharacterEncoder
                                .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE));
                        String templateImpl = CharacterEncoder
                                .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_TEMPLATE_IMPL));
                %>
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_NAME%>" value="<%=workflowName%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION%>" value="<%=event%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ACTION%>" value="<%=action%>">
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
                } else if (WorkflowUIConstants.ACTION_VALUE_ADD_ASSOCIATION.equals(action)) {
                    String workflowId = CharacterEncoder
                            .getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_ID));
                %>
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ASSOCIATED_OPERATION%>" value="<%=event%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_ID%>" value="<%=workflowId%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ACTION%>" value="<%=action%>">
                <%
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
                                    <td>
                                        <select id="paramSelect" onchange="updateOperator()"></select>
                                    </td>
                                    <td>
                                        <select id="operationSelect" disabled="disabled"></select>
                                    </td>
                                    <td>
                                        <input id="val1" type="text" disabled="disabled"/>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr id="conditionXpath" style="display: none">
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
    <script type="text/javascript">
        updateParams();
        updateOperator();
    </script>
</fmt:bundle>
