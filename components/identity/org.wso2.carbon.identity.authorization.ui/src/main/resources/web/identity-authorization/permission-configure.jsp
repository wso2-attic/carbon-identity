<!--
 ~ Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationConstants" %>
<%@ page import="org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationClient" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionModuleDTO" %>

<jsp:useBean id="authorizationBean" type="org.wso2.carbon.identity.authorization.ui.ErrorStatusBean"
             class="org.wso2.carbon.identity.authorization.ui.ErrorStatusBean" scope="session"/>
<jsp:setProperty name="authorizationBean" property="*" />
<%

    authorizationBean.setSecondaryRootNodes(null);
    authorizationBean.setRootNodes(null);

    String forwardTo = null;
    String[] permissionFinders = null;
    String[] actions = null;
    String selectedFinderName = null;
    boolean userMgt = false;

    String fromUserMgt = request.getParameter("fromUserMgt");
    String region = request.getParameter("region");
    if(region != null && region.trim().length() > 0){
        authorizationBean.cleanAuthorizationBean();        
    }

    if("true".equals(fromUserMgt)){
        userMgt = true;    
    }

    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config
            .getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    int noOfSelectedAttributes = 1;
    while (true) {
        String resourceNames = request.getParameter("resourceName" + noOfSelectedAttributes);
        if (resourceNames == null || resourceNames.trim().length() < 1) {
            break;
        }
        authorizationBean.addSelectedResource(resourceNames);
        noOfSelectedAttributes++;
    }

    String[] selectedRole = request.getParameterValues("roleNames");
    if(selectedRole != null){
        for(String role : selectedRole){
            authorizationBean.addSelectedRole(role);
        }
    }

    if(!userMgt){
        try {
            IdentityAuthorizationClient client =
                        new IdentityAuthorizationClient(cookie, serverURL, configContext);
            permissionFinders = client.getPermissionModules();

            if(selectedFinderName != null && selectedFinderName.trim().length() > 0){
                PermissionModuleDTO module = client.getModuleInfo(selectedFinderName);
                if(module != null){
                    authorizationBean.setPermissionModuleDTO(module);
                }
            } else {
                selectedFinderName = authorizationBean.getPermissionModuleName();
            }
        } catch (Exception e){
            forwardTo = "../admin/error.jsp";

    %>
    <script
        type="text/javascript">
        function forward() {
            location.href = "<%=forwardTo%>";
        }
    </script>

    <script type="text/javascript">
        forward();
    </script>

    <%
            }
        }
    %>


<fmt:bundle basename="org.wso2.carbon.identity.authorization.ui.i18n.Resources">
<carbon:breadcrumb
		label="advance.search"
		resourceBundle="org.wso2.carbon.identity.authorization.ui.i18n.Resources"
		topPage="true"
		request="<%=request%>" />

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="resources/js/main.js"></script>
    <!--Yahoo includes for dom event handling-->
    <script src="../yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>
    <script src="../identity-authorization/js/create-basic-policy.js" type="text/javascript"></script>
    <link href="../identity-authorization/css/entitlement.css" rel="stylesheet" type="text/css" media="all"/>

    
     <!--Yahoo includes for dom event handling-->
    <script src="http://yui.yahooapis.com/2.8.1/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>

    <!--Yahoo includes for animations-->
    <script src="http://yui.yahooapis.com/2.8.1/build/animation/animation-min.js" type="text/javascript"></script>

    <!--Local js includes-->
    <script type="text/javascript" src="js/treecontrol.js"></script>
    <script type="text/javascript" src="js/popup.js"></script>

    <link href="css/tree-styles.css" media="all" rel="stylesheet" />
    <%--<link href="css/dsxmleditor.css" media="all" rel="stylesheet" />--%>
<script type="text/javascript">

    function doUpdate(){
        document.attributeValueForm.action = "permission-update.jsp";
        document.attributeValueForm.submit();
    }


    function doCancel(){
         location.href = "permission-cancel.jsp";
    }


    function doClear(){
         location.href = "permission-cancel.jsp?clearData=true";
    }

    function doGoResources(){
//        var comboBox = document.getElementById("finderModule");
//        var finderModule = comboBox[comboBox.selectedIndex].value;
        location.href = 'permission-tree.jsp';
    }

    function goRoles(){
        location.href = 'permission-roles.jsp';
    }

    function getFinderModule() {
        var comboBox = document.getElementById("finderModule");
        var finderModule = comboBox[comboBox.selectedIndex].value;
        location.href = 'permission-configure.jsp?finderModule=' + finderModule ;
    }
    
</script>

<div id="middle">
    <h2>Configure Authorization</h2>
    <div id="workArea">
        <form id="attributeValueForm" name="attributeValueForm" method="post" action="">

            <%
                if(!userMgt){
            %>
            <table>
                <tr>
                <td class="leftCel-med">
                    Select Permission finder module
                </td>
                <td>
                    <select  onchange="getFinderModule()" id="finderModule" name="finderModule" class="text-box-big">
                        <option value="<%=IdentityAuthorizationConstants.COMBO_BOX_DEFAULT_VALUE%>" selected="selected">
                            <%=IdentityAuthorizationConstants.COMBO_BOX_DEFAULT_VALUE%></option>
                    <%
                        if (permissionFinders != null) {
                            for (String finder : permissionFinders) {
                                if(selectedFinderName != null && selectedFinderName.equals(finder)){
                    %>
                            <option value="<%=finder%>" selected="selected"><%=selectedFinderName%></option>
                    <%
                                } else {
                    %>
                            <option value="<%=finder%>"><%=finder%></option>
                    <%
                                }
                            }
                        }
                    %>
                    </select>
                </td>
                </tr>
            </table>

            <%
                }
            %>

            <table>
                <tr>
                    <td>Select Role </td>
                    <td><input type="button" onclick="goRoles();" value="View" class="button"/></td>
                </tr>
            </table>

            <%
                if(authorizationBean.getSelectedResources().size() > 0){
            %>
            <table>
                <tr>
                    <td style="border:solid 1px #ccc">
                        <div style="overflow: auto;height:300px" id="listView">
                            <table>
                <%
                    for(String resource : authorizationBean.getSelectedResources()){
                %>
                        <tr><td><%=resource%></td></tr>
                <%
                    }
                %>
                            </table>
                        </div>
                    </td>
                </tr>
            </table>
            <%
                }
            %>

            <%
                if(authorizationBean.getSelectedResources().size() > 0 && actions != null && actions.length > 0){
            %>
            <table>
                <tr>
                    <td>Select Action </td>
                </tr>
                <tr>
                <td>
                    <select id="selectedAction" name="selectedAction">
                    <%
                        for (String action : actions) {
                            if(action.equals(authorizationBean.getSelectedAction())){
                    %>
                        <option value="<%=action%>" selected="selected"><%=authorizationBean.getSelectedAction()%></option>
                    <%
                            } else {
                    %>
                        <option value="<%=action%>"><%=action%></option>
                    <%
                            }
                        }
                    %>
                    </select>
                </td>
                </tr>
            </table>
            <%
                }
            %>

            <%
            if(!"true".equals(fromUserMgt)){
            %>

            <table>
                <tr>
                    <td>Select Roles </td>
                    <td><input type="button" onclick="doGoRoles();" value="View" class="button"/></td>
                </tr>
            </table>

            <%
            if(authorizationBean.getSelectedRoles().size() > 0){
            %>
            <table>
                <tr>
                    <td style="border:solid 1px #ccc">
                        <div style="overflow: auto;height:300px" id="listView2">
                            <table>
                <%
                    for(String role : authorizationBean.getSelectedRoles()){
                %>
                        <tr>
                            <td><%=role%></td>
                        </tr>
                <%
                    }
                %>
                            </table>
                        </div>
                    </td>
                </tr>
            </table>
            <%
                }
            }
            %>

            <tr>
                <td class="buttonRow" >
                    <input type="button" onclick="doUpdate();" value="Update" class="button"/>
                    <input type="button" onclick="doClear();" value="Clear" class="button"/>
                </td>
            </tr>
        </form>
    </div>
</div>
</fmt:bundle>

