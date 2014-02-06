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

package org.wso2.carbon.identity.authenticator.webseal.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

/**
 *
 */
public class WebSealConsumerService  extends HttpServlet {

    public static final Log log = LogFactory.getLog(WebSealConsumerService.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getHeader(WebSealUIAuthenticator.WEBSEAL_USER);

        if(username == null){
            log.error("IV Header is not contained in the WebSeal response");
            throw new ServletException("IV Header is not contained in the WebSeal response");            
        }

        String url = req.getRequestURI();
        url = url.replace("webseal", "carbon/admin/login_action.jsp?username=" +
                                                                    URLEncoder.encode(username));
        RequestDispatcher reqDispatcher = req.getRequestDispatcher(url);
        reqDispatcher.forward(req, resp);
    }
}
