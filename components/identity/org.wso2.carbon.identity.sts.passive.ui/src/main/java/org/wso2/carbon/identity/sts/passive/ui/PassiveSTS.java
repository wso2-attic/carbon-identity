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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map.Entry;

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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationRequestCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCache;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCacheKey;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationResult;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.sts.passive.stub.types.RequestToken;
import org.wso2.carbon.identity.sts.passive.stub.types.ResponseToken;
import org.wso2.carbon.identity.sts.passive.ui.cache.SessionDataCache;
import org.wso2.carbon.identity.sts.passive.ui.cache.SessionDataCacheEntry;
import org.wso2.carbon.identity.sts.passive.ui.cache.SessionDataCacheKey;
import org.wso2.carbon.identity.sts.passive.ui.client.IdentityPassiveSTSClient;
import org.wso2.carbon.identity.sts.passive.ui.dto.SessionDTO;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationRequest;

public class PassiveSTS extends HttpServlet {

    private static final Log log = LogFactory.getLog(PassiveSTS.class);

    /**
     *
     */
    private static final long serialVersionUID = 1927253892844132565L;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
                                                                                  IOException {

        if (req.getParameter("sessionDataKey") != null) {
        	handleResponseFromAuthenticationFramework(req, resp);
        } else if ("wsignout1.0".equals(getAttribute(req.getParameterMap(), PassiveRequestorConstants.ACTION))) {
        	handleLogoutRequest(req, resp);
        } else {
        	handleAuthenticationRequest(req, resp);
        }
    }

    private void sendData(HttpServletRequest httpReq, HttpServletResponse httpResp,
            ResponseToken respToken, String action, String authenticatedIdPs)
            throws ServletException, IOException {

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
		
        if (authenticatedIdPs != null && !authenticatedIdPs.isEmpty()) {
            out.println("<input type='hidden' name='AuthenticatedIdPs' value='"
                    + URLEncoder.encode(authenticatedIdPs, "UTF-8") + "'>");
        }
		
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

    private void sendToAuthenticationFramework(HttpServletRequest request,
                                               HttpServletResponse response,
                                               String sessionDataKey,
                                               SessionDTO sessionDTO) throws IOException {

        String commonAuthURL = CarbonUIUtil.getAdminConsoleURL(request);
        commonAuthURL = commonAuthURL.replace(FrameworkConstants.CARBON + "/",
                FrameworkConstants.COMMONAUTH);

        String selfPath = URLEncoder.encode("/" + FrameworkConstants.PASSIVE_STS, "UTF-8");
        //Authentication context keeps data which should be sent to commonAuth endpoint
        AuthenticationRequest authenticationRequest = new
                AuthenticationRequest();
        authenticationRequest.setRelyingParty(sessionDTO.getRealm());
        authenticationRequest.setCommonAuthCallerPath(selfPath);
        authenticationRequest.setForceAuth(false);
        authenticationRequest.setRequestQueryParams(request.getParameterMap());

        //adding headers in out going request to authentication request context
        for (Enumeration e = request.getHeaderNames(); e.hasMoreElements(); ) {
            String headerName = e.nextElement().toString();
            authenticationRequest.addHeader(headerName, request.getHeader(headerName));
        }

        //Add authenticationRequest cache entry to cache
        AuthenticationRequestCacheEntry authRequest = new AuthenticationRequestCacheEntry
                (authenticationRequest);
        FrameworkUtils.addAuthenticationRequestToCache(sessionDataKey, authRequest,
                request.getSession().getMaxInactiveInterval());
	    StringBuilder queryStringBuilder = new StringBuilder();
	    queryStringBuilder.append(commonAuthURL).
	      append("?").
	      append(FrameworkConstants.SESSION_DATA_KEY).
	      append("=").
	      append(sessionDataKey).
	      append("&").
	      append(FrameworkConstants.RequestParams.TYPE).
	      append("=").
	      append(FrameworkConstants.PASSIVE_STS);
	    response.sendRedirect(commonAuthURL + queryStringBuilder.toString());
    }

    private void handleResponseFromAuthenticationFramework(HttpServletRequest request, HttpServletResponse response) 
    																throws ServletException, IOException {
    	
    	String sessionDataKey = request.getParameter(FrameworkConstants.SESSION_DATA_KEY);
    	SessionDTO sessionDTO = getSessionDataFromCache(sessionDataKey);
    	AuthenticationResult authnResult = getAuthenticationResultFromCache(sessionDataKey);
    	
    	if (sessionDTO != null && authnResult != null) {
    		
    		if (authnResult.isAuthenticated()) {
        		process(request, response, sessionDTO, authnResult);
        	}  else {
        		// TODO how to send back the authentication failure to client.
        		//for now user will be redirected back to the framework
        		sendToAuthenticationFramework(request, response, sessionDataKey, sessionDTO);
        	}
    	} else {
    		sendToRetryPage(request, response);
    	}
    }
    
    private void process(HttpServletRequest request, HttpServletResponse response, 
    		SessionDTO sessionDTO, AuthenticationResult authnResult)
    				throws ServletException, IOException {
    	
    	HttpSession session = request.getSession();
    	
    	session.removeAttribute(PassiveRequestorConstants.PASSIVE_REQ_ATTR_MAP);

    	RequestToken reqToken = new RequestToken();
    	
    	Map<ClaimMapping, String>  attrMap = authnResult.getUserAttributes();
    	StringBuffer buffer = null;
    	
        if (attrMap != null && attrMap.size() > 0) {
            buffer = new StringBuffer();
            for (Iterator<Entry<ClaimMapping, String>> iterator = attrMap.entrySet().iterator(); iterator
                    .hasNext();) {
                Entry<ClaimMapping, String> entry = iterator.next();
                buffer.append("{" + entry.getKey().getRemoteClaim().getClaimUri() + "|"
                        + entry.getValue() + "}#CODE#");
            }
        }
    	
        reqToken.setAction(sessionDTO.getAction());
        if (buffer!=null){
            reqToken.setAttributes(buffer.toString());
        }else{
            reqToken.setAttributes(sessionDTO.getAttributes());
        }
        reqToken.setContext(sessionDTO.getContext());
        reqToken.setReplyTo(sessionDTO.getReplyTo());
        reqToken.setPseudo(sessionDTO.getPseudo());
        reqToken.setRealm(sessionDTO.getRealm());
        reqToken.setRequest(sessionDTO.getRequest());
        reqToken.setRequestPointer(sessionDTO.getRequestPointer());
        reqToken.setPolicy(sessionDTO.getPolicy());
        reqToken.setPseudo(session.getId());
        reqToken.setUserName(authnResult.getSubject());

        String serverURL = CarbonUIUtil.getServerURL(session.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) session.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        
        IdentityPassiveSTSClient passiveSTSClient = null;
        passiveSTSClient = new IdentityPassiveSTSClient(serverURL, configContext);

        ResponseToken respToken = passiveSTSClient.getResponse(reqToken);

        if (respToken != null/* && respToken.getAuthenticated() */&& respToken.getResults() != null) {
//        	session.setAttribute("username", userName);
            persistRealms(reqToken, request.getSession());
            sendData(request, response, respToken, reqToken.getAction(),
                    authnResult.getAuthenticatedIdPs());
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
        
        //TODO send logout request to authentication framework
        request.getSession().invalidate();
        
        response.sendRedirect(getAttribute(request.getParameterMap(), PassiveRequestorConstants.REPLY_TO));
    }
    
    private void handleAuthenticationRequest(HttpServletRequest request, HttpServletResponse response) 
    																	throws IOException, ServletException {
    	
    	Map paramMap = request.getParameterMap();
    	
    	SessionDTO sessionDTO = new SessionDTO();
    	sessionDTO.setAction(getAttribute(paramMap, PassiveRequestorConstants.ACTION));
    	sessionDTO.setAttributes(getAttribute(paramMap, PassiveRequestorConstants.ATTRIBUTE));
    	sessionDTO.setContext(getAttribute(paramMap, PassiveRequestorConstants.CONTEXT));
    	sessionDTO.setReplyTo(getAttribute(paramMap, PassiveRequestorConstants.REPLY_TO));
    	sessionDTO.setPseudo(getAttribute(paramMap, PassiveRequestorConstants.PSEUDO));
    	sessionDTO.setRealm(getAttribute(paramMap, PassiveRequestorConstants.REALM));
    	sessionDTO.setRequest(getAttribute(paramMap, PassiveRequestorConstants.REQUEST));
    	sessionDTO.setRequestPointer(getAttribute(paramMap, PassiveRequestorConstants.REQUEST_POINTER));
    	sessionDTO.setPolicy(getAttribute(paramMap, PassiveRequestorConstants.POLCY));
    	sessionDTO.setReqQueryString(request.getQueryString());
    	
    	String sessionDataKey = UUIDGenerator.generateUUID();
    	addSessionDataToCache(sessionDataKey, sessionDTO, request.getSession().getMaxInactiveInterval());
    	
		sendToAuthenticationFramework(request, response, sessionDataKey, sessionDTO);
    }
    
    private void addSessionDataToCache(String sessionDataKey, SessionDTO sessionDTO, int cacheTimeout) {
    	SessionDataCacheKey cacheKey = new SessionDataCacheKey(sessionDataKey);
    	SessionDataCacheEntry cacheEntry = new SessionDataCacheEntry();
		cacheEntry.setSessionDTO(sessionDTO);
		SessionDataCache.getInstance(cacheTimeout).addToCache(cacheKey, cacheEntry);
    }
    
    private SessionDTO getSessionDataFromCache(String sessionDataKey) {
    	SessionDTO sessionDTO = null;
    	SessionDataCacheKey cacheKey = new SessionDataCacheKey(sessionDataKey);
		Object cacheEntryObj = SessionDataCache.getInstance(0).getValueFromCache(cacheKey);
		
		if (cacheEntryObj != null) {
			sessionDTO = ((SessionDataCacheEntry)cacheEntryObj).getSessionDTO();
    	} else {
    		log.error("SessionDTO does not exist. Probably due to cache timeout");
    	}
		
		return sessionDTO;
    }
    
    private void removeSessionDataFromCache(String sessionDataKey) {
    	SessionDataCacheKey cacheKey = new SessionDataCacheKey(sessionDataKey);
		SessionDataCache.getInstance(0).clearCacheEntry(cacheKey);
    }
    
    private AuthenticationResult getAuthenticationResultFromCache(String sessionDataKey) {
    	
    	AuthenticationResultCacheKey authResultCacheKey = new AuthenticationResultCacheKey(sessionDataKey);
		CacheEntry cacheEntry = AuthenticationResultCache.getInstance(0).getValueFromCache(authResultCacheKey);
		AuthenticationResult authResult = null;
		
		if (cacheEntry != null) {
			AuthenticationResultCacheEntry authResultCacheEntry = (AuthenticationResultCacheEntry)cacheEntry;
			authResult = authResultCacheEntry.getResult();
		} else {
			log.error("AuthenticationResult does not exist. Probably due to cache timeout");
		}
		
		return authResult;
    }
    
    private void sendToRetryPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		String redirectURL = CarbonUIUtil.getAdminConsoleURL(request);
        redirectURL = redirectURL.replace("passivests/carbon/", "authenticationendpoint/retry.do");
        response.sendRedirect(redirectURL);
	}
}
