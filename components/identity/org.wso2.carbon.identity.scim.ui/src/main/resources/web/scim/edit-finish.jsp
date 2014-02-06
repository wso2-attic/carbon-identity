<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@page import="org.wso2.carbon.utils.ServerConstants" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="java.util.Map" %>
<%@page import="java.util.Iterator" %>
<%@page import="java.lang.Exception" %>

<%@page import="org.wso2.carbon.user.core.UserCoreConstants" %>
<%@page import="java.util.ResourceBundle" %>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<jsp:include page="../dialog/display_messages.jsp"/>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.identity.scim.common.stub.config.SCIMProviderDTO" %>
<%@ page import="org.wso2.carbon.identity.scim.ui.client.SCIMConfigAdminClient" %>
<%@ page import="org.wso2.carbon.context.PrivilegedCarbonContext" %>
<%@ page import="org.wso2.carbon.identity.scim.ui.utils.SCIMUIUtils" %>

<%
    String providerId = CharacterEncoder.getSafeText(request.getParameter("providerId"));
    String username = CharacterEncoder.getSafeText(request.getParameter("username"));
    String password = CharacterEncoder.getSafeText(request.getParameter("password"));
    String userURL = CharacterEncoder.getSafeText(request.getParameter("userURL"));
    String groupURL = CharacterEncoder.getSafeText(request.getParameter("groupURL"));
    String forwardTo = "index.jsp";
    String BUNDLE = "org.wso2.carbon.identity.scim.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
    SCIMProviderDTO provider = new SCIMProviderDTO();

    try {
            String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
            String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
            ConfigurationContext configContext =
                    (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
            SCIMConfigAdminClient client = new SCIMConfigAdminClient(cookie, backendServerURL, configContext);

            provider.setProviderId(providerId);
            provider.setUserName(username);
            provider.setPassword(password);
            provider.setUserEPURL(userURL);
            provider.setGroupEPURL(groupURL);
            client.updateGlobalProvider(SCIMUIUtils.getGlobalConsumerId(), provider);
            String message = resourceBundle.getString("provider.updated.successfully");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.INFO, request);
        
    } catch (Exception e) {
        String message = resourceBundle.getString("error.while.updating.provider");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request, e);
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