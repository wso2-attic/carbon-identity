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
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>


<%@page import="org.wso2.carbon.identity.provider.openid.admin.ui.client.OpenIDConfigurationClient"%>
<%@ page import="org.wso2.carbon.identity.provider.openid.admin.stub.dto.OpenIDConfigurationDTO" %>
<script type="text/javascript" src="global-params.js"></script>
<jsp:include page="../dialog/display_messages.jsp"/>

<%
String backendServerURL;
ConfigurationContext configContext;
String cookie;
OpenIDConfigurationClient client = null;
OpenIDConfigurationDTO dto = null;

backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
configContext = (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
client = new OpenIDConfigurationClient(cookie, backendServerURL,configContext);
String userName = request.getParameter("username");

try {
	dto = client.getOpenIDAdmin(userName,null);	
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

<fmt:bundle basename="org.wso2.carbon.identity.provider.openid.admin.ui.i18n.Resources">
<carbon:breadcrumb 
		label="configure"
		resourceBundle="org.wso2.carbon.identity.provider.openid.admin.ui.i18n.Resources"
		topPage="false" 
		request="<%=request%>" />
		
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<div id="middle">
    <h2 id="identitysts"><fmt:message key='openid.configuration'/></h2>
    <div id="workArea">
         	<script type="text/javascript">

	    	 function setType() {
	    		var pattern = document.getElementById("openidpattern");
	    		var val = pattern.value;
    			var hiddenElement = document.getElementById("openidpatternhidden");  
    			hiddenElement.value=val;    		    	
   		   	  }

	         function validate(form) {            
	         	var value = document.getElementsByName("subDomain")[0].value;
	         	if (value == '') {
	             	CARBON.showWarningDialog('<fmt:message key="sub.domain.name.required"/>');
	             	return false;
	         	}    
   
	         	form.submit();
	     	 }  

  	       </script>
           <form action="update_config.jsp" id="configuration" name="configuration">
           <table class="styledLeft"  width="100%"> 
                      <tr>
			              <td colspan="2" style="border-left: 0px !important; border-right: 0px !important; border-top: 0px !important; padding-left: 0px !important; height: 5px !important;"></td>
			          </tr>		             
		              <tr>
			              <td class="sub-header" colspan="2"><fmt:message key='configuration.parameters'/></td>
		              </tr>
				<tr>
				<td class="formRow">
				<table class="normal" cellspacing="0">
		          <tr>
	                      <td><fmt:message key='sub.domain.name'/></td>
	                      <td> <input type="text" name="subDomain" id="subDomain" value="<%=dto.getSubDomain()%>" /></td>	                     
	                  </tr>
	                  <tr>
	                      <td><fmt:message key='default.openid.pattern'/></td>  
	                      <td><%=dto.getDefaultOpenIDPattern()%></td>                  
	                  </tr>
	                  <tr>
	                      <td><fmt:message key='available.openid.patterns'/></td>
	                      <td style="margin-left: 0">	 	
	                       <div>
	                      <input type="hidden" name="openidpatternhidden" id="openidpatternhidden" />
                          <select id="openidpattern" name="openidpattern" onchange="setType()">	                     
	                       <%for (int i=0; i<dto.getAvailableTenantOpenIDPattern().length;i++) { %> 
	                          <option value="<%=dto.getAvailableTenantOpenIDPattern()[i]%>"><%=dto.getAvailableTenantOpenIDPattern()[i]%></option>
	                      
	                       <%}%>	
	                       </select> 
	                        </div>
	                                             
	                      </td>	                     
	                  </tr>
	                 
	                  </table>
			</td>
			</tr>
	                  <tr>
			            <td colspan="2"  class="buttonRow">
                           <input class="button" type="button" value="<fmt:message key='update'/>" onclick="javascript: validate(document.configuration)"/>  
                           <input class="button" type="reset" value="<fmt:message key='cancel'/>"  onclick="javascript:document.location.href='openid_admin.jsp?region=region3&item=openid_admin__menu&ordinal=0'"/ >                        
                        </td>
			         </tr>	
          </table>
          </form>
    </div>
</div>
</fmt:bundle>