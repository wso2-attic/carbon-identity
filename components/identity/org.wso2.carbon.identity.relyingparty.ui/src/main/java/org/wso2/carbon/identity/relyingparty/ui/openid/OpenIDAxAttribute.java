/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty.ui.openid;

public class OpenIDAxAttribute {

    private String attributeName;
    private String namespace;

    /**
     * @param attributeName Name of the attribute - alias
     * @param namespace     Namespace url corresponding to the provided attribute
     */
    public OpenIDAxAttribute(String attributeName, String namespace) {
        this.attributeName = attributeName;
        this.namespace = namespace;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getNamespace() {
        return namespace;
    }

}