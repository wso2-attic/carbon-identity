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
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.owasp.encoder.Encode" %>
<script type="text/javascript" src="global-params.js"></script>

<%
String backendServerURL;
ConfigurationContext configContext;
String cookie;
STSAdminServiceClient client = null;
CardIssuerDTO cardIssuer = null;

backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
configContext = (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
client = new STSAdminServiceClient(cookie, backendServerURL,configContext);

try {
	cardIssuer = client.readCardIssuerConfiguration();	
} catch (Exception e) {
    CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
%>
	<script type="text/javascript">
		    location.href = "../admin/error.jsp";
	</script>
<%
	return;
}
%>

<fmt:bundle basename="org.wso2.carbon.identity.sts.mgt.ui.i18n.Resources">
<carbon:breadcrumb 
		label="sts"
		resourceBundle="org.wso2.carbon.identity.sts.mgt.ui.i18n.Resources"
		topPage="true" 
		request="<%=request%>" />
		
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<div id="middle">
    <h2 id="identitysts"><fmt:message key='card.issuer.configuration'/></h2>
    <div id="workArea">
      		<table cellspacing="0" class="normal">
		              <tr>
			                <td>      
			                   <a href="../identity-sts/configure.jsp" class="icon-link" style="background-image:url(images/configure.gif);"><fmt:message key='configure'/></a>  
			               </td>
			          </tr>
		   </table>
           <table class="styledLeft"  width="100%"> 
                      <tr>
			              <td colspan="2" style="border-left: 0px !important; border-right: 0px !important; border-top: 0px !important; padding-left: 0px !important; height: 5px !important;"><img src="../identity-sts/images/1px.gif" height="5"/></td>
			          </tr>		             
		              <tr>
			              <td class="sub-header" colspan="2"><fmt:message key='configuration.parameters'/></td>
		              </tr>
		              <tr>
	                      <td><fmt:message key='card.name'/></td>
	                      <td><%=Encode.forHtmlContent(cardIssuer.getCardName())%></td>
	                  </tr>
	                  <tr>
	                      <td><fmt:message key='valid.period'/></td>
	                      <td><%=cardIssuer.getValidPeriodInDays()%></td>
	                  </tr>
	                    <tr>
	                      <td><fmt:message key='supporting.token.types'/></td>
	                      <td><%=Encode.forHtmlContent(client.getSupportingTokens(cardIssuer.getSupportedTokenTypes()))%></td>
	                  </tr>
	                  <tr>
	                      <td><fmt:message key='symmetric.binding.used'/></td>
	                      <% if (cardIssuer.getSymmetricBinding()) { %>
	                      	<td>true</td>	
	                      <% } else { %>  
	                      	<td>false</td>
	                      <%}%>                   
	                  </tr>
          </table>
    </div>
</div>
</fmt:bundle>