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
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="java.util.ResourceBundle" %>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    //    String username = CharacterEncoder.getSafeText(request.getParameter("username"));

    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    String forwardTo = null;

    String alias = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ALIAS));
    String event =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT));
    String template = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_TEMPLATE));
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);


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
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.service.add'/></h2>

        <div id="workArea">
            <form method="post" name="serviceAdd" onsubmit="return doValidation();" action="<%=template%>-config.jsp">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ALIAS%>" value="<%=alias%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT%>" value="<%=event%>">
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key="workflow.bpel.deployment.config"/></th>
                    </tr>
                    </thead>
                    <tr>
                        <td class="formRow">
                            <table class="normal">
                                <tr>
                                    <td><fmt:message key='workflow.bpel.process.name'/></td>
                                    <td>
                                        <input type="text" name="<%=WorkflowUIConstants.PARAM_PROCESS_NAME%>"/>
                                    </td>
                                </tr>
                                <tr>
                                    <th><fmt:message key="workflow.bps.config"/></th>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.config.host'/></td>
                                    <td>
                                        <input type="text" name="<%=WorkflowUIConstants.PARAM_BPS_HOST%>"
                                               value="https://localhost:9444/"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.service.auth.user'/></td>
                                    <td>
                                        <input type="text" name="<%=WorkflowUIConstants.PARAM_SERVICE_AUTH_USERNAME%>"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.service.auth.password'/></td>
                                    <td>
                                        <input type="password"
                                               name="<%=WorkflowUIConstants.PARAM_SERVICE_AUTH_PASSWORD%>"/>
                                    </td>
                                </tr>
                                <tr>
                                    <th><fmt:message key="workflow.carbon.config"/></th>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.config.host'/></td>
                                    <td>
                                        <input type="text" name="<%=WorkflowUIConstants.PARAM_CARBON_HOST%>"
                                               value="<%=backendServerURL%>"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.service.auth.user'/></td>
                                    <td>
                                        <input type="text" name="<%=WorkflowUIConstants.PARAM_CARBON_AUTH_USER%>"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.service.auth.password'/></td>
                                    <td>
                                        <input type="password"
                                               name="<%=WorkflowUIConstants.PARAM_CARBON_AUTH_PASSWORD%>"/>
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
