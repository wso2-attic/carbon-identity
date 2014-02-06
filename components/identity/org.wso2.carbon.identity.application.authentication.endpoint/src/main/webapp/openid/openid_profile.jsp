<!DOCTYPE html>
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
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
           
<%
String selectedProfile = request.getParameter("selectedProfile");
String[] profiles = request.getParameterValues("profile");
String[] claimTags = request.getParameterValues("claimTag");
String[] claimValues = request.getParameterValues("claimValue");
%>

<fmt:bundle basename="org.wso2.carbon.identity.application.authentication.endpoint.i18n.openid.Resources">
<html lang="en">
    <head>
        <meta charset="utf-8">
        <title>OpenID2.0 Profile</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="description" content="">
        <meta name="author" content="">

        <!-- Le styles -->
        <link href="openid/assets/css/bootstrap.min.css" rel="stylesheet">
        <link href="openid/css/localstyles.css" rel="stylesheet">
        <!--[if lt IE 8]>
        <link href="css/localstyles-ie7.css" rel="stylesheet">
        <![endif]-->

        <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
        <!--[if lt IE 9]>
        <script src="openid/assets/js/html5.js"></script>
        <![endif]-->
        <script src="openid/assets/js/jquery-1.7.1.min.js"></script>
        <link media="all" type="text/css" rel="stylesheet" href="css/openid-provider.css">

           <script type="text/javascript">
               function submitProfileSelection() {
                   document.profileSelection.submit();
               }

               function approved() {
                   document.getElementById("hasApprovedAlways").value = "false";
                   document.profile.submit();
               }

               function approvedAlways() {
                   document.getElementById("hasApprovedAlways").value = "true";
                   document.profile.submit();
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
        <!-- Black strip you want to display a message or something.. -->
    </div>

    <div class="container">
        <div class="row">
            <div class="span6 offset3 content-section">
                <h3 style="text-align: center">OpenID User Profile</h3>
	                <div class="well">
			            <form name="profileSelection" action="openid_profile_view.jsp" method="POST"  class="form-horizontal">
                            <div class="control-group">
                               <label class="control-label" for="oauth_user_name"><fmt:message key='profile'/>:</label>

                               <div class="controls">
                                   <select name="selectedProfile" onchange="submitProfileSelection();">
                                    <%
                                       if(profiles != null) {
                                        for (int i = 0; i < profiles.length; i++) {
                                            String profile = profiles[i];
                                            if(profile.equals(request.getParameter("selectedProfile"))){
                                            %>
                                            <option value="<%=profile%>" selected="selected"><%=profile%>
                                            </option>
                                            <%}
                                            else{
                                              %><option value="<%=profile%>"><%=profile%></option><%
                                            }
                                        }
                                     }
                                    %>
                                    </select>
                               </div>
                           </div>
			            </form>



			
			            <form action="../../openidserver" id="profile" name="profile" class="form-horizontal">
                                <div class="control-group">
                                    <div class="controls">
                                        <img src="openid/images/profile-picture.gif" align="bottom"/>
                                        <%
                                        if(claimTags != null) {
                                            for (int i = 0; i < claimTags.length; i++) {
                                                String claimTag = claimTags[i];
                                            %>
                                                <div><strong><%=claimTag%></strong></div>
                                                <div><%=claimValues[i]%></div>
                                                <br/>
                                        <%
                                            }
                                        }
                                        %>
                                    </div>
                                </div>

                            <div class="form-actions">
                                <input type="button" class="btn  btn-primary" id="approve" name="approve"
                                                 onclick="javascript: approved(); return false;"
                                                 value="<fmt:message key='approve'/>"/>
                                <input type="button" class="btn" id="chkApprovedAlways" onclick="javascript: approvedAlways();" value="<fmt:message key='approve.always'/>"/>
                                <input type="hidden" id="hasApprovedAlways" name="hasApprovedAlways" value="false" />
                                <input class="btn" type="reset" value="<fmt:message key='cancel'/>"
                                   onclick="javascript:document.location.href='../../carbon/admin/login.jsp'"/>
                            </div>
			            </form>
			
			        </div>
                </div>
            </div>
        </div>
    </body>
    </html>
</fmt:bundle>
