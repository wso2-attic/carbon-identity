<!DOCTYPE html>
<!--
~ Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<html lang="en">
<head>
    <%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
    <%
		String queryString = request.getQueryString();
        String openidrealm = request.getParameter("openid.realm");
        String openidreturnto = request.getParameter("openid.return_to");
        String openidclaimedid = request.getParameter("openid.claimed_id");
        String openididentity = request.getParameter("openid.identity");
        String userName = request.getParameter("username");
        String errorMsg = request.getParameter("errorMsg");

        String openidrp  = openidreturnto;
        if (openidrp!=null && openidreturnto.indexOf("?")>0){
            openidrp = openidreturnto.substring(0,openidreturnto.indexOf("?"));
        }
     %>
    <fmt:bundle  basename="org.wso2.carbon.identity.application.authentication.endpoint.i18n.openid.Resources">
    <meta charset="utf-8">
    <title>OpenID2.0 Login</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">
    <style>
        .identity-box{
        	height:200px;
        }
    </style>
    <!-- Le styles -->
    <link href="/authenticationendpoint/openid/assets/css/bootstrap.min.css" rel="stylesheet">
    <link href="/authenticationendpoint/openid/css/localstyles.css" rel="stylesheet">
    <!--[if lt IE 8]>
    <link href="openid/css/localstyles-ie7.css" rel="stylesheet">
    <![endif]-->

    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="openid/assets/js/html5.js"></script>
    <![endif]-->
    <script src="/authenticationendpoint/openid/assets/js/jquery-1.7.1.min.js"></script>
    <script type="text/javascript">
        function doLogin(){
            var loginForm = document.getElementById('loginForm');

            var loadingImg = document.getElementById('loadingImg');
            var loginTable = document.getElementById('loginTable');

            loginTable.style.display = "none";
            loadingImg.style.display = "";
            loginForm.submit();
        }
    </script>
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
		<fmt:message key='signin.to.authenticate1'/><strong>"<%=openidrp%>" </strong><fmt:message key='signin.to.authenticate2'/><%if(!openididentity.endsWith("/openid/")){%><strong> "<%=openididentity%>"</strong><% } else { %><strong> "<%=openididentity%>&lt;username&gt;"</strong><% } %>.
	</div>

	<div class="container">
		<div class="row">
			<div class="span6 offset3 content-section well" style="margin-left:230px">
				<div id="loginTable">
				<form action="../../commonauth" method="post" id="loginForm" onsubmit="doLogin()" class="form-horizontal">
					<input type="hidden" id='openid' name='openid' value="<%=openididentity%>"/>

					<% if (errorMsg != null) { %>
					<div class="alert alert-error">
						<fmt:message key='<%=errorMsg%>'/>
					</div>
					<% } 
					
				   String loginType = "basicauth";
				   
				   if (request.getParameter("loginType") != null) {
					   loginType = request.getParameter("loginType");
				   }
					   
				   if (loginType.equals("basicauth") ) {%>
					   <%@ include file="../authenticator-pages/basicauth.jsp" %>
					<%} else if (loginType.equals("openid")) { %>
					   <%@ include file="../authenticator-pages/openid.jsp" %>
					<%}%>

					<!--div style="margin-top:20px;margin-bottom:20px;text-align:center">
					Login with :
						<a href="/authenticationendpoint/openid/openid_login.jsp?loginType=basicauth&<%=queryString%>">Username/Password</a> &nbsp; | &nbsp;
						<a href="/authenticationendpoint/openid/openid_login.jsp?loginType=openid&<%=queryString%>">OpenID</a> &nbsp; | &nbsp;
						<a href="../../commonauth?loginType=samlsso">SAMLSSO</a>
					</div-->
				</form>
				</div>

				<div style="display:none" id="loadingImg">
					<form action="openid_auth_submit.jsp" method="post" id="loginForm" onsubmit="doLogin()">
						<input type="hidden" id='openid' name='openid' value="<%=openididentity%>"/>

						<div id="loginbox" class="identity-box" style="padding-left: 20px">
							<strong id="loginDisplayText"><fmt:message key='authenticating.please.wait'/></strong>

							<h2></h2>
							<div style="padding-left:30px; padding-top:25px;">
								<img src="openid/images/ajax-loader.gif" vspace="20" />
							</div>

						</div>
					</form>
				</div>
			</div>
		</div>
	</div>

</body>
</html>
</fmt:bundle>
