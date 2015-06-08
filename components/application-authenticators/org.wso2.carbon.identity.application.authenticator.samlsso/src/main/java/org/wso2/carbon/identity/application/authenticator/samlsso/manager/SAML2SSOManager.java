/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authenticator.samlsso.manager;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authenticator.samlsso.exception.SAMLSSOException;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface SAML2SSOManager {

    public void init(String tenantDomain, Map<String, String> properties, IdentityProvider idp) throws SAMLSSOException;

    public String buildRequest(HttpServletRequest request, boolean isLogout, boolean isPassive,
                               String loginPage, AuthenticationContext context)
            throws SAMLSSOException;

    public void processResponse(HttpServletRequest request) throws SAMLSSOException;
}
