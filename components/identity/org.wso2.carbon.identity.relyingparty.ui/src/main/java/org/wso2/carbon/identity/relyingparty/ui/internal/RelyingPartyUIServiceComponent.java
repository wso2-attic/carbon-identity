/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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
package org.wso2.carbon.identity.relyingparty.ui.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="identity.relyingparty..ui.dscomponent" immediate="true"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 */
public class RelyingPartyUIServiceComponent {

    private static Log log = LogFactory.getLog(RelyingPartyUIServiceComponent.class);

    private static ConfigurationContextService ccServiceInstance;

    public static ConfigurationContextService getConfigurationContextService() {
        if (ccServiceInstance == null) {
            String msg = "Before activating Carbon UI bundle, an instance of "
                    + "UserRealm service should be in existance";
            log.error(msg);
            throw new RuntimeException(msg);
        }
        return ccServiceInstance;
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        ccServiceInstance = contextService;
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        ccServiceInstance = null;
    }


}