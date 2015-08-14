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
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.AssociationDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowEventDTO" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowRequestDTO" %>
<%@ page import="org.wso2.carbon.context.CarbonContext" %>
<%@ page import="org.wso2.carbon.user.core.util.UserCoreUtil" %>
<%@ page import="org.wso2.carbon.context.PrivilegedCarbonContext" %>

<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    //    String username = CharacterEncoder.getSafeText(request.getParameter("username"));

    //String loggedUser =  CarbonContext.getThreadLocalCarbonContext().getUsername();
    String loggedUser = (String)session.getAttribute("logged-user");
    String bundle = "org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());
    WorkflowAdminServiceClient client;
    String forwardTo = null;
    WorkflowRequestDTO[] associationToDisplay = new WorkflowRequestDTO[0];
    String paginationValue = "region=region1";

    String pageNumber = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_PAGE_NUMBER));
    int pageNumberInt = 0;
    int numberOfPages = 0;
    String workflowId = StringUtils.EMPTY;
    WorkflowEventDTO[] workflowEvents;
    Map<String, List<WorkflowEventDTO>> events = new HashMap<String, List<WorkflowEventDTO>>();

    if (pageNumber != null) {
        try {
            pageNumberInt = Integer.parseInt(pageNumber);
        } catch (NumberFormatException ignored) {
            //not needed here since it's defaulted to 0
        }
    }
    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        client = new WorkflowAdminServiceClient(cookie, backendServerURL, configContext);
        workflowId = CharacterEncoder.getSafeText(request.getParameter(WorkflowUIConstants.PARAM_WORKFLOW_ID));

        WorkflowRequestDTO[] requestList = client.getRequestsCreatedByUser(loggedUser);

        if (requestList == null) {
            requestList = new WorkflowRequestDTO[0];
        }

        numberOfPages = (int) Math.ceil((double) requestList.length / WorkflowUIConstants.RESULTS_PER_PAGE);

        int startIndex = pageNumberInt * WorkflowUIConstants.RESULTS_PER_PAGE;
        int endIndex = (pageNumberInt + 1) * WorkflowUIConstants.RESULTS_PER_PAGE;
        associationToDisplay = new WorkflowRequestDTO[WorkflowUIConstants.RESULTS_PER_PAGE];

        for (int i = startIndex, j = 0; i < endIndex && i < requestList.length; i++, j++) {
            associationToDisplay[j] = requestList[i];
        }

        workflowEvents = client.listWorkflowEvents();
        for (WorkflowEventDTO event : workflowEvents) {
            String category = event.getEventCategory();
            if (!events.containsKey(category)) {
                events.put(category, new ArrayList<WorkflowEventDTO>());
            }
            events.get(category).add(event);
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
    <carbon:breadcrumb label="view"
                       resourceBundle="org.wso2.carbon.identity.workflow.mgt.ui.i18n.Resources"
                       topPage="false" request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
    <script src="//code.jquery.com/jquery-1.10.2.js"></script>
    <script src="//code.jquery.com/ui/1.11.4/jquery-ui.js"></script>
    <link rel="stylesheet" href="/resources/demos/style.css">

    <script type="text/javascript">
        function doCancel() {
            location.href = 'list-workflows.jsp';
        }
        function removeAssociation(id, event) {
            function doDelete() {
                location.href = 'update-workflow-finish.jsp?<%=WorkflowUIConstants.PARAM_ACTION%>=' +
                        '<%=WorkflowUIConstants.ACTION_VALUE_DELETE_ASSOCIATION%>&' +
                        '<%=WorkflowUIConstants.PARAM_ASSOCIATION_ID%>=' + id + '&' +
                        '<%=WorkflowUIConstants.PARAM_WORKFLOW_ID%>=<%=workflowId%>';
            }

            CARBON.showConfirmationDialog('<fmt:message key="confirmation.association.delete"/> ' + event + '?',
                    doDelete, null);
        }
        function addAssociation() {
            window.location = "add-association.jsp?<%=WorkflowUIConstants.PARAM_WORKFLOW_ID%>=<%=workflowId%>";
        }

        function doCancel() {
            document.getElementById('addNew').style.display = 'none';
        }
    </script>
    <script type="text/javascript">
        var eventsObj = {};
        var lastSelectedCategory = '';
        <%
            for (Map.Entry<String,List<WorkflowEventDTO>> eventCategory : events.entrySet()) {
            %>
        eventsObj["<%=eventCategory.getKey()%>"] = [];
        <%
            for (WorkflowEventDTO event : eventCategory.getValue()) {
                %>
        var eventObj = {};
        eventObj.displayName = "<%=event.getEventFriendlyName()%>";
        eventObj.value = "<%=event.getEventId()%>";
        eventObj.title = "<%=event.getEventDescription()!=null?event.getEventDescription():""%>";
        eventsObj["<%=eventCategory.getKey()%>"].push(eventObj);
        <%
                    }
            }
        %>

        function updateActions() {
            var categoryDropdown = document.getElementById("categoryDropdown");
            var actionDropdown = document.getElementById("actionDropdown");
            var selectedCategory = categoryDropdown.options[categoryDropdown.selectedIndex].value;
            if (selectedCategory != lastSelectedCategory) {
                var eventsOfCategory = eventsObj[selectedCategory];
                for (var i = 0; i < eventsOfCategory.length; i++) {
                    var opt = document.createElement("option");
                    opt.text = eventsOfCategory[i].displayName;
                    opt.value = eventsOfCategory[i].value;
                    opt.title = eventsOfCategory[i].title;
                    actionDropdown.options.add(opt);
                }
                lastSelectedCategory = selectedCategory;
            }
        }

        function getSelectedRequestType() {
        }
        function getSelectedStatusType() {
        }
        function searchRequests() {
        }

    </script>

    <script>
        $(function() {
            $( "#from" ).datepicker({
                defaultDate: "+1w",
                changeMonth: true,
                numberOfMonths: 1,
                onClose: function( selectedDate ) {
                    $( "#to" ).datepicker( "option", "minDate", selectedDate );
                }
            });
            $( "#to" ).datepicker({
                defaultDate: "+1w",
                changeMonth: true,
                numberOfMonths: 1,
                onClose: function( selectedDate ) {
                    $( "#from" ).datepicker( "option", "maxDate", selectedDate );
                }
            });
        });
    </script>

    <div id="middle">
        <h2><fmt:message key='workflow.list'/></h2>

        <form action="index.jsp" name="searchForm" method="post">
            <table id="searchTable" name="searchTable" class="styledLeft" style="border:0;
                                                !important margin-top:10px;margin-bottom:10px;">
                <tr>
                    <td>
                        <table style="border:0; !important">
                            <tbody>
                            <tr style="border:0; !important">
                                <td style="border:0; !important">
                                    <nobr>
                                        <fmt:message key="workflow.request.type"/>
                                        <select name="requestTypeFilter" id="requestTypeFilter"
                                                onchange="getSelectedRequestType();">
                                            <option value="allTasks"
                                                    selected="selected"><fmt:message key="allTasks"/></option>
                                            <option value="myTasks"><fmt:message key="myTasks"/></option>
                                        </select>
                                    </nobr>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </td>

                    <td>
                        <table style="border:0; !important">
                            <tbody>
                            <tr style="border:0; !important">
                                <td style="border:0; !important">
                                    <nobr>
                                        <fmt:message key="workflow.request.status"/>
                                        <select name="requestTypeFilter" id="requestTypeFilter"
                                                onchange="getSelectedStatusType();">
                                            <option value="allTasks"
                                                    selected="selected"><fmt:message key="allTasks"/></option>
                                            <option value="pending"><fmt:message key="pending"/></option>
                                            <option value="approved"><fmt:message key="approved"/></option>
                                            <option value="rejected"><fmt:message key="rejected"/></option>
                                            <option value="failed"><fmt:message key="failed"/></option>
                                        </select>
                                    </nobr>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </td>
                    <td>
                        <table style="border:0; !important">
                            <tbody>
                            <tr style="border:0; !important">
                                <td style="border:0; !important">
                                    <nobr>
                                        <fmt:message key="workflow.request.createdAt"/>
                                        <label for="from">From</label>
                                        <input type="text" id="from" name="from">
                                        <label for="to">to</label>
                                        <input type="text" id="to" name="to">
                                    </nobr>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </td>
                    <td style="border:0; !important">
                        <a class="icon-link" href="#" style="background-image: url(images/search-top.png);"
                           onclick="searchRequests(); return false;"
                           alt="<fmt:message key="search"/>"></a>
                    </td>
                </tr>
            </table>
        </form>

        <div id="workArea">
            <table class="styledLeft" id="servicesTable">
                <thead>
                <tr>
                    <th><fmt:message key="workflow.eventType"/></th>
                    <th><fmt:message key="workflow.createdAt"/></th>
                    <th><fmt:message key="workflow.updatedAt"/></th>
                    <th><fmt:message key="workflow.status"/></th>
                    <th><fmt:message key="workflow.requestParams"/></th>
                    <th><fmt:message key="actions"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    for (WorkflowRequestDTO workflowReq : associationToDisplay) {
                        if (workflowReq != null ) {
                %>
                <tr>
                    <td><%=workflowReq.getEventType()%>
                    </td>
                    <td><%=workflowReq.getCreatedAt()%>
                    </td>
                    <td><%=workflowReq.getUpdatedAt()%>
                    </td>
                    <td><%=workflowReq.getStatus()%>
                    </td>
                    <td><%=workflowReq.getRequestParams()%>
                    </td>
                    <td>
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
                              page="view-workflow.jsp"
                              pageNumberParameterName="<%=WorkflowUIConstants.PARAM_PAGE_NUMBER%>"
                              resourceBundle="org.wso2.carbon.security.ui.i18n.Resources"
                              parameters="<%=paginationValue%>"
                              prevKey="prev" nextKey="next"/>
            <br/>
        </div>
    </div>
</fmt:bundle>