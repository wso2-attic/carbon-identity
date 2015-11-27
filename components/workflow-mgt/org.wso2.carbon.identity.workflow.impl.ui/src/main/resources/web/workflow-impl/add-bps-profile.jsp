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
<%@ page import="org.wso2.carbon.identity.workflow.impl.ui.WorkflowUIConstants" %>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>
<script type="text/javascript" src="../identity/validation/js/identity-validate.js"></script>

<fmt:bundle basename="org.wso2.carbon.identity.workflow.impl.ui.i18n.Resources">
    <carbon:breadcrumb
            label="workflow.mgt"
            resourceBundle="org.wso2.carbon.identity.workflow.impl.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">
        function doCancel(){
            window.location = "list-bps-profiles.jsp";
        }
        function doValidation(){
            return doValidateInput(document.getElementById('profile-name'), "<fmt:message key="error.input.validation.msg"/>");
        }
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.bps.profile.add'/></h2>

        <div id="workArea">
            <form method="post" name="serviceAdd" action="update-bps-profile-finish.jsp" onsubmit="return doValidation();">
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ACTION%>"
                       value="<%=WorkflowUIConstants.ACTION_VALUE_ADD%>">
                <table class="styledLeft noBorders">
                    <thead>
                    <tr>
                        <th colspan="2"><fmt:message key="workflow.bps.profile"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td width="30%"><fmt:message key='workflow.bps.profile.name'/></td>
                        <td><input id="profile-name" type="text"
                                   name="<%=WorkflowUIConstants.PARAM_BPS_PROFILE_NAME%>"
                                   white-list-patterns="^[a-zA-Z0-9]+$"/></td>
                    </tr>
                    </tbody>
                </table>
                <table class="styledLeft noBorders" style="margin-top: 10px">
                    <thead>
                    <tr>
                        <th colspan="2"><fmt:message key="workflow.bps.profile.connection.details"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td width="30%"><fmt:message key='workflow.bps.profile.manager.host'/></td>
                        <td><input type="text" name="<%=WorkflowUIConstants.PARAM_BPS_MANAGER_HOST%>"/></td>
                    </tr>
                    <tr>
                        <td width="30%"><fmt:message key='workflow.bps.profile.worker.host'/></td>
                        <td><input type="text" name="<%=WorkflowUIConstants.PARAM_BPS_WORKER_HOST%>"/></td>
                    </tr>
                    <tr>
                        <td width="30%"><fmt:message key='workflow.bps.profile.auth.user'/></td>
                        <td><input type="text" name="<%=WorkflowUIConstants.PARAM_BPS_AUTH_USER%>"/></td>
                    </tr>
                    <tr>
                        <td width="30%"><fmt:message key='workflow.bps.profile.auth.password'/></td>
                        <td><input type="password" name="<%=WorkflowUIConstants.PARAM_BPS_AUTH_PASSWORD%>"/>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <table style="margin-top: 10px">
                    <tr>
                        <td class="buttonRow">
                            <input class="button" value="<fmt:message key="add"/>" type="submit"/>
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
