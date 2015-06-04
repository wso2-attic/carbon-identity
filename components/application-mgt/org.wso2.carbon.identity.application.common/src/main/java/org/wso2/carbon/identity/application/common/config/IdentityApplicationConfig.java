/*
 *Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.identity.application.common.config;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.*;

public class IdentityApplicationConfig {

    private static final Log log = LogFactory.getLog(IdentityApplicationConfig.class);

    private static String configFilePath;
    private static OMElement rootElement;
    private static volatile IdentityApplicationConfig instance;
    private static Map<String, Object> configuration = new HashMap<String, Object>();

    private IdentityApplicationConfig() {

    }

    public static IdentityApplicationConfig getInstance() {
        if (instance == null) {
            synchronized (IdentityApplicationConfig.class) {
                if (instance == null) {
                    instance = new IdentityApplicationConfig();
                    try {
                        instance.buildConfiguration();
                    } catch (XMLStreamException e) {
                        log.error("Error occurred while building Identity Application Management configuration: " + e.getMessage(), e);
                    } catch (IOException e) {
                        log.error("Error occurred while building Identity Application Management configuration: " + e.getMessage(), e);
                    }
                }
            }
        }
        return instance;
    }

    public static IdentityApplicationConfig getInstance(String filePath) throws IdentityApplicationManagementException {
        configFilePath = filePath;
        return getInstance();
    }

    /**
     * Returns the element with the provided local part
     *
     * @param localPart local part name
     * @return Corresponding OMElement
     */
    public OMElement getConfigElement(String localPart) {
        return rootElement.getFirstChildWithName(
                IdentityApplicationManagementUtil.getQNameWithIdentityApplicationNS(localPart));
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    private void buildConfiguration() throws XMLStreamException, FileNotFoundException {

        InputStream inStream = null;
        StAXOMBuilder builder = null;
        try {
            File configFile = null;
            if (configFilePath != null) {
                configFile = new File(configFilePath);
                if (configFile.exists()) {
                    inStream = new FileInputStream(configFile);
                }
            } else {
                configFile = new File(CarbonUtils.getCarbonSecurityConfigDirPath(),
                        IdentityApplicationConstants.APPLICATION_AUTHENTICATION_CONGIG);
                if (configFile.exists()) {
                    inStream = new FileInputStream(configFile);
                }
            }
            if (inStream == null) {
                String message = "Identity Application Management configuration not found";
                log.error(message);
                throw new FileNotFoundException(message);
            }
            builder = new StAXOMBuilder(inStream);
            rootElement = builder.getDocumentElement();
            Stack<String> nameStack = new Stack<String>();


            readChildElements(rootElement, nameStack);

        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException e) {
                log.error("Error occurred while closing the input stream after " +
                        "reading Identity Application Management configuration", e);
            }
        }
    }

    private void readChildElements(OMElement serverConfig, Stack<String> nameStack) {

        for (Iterator childElements = serverConfig.getChildElements(); childElements.hasNext(); ) {
            OMElement element = (OMElement) childElements.next();
            nameStack.push(element.getLocalName());
            if (elementHasText(element)) {
                String key = getKey(nameStack);
                Object currentObject = configuration.get(key);
                String value = replaceSystemProperty(element.getText());
                if (currentObject == null) {
                    configuration.put(key, value);
                } else if (currentObject instanceof ArrayList) {
                    List list=(ArrayList) currentObject;
                    if (!list.contains(value)) {
                        list.add(value);
                    }
                } else {
                    if (!value.equals(currentObject)) {
                        List arrayList = new ArrayList(2);
                        arrayList.add(currentObject);
                        arrayList.add(value);
                        configuration.put(key, arrayList);
                    }
                }
            }
            readChildElements(element, nameStack);
            nameStack.pop();
        }
    }

    private String getKey(Stack<String> nameStack) {

        StringBuilder key = new StringBuilder();
        for (int i = 0; i < nameStack.size(); i++) {
            String name = nameStack.elementAt(i);
            key.append(name).append(".");
        }
        key.deleteCharAt(key.lastIndexOf("."));
        return key.toString();
    }

    private boolean elementHasText(OMElement element) {

        String text = element.getText();
        return text != null && text.trim().length() != 0;
    }

    private String replaceSystemProperty(String text) {

        int indexOfStartingChars = -1;
        int indexOfClosingBrace;
        String tmpText=null;
        // The following condition deals with properties.
        // Properties are specified as ${system.property},
        // and are assumed to be System properties
        while (indexOfStartingChars < text.indexOf("${")
                && (indexOfStartingChars = text.indexOf("${")) != -1
                && (indexOfClosingBrace = text.indexOf("}")) != -1) { // Is a property used?
            String sysProp = text.substring(indexOfStartingChars + 2, indexOfClosingBrace);
            String propValue = System.getProperty(sysProp);
            if (propValue != null) {
                tmpText= text.substring(0, indexOfStartingChars) + propValue
                        + text.substring(indexOfClosingBrace + 1);
            }
            if (sysProp.equals(ServerConstants.CARBON_HOME)&& System.getProperty(ServerConstants.CARBON_HOME).equals(".")) {
                tmpText = new File(".").getAbsolutePath() + File.separator + text;

            }
        }
        return tmpText;
    }
}
