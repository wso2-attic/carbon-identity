<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.impl.ui.WorkflowImplAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.impl.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>

<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.identity.workflow.impl.stub.bean.BPSProfile" %>
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

<%
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String paginationValue = null;
    String bundle = "org.wso2.carbon.identity.workflow.impl.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    String forwardTo = null;
    int pageNumberInt = 0;
    int numberOfPages = 0;
    BPSProfile[] profilesToDisplay = new BPSProfile[0];
    BPSProfile[] bpsProfiles = null;

    try {
        WorkflowImplAdminServiceClient client = new WorkflowImplAdminServiceClient(cookie, backendServerURL, configContext);
        bpsProfiles = client.listBPSProfiles();
        if (bpsProfiles == null) {
            bpsProfiles = new BPSProfile[0];
        }
        String serviceAlias = null;
        paginationValue = "region=region1&item=workflow_services_list_menu";

        String pageNumber = request.getParameter(WorkflowUIConstants.PARAM_PAGE_NUMBER);
        pageNumberInt = 0;
        numberOfPages = 0;

        try {
            pageNumberInt = Integer.parseInt(pageNumber);
        } catch (NumberFormatException ignored) {
        }
        numberOfPages = (int) Math.ceil((double) bpsProfiles.length / WorkflowUIConstants.RESULTS_PER_PAGE);

        int startIndex = pageNumberInt * WorkflowUIConstants.RESULTS_PER_PAGE;
        int endIndex = (pageNumberInt + 1) * WorkflowUIConstants.RESULTS_PER_PAGE;

        profilesToDisplay = new BPSProfile[WorkflowUIConstants.RESULTS_PER_PAGE];

        for (int i = startIndex, j = 0; i < endIndex && i < bpsProfiles.length; i++, j++) {
            profilesToDisplay[j] = bpsProfiles[i];
        }
    } catch (Exception e) {
        String message = resourceBundle.getString("workflow.error.when.listing.services");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/error.jsp";
    }

%>

<%
    if (forwardTo != null) {
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
    }
%>

<fmt:bundle basename="org.wso2.carbon.identity.workflow.impl.ui.i18n.Resources">
    <carbon:breadcrumb
            label="workflow.mgt"
            resourceBundle="org.wso2.carbon.identity.workflow.impl.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">

        function removeProfile(profileName) {
            function doDelete() {
                location.href = 'update-bps-profile-finish.jsp?<%=WorkflowUIConstants.PARAM_ACTION%>=' +
                '<%=WorkflowUIConstants.ACTION_VALUE_DELETE%>&<%=WorkflowUIConstants.PARAM_BPS_PROFILE_NAME%>=' +
                profileName;
            }

            CARBON.showConfirmationDialog('<fmt:message key="confirmation.bpel.profile.delete"/> ' + profileName + '?',
                    doDelete, null);
        }
        function editProfile(profileName){
            location.href = 'update-bps-profile.jsp?<%=WorkflowUIConstants.PARAM_BPS_PROFILE_NAME%>='+  profileName;
        }
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.bps.profile.list'/></h2>

        <div id="workArea">

            <table class="styledLeft" id="servicesTable">
                <thead>
                <tr>
                    <th width="12%"><fmt:message key="workflow.bps.profile.name"/></th>
                    <th width="20%"><fmt:message key="workflow.bps.profile.manager.host"/></th>
                    <th width="20%"><fmt:message key="workflow.bps.profile.worker.host"/></th>
                    <th width="15%"><fmt:message key="workflow.bps.profile.auth.user"/></th>
                    <th width="15%"><fmt:message key="workflow.bps.profile.callback.user"/></th>
                    <th><fmt:message key="actions"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (bpsProfiles != null && bpsProfiles.length > 0) {
                        for (BPSProfile profile : profilesToDisplay) {
                            if (profile != null) {

                %>
                <tr>
                    <td><%=profile.getProfileName()%>
                    </td>
                    <td><%=profile.getManagerHostURL()%>
                    </td>
                    <td><%=profile.getWorkerHostURL()%>
                    </td>
                    <td><%=profile.getUsername()%>
                    </td>
                    <td><%=profile.getCallbackUser()%>
                    </td>
                    <td>
                        <%
                            if (!WorkflowUIConstants.DEFAULT_BPS_PROFILE.equals(profile.getProfileName())) {
                                if (CarbonUIUtil.isUserAuthorized(request,"/permission/admin/manage/identity/workflow/profile/update")){
                        %>
                        <a title="<fmt:message key='workflow.bps.profile.edit.title'/>"
                           onclick="editProfile('<%=profile.getProfileName()%>');return false;"
                           href="#" style="background-image: url(images/edit.gif);"
                           class="icon-link"><fmt:message key='edit'/></a>
                        <%}
                        if(CarbonUIUtil.isUserAuthorized(request,"/permission/admin/manage/identity/workflow/profile/delete")) { %>
                        <a title="<fmt:message key='workflow.bps.profile.delete.title'/>"
                           onclick="removeProfile('<%=profile.getProfileName()%>');return false;"
                           href="#" style="background-image: url(images/delete.gif);"
                           class="icon-link"><fmt:message key='delete'/></a>
                        <%
                                }
                            }
                        %>
                    </td>
                </tr>
                <%
                            }
                        }
                    } else {%>
                <tr>
                    <td colspan="5"><i>No profiles found.</i></td>
                </tr>
                <%}
                %>
                </tbody>
            </table>
            <carbon:paginator pageNumber="<%=pageNumberInt%>"
                              numberOfPages="<%=numberOfPages%>"
                              page="list-bps-profiles.jsp"
                              pageNumberParameterName="<%=WorkflowUIConstants.PARAM_PAGE_NUMBER%>"
                              resourceBundle="org.wso2.carbon.security.ui.i18n.Resources"
                              parameters="<%=paginationValue%>"
                              prevKey="prev" nextKey="next"/>
            <br/>
        </div>
    </div>
</fmt:bundle>
