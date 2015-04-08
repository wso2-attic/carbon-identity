/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth.endpoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;

public class OAuthRequestWrapper extends HttpServletRequestWrapper {
    private MultivaluedMap<String, String> form;
    private Enumeration<String> parameterNames;

    public OAuthRequestWrapper(HttpServletRequest request, MultivaluedMap<String, String> form) {
        super(request);
        this.form = form;

        Set<String> parameterNameSet = new HashSet<String>();
        // Add post parameters
        parameterNameSet.addAll(form.keySet());
        // Add servlet request parameters
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            parameterNameSet.add(parameterNames.nextElement());
        }

        this.parameterNames = Collections.enumeration(parameterNameSet);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (value == null) {
            value = form.getFirst(name);
        }
        return value;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return parameterNames;
    }
}
