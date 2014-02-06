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
        session.setAttribute("forwardPage", forwardPage);
    }
%>

<%
    if (cssLocation != null) {
        // This request is coming from a non-admin console
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
<fmt:bundle basename="org.wso2.carbon.identity.relyingparty.ui.i18n.Resources">
<%
    if (cssLocation == null) {
%>
<carbon:breadcrumb
        label="signin.with.openid"
        resourceBundle="org.wso2.carbon.identity.relyingparty.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>
<%
    }
%>

<style type="text/css">
    /* styles for Information Card support. */
    .openid-url {
        background: #FFFFFF url( images/openid-input.gif ) no-repeat scroll 0pt 50%;
        padding-left: 18px;
    }
</style>
<div id="middle">
    <%
        String user = null;
        user = (String) session.getAttribute("logged-user");
        if (user != null) {
    %>
    <div id="middle">
        <%
            if (cssLocation == null) {
        %>
        <h2><fmt:message key='signin'/></h2>
        <%
            }
        %>

        <div id="workArea">
            <fmt:message key='signout.message1'/>
            <a href="../admin/logout_action.jsp"><fmt:message key='signout.message2'/></a>
            <fmt:message key='signout.message3'/>
        </div>
    </div>
    <%} else { %>

    <%
        if (cssLocation == null) {
    %>
    <h2><fmt:message key='association.message2'/></h2>
    <%
        }
    %>

    <div id="workArea">

        <div id="loginbox" style="width:400px;">
                        <strong><fmt:message key='signin.with.openid'/></strong>
                        <form action="openid.jsp">
                            <%
                                if (cssLocation != null) {
                            %>
                            <input type="hidden" name="forwardPage"
                                   value="<%=URLEncoder.encode(forwardPage,"UTF-8")%>"/>
                            <input type="hidden" name="css"
                                   value="<%=URLEncoder.encode(cssLocation,"UTF-8")%>"/>
                            <input type="hidden" name="title"
                                   value="<%=URLEncoder.encode(pageTitle,"UTF-8")%>"/>
                            <%
                                }
                            %>
                            <table>
                                <tr height="22">
                                    <td colspan="2"></td>
                                </tr>
                                <tr>
                                    <td>
                                        <input class='openid-url' id='openIdUrl'
                                               name="openIdUrl" size='30'/>
                                    </td>
                                    <td>
                                        <input type="submit" value="<fmt:message key='login'/>"
                                               class="button">
                                    </td>
                                </tr>
                                <tr height="22">
                                    <td colspan="20"></td>
                                </tr>
                            </table>
                        </form>
                        <br>
			           

                    </div>







    </div>
    <% } %>
</div>
</fmt:bundle>

<%
    if (cssLocation != null) {
        // Closing HTML page tags.
%>
<div class="footer-content">
    <div class="copyright">&copy; 2008 - 2013 WSO2 Inc. All Rights Reserved.</div>
</div>
</body>
</html>
<%
    }
%>
