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
<%@page import="org.wso2.carbon.identity.application.authentication.endpoint.oauth2.OAuth2Login"%>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.oauth2.OAuth2Constants" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	String app = OAuth2Login.getSafeText(request.getParameter("application"));
%>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>OAuth2.0 Consent</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
     <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">

    <!-- Le styles -->
    <link href="oauth2/assets/css/bootstrap.min.css" rel="stylesheet">
    <link href="oauth2/css/localstyles.css" rel="stylesheet">
    <!--[if lt IE 8]>
    <link href="oauth2/css/localstyles-ie7.css" rel="stylesheet">
    <![endif]-->

    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="oauth2/assets/js/html5.js"></script>
    <![endif]-->
    <script src="oauth2/assets/js/jquery-1.7.1.min.js"></script>
    <script src="oauth2/js/scripts.js"></script>
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

    <div id="middle">
       <div class="header-text">
    		<strong><%=app%></strong> requests access to your profile information
		</div>
        <div id="workArea"  class="container">
         <div class="row">
        <div class="span5 offset3 content-section">
            <script type="text/javascript">
                function approved() {
                	 document.getElementById('consent').value="approve";
                	 document.getElementById("profile").submit();
                }
                function approvedAlways() {
                	 document.getElementById('consent').value="approveAlways";
                	 document.getElementById("profile").submit();     	
                }  
                function deny() {
                	 document.getElementById('consent').value="deny";
                	 document.getElementById("profile").submit();
                }
            </script>

            <form id="profile" name="profile" method="post" action="../oauth2/authorize">
                <table width="100%" class="styledLeft">
                    <tbody>
                    <tr>
                        <td class="buttonRow" colspan="2">
                        	<input type="button" class="btn btn-primary btn-large" id="approve" name="approve"
                                             onclick="javascript: approved(); return false;"
                                             value="Approve"/>
                       		<input type="button" class="btn btn-primary btn-large" id="approveAlways" name="approveAlways"
                                             onclick="javascript: approvedAlways(); return false;"
                                             value="Approve Always"/>
                       		<input class="btn btn-large" type="reset"
										value="Deny" onclick="javascript: deny(); return false;" /> 
										
							<input type="hidden" name="<%=OAuth2Constants.SESSION_DATA_KEY_CONSENT%>"
										value="<%=request.getParameter(OAuth2Constants.SESSION_DATA_KEY_CONSENT)%>" />
							<input type="hidden" name="consent" id="consent"
										value="deny" />											
					   </td>
                    </tr>
                    </tbody>
                </table>
            </form>
			</div>
        </div>
    </div>
</div>
</body>
</html>

