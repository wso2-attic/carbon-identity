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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.BPSProfileDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateParameterDef" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>

<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateBean" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<%
    String requestPath = "list-workflows";
    //'path' parameter to use to track parent wizard path if this wizard trigger by another wizard
    if(request.getParameter("path") != null && !request.getParameter("path").isEmpty()){
        requestPath = request.getParameter("path")  ;
    }
    boolean isSelf = false;
    if(StringUtils.isNotBlank(request.getParameter("self")) && request.getParameter("self").equals("true")){
        isSelf =  true ;
    }



    String template = request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE);
    Map<String, String> templateParams = new HashMap<String, String>();

    Map<String, String> attribMap = new HashMap<String, String>() ;

    if (session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD) != null &&
            session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD) instanceof Map) {
        attribMap = (Map<String, String>) session.getAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD);
        //setting params from previous page
        if (template == null) {
            template = attribMap.get(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE);
        }
        for (Map.Entry<String, String> entry : attribMap.entrySet()) {
            if (entry.getKey().startsWith("p-")) {
                templateParams.put(entry.getKey(), entry.getValue());
            }
        }
    }else{
        session.setAttribute(WorkflowUIConstants.ATTRIB_WORKFLOW_WIZARD, attribMap);
    }

    if (!isSelf) {
        String workflowName = request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_NAME);
        String description = request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION);
        if (workflowName != null) {
            attribMap.put(WorkflowUIConstants.PARAM_WORKFLOW_NAME, workflowName);
        }
        if (description != null) {
            attribMap.put(WorkflowUIConstants.PARAM_WORKFLOW_DESCRIPTION, description);
        }
    }

    WorkflowAdminServiceClient client;
    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    String forwardTo = null;

    TemplateBean[] templateList = null;
    TemplateDTO templateDTO = null;
    BPSProfileDTO[] bpsProfiles = new BPSProfileDTO[0];

    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);

        templateList = client.listTemplates();
        if (templateList == null) {
            templateList = new TemplateBean[0];
        }else if(templateList.length == 1 && template == null){
            template = templateList[0].getId();
        }

        if(template != null) {
            attribMap.put(WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE, template);
            templateDTO = client.getTemplate(template);
            bpsProfiles = client.listBPSProfiles();
        }

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
            label="workflow.template"
            resourceBundle="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <!-- Override carbon jquery from latest release of it, because this tokenizer support for latest one -->
    <script type="text/javascript" src="js/jquery-1.11.3.js"></script>
    <script type="text/javascript" src="js/tokenizer.js"></script>
    <link rel="stylesheet" type="text/css" href="css/input_style.css">

    <style>

        .tknz-wrapper {
            width: 96%;
            height: 54px;
            margin: 5px;
            padding: 5px;
            overflow: auto;
            color: #fefefe;
            background: #fefefe;
            font-family: "Courier", Times, sans-serif;
            border: solid 1px #DFDFDF;
        }

    </style>

    <script type="text/javascript">

        function goBack() {
            location.href = "add-workflow.jsp?<%=WorkflowUIConstants.PARAM_ACTION%>=<%=WorkflowUIConstants.ACTION_VALUE_BACK%>";
        }

        function doCancel() {
            function cancel() {
                location.href = '<%=requestPath%>.jsp?wizard=finish';
            }
            CARBON.showConfirmationDialog('<fmt:message key="confirmation.workflow.add.abort"/> ' + name + '?', cancel, null);
        }


        var stepOrder = 0;
        jQuery(document).ready(function(){

            jQuery('h2.trigger').click(function(){
                if (jQuery(this).next().is(":visible")) {
                    this.className = "active trigger step_heads";
                } else {
                    this.className = "trigger step_heads";
                }
                jQuery(this).next().slideToggle("fast");
                return false; //Prevent the browser jump to the link anchor
            });

            jQuery('#stepsAddLink').click(function(){
                stepOrder++;
                jQuery('#stepsConfRow').append(jQuery('<div class="toggle_container sectionSub" id="div_step_head_'+stepOrder+'" style="border:solid 1px #ccc;padding: 10px;margin-bottom:10px;" >' +
                                                        '<h2 id="step_head_'+stepOrder+'" class="trigger active step_heads" style="background-color: beige; clear: both;">' +
                                                            '<input type="hidden" value="'+stepOrder+'" name="approve_step" id="approve_step">' +
                                                            '<a class="step_order_header" href="#">Step '+stepOrder+'</a>' +
                                                            '<a onclick="deleteStep(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif);float:right;width: 9px;"></a>' +
                                                        '</h2>' +
                                                            '<table style="width:100%;">' +
                                                                '<tr><td id="search_step_head_'+stepOrder+'"></td></tr>' +
                                                                '<tr id="id_step_roles_'+stepOrder+'" style="display:none;">' +
                                                                    '<td style="width:100%;">' +
                                                                        '<table  style="width:100%;">' +
                                                                            '<tr><td width="40px">Roles</td><td onclick="moveSearchController(\''+stepOrder+'\',\'roles\', false);"><input readonly  name="p-step-'+stepOrder+'-roles" id="p-step-'+stepOrder+'-roles"  type="text" class="tokenizer_'+stepOrder+'"/></td></tr>' +
                                                                        '</table>' +
                                                                    '</td>' +
                                                                '</tr>' +
                                                              '<tr id="id_step_users_'+stepOrder+'" style="width:100%;display:none;">' +
                                                                  '<td style="width:100%;">' +
                                                                      '<table style="width:100%;">' +
                                                                            '<tr><td width="40px">Users</td><td onclick="moveSearchController(\''+stepOrder+'\',\'users\', false);"><input readonly  name="p-step-'+stepOrder+'-users" id="p-step-'+stepOrder+'-users" type="text" class="tokenizer_'+stepOrder+'"/></td></tr>' +
                                                                      '</table>' +
                                                                  '</td>' +
                                                              '</tr>' +
                                                            '</table>' +
                                                      '</div>'));

                //Move search component to selected step
                moveSearchController(stepOrder, "roles", true)

                //Init tokanizer for users and roles inputs in given step.
                initInputs("p-step-"+stepOrder+"-roles");
                initInputs("p-step-"+stepOrder+"-users");



            });


        });

        function initInputs(id){
            $("#" + id).tokenizer({
                label: ''
            });
        }


        function moveSearchController(step, category, init){

            $("#id_search_controller").detach().appendTo("#search_step_head_"+step);
            $("#id_search_controller").show();
            $("#currentstep").val(step);

            loadCategory(category, init);
        }




        function deleteStep(obj){

            $("#id_search_controller").hide();
            $("#id_search_controller").detach().appendTo("#id_search_controller_base");

            stepOrder--;
            jQuery(obj).parent().next().remove();
            jQuery(obj).parent().parent().remove();
            if($('.step_heads').length > 0){
                var newStepOrderVal = 1;
                $.each($('.step_heads'), function(){
                    var oldApproveStepVal = parseInt($(this).find('input[name="approve_step"]').val());

                    //Changes in header
                    $(this).attr('id','step_head_'+newStepOrderVal);
                    $(this).find('input[name="approve_step"]').val(newStepOrderVal);
                    $(this).find('.step_order_header').text('Step '+newStepOrderVal);

                    var textArea_Users = $('#p-step-'+oldApproveStepVal+'-users');
                    textArea_Users.attr('id','#p-step-'+newStepOrderVal+'-users');
                    textArea_Users.attr('name','#p-step-'+newStepOrderVal+'-users');

                    var textArea_Roles = $('#p-step-'+oldApproveStepVal+'_roles');
                    textArea_Roles.attr('id','#p-step-'+newStepOrderVal+'_roles');
                    textArea_Roles.attr('name','#p-step-'+newStepOrderVal+'_roles');

                    newStepOrderVal++;
                });
            }
        }


        function getSelectedItems(allList, category){

            if(allList!=null && allList.length!=0) {
                var currentStep = $("#currentstep").val();

                $("#id_step_"+category+"_" + currentStep).show();
                var currentValues = $("#p-step-" + currentStep + "-" + category).val();
                for(var i=0;i<allList.length;i++) {
                    var newItem = allList[i];
                    $("#p-step-" + currentStep + "-" + category).tokenizer('push',newItem);
                }
                var newValues = $("#p-step-" + currentStep + "-" + category).tokenizer('get');

            }


        }


        function selectTemplate(){
            var workflowForm = document.getElementById("id_workflow_template");
            workflowForm.submit();
        }

        function nextWizard(){

            for(var currentStep=1;currentStep<=stepOrder ; currentStep++){
                var newValues = $("#p-step-" + currentStep + "-users" ).tokenizer('get');
                $("#p-step-" + currentStep + "-users").val(newValues);
                newValues = $("#p-step-" + currentStep + "-roles" ).tokenizer('get');
                $("#p-step-" + currentStep + "-roles").val(newValues);
            }

            var nextWizardForm = document.getElementById("id_nextwizard");
            nextWizardForm.submit();
        }

    </script>

    <div id="middle">

        <h2><fmt:message key='workflow.add'/></h2>

        <div id="workArea">

            <%
                if( isSelf  || template == null){
            %>

            <form id="id_workflow_template" method="post" name="serviceAdd" action="template-params.jsp">
                <input type="hidden" name="path" value="<%=requestPath%>"/>
                <input type="hidden" name="self" value="true"/>
                <table border="1">
                    <tr>
                        <td width="60px"><fmt:message key='workflow.template'/></td>
                        <td>
                            <select onchange="selectTemplate();" id="id_template" name="<%=WorkflowUIConstants.PARAM_WORKFLOW_TEMPLATE%>"
                                    style="min-width: 30%">
                                    <option value="" selected><fmt:message key="select"/></option>
                                <%
                                    for (TemplateBean templateTmp : templateList) {
                                %>
                                    <option value="<%=templateTmp.getId()%>"
                                            <%=templateTmp.getId().equals(template) ? "selected" : ""%>>
                                        <%=templateTmp.getName()%>
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

            <%
                if(template != null ){
            %>
            <form method="post" name="serviceAdd" id="id_nextwizard" action="template-impl-params.jsp">
                <input type="hidden" name="path" value="<%=requestPath%>"/>
                <table class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key='workflow.template'/> : <%= template %></th>
                    </tr>
                    </thead>
                    <tr>
                        <td class="formRow">
                            <table class="normal" style="width: 100%;">
                                <%
                                    boolean emptyList = true;
                                    for (TemplateParameterDef parameter : templateDTO.getParameters()) {
                                        if (parameter != null) {
                                            emptyList = false;
                                            break;
                                        }
                                    }

                                    if (emptyList) {
                                %>
                                <tr>
                                    <td colspan="2"><fmt:message key="workflow.template.has.no.params"/></td>
                                </tr>
                                <%
                                } else {
                                    for (TemplateParameterDef parameter : templateDTO.getParameters()) {
                                        if (parameter != null) {
                                %>
                                <tr>
                                    <td width="200px" style="vertical-align: top !important;"><%=parameter.getDisplayName()%></td>
                                </tr>
                                <tr>
                                    <%
                                        //Text areas
                                        if (WorkflowUIConstants.ParamTypes.LONG_STRING
                                                .equals(parameter.getParamType())) {
                                    %>
                                    <td><textarea name="p-<%=parameter.getParamName()%>"
                                                  title="<%=parameter.getDisplayName()%>" style="min-width: 30%"
                                            ><%=templateParams.get("p-" + parameter.getParamName()) != null ?
                                            templateParams.get("p-" + parameter.getParamName()) : ""%></textarea>
                                    </td>
                                    <%
                                    } else if (WorkflowUIConstants.ParamTypes.BPS_PROFILE
                                            .equals(parameter.getParamType())) {
                                        //bps profiles
                                    %>
                                    <td><select name="p-<%=parameter.getParamName()%>" style="min-width: 30%">
                                        <%
                                            for (BPSProfileDTO bpsProfile : bpsProfiles) {
                                                if (bpsProfile != null) {
                                                    boolean select = bpsProfile.getProfileName().equals(
                                                            templateParams.get("p-" +
                                                                    parameter.getParamName()));
                                        %>
                                        <option value="<%=bpsProfile.getProfileName()%>" <%=select ? "selected" :
                                                ""%>><%=bpsProfile.getProfileName()%>
                                        </option>
                                        <%
                                                }
                                            }
                                        %>
                                    </select>
                                    </td>
                                    <%
                                    } else if (WorkflowUIConstants.ParamTypes.USER_NAME_OR_USER_ROLE.equals(parameter.getParamType())) {
                                    %>
                                    <td>
                                        <a id="stepsAddLink" class="icon-link" style="background-image:url(images/add.png);margin-left:0"><fmt:message key='workflow.template.button.add.step'/></a>
                                       <div style="margin-bottom:10px;width: 100%" id="stepsConfRow"></div>
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
                                    <td>


                                        <input name="p-<%=parameter.getParamName()%>" style="min-width: 30%"
                                               value='<%=templateParams.get("p-" + parameter.getParamName()) != null ?
                                                templateParams.get("p-" + parameter.getParamName()) : ""%>'
                                               type="<%=type%>">
                                    </td>
                                    <%

                                            }
                                        //todo:handle 'required' value

                                        }
                                    %>
                                </tr>
                                <%
                                        }
                                    }

                                %>
                            </table>
                        </td>
                    </tr>
                </table>
                <br/>
                <table class="styledLeft">

                    <tr>
                        <td class="buttonRow">
                            <input class="button" value="<fmt:message key="back"/>" type="button" onclick="goBack();">
                            <input class="button" value="<fmt:message key="next"/>" type="button" onclick="nextWizard();"/>
                            <input class="button" value="<fmt:message key="cancel"/>" type="button"
                                   onclick="doCancel();"/>
                        </td>
                    </tr>
                </table>
                <br/>
            </form>
            <%
                }
            %>
        </div>
    </div>


    <!-- Using general search component, we have added for user/role search -->
    <div id="id_search_controller_base">
        <div id="id_search_controller" style="display:none;">
            <input type="hidden" id="currentstep" name="currentstep" value=""/>
            <div id="id_user_search">
                <jsp:include page="../search/user-role-search.jsp">
                    <jsp:param name="function-get-all-items" value="getSelectedItems"/>
                </jsp:include>
            </div>
            <!--div id="id-result-holder"></div>
            <div id="id-navigator-holder"></div-->
        </div>
    </div>


</fmt:bundle>