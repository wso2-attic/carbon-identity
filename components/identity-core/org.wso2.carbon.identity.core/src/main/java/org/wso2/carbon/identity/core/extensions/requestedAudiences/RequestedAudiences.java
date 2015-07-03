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

package org.wso2.carbon.identity.core.extensions.requestedAudiences;

import org.opensaml.common.SAMLObject;
import org.wso2.carbon.identity.core.extensions.ExtensionsConstants;
import org.wso2.carbon.identity.core.extensions.audience.Audience;

import javax.xml.namespace.QName;
import java.util.List;

public interface RequestedAudiences extends SAMLObject {

    /**
     * Element name, no namespace
     */
    String DEFAULT_ELEMENT_LOCAL_NAME = "RequestedAudiences";

    /**
     * Default element name
     */
    QName DEFAULT_ELEMENT_NAME = new QName(ExtensionsConstants.EXTENSION_NS,
            DEFAULT_ELEMENT_LOCAL_NAME, ExtensionsConstants.EXTENSION_PREFIX);

    /**
     * Local name of the XSI type
     */
    String TYPE_LOCAL_NAME = "RequestedAudiencesType";

    /**
     * QName of the XSI type
     */
    QName TYPE_NAME = new QName(ExtensionsConstants.EXTENSION_NS, TYPE_LOCAL_NAME,
            ExtensionsConstants.EXTENSION_PREFIX);

    /**
     * Element QName, no prefix
     */
    QName ELEMENT_QNAME = new QName(ExtensionsConstants.EXTENSION_NS, DEFAULT_ELEMENT_LOCAL_NAME);

    List<Audience> getAudiences();
}
