<!DOCTYPE html>
<!--
~ Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@page import="org.wso2.carbon.identity.application.authentication.endpoint.oauth2.OAuth2Login"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	String errorCode = OAuth2Login.getSafeText(request.getParameter("oauthErrorCode"));
	String errorMsg = OAuth2Login.getSafeText(request.getParameter("oauthErrorMsg"));
%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>OAuth2.0 Error</title>
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="">
<meta name="author" content="">

<!-- Le styles -->
<link href="assets/css/bootstrap.min.css" rel="stylesheet">
<link href="css/localstyles.css" rel="stylesheet">
<!--[if lt IE 8]>
    <link href="css/localstyles-ie7.css" rel="stylesheet">
    <![endif]-->

<!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
<!--[if lt IE 9]>
    <script src="assets/js/html5.js"></script>
    <![endif]-->
<script src="assets/js/jquery-1.7.1.min.js"></script>
<script src="js/scripts.js"></script>


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

	<div class="container">
		<div class="row">
			<div class="span5 offset3 content-section">
				<fmt:bundle
					basename="org.wso2.carbon.identity.oauth.ui.i18n.Resources">
					<div id="middle">
						<h2>
							<fmt:message key='oauth.processing.error' />
						</h2>

						<div id="workArea">
							<table>
								<%
									if (errorCode != null && errorMsg != null) {
								%>
								<tr>
									<td><b><%=errorCode%> </b></td>
								</tr>
								<tr>
									<td><%=errorMsg%></td>
								</tr>
								<%
									} else {
								%>
								<tr>
									<td><fmt:message key='oauth.processing.error.msg' /></td>
								</tr>
								<%
									}
								%>
							</table>
						</div>
					</div>
				</fmt:bundle>

			</div>
		</div>
	</div>
	<!-- /container -->


</body>
</html>

