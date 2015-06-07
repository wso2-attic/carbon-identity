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

package org.wso2.carbon.identity.oauth2.authcontext;

import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;

import java.util.SortedMap;

/**
 * This interface encapsulates a user claims retriever.
 * The retrieved claims are encoded to the token during subscriber validation
 * in the order defined by the SortedMap.
 * Anyone trying to add custom user properties to the token should implement this interface
 * and mention the fully qualified class name in identity.xml ->
 * OAuth -> TokenGeneration -> ClaimsRetrieverImplClass
 */
public interface ClaimsRetriever {

    /**
     * Initialization method that runs only once.
     *
     * @throws IdentityOAuth2Exception
     */
    public void init() throws IdentityOAuth2Exception;

    /**
     * Method that retrieves user claims
     *
     * @param requestedClaims TODO
     * @return a sorted map
     * keys - claimURIs
     * values - claim values.
     * @throws IdentityOAuth2Exception
     */
    public SortedMap<String, String> getClaims(String endUserName, String[] requestedClaims) throws IdentityOAuth2Exception;

    /**
     * Method that retrieves user defaultClaims
     *
     * @return a String[]
     * @throws IdentityOAuth2Exception
     */
    public String[] getDefaultClaims(String endUserName) throws IdentityOAuth2Exception;

}
