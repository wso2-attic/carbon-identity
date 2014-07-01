<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.stub.CsrMetaInfo" %>
<%@page import="org.wso2.carbon.identity.certificateauthority.ui.CAConstants" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.client.CAAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.util.ClientUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%

    CsrMetaInfo[] csrPerPage = null;
    CAAdminServiceClient client = null;
    CsrMetaInfo[] csrFiles = null;
    int numberOfPages = 0;
    int pageNumberInt = 0;


    String statusTypeFilter = CharacterEncoder.getSafeText(request.getParameter("statusTypeFilter"));
    if (statusTypeFilter == null || "".equals(statusTypeFilter)) {
        statusTypeFilter = "ALL";
    }

    String[] statusTypes = new String[]{"PENDING", "REJECTED", "SIGNED"};
    boolean isPaginated = Boolean.parseBoolean(request.getParameter("isPaginated"));
    String csrSearchString = request.getParameter("csrSearchString");

    if (csrSearchString == null) {
        csrSearchString = "*";
    } else {
        csrSearchString = csrSearchString.trim();
    }

    String paginationValue = "isPaginated=true&csrSearchString=" + csrSearchString;

    String pageNumber = request.getParameter("pageNumber");

    if (pageNumber == null) {
        pageNumber = "0";
    }

    try {
        pageNumberInt = Integer.parseInt(pageNumber);
    } catch (NumberFormatException ignored) {
        // ignore
    }

    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.
                    CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    try {
        if (client == null) {
            client = new CAAdminServiceClient(cookie,
                    serverURL, configContext);
            session.setAttribute(CAConstants.CA_ADMIN_CLIENT, client);
        }


        csrFiles = (CsrMetaInfo[]) session.getAttribute("csrFiles");
        if (csrFiles == null || !isPaginated) {

            if (statusTypeFilter.equals("ALL")) {
                csrFiles = client.getCSRFileList();
            } else {
                csrFiles = client.getCSRsFromType(statusTypeFilter);
            }
            if (csrSearchString != "*") {
                csrFiles = client.getCSRsFromCommonName(csrSearchString);
            }

            session.setAttribute("csrFiles", csrFiles);
        }

        int itemsPerPageInt = CAConstants.DEFAULT_ITEMS_PER_PAGE;


        if (csrFiles != null) {
            numberOfPages = (int) Math.ceil((double) csrFiles.length / itemsPerPageInt);
            csrPerPage = ClientUtil.doPagingForStrings(pageNumberInt, itemsPerPageInt, csrFiles);
        }
    } catch (Exception e) {
%>

<script type="text/javascript">
    CARBON.showErrorDialog('<%=e.getMessage()%>', function () {
        location.href = "csr-list-view.jsp";
    });
</script>
<%
    }
%>
<fmt:bundle basename="org.wso2.carbon.identity.certificateauthority.ui.i18n.Resources">
    <carbon:breadcrumb
            label="identity.ca.csr.list"
            resourceBundle="org.wso2.carbon.identity.certificateauthority.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="resources/js/main.js"></script>
    <!--Yahoo includes for dom event handling-->
    <script src="../yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>
    <script src="../ca/js/create-basic-policy.js" type="text/javascript"></script>
    <link href="css/ca.css" rel="stylesheet" type="text/css" media="all"/>

    <script type="text/javascript">

        function getSelectedStatusType() {
            var comboBox = document.getElementById("statusTypeFilter");
            var statusTypeFilter = comboBox[comboBox.selectedIndex].value;
            location.href = 'csr-list-view.jsp?statusTypeFilter=' + statusTypeFilter;
        }
        function searchServices() {
            document.searchForm.submit();
        }
        function viewCSR(serialNo) {
            location.href = "view-csr.jsp?view=true&serialNo=" + serialNo;
        }

    </script>

    <div id="middle">
        <h2><fmt:message key="csr.list"/></h2>

        <div id="workArea">
            <form action="csr-list-view.jsp" name="searchForm" method="post">
                <table id="searchTable" name="searchTable" class="styledLeft" style="border:0;
                                                !important margin-top:10px;margin-bottom:10px;">
                    <tr>
                        <td>
                            <table style="border:0; !important">
                                <tbody>
                                <tr style="border:0; !important">
                                    <td style="border:0; !important">
                                        <nobr>
                                            <fmt:message key="csr.type"/>
                                            <select name="statusTypeFilter" id="statusTypeFilter"
                                                    onchange="getSelectedStatusType();">
                                                <%
                                                    if (statusTypeFilter.equals("ALL")) {
                                                %>
                                                <option value="ALL" selected="selected"><fmt:message
                                                        key="all"/></option>
                                                <%
                                                } else {
                                                %>
                                                <option value="ALL"><fmt:message key="all"/></option>
                                                <%
                                                    }
                                                    for (String status : statusTypes) {
                                                        if (statusTypeFilter.equals(status)) {
                                                %>
                                                <option value="<%= status%>" selected="selected"><%= status%>
                                                </option>
                                                <%
                                                } else {
                                                %>
                                                <option value="<%= status%>"><%= status%>
                                                </option>
                                                <%
                                                        }
                                                    }
                                                %>
                                            </select>
                                            &nbsp;&nbsp;&nbsp;
                                            <fmt:message key="search.csr"/>
                                            <input type="text" name="csrSearchString"
                                                   value="<%= csrSearchString != null? csrSearchString :""%>"/>&nbsp;
                                        </nobr>
                                    </td>
                                    <td style="border:0; !important">
                                        <a class="icon-link" href="#" style="background-image: url(images/search.gif);"
                                           onclick="searchServices(); return false;"
                                           alt="<fmt:message key="search"/>"></a>
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                </table>
            </form>

            <form action="" name="policyForm" method="post">
                <table style="width: 100%" id="dataTable" class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key='user'/></th>
                        <th><fmt:message key='serial.No'/></th>
                        <th><fmt:message key='csr.detail.cn'/></th>
                        <th><fmt:message key='csr.detail.org'/></th>
                        <th><fmt:message key='requested.date'/></th>
                        <th><fmt:message key='status'/></th>
                        <th><fmt:message key='action'/></th>
                    </tr>
                    </thead>
                    <%
                        if (csrPerPage != null && csrPerPage.length > 0) {
                            for (CsrMetaInfo csr : csrPerPage) {
                                if (csr != null && csr.getSerialNo().trim().length() > 0) {
                    %>
                    <tr>
                        <td width="20%"><%=csr.getUserName()%>
                        </td>
                        <td><%=csr.getSerialNo()%>
                        </td>
                        <td><%=csr.getCommonName()%>
                        </td>
                        <td><%=csr.getOrganization()%>
                        </td>
                        <td><%=csr.getRequestedDate().toString()%>
                        </td>
                        <td><%=csr.getStatus()%>
                        </td>

                        <td>
                            <a onclick="viewCSR('<%=csr.getSerialNo()%>');return false;"
                               href="#" style="background-image: url(images/view.gif);"
                               class="icon-link">
                                <fmt:message key='view.csr'/></a>
                        </td>
                    </tr>
                    <%
                                }
                            }
                        }
                    %>

                </table>
                <carbon:paginator pageNumber="<%=pageNumberInt%>"
                                  numberOfPages="<%=numberOfPages%>"
                                  page="csr-list-view.jsp"
                                  pageNumberParameterName="pageNumber"
                                  parameters="<%=paginationValue%>"
                                  resourceBundle="org.wso2.carbon.identity.certificateauthority.ui.i18n.Resources"
                                  prevKey="prev" nextKey="next"/>
            </form>

        </div>
    </div>
</fmt:bundle>