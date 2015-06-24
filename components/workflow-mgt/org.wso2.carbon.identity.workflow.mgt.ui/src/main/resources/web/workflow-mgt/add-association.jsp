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
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.Parameter" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowBean" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowEventDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
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
    WorkflowAdminServiceClient client = null;
    String forwardTo = null;

    String workflowId = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_ID));
    WorkflowEventDTO[] workflowEvents = null;
    Map<String, List<WorkflowEventDTO>> events = new HashMap<String, List<WorkflowEventDTO>>();

    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);

        workflowEvents = client.listWorkflowEvents();
        for (WorkflowEventDTO event : workflowEvents) {
            String category = event.getEventCategory();
            if (!events.containsKey(category)) {
                events.put(category, new ArrayList<WorkflowEventDTO>());
            }
            events.get(category).add(event);
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
    <carbon:breadcrumb label="workflow.mgt"
                       resourceBundle="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources"
                       topPage="true" request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">
        var eventsObj = {};
        var lastSelectedCategory = '';
        <%
            for (Map.Entry<String,List<WorkflowEventDTO>> eventCategory : events.entrySet()) {
            %>
        eventsObj["<%=eventCategory.getKey()%>"] = [];
        <%
            for (WorkflowEventDTO event : eventCategory.getValue()) {
                %>
        var eventObj = {};
        eventObj.displayName = "<%=event.getEventFriendlyName()%>";
        eventObj.value = "<%=event.getEventId()%>";
        eventObj.title = "<%=event.getEventDescription()!=null?event.getEventDescription():""%>";
        eventsObj["<%=eventCategory.getKey()%>"].push(eventObj);
        <%
                    }
            }
        %>

        function updateActions() {
            var categoryDropdown = document.getElementById("categoryDropdown");
            var actionDropdown = document.getElementById("actionDropdown");
            var selectedCategory = categoryDropdown.options[categoryDropdown.selectedIndex].value;
            $("#actionDropdown").empty();
            var headOption = document.createElement("option");
            headOption.text = "--- Select ---";
            headOption.value = "";
            headOption.selected = true;
            headOption.disabled = "disabled";
            actionDropdown.options.add(headOption);
            if (selectedCategory != lastSelectedCategory) {
                var eventsOfCategory = eventsObj[selectedCategory];
                for (var i = 0; i < eventsOfCategory.length; i++) {
                    var opt = document.createElement("option");
                    opt.text = eventsOfCategory[i].displayName;
                    opt.value = eventsOfCategory[i].value;
                    opt.title = eventsOfCategory[i].title;
                    actionDropdown.options.add(opt);
                }
                lastSelectedCategory = selectedCategory;
            }
        }

        var paramDefs = {};
        var operations = {
            "INTEGER": ["equals", "less than", "greater than"],
            "DOUBLE": ["equals", "less than", "greater than"],
            "STRING": ["equals", "contains"],
            "STRING_LIST": ["has value"],
            "STRING_STRING_MAP": ["contains key"]
        };

        var selectionType = "applyToAll";
        <%
        if (workflowEvents != null) {
        for (WorkflowEventDTO event : workflowEvents) {
        %>
        paramDefs["<%=event.getEventId()%>"] = {};
        <%
                for (Parameter parameter : event.getParameters()) {
                    if(parameter!=null){

            %>
        paramDefs["<%=event.getEventId()%>"]["<%=parameter.getParamName()%>"] = "<%=parameter.getParamValue()%>";
        <%
                    }else {
                    System.out.println(event.getEventId()+" "+event.getParameters().length);
                    }
                }
            }
        }
        %>

        function updateParams() {
            var actionDropdown = document.getElementById("actionDropdown");
            var selectedCategory = actionDropdown.options[actionDropdown.selectedIndex].value;
            var paramDropDown = document.getElementById("paramSelect");
            $("#paramSelect").empty();
            $("#operationSelect").empty();
            var headOption = document.createElement("option");
            headOption.text = "--- Select ---";
            headOption.value = "";
            headOption.selected = true;
            headOption.disabled = "disabled";
            paramDropDown.options.add(headOption);
            for (var key in paramDefs[selectedCategory]) {
                if (paramDefs[selectedCategory].hasOwnProperty(key)) {
                    var opt = document.createElement("option");
                    opt.text = key;
                    opt.value = key;
                    paramDropDown.options.add(opt);
                }
            }
        }

        function updateOperator() {
            var actionDropdown = document.getElementById("actionDropdown");
            var selectedCategory = actionDropdown.options[actionDropdown.selectedIndex].value;
            var paramDropDown = document.getElementById("paramSelect");
            var operationDropdown = document.getElementById("operationSelect");
            $("#operationSelect").empty();
            var selectedParam = paramDropDown.options[paramDropDown.selectedIndex].value;
            var operationsForParam = operations[paramDefs[selectedCategory][selectedParam]];
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
            var condition = "boolean(1)";

            if (selectionType == "applyIf" && selectedOperation != null) {
                var selectedParam = paramDropDown.options[paramDropDown.selectedIndex].value;
                var selectedOperation = operationDropdown.options[operationDropdown.selectedIndex].value;
                var val1 = document.getElementById("val1").value;

                switch (selectedOperation) {
                    case "contains":
                        var template =
                                "boolean(//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value']/*[local-name()='itemValue'][contains(text(),'{{value}}')])";
                        condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                        break;
                    case "less than":
                        template =
                                "boolean(//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value']/*[local-name()='itemValue']/text()<{{value}})";
                        condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                        break;
                    case "greater than":
                        template =
                                "boolean(//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value']/*[local-name()='itemValue']/text()>{{value}})";
                        condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                        break;
                    case "equals"://both equals and has value has same condition, but works differently because of string and list
                    case "has value":
                        template =
                                "//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value']/*[local-name()='itemValue']/text()='{{value}}'";
                        condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                        break;
                    case "contains key":
                        template =
                                "boolean(//*[local-name()='parameter'][@name='{{paramName}}']/*[local-name()='value'][@itemName='{{value}}']/*[local-name()='itemValue'])";
                        condition = template.replace("{{paramName}}", selectedParam).replace("{{value}}", val1);
                        break;
                    default :
                        CARBON.showErrorDialog('<fmt:message key="condition.error"/>', null, null);
                        return false;
                }
            }

            if (selectionType != "advanced") {
                document.getElementsByName("<%=WorkflowUIConstants.PARAM_ASSOCIATION_CONDITION%>")[0].value =
                        condition;
            }
            return true;
        }

        function doValidation() {

//            todo:validate other params
        }

        function doCancel() {
            location.href = 'list-associations.jsp';
        }

        function handleRadioInput(radio) {
            if (radio.value != selectionType) {
                switch (radio.value) {
                    case "applyToAll":
                        document.getElementById("conditionSelectRow").style.display = 'none';
                        document.getElementById("conditionXpath").style.display = 'none';
                        break;
                    case "applyIf":
                        document.getElementById("conditionSelectRow").style.display = 'block';
                        document.getElementById("conditionXpath").style.display = 'none';
                        break;
                    case "advanced":
                        document.getElementById("conditionSelectRow").style.display = 'none';
                        document.getElementById("conditionXpath").style.display = 'block';
                        break;
                }
                selectionType = radio.value;
            }
        }

    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.list'/></h2>

        <div id="workArea">
            <div id="addNew">
                <form action="update-association-finish.jsp" method="post" onsubmit="return generateXpath();">
                    <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ACTION%>"
                           value="<%=WorkflowUIConstants.ACTION_VALUE_ADD%>">

                    <table class="styledLeft noBorders" style="margin-top: 10px">
                        <thead>
                        <tr>
                            <th colspan="2">
                                <fmt:message key="workflow.details">
                                </fmt:message>
                            </th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr>
                            <td><fmt:message key="workflow.service.association.name"/></td>
                            <td><input type="text" name="<%=WorkflowUIConstants.PARAM_ASSOCIATION_NAME%>">
                            </td>
                        </tr>

                        <tr>
                            <td><fmt:message key='workflow.operation.category'/></td>
                            <td>
                                <select id="categoryDropdown" onchange="updateActions();">
                                    <option disabled selected value="">--- Select ---</option>
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
                            <td><select id="actionDropdown" onchange="updateParams();"
                                        name="<%=WorkflowUIConstants.PARAM_OPERATION%>"></select>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                    <table class="styledLeft noBorders" style="margin-top: 10px">
                        <thead>
                        <tr>
                            <th colspan="2">
                                <fmt:message key="workflow.service.association.details">
                                </fmt:message>
                            </th>
                        </tr>
                        </thead>
                        <tr>
                            <td>
                                <fmt:message key="workflow.select">
                                </fmt:message>
                            </td>
                            <td>
                                <select name="<%=WorkflowUIConstants.PARAM_WORKFLOW_ID%>">
                                    <option disabled value="">--- Select ---</option>

                                    <%
                                        for (WorkflowBean workflowBean : client.listWorkflows()) {
                                            if (workflowBean != null) {
                                                boolean select = false;
                                                if (StringUtils.equals(workflowId, workflowBean.getWorkflowId())) {
                                                    select = true;
                                                }
                                    %>
                                    <option value="<%=workflowBean.getWorkflowId()%>" <%=select ? "selected" : ""%>
                                            title="<%=workflowBean.getWorkflowDescription()%>">
                                        <%=workflowBean.getWorkflowName()%>
                                    </option>
                                    <%
                                            }
                                        }

                                    %>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2">
                                <input type="radio" name="conditionType" value="applyToAll"
                                       onclick="handleRadioInput(this);" checked="checked">Apply to all
                                Requests
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2">
                                <input type="radio" name="conditionType" value="applyIf"
                                       onclick="handleRadioInput(this);">
                                Apply if,
                            </td>
                        </tr>
                        <tr id="conditionSelectRow" style="display: none">
                            <td class="formRow" colspan="2">
                                <table class="normal noBorders">
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
                        <tr>
                            <td colspan="2"><input type="radio" name="conditionType" value="advanced"
                                                   onclick="handleRadioInput(this);">
                                Advanced
                            </td>
                        </tr>
                        <tr id="conditionXpath" style="display: none">
                            <td class="formRow" colspan="2">
                                <table class="normal noBorders">
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
                    </table>
                    <table class="styledLeft noBorders" style="margin-top: 10px">
                        <tr>
                            <td class="buttonRow">
                                <input class="button" value="<fmt:message key="add"/>" type="submit"/>
                                <input class="button" value="<fmt:message key="cancel"/>" type="button"
                                       onclick="doCancel();"/>
                            </td>
                        </tr>
                    </table>
                </form>
            </div>
            <br/>
        </div>
    </div>
</fmt:bundle>