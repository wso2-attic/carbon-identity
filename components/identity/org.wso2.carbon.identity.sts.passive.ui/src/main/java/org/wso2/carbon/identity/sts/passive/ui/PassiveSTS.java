/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.sts.passive.ui;


import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.sts.passive.stub.types.RequestToken;
import org.wso2.carbon.identity.sts.passive.stub.types.ResponseToken;
import org.wso2.carbon.identity.sts.passive.ui.client.IdentityPassiveSTSClient;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.ui.CarbonUIUtil;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PassiveSTS extends HttpServlet {

    private static final Log log = LogFactory.getLog(PassiveSTS.class);

    /**
     *
     */
    private static final long serialVersionUID = 1927253892844132565L;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
                                                                                  IOException {

        if (req.getAttribute("commonAuthAuthenticated") != null) {
        	handleResponseFromAuthenticationFramework(req, resp);
        } else if ("wsignout1.0".equals(getAttribute(req.getParameterMap(), PassiveRequestorConstants.ACTION))) {
        	handleLogoutRequest(req, resp);
        } else {
        	handleAuthenticationRequest(req, resp);
        }
    }

    private void sendData(HttpServletRequest httpReq, HttpServletResponse httpResp,
                          ResponseToken respToken, String action)
            throws ServletException,
                   IOException {

//        String responseTokenResult = respToken.getResults();
//        
//        if (responseTokenResult == null) {
//        	httpResp.sendRedirect(frontEndUrl + "passivests_login.do");
//        	return;
//        }

        PrintWriter out = httpResp.getWriter();
		out.println("<html>");
		out.println("<body>");
		out.println("<form method='post' action='" + respToken.getReplyTo() + "'>");
		out.println("<p>");
		out.println("<input type='hidden' name='wa' value='" + action + "'>");
		out.println("<input type='hidden' name='wresult' value='" + respToken.getResults() + "'>");
		out.println("<input type='hidden' name='wctx' value='" + respToken.getContext() + "'>");
		out.println("<button type='submit'>POST</button>");
		out.println("</p>");
		out.println("</form>");
		out.println("<script type='text/javascript'>");
		out.println("document.forms[0].submit();");
		out.println("</script>");
		out.println("</body>");
		out.println("</html>");	
		
		return;
    }

    private String getAttribute(Map paramMap, String name) {
        if (paramMap.get(name) != null && paramMap.get(name) instanceof String[]) {
            return ((String[]) paramMap.get(name))[0];
        }
        return null;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    private String getAdminConsoleURL(HttpServletRequest request) {
        String url = CarbonUIUtil.getAdminConsoleURL(request);
        if (url.indexOf("/passivests/") != -1) {
            url = url.replace("/passivests", "");
        }
        url = url.replace("carbon/", "authenticationendpoint/");
        return url;
    }

    private void openURLWithNoTrust(String realm) throws IOException {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }};

        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory defaultSSLSocketFactory =
                    HttpsURLConnection.getDefaultSSLSocketFactory();
            HostnameVerifier defaultHostnameVerifier =
                    HttpsURLConnection.getDefaultHostnameVerifier();
            String renegotiation = System.getProperty("sun.security.ssl.allowUnsafeRenegotiation");
            try {
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(hv);
                System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
                new URL(realm).getContent();
            } finally {
                HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSocketFactory);
                HttpsURLConnection.setDefaultHostnameVerifier(defaultHostnameVerifier);
                System.getProperty("sun.security.ssl.allowUnsafeRenegotiation", renegotiation);
            }
        } catch (Exception ignore) {
        }
    }

    private void persistRealms(RequestToken reqToken, HttpSession session) {
        Set<String> realms = (Set<String>) session.getAttribute("realms");
        if (realms == null) {
            realms = new HashSet<String>();
            session.setAttribute("realms", realms);
        }
        realms.add(reqToken.getRealm());
    }
    
    private void sendToAuthenticationFramework(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
    	String sessionDataKey = UUIDGenerator.generateUUID();
        String commonAuthURL = CarbonUIUtil.getAdminConsoleURL(request);
        commonAuthURL = commonAuthURL.replace("carbon/", "commonauth");

        String selfPath = URLEncoder.encode("../../passivests","UTF-8");

        String queryParams = "?" + request.getQueryString() + 
                "&sessionDataKey=" + sessionDataKey +
                "&type=passivests" +
                "&commonAuthCallerPath=" + selfPath +
                "&forceAuthenticate=false";

        response.sendRedirect(commonAuthURL + queryParams);
    }
    
    private void handleResponseFromAuthenticationFramework(HttpServletRequest request, HttpServletResponse response) 
    																throws ServletException, IOException {
    	if (((Boolean)request.getAttribute("commonAuthAuthenticated")).booleanValue()) {
    		request.getSession().setAttribute("passive-sts-username", request.getAttribute("authenticatedUser"));
    		process(request, response);
    	} else {
    		// TODO how to send back the authentication failure to client
    	}
    }
    
    private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	HttpSession session = request.getSession();
    	
    	session.removeAttribute(PassiveRequestorConstants.PASSIVE_REQ_ATTR_MAP);

    	RequestToken reqToken = new RequestToken();
    	
        reqToken.setAction((String) session.getAttribute(PassiveRequestorConstants.ACTION));
        reqToken.setAttributes((String) session.getAttribute(PassiveRequestorConstants.ATTRIBUTE));
        reqToken.setContext((String) session.getAttribute(PassiveRequestorConstants.CONTEXT));
        reqToken.setReplyTo((String) session.getAttribute(PassiveRequestorConstants.REPLY_TO));
        reqToken.setPseudo((String) session.getAttribute(PassiveRequestorConstants.PSEUDO));
        reqToken.setRealm((String) session.getAttribute(PassiveRequestorConstants.REALM));
        reqToken.setRequest((String) session.getAttribute(PassiveRequestorConstants.REQUEST));
        reqToken.setRequestPointer((String) session.getAttribute(PassiveRequestorConstants.REQUEST_POINTER));
        reqToken.setPolicy((String) session.getAttribute(PassiveRequestorConstants.POLCY));
        reqToken.setPseudo(session.getId());
        reqToken.setUserName((String)session.getAttribute("passive-sts-username"));

        String serverURL = CarbonUIUtil.getServerURL(session.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) session.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        
        IdentityPassiveSTSClient passiveSTSClient = null;
        passiveSTSClient = new IdentityPassiveSTSClient(serverURL, configContext);

        ResponseToken respToken = passiveSTSClient.getResponse(reqToken);

        if (respToken != null/* && respToken.getAuthenticated() */&& respToken.getResults() != null) {
//        	session.setAttribute("username", userName);
            persistRealms(reqToken, request.getSession());
            sendData(request, response, respToken, reqToken.getAction());
        } /*else {
                resp.sendRedirect(frontEndUrl + "passivests_login.do");
                return;
        }*/
    }
    
    private void handleLogoutRequest(HttpServletRequest request, HttpServletResponse response) throws IOException{
    	
    	Set<String> realms = (Set<String>) request.getSession().getAttribute("realms");
    	
        if (realms != null && realms.size() > 0) {
            for (String realm : realms) {
                openURLWithNoTrust(realm + "?wa=wsignoutcleanup1.0");
            }
        }
        request.getSession().invalidate();
        
        response.sendRedirect(getAttribute(request.getParameterMap(), PassiveRequestorConstants.REPLY_TO));
    }
    
    private void handleAuthenticationRequest(HttpServletRequest request, HttpServletResponse response) 
    																	throws IOException, ServletException {
    	
    	Map paramMap = request.getParameterMap();
    	HttpSession session = request.getSession();
    	
    	session.setAttribute(PassiveRequestorConstants.ACTION, getAttribute(paramMap, PassiveRequestorConstants.ACTION));
    	session.setAttribute(PassiveRequestorConstants.ATTRIBUTE, getAttribute(paramMap, PassiveRequestorConstants.ATTRIBUTE));
    	session.setAttribute(PassiveRequestorConstants.CONTEXT, getAttribute(paramMap, PassiveRequestorConstants.CONTEXT));
    	session.setAttribute(PassiveRequestorConstants.REPLY_TO, getAttribute(paramMap, PassiveRequestorConstants.REPLY_TO));
    	session.setAttribute(PassiveRequestorConstants.PSEUDO, getAttribute(paramMap, PassiveRequestorConstants.PSEUDO));
    	session.setAttribute(PassiveRequestorConstants.REALM, getAttribute(paramMap, PassiveRequestorConstants.REALM));
    	session.setAttribute(PassiveRequestorConstants.REQUEST, getAttribute(paramMap, PassiveRequestorConstants.REQUEST));
    	session.setAttribute(PassiveRequestorConstants.REQUEST_POINTER, getAttribute(paramMap, PassiveRequestorConstants.REQUEST_POINTER));
    	session.setAttribute(PassiveRequestorConstants.POLCY, getAttribute(paramMap, PassiveRequestorConstants.POLCY));
		
		if (request.getSession().getAttribute("passive-sts-username") == null) {
			sendToAuthenticationFramework(request, response);
		} else {
			process(request, response);
		}
    }
}
