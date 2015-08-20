/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.sts;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.DeploymentConstants;
import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.deployment.DescriptionBuilder;
import org.apache.axis2.deployment.ServiceBuilder;
import org.apache.axis2.deployment.ServiceGroupBuilder;
import org.apache.axis2.deployment.repository.util.ArchiveReader;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.sts.internal.STSServiceDataHolder;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;
import org.wso2.carbon.utils.IOStreamUtils;
import org.wso2.carbon.utils.deployment.Axis2ServiceRegistry;
import org.wso2.carbon.utils.deployment.BundleClassLoader;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This deploys a wso2carbon-sts service when tenant is loaded.
 */
public class STSDeploymentListener extends AbstractAxis2ConfigurationContextObserver {

    private static final Log log = LogFactory.getLog(STSDeploymentListener.class);

    private static String componentsDirPath;

    static {
        String carbonRepo = System.getenv("CARBON_REPOSITORY");
        if (carbonRepo == null) {
            carbonRepo = System.getProperty("carbon.repository");
        }
        if (carbonRepo == null) {
            carbonRepo = System.getProperty("carbon.home") + File.separator + "repository";
        }
        componentsDirPath = carbonRepo + File.separator + "components";
    }

    @Override
    public void createdConfigurationContext(ConfigurationContext configContext) {

        AxisService service = null;
        try {
            service = configContext.getAxisConfiguration().getService("wso2carbon-sts");
        } catch (AxisFault axisFault) {
            // just ignore service is not in axis2 configuration
            if (log.isDebugEnabled()) {
                log.debug("wso2carbon-sts service is not available", axisFault);
            }
        }

        // creating service if it is null

        if (service == null) {
            configContext.getAxisConfiguration().addObservers(new STSDeploymentInterceptor());
            Bundle bundle = STSServiceDataHolder.getInstance().getBundle();
            AxisServiceGroup serviceGroup = createService(bundle, configContext);
            if (serviceGroup != null) {
                try {
                    configContext.getAxisConfiguration().addServiceGroup(serviceGroup);
                } catch (AxisFault axisFault) {
                    log.error("Error occurs while adding wso2carbon-sts service group in to axis2" +
                              "configuration of tenant " +
                              CarbonContext.getThreadLocalCarbonContext().getTenantId(), axisFault);
                }
            }
        }
    }

    private AxisServiceGroup createService(Bundle bundle, ConfigurationContext configContext) {

        Enumeration enumeration = bundle.findEntries("META-INF", "*services.xml", true);
        AxisServiceGroup serviceGroup = null;
        AxisConfiguration axisConfiguration = configContext.getAxisConfiguration();

        if (enumeration != null && enumeration.hasMoreElements()) {
            try {
                serviceGroup = new AxisServiceGroup(axisConfiguration);
                ClassLoader loader = new BundleClassLoader(bundle, Axis2ServiceRegistry.class.getClassLoader());
                URL url = (URL) enumeration.nextElement();
                Dictionary headers = bundle.getHeaders();
                String bundleSymbolicName = (String) headers.get("Bundle-SymbolicName");
                serviceGroup.setServiceGroupName(bundleSymbolicName);
                serviceGroup.setServiceGroupClassLoader(loader);
                InputStream inputStream = url.openStream();
                DescriptionBuilder builder = new DescriptionBuilder(inputStream, configContext);
                OMElement rootElement = builder.buildOM();
                String elementName = rootElement.getLocalName();
                HashMap<String, AxisService> wsdlServicesMap = processWSDL(bundle);
                if (MapUtils.isNotEmpty(wsdlServicesMap)) {
                    for (AxisService service : wsdlServicesMap.values()) {
                        Iterator<AxisOperation> operations = service.getOperations();
                        while (operations.hasNext()) {
                            AxisOperation axisOperation = operations.next();
                            axisConfiguration.getPhasesInfo().setOperationPhases(axisOperation);
                        }
                    }
                }
                if (DeploymentConstants.TAG_SERVICE.equals(elementName)) {
                    AxisService axisService = new AxisService(bundleSymbolicName);
                    axisService.setParent(serviceGroup);
                    axisService.setClassLoader(loader);
                    ServiceBuilder serviceBuilder = new ServiceBuilder(configContext, axisService);
                    serviceBuilder.setWsdlServiceMap(wsdlServicesMap);
                    AxisService service = serviceBuilder.populateService(rootElement);
                    ArrayList<AxisService> serviceList = new ArrayList<AxisService>();
                    serviceList.add(service);
                    DeploymentEngine.addServiceGroup(serviceGroup,
                                                     serviceList,
                                                     null,
                                                     null,
                                                     axisConfiguration);
                    if (log.isDebugEnabled()) {
                        log.debug("Deployed wso2carbon-sts service");
                    }
                } else if (DeploymentConstants.TAG_SERVICE_GROUP.equals(elementName)) {
                    ServiceGroupBuilder groupBuilder =
                            new ServiceGroupBuilder(rootElement, wsdlServicesMap, configContext);
                    ArrayList<? extends AxisService> serviceList = groupBuilder.populateServiceGroup(serviceGroup);
                    DeploymentEngine.addServiceGroup(serviceGroup,
                                                     serviceList,
                                                     null,
                                                     null,
                                                     axisConfiguration);
                    if (log.isDebugEnabled()) {
                        log.debug("Deployed wso2carbon-sts service group ");
                    }
                }

            } catch (AxisFault axisFault) {
                log.error("Error occur while deploying wso2carbon-sts service for tenant " +
                          CarbonContext.getThreadLocalCarbonContext().getTenantId(), axisFault);
            } catch (XMLStreamException e) {
                log.error("Error occur while deploying wso2carbon-sts service for tenant " +
                          CarbonContext.getThreadLocalCarbonContext().getTenantId(), e);
            } catch (IOException e) {
                log.error("Error occur while deploying wso2carbon-sts service for tenant " +
                          CarbonContext.getThreadLocalCarbonContext().getTenantId(), e);
            }
        }

        return serviceGroup;
    }

    private HashMap processWSDL(Bundle bundle) throws IOException, XMLStreamException {
        Enumeration enumeration = bundle.findEntries("META-INF", "*.wsdl", true);
        if (enumeration == null) {
            return new HashMap();
        }

        String bundleLocation = bundle.getLocation();
        // Sometimes value of the bundleLocation can be a string such as the following.
        // reference:file:plugins/org.wso2.carbon.statistics-3.2.0.jar
        // In these situations we need to remove the "reference:" part from the bundleLocation.
        if (bundleLocation.startsWith("reference:")) {
            bundleLocation = bundleLocation.substring("reference:".length());
        }

        // Extracting bundle file name.
        String[] subStrings = bundleLocation.split("/");
        String bundleFileName = subStrings[subStrings.length - 1];

        File bundleFile;
        URL bundleURL = new URL(bundleLocation);

        if ("file".equals(bundleURL.getProtocol())) {
            bundleFile = new File(bundleURL.getFile());
        } else {
            InputStream bundleStream = bundleURL.openStream();

            // Generate temp file path for the bundle.
            String tempBundleDirPath = System.getProperty("java.io.tmpdir") + File.separator + "bundles";

            //Creating a temp dir to store bundles.
            File tempBundleDir = new File(tempBundleDirPath);
            if (!tempBundleDir.exists() && !tempBundleDir.mkdir()) {
                log.warn("Could not create temp bundle directory " + tempBundleDir.getAbsolutePath());
                return new HashMap();
            }

            bundleFile = new File(tempBundleDirPath, bundleFileName);
            OutputStream bundleFileOutputSteam = new FileOutputStream(bundleFile);

            // Copying input stream to the file output stream
            IOStreamUtils.copyInputStream(bundleStream, bundleFileOutputSteam);
        }

        if (!bundleFile.exists()) {
            //If the bundle does not exits, then we check in the plugins dir.
            bundleFile = new File(componentsDirPath + File.separator + bundleURL.getFile());
        }

        if (!bundleFile.exists()) {
            return new HashMap();
        }

        DeploymentFileData deploymentFileData = new DeploymentFileData(bundleFile);
        ArchiveReader archiveReader = new ArchiveReader();
        return archiveReader.processWSDLs(deploymentFileData);
    }
}
