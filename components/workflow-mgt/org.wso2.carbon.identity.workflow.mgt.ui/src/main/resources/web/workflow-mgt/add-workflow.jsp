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
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>


<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    String requestPath = "list-workflows";
    //'path' parameter to use to track parent wizard path if this wizard trigger by another wizard
    if(request.getParameter("path") != null && !request.getParameter("path").isEmpty()){
        requestPath = request.getParameter("path");
    }

    String workflowName = null;
    String workflowDescription = null;

    String action =  request.getParameter(WorkflowUIConstants.PARAM_ACTION);

    if (session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD) != null &&
        session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD) instanceof Map &&
        WorkflowUIConstants.ACTION_VALUE_BACK.equals(action)) {
        Map<String, String> attribMap =
                (Map<String, String>) session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD);
        workflowName = attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_NAME);
        workflowDescription = attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION);
    } else {
        session.setAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD, new HashMap<String, String>());
    }
%>



<fmt:bundle basename="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources">
    <carbon:breadcrumb
            label="workflow.add"
            resourceBundle="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">

        function doCancel() {
            function cancel() {
                location.href = '<%=requestPath%>.jsp?wizard=finish';
            }
            CARBON.showConfirmationDialog('<fmt:message key="confirmation.workflow.add.abort"/> ' + name + '?', cancel, null);
        }

        function submitPage(){
            var workflowForm = document.getElementById("id_workflow");
            if($('#id_workflow_name').val().length > 0){
                workflowForm.submit();
            }else{
                CARBON.showWarningDialog("<fmt:message key="workflow.error.empty.workflow.name"/>" , null ,null) ;
            }
        }

    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.add'/></h2>

        <div id="workArea">
            <form id="id_workflow" method="post" name="serviceAdd" action="template-params.jsp">
                <input type="hidden" name="path" value="<%=requestPath%>"/>
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
                                    <td width="130px"><fmt:message key='workflow.name'/></td>
                                    <td>
                                        <input size="30" id="id_workflow_name" type="text" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_NAME%>" value="<%=workflowName != null ? workflowName : ""%>" style="min-width: 30%"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='workflow.description'/></td>
                                    <td>
                                        <textarea name="<%=WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION%>" cols="60" rows="4"><%=workflowDescription != null ? workflowDescription : ""%></textarea>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td class="buttonRow">
                            <input onclick="submitPage();" class="button" value="<fmt:message key="next"/>" type="button"/>
                            <input class="button" value="<fmt:message key="cancel"/>" type="button" onclick="doCancel();"/>
                        </td>
                    </tr>
                </table>
                <br/>
            </form>
        </div>
    </div>
</fmt:bundle>
