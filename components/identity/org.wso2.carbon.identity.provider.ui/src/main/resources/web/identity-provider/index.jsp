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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.base.MultitenantConstants" %>
<%@ page import="org.wso2.carbon.identity.provider.ui.client.IdentityProviderClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>

<script type="text/javascript" src="global-params.js"></script>

<%
    String backendServerURL;
    ConfigurationContext configContext;
    String cookie;
    IdentityProviderClient client;
    String[] openIDs;
    String primaryOpenID;
    String loggedInUser;

    String domain;

    backendServerURL = CarbonUIUtil.getServerURL(config
            .getServletContext(), session);
    configContext = (ConfigurationContext) config.getServletContext()
            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    cookie = (String) session
            .getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    client = new IdentityProviderClient(cookie, backendServerURL,
            configContext);
    loggedInUser = (String) session.getAttribute("logged-user");
    domain = (String)session.getAttribute(MultitenantConstants.TENANT_DOMAIN);
    String tenantUser = null;

    try {    	
    	
    	tenantUser = loggedInUser;
    	
    	if (domain != null && loggedInUser != null &&
                                !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(domain)){
    		tenantUser = loggedInUser + "@" + domain;
    	}    	
        openIDs = client.getAllOpenIDs(tenantUser);
        primaryOpenID = client.getOpenID(tenantUser);
    } catch (Exception e) {
        CarbonUIMessage.sendCarbonUIMessage(e.getMessage(),
                CarbonUIMessage.ERROR, request, e);
%>
<script type="text/javascript">
    location.href = "../admin/error.jsp";
</script>
<%
        return;
    }
%>

<fmt:bundle basename="org.wso2.carbon.identity.provider.ui.i18n.Resources">
    <carbon:breadcrumb
            label="openid.infocard"
            resourceBundle="org.wso2.carbon.identity.provider.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <div id="middle">
        <h2 id="identity"><fmt:message key='openid.infocard.dashboard'/></h2>

        <div id="workArea">
            <script type="text/javascript">
                function itemRemove(openID, index) {
                    if (index == '0') {
                        CARBON.showWarningDialog("<fmt:message key='cannot.remove.openid'/>", null, null);
                        return;
                    }
                    CARBON.showConfirmationDialog("<fmt:message key='remove.message1'/>" + openID + "<fmt:message key='remove.message2'/>",
                            function() {
                                location.href = "remove-openid-signup.jsp?openid=" + openID;
                            }, null);
                }
            </script>
            <table cellspacing="0" class="styledLeft">
                <thead>
                <tr>
                    <th colspan="2"><fmt:message key='my.openids'/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if(openIDs != null){
                        for (int i = 0; i < openIDs.length; i++) {
                            if (openIDs[i] != null && !openIDs[i].trim().equals("")) {
                %>
                <tr>
                    <td width="50%">
                        <a href="<%=openIDs[i]%>" style="background-image: url(images/openid-input.gif);" class="icon-link"><%=openIDs[i]%>
                        </a>
                    </td>
                    <td width="50%">
                    <% if(!openIDs[i].equals(primaryOpenID)) {%>
                        <a title="<fmt:message key='remove.openid'/>"
                           onclick="itemRemove('<%=openIDs[i]%>','<%=i%>');return false;"
                           href="#" style="background-image: url(images/delete.gif);" class="icon-link"><fmt:message key='delete'/></a>
                    <% } else {%>      
                        <fmt:message key='openid.primary'/>
                    <% } %> 
                    </td>
                </tr>
                <%
                            }
                        }
                    }
                %>
                </tbody>
            </table>
            <hr/>
        </div>
    </div>
</fmt:bundle>
