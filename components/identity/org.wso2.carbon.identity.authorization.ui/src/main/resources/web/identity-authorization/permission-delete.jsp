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

    Set<PermissionDTO> permissionDTOs = new HashSet<PermissionDTO>();
    PermissionModuleDTO moduleDTO = authorizationBean.getPermissionModuleDTO();
    String[] userPermissions = request.getParameterValues("userPermission");

    if(userPermissions != null){
        for(String userPermission : userPermissions){
            PermissionDTO dto = new PermissionDTO();
            if(userPermission.contains(",")){
                String[] array = userPermission.split(",");
                if(array != null && array.length == 2){
                    dto.setPermissionId(array[0]);
                    dto.setAction(array[1]);        
                } else if(array != null && array.length == 1){
                    dto.setPermissionId(array[0]);
                }
            }
            if(authorizationBean.getUserName() != null){
                dto.setSubject(authorizationBean.getUserName());
                dto.setUserPermission(true);
            } else if(authorizationBean.getRoleName() != null) {
                dto.setSubject(authorizationBean.getRoleName());
            }
            permissionDTOs.add(dto);
        }
    }
    try {
        IdentityAuthorizationClient client =
                    new IdentityAuthorizationClient(cookie, serverURL, configContext);
        if(moduleDTO != null){
            client.clearUserAuthorization(permissionDTOs.toArray(new PermissionDTO[permissionDTOs.size()]),
                    moduleDTO.getModuleName());
        } else {
            // error
        }
        forwardTo = "permission-root.jsp";
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


