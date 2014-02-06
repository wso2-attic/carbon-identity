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
<%@page import="org.apache.axis2.AxisFault"%>
<%@page import="java.util.ResourceBundle"%>
<%@page import="java.net.URLDecoder"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="org.wso2.carbon.identity.relyingparty.ui.client.AuthenticationServiceClient" %>
<%@page import="org.wso2.carbon.identity.relyingparty.ui.client.RelyingPartyServiceClient" %>
<%@page import="org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDConsumer" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@page import="org.wso2.carbon.utils.ServerConstants" %>
<%@page import="java.util.ArrayList" %>
<%@page import="org.wso2.carbon.identity.relyingparty.stub.dto.OpenIDSignInDTO" %>
<%@page import="org.wso2.carbon.identity.relyingparty.stub.dto.OpenIDDTO" %>

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
	if (forwardPage != null && forwardPage.trim().length() > 0) {
		forwardPage = URLDecoder.decode(forwardPage, "UTF-8");
		session.setAttribute("forwardPage", forwardPage);
	}

	String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
	ConfigurationContext configContext =
	                                     (ConfigurationContext) config.getServletContext()
	                                                                  .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
	String forwardTo = null;
	String BUNDLE = "org.wso2.carbon.identity.relyingparty.ui.i18n.Resources";
	ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

	try {
		OpenIDDTO openID =
		                   OpenIDConsumer.getInstance().validateOpenIDAuthentication(request, null);

		String gappDomain = (String) request.getSession().getAttribute("GAppDomain");

		if (gappDomain != null) {
			RelyingPartyServiceClient rpServiceclient =
			                                            new RelyingPartyServiceClient(null, serverURL, configContext);
			try {
				rpServiceclient.signInGAppUser(request, response, openID, gappDomain);
				forwardTo = "../relyingparty/redirect_ajaxprocessor.jsp";
			} catch (Exception e) {
				forwardTo = "../sso-acs/redirect_ajaxprocessor.jsp";
				throw e;
			}
		
		} else {

			RelyingPartyServiceClient client =
			                                   new RelyingPartyServiceClient(null, serverURL, configContext);
			OpenIDSignInDTO signin = client.signInWithOpenID(openID);

			if (signin.getAuthenticated()) {
				String cookie = client.getCookie();
				AuthenticationServiceClient.setUserInformation(session, cookie, request,
				                                               signin.getUserID());
				
				session.setAttribute(CarbonConstants.SERVER_URL, serverURL);
				session.setAttribute("authenticated", signin.getAuthenticated());
				
				if (forwardPage == null) {
					forwardTo = "../../carbon/admin/index.jsp?loginStatus=true";
				} else {
					forwardTo = forwardPage;
				}
				
			} else {
				session.setAttribute("openId", openID);
				session.setAttribute("openIdURL", openID.getOpenID());
				forwardTo = "associate_openid.jsp";

				if (forwardPage != null) {
					forwardTo =
					            forwardTo + "?forwardPage=" +
					                    URLEncoder.encode(forwardPage, "UTF-8") + "&css=" +
					                    URLEncoder.encode(cssLocation, "UTF-8") +
					                    "&title=" + URLEncoder.encode(pageTitle, "UTF-8");
				}
			}
		}
	} catch (Exception e) {
		String message =
		                 resourceBundle.getString("openid.not.accepted") + "." +
		                         e.getMessage();
		CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
		if (forwardTo == null) {
			forwardTo = "index.jsp?region=region1&item=infocard_sign_in";
		}

		if (forwardPage != null && forwardPage.trim().length() > 0) {
			forwardTo =
			            "index.jsp?forwardPage=" + URLEncoder.encode(forwardPage, "UTF-8") +
			                    "&css=" + URLEncoder.encode(cssLocation, "UTF-8") +
			                    "&title=" + URLEncoder.encode(pageTitle, "UTF-8");
		}
	}
%>

<jsp:forward page="<%=forwardTo%>"/>

<%@page import="java.util.ResourceBundle" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.wso2.carbon.ui.util.CarbonUIAuthenticationUtil" %>

