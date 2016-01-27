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
<%@ page import="org.wso2.carbon.identity.workflow.impl.stub.WorkflowImplAdminServiceWorkflowImplException" %>
<%@ page import="org.wso2.carbon.identity.workflow.impl.ui.WorkflowImplAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.impl.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>

<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.identity.workflow.impl.stub.bean.BPSProfile" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<%
    String action = request.getParameter(WorkflowUIConstants.PARAM_ACTION);

    WorkflowImplAdminServiceClient client = null;

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    client = new WorkflowImplAdminServiceClient(cookie, backendServerURL, configContext);

    String bundle = "org.wso2.carbon.identity.workflow.impl.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    String forwardTo = "list-bps-profiles.jsp";

    String profileName = request.getParameter(WorkflowUIConstants.PARAM_BPS_PROFILE_NAME);

    if (WorkflowUIConstants.ACTION_VALUE_ADD.equals(action)) {

        String managerHost = request.getParameter(WorkflowUIConstants.PARAM_BPS_MANAGER_HOST);
        String workerHost = request.getParameter(WorkflowUIConstants.PARAM_BPS_WORKER_HOST);
        String username = request.getParameter(WorkflowUIConstants.PARAM_BPS_AUTH_USER);
        String password = request.getParameter(WorkflowUIConstants.PARAM_BPS_AUTH_PASSWORD);
        String[] passwordAsArray;
        if (StringUtils.isNotBlank(password)) {

            passwordAsArray = new String[password.length()];
            for (int i = 0; i < password.length(); i++) {
                passwordAsArray[i] = password.charAt(i) + "";
            }
        } else {
            passwordAsArray = new String[0];
        }
        try {
            BPSProfile bpsProfile = new BPSProfile();
            bpsProfile.setProfileName(profileName);
            bpsProfile.setManagerHostURL(managerHost);
            bpsProfile.setWorkerHostURL(workerHost);
            bpsProfile.setUsername(username);
            bpsProfile.setPassword(passwordAsArray);
            client.addBPSProfile(bpsProfile);

        } catch (WorkflowImplAdminServiceWorkflowImplException e) {
            String message = resourceBundle.getString("workflow.error.bps.profile.add");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "../admin/error.jsp";
        }
    }else if (WorkflowUIConstants.ACTION_VALUE_UPDATE.equals(action)) {
        String managerHost = request.getParameter(WorkflowUIConstants.PARAM_BPS_MANAGER_HOST);
        String workerHost = request.getParameter(WorkflowUIConstants.PARAM_BPS_WORKER_HOST);
        String username = request.getParameter(WorkflowUIConstants.PARAM_BPS_AUTH_USER);
        String password = request.getParameter(WorkflowUIConstants.PARAM_BPS_AUTH_PASSWORD);
        String[] passwordAsArray;
        if (StringUtils.isNotBlank(password)) {

            passwordAsArray = new String[password.length()];
            for (int i = 0; i < password.length(); i++) {
                passwordAsArray[i] = password.charAt(i) + "";
            }
        } else {
            passwordAsArray = new String[0];
        }
        try {
            BPSProfile bpsProfile = new BPSProfile();
            bpsProfile.setProfileName(profileName);
            bpsProfile.setManagerHostURL(managerHost);
            bpsProfile.setWorkerHostURL(workerHost);
            bpsProfile.setUsername(username);

            if(StringUtils.isNotBlank(password)) {
                bpsProfile.setPassword(passwordAsArray);
            }

            client.updateBPSProfile(bpsProfile);

        } catch (WorkflowImplAdminServiceWorkflowImplException e) {
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