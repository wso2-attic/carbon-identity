/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.identity.mgt.config;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.mail.EmailConfig;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.Iterator;

/**
 * This Singleton is used to load and save tenant specific configurations.
 */
public class ConfigBuilder {

    private static final String EMAIL_TEMPLATE_PATH = "identity/config/emailTemplate";
    private static final Log log = LogFactory.getLog(ConfigBuilder.class);
    private static ConfigBuilder instance = null;
    private ConfigManager cm = null;

    private ConfigBuilder() {
        cm = new ConfigManagerImpl();
    }

    public static ConfigBuilder getInstance() {
        if (instance == null) {
            instance = new ConfigBuilder();
        }

        return instance;
    }

    /**
     * Loads the tenant specific configuration.
     *
     * @param configType
     * @param stype
     * @param tenantId
     * @return
     * @throws Exception
     */
    public Config loadConfiguration(ConfigType configType, StorageType stype, int tenantId) throws Exception {
        Config config = null;

        switch (stype) {
            case REGISTRY:
                cm.setReader(new RegistryConfigReader());

                switch (configType) {
                    case EMAIL:
                        cm.setConfig(new EmailNotificationConfig());
                        cm.setResourcePath(EMAIL_TEMPLATE_PATH);
                        config = cm.loadConfig(tenantId);
                        if (config == null) {
                            config = loadEmailConfigFile();
                        }
                        break;
                    case CREDENTIALMGT:
                        break;
                    default:
                        throw new Exception("Configuration type not supported");
                }
                break;
            case DB:
                break;
            case FILE:
                break;
            default:
                throw new Exception("Configuration storage type not supported");
        }

        return config;
    }

    /**
     * Save the tenant specific configuration.
     *
     * @param stype
     * @param tenantId
     * @param config
     * @throws Exception
     */
    public void saveConfiguration(StorageType stype, int tenantId, Config config) throws Exception {

        switch (stype) {
            case REGISTRY:
                cm.setWriter(new RegistryConfigWriter());
                if (config instanceof EmailNotificationConfig) {
                    cm.setConfig(new EmailNotificationConfig());
                    cm.setResourcePath(EMAIL_TEMPLATE_PATH);
                    cm.saveConfig(config, tenantId);
                } else {
                    throw new Exception("Configuration type not supported");
                }

                break;
            case DB:
                break;
            case FILE:
                break;
            default:
                break;
        }
    }

    private Config loadEmailConfigFile() {

        String confXml = CarbonUtils.getCarbonConfigDirPath() + File.separator
                + IdentityMgtConstants.EMAIL_CONF_DIRECTORY + File.separator
                + IdentityMgtConstants.EMAIL_ADMIN_CONF_FILE;

        Config emailConfig = new EmailNotificationConfig();

        File configfile = new File(confXml);
        if (!configfile.exists()) {
            log.warn("Email Configuration File is not present at: " + confXml);
        }

        XMLStreamReader parser = null;
        InputStream stream = null;

        try {
            stream = new FileInputStream(configfile);
            parser = XMLInputFactory.newInstance()
                    .createXMLStreamReader(stream);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            OMElement documentElement = builder.getDocumentElement();
            Iterator iterator = documentElement.getChildElements();
            while (iterator.hasNext()) {
                OMElement omElement = (OMElement) iterator.next();
                String configType = omElement.getAttributeValue(new QName(
                        "type"));
                if (configType != null && configType.trim().length() > 0) {
                    emailConfig.setProperty(configType, loadEmailConfig(omElement));
                }

            }
        } catch (XMLStreamException e) {
            log.warn("Error while loading email config. using default configuration");
        } catch (FileNotFoundException e) {
            log.warn("Error while loading email config. using default configuration");
        } finally {
            try {
                if (parser != null) {
                    parser.close();
                }
                if (stream != null) {
                    stream.close();
                }
            } catch (XMLStreamException e) {
                log.error("Error while closing XML stream");
            } catch (IOException e) {
                log.error("Error while closing input stream");
            }
        }
        return emailConfig;

    }


    private String loadEmailConfig(OMElement configElement) {
        StringBuilder emailTemplate = new StringBuilder();
        EmailConfig config = new EmailConfig();
        Iterator it = configElement.getChildElements();
        while (it.hasNext()) {
            OMElement element = (OMElement) it.next();
            if ("subject".equals(element.getLocalName())) {
                emailTemplate.append(element.getText());
            } else if ("body".equals(element.getLocalName())) {
                emailTemplate.append("|");
                emailTemplate.append(element.getText());
            } else if ("footer".equals(element.getLocalName())) {
                emailTemplate.append("|");
                emailTemplate.append(element.getText());
            } else if ("targetEpr".equals(element.getLocalName())) {
//				config.setTargetEpr(element.getText());
            } else if ("redirectPath".equals(element.getLocalName())) {
//				config.setRedirectPath(element.getText());
            }
        }
        return emailTemplate.toString();
    }
}
