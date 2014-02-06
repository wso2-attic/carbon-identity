/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth.callback;

import java.util.Properties;

/**
 * An abstract implementations of the <Code>OAuthCallbackHandler</Code>
 * with the implementations for the basic methods.
 */
public abstract class AbstractOAuthCallbackHandler
        implements OAuthCallbackHandler {

    protected int priority = 5;
    protected Properties properties;

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setProperties(Properties props) {
        properties = props;
    }
}
