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
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="java.util.HashMap" %>
<%@page import="org.wso2.carbon.utils.ServerConstants" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.text.MessageFormat" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>


<%
    String BUNDLE = "org.wso2.carbon.identity.mgt.admin.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
    String forwardTo = null;
    try {
        String cookie = (String) session
                .getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(),
                session);
        ConfigurationContext configContext = (ConfigurationContext) config
                .getServletContext()
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

        List updatedConfigList = (List) request.getAttribute("updatedConfigList");
        HashMap<String, String> configMap = new HashMap<>();


/*        String accountLockEnable = request.getParameter("Account.Lock.Enable");
        String notificationSendingEnable = request.getParameter("Notification.Sending.Enable");
        String notificationExpireTime = request.getParameter("Notification.Expire.Time");
        String notificationSendingInternallymanaged = request.getParameter("Notification.Sending.Internally.Managed");
        String authenticationPolicyEnable = request.getParameter("Authentication.Policy.Enable");
        String authenticationPolicyCheckAccountExist = request.getParameter("Authentication.Policy.Check.Account.Exist");
        String authenticationPolicyCheckPasswordExpire = request.getParameter("Authentication.Policy.Check.Password.Expire");
        String authenticationPolicyPasswordExpireTime = request.getParameter("Authentication.Policy.Password.Expire.Time");
        String authenticationPolicyAccountLockTime = request.getParameter("Authentication.Policy.Account.Lock.Time");
        String authenticationPolicyAccountLockOnFailure = request.getParameter("Authentication.Policy.Account.Lock.On.Failure");
        String authenticationPolicyAccountLockOnFailureMaxAttempts = request.getParameter("Authentication.Policy.Account.Lock.On.Failure.Max.Attempts");
        String authenticationPolicyCheckPasswordReuse = request.getParameter("Authentication.Policy.Check.Password.Reuse");
        String passwordExpireFrequency = request.getParameter("Password.Expire.Frequency");
        String passwordReuseFrequency = request.getParameter("Password.Reuse.Frequency");
        String captchaVerificationInternallyManaged = request.getParameter("Captcha.Verification.Internally.Managed");
        String authenticationPolicyCheckAccountLock = request.getParameter("Authentication.Policy.Check.Account.Lock");
        String passwordPolicyExtensions1MinLength = request.getParameter("Password.policy.extensions.1.min.length");
        String passwordPolicyExtensions1MaxLength = request.getParameter("Password.policy.extensions.1.max.length");
        String passwordPolicyExtensions3Pattern = request.getParameter("Password.policy.extensions.3.pattern");
        String authenticationPolicyCheckOneTimePassword = request.getParameter("Authentication.Policy.Check.OneTime.Password");
        String userAccountVerificationEnable = request.getParameter("UserAccount.Verification.Enable");
        String temporaryPasswordEnable = request.getParameter("Temporary.Password.Enable");
        String temporaryPasswordDefaultValue = request.getParameter("Temporary.Password.Default.Value");
        String authenticationPolicyAccountLockOnCreation = request.getParameter("Authentication.Policy.Account.Lock.On.Creation");

        HashMap<String, String> configMap = new HashMap<String, String>();
        configMap.put("Account.Lock.Enable", accountLockEnable);
        configMap.put("Notification.Sending.Enable", notificationSendingEnable);
        configMap.put("Notification.Expire.Time", notificationExpireTime);
        configMap.put("Notification.Sending.Internally.Managed", notificationSendingInternallymanaged);
        configMap.put("UserAccount.Verification.Enable", userAccountVerificationEnable);
        configMap.put("Captcha.Verification.Internally.Managed", captchaVerificationInternallyManaged);
        configMap.put("Temporary.Password.Enable", temporaryPasswordEnable);
        configMap.put("Temporary.Password.Default.Value", temporaryPasswordDefaultValue);
        configMap.put("Authentication.Policy.Enable", authenticationPolicyEnable);
        configMap.put("Authentication.Policy.Check.Account.Exist", authenticationPolicyCheckAccountExist);
        configMap.put("Authentication.Policy.Check.Account.Lock", authenticationPolicyCheckAccountLock);
        configMap.put("Authentication.Policy.Check.OneTime.Password", authenticationPolicyCheckOneTimePassword);
        configMap.put("Authentication.Policy.Check.Password.Expire", authenticationPolicyCheckPasswordExpire);
        configMap.put("Authentication.Policy.Password.Expire.Time", authenticationPolicyPasswordExpireTime);
        configMap.put("Authentication.Policy.Account.Lock.On.Creation", authenticationPolicyAccountLockOnCreation);
        configMap.put("Authentication.Policy.Account.Lock.Time", authenticationPolicyAccountLockTime);
        configMap.put("Authentication.Policy.Account.Lock.On.Failure", authenticationPolicyAccountLockOnFailure);
        configMap.put("Authentication.Policy.Account.Lock.On.Failure.Max.Attempts", authenticationPolicyAccountLockOnFailureMaxAttempts);
        configMap.put("Authentication.Policy.Check.Password.Reuse", authenticationPolicyCheckPasswordReuse);
        configMap.put("Password.Expire.Frequency", passwordExpireFrequency);
        configMap.put("Password.Reuse.Frequency", passwordReuseFrequency);

        configMap.put("Password.policy.extensions.1.min.length", passwordPolicyExtensions1MinLength);
        configMap.put("Password.policy.extensions.1.max.length", passwordPolicyExtensions1MaxLength);
        configMap.put("Password.policy.extensions.3.pattern", passwordPolicyExtensions3Pattern);*/


        for(int i=0; i<updatedConfigList.size(); i++){
            configMap.put(updatedConfigList.get(i), request.getParameter(updatedConfigList.get(i)));
        }

        TenantIdentityMgtClient client =
                new TenantIdentityMgtClient(cookie, backendServerURL, configContext);
        client.updateConfiguration(configMap);
        String message = MessageFormat.format(resourceBundle.getString("success.adding.config"), null);
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.INFO, request);
        forwardTo = "../admin/login.jsp";
    } catch (Exception e) {
        String message = MessageFormat.format(resourceBundle.getString("error.adding.config"),
                new Object[]{e.getMessage()});
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/login.jsp";
    }

%>

<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }

    forward();
</script>



