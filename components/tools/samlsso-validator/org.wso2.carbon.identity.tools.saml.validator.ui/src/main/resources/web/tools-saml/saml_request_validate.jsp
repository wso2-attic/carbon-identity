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
<%@ page import="org.wso2.carbon.identity.tools.saml.validator.stub.types.ValidatedItemDTO"%>
<%@ page import="org.wso2.carbon.identity.tools.saml.validator.ui.SAMLSSOUIUtil"%>
<%@ page import="org.wso2.carbon.identity.tools.saml.validator.ui.client.SAMLSSOValidatorServiceClient"%>
<%@ page
	import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="java.util.ResourceBundle"%>
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
		if($("#samlReqest").val().length == 0)
		{
	        CARBON.showWarningDialog(
	               	"Please provide encoded SAML request.",
	                null, null);
			return false;
		}
		else if($('#isPost').val() == "false" && ($("#samlReqest").val().indexOf('http') == -1 || $("#samlReqest").val().indexOf('SAMLRequest') == -1))
		{
	        CARBON.showWarningDialog(
	               	"SAML Request format is invalid.",
	                null, null);
			return false;
		}
		return true;
	}
	
	function changeVisbilityHelpContent(){
		if($("#isPost").val() == "false"){
			$('#helpReqFormat').show();
		}else{
			$('#helpReqFormat').hide();
		}
	}
</script>

<%
	String backendServerURL;
	ConfigurationContext configContext;
	String cookie;
	SAMLSSOValidatorServiceClient client;
	ValidatedItemDTO[] validatedItems = null;
	String doValidate = SAMLSSOUIUtil.getSafeInput(request, "doValidate");
	String samlRequest = null;
	boolean isDoValidate = false;
	boolean isPost = true;
	if (doValidate != null && !doValidate.trim().isEmpty() && doValidate.equals("true")) {
		isDoValidate = true;
	}

	if (isDoValidate) {
		backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
		configContext =
		                (ConfigurationContext) config.getServletContext()
		                                             .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
		cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
		String BUNDLE = "org.wso2.carbon.identity.tools.saml.validator.ui.i18n.Resources";
		ResourceBundle resourceBundle =
		                                ResourceBundle.getBundle(BUNDLE,
		                                                         request.getLocale());

		try {
			client =
			         new SAMLSSOValidatorServiceClient(cookie, backendServerURL,
			                                           configContext);
			samlRequest = request.getParameter("samlReqest");
			String isPostStr = SAMLSSOUIUtil.getSafeInput(request, "isPost");
			isPost = isPostStr != null ? Boolean.parseBoolean(isPostStr) : false;
			validatedItems = client.validate(samlRequest, isPost);
		} catch (Exception e) {
			CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR,
			                                    request, e);
		}
	}
%>
<div id="middle">
	<%
		if (isDoValidate && validatedItems != null && validatedItems.length > 0) {
	%>
	<h2>Results</h2>
	<div id="workArea">
		<%
			for (ValidatedItemDTO item : validatedItems) {
		%>
		<div style="padding-bottom: 10px;">
			<div style="font-weight: bold; padding-bottom: 5px;"><%=Encode.forHtml(item.getType())%></div>
			<div
				style="padding-left: 10px; color: <%=item.getSuccess() ? "#029219" : "#E01212"%>"><%=Encode.forHtml(item.getMessage())%></div>
		</div>
		<%
			}
		%>
	</div>
	<%
		}
	%>
	<h2 style="padding-top: 20px">Validate SAML Request</h2>
	<div id="workArea">
		<form method="POST" action="saml_request_validate.jsp" onsubmit="return doValidation();">
			<table id="mainTable" class="styledLeft noBorders carbonFormTable">
				<tbody>
					<tr>
						<td style="width: 20%;">Service Provider Initiated Request
							Binding <span class="required">*</span></td>
						<td><select id="isPost" name="isPost" class="text-box-big" onchange="changeVisbilityHelpContent();">
								<option value="true" <%=isPost ? "selected" : "" %>>HTTP POST</option>
								<option value="false" <%=isPost ? "" : "selected" %>>HTTP Redirect</option>
						</select> <input type="hidden" id="doValidate" name="doValidate"
							value="true"></td>
					</tr>
					<tr>
						<td class="leftCol-small" style="vertical-align: top !important">SAML
							Request <span class="required">*</span></td>
						<td style="height: 200px;"><textarea type="text"
								name="samlReqest" id="samlReqest" class="text-box-big"
								style="width: 50%; min-height: 180px;" autocomplete="off"><%=(isDoValidate && samlRequest != null) ? Encode.forHtmlContent(samlRequest): ""%></textarea>
								<div id="helpReqFormat" class="sectionHelp" style="display: none;">SAML Request should be in similar format to 'https://localhost:9443/samlsso?SAMLRequest=jZJdj6owEIb%2FCum9CGIUG9Gg%...'</div>
								</td>
					</tr>
					<tr>
						<td class="buttonRow" colspan="2"><input class="button"
							type="submit" value="Validate"> <input type="button"
							class="button"
							onclick="javascript:location.href='../admin/index.jsp'"
							value="Cancel"></td>
					</tr>
				</tbody>
			</table>
		</form>
	</div>
</div>