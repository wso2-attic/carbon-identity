<%--
  ~ Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
  --%>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.sts.mgt.stub.dto.CardIssuerDTO" %>
<%@ page import="org.wso2.carbon.identity.sts.mgt.ui.client.STSAdminServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="java.util.ResourceBundle" %>
<script type="text/javascript" src="global-params.js"></script>
<jsp:include page="../dialog/display_messages.jsp"/>

<%
String backendServerURL;
ConfigurationContext configContext;
String cookie;
STSAdminServiceClient client = null;
CardIssuerDTO cardIssuer = null;
String cardName = null;
String validPeriod =null;
String symmetricBinding=null;

backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
configContext = (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
client = new STSAdminServiceClient(cookie, backendServerURL,configContext);
String BUNDLE = "org.wso2.carbon.identity.sts.mgt.ui.i18n.Resources";
ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

try {
	cardIssuer = client.readCardIssuerConfiguration();
	cardIssuer.setCardName(request.getParameter("cardName"));
	cardIssuer.setValidPeriodInDays(Integer.parseInt(request.getParameter("validPeriod")));
	if ("true".equals(request.getParameter("symmetricBinding")))
	{
		cardIssuer.setSymmetricBinding(true);
	} else
	{
		cardIssuer.setSymmetricBinding(false);
	}
	
	boolean hasToken = false;
	
	for(int i = 0; i<cardIssuer.getSupportedTokenTypes().length; i++)
	{
		String val = null;
		val = request.getParameter(cardIssuer.getSupportedTokenTypes()[i].getTokenType()+"hidden");
		if (val!=null)
		{
			if ("false".equals(val))			{
				cardIssuer.getSupportedTokenTypes()[i].setSupported(false);
			} else {
				hasToken = true;
				cardIssuer.getSupportedTokenTypes()[i].setSupported(true);
			}				
		}
		
	}	
	
	if (!hasToken){
		 String message = resourceBundle.getString("select.atleast.one.supporting.token");
		 CarbonUIMessage.sendCarbonUIMessage(message,CarbonUIMessage.INFO, request);
    %>
    <script type="text/javascript">              
			   location.href = "configure.jsp";
	</script>
	<%
		
	} else {	
	client.updateCardIssueConfiguration(cardIssuer);
	%>
	 <script type="text/javascript">
			  location.href = "index.jsp?region=region1&item=sts_menu&ordinal=0";
	 </script>
    <%	 
	}
	return;
 } catch (Exception e) {
	%>
	 <script type="text/javascript">
	        location.href = "../admin/error.jsp";
	 </script>
	<%
	return;
}	
	%>