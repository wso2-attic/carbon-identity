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


<%
    String errorMessage = "Authentication Failed";
    boolean loginFailed = false;
    
    if (request.getParameter("authFailure") != null && "true".equals(request.getParameter("authFailure"))) {
	    loginFailed = true;
	    
	    if(request.getParameter("authFailureMsg") != null){
	        errorMessage = (String) request.getParameter("authFailureMsg");
	        
	        if (errorMessage.equalsIgnoreCase("domain.unknown")) {
	        	errorMessage = "Domain cannot be identified! Please retry.";
	        }
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
        <title>Enter Domain</title>
        <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="description" content="">
        <meta name="author" content="">

        <!-- Le styles -->
        <link href="/authenticationendpoint/assets/css/bootstrap.min.css" rel="stylesheet">
        <link href="/authenticationendpoint/css/localstyles.css" rel="stylesheet">
        <!--[if lt IE 8]>
        <link href="samlsso/css/localstyles-ie7.css" rel="stylesheet">
        <![endif]-->

        <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
        <!--[if lt IE 9]>
        <script src="samlsso/assets/js/html5.js"></script>
        <![endif]-->
        <script src="/authenticationendpoint/realm-disc/assets/js/jquery-1.7.1.min.js"></script>
        <script src="/authenticationendpoint/realm-disc/js/scripts.js"></script>


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
            <!-- put the info you want to display in the black strip here -->
    </div>
     <div class="container">
	<div class="row">
		<div class="span12">
			<h1>Enter your domain</h1>
		</div>
	</div>
    </div>




    <div class="container main-login-container" style="margin-top:10px;">
	<div class="row">
                <div class="span12">
<form action="../../commonauth" method="post" id="loginForm" class="form-horizontal" >
				<div id="loginTable1" class="identity-box">
					<div class="control-group">
					   <% if (loginFailed) { %>
                          <div class="alert alert-error">
                              <%=errorMessage%>
                          </div>  
                       <% } %>
						<label class="control-label" for="fidp">Domain:</label>

						<div class="controls">
							<input class="input-large" type="text" id="fidp" name="fidp" size="15"/>
							<input type="hidden" name="sessionDataKey" value='<%=request.getParameter("sessionDataKey")%>'/>
						</div>
					</div>

					<div class="form-actions">
						<input type="submit" value="Submit" class="btn btn-primary">
					</div>
					</div>
				</div>
			</form>			
		</div>
	</div>
    </div>

    <!-- /container -->
    
    </body>
</html>

