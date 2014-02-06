/*
*  Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.authenticator.webseal.ui.filters;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 
 */
public class LogOutPageFilter implements Filter {

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        if(!(servletRequest instanceof HttpServletRequest)){
            return;
        }

        if(servletResponse.isCommitted()){
            return;
        }

        String logoutReq = "../web-seal/redirect_ajaxprocessor.jsp";
        RequestDispatcher reqDispatcher = servletRequest.getRequestDispatcher(logoutReq);
        reqDispatcher.forward(servletRequest, servletResponse);

    }

    public void destroy() {
        // This method is not required at the moment
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // This method is not required at the moment
    }

    
}
