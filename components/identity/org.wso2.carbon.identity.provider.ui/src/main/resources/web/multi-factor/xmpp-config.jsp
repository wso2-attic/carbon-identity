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
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.identity.provider.ui.client.XMPPConfiguratorClient" %>
<%@ page import="org.wso2.carbon.identity.provider.ui.client.XMPPConfiguratorClient" %>
<%@ page import="org.wso2.carbon.identity.provider.stub.dto.XMPPSettingsDTO" %>
<jsp:include page="../dialog/display_messages.jsp"/>

<%
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    XMPPConfiguratorClient client = new XMPPConfiguratorClient(cookie, serverURL, configContext);

    boolean isXmppSettingsAvailable = client.isXMPPSettingsAvailable((String) session.getAttribute("logged-user"));

    XMPPSettingsDTO xmppSettingsDto = null;
    boolean isXmppEnabled = false;
    boolean isPINEnabled = false;
    String defaultIM = client.getUserIM((String)session.getAttribute("logged-user"));
    if(defaultIM == null){
        defaultIM = "";
    }
    else if(!defaultIM.trim().endsWith("@gmail.com")){
        defaultIM = "";        
    }

    if (isXmppSettingsAvailable) {
        xmppSettingsDto = client.getXmppSettingsDTO((String) session.getAttribute("logged-user"));
        isXmppEnabled = xmppSettingsDto.getXmppEnabled();
        isPINEnabled = xmppSettingsDto.getPINEnabled();
    }

%>

<link media="all" type="text/css" rel="stylesheet" href="css/registration.css"/>

<fmt:bundle basename="org.wso2.carbon.identity.provider.ui.i18n.Resources">
<carbon:breadcrumb
        label="muliti.factor.auth"
        resourceBundle="org.wso2.carbon.identity.provider.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<div id="middle">

<h2 id="identity"><fmt:message key='multifactor.authentication.configurations'/></h2>

<% if (!isXmppSettingsAvailable) { %>

<div id="workArea">
    <script type="text/javascript">
        function validateSubmit() {
                var value = document.getElementsByName("username")[0].value;
                if (value == '') {
                    CARBON.showWarningDialog('<fmt:message key="username.required"/>');
                    return false;
                }
                value = document.getElementsByName("usercode")[0].value;
                var password = value;
                if (value == '') {
                    CARBON.showWarningDialog('<fmt:message key="pin.required"/>');
                    return false;
                }

                value = document.getElementsByName("reusercode")[0].value;
                var repassword = value;
                if (value == '') {
                    CARBON.showWarningDialog('<fmt:message key="retype.pin"/>');
                    return false;
                }

                if (value.length < 6) {
                    CARBON.showWarningDialog('<fmt:message key="short.pin"/>');
                    return false;
                }

                if (repassword != password) {
                    CARBON.showWarningDialog('<fmt:message key="pin.mismatch"/>');
                    return false;
                }

            document.addxmppconfigform.submit();
        }
        function clearInputs() {
        	document.addxmppconfigform.server.value = "";
        	document.addxmppconfigform.username.value = "";
        	document.addxmppconfigform.usercode.value = "";
        	document.addxmppconfigform.reusercode.value = "";
        	document.addxmppconfigform.enablePIN.checked=false;
        }
    </script>
    <script>
        function disableFields(chkbx) {
            document.addxmppconfigform.username.readOnly = (chkbx.checked) ? false : true;
            document.addxmppconfigform.server.readOnly = (chkbx.checked) ? false : true;
            document.addxmppconfigform.enablePIN.readOnly = (chkbx.checked) ? false : true;
            document.addxmppconfigform.adduser.disabled = (chkbx.checked) ? 0 : 1;
            document.addxmppconfigform.usercode.readOnly = (chkbx.checked) ? false : true;
            document.addxmppconfigform.reusercode.readOnly = (chkbx.checked) ? false : true;

        }        
    </script>


    <form name="addxmppconfigform" action="add-xmpp-settings.jsp" method="POST" target="_self">
        <!-- table>
            <tr>
                <td><input type="checkbox" name="enablexmppmultifact" VALUE="true" onclick="disableFields(this);">Enable
                    <fmt:message key='xmpp.based.multifactor.authentication'/>
                </td>
            </tr>
        </table-->
        <table style="width: 100%" class="styledLeft">
            <thead>
            <tr>
                <th colspan="2"><fmt:message key='add.new.xmpp.configuration'/></th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="leftCol-small"><fmt:message key='xmpp.provider'/></td>
                <td>
                    <select id="server" name="server">
                        <option value="GTalk"><fmt:message key='gtalk'/></option>
                    </select>
                </td>
            </tr>
            <tr>
                <td class="leftCol-small"><fmt:message key='user.name'/><span class="required">*</span></td>
                <td><input type="text" name="username" id="username" value="<%=defaultIM%>"/></td>
            </tr>

            <tr>
                <td class="leftCol-small"><fmt:message key='pin.number'/><span class="required">*</span></td>
                <td><input type="password" name="usercode" id="usercode" /></td>
            </tr>
            <tr>
                <td class="leftCol-small"><fmt:message key='retype.pin.number'/><span class="required">*</span></td>
                <td><input type="password" name="reusercode" id="reusercode" /></td>
            </tr>
            <tr>
                <td colspan="2"><input type="checkbox" name="enablePIN" VALUE="true" >
                    <fmt:message key='use.pin.for.authentication'/>
                </td>
            </tr>
            <tr>
                <td colspan="2" class="buttonRow">
                    <input name="adduser" type="button" id="addbutton" class="button"
                           value="<fmt:message key='add'/>" onclick="validateSubmit();"/>
                    <input name="clearButton" type="button" id="clearButton" class="button"
                           value="<fmt:message key='clear'/>" onclick="clearInputs()"/>
                </td>
            </tr>
            </tbody>
        </table>
    </form>
</div>
<% } else {%>
<div id="workArea">
    <script type="text/javascript">
        function validateEdit() {

            var value = document.getElementsByName("updatedUsername")[0].value;
            if (value == '') {
                CARBON.showWarningDialog('<fmt:message key="username.required"/>');
                return false;
            }
            value = document.getElementsByName("updatedUsercode")[0].value;
            var password = value;
            if (value == '') {
                CARBON.showWarningDialog('<fmt:message key="pin.required"/>');
                return false;
            }

            value = document.getElementsByName("reupdatedUsercode")[0].value;
            var repassword = value;
            if (value == '') {
                CARBON.showWarningDialog('<fmt:message key="retype.pin"/>');
                return false;
            }

            if (value.length < 6) {
                CARBON.showWarningDialog('<fmt:message key="short.pin"/>');
                return false;
            }

            if (repassword != password) {
                CARBON.showWarningDialog('<fmt:message key="pin.mismatch"/>');
                return false;
            }

            document.editxmppconfigform.submit();
        }
    </script>
    <script>
        function disableFields(chkbx) {
            document.editxmppconfigform.updatedUsername.readOnly = (chkbx.checked) ? false : true;
            document.editxmppconfigform.server.readOnly = (chkbx.checked) ? false : true;
            document.editxmppconfigform.updatedUsercode.readOnly = (chkbx.checked) ? false : true;
            document.editxmppconfigform.reupdatedUsercode.readOnly = (chkbx.checked) ? false : true;
            document.editxmppconfigform.EnablePIN.readOnly = (chkbx.checked) ? false : true;
        }
        function clearInputs() {
        	document.editxmppconfigform.updatedUsername.value = "";
            document.editxmppconfigform.server.value = "";
            document.editxmppconfigform.updatedUsercode.value = "";
            document.editxmppconfigform.reupdatedUsercode.value = "";
            document.editxmppconfigform.EnablePIN.checked=false;
        }
    </script>
    <form name="editxmppconfigform" action="edit-xmpp-settings.jsp" method="POST" target="_self">
        <!-- table style="width: 100%" class="styledLeft">
            <tr>
                <td><input type="checkbox" name="enablexmppmultifact" VALUE="true"
                           onclick="disableFields(this);" <%if(isXmppEnabled){ out.print("checked");}%>>
                      <fmt:message key='enable.xmpp.authentication'/>
                </td>
            </tr>
         </table-->
            <table style="width: 100%" class="styledLeft">
               <thead>
                    <tr>
                        <th colspan="2"><fmt:message key='update.xmpp.configuration'/></th>
                    </tr>
               </thead>
               <tbody>
                    <tr>
                        <td class="leftCol-small"><fmt:message key='xmpp.provider'/></td>
                        <td>
                            <select id="server" name="server">
                                <option value="GTalk"><fmt:message key='gtalk'/></option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-small"><fmt:message key='user.name'/><span class="required">*</span></td>
                        <td><input type="text" name="updatedUsername" id="updatedUsername"
                                   value="<%=xmppSettingsDto.getXmppUserName()%>" <%
                            if (!isXmppEnabled) {
                                out.print("readonly=\"readonly\"");
                            }
                        %>/></td>
                    </tr>
                    <tr>
                        <td class="leftCol-small"><fmt:message key='pin.number'/><span class="required">*</span></td>
                        <td><input type="password" name="updatedUsercode" id="updatedUsercode"
                                   value="<%=xmppSettingsDto.getUserCode()%>" <%
                            if (!isXmppEnabled) {
                                out.print("readonly=\"readonly\"");
                            }
                        %>/></td>
                    </tr>
                    <tr>
                        <td class="leftCol-small"><fmt:message key='retype.pin.number'/><span class="required">*</span></td>
                        <td><input type="password" name="reupdatedUsercode" id="reupdatedUsercode"
                                   value="<%=xmppSettingsDto.getUserCode()%>" <%
                            if (!isXmppEnabled) {
                                out.print("readonly=\"readonly\"");
                            }
                        %>/></td>
                    </tr>
                    <tr>
                        <td colspan="2"><input type="checkbox" name="EnablePIN" VALUE="true" <%if(isPINEnabled){ out.print("checked");}%>
                                <%
                            if (!isXmppEnabled) {
                                out.print("readonly=\"readonly\"");
                            }
                        %>/>
                            <fmt:message key='use.pin.for.authentication'/>
                        </td>
                    </tr>
                     <tr>
                		<td colspan="2"><input type="checkbox" name="enablexmppmultifact" VALUE="true"
                           <%if(isXmppEnabled){ out.print("checked");}%>>
                      		<fmt:message key='enable.xmpp.authentication'/>
                		</td>
            		 </tr>
                    <tr>
                        <td class="buttonRow" colspan="2">
                            <input name="adduser" type="button" class="button"
                                   value="<fmt:message key='save'/>" onclick="validateEdit();"/>
                		</td>                		
                    </tr>
               </tbody>
                </table>
                 
    </form>
</div>
<% }%>
</div>
</fmt:bundle>