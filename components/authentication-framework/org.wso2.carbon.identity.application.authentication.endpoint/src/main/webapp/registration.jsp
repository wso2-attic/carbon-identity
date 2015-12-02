<% /**
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
*/ %>
<%@ page
        import="org.wso2.carbon.identity.application.authentication.endpoint.util.UserRegistrationAdminServiceClient" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.user.registration.stub.dto.UserFieldDTO" %>
<%@ page import="java.util.List" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<%
    String forwardTo;
    try {

        UserRegistrationAdminServiceClient registrationClient = new UserRegistrationAdminServiceClient();
        boolean isExistingUser = registrationClient.isUserExist(request.getParameter("reg_username"));

        if (StringUtils.equals(request.getParameter("is_validation"), "true")) {
            if (isExistingUser) {
                out.write("User Exist");
            } else {
                out.write("Ok");
            }
            return;
        }

        if (isExistingUser) {
            throw new Exception("User exist");
        }

        List<UserFieldDTO> fields = (List<UserFieldDTO>) session.getAttribute("fields");

        for(UserFieldDTO userFieldDTO : fields) {
            userFieldDTO.setFieldValue(request.getParameter(userFieldDTO.getFieldName()));
        }

        String username = request.getParameter("reg_username");
        char [] password = request.getParameter("reg_password").toCharArray();
        registrationClient.addUser(username, password, fields);

        forwardTo = "../dashboard/index.jag";

    } catch (Exception e) {
        forwardTo = "create-account.jsp?sessionDataKey=" + request.getParameter("sessionDataKey") +
                "&failedPrevious=true&errorCode=" + e.getMessage();
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