/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.carbon.identity.entitlement.filter;


import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.entitlement.filter.callback.BasicAuthCallBackHandler;
import org.wso2.carbon.identity.entitlement.filter.callback.EntitlementFilterCallBackHandler;
import org.wso2.carbon.identity.entitlement.filter.exception.EntitlementFilterException;
import org.wso2.carbon.identity.entitlement.proxy.PEPProxy;
import org.wso2.carbon.identity.entitlement.proxy.PEPProxyConfig;
import org.wso2.carbon.identity.entitlement.proxy.exception.EntitlementProxyException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;


public class EntitlementFilter implements Filter {

    private static final Log log = LogFactory.getLog(EntitlementFilter.class);

    private PEPProxy pepProxy;
    private String client;
    private String remoteServiceUrl;
    private String remoteServiceUserName;
    private String remoteServicePassword;
    private String thriftHost;
    private String thriftPort;
    private String reuseSession;
    private String cacheType;
    private int invalidationInterval;
    private int maxCacheEntries;
    private String subjectScope;
    private String subjectAttributeName;
    private String authRedirectURL;

    /**
     * In this init method the required attributes are taken from web.xml, if there are not provided they will be set to default.
     * authRedirectURL attribute have to provided
     */
    @Override
    public void init(FilterConfig filterConfig) throws EntitlementFilterException {


        //This Attributes are mandatory So have to be specified in the web.xml
        authRedirectURL = filterConfig.getInitParameter(EntitlementConstants.AUTH_REDIRECT_URL);
        remoteServiceUserName = filterConfig.getServletContext().getInitParameter(EntitlementConstants.USERNAME);
        remoteServicePassword = filterConfig.getServletContext().getInitParameter(EntitlementConstants.PASSWORD);
        remoteServiceUrl = filterConfig.getServletContext().getInitParameter(EntitlementConstants.REMOTE_SERVICE_URL);

        //This Attributes are not mandatory
        client = filterConfig.getServletContext().getInitParameter(EntitlementConstants.CLIENT);
        if (client == null) {
            client = EntitlementConstants.defaultClient;
        }
        subjectScope = filterConfig.getServletContext().getInitParameter(EntitlementConstants.SUBJECT_SCOPE);
        if (subjectScope == null) {
            subjectScope = EntitlementConstants.defaultSubjectScope;
        }
        subjectAttributeName = filterConfig.getServletContext().getInitParameter(EntitlementConstants.SUBJECT_ATTRIBUTE_NAME);
        if (subjectAttributeName == null) {
            subjectAttributeName = EntitlementConstants.defaultSubjectAttributeName;
        }
        cacheType = filterConfig.getInitParameter(EntitlementConstants.CACHE_TYPE);
        if (cacheType == null) {
            cacheType = EntitlementConstants.defaultCacheType;
        }
        if (filterConfig.getInitParameter(EntitlementConstants.MAX_CACHE_ENTRIES) != null) {
            maxCacheEntries = Integer.parseInt(filterConfig.getInitParameter(EntitlementConstants.MAX_CACHE_ENTRIES));
        } else {
            maxCacheEntries = 0;
        }
        if (filterConfig.getInitParameter(EntitlementConstants.INVALIDATION_INTERVAL) != null) {
            invalidationInterval = Integer.parseInt(filterConfig.getInitParameter(EntitlementConstants.INVALIDATION_INTERVAL));
        } else {
            invalidationInterval = 0;
        }

        if (filterConfig.getInitParameter(EntitlementConstants.THRIFT_HOST) != null) {
            thriftHost = filterConfig.getInitParameter(EntitlementConstants.THRIFT_HOST);
        } else {
            thriftHost = EntitlementConstants.defaultThriftHost;
        }

        if (filterConfig.getInitParameter(EntitlementConstants.THRIFT_PORT) != null) {
            thriftPort = filterConfig.getInitParameter(EntitlementConstants.THRIFT_PORT);
        } else {
            thriftPort = EntitlementConstants.defaultThriftPort;
        }



        Map<String, Map<String, String>> appToPDPClientConfigMap = new HashMap<String, Map<String, String>>();
        Map<String, String> clientConfigMap = new HashMap<String, String>();


        if(client!=null){
            if(client.equals(EntitlementConstants.SOAP)){
                clientConfigMap.put(EntitlementConstants.CLIENT, client);
                clientConfigMap.put(EntitlementConstants.SERVER_URL, remoteServiceUrl);
                clientConfigMap.put(EntitlementConstants.USERNAME, remoteServiceUserName);
                clientConfigMap.put(EntitlementConstants.PASSWORD, remoteServicePassword);
                clientConfigMap.put(EntitlementConstants.REUSE_SESSION, reuseSession);
            } else if (client.equals(EntitlementConstants.BASIC_AUTH)) {
                clientConfigMap.put(EntitlementConstants.CLIENT, client);
                clientConfigMap.put(EntitlementConstants.SERVER_URL, remoteServiceUrl);
                clientConfigMap.put(EntitlementConstants.USERNAME, remoteServiceUserName);
                clientConfigMap.put(EntitlementConstants.PASSWORD, remoteServicePassword);
            }else if (client.equals(EntitlementConstants.THRIFT)) {
                clientConfigMap.put(EntitlementConstants.CLIENT, client);
                clientConfigMap.put(EntitlementConstants.SERVER_URL, remoteServiceUrl);
                clientConfigMap.put(EntitlementConstants.USERNAME, remoteServiceUserName);
                clientConfigMap.put(EntitlementConstants.PASSWORD, remoteServicePassword);
                clientConfigMap.put(EntitlementConstants.REUSE_SESSION, reuseSession);
                clientConfigMap.put(EntitlementConstants.THRIFT_HOST, thriftHost);
                clientConfigMap.put(EntitlementConstants.THRIFT_PORT, thriftPort);
            }else {
                throw new EntitlementFilterException("EntitlementMediator initialization error: Unsupported client");
            }

        }else {
            clientConfigMap.put(EntitlementConstants.SERVER_URL, remoteServiceUrl);
            clientConfigMap.put(EntitlementConstants.USERNAME, remoteServiceUserName);
            clientConfigMap.put(EntitlementConstants.PASSWORD, remoteServicePassword);
        }

        appToPDPClientConfigMap.put("EntitlementMediator", clientConfigMap);
        PEPProxyConfig config = new PEPProxyConfig(appToPDPClientConfigMap, "EntitlementMediator", cacheType, invalidationInterval, maxCacheEntries);

        try {
            pepProxy = new PEPProxy(config);
        } catch (EntitlementProxyException e) {
            throw new EntitlementFilterException("Error while initializing the Entitlement PEP Proxy",e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws EntitlementFilterException {

        String simpleDecision = EntitlementConstants.DENY;
        String userName;
        String action;
        String resource;
        String env = "";

        userName = findUserName((HttpServletRequest) servletRequest, subjectScope, subjectAttributeName);
        resource = findResource((HttpServletRequest) servletRequest);
        action = findAction((HttpServletRequest) servletRequest);

        if (((HttpServletRequest) servletRequest).getRequestURI().contains("/updateCacheAuth.do")) {
            try {
                pepProxy.clear();
            } catch (Exception e) {
                log.error("Error while Making the Decision ", e);
            }

        } else {
            try {
                String decision = pepProxy.getDecision(userName, resource, action, env);
                OMElement decisionElement = AXIOMUtil.stringToOM(decision);
                String namespace = "";
                if (decisionElement.getNamespace().getNamespaceURI() != null) {
                    namespace = decisionElement.getNamespace().getNamespaceURI();
                }
                simpleDecision = decisionElement.getFirstChildWithName(new QName(namespace, "Result")).
                        getFirstChildWithName(new QName(namespace, "Decision")).getText();
            } catch (Exception e) {

                throw new EntitlementFilterException("Exception while making the decision " , e);
            }
        }
        completeAuthorization(simpleDecision, servletRequest, servletResponse, filterChain);
    }

    @Override
    public void destroy() {

        pepProxy = null;
        client = null;
        remoteServiceUrl = null;
        remoteServiceUserName = null;
        remoteServicePassword = null;
        thriftHost = null;
        thriftPort = null;
        reuseSession = null;
        cacheType = null;
        invalidationInterval = 0;
        maxCacheEntries = 0;
        subjectScope = null;
        subjectAttributeName = null;
        authRedirectURL = null;
    }

    private String findUserName(HttpServletRequest request, String subjectScope,
                                String subjectAttributeName) throws EntitlementFilterException {
        String subject;
        if (subjectScope.equals(EntitlementConstants.SESSION)) {
            subject = (String) request.getSession(false).getAttribute(subjectAttributeName);
        } else if (subjectScope.equals(EntitlementConstants.REQUEST_PARAM)) {
            subject = request.getParameter(subjectAttributeName);
        } else if (subjectScope.equals(EntitlementConstants.REQUEST_ATTIBUTE)) {
            subject = (String) request.getAttribute(subjectAttributeName);
        } else if (subjectScope.equals(EntitlementConstants.BASIC_AUTH)) {
            EntitlementFilterCallBackHandler callBackHandler = new BasicAuthCallBackHandler(request);
            subject = callBackHandler.getUserName();
        } else {
            log.error(subjectScope + " is an invalid"
                    + " configuration for subjectScope parameter in web.xml. Valid configurations are"
                    + " \'" + EntitlementConstants.REQUEST_PARAM + "\', " + EntitlementConstants.REQUEST_ATTIBUTE + "\' and \'"
                    + EntitlementConstants.SESSION + "\'");

            throw new EntitlementFilterException(subjectScope + " is an invalid"
                    + " configuration for subjectScope parameter in web.xml. Valid configurations are"
                    + " \'" + EntitlementConstants.REQUEST_PARAM + "\', " + EntitlementConstants.REQUEST_ATTIBUTE + "\' and \'"
                    + EntitlementConstants.SESSION + "\'");
        }
        if (subject == null || "null".equals(subject)) {
            log.error("Username not provided in " + subjectScope);
            throw new EntitlementFilterException("Username not provided in " + subjectScope);
        }
        return subject;
    }

    private String findResource(HttpServletRequest request) {
        return request.getRequestURI();
    }

    private String findAction(HttpServletRequest request) {
        return request.getMethod();
    }

    private void completeAuthorization(String decision, ServletRequest servletRequest,
                                       ServletResponse servletResponse,
                                       FilterChain filterChain)
            throws EntitlementFilterException {
        try {
            if (decision.equals(EntitlementConstants.PERMIT)) {
                if (((HttpServletRequest) servletRequest).getRequestURI().contains("/updateCacheAuth.do")) {
                    pepProxy.clear();
                    log.info("PEP cache has been updated");
                    servletResponse.getWriter().print("PEP cache has been updated");
                } else {
                    filterChain.doFilter(servletRequest, servletResponse);
                }
            } else if (decision.equals(EntitlementConstants.DENY)) {
                log.debug("User not authorized to perform the action");
                servletRequest.getRequestDispatcher(authRedirectURL).
                        forward(servletRequest, servletResponse);
            } else if (decision.equals(EntitlementConstants.NOT_APPLICABLE)) {
                log.debug("No applicable policies found");
                servletRequest.getRequestDispatcher(authRedirectURL).
                        forward(servletRequest, servletResponse);
            } else if (decision.equals(EntitlementConstants.INDETERMINATE)) {
                log.debug("Indeterminate");
                servletRequest.getRequestDispatcher(authRedirectURL).
                        forward(servletRequest, servletResponse);
            } else {
                log.error("Unrecognized decision returned from PDP");
                servletRequest.getRequestDispatcher(authRedirectURL).
                        forward(servletRequest, servletResponse);
            }
        } catch (Exception e) {
            log.error("Error occurred while completing authorization", e);
            throw new EntitlementFilterException("Error occurred while completing authorization", e);
        }
    }

}
