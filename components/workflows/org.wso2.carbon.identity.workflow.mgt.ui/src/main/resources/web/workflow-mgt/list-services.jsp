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

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@page import="org.apache.axis2.AxisFault" %>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException" %>
<%@page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.ServiceAssociationDTO" %>
<%@page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>

<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    //    String username = CharacterEncoder.getSafeText(request.getParameter("username"));

    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    WorkflowAdminServiceClient client;
    String forwardTo = null;
    String serviceAlias = null;
    ServiceAssociationDTO[] servicesToDisplay = new ServiceAssociationDTO[0];
    String paginationValue = "region=region1&item=workflow_services_list_menu";

    String pageNumber = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_PAGE_NUMBER));
    int pageNumberInt = 0;
    int numberOfPages = 0;

    try {
        pageNumberInt = Integer.parseInt(pageNumber);
    } catch (NumberFormatException ignored) {
    }
    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);

        ServiceAssociationDTO[] services = client.listServices();

        numberOfPages = (int) Math.ceil((double) services.length / WorkflowUIConstants.SERVICES_PER_PAGE);

        int startIndex = pageNumberInt * WorkflowUIConstants.SERVICES_PER_PAGE;
        int endIndex = (pageNumberInt + 1) * WorkflowUIConstants.SERVICES_PER_PAGE;
        servicesToDisplay = new ServiceAssociationDTO[WorkflowUIConstants.SERVICES_PER_PAGE];

        for (int i = startIndex, j = 0; i < endIndex && i < services.length; i++, j++) {
            servicesToDisplay[j] = services[i];
        }
    } catch (WorkflowAdminServiceWorkflowException e) {
        String message = resourceBundle.getString("workflow.error.when.listing.services");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/error.jsp";
    } catch (AxisFault e) {
        String message = resourceBundle.getString("workflow.error.when.initiating.service.client");
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
<fmt:bundle basename="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources">
    <carbon:breadcrumb
            label="workflow.mgt"
            resourceBundle="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">
        function doCancel() {
            location.href = 'list-services.jsp';
        }
        function removeAssociation(alias, event) {
            location.href = 'service-condition.jsp?<%=WorkflowUIConstants.PARAM_ACTION%>=' +
            '<%=WorkflowUIConstants.ACTION_VALUE_DELETE%>&<%=WorkflowUIConstants.PARAM_SERVICE_ALIAS%>=' + alias +
            '&<%=WorkflowUIConstants.PARAM_SERVICE_ASSOCIATION_EVENT%>=' + event;
        }
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.service.associate'/></h2>

        <div id="workArea">

            <table class="styledLeft" id="servicesTable">
                <thead>
                <tr>
                    <th width="30%"><fmt:message key="service.alias"/></th>
                    <th width="30%"><fmt:message key="workflow.service.associate.event"/></th>
                    <th width="15%"><fmt:message key="workflow.service.associate.priority"/></th>
                    <th><fmt:message key="service.actions"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    for (ServiceAssociationDTO service : servicesToDisplay) {
                        if (service != null) {

                %>
                <tr>
                    <td><%=service.getServiceAlias()%>
                    </td>
                    <td><%=service.getEvent()%>
                    </td>
                    <td><%=service.getPriority()%>
                    </td>
                    <td>
                        <a title="<fmt:message key='workflow.service.association.delete.title'/>"
                           onclick="removeAssociation('<%=service.getServiceAlias()%>','<%=service.getEvent()%>');return false;"
                           href="#" style="background-image: url(images/delete.gif);"
                           class="icon-link"><fmt:message key='delete'/></a>
                    </td>
                </tr>
                <%
                        }
                    }
                %>
                </tbody>
            </table>
            <carbon:paginator pageNumber="<%=pageNumberInt%>"
                              numberOfPages="<%=numberOfPages%>"
                              page="list-services.jsp"
                              pageNumberParameterName="<%=WorkflowUIConstants.PARAM_PAGE_NUMBER%>"
                              resourceBundle="org.wso2.carbon.security.ui.i18n.Resources"
                              parameters="<%=paginationValue%>"
                              prevKey="prev" nextKey="next"/>
            <br/>
        </div>
    </div>
</fmt:bundle>