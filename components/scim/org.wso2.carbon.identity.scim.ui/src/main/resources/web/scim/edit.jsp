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
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="org.wso2.carbon.identity.scim.common.stub.config.SCIMProviderDTO" %>
<%@page import="org.wso2.carbon.identity.scim.ui.client.SCIMConfigAdminClient" %>
<%@page import="org.wso2.carbon.identity.scim.ui.utils.SCIMUIUtils" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage" %>

<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<jsp:include page="../dialog/display_messages.jsp"/>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>

<%
    String providerId = CharacterEncoder.getSafeText(request.getParameter("providerId"));

    SCIMProviderDTO provider = null;
    String forwardTo = null;
    String BUNDLE = "org.wso2.carbon.identity.scim.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
    String id = null;
    String secret = null;

    try {
            String cookie = (String) session
                    .getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
            String backendServerURL = CarbonUIUtil.getServerURL(config
                    .getServletContext(), session);
            ConfigurationContext configContext = (ConfigurationContext) config
                    .getServletContext().getAttribute(
                            CarbonConstants.CONFIGURATION_CONTEXT);
            SCIMConfigAdminClient client = new SCIMConfigAdminClient(cookie,
                                                                     backendServerURL, configContext);

            provider = client.getGlobalProvider(SCIMUIUtils.getGlobalConsumerId(), providerId);
        
} catch (Exception e) {
    String message = resourceBundle.getString("error.while.loading.scim.provider.data");
    CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
    forwardTo = "../admin/error.jsp";
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

<script type="text/javascript">
</script>


<%@page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>
<fmt:bundle basename="org.wso2.carbon.identity.scim.ui.i18n.Resources">
    <carbon:breadcrumb label="provider.settings"
                       resourceBundle="org.wso2.carbon.identity.scim.ui.i18n.Resources"
                       topPage="false" request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <div id="middle">

        <h2><fmt:message key='view.provider'/></h2>

        <div id="workArea">
            <script type="text/javascript">
                function validate() {
                    value = document.getElementsByName("username")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="username.required"/>');
                        return false;
                    }
                    value = document.getElementsByName("password")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="password.required"/>');
                        return false;
                    }
                    value = document.getElementsByName("userURL")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="user.url.is.required"/>');
                        return false;
                    }
                    document.editProviderform.submit();
                }
            </script>

            <form method="post" name="editProviderform" action="edit-finish.jsp" target="_self">
                <table style="width: 100%" class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key='provider.settings'/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td class="formRow">
                            <table class="normal" cellspacing="0">
                                <tr>
                                    <td class="leftCol-small"><fmt:message
                                            key='provider.id'/></td>
                                    <td><%=provider.getProviderId()%><input id="providerId"
                                                                         name="providerId"
                                                                         type="hidden"
                                                                         value="<%=provider.getProviderId()%>"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='user.name'/><font
                                            class="required">*</font></td>
                                    <td><input class="text-box-big" id="username" name="username"
                                               type="text" value="<%=provider.getUserName()%>"/></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='password'/><font
                                            class="required">*</font></td>
                                    <td><input class="text-box-big" id="password" name="password"
                                               type="password" value="<%=provider.getPassword()%>"/></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='user.endpoint.url'/><font
                                            class="required">*</font></td>
                                    <td><input class="text-box-big" id="userURL" name="userURL"
                                               type="text" value="<%=provider.getUserEPURL()%>"/></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='group.endpoint.url'/><font
                                            class="required">*</font></td>
                                    <td><input class="text-box-big" id="groupURL" name="groupURL"
                                               type="text" value="<%=provider.getGroupEPURL()%>"/></td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td class="buttonRow">
                            <input name="update"
                                   type="button" class="button" value="<fmt:message key='update'/>"
                                   onclick="validate();"/>
                            <input type="button" class="button"
                                   onclick="javascript:location.href='index.jsp?region=region5&item=userprofiles_menu&ordinal=0'"
                                   value="<fmt:message key='cancel'/>"/></td>
                    </tr>
                    </tbody>
                </table>

            </form>
        </div>
    </div>
</fmt:bundle>

