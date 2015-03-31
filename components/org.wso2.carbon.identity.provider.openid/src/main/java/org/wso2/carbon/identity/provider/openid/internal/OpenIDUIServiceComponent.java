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
package org.wso2.carbon.identity.provider.openid.internal;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;

/**
 * @scr.component name="identity.provider.openid.ui.component" immediate="true"
 * 
 */
public class OpenIDUIServiceComponent {

    private static final Log log = LogFactory.getLog(OpenIDUIServiceComponent.class);

    /**
     * 
     * @param ctxt
     */
    protected void activate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("Starting OpenID Provider UI bundle");
        }

        try {
            String filter = "(objectclass=" + ServletContext.class.getName() + ")";
            ctxt.getBundleContext().addServiceListener(
                    new ServletContextListener(ctxt.getBundleContext()), filter);

        } catch (Throwable e) {
            log.error("Failed to activate ProfileMgt bundle", e);
        }
    }
    /**
     * 
     * @param ctxt
     */
    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("OpenID Provider UI bundle is deactivated");
        }
    }

}
