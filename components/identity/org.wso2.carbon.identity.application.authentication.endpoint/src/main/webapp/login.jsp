<!DOCTYPE html>
<!--
~ Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.CharacterEncoder"%>

<fmt:bundle basename="org.wso2.carbon.identity.application.authentication.endpoint.i18n.Resources">

    <html lang="en">
    <head>
        <meta charset="utf-8">
        <title>Login with WSO2 Identity Server</title>
        <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="description" content="">
        <meta name="author" content="">

        <!-- Le styles -->
        <link href="/authenticationendpoint/assets/css/bootstrap.min.css" rel="stylesheet">
        <link href="/authenticationendpoint/css/localstyles.css" rel="stylesheet">
        <!--[if lt IE 8]>
        <link href="css/localstyles-ie7.css" rel="stylesheet">
        <![endif]-->

        <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
        <!--[if lt IE 9]>
        <script src="assets/js/html5.js"></script>
        <![endif]-->
        <script src="/authenticationendpoint/assets/js/jquery-1.7.1.min.js"></script>
        <script src="/authenticationendpoint/js/scripts.js"></script>


    </head>

    <body>

    <div class="header-strip">&nbsp;</div>
    <div class="header-back">
        <div class="container">
            <div class="row">
                <div class="span4 offset3">
                    <a class="logo">&nbsp</a>
                </div>
            </div>
        </div>
    </div>

    <div class="header-text">
        <strong>Please login to continue</strong>
    </div>

    <!-- container -->
    <%@ page import="java.util.Map" %>
    <%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.CharacterEncoder" %>
    <%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.Constants" %>

    <%
        String queryString = request.getQueryString();
        Map<String, String> idpAuthenticatorMapping = null;
        if (request.getAttribute("idpAuthenticatorMap") != null) {
            idpAuthenticatorMapping = (Map<String, String>)request.getAttribute("idpAuthenticatorMap");
        }
        String errorMessage = "login.fail.message";
        String loginFailed = "false";
        if (request.getParameter(Constants.AUTH_FAILURE) != null &&
                "true".equals(request.getParameter(Constants.AUTH_FAILURE))) {
            loginFailed = "true";
            if(request.getParameter(Constants.AUTH_FAILURE_MSG) != null){
                errorMessage = CharacterEncoder.getSafeText(request.getParameter(Constants.AUTH_FAILURE_MSG));
            }
        }
    %>

    <script type="text/javascript">
        function doLogin() {
            var loginForm = document.getElementById('loginForm');
            loginForm.submit();
        }
    </script>

    <div class="container" style="margin-top:10px;">

        <form action="../../commonauth" method="post" id="loginForm" class="form-horizontal" >
            <%
                if(idpAuthenticatorMapping.containsKey("LOCAL")) {

                    if(idpAuthenticatorMapping.get("LOCAL") != null &&
                            idpAuthenticatorMapping.get("LOCAL").equals("OpenIDAuthenticator")){
            %>

            <div class="row">
                <div class="span6">
                    <div class="well">
                        <%@ include file="openid.jsp" %>
                    </div>
                </div>
            </div>

            <%
            } else if(idpAuthenticatorMapping.get("LOCAL") != null && idpAuthenticatorMapping.get("LOCAL").equals("BasicAuthenticator")) {
            %>

            <div class="row">
                <div class="span6">
                    <div class="well">
                        <%@ include file="basicauth.jsp" %>
                    </div>
                </div>
            </div>

            <%
            } else if(idpAuthenticatorMapping.get("LOCAL") != null){
            %>

            <div class="row">
                <div class="span6">
                    <div class="well">
                        <%@ include file="basicauth.jsp" %>
                    </div>
                </div>
                <div class="span6">
                    <div class="well">
                        <%@ include file="openid.jsp" %>
                    </div>
                </div>
            </div>

            <%
                }
            } else {
            %>
            <div class="row">
                <div class="span6">
                    <div class="well">
                        <%@ include file="basicauth.jsp" %>
                    </div>
                </div>
            </div>
            <%
                }
            %>
            <div class="row">
                <div class="span12">
                    <%
                        if (idpAuthenticatorMapping != null && idpAuthenticatorMapping.size() != 0 &&
                                (idpAuthenticatorMapping.size() != 1 || !idpAuthenticatorMapping.containsKey("LOCAL"))) {
                    %>
                    Login with :
                    <%
                        for (Map.Entry<String, String> idpEntry : idpAuthenticatorMapping.entrySet())  {
                            if(!idpEntry.getKey().equals("LOCAL") && !idpEntry.getKey().equals("internal")) {
                    %>
                    <a href="../../commonauth?idp=<%=idpEntry.getKey()%>&authenticator=<%=idpEntry.getValue()%>&sessionDataKey=<%=request.getParameter("sessionDataKey")%>"><%=idpEntry.getKey()%></a>
                    <%
                                }
                            }
                        }
                    %>
                </div>
            </div>
        </form>
    </div>

    </body>
    </html>

</fmt:bundle>
