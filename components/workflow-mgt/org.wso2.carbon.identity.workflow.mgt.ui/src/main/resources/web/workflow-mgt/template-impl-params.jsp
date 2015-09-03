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
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.BPSProfileDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateImplDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateParameterDef" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>

<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDTO" %>
<%@ page import="java.util.HashMap" %>


<%
    String requestPath = "list-workflows";
    if(request.getParameter("path") != null && !request.getParameter("path").isEmpty()){
        requestPath = request.getParameter("path");
    }
    boolean isSelf = false;
    if(StringUtils.isNotBlank(request.getParameter("self")) && request.getParameter("self").equals("true")){
        isSelf =  true ;
    }

    String template = null ;
    String templateImpl = request.getParameter(WorkflowUIConstants.PARAM_TEMPLATE_IMPL);


    WorkflowAdminServiceClient client;
    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());

    String forwardTo = null;
    TemplateImplDTO templateImplDTO = null;
    TemplateDTO templateDTO = null;

    Map<String, String> attribMap = new HashMap<String,String>();


    if (session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD) != null &&
            session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD) instanceof Map) {
        attribMap =
                (Map<String, String>) session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD);

        template = attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE);

        if(!isSelf){

            //removing existing session params
            for (Iterator<Map.Entry<String, String>> iterator = attribMap.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, String> entry = iterator.next();
                if (entry.getKey().startsWith("p-")) {
                    iterator.remove();
                }
            }
            //setting params from previous page
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (Map.Entry<String, String[]> paramEntry : parameterMap.entrySet()) {
                if (StringUtils.startsWith(paramEntry.getKey(), "p-") &&
                    paramEntry.getValue() != null && paramEntry.getValue().length > 0) {
                    String paramValue = paramEntry.getValue()[0];
                    attribMap.put(paramEntry.getKey(), paramValue);
                }
            }
        }

    }

    BPSProfileDTO[] bpsProfiles = new BPSProfileDTO[0];
    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);

        templateDTO = client.getTemplate(template);
        if(templateDTO != null && templateDTO.getImplementations() != null  && templateDTO.getImplementations().length == 1 ){
            templateImpl = templateDTO.getImplementations()[0].getImplementationId() ;
        }

        if(templateImpl != null) {

            attribMap.put(WorkflowUIConstants.PARAM_TEMPLATE_IMPL,templateImpl);

            templateImplDTO = client.getTemplateImpDTO(template, templateImpl);
            bpsProfiles = client.listBPSProfiles();
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
            label="workflow.template.impl"
            resourceBundle="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">
        function goBack() {
            location.href = "template-params.jsp";
        }

        function doCancel() {
            function cancel() {
                location.href = '<%=requestPath%>.jsp?wizard=finish';
            }

            CARBON.showConfirmationDialog('<fmt:message key="confirmation.workflow.add.abort"/> ' + name + '?',
                    cancel, null);
        }

        function selectTemplate(){
            var workflowForm = document.getElementById("id_param_template_impl");
            workflowForm.submit();
        }
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.add'/></h2>

        <div id="workArea">

            <%
                if( isSelf  || templateImpl == null){
            %>

            <form id="id_param_template_impl" method="post" name="serviceAdd" action="template-impl-params.jsp">
                <input type="hidden" name="path" value="<%=requestPath%>"/>
                <input type="hidden" name="self" value="true"/>
                <table class="normal">
                    <tr>
                        <td><fmt:message key='workflow.deployment.type'/></td>
                        <td>
                            <select onchange="selectTemplate();" name="<%=WorkflowUIConstants.PARAM_TEMPLATE_IMPL%>">
                                <option value="" disabled selected><fmt:message key="select"/></option>
                                <%
                                    for (TemplateImplDTO impl : templateDTO.getImplementations()) {
                                %>
                                <option value="<%=impl.getImplementationId()%>"
                                        <%=impl.getImplementationId().equals(templateImpl) ? "selected" :""%>>
                                    <%=impl.getImplementationName()%>
                                </option>
                                <%
                                    }
                                %>
                            </select>
                        </td>
                    </tr>
                </table>


            </form>

            <%
            }
            %>

            </br>


            <form id="param-form" method="post" name="serviceAdd" action="update-workflow-finish.jsp">
                <input type="hidden" name="path" value="<%=requestPath%>"/>
                <input type="hidden" name="<%=WorkflowUIConstants.PARAM_ACTION%>" value="<%=WorkflowUIConstants.ACTION_VALUE_ADD%>">
                <!--
                <%
                    Map<String, String[]> parameterMap = request.getParameterMap();
                    for (Map.Entry<String, String[]> paramEntry : parameterMap.entrySet()) {
                        if (StringUtils.startsWith(paramEntry.getKey(), "p-") &&
                                paramEntry.getValue() != null && paramEntry.getValue().length > 0) {
                            String paramValue = paramEntry.getValue()[0];
                %>
                    <input type="hidden" name="<%=paramEntry.getKey()%>" value="<%=paramValue%>">
                <%
                        }
                    }
                %>

                -->

                <%
                    if(templateImpl != null){
                %>
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key='workflow.deployment.type'/> : <%= templateImpl %></th>
                    </tr>
                    </thead>
                    <tr>
                        <td class="formRow">
                            <table class="normal">
                                <%
                                    for (TemplateParameterDef parameter : templateImplDTO.getImplementationParams()) {
                                        if (parameter != null) {
                                %>
                                <tr>
                                    <td width="30%"><%=parameter.getDisplayName()%>
                                    </td>
                                    <%
                                        //Text areas
                                        if (WorkflowUIConstants.ParamTypes.LONG_STRING
                                                .equals(parameter.getParamType())) {
                                    %>
                                    <td><textarea name="imp-<%=parameter.getParamName()%>" style="min-width: 30%"
                                                  title="<%=parameter.getDisplayName()%>"></textarea>
                                    </td>
                                    <%
                                    } else if (WorkflowUIConstants.ParamTypes.BPS_PROFILE
                                            .equals(parameter.getParamType())) {
                                        //bps profiles
                                    %>
                                    <td><select name="imp-<%=parameter.getParamName()%>" style="min-width: 30%">
                                        <%
                                            for (BPSProfileDTO bpsProfile : bpsProfiles) {
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
                                    <td><input name="imp-<%=parameter.getParamName()%>" type="<%=type%>"
                                               style="min-width: 30%"></td>
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
                    <tr>
                        <td class="buttonRow">
                            <input class="button" value="<fmt:message key="back"/>" type="button" onclick="goBack();"/>
                            <input class="button" value="<fmt:message key="finish"/>" type="submit"/>
                            <input class="button" value="<fmt:message key="cancel"/>" type="button"
                                   onclick="doCancel();"/>
                        </td>
                    </tr>
                </table>
                <%
                    }
                %>

                <br/>
            </form>

        </div>

    </div>
</fmt:bundle>