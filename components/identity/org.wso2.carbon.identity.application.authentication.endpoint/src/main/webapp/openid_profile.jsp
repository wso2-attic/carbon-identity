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
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
           
<%
    String[] profiles = request.getParameterValues("profile");
    String[] claimTags = request.getParameterValues("claimTag");
    String[] claimValues = request.getParameterValues("claimValue");
    String openidreturnto = request.getParameter("openid.return_to");
    String openididentity = request.getParameter("openid.identity");
    if (openidreturnto!=null && openidreturnto.indexOf("?") > 0){
        openidreturnto = openidreturnto.substring(0,openidreturnto.indexOf("?"));
    }
%>

<fmt:bundle basename="org.wso2.carbon.identity.application.authentication.endpoint.i18n.Resources">
<html lang="en">
    <head>
        <meta charset="utf-8">
        <title>OpenID2.0 Profile</title>
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
	            	<fmt:message key='signin.to.authenticate1'/>  <strong>"<%=openidreturnto%>" </strong><fmt:message key='signin.to.authenticate2'/><%if(!openididentity.endsWith("/openid/")){%><strong> "<%=openididentity%>"</strong><% } else { %><strong> "<%=openididentity%>&lt;username&gt;"</strong><% } %>.
	        	</div>
	        </div>
	   </div>
    </div>

    <div class="container main-login-container" style="margin-top:10px;">
        <div class="row">
            <div class="span12 content-section">
                <h3 style="text-align:left;margin-bottom:10px;">OpenID User Claims</h3>
	                <div>
			            <form action="../../openidserver" id="profile" name="profile" class="form-horizontal">
                                <div class="control-group">
                                    <div class="controls" style="margin-left: 0px !important;">

                                        <%
                                        if(claimTags != null && claimTags.length > 0 ) { %>
                                            <table class="table table-striped table-bordered">
                                            <tr>
                                                <th>Claim URI</th>
                                                <th>Claim Value</th>
                                            </tr>
                                            <%for (int i = 0; i < claimTags.length; i++) {
                                                String claimTag = claimTags[i];
                                            %>
                                                <tr><td><%=claimTag%></td><td><%=claimValues[i]%></td></tr>
                                            <%
                                            } %>
                                            </table>
                                            <%
                                        }
                                        %>
                                    </div>
                                </div>

                            <div style="text-align:left;">
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
