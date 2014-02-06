/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework;

import org.wso2.carbon.identity.application.authentication.framework.config.FileBasedConfigurationBuilder;
import org.wso2.carbon.identity.application.authentication.framework.config.AuthenticatorConfig;


/**
 * This is the super class of all the Application Authenticators.
 * Authenticator writers must extend this.
 */
public abstract class AbstractApplicationAuthenticator implements ApplicationAuthenticator {
	
	/* (non-Javadoc)
	 * @see org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator#isDisabled()
	 */
	@Override
	public boolean isEnabled() {
		if (getAuthenticatorConfig() != null){
			return getAuthenticatorConfig().isEnabled();
		}
		return true;
	}
	
	protected AuthenticatorConfig getAuthenticatorConfig() {
		return FileBasedConfigurationBuilder.getInstance().getAuthenticatorBean(getAuthenticatorName());
    }
} 