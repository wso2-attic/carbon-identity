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
package org.wso2.carbon.identity.provider.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.wso2.carbon.identity.provider.openid.servlets.OpenIDProviderServlet;
import org.wso2.carbon.identity.provider.openid.servlets.OpenIDUserServlet;

public class ServletContextListener implements ServiceListener {

    private static final Log log = LogFactory.getLog(ServletContextListener.class);
    private BundleContext bundleContext;

    public ServletContextListener(BundleContext bc) {
        this.bundleContext = bc;
    }

    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED) {
            try {
                ServletContextListener.registerServlets(bundleContext);
            } catch (Exception e) {
                log.error("Failed to initialize the OpenID UI component", e);
            }
        }
    }

    private static void registerServlets(BundleContext bundleContext) {
        Dictionary dictionaryResourceParams = null;
        OpenIDUserServlet userServlet = null;
        OpenIDProviderServlet providerServlet = null;

        // Register OpenIDUserServlet
        dictionaryResourceParams = new Hashtable(1);
        dictionaryResourceParams.put("url-pattern", "/openid");
        userServlet = new OpenIDUserServlet();
        bundleContext.registerService(Servlet.class.getName(), userServlet,
                dictionaryResourceParams);

        // Register OpenIDServerServlet
        dictionaryResourceParams = new Hashtable(1);
        dictionaryResourceParams.put("url-pattern", "/openidserver");
        providerServlet = new OpenIDProviderServlet();
        bundleContext.registerService(Servlet.class.getName(), providerServlet,
                dictionaryResourceParams);
    }

}
