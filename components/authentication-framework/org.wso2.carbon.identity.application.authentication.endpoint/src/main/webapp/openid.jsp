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

    <div class="control-group">
        <label class="control-label" for="claimed_id">OpenID:</label>

        <div class="controls">
            <input class="input-large" type="text" id="claimed_id" name="claimed_id" size='30'/>
            <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute(request.getParameter("sessionDataKey"))%>'/>
        </div>
    </div>

    <div class="form-actions">
        <input type="submit" value='<fmt:message key='login'/>' class="btn btn-primary">
    </div>

</div>


