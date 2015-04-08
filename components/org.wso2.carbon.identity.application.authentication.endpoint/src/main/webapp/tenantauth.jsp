<%@ page
        import="org.wso2.carbon.identity.application.authentication.endpoint.util.TenantDataManager" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.CharacterEncoder"%>

<style type="text/css">
    select.input-xlarge {
        width: 280px;
    }

    input.input-xlarge {
        width: 270px;
    }
</style>

<div id="loginTable1" class="identity-box">
    <%
        loginFailed = CharacterEncoder.getSafeText(request.getParameter("loginFailed"));
        if (loginFailed != null) {

    %>

    <div class="alert alert-error">
        <fmt:message key='<%=CharacterEncoder.getSafeText(request.getParameter("errorMessage"))%>'/>
    </div>
    <% } %>

    <% if (CharacterEncoder.getSafeText(request.getParameter("username")) == null ||
            "".equals(CharacterEncoder.getSafeText(request.getParameter("username")).trim())) { %>

    <!--tenant list dropdown-->
    <div class="control-group">
        <label class="control-label" for="tenantList"><fmt:message key='tenantListLabel'/>:</label>

        <div class="controls">
            <select class="input-xlarge" id='tenantList' name="tenantList" size='1'>
                <option value="<fmt:message key='select.tenant.dropdown.display.name'/>"><fmt:message
                        key='select.tenant.dropdown.display.name'/></option>
                <option value="<fmt:message key='super.tenant'/>"><fmt:message
                        key='super.tenant.display.name'/></option>
                <%
                    List<String> tenantDomainsList = TenantDataManager.getAllActiveTenantDomains();
                    if (tenantDomainsList != null) {
                        for (String tenant : tenantDomainsList) {
                %>
                <option value="<%=tenant%>"><%=tenant%>
                </option>
                <%
                        }
                    }
                %>
            </select>
        </div>
    </div>

    <!-- Username -->
    <input type="hidden" id='username' name='username'/>

    <div class="control-group">
        <label class="control-label" for="username_tmp"><fmt:message key='username'/>:</label>

        <div class="controls">
            <input class="input-xlarge" type="text" id='username_tmp' name="username_tmp" size='30'/>
        </div>
    </div>


    <%} else { %>

    <input type="hidden" id='username' name='username'
           value='<%=CharacterEncoder.getSafeText(request.getParameter("username"))%>'/>

    <% } %>

    <!--Password-->
    <div class="control-group">
        <label class="control-label" for="password"><fmt:message key='password'/>:</label>

        <div class="controls">
            <input type="password" id='password' name="password" class="input-xlarge" size='30'/>
            <input type="hidden" name="sessionDataKey"
                   value='<%=CharacterEncoder.getSafeText(request.getParameter("sessionDataKey"))%>'/>
            <label class="checkbox" style="margin-top:10px"><input type="checkbox" id="chkRemember"
                                                                   name="chkRemember"><fmt:message
                    key='remember.me'/></label>
        </div>
    </div>

    <script>
        function appendTenantDomain() {

            var element = document.getElementById("tenantList");
            var tenantDomain = element.options[element.selectedIndex].value;

            setSelectedTenantCookie(tenantDomain, 30);

            if (tenantDomain != "<fmt:message key='select.tenant.dropdown.display.name'/>") {

                var username = document.getElementsByName("username_tmp")[0].value;
                var userWithDomain = username + "@" + tenantDomain;

                document.getElementsByName("username")[0].value = userWithDomain;
            }
        }

        function setSelectedTenantCookie(cvalue, exdays) {
            var date = new Date();
            date.setTime(date.getTime() + (exdays * 24 * 60 * 60 * 1000));
            var expires = "expires=" + date.toUTCString();
            document.cookie = "selectedTenantDomain=" + cvalue + "; " + expires;
        }

        function getSelectedTenantCookie() {
            var name = "selectedTenantDomain=";
            var cookieItems = document.cookie.split(';');
            for (var i = 0; i < cookieItems.length; i++) {
                var item = cookieItems[i];
                while (item.charAt(0) == ' ') item = item.substring(1);
                if (item.indexOf(name) != -1) return item.substring(name.length, item.length);
            }
            return "";
        }

        function selectTenantFromCookie() {
            var tenant = getSelectedTenantCookie();

            var element = document.getElementById("tenantList");

            for (var i = 0; i < element.options.length; i++) {
                if (element.options[i].value == tenant) {
                    element.value = tenant;
                    break;
                }
            }

            //remove super tenant from dropdown based on the properties
            var superTenant = "<fmt:message key='super.tenant'/>";
            if(superTenant == null || superTenant == ""){
                for (i=0;i<element.options.length;  i++) {
                    if (element.options[i].value==superTenant) {
                        element.remove(i);
                        break;
                    }
                }
            }
        }
    </script>

    <div class="form-actions">
        <input type="submit" value='<fmt:message key='login'/>' class="btn btn-primary"
               onclick="appendTenantDomain();">
    </div>

</div>

