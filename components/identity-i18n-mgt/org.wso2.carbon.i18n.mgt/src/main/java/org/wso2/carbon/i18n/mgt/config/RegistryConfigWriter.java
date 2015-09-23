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
import org.wso2.carbon.i18n.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.i18n.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.i18n.mgt.internal.I18nMgtServiceComponent;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class RegistryConfigWriter implements ConfigWriter {

    private static Log log = LogFactory.getLog(RegistryConfigWriter.class);

	/**
	 * This method is used to add a new Email template in a specific tenant space.
	 *
	 * @param tenantId     - The tenant Id of the tenant that specific email template needs to be add.
	 * @param props        - Property configurations of the specific email template.
	 * @param resourcePath - Path to be add the specific email template.
	 * @throws org.wso2.carbon.i18n.mgt.exceptions.I18nMgtEmailConfigException
	 */
	@Override
	public void write(int tenantId, Properties props, String resourcePath) throws I18nMgtEmailConfigException {

		if (log.isDebugEnabled()) {
			log.debug("Saving email template at registry path : " + resourcePath);
		}

		RegistryService registry = I18nMgtServiceComponent.getRegistryService();
		try {
			UserRegistry userReg = registry.getConfigSystemRegistry(tenantId);
			Set<String> names = props.stringPropertyNames();

			if (!userReg.resourceExists(resourcePath)) {
				for (String propsKeyName : names) {
					// This is done due to casting to List in JDBCRegistryDao when adding a registry property.
					List<String> value = new ArrayList<String>();
					String[] propertyArray = propsKeyName.split("\\|");
					value.add(propertyArray[1]);
					Collection emailCollection = userReg.newCollection();
					String emailTemplateFolderName = propertyArray[0].replaceAll("\\s", "").toLowerCase();
					emailCollection.setProperty(emailTemplateFolderName, value);

					String emailTemplateFilename = emailTemplateFolderName + I18nMgtConstants.EMAIL_LOCALE_SEPARATOR +
							I18nMgtConstants.DEFAULT_EMAIL_LOCALE;
					String emailTemplateFolderResourcePath = resourcePath +
							I18nMgtConstants.EMAIL_FOLDER_SEPARATOR + emailTemplateFolderName;
					String emailTemplateFileResourcePath = resourcePath + I18nMgtConstants.EMAIL_FOLDER_SEPARATOR
							+ emailTemplateFolderName + I18nMgtConstants.EMAIL_FOLDER_SEPARATOR + emailTemplateFilename;

					userReg.put(emailTemplateFolderResourcePath, emailCollection);
					Resource resource = userReg.newResource();
					String emailTemplateContent = props.getProperty(propsKeyName);
					resource.setMediaType("text/plain");
					resource.setContent(emailTemplateContent);
					userReg.put(emailTemplateFileResourcePath, resource);
				}

				if (log.isDebugEnabled()) {
					log.debug("Default email template added to :" + " " + resourcePath + "successfully.");
				}

			} else {
				String[] propsKeyName = names.toArray(new String[names.size()]);
				String[] propsKeyNameSplit = propsKeyName[0].split("\\.");
				String emailTemplateFolderName = propsKeyNameSplit[0];
				String emailTemplateFilename = propsKeyName[0];
				String emailTemplateFileResourcePath = resourcePath + I18nMgtConstants.EMAIL_FOLDER_SEPARATOR +
						emailTemplateFolderName + I18nMgtConstants.EMAIL_FOLDER_SEPARATOR + emailTemplateFilename;

				Resource resource = userReg.newResource();
				String emailTemplateContent = props.getProperty(emailTemplateFilename);
				resource.setMediaType("text/plain");
				resource.setContent(emailTemplateContent);
				userReg.put(emailTemplateFileResourcePath, resource);

				if (log.isDebugEnabled()) {
					log.debug("Email template : " + emailTemplateFilename + " " +
							"saved successfully to path " + " " + emailTemplateFileResourcePath);
				}

			}
		} catch (RegistryException e) {
			throw new I18nMgtEmailConfigException("Error occurred while adding email template to registry path : "
					+ resourcePath);
		}

	}

}
