<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationClient" %>
<%@ page import="java.util.Set" %>
<%@ page import="org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionDTO" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionModuleDTO" %>
<jsp:useBean id="authorizationBean" type="org.wso2.carbon.identity.authorization.ui.ErrorStatusBean"
             class="org.wso2.carbon.identity.authorization.ui.ErrorStatusBean" scope="session"/>
<jsp:setProperty name="authorizationBean" property="*" />

<%
    String forwardTo = null;
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config
            .getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    String selectedRootNode = request.getParameter("rootNode");
    String selectedRootSecondaryNode = request.getParameter("secondaryRootNode");
    String selectedAction = request.getParameter("selectedAction");
    int noOfSelectedAttributes = 1;
    while (true) {
        String resourceNames = request.getParameter("resourceName" + noOfSelectedAttributes);
        if (resourceNames == null || resourceNames.trim().length() < 1) {
            break;
        }

        if(selectedRootSecondaryNode != null) {
            resourceNames = selectedRootSecondaryNode + "/" + resourceNames;
        }

        if(selectedRootNode != null){
             resourceNames = selectedRootNode + "/" + resourceNames;       
        }

        authorizationBean.addSelectedResource(resourceNames);
        noOfSelectedAttributes++;
    }

    Set<String> selectedRoles = authorizationBean.getSelectedRoles();
    Set<String> selectedUsers = authorizationBean.getSelectedUsers();
    Set<String> selectedResources = authorizationBean.getSelectedResources();
    PermissionModuleDTO moduleDTO = authorizationBean.getPermissionModuleDTO();
    Set<PermissionDTO> permissionDTOs = new HashSet<PermissionDTO>();
    

    if((selectedRoles != null || selectedUsers != null) && selectedResources != null){
        if(selectedRoles != null && selectedRoles.size() > 0){
            for(String role : selectedRoles){
                PermissionDTO permissionDTO = new PermissionDTO();
                permissionDTO.setSubject(role);
                permissionDTO.setAuthorized(true);
                permissionDTO.setResources(selectedResources.toArray(new String[selectedResources.size()]));
                if(selectedAction != null){
                    permissionDTO.setAction(selectedAction);
                }
                permissionDTO.setUserPermission(false);
                permissionDTOs.add(permissionDTO);
            }
        } else {
            for(String role : selectedUsers){
                PermissionDTO permissionDTO = new PermissionDTO();
                permissionDTO.setSubject(role);
                permissionDTO.setAuthorized(true);
                permissionDTO.setResources(selectedResources.toArray(new String[selectedResources.size()]));
                if(selectedAction != null){
                    permissionDTO.setAction(selectedAction);
                }
                permissionDTO.setUserPermission(true);
                permissionDTOs.add(permissionDTO);
            }
        }
    }

    try {

        IdentityAuthorizationClient client =
                    new IdentityAuthorizationClient(cookie, serverURL, configContext);
        if(moduleDTO != null){
            client.configurePermission(permissionDTOs.toArray(new PermissionDTO[permissionDTOs.size()]), 
                    moduleDTO.getModuleName());
            String userName = authorizationBean.getUserName();
            String roleName = authorizationBean.getRoleName();
            if(userName != null){
                forwardTo = "permission-root.jsp?fromUserMgt=true&userName=" + userName +
                        "&finderModule=" + authorizationBean.getPermissionModuleName();
            } else if(roleName != null){
                forwardTo = "permission-root.jsp?fromUserMgt=true&roleName=" + roleName +
                        "&finderModule=" + authorizationBean.getPermissionModuleName();
            } else {
                forwardTo = "../user/user-mgt.jsp";
            }
        } else {
            // error
        }

        authorizationBean.cleanAuthorizationBean();
    } catch (Exception e){
        forwardTo = "../admin/error.jsp";
    }
%>
<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>


