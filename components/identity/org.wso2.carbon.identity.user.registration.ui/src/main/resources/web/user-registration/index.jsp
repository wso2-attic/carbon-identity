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
<%@ page import="java.net.URLDecoder" %>
<%@ page import="java.net.URLEncoder" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.user.registration.ui.UserRegistrationConstants" %>
<%@ page import="org.wso2.carbon.identity.user.registration.ui.client.UserRegistrationClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>

<%@ page import="java.util.ResourceBundle" %>

<%
    String cssLocation = request.getParameter("css");
    if ("null".equals(cssLocation)) {
        cssLocation = null;
    }

    if (cssLocation != null) {
        cssLocation = URLDecoder.decode(cssLocation, "UTF-8");
    }

    String pageTitle = request.getParameter("title");

    String forwardPage = request.getParameter("forwardPage");
    if (forwardPage != null) {
        forwardPage = URLDecoder.decode(forwardPage, "UTF-8");
    }

    String forwardTo = "index.jsp?region=region1&item=user_registration_menu&ordinal=0";

    String BUNDLE = "org.wso2.carbon.identity.user.registration.ui.i18n.Resources";
    java.util.ResourceBundle resourceBundle = java.util.ResourceBundle.getBundle(BUNDLE, request.getLocale());

    UserRegistrationClient userRegistrationClient = (UserRegistrationClient)session.
            getAttribute(UserRegistrationClient.USER_REGISTRATION_CLIENT_NAME);

    if (userRegistrationClient == null) {
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);

        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        try {
            userRegistrationClient =
                new UserRegistrationClient(cookie, serverURL, configContext);
            session.setAttribute(UserRegistrationClient.USER_REGISTRATION_CLIENT_NAME, userRegistrationClient);
            
        }catch (Exception e) {
            String message = resourceBundle.getString("error.while.loading.user.registration.fields");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
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
    }
%>

<%
    if (cssLocation != null) {
        // We assume that this is a request coming from a non admin console
%>
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="content-type" content="text/html;charset=utf-8"/>
    <title><%=pageTitle%>
    </title>

    <link href="../admin/css/global.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="../dialog/css/jqueryui/jqueryui-themeroller.css" rel="stylesheet" type="text/css"
          media="all"/>
    <link href="../dialog/css/dialog.css" rel="stylesheet" type="text/css" media="all"/>

    <link href="<%=cssLocation%>" rel="stylesheet" type="text/css"
          media="all"/>

    <link rel="icon" href="../admin/images/favicon.ico" type="image/x-icon"/>
    <link rel="shortcut icon" href="../admin/images/favicon.ico" type="image/x-icon"/>

    <script type="text/javascript" src="../admin/js/jquery.js"></script>

    <script type="text/javascript" src="../admin/js/jquery.form.js"></script>
    <script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>

    <script type="text/javascript" src="../admin/js/WSRequest.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
</head>

<body>
<div id="dcontainer"></div>
<script type="text/javascript" src="../dialog/js/dialog.js"></script>

<!--This is the link panel of the portal page-->
<div id="link-panel">
    <div class="left-logo">
        <a class="header-home" href="../../portal">
            <img width="179" height="28" src="images/1px.gif"/>
        </a>
    </div>
</div>
<div id="dcontainer"></div>
<script type="text/javascript" src="../dialog/js/dialog.js"></script>
<%
} else {
%>
<jsp:include page="../dialog/display_messages.jsp"/>
<%
    }
%>


<link media="all" type="text/css" rel="stylesheet" href="css/registration.css"/>

<fmt:bundle basename="org.wso2.carbon.identity.user.registration.ui.i18n.Resources">

    <%
        if (cssLocation == null) {
    %>
    <carbon:breadcrumb
            label="sign.up"
            resourceBundle="org.wso2.carbon.identity.user.registration.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>
    <%
        }
    %>

    <div id="middle">
        <%
            String user = null;
            user = (String) session.getAttribute("logged-user");
            if (user != null) {
        %>
        <div id="middle">
            <h2>Sign Up</h2>

            <div id="workArea">
                <fmt:message key='signout.message1'/><a
                    href="../admin/logout_action.jsp"><fmt:message
                    key='signout.message2'/></a><fmt:message key='signout.message3'/>
            </div>
        </div>
        <%} else { %>

        <%
            if (cssLocation == null) {
        %>
        <h2><fmt:message key='sign.up'/></h2>
        <%
            }
        %>

        <div id="workArea">
            <table style="width:100%">
                <tr>

                <%
                    if (userRegistrationClient != null) {
                        if (userRegistrationClient.isAddUserEnabled()) {
                %>
                    <td>
                        <div id="loginbox" class="identity-box">
                            <strong><fmt:message key='signup.with.username.password'/></strong>

                            <h2></h2>
                            <%
                                String userRegLink = "user-registration.jsp";
                                if (cssLocation != null) {
                                    userRegLink = "user-registration.jsp?css=" +
                                            URLEncoder.encode(cssLocation, "UTF-8") + "&title=" +
                                            pageTitle + "&forwardPage=" +
                                            URLEncoder.encode(forwardPage, "UTF-8");
                                }
                            %>
                            <a href="<%=userRegLink%>"><img
                                    src="images/user-reg.jpg"
                                    border="0"></a>
                        </div>
                    </td>
                <%
                        }
                    }
                %>
                </tr>
            </table>


        </div>
        <% } %>
    </div>
</fmt:bundle>

<%
    if (cssLocation != null) {
        // Closing HTML page tags.
%>
<div class="footer-content">
    <div class="copyright">&copy; 2008 - 2009 WSO2 Inc. All Rights Reserved.</div>
</div>
</body>
</html>
<%
    }
%>