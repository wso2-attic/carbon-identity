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

package org.wso2.carbon.identity.saml.metadata.extensions.requestedRecipients;

import org.opensaml.common.impl.AbstractSAMLObject;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.util.XMLObjectChildrenList;
import org.wso2.carbon.identity.saml.metadata.extensions.recipient.Recipient;

import java.util.ArrayList;
import java.util.List;

public class RequestedRecipientsImpl extends AbstractSAMLObject implements RequestedRecipients {

    private XMLObjectChildrenList recepientsList;

    protected RequestedRecipientsImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        recepientsList = new XMLObjectChildrenList<>(this);
    }

    public List<Recipient> getRecipients() {
        return (List<Recipient>) recepientsList;
    }

    public List<XMLObject> getOrderedChildren() {
        ArrayList<XMLObject> children = new ArrayList<>();

        children.addAll(recepientsList);
        return children;
    }
}
