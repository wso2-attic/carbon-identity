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

<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@page import="org.wso2.carbon.identity.provider.ui.client.XMPPConfiguratorClient"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page import="org.wso2.carbon.identity.provider.stub.dto.XMPPSettingsDTO" %>
<script type="text/javascript" src="global-params.js"></script>
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>


<%
String backendServerURL;
ConfigurationContext configContext;
String cookie;
XMPPConfiguratorClient client = null;
String hostName = null;
XMPPSettingsDTO dto = null;
String user = null;

backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
configContext = (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
client = new XMPPConfiguratorClient(cookie, backendServerURL,configContext);
String BUNDLE = "org.wso2.carbon.identity.provider.ui.i18n.Resources";
ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

hostName = request.getParameter("hostName");
user = (String) session.getAttribute("logged-user");

try	{
    boolean isXmppEnabled = true;
    boolean isPINEnabled = false;
    String[] enableXmpp =  request.getParameterValues("enablexmppmultifact");

    String[] enablePIN =  request.getParameterValues("enablePIN");

    if (enablePIN != null) {
        for(String str : enablePIN){
            if(str.equals("true")){
                isPINEnabled = true;
                break;
            }
        }
    }

    dto = new XMPPSettingsDTO();
	dto.setUserId(user);
    dto.setXmppServer(request.getParameter("server"));
    dto.setXmppUserName(request.getParameter("username"));
    dto.setUserCode(request.getParameter("usercode"));
    dto.setXmppEnabled(isXmppEnabled);
    dto.setPINEnabled(isPINEnabled);
    client.addXmppSettings(dto);
    String message = resourceBundle.getString("xmpp.configuration.added.successfully");
    CarbonUIMessage.sendCarbonUIMessage(message,CarbonUIMessage.INFO, request);
%>
	 <script>
          location.href = 'xmpp-config.jsp?region=region5&item=multi_factor_auth_menu';
     </script>
<%

}catch (Exception e) {
      CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
%>
	  <script type="text/javascript">
	    location.href = "../admin/error.jsp";
	  </script>
<%
	return;
}
%>