
<!DOCTYPE html>
<!--
~ Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	<style>
	div.different-login-container a.truncate {
	  width: 148px;
	  white-space: nowrap;
	  overflow: hidden;
	  text-overflow: ellipsis;
	}
	</style>

    </head>

    <%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.Constants" %>

    <%
        String errorMessage = "Authentication Failed! Please Retry";
        String loginFailed = "false";
        if (request.getParameter(Constants.AUTH_FAILURE) != null &&
                "true".equals(request.getParameter(Constants.AUTH_FAILURE))) {
            loginFailed = "true";

            if(request.getParameter(Constants.AUTH_FAILURE_MSG) != null){
                errorMessage = (String) request.getParameter(Constants.AUTH_FAILURE_MSG);

                if (errorMessage.equalsIgnoreCase("login.fail.message")) {
                    errorMessage = "Authentication Failed! Please Retry";
                }
            }
        }
    %>

    <body>
    <div class="overlay" style="display:none"></div>
    <div class="header-strip">&nbsp;</div>
    <div class="header-back">
        <div class="container">
            <div class="row">
                <div class="span12">
                    <a class="logo">&nbsp</a>
                </div>
            </div>
        </div>
    </div>

    <div class="header-text">

    </div>
    <div class="container">
	<div class="row">
		<div class="span12">
			<h1>Enter the TOTP to continue</h1>
		</div>
	</div>
    </div>

    <div id="local_auth_div" class="container different-login-container" style="margin-top:10px;">

        <% if ("true".equals(loginFailed)) { %>
                    <div class="alert alert-error">
                        <%=errorMessage%>
                    </div>
        <% } %>
        <input id="username" type="hidden" value='<%=request.getParameter("username")%>'>
        <table>
            <tr>
                <td>
                    <div>
                        <form action="../../commonauth" method="post" id="totpForm" class="form-horizontal">
                            <div>TOTP Token : <input type="text" name="token"></div>
                            <div><input id="sessionDataKey" type="hidden" name="sessionDataKey" value='<%=request.getParameter("sessionDataKey")%>' /></div>
                            <div style="align-text:center;padding-top:10px"><input type="submit" value="Submit"></div>
                        </form>
                    </div>
                </td>
                <td style="vertical-align:top">
                    <div style="margin-left:20px">
                        <input type="button" value="Generate TOTP" onclick="requestTOTPToken()">
                    </div>
                </td>
            </tr>
        </table>
	</div>


    <script>

 	$(document).ready(function(){
		$('.main-link').click(function(){
			$('.main-link').next().hide();
			$(this).next().toggle('fast');
			var w = $(document).width();
			var h = $(document).height();
			$('.overlay').css("width",w+"px").css("height",h+"px").show();
		});
		$('.overlay').click(function(){$(this).hide();$('.main-link').next().hide();});

	    });
    </script>

    </body>
</html>



