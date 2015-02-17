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

package org.wso2.carbon.sso.redirector.ui.internal;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This class acts as a servlet filter which forwads the requests coming for sso-acs/redirect_ajaxprocessor.jsp
 * to stratos-auth/redirect_ajaxprocessor.jsp
 */
public class RedirectorJSPFilter implements Filter {

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest)) {
            return;
        }
        String url = ((HttpServletRequest) servletRequest).getRequestURI();
        if (url.contains("//")) {
            url = url.replace("//", "/");
        }
        url = url.replace("sso-acs/redirect_ajaxprocessor.jsp", "stratos-auth/redirect_ajaxprocessor.jsp");
        RequestDispatcher requestDispatcher =
                servletRequest.getRequestDispatcher(url);
        requestDispatcher.forward(servletRequest, servletResponse);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // not required to implement
    }

    public void destroy() {
        // not required to implement
    }
}
