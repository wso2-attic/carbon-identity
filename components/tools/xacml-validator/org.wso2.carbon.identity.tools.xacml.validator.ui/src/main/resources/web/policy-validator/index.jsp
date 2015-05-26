<!--
~ Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
        %>

<%@page import="org.wso2.carbon.identity.tools.xacml.validator.ui.client.IdentityXACMLValidatorServiceClient" %>
<%@page import="java.util.ResourceBundle" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.identity.entitlement.ui.EntitlementPolicyConstants" %>
<%@ page import="org.wso2.carbon.identity.entitlement.dto.xsd.PolicyDTO" %>
<%@ page import="org.wso2.carbon.identity.tools.xacml.validator.bean.xsd.ValidationResult" %>

<jsp:include page="../resources/resources-i18n-ajaxprocessor.jsp"/>
<jsp:include page="../dialog/display_messages.jsp"/>


<carbon:breadcrumb label="import.policy"
                   resourceBundle="org.wso2.carbon.identity.tools.xacml.validator.ui.i18n.Resources"
                   topPage="true" request="<%=request%>"/>

<%
    String importFrom = (String) request.getParameter("importFrom");
    String[] importingMethods = new String[]{EntitlementPolicyConstants.IMPORT_POLICY_REGISTRY,
            EntitlementPolicyConstants.IMPORT_POLICY_FILE_SYSTEM};
    if (importFrom == null || importFrom.trim().length() == 0) {
        importFrom = EntitlementPolicyConstants.IMPORT_POLICY_FILE_SYSTEM;
    }
%>
<script type="text/javascript" src="global-params.js"></script>
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="../resources/js/resource_util.js"></script>
<script type="text/javascript" src="../resources/js/registry-browser.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>
<script type="text/javascript" src="../yui/build/event/event-min.js"></script>
<script src="../editarea/edit_area_full.js" type="text/javascript"></script>

<script type="text/javascript">
    jQuery(document).ready(function () {
        editAreaLoader.init({
            id: "policy", syntax: "xml", start_highlight: true
        });
    });
    function doValidation() {
        if ($("#samlReqest").val().length == 0) {
            CARBON.showWarningDialog(
                    "Please provide encoded SAML request.",
                    null, null);
            return false;
        } else if ($('#isPost').val() == "false" && ($("#samlReqest").val().indexOf('http') == -1 || $("#samlReqest").val().indexOf('SAMLRequest') == -1)) {
            CARBON.showWarningDialog(
                    "SAML Request format is invalid.",
                    null, null);
            return false;
        }
        return true;
    }

    function changeVisbilityHelpContent() {
        if ($("#isPost").val() == "false") {
            $('#helpReqFormat').show();
        } else {
            $('#helpReqFormat').hide();
        }
    }

    function doSubmit() {
        var policy;
        var importFrom = "<%=importFrom%>";

        if (importFrom == 'FileSystem') {
            policy = document.importPolicy.policyFromFileSystem.value;
        } else {
            policy = document.importPolicy.policyFromRegistry.value;
            document.importPolicy.action = "tools-import-policy-submit.jsp";
        }

        if (policy == '') {
            CARBON.showWarningDialog("Please select a policy to upload");
            return;
        }
        document.importPolicy.submit();
    }

    function doCancel() {
        location.href = 'index.jsp';
    }

    function selectPolicyImportMethod() {

        var comboBox = document.getElementById("importingMethod");
        var importingMethod = comboBox[comboBox.selectedIndex].value;
        location.href = 'index.jsp?importFrom=' + importingMethod;
    }
</script>

<%

    String serverURL = CarbonUIUtil.getServerURL(config
            .getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config
            .getServletContext().getAttribute(
                    CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session
            .getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String forwardTo = request.getParameter("forwardTo");
    if (forwardTo == null) {
        forwardTo = "index";
    }
    forwardTo = forwardTo + ".jsp";
    PolicyDTO dto = null;
    ValidationResult[] errorItems = null;
    dto = (PolicyDTO) session.getAttribute("policyDTO");
    IdentityXACMLValidatorServiceClient client = null;
    String policy = request.getParameter("policy");

    if (policy != null) {
        try {
            client = new IdentityXACMLValidatorServiceClient(cookie, serverURL, configContext);
            dto = new PolicyDTO();
            dto.setPolicy(policy);
            errorItems = client.validateXACMLPolicy(dto);
        } catch (Exception e) {
            String message = e.getMessage();
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        }
    } else if (dto != null) {
        try {
            client = new IdentityXACMLValidatorServiceClient(cookie, serverURL, configContext);
            dto.setPolicyEditor("XML");
            errorItems = client.validateXACMLPolicy(dto);
        } catch (Exception e) {
            CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request);
        }
    }
%>
<div id="middle">
    <%
        if (errorItems != null && errorItems.length > 0) {
    %>
    <h2>Results</h2>

    <div id="workArea">
        <%
            for (ValidationResult item : errorItems) {
        %>
        <div style="padding-bottom: 10px;">
            <div style="font-weight: bold; padding-bottom: 5px;"><%=item.getResponseType()%>
            </div>
            <div
                    style="padding-left: 10px; color: <%=item.getResponseType().equals("SUCCESS")
                     ? "#029219" : "#E01212"%>">
                <%=!(item.getResponseType().equals("SUCCESS")) ? "Line Number: " + item.getLineNumber() + "-->" : ""%>
                <%=item.getMessage()%>
            </div>
        </div>
        <%
            }
        %>
    </div>
    <%
        }
    %>
    <h2 style="padding-top: 20px">Validate XACML Policy</h2>

        <div id="workArea">
            <form method="POST" action="index.jsp">
                <table id="mainTable" class="styledLeft noBorders carbonFormTable">
                    <tbody>
                    <tr>
                        <td class="leftCol-small" style="vertical-align: top !important">XACML Policy
                            <span class="required">*</span></td>
                        <td style="height: 200px;"><textarea rows="30" cols="120"

                                                             name="policy" id="policy"
                                                             class="text-box-big"
                                                             style="width: 100%; min-height: 180px;"
                                                             autocomplete="on"><%=dto != null ? dto.getPolicy() : ""%>
                        </textarea>

                            <div id="helpReqFormat" class="sectionHelp" style="display: none;"></div>
                        </td>
                    </tr>
                    <tr>
                        <td class="buttonRow" colspan="2"><input class="button"
                                                                 type="submit" value="Validate"> <input
                                type="button"
                                class="button"
                                onclick="javascript:location.href='../admin/index.jsp'"
                                value="Cancel"></td>
                    </tr>
                    </tbody>
                </table>
            </form>
        </div>

<h1>Hiii</h1>