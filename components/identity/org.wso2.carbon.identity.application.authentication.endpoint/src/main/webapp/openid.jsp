<div id="loginTable1" class="identity-box">
    <%
        loginFailed = request.getParameter("loginFailed");
        if (loginFailed != null) {

    %>
    <div class="alert alert-error">
        <fmt:message key='<%=request.getParameter("errorMessage")%>'/>
    </div>
    <% } %>

    <div class="control-group">
        <label class="control-label" for="claimed_id">OpenID:</label>

        <div class="controls">
            <input class="input-large" type="text" id="claimed_id" name="claimed_id" size='30'/>
            <input type="hidden" name="sessionDataKey" value='<%=request.getParameter("sessionDataKey")%>'/>
        </div>
    </div>

    <div class="form-actions">
        <input type="submit" value='<fmt:message key='login'/>' class="btn btn-primary">
    </div>

</div>


