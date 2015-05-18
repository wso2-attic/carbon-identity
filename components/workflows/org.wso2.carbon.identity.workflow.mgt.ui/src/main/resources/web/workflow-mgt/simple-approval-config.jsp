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
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.bpel.ApprovalServiceConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.bpel.ApprovalServiceGenerator" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.bpel.bean.ApprovalServiceParams" %>
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
    String processName = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_PROCESS_NAME));
    String bpsHost = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_BPS_HOST));
    String carbonHost = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_CARBON_HOST));
    String carbonUser = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_CARBON_AUTH_USER));
    String carbonUserPassword =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_CARBON_AUTH_PASSWORD));
    String bpsUser =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_AUTH_USERNAME));
    String bpsUserPassword =
            CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_SERVICE_AUTH_PASSWORD));
    String action = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_ACTION));
    if (WorkflowUIConstants.ACTION_VALUE_DEPLOY.equals(action)) {
        String htName = CharacterEncoder.getSafeText(request.getParameter(ApprovalServiceConstants.PARAM_HT_NAME));
        String htOwner =
                CharacterEncoder.getSafeText(request.getParameter(ApprovalServiceConstants.PARAM_HT_OWNER_ROLE));
        String htAdmin =
                CharacterEncoder.getSafeText(request.getParameter(ApprovalServiceConstants.PARAM_HT_ADMIN_ROLE));
        String htSubject =
                CharacterEncoder.getSafeText(request.getParameter(ApprovalServiceConstants.PARAM_HT_SUBJECT));
        String htDescription =
                CharacterEncoder.getSafeText(request.getParameter(ApprovalServiceConstants.PARAM_HT_DESCRIPTION));
        ApprovalServiceParams serviceParams = new ApprovalServiceParams();
        serviceParams.setBpelProcessName(processName);
        serviceParams.setBpsHostName(bpsHost);
        serviceParams.setCarbonAuthUser(carbonUser);
        serviceParams.setCarbonHostName(carbonHost);
        serviceParams.setCarbonUserPassword(carbonUserPassword);
        serviceParams.setHtAdminRole(htAdmin);
        serviceParams.setHtPotentialOwnerRole(htOwner);
        serviceParams.setHtServiceName(htName);
        serviceParams.setHumanTaskDescription(htDescription);
        serviceParams.setHumanTaskSubject(htSubject);
        serviceParams.setBpsUsername(bpsUser);
        serviceParams.setBpsUserPassword(bpsUserPassword);
        ApprovalServiceGenerator serviceGenerator = new ApprovalServiceGenerator(serviceParams);
        serviceGenerator.generateAndDeployArtifacts();
        String serviceEP =
                serviceParams.getBpsHostName() + "/services/" + serviceParams.getBpelProcessName() + "Service";
%>
<form name="associate_deployed" action="service-condition.jsp" method="POST">
    <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ALIAS%>" value="<%=alias%>">
    <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT%>" value="<%=event%>">
    <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_EPR%>" value="<%=serviceEP%>">
    <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ACTION%>"
           value="<%=ApprovalServiceConstants.SERVICE_ACTION%>">
    <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_AUTH_USERNAME%>" value="<%=bpsUser%>">
    <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_AUTH_PASSWORD%>" value="<%=bpsUserPassword%>">
    <input type="submit" value="Continue to event association">
</form>
<script type="text/javascript">
    document.forms["associate_deployed"].submit();
</script>
<%
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
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.service.add'/></h2>

        <div id="workArea">
            <form method="post">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ACTION%>"
                       value="<%=WorkflowUIConstants.ACTION_VALUE_DEPLOY%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ALIAS%>" value="<%=alias%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT%>" value="<%=event%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_PROCESS_NAME%>" value="<%=processName%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_BPS_HOST%>" value="<%=bpsHost%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_CARBON_HOST%>" value="<%=carbonHost%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_CARBON_AUTH_USER%>" value="<%=carbonUser%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_CARBON_AUTH_PASSWORD%>"
                       value="<%=carbonUserPassword%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_AUTH_USERNAME%>" value="<%=bpsUser%>">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_SERVICE_AUTH_PASSWORD%>"
                       value="<%=bpsUserPassword%>">
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
                                    <td><fmt:message key='workflow.template.simple.approval.ht.name'/></td>
                                    <td>
                                        <input type="text" name="<%=ApprovalServiceConstants.PARAM_HT_NAME%>"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.template.simple.approval.ht.subject'/></td>
                                    <td>
                                        <input type="text" name="<%=ApprovalServiceConstants.PARAM_HT_SUBJECT%>"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.template.simple.approval.ht.description'/></td>
                                    <td>
                                        <textarea name="<%=ApprovalServiceConstants.PARAM_HT_DESCRIPTION%>" />
                                        <%--<input type="text" name="<%=ApprovalServiceConstants.PARAM_HT_DESCRIPTION%>"/>--%>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.template.simple.approval.ht.owner'/></td>
                                    <td>
                                        <input type="text" name="<%=ApprovalServiceConstants.PARAM_HT_OWNER_ROLE%>"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.template.simple.approval.ht.admin'/></td>
                                    <td>
                                        <input type="text" name="<%=ApprovalServiceConstants.PARAM_HT_ADMIN_ROLE%>"/>
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
