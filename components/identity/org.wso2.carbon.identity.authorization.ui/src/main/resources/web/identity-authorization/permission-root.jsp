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
<%@ page import="org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionDTO" %>

<jsp:useBean id="authorizationBean" type="org.wso2.carbon.identity.authorization.ui.ErrorStatusBean"
             class="org.wso2.carbon.identity.authorization.ui.ErrorStatusBean" scope="session"/>
<jsp:setProperty name="authorizationBean" property="*" />
<%
    authorizationBean.setSecondaryRootNodes(null);
    String forwardTo = null;
    String[] permissionFinders = null;
    String selectedFinderName = null;
    PermissionDTO[] permissionDTOs = null;
    boolean role = false;

    selectedFinderName = request.getParameter("finderModule");
    if(IdentityAuthorizationConstants.COMBO_BOX_DEFAULT_VALUE.equals(selectedFinderName)){
        selectedFinderName = null;
    }

    String fromUserMgt = request.getParameter("fromUserMgt");

    if("true".equals(fromUserMgt)){
        authorizationBean.cleanAuthorizationBean();
        String userName = request.getParameter("userName");
        String roleName = request.getParameter("roleName");
        if(userName != null && userName.trim().length() > 0){
            authorizationBean.addSelectedUser(userName);
            authorizationBean.setFilter(userName);
            authorizationBean.setUserName(userName);
        }
        if(roleName != null && roleName.trim().length() > 0){
            authorizationBean.addSelectedRole(roleName);
            authorizationBean.setRoleName(roleName);
            role = true;
        }
    }

    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config
            .getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

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

        if(selectedFinderName != null && selectedFinderName.trim().length() > 0){
            if(authorizationBean.getUserName() != null){
                permissionDTOs = client.
                        getUserPermissions(authorizationBean.getUserName(), selectedFinderName);
            } else if(authorizationBean.getRoleName() != null){
                permissionDTOs = client.
                        getRolePermissions(authorizationBean.getRoleName(), selectedFinderName);
            }
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

    function doCancel(jspName){
         location.href = jspName;
    }

    function getFinderModule() {
        var comboBox = document.getElementById("finderModule");
        var finderModule = comboBox[comboBox.selectedIndex].value;
        location.href = 'permission-root.jsp?finderModule=' + finderModule ;
    }

    var allPolicesSelected = false;

    function deleteServices() {
        var selected = false;
        if (document.userPermissionForm.userPermission[0] != null) { // there is more than 1 policy
            for (var j = 0; j < document.userPermissionForm.userPermission.length; j++) {
                selected = document.userPermissionForm.userPermission[j].checked;
                if (selected) break;
            }
        } else if (document.userPermissionForm.userPermission != null) { // only 1 policy
            selected = document.userPermissionForm.userPermission.checked;
        }
        if (!selected) {
            CARBON.showInfoDialog('Please select permission to be Delete');
            return;
        }
        if (allPolicesSelected) {
            CARBON.showConfirmationDialog("Do you want to remove all permission from use?",function() {
                document.userPermissionForm.action = "permission-delete.jsp";
                document.userPermissionForm.submit();
            });
        } else {
            CARBON.showConfirmationDialog("Do you want to remove selected permission?",function() {
                document.userPermissionForm.action = "permission-delete.jsp";
                document.userPermissionForm.submit();
            });
        }
    }

    function selectAllInThisPage(isSelected) {
        allPolicesSelected = false;
        if (document.userPermissionForm.userPermission != null &&
            document.userPermissionForm.userPermission[0] != null) { // there is more than 1 service
            if (isSelected) {
                for (var j = 0; j < document.userPermissionForm.userPermission.length; j++) {
                    document.userPermissionForm.userPermission[j].checked = true;
                }
            } else {
                for (j = 0; j < document.userPermissionForm.userPermission.length; j++) {
                    document.userPermissionForm.userPermission[j].checked = false;
                }
            }
        } else if (document.userPermissionForm.userPermission != null) { // only 1 service
            document.userPermissionForm.userPermission.checked = isSelected;
        }
        return false;
    }

    function selectAllInAllPages() {
        selectAllInThisPage(true);
        allPolicesSelected = true;
        return false;
    }

    function resetVars() {
        allPolicesSelected = false;

        var isSelected = false;
        if (document.userPermissionForm.userPermission[0] != null) { // there is more than 1 service
            for (var j = 0; j < document.userPermissionForm.userPermission.length; j++) {
                if (document.userPermissionForm.userPermission[j].checked) {
                    isSelected = true;
                }
            }
        } else if (document.userPermissionForm.userPermission != null) { // only 1 service
            if (document.userPermissionForm.userPermission.checked) {
                isSelected = true;
            }
        }
        return false;
    }

    function addNewPermission(){
        var value = document.getElementsByName("finderModule")[0].value;
        if(value == '<%=IdentityAuthorizationConstants.COMBO_BOX_DEFAULT_VALUE%>') {
            CARBON.showWarningDialog('Please select permission module to configure permission');
            return;
        }
        location.href = "permission-tree.jsp"    
    }
</script>

<div id="middle">
    <h2>Configure Authorization</h2>
    <div id="workArea">
        <form id="userPermissionForm" name="userPermissionForm" method="post" action="">
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

        <table style="margin-top:10px;margin-bottom:10px">
            <tbody>
            <tr>
                <td>
                    <a style="cursor: pointer;" onclick="selectAllInThisPage(true);return false;" href="#"><fmt:message key="selectAllInPage"/></a>
                    &nbsp;<b>|</b>&nbsp;</td><td><a style="cursor: pointer;" onclick="selectAllInThisPage(false);return false;" href="#"><fmt:message key="selectNone"/></a>
                </td>
                <td>
                    <a onclick="deleteServices();return false;"  href="#"  class="icon-link"
                       style="background-image: url(images/delete.gif);" ><fmt:message key="delete"/></a>
                </td>
                <td>
                    <a onclick="addNewPermission();return false;"  href="#" class="icon-link"
                       style="background-image: url(images/publish.gif);" >Add New Permission</a>
                </td>
            </tr>
            </tbody>
        </table>

        <table class="styledLeft" id="userTable">
            <thead>
                <tr>
                    <th colspan="5" class="leftCol-big">
<%

    String fullString = "";
    if(authorizationBean.getPermissionModuleDTO() != null &&
            authorizationBean.getPermissionModuleDTO().getNameForChildRootNodeSet() != null) {
        String[] names = authorizationBean.getPermissionModuleDTO().getNameForChildRootNodeSet();
        for(String name : names){
            if("".equals(fullString)){
                fullString = name; 
            } else {
                fullString = fullString +  " -> " + name;
            }
        }
%>
     User Permissions  [ <%=fullString%> ]
<%
} else {
%>
     User Permissions
<%
    }
%>

                    </th>
                </tr>
            </thead>
            <tbody>
            <%
                if(permissionDTOs != null && permissionDTOs.length > 0){
                    for(PermissionDTO dto : permissionDTOs){
                        if(dto.getPermissionId() != null){
                            String id = dto.getPermissionId();
                            if(authorizationBean.getPermissionModuleName() != null){
                                id = id.substring(authorizationBean.getPermissionModuleDTO().
                                        getRootIdentifier().length() + 1);
                                    if(id.contains("/")){
                                        id = id.replace("/",  " -> ");
                                    }
                            }
                            String authorization = "NOT ALLOW";
                            if(dto.getAction() == null){
                                dto.setAction("");
                            }
                            if(dto.getAuthorized()){
                                authorization = "ALLOW";
                            }
            %>
                    <tr>
                        <td width="10px" style="text-align:center; !important">
                        <input type="checkbox" name="userPermission"
                               value="<%=dto.getPermissionId()+ ',' + dto.getAction()%>"
                               onclick="resetVars()" class="chkBox" />
                        </td>
                        <td><%=id%></td>
                        <%--<td><%=dto.getAction()%></td>--%>
                        <%--<td><%=authorization%></td>--%>
                    </tr>

            <%
                        }
                    }
                } else {
            %>
              <tr>
                  <td><%if(role){%>No role permissions are defined <%}else{%> No user permissions are defined <%}%></td>
              </tr>

            <%
                }
            %>
            </tbody>
        </table>
            <tr>
                <td class="buttonRow" >
                    <%if(role){%>
                    <input type="button" onclick="doCancel('../role/role-mgt.jsp');" value="Cancel" class="button"/>
                    <%}else{%>
                     <input type="button" onclick="doCancel('../user/user-mgt.jsp');" value="Cancel" class="button"/>
                    <%}%>
                </td>
            </tr>
        </form>
    </div>
</div>
</fmt:bundle>

