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
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.wso2.carbon.identity.relyingparty.stub.dto.OpenIDDTO" %>
<%@ page import="org.wso2.carbon.identity.relyingparty.stub.dto.ClaimDTO" %>

<script type="text/javascript" src="global-params.js"></script>

<%

    String cssLocation = request.getParameter("css");
    if ("null".equals(cssLocation)) {
        cssLocation = null;
    }

    if (cssLocation != null) {
        cssLocation = URLDecoder.decode(cssLocation, "UTF-8");
    }

    String pageTitle = request.getParameter("title");
    if (pageTitle != null) {
        pageTitle = URLDecoder.decode(pageTitle, "UTF-8");
    }

    String forwardPage = request.getParameter("forwardPage");
    if (forwardPage != null) {
        forwardPage = URLDecoder.decode(forwardPage, "UTF-8");
    }

    String backendServerURL;
    ConfigurationContext configContext;
    String cookie;

    String openID = (String) session.getAttribute("openIdURL");
    OpenIDDTO openIddto = (OpenIDDTO) session.getAttribute("openId");
    ClaimDTO[] claims = openIddto.getClaims();

    for (ClaimDTO claim : claims) {
        session.setAttribute(claim.getClaimUri(), claim.getClaimValue());
    }

    backendServerURL = CarbonUIUtil.getServerURL(config
            .getServletContext(), session);
    configContext = (ConfigurationContext) config.getServletContext()
            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    cookie = (String) session
            .getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
%>

<%
    if (cssLocation != null) {
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
    }

%>
<fmt:bundle basename="org.wso2.carbon.identity.relyingparty.ui.i18n.Resources">

    <%
        if (cssLocation == null) {
    %>
    <carbon:breadcrumb
            label="associate.openid"
            resourceBundle="org.wso2.carbon.identity.relyingparty.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>
    <%
        }
    %>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <div id="middle">
        <h2 id="identity"><fmt:message key='associate.openid.with.profile'/></h2>

        <div id="workArea">
            <form>
                <table cellspacing="0" class="normal">
                    <tr>
                        <td class="sub-header" colspan="2">
                            <a href="<%=openID%>" class="icon-link"
                               style="background-image:url(images/openid-input.gif); margin-left:0"><%=openID%>
                            </a>
                        </td>
                    </tr>
                    <tr height="4">
                        <td colspan="2"></td>
                    </tr>
                    <tr>
                        <td><fmt:message key='association.message1'/>
                            <%
                                String signInLink = "openid_sign_in.jsp";
                                if (cssLocation != null) {
                                    signInLink = "openid_sign_in.jsp?forwardPage=" +
                                            URLEncoder.encode(forwardPage, "UTF-8") +
                                            "&css=" + URLEncoder.encode(cssLocation, "UTF-8") +
                                            "&title=" +
                                            URLEncoder.encode(pageTitle, "UTF-8");
                                }
                            %>

                            <a href="javascript:document.location.href='<%=signInLink%>'">
                                <fmt:message key='association.message11'/></a><fmt:message
                                key='association.message3'/>
                            <%
                                String signUpLink = "../user-registration/user-registration.jsp";
                                if (cssLocation != null) {
                                    signUpLink = "../user-registration/user-registration.jsp?forwardPage=" +
                                            URLEncoder.encode(forwardPage, "UTF-8") +
                                            "&css=" + URLEncoder.encode(cssLocation, "UTF-8") +
                                            "&title=" +
                                            URLEncoder.encode(pageTitle, "UTF-8");
                                }
                            %>

                            <a href="javascript:document.location.href='<%=signUpLink%>'">
                                <fmt:message key='association.message4'/></a><fmt:message
                                key='association.message5'/>
                        </td>

                    </tr>
                </table>
            </form>
        </div>
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