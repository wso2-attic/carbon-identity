/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt.service;

import org.wso2.carbon.identity.mgt.IdentityMgtException;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.user.core.UserStoreException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public interface IdentityMgtService {

    public Properties addConfiguration(int tenantId) throws IdentityMgtException;

    public void updateConfiguration(int tenantId, Map<String, String> configurationDetails) throws IdentityMgtException;

    public Map<String, String> getConfiguration(int tenantId) throws IdentityMgtException;

    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws UserStoreException, IdentityMgtException;

}
