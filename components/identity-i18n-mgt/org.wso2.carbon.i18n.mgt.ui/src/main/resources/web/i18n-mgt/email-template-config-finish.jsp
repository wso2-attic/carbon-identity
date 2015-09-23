<!--
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
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<jsp:include page="../dialog/display_messages.jsp"/>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.i18n.mgt.ui.EmailConfigDTO" %>
<%@ page import="org.wso2.carbon.i18n.mgt.dto.xsd.EmailTemplateDTO" %>
<%@ page import="org.wso2.carbon.i18n.mgt.ui.I18nEmailMgtConfigServiceClient" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    String emailSubject = request.getParameter("emailSubject");
    String emailBody = request.getParameter("emailBody");
    String emailFooter = request.getParameter("emailFooter");
    String templateName = request.getParameter("templateName");

    EmailConfigDTO emailConfig = null;
    EmailTemplateDTO templateChanged = new EmailTemplateDTO();

    if (StringUtils.isNotBlank(emailSubject)) {
        templateChanged.setSubject(emailSubject);
    }
    if (StringUtils.isNotBlank(emailBody)) {
        templateChanged.setBody(emailBody);
    }
    if (StringUtils.isNotBlank(emailFooter)) {
        templateChanged.setFooter(emailFooter);
    }
    if (StringUtils.isNotBlank(templateName)) {
        templateChanged.setName(templateName);
    }

    try {
        String cookie = (String) session
                .getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(),
                session);
        ConfigurationContext configContext = (ConfigurationContext) config
                .getServletContext()
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        I18nEmailMgtConfigServiceClient configClient =
                new I18nEmailMgtConfigServiceClient(cookie, backendServerURL, configContext);
        configClient.saveEmailConfig(templateChanged);
%>
<script type="text/javascript">
    location.href = "email-template-config.jsp";
</script>
<%
} catch (Exception e) {
    CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR,
            request);
%>
<script type="text/javascript">
    location.href = "email-template-config.jsp";
</script>
<%
        return;
    }
%>


