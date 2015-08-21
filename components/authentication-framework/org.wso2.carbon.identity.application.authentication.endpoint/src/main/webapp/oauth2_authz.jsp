<%--
  ~ Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.Constants" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.CharacterEncoder" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
    String loggedInUser = CharacterEncoder.getSafeText(request.getParameter("loggedInUser"));
    String scopeString = CharacterEncoder.getSafeText(request.getParameter("scope"));
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>WSO2 Identity Server OAuth2 Consent</title>
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
            <div class="span12">
                <a class="logo">&nbsp</a>
            </div>
        </div>
    </div>
</div>
<div class="header-text">
    <div class="container">
        <div class="row">
            <div class="span12 content-section">
                You are logged in as <strong><%=loggedInUser%>
            </strong>.
                <strong>
                    <%=CharacterEncoder.getSafeText(request.getParameter("application"))%>
                </strong> requests access to your profile information
            </div>
        </div>
    </div>
</div>

<div class="container main-login-container" style="margin-top:10px;">
    <div class="row">
        <div class="span12 content-section">
            <script type="text/javascript">
                function approved() {
                    document.getElementById('consent').value = "approve";
                    document.getElementById("oauth2_authz").submit();
                }
                function approvedAlways() {
                    document.getElementById('consent').value = "approveAlways";
                    document.getElementById("oauth2_authz").submit();
                }
                function deny() {
                    document.getElementById('consent').value = "deny";
                    document.getElementById("oauth2_authz").submit();
                }
            </script>

            <form id="oauth2_authz" name="oauth2_authz" method="post" action="../oauth2/authorize">
                <table width="100%" class="styledLeft">
                    <tbody>
                    <tr>
                        <td class="buttonRow" colspan="2">
                            <input type="button" class="btn btn-primary btn-large" id="approve" name="approve"
                                   onclick="javascript: approved(); return false;"
                                   value="Approve"/>
                            <input type="button" class="btn btn-primary btn-large" id="approveAlways"
                                   name="approveAlways"
                                   onclick="javascript: approvedAlways(); return false;"
                                   value="Approve Always"/>
                            <input class="btn btn-primary-deny btn-large" type="reset"
                                   value="Deny" onclick="javascript: deny(); return false;"/>

                            <input type="hidden" name="<%=Constants.SESSION_DATA_KEY_CONSENT%>"
                                   value="<%=CharacterEncoder.getSafeText(request
                                       .getParameter(Constants.SESSION_DATA_KEY_CONSENT))%>"/>
                            <input type="hidden" name="consent" id="consent"
                                   value="deny"/>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </form>
        </div>
    </div>
</div>

</body>
</html>

