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

<div id="loginTable1" class="identity-box">
    <%
        loginFailed = request.getParameter("loginFailed");
        if (loginFailed != null) {

    %>
    <div class="alert alert-error">
        <fmt:message key='<%=Encode.forHtml(request.getParameter("errorMessage"))%>'/>
    </div>
    <% } %>

    <% if (StringUtils.isBlank(request.getParameter("username"))) { %>

    <!-- Username -->
    <div class="control-group">
        <label class="control-label" for="username"><fmt:message key='username'/>:</label>

        <div class="controls">
            <input class="input-xlarge" type="text" id='username' name="username" style="height:20px"/>
        </div>
    </div>

    <%} else { %>

    <input type="hidden" id='username' name='username' value='<%=Encode.forHtmlAttribute
    (request.getParameter("username"))%>'/>

    <% } %>

    <!--Password-->
    <div class="control-group">
        <label class="control-label" for="password"><fmt:message key='password'/>:</label>

        <div class="controls">
            <input type="password" id='password' name="password" class="input-xlarge" style="height:20px"/>
            <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute
            (request.getParameter("sessionDataKey"))%>'/>
            <label class="checkbox" style="margin-top:10px"><input type="checkbox" id="chkRemember"
                                                                   name="chkRemember"><fmt:message
                    key='remember.me'/></label>
        </div>
    </div>

    <div class="form-actions">
        <input type="submit" value='<fmt:message key='login'/>' class="btn btn-primary">
    </div>
</div>

