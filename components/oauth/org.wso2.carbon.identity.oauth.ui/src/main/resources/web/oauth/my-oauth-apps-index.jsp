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
<%@ page import="org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO" %>
<%@ page import="org.wso2.carbon.identity.oauth.ui.client.OAuthAdminClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>

<%@ page import="java.util.ResourceBundle" %>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon"%>

<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<jsp:include page="../dialog/display_messages.jsp" />

<%
    String forwardTo;
    OAuthAdminClient client;

    OAuthConsumerAppDTO[] apps = null;
    String BUNDLE = "org.wso2.carbon.identity.oauth.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.getServletContext().
                getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new OAuthAdminClient(cookie,backendServerURL, configContext);
        apps = client.getAppsAuthorizedByUser();
    } catch (Exception e) {
        String message = resourceBundle.getString("error.while.loading.apps");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/error.jsp";
%>
        <script type="text/javascript">
            location.href = "<%=forwardTo%>";
        </script>
<%

    }
%>

<fmt:bundle basename="org.wso2.carbon.identity.oauth.ui.i18n.Resources">
    <carbon:breadcrumb
            label="identity.oauth.my.apps"
            resourceBundle="org.wso2.carbon.identity.oauth.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <script type="text/javascript">
        function itemRemove(appName, userName) {
            CARBON.showConfirmationDialog("<fmt:message key='remove.message1'/>" + appName + "<fmt:message key='remove.message2'/>",
                    function() {
                        location.href = "my-oauth-app-remove-finish.jsp?appName=" + appName;
                    }, null);
        }
    </script>

    <div id="middle">
        <h2><fmt:message key='my.authorized.apps'/></h2>
        <div id="workArea">
            <table style="width: 100%" class="styledLeft">
                <thead>
                <tr>
                    <th><fmt:message key='authorized.apps'/></th>
                    <th><fmt:message key='application.developer'/></th>
                    <th><fmt:message key='actions'/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (apps != null && apps.length > 0) {
                        for (int i = 0; i < apps.length; i++) {
                            String appName = apps[i].getApplicationName();
                            String userName = apps[i].getUsername();
                %>
                <tr>
                    <td width="30%"><%=Encode.forHtml(appName)%></td>
                    <td width="30%"><%=Encode.forHtml(userName)%></td>
                    <td width="40%"><a
                            title="<fmt:message key='remove.app'/>"
                            onclick="itemRemove('<%=Encode.forJavaScriptAttribute(appName)%>');return false;"
                            href="#"
                            style="background-image: url(../oauth/images/delete.gif);"
                            class="icon-link"><fmt:message key='remove.app'/></a>
                </tr>
                <%
                        }
                    } else {
                %>
                <tr>
                    <td colspan="3"><i><fmt:message key='no.oauth.apps'/></i></td>
                </tr>
                <%
                    }
                %>
                </tbody>
            </table>
        </div>
    </div>
</fmt:bundle>
