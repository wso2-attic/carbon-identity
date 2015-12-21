/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.ldap.server.util;

import org.wso2.carbon.identity.base.IdentityException;

/**
 * This is to differentiate the exceptional events happen in wso2/carbon/apacheds-server package's
 * code (in the process of embedding LDAP to carbon server) from others' code such as ApacheDS's.
 */
public class EmbeddingLDAPException extends IdentityException {

    private static final long serialVersionUID = 8759746693388071061L;

    public EmbeddingLDAPException(String errorMessage) {
        super(errorMessage);
    }

    public EmbeddingLDAPException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

}
