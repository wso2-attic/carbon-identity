/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.sso.saml.query.processors;

import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Response;

/**
 *  Class to handle <AuthnQuery> attribute query type in SAML 2.0
 */
public class AuthnQueryProcessor extends AbstractAttributeQueryProcessor {

    @Override
    protected void processAttributeQuery(RequestAbstractType attributeQuery, Response response) {
        super.processAttributeQuery(attributeQuery, response);

        // do the specific implementation
    }
}
