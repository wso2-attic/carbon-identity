<%--
  ~ Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~  WSO2 Inc. licenses this file to you under the Apache License,
  ~  Version 2.0 (the "License"); you may not use this file except
  ~  in compliance with the License.
  ~  You may obtain a copy of the License at
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
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@page import="org.wso2.carbon.sts.stub.service.util.xsd.TrustedServiceData" %>
<%@ page import="org.wso2.carbon.sts.ui.STSUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.owasp.encoder.Encode" %>
<jsp:include page="../dialog/display_messages.jsp"/>

<fmt:bundle basename="org.wso2.carbon.sts.ui.i18n.Resources">
    <carbon:breadcrumb
            label="sts.configuration"
            resourceBundle="org.wso2.carbon.sts.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="global-params.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <script type="text/javascript">

        function doValidation() {
            var reason = "";
            reason = validateEmpty("endpointaddrs");
            if (reason != "") {
                CARBON.showWarningDialog("Enter a valid : End-point Address", null, null);
                return false;
            }
            return true;
        }
        function remove(hostName) {
            CARBON.showConfirmationDialog('You are about to remove ' + hostName + '. Do you want to proceed?',
                    function () {
                        location.href = "remove-trusted-service.jsp?endpointaddrs=" + hostName;
                    }, null);
        }

    </script>

    <%
        TrustedServiceData[] services = null;
        String[] aliases = null;
        STSUtil sts = null;
        String address = null;
        String keyAlias = null;
        String cookie = null;
        String serverUrl = null;

        try {
            serverUrl = CarbonUIUtil.getServerURL(config.getServletContext(), session) + "wso2carbon-sts";
            cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
            sts = new STSUtil(config, session, cookie);
            address = (String) request.getParameter("endpointaddrs");
            keyAlias = (String) request.getParameter("alias");
            try {
                sts.addTrustedService(address, keyAlias);
            } catch (Exception e) {
            }
            aliases = sts.getAliasFromPrimaryKeystore();
            try {
                services = sts.getTrustedServices();
            } catch (Exception e) {
                // error already being logged by the STSUtil
            }
        } catch (Exception e) {
    %>
    <script>
        <jsp:forward page="../admin/error.jsp?<%=e.getMessage()%>"/>
    </script>
    <%
            return;
        }
    %>

    <div id="middle">
        <h2><fmt:message key="sts.configuration"/></h2>

        <div id="workArea">
            <form>
                <table class="styledLeft" width="100%" id="trustedServices">
                    <thead>
                    <tr>
                        <th width="40%"><fmt:message key="sts.service.endpoint"/></th>
                        <th colspan="2"><fmt:message key="sts.certificate.alias"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <%
                        if (services != null && services.length > 0) {
                            for (TrustedServiceData service : services) {
                    %>
                    <tr>
                        <td width="40%"><%=Encode.forHtmlContent(service.getServiceAddress())%>
                        </td>
                        <td width="40%">&nbsp;&nbsp;<%=Encode.forHtmlContent(service.getCertAlias())%>
                        </td>
                        <td width="20%">
                            <a title="Remove Trusted RP"
                               onclick="remove('<%=Encode.forJavaScriptAttribute(service.getServiceAddress())%>');return false;"
                               href="#">
                                <img src="images/delete.gif" alt="Remove Trusted Service"/>
                            </a>
                        </td>
                    </tr>
                    <%
                        }
                    } else {
                    %>
                    <tr>
                        <td colspan="3"><i><fmt:message key="sts.trusted.services.not.found"/></i></td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
                <script type="text/javascript">
                    alternateTableRows('trustedServices', 'tableEvenRow', 'tableOddRow');
                </script>
            </form>

            <br/>

            <form method="get" action="index.jsp" name="trustedservice" onsubmit="return doValidation();">
                <table class="styledLeft" width="100%">
                    <thead>
                    <tr>
                        <th><fmt:message key="sts.trusted.services.new"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td class="formRow">
                            <table class="normal" cellspacing="0">
                                <tr>
                                    <td><fmt:message key="sts.endpoint.address"/><font color="red">*</font></td>
                                    <td><input type="text" id="endpointaddrs" name="endpointaddrs"
                                               class="text-box-big"/></td>
                                </tr>
                                <tr>
                                    <td><fmt:message key="sts.certificate.alias"/></td>
                                    <td>
                                        <select id="alias" name="alias">
                                            <%
                                                if (aliases != null) {
                                                    for (String alias : aliases) {
                                                        if (alias != null) {
                                            %>
                                            <option value="<%=Encode.forHtmlAttribute(alias)%>">
                                                <%=Encode.forHtmlContent(alias)%>
                                            </option>
                                            <%
                                                        }
                                                    }
                                                }
                                            %>
                                        </select>
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td class="buttonRow">
                            <input class="button" type="submit" value="<fmt:message key="sts.apply.caption"/>"/>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </form>
        </div>

    </div>
</fmt:bundle>