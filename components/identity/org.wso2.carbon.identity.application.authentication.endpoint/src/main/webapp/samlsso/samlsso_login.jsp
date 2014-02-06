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
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.samlsso.SAMLSSOConstants" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<fmt:bundle basename="org.wso2.carbon.identity.application.authentication.endpoint.i18n.samlsso.Resources">
    <%
    
		String queryString = request.getQueryString();
    
        String errorMessage = "login.fail.message";
        boolean loginFailed = false;
        if (request.getParameter(SAMLSSOConstants.AUTH_FAILURE) != null &&
            "true".equals(request.getParameter(SAMLSSOConstants.AUTH_FAILURE))) {
            loginFailed = true;
            if(request.getParameter(SAMLSSOConstants.AUTH_FAILURE_MSG) != null){
                errorMessage = (String) request.getParameter(SAMLSSOConstants.AUTH_FAILURE_MSG);
            }
        }
    %>

    <script type="text/javascript">
        function doLogin() {
            var loginForm = document.getElementById('loginForm');
            loginForm.submit();
        }
    </script>

<html lang="en">
    <head>
        <meta charset="utf-8">
        <title>SAML2.0 Web SSO</title>
        <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="description" content="">
        <meta name="author" content="">

        <!-- Le styles -->
        <link href="/authenticationendpoint/samlsso/assets/css/bootstrap.min.css" rel="stylesheet">
        <link href="/authenticationendpoint/samlsso/css/localstyles.css" rel="stylesheet">
        <!--[if lt IE 8]>
        <link href="samlsso/css/localstyles-ie7.css" rel="stylesheet">
        <![endif]-->

        <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
        <!--[if lt IE 9]>
        <script src="samlsso/assets/js/html5.js"></script>
        <![endif]-->
        <script src="/authenticationendpoint/samlsso/assets/js/jquery-1.7.1.min.js"></script>
        <script src="/authenticationendpoint/samlsso/js/scripts.js"></script>


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
            <!-- put the info you want to display in the black strip here -->
    </div>

    <div class="container">
        <div class="row">
            <div class="span6 offset3 content-section" style="margin-left:230px">
				<form action="../../commonauth" method="post" id="loginForm" class="well form-horizontal" >
				<div id="loginTable1" class="identity-box">
					<% if (loginFailed) { %>
						<div class="alert alert-error">
							<fmt:message key='<%=errorMessage%>'/>
						</div>	
					<% } 
				   String loginType = "basicauth";
				   
				   if (request.getParameter("loginType") != null) {
					   loginType = request.getParameter("loginType");
				   }
					   
				   if (loginType.equals("basicauth") ) {%>
					   <%@ include file="../authenticator-pages/basicauth.jsp" %>
					<%} else if (loginType.equals("openid")) { %>
					   <%@ include	 file="../authenticator-pages/openid.jsp" %>
					<%}%>
					
					</div>
					<!--div style="margin-top:20px;margin-bottom:20px;text-align:center">
					Login with : 
						<a href="/authenticationendpoint/samlsso/samlsso_login.jsp?loginType=basicauth&<%=queryString%>">Username/Password</a> &nbsp; | &nbsp;
						<a href="/authenticationendpoint/samlsso/samlsso_login.jsp?loginType=openid&<%=queryString%>">OpenID</a> &nbsp; | &nbsp;
                        <a href="../../commonauth?loginType=samlsso&sessionDataKey=<%=request.getParameter("sessionDataKey")%>">SAMLSSO</a>
					</div-->
				</div>
			</form>			
            </div>
        </div>
    </div>
    <!-- /container -->
    
    </body>
    </html>

</fmt:bundle>
