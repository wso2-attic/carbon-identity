/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.application.authentication.endpoint;

import org.wso2.carbon.identity.application.authentication.endpoint.util.CharacterEncoder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationEndpoint extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        try {
            String loadPage = null;
            String hrdParam = CharacterEncoder.getSafeText(request.getParameter("hrd"));
            Map<String, String> idpAuthenticatorMapping = new HashMap<String, String>();

            String authenticators = CharacterEncoder.getSafeText(request.getParameter("authenticators"));

            if (authenticators != null) {

                String[] authenticatorIdPMappings = authenticators.split(";");
                for (String authenticatorIdPMapping : authenticatorIdPMappings) {
                    String[] authenticatorIdPMapArr = authenticatorIdPMapping.split(":");
                    for (int i = 1; i < authenticatorIdPMapArr.length; i++) {
                        if (idpAuthenticatorMapping.containsKey(authenticatorIdPMapArr[i])) {
                            idpAuthenticatorMapping.put(authenticatorIdPMapArr[i],
                                    idpAuthenticatorMapping.get(authenticatorIdPMapArr[i]) + "," + authenticatorIdPMapArr[0]);
                        } else {
                            idpAuthenticatorMapping.put(authenticatorIdPMapArr[i], authenticatorIdPMapArr[0]);
                        }
                    }
                }
            }

            if (idpAuthenticatorMapping != null) {
                request.setAttribute("idpAuthenticatorMap", idpAuthenticatorMapping);
            }

            if (hrdParam != null && "true".equalsIgnoreCase(hrdParam)) {
                request.getRequestDispatcher("domain.jsp").forward(request, response);
                return;
            }

            if (request.getRequestURI().contains("retry.do")) {
                request.getRequestDispatcher("retry.jsp").forward(request, response);
                return;
            }

            if ((request.getParameter("type")).equals("samlsso")) {
                loadPage = "samlsso_login.do";
            } else if (request.getParameter("type").equals("openid")) {
                loadPage = "openid_login.do";
            } else if (request.getParameter("type").equals("passivests")) {
                loadPage = "passivests_login.do";
            } else if (request.getParameter("type").equals("oauth2") || request.getParameter("type").equals("oidc")) {
                loadPage = "oauth2_login.do";
            }
            else if(request.getParameter("type").equals("fido")){
                loadPage = "authentication.jsp";
            }
            request.getRequestDispatcher(loadPage).forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AuthenticationException(e);
        }

    }
}
