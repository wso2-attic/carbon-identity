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
<%@ page import="org.wso2.carbon.sts.stub.service.util.xsd.TrustedServiceData" %>
<%@ page import="org.wso2.carbon.sts.ui.STSUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>

<jsp:include page="../admin/layout/ajaxheader.jsp"/>

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
        sts.removeTrustedService(address);
%>
<script>
    location.href = 'index.jsp';
</script>
<%

} catch (Exception e) {
    CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
%>
<script type="text/javascript">
    location.href = "../admin/error.jsp";
</script>
<%
        return;
    }
%>