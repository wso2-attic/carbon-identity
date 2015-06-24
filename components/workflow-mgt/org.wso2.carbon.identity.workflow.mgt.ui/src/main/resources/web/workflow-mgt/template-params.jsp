<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.BPSProfileBean" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateImplDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateParameterDef" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>
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

<%
    String workflowName = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_NAME));
    String template = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE));
    String description =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION));
    WorkflowAdminServiceClient client;
    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    String forwardTo = null;
    TemplateDTO templateDTO = null;
    BPSProfileBean[] bpsProfiles = new BPSProfileBean[0];

    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
        templateDTO = client.getTemplate(template);
        bpsProfiles = client.listBPSProfiles();
    } catch (Exception e) {
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
            <form method="post" name="serviceAdd" action="template-impl-params.jsp">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE%>" value="<%=template%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_NAME%>" value="<%=workflowName%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION%>" value="<%=description%>">
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key="workflow.details"/></th>
                    </tr>
                    </thead>
                    <tr>
                        <td class="formRow">
                            <table class="normal">
                                <%
                                    for (TemplateParameterDef parameter : templateDTO.getParameters()) {
                                        if (parameter != null) {
                                %>
                                <tr>
                                    <td><%=parameter.getDisplayName()%>
                                    </td>
                                    <%
                                        //Text areas
                                        if (WorkflowUIConstants.ParamTypes.LONG_STRING
                                                .equals(parameter.getParamType())) {
                                    %>
                                    <td><textarea name="p-<%=parameter.getParamName()%>"
                                                  title="<%=parameter.getDisplayName()%>"></textarea>
                                    </td>
                                    <%
                                    } else if (WorkflowUIConstants.ParamTypes.BPS_PROFILE
                                            .equals(parameter.getParamType())) {
                                        //bps profiles
                                    %>
                                    <td><select name="p-<%=parameter.getParamName()%>">
                                        <%
                                            for (BPSProfileBean bpsProfile : bpsProfiles) {
                                                if (bpsProfile != null) {
                                        %>
                                        <option value="<%=bpsProfile.getProfileName()%>"><%=bpsProfile
                                                .getProfileName()%>
                                        </option>
                                        <%
                                                }
                                            }
                                        %>
                                    </select>
                                    </td>
                                    <%
                                    } else {
                                        //other types
                                        String type = "text";
                                        if (WorkflowUIConstants.ParamTypes.BOOLEAN
                                                .equals(parameter.getParamType())) {
                                            type = "checkbox";
                                        } else if (WorkflowUIConstants.ParamTypes.INTEGER
                                                .equals(parameter.getParamType())) {
                                            type = "number";
                                        } else if (WorkflowUIConstants.ParamTypes.PASSWORD
                                                .equals(parameter.getParamType())) {
                                            type = "password";
                                        }
                                    %>
                                        <%--Appending 'p-' to differentiate dynamic params--%>
                                    <td><input name="p-<%=parameter.getParamName()%>" type="<%=type%>"></td>
                                    <%

                                            }
//                            todo:handle 'required' value

                                        }
                                    %>
                                </tr>
                                <%
                                    }

                                %>
                            </table>
                        </td>
                    </tr>
                </table>
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key="workflow.deployment"/></th>
                    </tr>
                    </thead>

                    <tr>
                        <td class="formRow">
                            <table class="normal">
                                <tr>
                                    <td><fmt:message key='workflow.deployment.type'/></td>
                                    <td>
                                        <select name="<%=WorkflowUIConstants.PARAM_TEMPLATE_IMPL%>">
                                            <%
                                                for (TemplateImplDTO impl : templateDTO.getImplementations()) {
                                            %>
                                            <option value="<%=impl.getImplementationId()%>"><%=impl
                                                    .getImplementationName()%>
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
                                <%--todo : implement back--%>
                            <input class="button" value="<fmt:message key="back"/>" type="button">
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