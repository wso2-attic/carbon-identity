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
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>


<%
    String forwardTo = "view-csr.jsp?view=true&serialNo=";
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.
                    CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CAAdminServiceClient client = (CAAdminServiceClient) session.getAttribute(CAConstants.CA_ADMIN_CLIENT);

    if (client == null) {
        client = new CAAdminServiceClient(cookie, serverURL, configContext);
        session.setAttribute(CAConstants.CA_ADMIN_CLIENT, client);
    }
    String serialNo = "";

    String action = request.getParameter("action");
    if ("sign".equals(action)) {
        serialNo = request.getParameter("serial");
        int validity = Integer.parseInt(request.getParameter("validity"));
        client.sign(serialNo, validity);
    } else if ("reject".equals(action)) {
        serialNo = request.getParameter("serial");
        client.rejectCSR(serialNo);
    }
    //todo: fix url for redirect
    //  response.sendRedirect("/carbon/ca/csr-list-view.jsp");
%>
<script
        type="text/javascript">

    function forward() {
        location.href = "<%=forwardTo%><%=serialNo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>