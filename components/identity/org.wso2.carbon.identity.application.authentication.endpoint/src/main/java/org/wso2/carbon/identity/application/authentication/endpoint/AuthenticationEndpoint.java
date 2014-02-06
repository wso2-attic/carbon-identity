/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authentication.endpoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationEndpoint extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String loadPage = null;

        String authenticatorsParam = request.getParameter("authenticators");
        
        if (authenticatorsParam != null && !authenticatorsParam.isEmpty()) {
        	String[] authenticators = authenticatorsParam.split(",");
        	
        	for (String authenticator : authenticators) {
        		if (!authenticator.equalsIgnoreCase("BasicAuthenticator")) {
        			request.getRequestDispatcher("phone-number/phone-number.jsp").forward(request, response);
        			break;
        		}
        	}
        	
        	return;
        }
        
        if (request.getRequestURI().contains("retry.do")) {
        	request.getRequestDispatcher("error-pages/retry.jsp").forward(request, response);
        	return;
        }

        if((request.getParameter("type")).equals("samlsso") ){
            loadPage = "samlsso_login.do";
        }
        else if (request.getParameter("type").equals("openid")) {
            loadPage = "openid_login.do";
        }
        else if (request.getParameter("type").equals("passivests")) {
            loadPage = "passivests_login.do";
        }
        else if (request.getParameter("type").equals("oauth2")) {
            loadPage = "oauth2_login.do";
        }

        request.getRequestDispatcher(loadPage).forward(request, response);
    }
}
