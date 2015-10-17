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
<%@ page
        import="org.wso2.carbon.identity.application.authentication.endpoint.util.UserRegistrationAdminServiceClient" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%
    String forwardTo;
    int errorCode = 0;
    try {
        UserRegistrationAdminServiceClient registrationClient = new UserRegistrationAdminServiceClient();
        if (!request.getParameter("reg-password").equals(request.getParameter("reg-password2"))) {
            errorCode = 1;
            throw new Exception();
        }
        Map<String, String> registrationParameters = new HashMap<String, String>();
        registrationParameters.put("reg-username", request.getParameter("reg-username"));
        registrationParameters.put("reg-first-name", request.getParameter("reg-first-name"));
        registrationParameters.put("reg-last-name", request.getParameter("reg-last-name"));
        registrationParameters.put("reg-password", request.getParameter("reg-password"));
        registrationParameters.put("reg-email", request.getParameter("reg-email"));
        registrationClient.addUser(registrationParameters);
        forwardTo = "../dashboard/index.jag";

    } catch (Exception e) {
        if (errorCode == 0) {
            errorCode = 2;
        }
        forwardTo = "create-account.jsp?sessionDataKey=" + request.getParameter("sessionDataKey") +
                "&failedPrevious=true&errorCode=" + errorCode;
    }


%>
<script type="text/javascript">
    function forward() {
        location.href = "<%=Encode.forJavaScriptBlock(forwardTo)%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>