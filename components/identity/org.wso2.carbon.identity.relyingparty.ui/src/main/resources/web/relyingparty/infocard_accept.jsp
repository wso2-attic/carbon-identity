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
<%@page import="java.util.ArrayList" %>
<%@page
        import="org.wso2.carbon.identity.relyingparty.ui.client.AuthenticationServiceClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.identity.relyingparty.ui.client.RelyingPartyServiceClient" %>
<%@ page import="org.wso2.carbon.identity.relyingparty.ui.util.TokenDecrypter" %>
<%@ page import="org.w3c.dom.Element" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="org.apache.xml.serialize.XMLSerializer" %>
<%@ page import="org.apache.xml.serialize.OutputFormat" %>

<%
    String cssLocation = request.getParameter("css");
    if ("null".equals(cssLocation)) {
        cssLocation = null;
    }

    if (cssLocation != null) {
        cssLocation = URLDecoder.decode(cssLocation, "UTF-8");
    }

    String pageTitle = request.getParameter("title");

    String forwardPage = request.getParameter("forwardPage");
    if (forwardPage != null) {
        forwardPage = URLDecoder.decode(forwardPage, "UTF-8");
        session.setAttribute("forwardPage", forwardPage);
    }

    String xmlToken = request.getParameter("xmlToken");
    String forwardTo = null;
    String BUNDLE = "org.wso2.carbon.identity.relyingparty.ui.i18n.Resources";
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
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = null;

        try {
            RelyingPartyServiceClient client =
                    new RelyingPartyServiceClient(cookie, serverURL, configContext);
            AuthenticationServiceClient authClient = null;
            InfoCardDTO infoCard = new InfoCardDTO();
            InfoCardSignInDTO signin = null;
            infoCard.setXmlToken(stringToken);

            signin = client.signInWithInfoCard(infoCard);

            if (signin.getAuthenticated()) {      
                cookie = client.getCookie();
                AuthenticationServiceClient.setUserInformation(session,cookie,request, signin.getUserID());
                session.setAttribute(CarbonConstants.SERVER_URL, serverURL);
                session.setAttribute("authenticated", signin.getAuthenticated());

                if (forwardPage == null) {
                    forwardTo = "../../carbon/admin/index.jsp?loginStatus=true";
                } else {
                    forwardTo = forwardPage;
                }
            }
        } catch (Exception e) {
            String message = resourceBundle.getString("information.card.not.accepted");
            CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
            forwardTo = "index.jsp?region=region1&item=infocard_sign_in";

            if (cssLocation != null) {
                forwardTo = "index.jsp?forwardPage=" + URLEncoder.encode(forwardPage, "UTF-8") +
                        "&css=" + URLEncoder.encode(cssLocation, "UTF-8") + "&title=" +
                        URLEncoder.encode(pageTitle, "UTF-8");
            }
        }
    } else {
        String message = resourceBundle.getString("information.card.not.accepted");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "index.jsp?region=region1&item=infocard_sign_in";

        if (cssLocation != null) {
            forwardTo = "index.jsp?forwardPage=" + URLEncoder.encode(forwardPage, "UTF-8") +
                    "&css=" + URLEncoder.encode(cssLocation, "UTF-8") + "&title=" +
                    URLEncoder.encode(pageTitle, "UTF-8");
        }
    }
%>


<%@page import="java.util.ResourceBundle" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.wso2.carbon.identity.relyingparty.stub.dto.InfoCardDTO" %>
<%@ page import="org.wso2.carbon.identity.relyingparty.stub.dto.InfoCardSignInDTO" %>
<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>
