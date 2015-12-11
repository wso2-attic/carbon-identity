<!--
 ~ Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

<%@page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO"%>
<%@ page import="org.wso2.carbon.identity.oauth.ui.client.OAuthAdminClient"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>

<%@ page import="java.util.ResourceBundle"%>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon"%>

<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<jsp:include page="../dialog/display_messages.jsp" />

<%
	String consumerkey = request.getParameter("consumerkey");
    String appName = request.getParameter("appName");
    String spName = request.getParameter("spName");

	String forwardTo = "index.jsp";
	
	boolean qpplicationComponentFound = CarbonUIUtil.isContextRegistered(config, "/application/");
	if (qpplicationComponentFound) {
        forwardTo = "../application/configure-service-provider.jsp?action=delete&spName="+ Encode.forUriComponent(spName)+"&oauthapp="+ Encode.forUriComponent(consumerkey);
	}
	
    String BUNDLE = "org.wso2.carbon.identity.oauth.ui.i18n.Resources";
	ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
	
    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        OAuthAdminClient client = new OAuthAdminClient(cookie, backendServerURL, configContext);
        
        if (appName!=null){
			OAuthConsumerAppDTO app = client.getOAuthApplicationDataByAppName(appName);
			consumerkey = app.getOauthConsumerKey();
        }
        
        client.removeOAuthApplicationData(consumerkey);
        String message = resourceBundle.getString("app.removed.successfully");
        CarbonUIMessage.sendCarbonUIMessage(message,CarbonUIMessage.INFO, request);
    } catch (Exception e) {
    	String message = resourceBundle.getString("error.while.removing.app");
    	CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request,e);
        forwardTo ="../admin/error.jsp";
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