/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.core;

import org.wso2.carbon.identity.core.model.IdentityEventListenerProperty;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;

public class AbstractIdentityUserOperationEventListener extends AbstractUserOperationEventListener {
    public boolean isEnable(String name) {
        IdentityEventListenerProperty identityEventListenerProperty = IdentityUtil.readEventListenerProperty
                (UserOperationEventListener.class.getName(), name);
        return identityEventListenerProperty.isEnable();
    }

    public int getOrderId(String name) {
        IdentityEventListenerProperty identityEventListenerProperty = IdentityUtil.readEventListenerProperty(UserOperationEventListener.class.getName(), name);
        return identityEventListenerProperty.getOrder();
    }
}
