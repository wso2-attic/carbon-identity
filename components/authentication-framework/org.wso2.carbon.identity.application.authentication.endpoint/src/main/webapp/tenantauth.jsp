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

<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.TenantDataManager" %>
<%@ page import="java.util.List" %>
<%@ page import="org.owasp.encoder.Encode" %>

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
        loginFailed = request.getParameter("loginFailed");
        if (loginFailed != null) {
    %>
    <div class="alert alert-error">
        <fmt:message key='<%=Encode.forHtml(request.getParameter("errorMessage"))%>'/>
    </div>
    <% } %>

    <% if (StringUtils.isEmpty(request.getParameter("username"))) { %>

    <!--tenant list dropdown-->
    <div class="control-group">
        <label class="control-label" for="tenantList"><fmt:message key='tenantListLabel'/>:</label>

        <div class="controls">
            <select class="input-xlarge" id='tenantList' name="tenantList" size='1'>
                <option value="<fmt:message key='select.tenant.dropdown.display.name'/>">
                    <fmt:message key='select.tenant.dropdown.display.name'/>
                </option>
                <option value="<fmt:message key='super.tenant'/>"><fmt:message key='super.tenant.display.name'/>
                </option>

                <%
                    List<String> tenantDomainsList = TenantDataManager.getAllActiveTenantDomains();
                    if (!tenantDomainsList.isEmpty()) {
                        for (String tenant : tenantDomainsList) {
                %>
                <option value="<%=Encode.forHtmlAttribute(tenant)%>"><%=Encode.forHtmlContent(tenant)%>
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
           value='<%=Encode.forHtmlAttribute(request.getParameter("username"))%>'/>

    <% } %>

    <!--Password-->
    <div class="control-group">
        <label class="control-label" for="password"><fmt:message key='password'/>:</label>

        <div class="controls">
            <input type="password" id='password' name="password" class="input-xlarge" size='30'/>
            <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute(request.getParameter("sessionDataKey"))%>'/>
            <label class="checkbox" style="margin-top:10px">
                <input type="checkbox" id="chkRemember" name="chkRemember"><fmt:message key='remember.me'/>
            </label>
        </div>
    </div>

    <script>

        /**
         * Append the tenant domain to the username
         */
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

        /**
         * Write the selected tenant domain to the cookie
         */
        function setSelectedTenantCookie(cvalue, exdays) {
            var date = new Date();
            date.setTime(date.getTime() + (exdays * 24 * 60 * 60 * 1000));
            var expires = "expires=" + date.toUTCString();
            document.cookie = "selectedTenantDomain=" + cvalue + "; " + expires + "; secure; HttpOnly";
        }

        /**
         * Get the previously selected tenant domain from the cookie
         */
        function getSelectedTenantCookie() {
            var selectedTenantDomain = "";
            var name = "selectedTenantDomain=";
            var cookieItems = document.cookie.split(';');

            for (var i = 0; i < cookieItems.length; i++) {
                var item = cookieItems[i];
                item = item.trim();

                if (item.indexOf(name) != -1) {
                    selectedTenantDomain = item.substring(name.length, item.length);
                    break;
                }
            }
            return selectedTenantDomain;
        }

        /**
         * Select the tenant domain based on the previously selected tenant domain in cookie
         */
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
            if (superTenant == null || superTenant == "") {
                for (i = 0; i < element.options.length; i++) {
                    if (element.options[i].value == superTenant) {
                        element.remove(i);
                        break;
                    }
                }
            }
        }
    </script>

    <div class="form-actions">
        <input type="submit" value="<fmt:message key='login'/>" class="btn btn-primary"
               onclick="appendTenantDomain();">
    </div>

</div>

