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
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.claim.mgt.stub.dto.ClaimDialectDTO" %>
<%@ page import="org.wso2.carbon.claim.mgt.stub.dto.ClaimMappingDTO" %>

<%@page import="org.wso2.carbon.claim.mgt.ui.client.ClaimAdminClient" %>

<%@page import="org.wso2.carbon.identity.sts.passive.stub.types.ClaimDTO" %>
<%@ page import="org.wso2.carbon.identity.sts.passive.ui.client.IdentityPassiveSTSClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<jsp:include page="../dialog/display_messages.jsp"/>

<fmt:bundle basename="org.wso2.carbon.identity.sts.mgt.ui.i18n.Resources">
<carbon:breadcrumb
        label="sts.configuration"
        resourceBundle="org.wso2.carbon.identity.sts.mgt.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<script type="text/javascript" src="global-params.js"></script>
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<script type="text/javascript">

    function removePassiveSTSTrustService(realm) {
        CARBON.showConfirmationDialog("<fmt:message key='remove.message1'/>" + realm + "<fmt:message key='remove.message2'/>",
                                      function() {
                                          location.href = "remove-passive-sts-trusted-service.jsp?realmName=" + realm;
                                      }, null);
    }
    function alternateTableRowsOnlyForOuterTable(id, evenStyle, oddStyle) {
        if (document.getElementsByTagName) {
            if (document.getElementById(id)) {
                var table = document.getElementById(id);
                var rows = table.rows;
                for (var i = 0; i < rows.length; i++) {
                    //manipulate rows
                    if (i % 2 == 0) {
                        rows[i].className = evenStyle;
                    } else {
                        rows[i].className = oddStyle;
                    }
                }
            }
        }
    }

</script>

<%
    String addAction = "add-passive-sts-trusted-service.jsp";
    String cookie = null;
    IdentityPassiveSTSClient passiveSTSClient = null;
    ClaimDTO[] trustedServices = null;
    try {
        cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        ConfigurationContext configContext = (ConfigurationContext) session.getServletContext()
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String backendURL = CarbonUIUtil.getServerURL(session.getServletContext(), session);
        passiveSTSClient = new IdentityPassiveSTSClient(backendURL, configContext);
        trustedServices = passiveSTSClient.getAllTrustedServices();

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
    <h2><fmt:message key="passive.sts.configuration"/></h2>

    <div style="height:30px;">
        <a href="javascript:document.location.href='<%=addAction%>'" class="icon-link"
           style="background-image:url(../admin/images/add.gif);"><fmt:message
                key='sts.trusted.services.new'/></a>
    </div>

    <div id="workArea">
        <br/>

        <form>
            <table class="styledLeft" width="100%" id="trustedServicesForPassiveSTS">
                <thead>
                <tr>
                    <th><fmt:message key="passive.sts.service.realms"/></th>
                    <th colspan="2"><fmt:message key="passive.sts.default.claims"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (trustedServices != null && trustedServices.length > 0) {
                        for (ClaimDTO claimDTO : trustedServices) {
                %>
                <tr>
                    <td><%=claimDTO.getRealm()%>
                    </td>
                    <td>
                        <table class="normal" style="width:100%">
                            <%
                                for (String claim : claimDTO.getDefaultClaims()) {
                            %>
                            <tr>
                                <td><%=claim%>
                                </td>
                            </tr>
                            <%
                                }
                            %>
                        </table>
                    </td>
                    <td width="20px">
                        <a title="Remove Trusted RP"
                           onclick="removePassiveSTSTrustService('<%=claimDTO.getRealm()%>');return false;"
                           href="#" class="icon-link"
                           style="background-image:url(../admin/images/delete.gif)">Delete
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
                alternateTableRowsOnlyForOuterTable('trustedServicesForPassiveSTS', 'tableEvenRow', 'tableOddRow');
            </script>
        </form>
    </div>
</div>
</fmt:bundle>