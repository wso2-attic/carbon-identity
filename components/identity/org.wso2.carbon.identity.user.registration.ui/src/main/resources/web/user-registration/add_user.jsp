<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.user.registration.ui.client.UserRegistrationClient" %>
<%@ page import="org.wso2.carbon.identity.user.registration.ui.util.UserRegistrationUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>

<%

    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);    

    String forwardPage = request.getParameter("forwardPage");
    if (forwardPage != null) {
        forwardPage = URLDecoder.decode(forwardPage, "UTF-8");
    }

    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String forwardTo = "user-registration.jsp?region=region1&item=user_registration_menu&ordinal=0";

    if (forwardPage != null) {
        forwardTo = forwardPage;
    }

    String BUNDLE = "org.wso2.carbon.identity.user.registration.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());


    try {
        UserRegistrationClient client =
                new UserRegistrationClient(cookie, serverURL, configContext);
        UserFieldDTO[] fieldDTOs = null;
        if (session.getAttribute("openId") == null) {
            fieldDTOs = client.readUserFieldsForUserRegistration(
                    UserCoreConstants.DEFAULT_CARBON_DIALECT);
        } else {
            session.removeAttribute("openId");
            fieldDTOs =
                    client.readUserFieldsForUserRegistration(IdentityConstants.OPENID_SREG_DIALECT);
        }

        fieldDTOs = client.getOrderedUserFields(fieldDTOs);
        UserDTO userDTO = new UserDTO();

        if (fieldDTOs != null) {
            for (UserFieldDTO field : fieldDTOs) {
                String value = request.getParameter(field.getClaimUri());
                field.setFieldValue(value);
            }
        }

        userDTO.setUserFields(fieldDTOs);
        userDTO.setUserName(request.getParameter(UserRegistrationConstants.PARAM_DOMAINNAME)+"/"+request.getParameter(UserRegistrationConstants.PARAM_USERNAME));
        userDTO.setPassword(request.getParameter(UserRegistrationConstants.PARAM_PASSWORD));
        userDTO.setOpenID((String) session.getAttribute("openIdURL"));
        client.addUser(userDTO);
        String message = resourceBundle.getString("user.registered");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.INFO, request);
    } catch (Exception e) {
        String message = resourceBundle.getString("error.while.adding.user");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
    }

%>


<%@page import="java.util.Map" %>
<%@page
        import="org.wso2.carbon.identity.user.registration.ui.UserRegistrationConstants" %>
<%@page import="org.wso2.carbon.identity.base.IdentityConstants" %>
<%@page import="org.wso2.carbon.user.core.UserCoreConstants" %>
<%@page import="java.util.ResourceBundle" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="org.wso2.carbon.identity.user.registration.stub.dto.UserDTO" %>
<%@ page import="org.wso2.carbon.identity.user.registration.stub.dto.UserFieldDTO" %>
<script
        type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>

