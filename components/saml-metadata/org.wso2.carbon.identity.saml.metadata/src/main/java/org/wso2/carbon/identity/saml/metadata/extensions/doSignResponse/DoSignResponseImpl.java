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

package org.wso2.carbon.identity.saml.metadata.extensions.doSignResponse;

import org.opensaml.common.impl.AbstractSAMLObject;
import org.opensaml.xml.XMLObject;

import java.util.List;

public class DoSignResponseImpl extends AbstractSAMLObject implements DoSignResponse {

    private boolean doSignResponse;

    protected DoSignResponseImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
    }

    public boolean getDoSignResponse() {
        return doSignResponse;
    }

    public void setDoSignResponse(boolean doSignResponse) {
        this.doSignResponse = prepareForAssignment(this.doSignResponse, doSignResponse);
    }

    public List<XMLObject> getOrderedChildren() {
        return null;
    }
}
