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

package org.wso2.carbon.identity.authenticator.webseal.ui.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.authenticator.webseal.ui.Utils;
import org.wso2.carbon.identity.authenticator.webseal.ui.WebSealConsumerService;
import org.wso2.carbon.identity.authenticator.webseal.ui.WebSealUIAuthenticator;
import org.wso2.carbon.identity.authenticator.webseal.ui.filters.LogOutPageFilter;
import org.wso2.carbon.ui.CarbonUIAuthenticator;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @scr.component name="webseal.authenticator.ui.dscomponent" immediate="true"
 */
public class Activator implements BundleActivator {

    private static final Log log = LogFactory.getLog(Activator.class);

    public void start(BundleContext bc) {

        WebSealUIAuthenticator authenticator = new WebSealUIAuthenticator();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(CarbonConstants.AUTHENTICATOR_TYPE, authenticator.getAuthenticatorName());
        bc.registerService(CarbonUIAuthenticator.class.getName(), authenticator, props);

        //register log-out filter
        Utils.initConfig();
        HttpServlet logOutServlet = new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

            }
        };

        Filter logOutPageFilter = new LogOutPageFilter();
        Dictionary logOutPageFilterProps = new Hashtable(2);
        Dictionary redirectorParams = new Hashtable(3);

        redirectorParams.put("url-pattern", Utils.getLogOutPage());

        redirectorParams.put("associated-filter", logOutPageFilter);
        redirectorParams.put("servlet-attributes", logOutPageFilterProps);
        bc.registerService(Servlet.class.getName(), logOutServlet, redirectorParams);


        //Register the SSO Assertion Consumer Service Servlet
        HttpServlet acsServlet = new WebSealConsumerService();
        Dictionary acsParams = new Hashtable(2);
        acsParams.put("url-pattern","/webseal");
        acsParams.put("display-name", "WebSeal Consumer Service");
        bc.registerService(Servlet.class.getName(), acsServlet, acsParams);

        log.info("WebSeal Authenticator FE Bundle activated successfully.");

    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }

}
