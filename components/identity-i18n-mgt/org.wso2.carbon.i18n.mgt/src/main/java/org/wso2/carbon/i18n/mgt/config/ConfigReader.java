/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.i18n.mgt.config;

import org.wso2.carbon.i18n.mgt.exceptions.I18nMgtEmailConfigException;

import java.util.Properties;

/**
 * This is used to read email template property configurations from a persistence store.
 *
 */
public interface ConfigReader {

	/**
	 * Reads a tenant specific configuration
	 *
	 * @param tenantId     -  Tenant Id which the Email Template configuration belongs.
	 * @param resourcePath - Resource path to be saved the email templates.
	 * @return The populated email template configuration properties.
	 * @throws org.wso2.carbon.i18n.mgt.exceptions.I18nMgtEmailConfigException
	 */
	Properties read(int tenantId, String resourcePath) throws I18nMgtEmailConfigException;
}
