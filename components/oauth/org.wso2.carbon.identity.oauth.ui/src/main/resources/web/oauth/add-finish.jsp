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

<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.identity.oauth.common.OAuthConstants"%>
<%@ page import="org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO"%>
<%@ page import="org.wso2.carbon.identity.oauth.ui.client.OAuthAdminClient"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>

<%@ page import="java.util.ResourceBundle" %>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon"%>

<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<jsp:include page="../dialog/display_messages.jsp" />

<%
    String applicationName = request.getParameter("application");
    String callback = request.getParameter("callback");
    String oauthVersion = request.getParameter("oauthVersion");
    //-- start setting grants
    String grantCode = request.getParameter("grant_code");
    String grantImplicit = request.getParameter("grant_implicit");
    String grantPassword = request.getParameter("grant_password");
    String grantClient = request.getParameter("grant_client");
    String grantRefresh = request.getParameter("grant_refresh");
    String grantSAML1 = request.getParameter("grant_saml1");
    String grantSAML2 = request.getParameter("grant_saml2");
    String grantNTLM = request.getParameter("grant_ntlm");
    String grants = null;
   	StringBuffer buff = new StringBuffer();
	if (grantCode != null) {
		buff.append(grantCode + " ");
	}
    if (grantImplicit != null) {
        buff.append(grantImplicit + " ");
    }
	if (grantPassword != null) {
		buff.append(grantPassword + " ");
	}
	if (grantClient != null) {
		buff.append(grantClient + " ");
	}
	if (grantRefresh != null) {
		buff.append(grantRefresh + " ");
	}
	if (grantSAML1 != null) {
		buff.append(grantSAML1+" ");
	}
	if (grantSAML2 != null) {
    	buff.append(grantSAML2+" ");
    }
    if (grantNTLM != null) {
		buff.append(grantNTLM);
	}
	grants = buff.toString();
	// -- end setting grants
	String forwardTo = "index.jsp";
	String BUNDLE = "org.wso2.carbon.identity.oauth.ui.i18n.Resources";
	ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
	OAuthConsumerAppDTO app = new OAuthConsumerAppDTO();
	
	String spName = (String) session.getAttribute("application-sp-name");
	session.removeAttribute("application-sp-name");
	boolean isError = false;
	OAuthConsumerAppDTO consumerApp = null;

	try {

		String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
		String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
		ConfigurationContext configContext =
		                                     (ConfigurationContext) config.getServletContext()
		                                                                  .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
		OAuthAdminClient client = new OAuthAdminClient(cookie, backendServerURL, configContext);
		app.setApplicationName(applicationName);
		app.setCallbackUrl(callback);
		app.setOAuthVersion(oauthVersion);
        if(OAuthConstants.OAuthVersions.VERSION_2.equals(oauthVersion)){
            app.setGrantTypes(grants);
        }
		client.registerOAuthApplicationData(app);
		
		consumerApp = client.getOAuthApplicationDataByAppName(applicationName);
		
		String message = resourceBundle.getString("app.added.successfully");
		CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.INFO, request);

	} catch (Exception e) {
		isError = true;
		String message = resourceBundle.getString("error.while.adding.app") + " : " + e.getMessage();
		CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request, e);
	}
%>

<script>

<%

boolean qpplicationComponentFound = CarbonUIUtil.isContextRegistered(config, "/application/");
if (qpplicationComponentFound) {
	if (!isError) {
		session.setAttribute("oauth-consum-secret", consumerApp.getOauthConsumerSecret());
%>
    location.href = '../application/configure-service-provider.jsp?action=update&display=oauthapp&spName=<%=Encode.forUriComponent(spName)%>&oauthapp=<%=Encode.forUriComponent(consumerApp.getOauthConsumerKey())%>';
<% } else { %>
    location.href = '../application/configure-service-provider.jsp?display=oauthapp&spName=<%=Encode.forUriComponent(spName)%>&action=cancel';
<% } 
} else {%>
    location.href = 'index.jsp';
<% } %>

</script>


<script type="text/javascript">
    forward();
</script>