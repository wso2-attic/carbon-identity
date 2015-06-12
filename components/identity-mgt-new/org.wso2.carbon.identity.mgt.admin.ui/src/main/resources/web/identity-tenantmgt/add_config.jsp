<!--
~ Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

<%@ page import="org.wso2.carbon.identity.mgt.admin.ui.client.TenantIdentityMgtClient" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>


<%
    String cookie = (String) session
            .getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(),
            session);
    ConfigurationContext configContext = (ConfigurationContext) config
            .getServletContext()
            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    Map<String, String> configMap = new HashMap<>();
    TenantIdentityMgtClient client =
            new TenantIdentityMgtClient(cookie, backendServerURL, configContext);
    configMap = client.getConfiguration();

%>


<script type="text/javascript">
    jQuery(document).ready(function () {

        jQuery('h2.trigger').click(function () {
            if (jQuery(this).next().is(":visible")) {
                this.className = "active trigger";
            } else {
                this.className = "trigger";
            }
            jQuery(this).next().slideToggle("fast");
            return false; //Prevent the browser jump to the link anchor
        });
    });

    $(function () {
        $('#submitBtn').click(function (e) {
            e.preventDefault();
            $('#addTenantConfigurationForm').submit();
        });
    });

    function cancel() {
        location.href = "../admin/login.jsp";
    }

</script>

<fmt:bundle basename="org.wso2.carbon.identity.mgt.admin.ui.i18n.Resources">
<div id="middle">

<h2>
    Add Configuration Details
</h2>

<div id="workArea">
<form id="addTenantConfigurationForm" name="addTenantConfigurationForm" action="add_config_ajaxprocessor.jsp"
      method="post">
    <% Map<String, String> configurations = new HashMap<>();
   int i=0;
    List<String> values = new ArrayList<String>() {{
        add("true");
        add("false");
    }};

    List<String> updatedConfigList = new ArrayList<>();
%>

<h2 id="role_permission_config_head22" class="active trigger">
    <a href="#">User Information Recovery Configuration</a>
</h2>

<div class="toggle_container sectionSub" style="margin-bottom:10px; display: none;" id="roleConfig2">

<table>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Account.Lock.Enable"));
    %>

    <td><span>Account Lock Enable</span></td>
    <td colspan="2">
        <select name="Account.Lock.Enable"
                id="Account.Lock.Enable" style="width:410px"
                onchange=<%updatedConfigList.add("Account.Lock.Enable");%>>

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>


    </td>

</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Account.Unlock.Enable"));
    %>

    <td><span>Account Unlock Enable</span></td>
    <td colspan="2">
        <select name="Account.Unlock.Enable"
                id="Account.Unlock.Enable" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>


    </td>

</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Account.Max.Attempt.Enable"));
    %>

    <td><span>Account Max Attempt Enable</span></td>
    <td colspan="2">
        <select name="Account.Max.Attempt.Enable"
                id="Account.Max.Attempt.Enable" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>


    </td>

</tr>


<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Account.OneTime.Password.Enable"));
    %>

    <td><span>Account Onetime Password Enable</span></td>
    <td colspan="2">
        <select name="Account.OneTime.Password.Enable"
                id="Account.OneTime.Password.Enable" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>


    </td>

</tr>


<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Account.Password.Reuse.Enable"));
    %>

    <td><span>Account Password Reuse Enable</span></td>
    <td colspan="2">
        <select name="Account.Password.Reuse.Enable"
                id="Account.Password.Reuse.Enable" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>


    </td>

</tr>


<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Account.Password.Expire.Enable"));
    %>

    <td><span>Account Password Expire Enable</span></td>
    <td colspan="2">
        <select name="Account.Password.Expire.Enable"
                id="Account.Password.Expire.Enable" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>


    </td>

</tr>


<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Notification.Sending.Enable"));
    %>
    <td>
        <span>Notification Sending Enable</span></td>
    <td colspan="2">
        <select name="Notification.Sending.Enable"
                id="Notification.Sending.Enable" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>

    </td>
</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Notification.Expire.Time"));
    %>
    <td>
        Notification Expire Time
    </td>
    <td colspan="2"><input type="text" name="Notification.Expire.Time"
                           id="Notification.Expire.Time" style="width:400px"
                           value="<%=configurations.get("tenantConfiguration"+i)%>"/></td>
</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Notification.Sending.Internally.Managed"));
    %>
    <td>
        Notification Sending Internally Managed
    </td>
    <td colspan="2">
        <select name="Notification.Sending.Internally.Managed"
                id="Notification.Sending.Internally.Managed" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>
    </td>
</tr>


<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Enable"));
    %>
    <td>
        Authentication Policy Enable
    </td>
    <td colspan="2">

        <select name="Authentication.Policy.Enable"
                id="Authentication.Policy.Enable" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }

                ++i;
            %>

        </select>
    </td>
</tr>


<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Check.Account.Exist"));
    %>
    <td>
        Authentication Policy Check Account Exist
    </td>
    <td colspan="2">

        <select name="Authentication.Policy.Check.Account.Exist"
                id="Authentication.Policy.Check.Account.Exist" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
                ++i;
            %>

        </select>


    </td>
</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Check.Password.Expire"));
    %>
    <td>
        Authentication Policy Check Password Expire
    </td>
    <td colspan="2">

        <select name="Authentication.Policy.Check.Password.Expire"
                id="Authentication.Policy.Check.Password.Expire" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>
    </td>
</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Password.Expire.Time"));
    %>
    <td>
        Authentication Policy Password Expire Time
    </td>
    <td colspan="2"><input type="text" name="Authentication.Policy.Password.Expire.Time"
                           id="Authentication.Policy.Password.Expire.Time" style="width:400px"
                           value="<%=configurations.get("tenantConfiguration"+i)%>"/></td>
</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Account.Lock.Time"));
    %>
    <td>
        Authentication Policy Account Lock Time
    </td>
    <td colspan="2"><input type="text" name="Authentication.Policy.Account.Lock.Time"
                           id="Authentication.Policy.Account.Lock.Time" style="width:400px"
                           value="<%=configurations.get("tenantConfiguration"+i)%>"/></td>
</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Account.Lock.On.Failure"));
    %>
    <td>
        Authentication Policy Account Lock On Failure
    </td>
    <td colspan="2">

        <select name="Authentication.Policy.Account.Lock.On.Failure"
                id="Authentication.Policy.Account.Lock.On.Failure" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>
    </td>
</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Account.Lock.On.Failure.Max.Attempts"));
    %>
    <td>
        Authentication Policy Account Lock On Failure Max Attempts
    </td>
    <td colspan="2"><input type="text" name="Authentication.Policy.Account.Lock.On.Failure.Max.Attempts"
                           id="Authentication.Policy.Account.Lock.On.Failure.Max.Attempts" style="width:400px"
                           value="<%=configurations.get("tenantConfiguration"+i)%>"/></td>
</tr>


<%--<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Identity.Mgt.User.Recovery.Data.Store"));
    %>
    <td>
        Identity Mgt User Recovery Data Store
    </td>
    <td colspan="2"><input type="text" name="Identity.Mgt.User.Recovery.Data.Store"
                           id="Identity.Mgt.User.Recovery.Data.Store" style="width:400px"
                           value="<%=configurations.get("tenantConfiguration"+i)%>"/>
    </td>
</tr>--%>


<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Check.Password.Reuse"));
    %>
    <td>
        Authentication Policy Check Password Reuse
    </td>
    <td colspan="2">

        <select name="Authentication.Policy.Check.Password.Reuse"
                id="Authentication.Policy.Check.Password.Reuse" style="width:410px">

            <%
                for (String value : values) {
                    if (configurations.get("tenantConfiguration" + i).equals(value)) {
            %>
            <option selected="selected" value="<%=value%>"><%=value%>
            </option>
            <%
            } else {
            %>
            <option value="<%=value%>"><%=value%>
            </option>
            <%
                    }
                }
            %>

        </select>
    </td>
</tr>

<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Password.Expire.Frequency"));
    %>
    <td>
        Password Expire Frequency
    </td>
    <td colspan="2"><input type="text" name="Password.Expire.Frequency"
                           id="Password.Expire.Frequency" style="width:400px"
                           value="<%=configurations.get("tenantConfiguration"+i)%>"/>
    </td>
</tr>


<tr>
    <%
        configurations.put("tenantConfiguration" + i, configMap.get("Password.Reuse.Frequency"));
    %>
    <td>
        Password Reuse Frequency
    </td>
    <td colspan="2"><input type="text" name="Password.Reuse.Frequency"
                           id="Password.Reuse.Frequency" style="width:400px"
                           value="<%=configurations.get("tenantConfiguration"+i)%>"/>
    </td>
</tr>


</table>
</div>

<h2 id="role_permission_config_head22" class="active trigger">
    <a href="#">Self-signup Configuration</a>
</h2>

<div class="toggle_container sectionSub" style="margin-bottom:10px; display: none;" id="roleConfig2">

    <table>


        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("Captcha.Verification.Internally.Managed"));
            %>
            <td>
                Captcha Verification Internally Managed
            </td>
            <td colspan="2">
                <select name="Captcha.Verification.Internally.Managed"
                        id="Captcha.Verification.Internally.Managed" style="width:410px">

                    <%
                        for (String value : values) {
                            if (configurations.get("tenantConfiguration" + i).equals(value)) {
                    %>
                    <option selected="selected" value="<%=value%>"><%=value%>
                    </option>
                    <%
                    } else {
                    %>
                    <option value="<%=value%>"><%=value%>
                    </option>
                    <%
                            }
                        }
                    %>

                </select>
            </td>
        </tr>


    </table>
</div>


<h2 id="role_permission_config_head22" class="active trigger">
    <a href="#">User ID Recovery Configuration</a>
</h2>

<div class="toggle_container sectionSub" style="margin-bottom:10px; display: none;" id="roleConfig2">

    <table>

        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Check.Account.Lock"));
            %>
            <td>
                Authentication Policy Check Account Lock
            </td>
            <td colspan="2">

                <select name="Authentication.Policy.Check.Account.Lock"
                        id="Authentication.Policy.Check.Account.Lock" style="width:410px">

                    <%
                        for (String value : values) {
                            if (configurations.get("tenantConfiguration" + i).equals(value)) {
                    %>
                    <option selected="selected" value="<%=value%>"><%=value%>
                    </option>
                    <%
                    } else {
                    %>
                    <option value="<%=value%>"><%=value%>
                    </option>
                    <%
                            }
                        }

                        ++i;
                    %>

                </select>
            </td>

        </tr>

    </table>
</div>


<h2 id="role_permission_config_head22" class="active trigger">
    <a href="#">Password Policy Configuration</a>
</h2>

<div class="toggle_container sectionSub" style="margin-bottom:10px; display: none;" id="roleConfig2">

    <table>

        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("Password.policy.extensions.1.min.length"));
            %>
            <td>
                Password policy extensions 1 min length
            </td>
            <td colspan="2"><input type="text" name="Password.policy.extensions.1.min.length"
                                   id="Password.policy.extensions.1.min.length" style="width:400px"
                                   value="<%=configurations.get("tenantConfiguration"+i)%>"/></td>
        </tr>

        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("Password.policy.extensions.1.max.length"));
            %>
            <td>
                Password policy extensions 1 max length
            </td>
            <td colspan="2"><input type="text" name="Password.policy.extensions.1.max.length"
                                   id="Password.policy.extensions.1.max.length" style="width:400px"
                                   value="<%=configurations.get("tenantConfiguration"+i)%>"/></td>
        </tr>

        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("Password.policy.extensions.3.pattern"));
            %>
            <td>
                Password policy extensions 3 pattern
            </td>
            <td colspan="2"><input type="text" name="Password.policy.extensions.3.pattern"
                                   id="Password.policy.extensions.3.pattern" style="width:400px"
                                   value="<%=configurations.get("tenantConfiguration"+i)%>"/>

            </td>
        </tr>

    </table>
</div>


<h2 id="role_permission_config_head22" class="active trigger">
    <a href="#">One Time Password Configuration</a>
</h2>

<div class="toggle_container sectionSub" style="margin-bottom:10px; display: none;" id="roleConfig2">

    <table>

        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Check.OneTime.Password"));
            %>
            <td>
                Authentication Policy Check OneTime Password
            </td>
            <td colspan="2">
                <select name="Authentication.Policy.Check.OneTime.Password"
                        id="Authentication.Policy.Check.OneTime.Password" style="width:410px">

                    <%
                        for (String value : values) {
                            if (configurations.get("tenantConfiguration" + i).equals(value)) {
                    %>
                    <option selected="selected" value="<%=value%>"><%=value%>
                    </option>
                    <%
                    } else {
                    %>
                    <option value="<%=value%>"><%=value%>
                    </option>
                    <%
                            }
                        }
                    %>

                </select></td>
        </tr>
    </table>
</div>


<h2 id="role_permission_config_head22" class="active trigger">
    <a href="#">User Creation Configuration</a>
</h2>

<div class="toggle_container sectionSub" style="margin-bottom:10px; display: none;" id="roleConfig2">

    <table>

        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("UserAccount.Verification.Enable"));
            %>
            <td>
                UserAccount Verification Enable
            </td>
            <td colspan="2">

                <select name="UserAccount.Verification.Enable"
                        id="UserAccount.Verification.Enable" style="width:410px">

                    <%
                        for (String value : values) {
                            if (configurations.get("tenantConfiguration" + i).equals(value)) {
                    %>
                    <option selected="selected" value="<%=value%>"><%=value%>
                    </option>
                    <%
                    } else {
                    %>
                    <option value="<%=value%>"><%=value%>
                    </option>
                    <%
                            }
                        }
                    %>

                </select>
            </td>
        </tr>

        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("Temporary.Password.Enable"));
            %>
            <td>
                Temporary Password Enable
            </td>
            <td colspan="2">

                <select name="Temporary.Password.Enable"
                        id="Temporary.Password.Enable" style="width:410px">

                    <%
                        for (String value : values) {
                            if (configurations.get("tenantConfiguration" + i).equals(value)) {
                    %>
                    <option selected="selected" value="<%=value%>"><%=value%>
                    </option>
                    <%
                    } else {
                    %>
                    <option value="<%=value%>"><%=value%>
                    </option>
                    <%
                            }
                        }
                    %>

                </select>

            </td>
        </tr>


        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("Temporary.Password.Default.Value"));
            %>
            <td>
                Temporary Password Default Value
            </td>
            <td colspan="2"><input type="text" name="Temporary.Password.Default.Value"
                                   id="Temporary.Password.Default.Value" style="width:400px"
                                   value="<%=configurations.get("tenantConfiguration"+i)%>"/></td>
        </tr>

        <tr>
            <%
                configurations.put("tenantConfiguration" + i, configMap.get("Authentication.Policy.Account.Lock.On.Creation"));
            %>
            <td>
                Authentication Policy Account Lock On Creation
            </td>
            <td colspan="2">
                <select name="Authentication.Policy.Account.Lock.On.Creation"
                        id="Authentication.Policy.Account.Lock.On.Creation" style="width:410px">

                    <%
                        for (String value : values) {
                            if (configurations.get("tenantConfiguration" + i).equals(value)) {
                    %>
                    <option selected="selected" value="<%=value%>"><%=value%>
                    </option>
                    <%
                    } else {
                    %>
                    <option value="<%=value%>"><%=value%>
                    </option>
                    <%
                            }
                        }
                    %>

                </select>
            </td>
        </tr>
    </table>
</div>

<%
request.setAttribute("updatedConfigList", updatedConfigList);
%>

<tr id="buttonRow">
    <td class="buttonRow">
        <input class="button" type="button" value="Cancel" onclick="cancel()"/>
        <input class="button" type="button" value="Update" id="submitBtn"/>
    </td>
</tr>


</div>
</form>
</div>
</div>
</fmt:bundle>