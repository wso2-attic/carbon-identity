/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.saml.metadata.extensions.enableAttributesByDefault;

import org.opensaml.common.impl.AbstractSAMLObjectBuilder;
import org.opensaml.common.xml.SAMLConstants;

public class EnableAttributesByDefaultBuilder extends AbstractSAMLObjectBuilder<EnableAttributesByDefault> {

    public EnableAttributesByDefaultBuilder() {

    }

    public EnableAttributesByDefault buildObject() {
        return buildObject(SAMLConstants.SAML20MD_NS, EnableAttributesByDefault.DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants
                .SAML20MD_PREFIX);
    }

    public EnableAttributesByDefault buildObject(String namespaceURI, String localName, String namespacePrefix) {
        return new EnableAttributesByDefaultImpl(namespaceURI, localName, namespacePrefix);
    }
}
