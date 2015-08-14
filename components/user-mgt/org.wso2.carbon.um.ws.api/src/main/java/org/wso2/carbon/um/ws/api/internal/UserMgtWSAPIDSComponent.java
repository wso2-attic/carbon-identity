/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.um.ws.api.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="remote.um.api.component" immediate="true"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"
 * bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 */
public class UserMgtWSAPIDSComponent {

    private static final Log log = LogFactory.getLog(UserMgtWSAPIDSComponent.class);
    private static final String ACTIVATION_MESSGE = "Remote User mgt bundle is activated ";

    public static ConfigurationContextService getCcServiceInstance() {
        return UserMgtWSAPIDataHolder.getInstance().getCcServiceInstance();
    }

    protected void activate(ComponentContext ctxt) {
        log.debug(ACTIVATION_MESSGE);
    }

    protected void deactivate(ComponentContext ctxt) {
        return;
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        UserMgtWSAPIDataHolder.getInstance().setCcServiceInstance(contextService);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        UserMgtWSAPIDataHolder.getInstance().setCcServiceInstance(null);
    }

}
