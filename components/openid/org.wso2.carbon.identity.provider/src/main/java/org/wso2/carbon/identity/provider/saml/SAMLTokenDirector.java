/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.saml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasData;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;
import org.wso2.carbon.identity.provider.GenericIdentityProviderData;
import org.wso2.carbon.identity.provider.IdentityProviderException;

public class SAMLTokenDirector {

    private static final Log log = LogFactory.getLog(SAMLTokenDirector.class);

    private SAMLTokenBuilder builder = null;
    private RahasData rahasData = null;
    private GenericIdentityProviderData ipData = null;

    static {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            log.error("SAMLTokenDirectorBootstrapError", e);
            throw new RuntimeException(e);
        }
    }

    public SAMLTokenDirector(SAMLTokenBuilder builder, RahasData rData, GenericIdentityProviderData iData)
            throws IdentityProviderException {
        this.builder = builder;
        this.rahasData = rData;
        this.ipData = iData;
    }

}