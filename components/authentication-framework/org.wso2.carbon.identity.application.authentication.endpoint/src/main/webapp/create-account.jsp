<%@ page import="org.owasp.encoder.Encode" %>
<!--
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
-->

<fmt:bundle basename="org.wso2.carbon.identity.application.authentication.endpoint.i18n.Resources">
    <html>
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>WSO2 Identity Server</title>

        <link rel="icon" href="images/favicon.png" type="image/x-icon"/>
        <link href="libs/bootstrap_3.3.5/css/bootstrap.min.css" rel="stylesheet">
        <link href="css/Roboto.css" rel="stylesheet">
        <link href="css/custom-common.css" rel="stylesheet">

        <!--[if lt IE 9]>
        <script src="js/html5shiv.min.js"></script>
        <script src="js/respond.min.js"></script>
        <![endif]-->
    </head>

    <body>

    <!-- header -->
    <header class="header header-default">
        <div class="container-fluid">
            <div class="pull-left brand float-remove-xs text-center-xs">
                <a href="#">
                    <img src="images/logo-inverse.svg" alt="wso2" title="wso2" class="logo">

                    <h1><em>Identity Server</em></h1>
                </a>
            </div>
        </div>
    </header>

    <!-- page content -->
    <div class="container-fluid body-wrapper">

        <div class="row">
            <!-- content -->
            <div class="col-xs-12 col-sm-10 col-md-8 col-lg-5 col-centered wr-login">
                <form action="registration.do" method="post" id="register">
                    <h2 class="wr-title uppercase blue-bg padding-double white boarder-bottom-blue margin-none">Create
                        An Account</h2>

                    <div class="clearfix"></div>
                    <div class="boarder-all ">

                        <% if (request.getParameter("failedPrevious") != null &&
                            "true".equals(request.getParameter("failedPrevious"))) { %>
                        <div class="alert alert-error" id="error-msg">Something went wrong. Please try again.
                        </div>
                        <%}%>

                        <div class="padding-double font-large">Enter all text fields to complete registration</div>
                        <!-- validation -->
                        <div class="padding-double">
                            <div id="regFormError" class="alert alert-error" style="display:none"></div>
                            <div id="regFormSuc" class="alert alert-success" style="display:none"></div>

                            <div class="col-xs-12 col-sm-12 col-md-6 col-lg-6 form-group">
                                <label>First Name</label>
                                <input type="text" name="reg-first-name"
                                       data-claim-uri="http://wso2.org/claims/givenname"
                                       class="form-control  required null">
                            </div>

                            <div class="col-xs-12 col-sm-12 col-md-6 col-lg-6 form-group">
                                <label>Last Name</label>
                                <input type="text" name="reg-last-name" data-claim-uri="http://wso2.org/claims/lastname"
                                       class="form-control  required null">
                            </div>

                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                                <label>Username</label>
                                <input id="reg-username" name="reg-username" type="text"
                                       class="form-control required usrName usrNameLength">
                            </div>
                            <div class="col-xs-12 col-sm-12 col-md-6 col-lg-6 form-group">
                                <label>Password</label>
                                <input id="reg-password" name="reg-password" type="password"
                                       class="form-control required passwordLength">
                            </div>
                            <div class="col-xs-12 col-sm-12 col-md-6 col-lg-6 form-group">
                                <label>Confirm password</label>
                                <input id="reg-password2" name="password2" type="password" class="form-control required"
                                       equalto="#reg-password">
                            </div>


                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                                <label>Email</label>
                                <input type="text" name="reg-email" data-claim-uri="http://wso2.org/claims/emailaddress"
                                       class="form-control email required null">
                            </div>

                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                                <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute
            (request.getParameter("sessionDataKey"))%>'/>
                            </div>

                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                                <br><br>
                                <button id="registrationSubmit"
                                        class="wr-btn grey-bg col-xs-12 col-md-12 col-lg-12 uppercase font-extra-large"
                                        type="submit">Register
                                </button>
                            </div>
                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                                <span class="margin-top padding-top-double font-large">Already have an account? </span>
                                <a href="login.jsp" id="signInLink" class="font-large">Sign in</a>
                            </div>
                            <div class="clearfix"></div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
        <!-- /content/body -->

    </div>

    <!-- footer -->
    <footer class="footer">
        <div class="container-fluid">
            <p>WSO2 Identity Server | &copy;
                <script>document.write(new Date().getFullYear());</script>
                <a href="http://wso2.com/" target="_blank"><i class="icon fw fw-wso2"></i> Inc</a>. All Rights Reserved.
            </p>
        </div>
    </footer>

    <script src="libs/jquery_1.11.0/jquery-1.11.3.min.js"></script>
    <script src="libs/bootstrap_3.3.5/js/bootstrap.min.js"></script>
    </body>
    </html>
</fmt:bundle>

