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
<%@page import="org.wso2.carbon.ui.util.CharacterEncoder"%>
<%@page import="org.wso2.carbon.identity.base.IdentityConstants"%>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page
        import="org.wso2.carbon.identity.relyingparty.ui.client.RelyingPartyServiceClient" %>
<%@page import="org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDAuthenticationRequest" %>
<%@page
        import="org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDConsumer" %>
<%@page
        import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.ResourceBundle" %>

<%
    String cssLocation = request.getParameter("css");
    if ("null".equals(cssLocation)) {
        cssLocation = null;
    }

    if (cssLocation != null) {
        cssLocation = URLDecoder.decode(cssLocation, "UTF-8");
    }

    String pageTitle = request.getParameter("title");
    if (pageTitle != null) {
        pageTitle = URLDecoder.decode(pageTitle, "UTF-8");
    }

    String forwardPage = request.getParameter("forwardPage");
    if (forwardPage != null) {
        forwardPage = URLDecoder.decode(forwardPage, "UTF-8");
        session.setAttribute("forwardPage", forwardPage);
    }

    String forwardTo = null;
    String BUNDLE = "org.wso2.carbon.identity.relyingparty.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    try {
        String dialect = null;
        String openid = CharacterEncoder.getSafeText(request.getParameter("openIdUrl"));
        
        if(openid == null) {
            openid = request.getParameter("gAppDomainOpenId");
       		request.getSession().setAttribute("GAppDomain", openid);
       		dialect = IdentityConstants.OPENID_AX_DIALECT;
        }

        openid = openid.trim();

        // creating RelyingPartyService client
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(
                        CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        RelyingPartyServiceClient client =
                new RelyingPartyServiceClient(cookie, serverURL, configContext);

        // creating authentication request for the given openid
        OpenIDAuthenticationRequest openIDAuthRequest = client.getOpenIDAuthInfo(request, response, dialect);
        openIDAuthRequest.setOpenIDUrl(openid);
        
        // computing the return to URL		
        String returnUrl = OpenIDConsumer.getInstance().getAdminConsoleURL(request) +
                "relyingparty/openid_accept.jsp";

        if (cssLocation != null) {
            returnUrl = returnUrl + "?forwardPage=" + URLEncoder.encode(forwardPage, "UTF-8") +
                    "&css=" + URLEncoder.encode(cssLocation, "UTF-8") + "&title=" +
                    URLEncoder.encode(pageTitle, "UTF-8");
        }
        openIDAuthRequest.setReturnUrl(returnUrl);
        
        // get the URL encoded authentication request message and forward to OP
        forwardTo = OpenIDConsumer.getInstance().doOpenIDAuthentication(openIDAuthRequest);
        response.sendRedirect(forwardTo);
    } catch (Exception e) {
        String message = resourceBundle.getString("error")  + "! " + e.getMessage() ;
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "index.jsp?region=region1&item=infocard_sign_in";

        if (forwardPage != null) {
            forwardTo = "index.jsp?forwardPage=" + URLEncoder.encode(forwardPage, "UTF-8") +
                    "&css=" + URLEncoder.encode(cssLocation, "UTF-8") + "&title=" +
                    URLEncoder.encode(pageTitle, "UTF-8");
        }
    }
%>


<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>
