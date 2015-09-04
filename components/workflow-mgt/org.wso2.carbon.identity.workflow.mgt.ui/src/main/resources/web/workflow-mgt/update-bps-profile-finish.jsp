<%--
  ~ Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>

<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.BPSProfileDTO" %>

<%
    String action = request.getParameter(WorkflowUIConstants.PARAM_ACTION);

    WorkflowAdminServiceClient client = null;

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);

    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    String forwardTo = "list-bps-profiles.jsp";

    String profileName = request.getParameter(WorkflowUIConstants.PARAM_BPS_PROFILE_NAME);

    if (WorkflowUIConstants.ACTION_VALUE_ADD.equals(action)) {

        String host = request.getParameter(WorkflowUIConstants.PARAM_BPS_HOST);
        String username = request.getParameter(WorkflowUIConstants.PARAM_BPS_AUTH_USER);
        String password = request.getParameter(WorkflowUIConstants.PARAM_BPS_AUTH_PASSWORD);
        String callbackUser = request.getParameter(WorkflowUIConstants.PARAM_CARBON_AUTH_USER);
        String callbackPassword = request.getParameter(WorkflowUIConstants.PARAM_CARBON_AUTH_PASSWORD);
        try {
            BPSProfileDTO bpsProfileDTO = new BPSProfileDTO();
            bpsProfileDTO.setProfileName(profileName);
            bpsProfileDTO.setHost(host);
            bpsProfileDTO.setUsername(username);
            bpsProfileDTO.setPassword(password);
            bpsProfileDTO.setCallbackUser(username);
            bpsProfileDTO.setCallbackPassword(callbackPassword);
            client.addBPSProfile(bpsProfileDTO);

        } catch (WorkflowAdminServiceWorkflowException e) {
            String message = resourceBundle.getString("workflow.error.bps.profile.add");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "../admin/error.jsp";
        }
    }else if (WorkflowUIConstants.ACTION_VALUE_UPDATE.equals(action)) {
        String host = request.getParameter(WorkflowUIConstants.PARAM_BPS_HOST);
        String username = request.getParameter(WorkflowUIConstants.PARAM_BPS_AUTH_USER);
        String password = request.getParameter(WorkflowUIConstants.PARAM_BPS_AUTH_PASSWORD);
        String callbackUser = request.getParameter(WorkflowUIConstants.PARAM_CARBON_AUTH_USER);
        String callbackPassword = request.getParameter(WorkflowUIConstants.PARAM_CARBON_AUTH_PASSWORD);
        try {
            BPSProfileDTO bpsProfileDTO = new BPSProfileDTO();
            bpsProfileDTO.setProfileName(profileName);
            bpsProfileDTO.setHost(host);
            bpsProfileDTO.setUsername(username);
            bpsProfileDTO.setCallbackUser(callbackUser);

            if(password!=null && !password.isEmpty()) {
                bpsProfileDTO.setPassword(password);
            }
            if(callbackPassword!=null && !callbackPassword.isEmpty()) {
                bpsProfileDTO.setCallbackPassword(callbackPassword);
            }

            client.updateBPSProfile(bpsProfileDTO);

        } catch (WorkflowAdminServiceWorkflowException e) {
            String message = resourceBundle.getString("workflow.error.bps.profile.add");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "../admin/error.jsp";
        }
    }else if (WorkflowUIConstants.ACTION_VALUE_DELETE.equals(action)) {
        try {
            client.deleteBPSProfile(profileName);
            forwardTo = "list-bps-profiles.jsp";
        } catch (Exception e) {
            String message = resourceBundle.getString("workflow.error.bps.profile.delete");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "../admin/error.jsp";
        }
    }


%>
<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>
<%
    return;
%>