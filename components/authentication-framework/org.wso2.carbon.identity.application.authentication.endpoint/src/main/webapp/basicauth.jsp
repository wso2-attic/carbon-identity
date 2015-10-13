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

<%@ page import="org.owasp.encoder.Encode" %>

<form action="../commonauth" method="post" id="loginForm" class="form-horizontal">

    <% if ("true".equals(loginFailed)) { %>
    <div class="alert alert-error" id="error-msg">Username or password is
        invalid
    </div>
    <%}%>

    <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
        <input id="username" name="username" type="text" class="form-control" tabindex="0"
               placeholder="Username">
    </div>
    <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
        <input id="password" name="password" type="password" class="form-control"
               placeholder="Password">
    </div>
    <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
        <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute
            (request.getParameter("sessionDataKey"))%>'/>
    </div>

    <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
        <div class="checkbox">
            <label>
                <input type="checkbox"> Remember me on this computer
            </label>
        </div>
        <br>

        <div class="form-actions">
            <button
                    class="wr-btn grey-bg col-xs-12 col-md-12 col-lg-12 uppercase font-extra-large"
                    type="submit">Sign in
            </button>
        </div>
    </div>
    <input type="hidden" name="sessionDataKey" value="0ac33a12-943a-459f-9eb4-06055d264d45">
    <input type="hidden" name="RelayState" value="/store/pages/top-assets">

    <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">


        <a id="registerLink" href="create-account.jsp" class="font-large">Create an
            account</a>

    </div>
    <div class="clearfix"></div>
</form>

