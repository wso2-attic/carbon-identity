<%--
  ~ Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
  --%>
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.sts.passive.ui.client.IdentityPassiveSTSClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%
    String realmName = request.getParameter("realmName");
    String[] claims = request.getParameterValues("claims");
    String claimDialect = request.getParameter("dialect");
    if (realmName != null && claims != null && (claims.length > 0)) {
        String claimsString = "";
        for (String claim : claims) {
            claimsString = claimsString + claim + ",";
        }
        ConfigurationContext configContext = (ConfigurationContext) session.getServletContext()
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String backendURL = CarbonUIUtil.getServerURL(session.getServletContext(), session);
        try {
            IdentityPassiveSTSClient passiveSTSClient = new IdentityPassiveSTSClient(backendURL, configContext);
            passiveSTSClient.addTrustedService(realmName, claimDialect, claimsString);
        } catch (AxisFault axisFault) {
%>
<script>
    CARBON.showErrorDialog("Failed to add trusted service", function() {
        location.href = 'passive-sts.jsp';
    }, null);
</script>
<%
        }
    }
%>
<script>
    location.href = 'passive-sts.jsp';
</script>