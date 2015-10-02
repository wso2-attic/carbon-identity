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

<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.oauth.common.OAuthConstants" %>
<%@ page import="org.wso2.carbon.identity.oauth.ui.client.OAuthAdminClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ResourceBundle" %>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"  prefix="carbon" %>

<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>
<script type="text/javascript" src="../identity/validation/js/identity-validate.js"></script>

<%
    String BUNDLE = "org.wso2.carbon.identity.oauth.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
    String applicationSPName = request.getParameter("spName");
    session.setAttribute("application-sp-name", applicationSPName);
%>

<jsp:include page="../dialog/display_messages.jsp"/>

<fmt:bundle
        basename="org.wso2.carbon.identity.oauth.ui.i18n.Resources">
    <carbon:breadcrumb label="add.new.application"
                       resourceBundle="org.wso2.carbon.identity.oauth.ui.i18n.Resources"
                       topPage="false" request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <div id="middle">
        <h2><fmt:message key='add.new.application'/></h2>

        <div id="workArea">
            <script type="text/javascript">
                function onClickAdd() {
                    var isValidated = doValidateInputToConfirm(document.getElementById('callback'), "<fmt:message key='callback.is.http'/>",
                            validate, null, null);
                    if (isValidated) {
                        validate();
                    }
                }
                function validate() {
                    var value = document.getElementsByName("application")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="application.is.required"/>');
                        return false;
                    }
                    value = document.getElementsByName("callback")[0].value;
                    var version2Checked = document.getElementById("oauthVersion20").checked;
                    if(version2Checked){
                        if (value == '') {
                            if($(jQuery("#grant_code"))[0].checked || $(jQuery("#grant_implicit"))[0].checked){
                                CARBON.showWarningDialog('<fmt:message key="callback.is.required"/>');
                                return false;
                            }
                        } else {
                            if(!$(jQuery("#grant_code"))[0].checked && !$(jQuery("#grant_implicit"))[0].checked){
                                document.getElementsByName("callback")[0].value = '';
                            }
                        }
                    } else {
                        if(value == ''){
                            CARBON.showWarningDialog('<fmt:message key="callback.is.required"/>');
                            return false;
                        }
                    }
                    document.addAppform.submit();
                }
                jQuery(document).ready(function(){
                    jQuery('#oauthVersion10a').click(function(){
                        $(jQuery('#grant_row')).toggle();
                    })
                })
                jQuery(document).ready(function(){
                    jQuery('#oauthVersion20').click(function(){
                        $(jQuery('#grant_row')).toggle();
                    })
                })
                function toggleCallback(){
                    if(!$(jQuery("#grant_code"))[0].checked && !$(jQuery("#grant_implicit"))[0].checked){
                        $(jQuery('#callback_row')).attr('style','display:none');
                    } else {
                        $(jQuery('#callback_row')).attr('style','');
                    }
                }
                function toggleVersion(){
                    if($(jQuery("#oauthVersion10a"))[0].checked){
                        $(jQuery('#callback_row')).attr('style','');
                    } else {
                        toggleCallback();
                    }
                }
            </script>

            <form method="post" name="addAppform" action="add-finish.jsp"
                  target="_self">
                <table style="width: 100%" class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key='new.app'/></th>
                    </tr>
                    </thead>
                    <tbody>
		    <tr>
			<td class="formRow">
				<table class="normal" >
                            <tr>
                                <td class="leftCol-small"><fmt:message key='oauth.version'/><span class="required">*</span> </td>
                                <td><input id="oauthVersion10a" name="oauthVersion" type="radio" value="<%=OAuthConstants.OAuthVersions.VERSION_1A%>" onclick="toggleVersion()"/>1.0a
                                    <input id="oauthVersion20" name="oauthVersion" type="radio" value="<%=OAuthConstants.OAuthVersions.VERSION_2%>" CHECKED onclick="toggleVersion()"/>2.0</td>
                            </tr>
                            <%if  (applicationSPName!= null) {%>
                             <tr style="display: none;">
		                        <td colspan="2" style="display: none;"><input class="text-box-big" type="hidden" id="application" name="application"
		                                   value="<%=Encode.forHtmlAttribute(applicationSPName)%>" /></td>
		                    </tr>
                            <% } else { %>
		                    <tr>
		                        <td class="leftCol-small"><fmt:message key='application.name'/><span class="required">*</span></td>
		                        <td><input class="text-box-big" id="application" name="application"
		                                   type="text" /></td>
		                    </tr>
		                    <% } %>
		                    <tr id="callback_row">
		                        <td class="leftCol-small"><fmt:message key='callback'/><span class="required">*</span></td>
                                <td><input class="text-box-big" id="callback" name="callback" type="text"
                                           black-list-patterns="http-url"/></td>
		                    </tr>
		                     <tr id="grant_row" name="grant_row">
		                        <td class="leftCol-small"><fmt:message key='grantTypes'/></td>
		                        <td>
		                        <table>
                                    <%
                                        String forwardTo = "index.jsp";
                                        try{
                                            String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
                                            String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
                                            ConfigurationContext configContext =
                                                    (ConfigurationContext) config.getServletContext()
                                                            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
                                            OAuthAdminClient client = new OAuthAdminClient(cookie, backendServerURL, configContext);
                                            List<String> allowedGrants = new ArrayList<String>(Arrays.asList(client.getAllowedOAuthGrantTypes()));
                                            if(allowedGrants.contains("authorization_code")){
                                                %><tr><label><input type="checkbox" id="grant_code" name="grant_code" value="authorization_code" checked="checked" onclick="toggleCallback()"/>Code</label></tr><%
                                            }
                                            if(allowedGrants.contains("implicit")){
                                                %><tr><label><input type="checkbox" id="grant_implicit" name="grant_implicit" value="implicit" checked="checked" onclick="toggleCallback()"/>Implicit</label></tr><%
                                            }
                                            if(allowedGrants.contains("password")){
                                                %><tr><lable><input type="checkbox" id="grant_password" name="grant_password" value="password" checked="checked"/>Password</lable></tr><%
                                            }
                                            if(allowedGrants.contains("client_credentials")){
                                                %><tr><label><input type="checkbox" id="grant_client" name="grant_client" value="client_credentials" checked="checked"/>Client Credential</label></tr><%
                                            }
                                            if(allowedGrants.contains("refresh_token")){
                                                %><tr><label><input type="checkbox" id="grant_refresh" name="grant_refresh" value="refresh_token" checked="checked"/>Refresh Token</label></tr><%
                                            }
                                            if(allowedGrants.contains("urn:ietf:params:oauth:grant-type:saml1-bearer")){
                                                %><tr><label><input type="checkbox" id="grant_saml1" name="grant_saml1" value="urn:ietf:params:oauth:grant-type:saml1-bearer" checked="checked"/>SAML1</label></tr><%
                                            }
                                            if(allowedGrants.contains("urn:ietf:params:oauth:grant-type:saml2-bearer")){
                                                %><tr><label><input type="checkbox" id="grant_saml2" name="grant_saml2" value="urn:ietf:params:oauth:grant-type:saml2-bearer" checked="checked"/>SAML2</label></tr><%
                                            }if(allowedGrants.contains("iwa:ntlm")){
                                                %><tr><label><input type="checkbox" id="grant_ntlm" name="grant_ntlm" value="iwa:ntlm" checked="checked"/>IWA-NTLM</label></tr><%
                                            }
                                        } catch (Exception e){
                                            String message = resourceBundle.getString("error.while.getting.allowed.grants") + " : " + e.getMessage();
                                            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request, e);
                                    %>
                                                <script type="text/javascript">
                                                    function forward() {
                                                        location.href = "<%=forwardTo%>";
                                                    }
                                                </script>

                                                <script type="text/javascript">
                                                        forward();
                                                </script>
                                    <%
                                        }

                                    %>

		                        </table>   
		                        </td>
		                    </tr>
				</table>
			</td>
		    </tr>
                    <tr>
                        <td class="buttonRow" >
                            <input name="addprofile" type="button" class="button" value="<fmt:message key='add'/>" onclick="onClickAdd();"/>
                            
                            <%
                            
                            boolean applicationComponentFound = CarbonUIUtil.isContextRegistered(config, "/application/");
                            if (applicationComponentFound) {                            
                            %>
                            <input type="button" class="button"
                                       onclick="javascript:location.href='../application/configure-service-provider.jsp'"
                                   value="<fmt:message key='cancel'/>"/>
                            <% } else { %>
                                   
                            <input type="button" class="button"
                                       onclick="javascript:location.href='index.jsp?region=region1&item=oauth_menu&ordinal=0'"
                                   value="<fmt:message key='cancel'/>"/>
                            <%} %>
                       </td>
                    </tr>
                    </tbody>
                </table>

            </form>
        </div>
    </div>
</fmt:bundle>

