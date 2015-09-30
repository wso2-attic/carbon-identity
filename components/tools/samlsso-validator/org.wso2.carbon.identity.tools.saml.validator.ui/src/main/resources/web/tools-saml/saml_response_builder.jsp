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
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.identity.tools.saml.validator.stub.types.GeneratedResponseDTO"%>
<%@ page import="org.wso2.carbon.identity.tools.saml.validator.ui.SAMLSSOUIUtil"%>
<%@ page import="org.wso2.carbon.identity.tools.saml.validator.ui.client.SAMLSSOValidatorServiceClient"%>
<%@ page
	import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page
	import="java.util.ResourceBundle"%>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>
<jsp:include page="../dialog/display_messages.jsp" />

<script type="text/javascript" src="global-params.js"></script>
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>
<script type="text/javascript">
	function doValidation(){
		if($("#issuer").length == 0 && $("#issuer").val() == null || $("#issuer").val().length == 0){
	        CARBON.showWarningDialog(
	               	"Please register at least one SAML2 SSO enabled Service Provider.",
	                null, null);
			return false;
		}
		else if($("#userName").val().length == 0){
	        CARBON.showWarningDialog(
	               	"Please provide user name.",
	                null, null);
			return false;
		}

		return true;
	}
</script>
<%
	String backendServerURL;
	ConfigurationContext configContext;
	String cookie;
	SAMLSSOValidatorServiceClient client;
	String buildResponse = request.getParameter("buildResponse");
	String[] issuers = null;
	GeneratedResponseDTO responseDTO = null;
	boolean isBuildResponse = false;
	String issuer = null;
	String userName = null;
	if (buildResponse != null && !buildResponse.trim().isEmpty() &&
	    buildResponse.equals("true")) {
		isBuildResponse = true;
	}
	backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
	configContext =
	                (ConfigurationContext) config.getServletContext()
	                                             .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
	cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
	String BUNDLE = "org.wso2.carbon.identity.tools.saml.validator.ui.i18n.Resources";
	ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
	try {
		client = new SAMLSSOValidatorServiceClient(cookie, backendServerURL, configContext);
		issuers = client.getIssuersOfSAMLServiceProviders();
		if (isBuildResponse) {
			issuer = request.getParameter("issuer");
			userName = request.getParameter("userName");
			responseDTO = client.buildResponse(issuer, userName);
		}
	} catch (Exception e) {
		CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request,
		                                    e);
	}
%>
<div id="middle">
	<%
		if (isBuildResponse) {
			if(!responseDTO.getSuccess()){
				%>
				<div id="workArea">
					<div style="padding-bottom: 10px;">
						<div style="font-weight: bold; padding-bottom: 5px;">Building Response Failed</div>
						<div style="padding-left: 10px; color:#E01212;"><%=Encode.forHtml(responseDTO.getMessage())%></div>
					</div>
				</div>
				<%
			} else {
				%>
					<h2>SAML Response</h2>
						<div id="workArea">
							<div style="width: 100%;">
								<div style="width: 100%;"><textarea type="text"
								name="samlResponse" id="samlResponse" class="text-box-big"
								style="width: 99%; height: 250px; background: #FFFFFF;" autocomplete="off" readonly><%=Encode.forHtmlContent(responseDTO.getXmlResponse())%></textarea></div>
							</div>
						</div>
					<h2 style="padding-top: 20px" >Encoded SAML Response</h2>
						<div id="workArea">
							<div style="width: 100%;">
								<div style="width: 100%;"><textarea type="text"
								name="samlResponse" id="samlResponse" class="text-box-big"
								style="width: 99%; height: 250px; background: #FFFFFF;" autocomplete="off" readonly><%=Encode.forHtmlContent(responseDTO.getEncodedResponse())%></textarea></div>
							</div>
						</div>
			<%
				}
			%>
		<%
			}
		%>
	<h2 style="padding-top: 20px">Build SAML Response</h2>
	<div id="workArea">
		<form method="POST" action="saml_response_builder.jsp" onsubmit="return doValidation();">
			<table id="mainTable" class="styledLeft noBorders carbonFormTable">
				<tbody>
					<tr>
						<td>Issuer <span class="required">*</span></td>
						<td>
							<%
								if (issuers != null && issuers.length > 0 && issuers[0] != null) {
							%> 
							<select id="issuer" name="issuer" class="text-box-big">
							<% for(String issuerItem : issuers){ 
									if(issuerItem != null) {%>
									<option value="<%=Encode.forHtmlAttribute(issuerItem)%>" <%=(issuer != null && issuer.equals(issuerItem)) ? "selected" : ""%>><%=Encode.forHtmlContent(issuerItem)%></option>
									<%
									}
								}
 							%> 
							</select> 
							<%
 								} else {
 							%> 
 								There are no SAML2 SSO enabled Service Providers in the system.
 							<%  } %>
 							<input type="hidden" id="buildResponse" name="buildResponse" value="true">
						</td>
					</tr>
					<tr>
						<td class="leftCol-small">User Name <span class="required">*</span></td>
						<td><input type="text" id="userName" name="userName" value="<%=userName != null ? Encode.forHtmlAttribute(userName) : ""%>"></td>
					</tr>
					<tr>
						<td class="buttonRow" colspan="2"><input class="button"
							type="submit" value="Generate"> <input type="button"
							class="button"
							onclick="javascript:location.href='../admin/index.jsp'"
							value="Cancel"></td>
					</tr>
				</tbody>
			</table>
		</form>
	</div>
</div>