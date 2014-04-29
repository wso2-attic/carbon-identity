<div id="loginTable1" class="identity-box">
    <%
        loginFailed = request.getParameter("loginFailed");
        if (loginFailed != null) {

    %>
            <div class="alert alert-error">
                <fmt:message key='<%=request.getParameter("errorMessage")%>'/>
            </div>
    <% } %>

    <% if (request.getParameter("username") == null || "".equals(request.getParameter("username").trim())) { %>

        <!-- Username -->
        <div class="control-group">
            <label class="control-label" for="username"><fmt:message key='username'/>:</label>

            <div class="controls">
                <input class="input-xlarge" type="text" id='username' name="username" size='30'/>
            </div>
        </div>

    <%} else { %>

        <input type="hidden" id='username' name='username' value='<%=request.getParameter("username")%>'/>

    <% } %>

    <!--Password-->
    <div class="control-group">
        <label class="control-label" for="password"><fmt:message key='password'/>:</label>

        <div class="controls">
            <input type="password" id='password' name="password"  class="input-xlarge" size='30'/>
            <input type="hidden" name="sessionDataKey" value='<%=request.getParameter("sessionDataKey")%>'/>
            <label class="checkbox" style="margin-top:10px"><input type="checkbox" id="chkRemember" name="chkRemember"><fmt:message key='remember.me'/></label>
        </div>
    </div>

    <div class="form-actions">
        <input type="submit" value='<fmt:message key='login'/>' class="btn btn-primary">
    </div>

</div>

