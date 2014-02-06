<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 -->
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.xml.serialize.OutputFormat" %>
<%@ page import="org.apache.xml.serialize.XMLSerializer" %>
<%@ page import="org.openid4java.message.ParameterList" %>
<%@ page import="org.w3c.dom.Element" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>

<%@ page import="org.wso2.carbon.identity.provider.openid.client.OpenIDAdminClient" %>
<%@ page import="org.wso2.carbon.identity.provider.openid.util.TokenDecrypter" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.util.ArrayList" %>

<%
    String openididentity = (String) session.getAttribute("openId");
    String xmlToken = request.getParameter("xmlToken");
    String forwardTo = null;
    String BUNDLE = "org.wso2.carbon.identity.provider.openid.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
    
    if (xmlToken != null) {

        Element element = TokenDecrypter.decryptToken(xmlToken);

        OutputFormat format = new OutputFormat();
        StringWriter sw = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(sw, format);
        serializer.serialize(element);
        String stringToken = sw.toString();

        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = null;
        String isPhishingresistanceEnabled = (String) session.getAttribute("papePhishingResistance");
        String isMultiFactorAuthenticated = (String) session.getAttribute("multiFactorAuth");
        boolean enabledFishingResistance = (isPhishingresistanceEnabled == null || isPhishingresistanceEnabled
                .equals("false")) ? false : true;
        boolean multifactorAuthenticated = (isMultiFactorAuthenticated == null || isMultiFactorAuthenticated
                .equals("false")) ? false : true;

        try {
            OpenIDAdminClient client = new OpenIDAdminClient(configContext, serverURL,null);
            InfoCardDTO infoCard = new InfoCardDTO();
            InfoCardSignInDTO signin = null;
            infoCard.setXmlToken(stringToken);
            infoCard.setOpenId(openididentity);
            signin = client.signInWithInfoCard(infoCard);

            if (signin.getAuthenticated()) {
                boolean isAuthenticated = true;

             if (!enabledFishingResistance) {
                 if (multifactorAuthenticated) {
                    forwardTo = "PAPE_multi_factor_signin.jsp";
%>

<%@page import="java.util.ResourceBundle"%>
<%@ page import="org.wso2.carbon.identity.provider.openid.stub.dto.InfoCardDTO" %>
<%@ page import="org.wso2.carbon.identity.provider.openid.stub.dto.InfoCardSignInDTO" %>
<script type="text/javascript">
    forward();
</script>

<%
            } else {
                isAuthenticated = client.doxmppBasedMultiFactorAuthForInfoCards(signin.getUserID());
                if (!isAuthenticated) {
                    throw new Exception();
                }
            }
        }
        session.setAttribute("infoCardAuthenticated", "true");

    }

} catch (Exception e) {
	String message = resourceBundle.getString("information.card.not.accepted");
    CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
    if (isPhishingresistanceEnabled != null && isPhishingresistanceEnabled.equals("true")) {
        forwardTo = "PAPE_info.jsp";
    } else if (isMultiFactorAuthenticated != null && isMultiFactorAuthenticated.equals("true")) {
        forwardTo = "PAPE_info.jsp";
    }
    if (forwardTo == null) {
        forwardTo = "../admin/login.jsp";
%>
<script type="text/javascript">
    forward();
</script>

<%
        }
    }
} else {
	String message = resourceBundle.getString("information.card.not.accepted");
    CarbonUIMessage.sendCarbonUIMessage(message,CarbonUIMessage.ERROR, request);
    forwardTo = "../admin/login.jsp";
%>
<script type="text/javascript">
    forward();
</script>
<%
    }
%>


<form name="frm" id="frm" method="post" action="openid_profile_view.jsp">
</form>
<script language="JavaScript" type="text/JavaScript">
    <!--
    document.frm.submit();
    -->
</script>

<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

