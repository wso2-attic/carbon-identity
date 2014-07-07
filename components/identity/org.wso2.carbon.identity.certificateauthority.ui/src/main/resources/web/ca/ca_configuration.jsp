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

<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.CAConstants" %>
<%@ page import="org.wso2.carbon.identity.certificateauthority.ui.client.CAAdminServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>

<%
    String forwardTo = null;
    String[] listKeyAliases = null;
    CAAdminServiceClient client = null;
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

        if ("changeKS".equals(request.getParameter("action"))) {
            client.changeKeyStore(request.getParameter("selectedKeyAlias"));
        }

        listKeyAliases = client.getListKeyAliases();


    } catch (Exception e) {
        String message = resourceBundle.getString("error.while.loading.configurations");
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
            label="identity.config.details"
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


        function getSelectedKeyAlias() {

            var comboBox = document.getElementById("selectedKeyAlias");
            var selectedReason = comboBox[comboBox.selectedIndex].value;
            /*  location.href = 'certificate-list-view.jsp?selectedReason=' + selectedReason ;*/
        }
        function configure() {
            CARBON.showConfirmationDialog("<fmt:message key="configure.keystore.prompt"/>", function () {
                document.configureForm.submit();
            });
        }

    </script>

    <div id="middle">

    <h2><fmt:message key="identity.config.details"/></h2>

    <div id="workArea">

        <div class="sectionSub" style="width: 100%">
            <table style="border:0; !important">
                <tbody>
                <tr style="border:0; !important">
                    <td></td>

                    <td style="border:0; !important">
                        <nobr>
                            <form method="post" name="configureForm">
                                <fmt:message key="configure.key.alis.pair"/>
                                <input type="hidden" name="action" value="changeKS">
                                <select name="selectedKeyAlias" id="selectedKeyAlias" onchange="getSelectedKeyAlias();">
                                    <%
                                        boolean first = true;
                                        for (String keyAlias : listKeyAliases) {
                                    %>
                                    <option value="<%= keyAlias%>" <%= first ? "selected" : ""%>><%= keyAlias%>
                                    </option>
                                    <%
                                            first = false;
                                        }
                                    %>
                                </select>
                            </form>

                        </nobr>
                    </td>
                    <td>
                        <a onclick="configure();return false;"
                           href="#" style="background-image: url(images/config.gif);"
                           class="icon-link">
                            <fmt:message key='configure'/></a>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>

    </div>
</fmt:bundle>
