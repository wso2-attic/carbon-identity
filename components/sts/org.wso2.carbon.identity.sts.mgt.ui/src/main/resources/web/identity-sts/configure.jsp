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
<jsp:include page="../dialog/display_messages.jsp"/>

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
		label="configure"
		resourceBundle="org.wso2.carbon.identity.sts.mgt.ui.i18n.Resources"
		topPage="false" 
		request="<%=request%>" />
		
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<div id="middle">
    <h2 id="identitysts"><fmt:message key='card.issuer.configuration'/></h2>
    <div id="workArea">
         	<script type="text/javascript">

	    	 function setValue(chk,hidden) {
	    		var val = document.getElementById(chk).checked; 
    			var hiddenElement = document.getElementById(hidden); 

    			if (val){
    				hiddenElement.value="true";
    			}else {
    				hiddenElement.value="false";
    			}    	
   		   	  }

	         function validate(form) {            
	         	var value = document.getElementsByName("cardName")[0].value;
	         	if (value == '') {
	             	CARBON.showWarningDialog('<fmt:message key="card.name.required"/>');
	             	return false;
	         	}      
   
	         	var value = document.getElementsByName("validPeriod")[0].value;
	         	if (value != '') {
	         		var IsFound = /^-?\d+$/.test(value);
	             	if(!IsFound) {
	                  CARBON.showWarningDialog('<fmt:message key="invalid.valid.period"/>');
	             	  return false;
	             	}else if (value <= 0){
	             		CARBON.showWarningDialog('<fmt:message key="invalid.valid.period"/>');
		                return false;
	             	}		             	
	         	}else {
	         		CARBON.showWarningDialog('<fmt:message key="invalid.valid.period"/>');
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
	                      <td><fmt:message key='card.name'/><font class="required">*</font> </td>
	                      <td> <input type="text" name="cardName" id="cardName" value="<%=Encode.forHtmlAttribute(cardIssuer.getCardName())%>" /></td>
	                  </tr>
	                  <tr>
	                      <td><fmt:message key='valid.period'/><font class="required">*</font> </td>  
	                      <td><input type="text" name="validPeriod" id="validPeriod" value="<%=cardIssuer.getValidPeriodInDays()%>" /></td>
	                  </tr>
	                    <tr>
	                      <td><fmt:message key='supporting.token.types'/><font class="required">*</font></td>
	                      <td style="margin-left: 0">	                       
	                       <%for (int i=0; i<cardIssuer.getSupportedTokenTypes().length;i++) { 
	                         if (cardIssuer.getSupportedTokenTypes()[i].getSupported()) {
	                       %> 
	                       <div>
	                          <input type="hidden" name='<%=Encode.forHtmlAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType()) + "hidden"%>' id='<%=Encode.forHtmlAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType()) + "hidden"%>' value="true"/>
	                          <input type='checkbox' name='<%=Encode.forHtmlAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())%>' id='<%=Encode.forHtmlAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())%>' checked='checked' onclick="setValue('<%=Encode.forJavaScriptAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())%>','<%=Encode.forJavaScriptAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())+"hidden"%>')" /> <%=Encode.forHtmlContent(cardIssuer.getSupportedTokenTypes()[i].getTokenType())%> &nbsp;
	                       </div>
	                       <% } else { %>
	                       <div>
	                          <input type="hidden" name='<%=Encode.forHtmlAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())+ "hidden"%>' id='<%=Encode.forHtmlAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())+ "hidden"%>' value="false"/>
	                          <input type='checkbox' name='<%=Encode.forHtmlAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())%>' id='<%=Encode.forHtmlAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())%>' onclick="setValue('<%=Encode.forJavaScriptAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())%>','<%=Encode.forJavaScriptAttribute(cardIssuer.getSupportedTokenTypes()[i].getTokenType())+"hidden"%>')" /> <%=Encode.forHtmlContent(cardIssuer.getSupportedTokenTypes()[i].getTokenType())%> &nbsp;
	                       </div>
	                       <%} }%>	                      
	                      </td>	                     
	                  </tr>
	                  <tr>
	                      <td><fmt:message key='symmetric.binding.used'/></td>
	                      <% if (cardIssuer.getSymmetricBinding()) { %>
	                      	<td>
	                      	   <input type="hidden" name='symmetricBinding' id='symmetricBinding' value="true"/>
	                      	   <input type='checkbox' name='isSymmetricBinding' value='isSymmetricBinding' id='isSymmetricBinding' checked='checked' onclick="setValue('isSymmetricBinding','symmetricBinding')" />
	                      	</td>	
	                      <% } else { %>  
	                      	<td>
	                      	   <input type="hidden" name='symmetricBinding' id='symmetricBinding' value="false"/>
                               <input type='checkbox' name='isSymmetricBinding' value='isSymmetricBinding' id='isSymmetricBinding' onclick="setValue('isSymmetricBinding','symmetricBinding')" />
							</td>
	                      <%}%>                   
	                  </tr>
	                  </table>
			</td>
			</tr>
	                  <tr>
			            <td colspan="2"  class="buttonRow">
                           <input class="button" type="button" value="<fmt:message key='update'/>" onclick="javascript: validate(document.configuration)"/>  
                           <input class="button" type="reset" value="<fmt:message key='cancel'/>"  onclick="javascript:document.location.href='index.jsp?region=region1&item=sts_menu&ordinal=0'"/>
                        </td>
			         </tr>	
          </table>
          </form>
    </div>
</div>
</fmt:bundle>