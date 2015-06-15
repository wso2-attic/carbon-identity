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
 *
 */

package org.wso2.carbon.identity.sso.agent;

import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.sso.agent.bean.SSOAgentConfig;
import org.wso2.carbon.identity.sso.agent.saml.SSOAgentCarbonX509Credential;
import org.wso2.carbon.user.api.UserStoreException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CarbonSSOAgentFilter extends SSOAgentFilter {

    private static final Logger LOGGER = Logger.getLogger(SSOAgentConstants.LOGGER_NAME);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        SSOAgentConfig config = (SSOAgentConfig) request.getAttribute(
                SSOAgentConstants.CONFIG_BEAN_NAME);
        if (config == null) {
            config = new SSOAgentConfig();
        }
        String tenantDomain = (String) request.getAttribute("tenantDomain");
        int tenantId = 0;
        try {
            tenantId = IdentityTenantUtil.getRealmService().
                    getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            throw new SSOAgentException("Error occurred while retrieving tenant ID " +
                    "from tenant domain", e);
        }
        config.getSAML2().setSSOAgentX509Credential(
                new SSOAgentCarbonX509Credential(tenantId, tenantDomain));

        String htmlPayload = "";
        String htmlFilePath = (String) request.getAttribute(
                SSOAgentConstants.SSOAgentConfig.SAML2.POST_BINDING_REQUEST_HTML_FILE_PATH);
        if (htmlFilePath != null && !htmlFilePath.isEmpty()) {
            File requestPageFile = new File(htmlFilePath);
            Scanner scanner = null;
            if (requestPageFile.exists() && requestPageFile.isFile()) {
                try {
                    htmlPayload = new Scanner(htmlPayload).useDelimiter("\\Z").next();
                } finally {
                    if (scanner != null && scanner.ioException() != null) {
                        IOException ioException = scanner.ioException();
                        LOGGER.log(Level.SEVERE,
                                "Error occurred while reading file " + htmlFilePath +
                                        ". Will be using the default", ioException);
                        htmlPayload = null;
                        scanner.close();
                    }
                }
            } else {
                LOGGER.log(Level.WARNING, "Cannot find file " + htmlFilePath +
                        ". Will be using the default");
            }
        } else {
            LOGGER.log(Level.FINE, "\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2.POST_BINDING_REQUEST_HTML_FILE_PATH +
                    "\' not configured. Will be using the default");
        }
        config.getSAML2().setPostBindingRequestHTMLPayload(htmlPayload);
        request.setAttribute(SSOAgentConstants.CONFIG_BEAN_NAME, config);
        super.doFilter(servletRequest, servletResponse, filterChain);
    }
}
