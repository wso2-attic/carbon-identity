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
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.stub.Certificate" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.CAConstants" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.client.CAAdminServiceClient" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.util.ClientUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ResourceBundle" %>

<%
    String serialNo;
    Certificate certificate = null;
    String revokedReason = "";
    String forwardTo = null;
    boolean view = false;
    CAAdminServiceClient client = null;
    int revokedReasonVal = 0;

    String viewString = request.getParameter("view");
    serialNo = request.getParameter("serialNo");
    String redirect = request.getParameter("redirect");
    String selectedReason = CharacterEncoder.getSafeText(request.getParameter("selectedReason"));

    if (selectedReason == null || "".equals(selectedReason)) {
        selectedReason = "Unspecified";
    }

    HashMap<String, Integer> reasonList = ClientUtil.getReasonMap();

    if ((viewString != null)) {
        view = Boolean.parseBoolean(viewString);
    }

    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.
                    CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String bundle = "org.wso2.carbon.identity.certificateauthority.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, request.getLocale());

    try {

        if (client == null) {
            client = new CAAdminServiceClient(cookie,
                    serverURL, configContext);
            session.setAttribute(CAConstants.CA_ADMIN_CLIENT, client);
        }

        if (serialNo != null) {
            certificate = client.getCertificateBySerialNo(serialNo);
        }
        if (certificate.getStatus().equals("REVOKED")) {
            revokedReasonVal = client.getRevokeReason(serialNo);
        }
        for (String reason : ClientUtil.getReasonMap().keySet()) {
            if (ClientUtil.getReasonMap().get(reason) == revokedReasonVal) {
                revokedReason = reason;
                break;
            }
        }

    } catch (Exception e) {
        String message = resourceBundle.getString("error.while.performing.advance.search");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/error.jsp";
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
    }
%>


<fmt:bundle basename="org.wso2.carbon.identity.certificateauthority.ui.i18n.Resources">
    <carbon:breadcrumb
            label="add.new.subscriber"
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


        function redirectToList() {
            location.href = 'certificate-list-view.jsp';
        }

        function redirectToCSR(serialNo) {
            location.href = "view-csr.jsp?view=true&serialNo=" + serialNo;
        }
        function getSelectedReason() {
            var comboBox = document.getElementById("selectedReason");
            var selectedReason = comboBox[comboBox.selectedIndex].value;
            location.href = 'certificate-list-view.jsp?selectedReason=' + selectedReason;
        }
        function revokeCertificate() {
            CARBON.showConfirmationDialog("<fmt:message key="revoke.single.certificate"/>", function () {
                document.searchForm.action = "revoke-single-certificate.jsp";
                document.searchForm.submit();
            });
        }


    </script>

    <div id="middle">

        <h2><fmt:message key="cert.dashboard"/></h2>

        <div id="workArea">
            <%
                if (view) {
            %>
            <div class="sectionSub" style="width: 100%">
                <table style="width: 100%" id="certDashboard" cellspacing="0" cellpadding="0" border="0">
                    <tr>
                        <td width="50%">
                            <table style="width: 100%" id="certDetails" class="styledLeft">
                                <thead>
                                <tr>
                                    <th colspan="2"><fmt:message key='cert.details'/></th>

                                </tr>
                                </thead>
                                <tr>
                                    <td style="width: 50%"><fmt:message key='user'/></td>
                                    <td><%=certificate.getUsername()%>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='serial.No'/></td>
                                    <td><%=certificate.getSerialNo()%>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='issued.date'/></td>
                                    <td><%=certificate.getIssuedDate()%>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='expiry.date'/></td>
                                    <td><%=certificate.getExpiaryDate()%>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key='status'/></td>
                                    <td><%=certificate.getStatus()%>
                                    </td>
                                </tr>

                                <tr>
                                    <td><fmt:message key='view.reason'/></td>
                                    <td><%=revokedReason%>
                                    </td>
                                </tr>

                            </table>
                        </td>
                        <td width="10px">&nbsp;</td>
                        <td>
                            <form action="" name="searchForm" method="post">
                                <table style="width: 100%" id="actions" class="styledLeft">
                                    <thead>
                                    <tr>
                                        <th colspan="3"><fmt:message key='action'/></th>
                                        <input type="hidden" name="serialNo" value="<%=certificate.getSerialNo() %>">
                                        <input type="hidden" name="redirect" value="<%=redirect %>">

                                    </tr>
                                    </thead>
                                    <tbody>
                                    <tr>
                                        <td style="border-right: transparent">
                                            <nobr>
                                                <fmt:message key="revoke.reason"/>
                                                <select name="selectedReason" id="selectedReason">
                                                    <%
                                                        if (selectedReason.equals("Unspecified")) {
                                                    %>
                                                    <option value="Unspecified" selected="selected"><fmt:message
                                                            key="unspecified"/></option>
                                                    <%
                                                    } else {
                                                    %>
                                                    <option value="Unspecified"><fmt:message
                                                            key="unspecified"/></option>
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
                                        <td style="width: 10%;border-left: transparent">

                                            <a onclick="revokeCertificate();"
                                               href="#" style="background-image: url(images/up.gif);"
                                               class="icon-link">
                                                <fmt:message key='revoke'/></a>

                                        </td>
                                        <td>
                                            <a href="/ca/certificate/<%=certificate.getSerialNo()%>.crt"
                                               style="background-image: url(images/download.gif);"
                                               class="icon-link">
                                                <fmt:message key='download.certificate'/></a>
                                        </td>
                                    </tr>
                                    </tbody>
                                </table>
                            </form>
                        </td>
                    </tr>
                </table>

            </div>

            <div class="buttonRow">
                <%
                    if (redirect.equals("list")) {
                %>
                <a onclick="redirectToList()" class="icon-link" style="background-image:none;"><fmt:message
                        key="back.to.cert.list"/></a>

                <div style="clear:both"></div>
                <%
                } else if (redirect.equals("csr")) {
                %>
                <a onclick="redirectToCSR('<%=certificate.getSerialNo()%>')" class="icon-link"
                   style="background-image:none;"><fmt:message key="back.to.csr"/></a>

                <div style="clear:both"></div>
                <%
                    } else {

                    }
                %>

            </div>
            <%
                }
            %>

            </form>
        </div>
    </div>
</fmt:bundle>
