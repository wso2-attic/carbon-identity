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

package org.wso2.carbon.i18n.mgt.ui;


import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.i18n.mgt.dto.xsd.EmailTemplateDTO;
import org.wso2.carbon.i18n.mgt.stub.I18NEmailMgtConfigServiceStub;

import java.util.HashMap;
import java.util.Map;

public class I18nEmailMgtConfigServiceClient {

    private static Log log = LogFactory.getLog(I18nEmailMgtConfigServiceClient.class);

    private I18NEmailMgtConfigServiceStub stub;

    public I18nEmailMgtConfigServiceClient(String url,
                                           ConfigurationContext configContext) throws Exception {
        try {
            stub = new I18NEmailMgtConfigServiceStub(configContext, url
                    + "I18nEmailMgtConfigService");
        } catch (java.lang.Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    /**
     * @param cookie        HttpSession cookie
     * @param url           Backend Carbon server URL
     * @param configContext Axis2 Configuration Context
     */
    public I18nEmailMgtConfigServiceClient(String cookie, String url,
                                           ConfigurationContext configContext) throws Exception {
        try {
            stub = new I18NEmailMgtConfigServiceStub(configContext, url
                    + "I18nEmailMgtConfigService");
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(
                    org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                    cookie);
        } catch (java.lang.Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    private String[] handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    /**
     * Saves specific Email Template of a given tenant.
     *
     * @param emailTemplate <code>Email Template</code> new Email Template information
     * @throws Exception Error when saving the Email Template.
     */
    public void saveEmailConfig(EmailTemplateDTO emailTemplate) throws AxisFault {
        try {
            stub.saveEmailConfig(emailTemplate);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    /**
     * Adds an Email Template to the given tenant.
     *
     * @param emailTemplate <code>Email Template</code> new Email Template information
     * @throws Exception Error when adding new Email Template information
     */
    public void addEmailConfig(EmailTemplateDTO emailTemplate) throws AxisFault {
        try {
            stub.addEmailConfig(emailTemplate);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    /**
     * Loads set of Email Templates of a given tenant.
     *
     * @throws Exception Error when loading Email Template information
     */
    public EmailConfigDTO loadEmailConfig() throws AxisFault {
        EmailTemplateDTO[] emailTemplates = null;
        EmailConfigDTO emailConfig = new EmailConfigDTO();
        try {
            emailTemplates = stub.getEmailConfig();
            Map<String, String> emailTypes = new HashMap<String, String>();
            for (int i = 0; i < emailTemplates.length; i++) {
                emailTypes.put(emailTemplates[i].getName(), emailTemplates[i].getDisplayName());
            }
            emailConfig.setEmailTypes(emailTypes);
            emailConfig.setTemplates(emailTemplates);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return emailConfig;
    }

}
