/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth2.validators;

import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;

import java.util.Set;

/**
 * This class should be extended by all OAuth2 Scope Validators.
 */
public abstract class OAuth2ScopeValidator {

    protected Set<String> scopesToSkip;

    /**
     * Method to validate the scopes associated with the access token against the resource that is being accessed.
     *
     * @param accessTokenDO - The access token data object
     * @param resource      - The resource that is being accessed.
     * @return - true if scope is valid, false otherwise
     * @throws IdentityOAuth2Exception
     */
    public abstract boolean validateScope(AccessTokenDO accessTokenDO, String resource) throws IdentityOAuth2Exception;

    public Set<String> getScopesToSkip() {
        return scopesToSkip;
    }

    public void setScopesToSkip(Set<String> scopesToSkip) {
        this.scopesToSkip = scopesToSkip;
    }

}
