<%--
  ~ Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
  --%>


<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.stub.CertificateMetaInfo" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.CAConstants" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.client.CAAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.util.ClientUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.HashMap" %>
<%

    CertificateMetaInfo[] certPerPage = null;


    CAAdminServiceClient client = null;
    CertificateMetaInfo[] certFiles = null;

    String statusTypeFilter = CharacterEncoder.getSafeText(request.getParameter("statusTypeFilter"));
    if (statusTypeFilter == null || "".equals(statusTypeFilter)) {
        statusTypeFilter = "ALL";
    }

    String[] statusTypes = new String[]{"REVOKED", "ACTIVE"};

    String selectedReason = CharacterEncoder.getSafeText(request.getParameter("selectedReason"));
    if (selectedReason == null || "".equals(selectedReason)) {
        selectedReason = "Unspecified";
    }
    HashMap<String, Integer> reasonList = ClientUtil.getReasonMap();


    int numberOfPages = 0;
    boolean isPaginated = Boolean.parseBoolean(request.getParameter("isPaginated"));
    String certSearchString = request.getParameter("certSearchString");
    if (certSearchString == null) {
        certSearchString = "*";
    } else {
        certSearchString = certSearchString.trim();
    }
    String paginationValue = "isPaginated=true&certSearchString=" + certSearchString;

    String pageNumber = request.getParameter("pageNumber");
    if (pageNumber == null) {
        pageNumber = "0";
    }
    int pageNumberInt = 0;
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

        certFiles = (CertificateMetaInfo[]) session.getAttribute("certFiles");
        if (certFiles == null || !isPaginated) {
            if (statusTypeFilter.equals("ALL")) {
                certFiles = client.getPubCertList();
            } else {
                certFiles = client.getCertificatesFromStatus(statusTypeFilter);
            }
            session.setAttribute("certFiles", certFiles);

        }
        int itemsPerPageInt = CAConstants.DEFAULT_ITEMS_PER_PAGE;


        if (certFiles != null) {
            numberOfPages = (int) Math.ceil((double) certFiles.length / itemsPerPageInt);
            certPerPage = ClientUtil.doPagingForCertificates(pageNumberInt, itemsPerPageInt, certFiles);
        }


    } catch (Exception e) {
%>

<script type="text/javascript">
    CARBON.showErrorDialog('<%=e.getMessage()%>', function () {
        location.href = "certificate-list-view.jsp";
    });
</script>
<%
    }
%>
<fmt:bundle basename="org.wso2.carbon.identity.certificateauthority.ui.i18n.Resources">
<carbon:breadcrumb
        label="identity.ca.pub.cert.list"
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

    var allSubscribersSelected = false;

    function getSelectedStatusType() {
        var comboBox = document.getElementById("statusTypeFilter");
        var statusTypeFilter = comboBox[comboBox.selectedIndex].value;
        location.href = 'certificate-list-view.jsp?statusTypeFilter=' + statusTypeFilter;
    }
    function getSelectedReason() {
        var comboBox = document.getElementById("selectedReason");
        var selectedReason = comboBox[comboBox.selectedIndex].value;
        location.href = 'certificate-list-view.jsp?selectedReason=' + selectedReason;
    }

    function searchServices() {
        document.searchForm.submit();
    }
    function download(serialNo) {

    }
    function revoke(serialNo) {

    }
    function resetVars() {
        allSubscribersSelected = false;

        var isSelected = false;
        if (document.certificateForm.subscribers[0] != null) { // there is more than 1 service
            for (var j = 0; j < document.certificateForm.subscribers.length; j++) {
                if (document.certificateForm.subscribers[j].checked) {
                    isSelected = true;
                }
            }
        } else if (document.certificateForm.subscribers != null) { // only 1 service
            if (document.certificateForm.subscribers.checked) {
                isSelected = true;
            }
        }
        return false;
    }

    function revokeCertificates() {
        var selected = false;
        if (document.certificateForm.subscribers[0] != null) { // there is more than 1 policy
            for (var j = 0; j < document.certificateForm.subscribers.length; j++) {
                selected = document.certificateForm.subscribers[j].checked;
                if (selected) break;
            }
        } else if (document.certificateForm.subscribers != null) { // only 1 policy
            selected = document.certificateForm.subscribers.checked;
        }
        if (!selected) {
            CARBON.showInfoDialog('<fmt:message key="select.certificates.to.be.revoked"/>');
            return;
        }
        if (allSubscribersSelected) {
            CARBON.showConfirmationDialog("<fmt:message key="revoke.all.certificates.prompt"/>", function () {
                document.certificateForm.action = "revoke-certificates.jsp";
                document.certificateForm.submit();
            });
        } else {
            CARBON.showConfirmationDialog("<fmt:message key="revoke.certificates.on.page.prompt"/>", function () {
                document.certificateForm.action = "revoke-certificates.jsp";
                document.certificateForm.submit();
            });
        }
    }

    function selectAllInThisPage(isSelected) {

        allSubscribersSelected = false;
        if (document.certificateForm.subscribers != null &&
                document.certificateForm.subscribers[0] != null) { // there is more than 1 service
            if (isSelected) {
                for (var j = 0; j < document.certificateForm.subscribers.length; j++) {
                    document.certificateForm.subscribers[j].checked = true;
                }
            } else {
                for (j = 0; j < document.certificateForm.subscribers.length; j++) {
                    document.certificateForm.subscribers[j].checked = false;
                }
            }
        } else if (document.certificateForm.subscribers != null) { // only 1 service
            document.certificateForm.subscribers.checked = isSelected;
        }
        return false;
    }

    function viewCertificate(serialNo) {
        location.href = "view-certificate.jsp?redirect=list&view=true&serialNo=" + serialNo;
    }

</script>

<div id="middle">

<h2><fmt:message key="identity.ca.pub.cert.list"/></h2>

<div id="workArea">

    <form action="certificate-list-view.jsp" name="searchForm" method="post">
        <table id="searchTable" name="searchTable" class="styledLeft" style="border:0;
                                                !important margin-top:10px;margin-bottom:10px;">
            <tr>
                <td>
                    <table style="border:0; !important">
                        <tbody>
                        <tr style="border:0; !important">

                            <td style="border:0; !important">
                                <nobr>
                                    <fmt:message key="cert.type"/>
                                    <select name="statusTypeFilter" id="statusTypeFilter"
                                            onchange="getSelectedStatusType();">
                                        <%
                                            if (statusTypeFilter.equals("ALL")) {
                                        %>
                                        <option value="ALL" selected="selected"><fmt:message key="all"/></option>
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
                                    <fmt:message key="cert.search"/>
                                    <input type="text" name="certSearchString"
                                           value="<%= certSearchString != null? certSearchString :""%>"/>&nbsp;
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
            <tr>

            </tr>
        </table>
    </form>


    <form action="" name="certificateForm" method="post">
        <table style="border:0; !important">
            <tbody>
            <tr style="border:0; !important">

                <td style="border:0; !important">
                    <nobr>
                        <fmt:message key="revoke.reason"/>
                        <select name="selectedReason" id="selectedReason" onchange="getSelectedReason();">
                            <%
                                if (selectedReason.equals("Unspecified")) {
                            %>
                            <option value="Unspecified" selected="selected"><fmt:message
                                    key="unspecified"/></option>
                            <%
                            } else {
                            %>
                            <option value="Unspecified"><fmt:message key="unspecified"/></option>
                            <%
                                }
                                for (String reason : reasonList.keySet()) {
                                    if (selectedReason.equals(reason)) {
                            %>
                            <option value="<%= reason%>" selected="selected"><%= reason%>
                            </option>
                            <%
                            } else {
                            %>
                            <option value="<%= reason%>"><%= reason%>
                            </option>
                            <%
                                    }
                                }
                            %>
                        </select>
                    </nobr>
                </td>
                <td>
                    <a onclick="revokeCertificates();return false;"
                       href="#" style="background-image: url(images/up.gif);"
                       class="icon-link">
                        <fmt:message key='revoke.selected'/></a>
                </td>
            </tr>
            <tr>
                <td>
                    <a style="cursor: pointer;" onclick="selectAllInThisPage(true);return false;"
                       href="#"><fmt:message key="selectAllInPage"/></a>
                    &nbsp;<b>|</b>&nbsp;
                    <a style="cursor: pointer;" onclick="selectAllInThisPage(false);return false;"
                       href="#"><fmt:message key="selectNone"/></a>
                </td>
            </tr>
            </tbody>
        </table>
        <%
            if (certFiles == null) {
        %>

        <fmt:message key="no.cert.available"/>
        <%
        } else {
        %>
        <table style="width: 100%" id="dataTable" class="styledLeft">
            <thead>
            <tr>
                <th></th>
                <th><fmt:message key='serial.No'/></th>
                <th><fmt:message key='user'/></th>
                <th><fmt:message key='issued.date'/></th>
                <th><fmt:message key='expiry.date'/></th>
                <th><fmt:message key='status'/></th>
                <th><fmt:message key='download'/></th>
                <th><fmt:message key='view.certificate'/></th>
            </tr>
            </thead>
            <%


                if (certPerPage != null && certPerPage.length > 0) {
                    for (CertificateMetaInfo certificate : certPerPage) {
                        if (certificate != null && certificate.getSerialNo().trim().length() > 0) {

            %>
            <tr>
                <td width="10px" style="text-align:center; !important">
                    <input type="checkbox" name="subscribers"
                           value="<%=certificate.getSerialNo()%>"
                           onclick="resetVars()" class="chkBox"/>
                </td>
                <td width="20%"><%=certificate.getSerialNo()%>
                </td>
                <td width="20%"><%=certificate.getUsername()%>
                </td>
                <td><%=certificate.getIssuedDate()%>
                </td>
                <td><%=certificate.getExpiryDate()%>
                </td>
                <td><%=certificate.getStatus()%>
                </td>

                <td>
                    <nobr>
                        <a href="/ca/certificate/<%=certificate.getSerialNo()%>.crt"
                           class="icon-link" style="background-image:url(images/download.gif);"
                           target="_self">
                            <fmt:message key="download"/>
                        </a>
                    </nobr>
                </td>
                <td>
                    <a onclick="viewCertificate('<%=certificate.getSerialNo()%>');return false;"
                       href="#" style="background-image: url(images/up.gif);"
                       class="icon-link">
                        <fmt:message key='view.certificate'/></a>
                </td>
            </tr>
            <%
                            }
                        }
                    }
                }
            %>

        </table>
        <carbon:paginator pageNumber="<%=pageNumberInt%>"
                          numberOfPages="<%=numberOfPages%>"
                          page="certificate-list-view.jsp"
                          pageNumberParameterName="pageNumber"
                          parameters="<%=paginationValue%>"
                          resourceBundle="org.wso2.carbon.identity.certificateauthority.ui.i18n.Resources"
                          prevKey="prev" nextKey="next"/>
    </form>
</div>
</div>
</fmt:bundle>