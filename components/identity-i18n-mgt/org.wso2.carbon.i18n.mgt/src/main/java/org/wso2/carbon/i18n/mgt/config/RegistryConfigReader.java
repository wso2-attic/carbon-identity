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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.i18n.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.i18n.mgt.internal.I18nMgtServiceComponent;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

import java.nio.charset.Charset;
import java.util.Properties;

public class RegistryConfigReader implements ConfigReader {

	private static Log log = LogFactory.getLog(RegistryConfigReader.class);

	/**
	 * This method is used to add a new Email template in a specific tenant space.
	 *
	 * @param tenantId     - The tenant Id of the tenant that specific email template needs to be add.
	 * @param resourcePath - Path to be add the specific email template.
	 * @throws org.wso2.carbon.i18n.mgt.exceptions.I18nMgtEmailConfigException
	 */
	@Override
	public Properties read(int tenantId, String resourcePath) throws I18nMgtEmailConfigException {

		if (log.isDebugEnabled()) {
			log.debug("Reading email templates from registry path : " + resourcePath);
		}

		Resource resourceValue = null;
		Properties readerProps = new Properties();
		RegistryService registry = I18nMgtServiceComponent.getRegistryService();

		try {
			UserRegistry userReg = registry.getConfigSystemRegistry(tenantId);
			resourceValue = userReg.get(resourcePath);
			Collection emailSubjectCollecton = (Collection) resourceValue;
			String[] emailSubjectCollectionArray = emailSubjectCollecton.getChildren();

			Resource resourceIter = null;
			Collection collectionIter = null;

			//Iterating through email template subject collection.
			for (String emailSubjectResourcePath : emailSubjectCollectionArray) {
				resourceIter = userReg.get(emailSubjectResourcePath);
				collectionIter = (Collection) resourceIter;
				String[] emailLocaleCollectionArray = collectionIter.getChildren();

				String[] emailSubjectPathArray = emailSubjectResourcePath.split("/");
				String emailSubjectFolder = emailSubjectPathArray[3];
				String emailTemplateDisplayValue = resourceIter.getProperty(emailSubjectFolder);

				//Iterating through email locale file collection.
				for (String emailTemplateResourcePath : emailLocaleCollectionArray) {
					Resource emailTemplateResource = userReg.get(emailTemplateResourcePath);
					String[] emailTemplateFolderArray = emailTemplateResourcePath.split("/");
					String emailTemplateFileName = emailTemplateFolderArray[4];
					byte[] emailTemplateContentArray = (byte[]) emailTemplateResource.getContent();
					String emailTemplateLocaleContent = new String(emailTemplateContentArray, Charset.forName("UTF-8"));
					String emailTemplateKey = emailTemplateFileName + "|" + emailTemplateDisplayValue;
					readerProps.put(emailTemplateKey, emailTemplateLocaleContent);
				}
			}

			if (log.isDebugEnabled()) {
				log.debug("Successfully read the email templates in resource path : " + resourcePath);
			}

		} catch (ResourceNotFoundException e) {
			// Ignore the registry resource exception.
			readerProps = new Properties();
			if (log.isDebugEnabled()) {
				log.debug("Ignored ResourceNotFoundException", e);
			}
		} catch (RegistryException e) {
			throw new I18nMgtEmailConfigException(
					"Error occurred while reading email templates from path : " + resourcePath, e);
		}
		return readerProps;
	}
}