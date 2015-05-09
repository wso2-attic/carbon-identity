/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.security.persistence;

import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.PolicyUtil;
import org.apache.axis2.util.Utils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.PolicyReference;
import org.apache.ws.security.util.UUIDGenerator;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.util.ParameterUtil;
import org.wso2.carbon.registry.core.Association;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.security.SecurityConfigException;
import org.wso2.carbon.security.SecurityServiceHolder;
import org.wso2.carbon.security.util.ServicePersistenceUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class ServicePersistenceManager {

    private static final Log log = LogFactory.getLog(ServicePersistenceManager.class);
    private static final Object WRITE_LOCK = new Object();
    private static final String ADDRESSING_MODULE = "addressing";
    private static final String TRANSPORT_LISTENER = "listener";
    private static final String TRANSPORT_SENDER = "sender";
    private static final String GLOBALLY_ENGAGED_PARAM_NAME = "globallyEngaged";
    private static final String GLOBALLY_ENGAGED_CUSTOM = "globallyEngagedCustom";

    private Registry configRegistry;
    private AxisConfiguration axisConfig;


    public ServicePersistenceManager(AxisConfiguration axisConfig) throws Exception {
        this.axisConfig = axisConfig;
        configRegistry = SecurityServiceHolder.getRegistry();
    }

    public ServicePersistenceManager() throws Exception {
        configRegistry = SecurityServiceHolder.getRegistry();
    }

    /**
     * Handle existing service initialization
     *
     * @param resource    Resource
     * @param axisService Axis service
     * @throws Exception
     */
    public void handleExistingServiceInit(Resource resource, AxisService axisService) throws Exception {

        boolean isProxyService = ServicePersistenceUtil.isProxyService(axisService);
        boolean wsdlChangeDetected = false;
        // this flag is to identify whether we need to call a 'put' on service resource
        boolean needServiceResourceUpdate = false;

        configRegistry.beginTransaction();
        String serviceResourcePath = resource.getPath();

        // Fetch and attach Service policies
        if (!isProxyService) {
            loadPolicies(axisService, getPropertyValues(serviceResourcePath,
                    RegistryResources.ServiceProperties.POLICY_UUID), serviceResourcePath);
        }

        for (Iterator iterator = axisService.getOperations(); iterator.hasNext(); ) {
            AxisOperation axisOperation = (AxisOperation) iterator.next();
            String operationPath = ServicePersistenceUtil.getResourcePath(axisOperation);

            // check whether the operation exists in the registry
            if (configRegistry.resourceExists(operationPath)) {
                // Fetch and attach Operation and Message Policies
                if (!axisOperation.isControlOperation()) {
                    // First load operation policies
                    if (!isProxyService) {
                        loadPolicies(axisOperation, getPropertyValues(operationPath,
                                        RegistryResources.ServiceProperties.POLICY_UUID),
                                serviceResourcePath);
                    }

                    // Fetch and attach MessageIn policies for this operation
                    if (!(axisOperation instanceof OutOnlyAxisOperation) && !isProxyService) {
                        loadPolicies(axisOperation.getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE),
                                getPropertyValues(operationPath,
                                        RegistryResources.ServiceProperties.MESSAGE_IN_POLICY_UUID),
                                serviceResourcePath);
                    }

                    // Fetch and attach MessageOut policies for this operation
                    if (!(axisOperation instanceof InOnlyAxisOperation) && !isProxyService) {
                        loadPolicies(axisOperation.getMessage(
                                WSDLConstants.MESSAGE_LABEL_OUT_VALUE), getPropertyValues(operationPath,
                                RegistryResources.ServiceProperties.MESSAGE_OUT_POLICY_UUID), serviceResourcePath);
                    }

                    // Disengage all the statically engaged modules (i.e. those module engaged
                    // from the services.xml file)
                    axisOperation.getEngagedModules().clear();

                    for (AxisModule axisModule : axisOperation.getEngagedModules()) {
                        axisOperation.disengageModule(axisModule);
                    }

                    Association[] associations = configRegistry.getAssociations(operationPath,
                            RegistryResources.Associations.ENGAGED_MODULES);
                    for (Association association : associations) {
                        AxisModule axisModule = getExistingAxisModule(association.getDestinationPath());
                        if (!isGloballyEngaged(association.getDestinationPath())) {
                            if (!axisService.isEngaged(axisModule)) {
                                axisOperation.engageModule(axisModule);
                            }
                        }
                    }
                    //Handle operation parameters
                    //loadParameters(axisOperation, operationPath);

                    //Handle operation documentation
                    loadDocumentation(axisOperation, operationPath);
                }
            } else {
                wsdlChangeDetected = true;
                writeParameters(axisOperation.getParameters(),
                        ServicePersistenceUtil.getResourcePath(axisOperation));

                for (AxisModule axisModule : axisOperation.getEngagedModules()) {
                    String moduleResourcePath = ServicePersistenceUtil.getResourcePath(axisModule);
                    if (!isGloballyEngaged(moduleResourcePath)
                            && !axisService.getParent().isEngaged(axisModule.getName())
                            && !axisService.isEngaged(axisModule.getName())) {
                        configRegistry.addAssociation(ServicePersistenceUtil.getResourcePath(axisOperation),
                                moduleResourcePath, RegistryResources.Associations.ENGAGED_MODULES);
                    }
                }
            }
        }

        String operationsPath = resource.getPath() + RegistryResources.OPERATIONS;
        if (configRegistry.resourceExists(operationsPath)) {
            Collection operationsCollection = (Collection) configRegistry.get(operationsPath);
            for (String opPath : operationsCollection.getChildren()) {
                String opName = opPath.substring(operationsPath.length()).replace("/", "");
                if (axisService.getOperation(new QName(opName)) == null) {
                    wsdlChangeDetected = true;
                    configRegistry.delete(opPath);
                }
            }
        }

        // Fetch and attach Binding, Binding operation and their Message policies
        Map endPointMap = axisService.getEndpoints();
        for (Object o : endPointMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            AxisEndpoint point = (AxisEndpoint) entry.getValue();
            AxisBinding currentAxisBinding = point.getBinding();

            // Fetch binding policies
            String bindingPath = serviceResourcePath + RegistryResources.ServiceProperties.BINDINGS +
                    currentAxisBinding.getName().getLocalPart();
            if (configRegistry.resourceExists(bindingPath)) {
                if (isProxyService) {
                    // This is to ensure that binding level policies applied from the UI
                    // get precedence over service level policies for proxy services
                    java.util.Collection<PolicyComponent> attachedPolicies = axisService.getPolicySubject().
                            getAttachedPolicyComponents();
                    if (attachedPolicies != null && !attachedPolicies.isEmpty()) {
                        List properties = getPropertyValues(bindingPath,
                                RegistryResources.ServiceProperties.POLICY_UUID);
                        if (properties != null && properties.size() > 0) {
                            List<String> removablePolicies = new ArrayList<String>();
                            for (PolicyComponent pc : attachedPolicies) {
                                if (pc instanceof Policy) {
                                    String id = ((Policy) pc).getId();
                                    if (properties.contains(id)) {
                                        removablePolicies.add(id);
                                    }
                                }
                            }

                            for (String id : removablePolicies) {
                                axisService.getPolicySubject().detachPolicyComponent(id);
                            }
                        }
                    }
                }
                loadPolicies(currentAxisBinding, getPropertyValues(bindingPath,
                                RegistryResources.ServiceProperties.POLICY_UUID),
                        serviceResourcePath);
                Iterator operationsItr = currentAxisBinding.getChildren();
                while (operationsItr.hasNext()) {
                    AxisBindingOperation bindingOp = (AxisBindingOperation) operationsItr.next();

                    // Fetch and attach binding operation policies
                    String bindingOpPath = getBindingOperationPath(serviceResourcePath, bindingOp);
                    if (configRegistry.resourceExists(bindingOpPath)) {
                        loadPolicies(bindingOp, getPropertyValues(bindingOpPath,
                                        RegistryResources.ServiceProperties.POLICY_UUID),
                                serviceResourcePath);
                        // Fetch and attach MessageIn policies for this operation
                        loadPolicies(bindingOp.getChild(
                                WSDLConstants.MESSAGE_LABEL_IN_VALUE), getPropertyValues(
                                bindingOpPath, RegistryResources.ServiceProperties
                                        .MESSAGE_IN_POLICY_UUID), serviceResourcePath);
                        // Fetch and attach MessageOut policies for this operation
                        loadPolicies(bindingOp.getChild(WSDLConstants.MESSAGE_LABEL_OUT_VALUE),
                                getPropertyValues(bindingOpPath, RegistryResources.ServiceProperties.
                                        MESSAGE_OUT_POLICY_UUID), serviceResourcePath);
                    } else {
                        writeAxisDescription(bindingOp, bindingOp.getName().getLocalPart(),
                                ServicePersistenceUtil.getBindingOperationPath(serviceResourcePath, bindingOp));
                    }
                }
            } else {
                handleNewBindingAddition(serviceResourcePath, currentAxisBinding);
            }

        }

        // Disengage all the statically engaged modules (i.e. those module
        // engaged from the services.xml file)
        for (AxisModule axisModule : axisService.getEngagedModules()) {
            axisService.disengageModule(axisModule);
        }

        // Engage modules to service
        Association[] engModules = configRegistry.getAssociations(serviceResourcePath,
                RegistryResources.Associations.ENGAGED_MODULES);
        for (Association association : engModules) {
            AxisModule axisModule = getExistingAxisModule(association.getDestinationPath());
            if (!isGloballyEngaged(association.getDestinationPath())) {
                axisService.disengageModule(axisModule);
                axisService.engageModule(axisModule);
            }
        }

        // add description
        if (wsdlChangeDetected) {
            resource.setProperty(RegistryResources.ServiceProperties.DOCUMENTATION,
                    axisService.getDocumentation());
        } else {
            loadDocumentation(axisService, serviceResourcePath);
        }

        // If the current service is proxy service, write existing params into registry, because the proxy
        // editing also supports parameter editing, to which we should give the precedence
        if (isProxyService) {
            ArrayList<Parameter> availableParameters = axisService.getParameters();
            // Adding the parameters to the configRegistry
            for (Parameter serviceParameter : availableParameters) {
                if (serviceParameter.getParameterType() != Parameter.ANY_PARAMETER) {
                    //updateServiceParameter(axisService, serviceParameter);
                }
            }
        }
        //loadParameters(axisService, serviceResourcePath);

        // Handle existing transports
        if (isProxyService) {
            List<String> availableTransports = axisService.getExposedTransports();
            ListIterator<String> transportItr = availableTransports.listIterator();

            // Removing transports from the configRegistry
            Association[] associations = configRegistry.getAssociations(serviceResourcePath,
                    RegistryResources.Associations.EXPOSED_TRANSPORTS);
            for (Association a : associations) {
                configRegistry.removeAssociation(a.getSourcePath(), a.getDestinationPath(),
                        a.getAssociationType());
            }

            String allTransports = resource.getProperty(RegistryResources.ServiceProperties.
                    EXPOSED_ON_ALL_TANSPORTS);
            if (allTransports != null && "true".equals(allTransports)) {
                resource.setProperty(RegistryResources.ServiceProperties.EXPOSED_ON_ALL_TANSPORTS,
                        String.valueOf(false));
                needServiceResourceUpdate = true;
            }

            // Adding the transports to the configRegistry
            while (transportItr.hasNext()) {
                String transport = transportItr.next();
                Resource transportResource = getTransportResource(transport, true);
                if (transportResource == null) {
                    throw new SecurityException("The configuration resource for " + transport +
                            " transport does not exist");
                }
                configRegistry.addAssociation(serviceResourcePath, transportResource.getPath(),
                        RegistryResources.Associations.EXPOSED_TRANSPORTS);
                if (log.isDebugEnabled()) {
                    log.debug("Added " + transport + " transport binding for " +
                            axisService.getName() + " service");
                }
            }
        } else {
            if (!Boolean.valueOf(resource.getProperty(RegistryResources
                    .ServiceProperties.EXPOSED_ON_ALL_TANSPORTS))) {
                axisService.setExposedTransports(new ArrayList());
                Association[] associations = configRegistry.getAssociations(resource.getPath(),
                        RegistryResources.Associations.EXPOSED_TRANSPORTS);
                for (Association association : associations) {
                    Resource protocolResource = configRegistry.get(association.getDestinationPath());
                    String transportProtocol = protocolResource
                            .getProperty(RegistryResources.Transports.PROTOCOL_NAME);
                    axisService.addExposedTransport(transportProtocol);
                    protocolResource.discard();
                }
            }
        }
        // Activate/Deactivate service
        String serviceState = resource.getProperty(RegistryResources.ServiceProperties.ACTIVE);
        if (serviceState == null || serviceState.trim().length() == 0) {
            serviceState = "true";
        }
        axisService.setActive(Boolean.parseBoolean(serviceState));

        if (wsdlChangeDetected || needServiceResourceUpdate) {
            configRegistry.put(serviceResourcePath, resource);
        }
        configRegistry.commitTransaction();

        if (log.isDebugEnabled()) {
            log.debug("Initialized service - " + axisService.getName());
        }
    }

    /**
     * Get existing axis module
     *
     * @param modulePath Module path
     * @return Axis module
     * @throws Exception
     */
    protected AxisModule getExistingAxisModule(String modulePath) throws Exception {

        if (modulePath.endsWith("/")) {
            modulePath = modulePath.substring(0, modulePath.length() - 1);
        }
        AxisModule existingModule = null;
        if (configRegistry.resourceExists(modulePath)) {
            Resource givenResource = configRegistry.get(modulePath);
            existingModule = getAxisModule(givenResource);
            givenResource.discard();
        }
        // if the existingModule is null, check whether there are new versions..
        if (existingModule == null) {
            String temp = modulePath.substring(0, modulePath.lastIndexOf('/'));
            if (configRegistry.resourceExists(temp)) {
                Collection moduleVersions = (Collection) configRegistry.get(temp);
                // we iterate through all the version of the module and finds on which currently
                // exists in the system
                for (String versionPath : moduleVersions.getChildren()) {
                    Resource versionResource = configRegistry.get(versionPath);
                    existingModule = getAxisModule(versionResource);
                    if (existingModule != null) {
                        versionResource.discard();
                        break;
                    }
                    versionResource.discard();
                }
                moduleVersions.discard();
            }
        }
        if (existingModule == null) {
            throw new Exception("Axis Module not found for resource path : " + modulePath);
        }
        return existingModule;
    }

    private AxisModule getAxisModule(Resource moduleResource) {

        String modName = moduleResource.getProperty(RegistryResources.ModuleProperties.NAME);
        String modVersion = moduleResource.getProperty(RegistryResources.ModuleProperties.VERSION);
        return axisConfig.getModule(modName, modVersion);
    }

    /**
     * Load documentation
     *
     * @param axisDescription Axis description
     * @param resourcePath    Resource path
     * @throws RegistryException
     */
    protected void loadDocumentation(AxisDescription axisDescription, String resourcePath) throws RegistryException {

        Resource resource = configRegistry.get(resourcePath);
        String documentation = resource.getProperty(RegistryResources.ServiceProperties.DOCUMENTATION);
        if (documentation != null) {
            try {
                axisDescription.setDocumentation(AXIOMUtil.stringToOM(documentation));
            } catch (XMLStreamException ex) {
                axisDescription.setDocumentation(documentation);
            }
        }
    }

    /**
     * Handle new service addition
     *
     * @param axisService Axis service
     * @throws SecurityConfigException
     */
    public void handleNewServiceAddition(AxisService axisService) throws SecurityConfigException {

        if (axisService.isClientSide()) {
            return;
        }
        synchronized (WRITE_LOCK) {
            try {
                configRegistry.beginTransaction();

                //Persist service into registry
                Collection service = configRegistry.newCollection();
                service.setProperty(RegistryResources.ServiceProperties.DOCUMENTATION, axisService.getDocumentation());
                service.setProperty(RegistryResources.ServiceProperties.EXPOSED_ON_ALL_TANSPORTS,
                        String.valueOf(axisService.isEnableAllTransports()));

                String serviceResourcePath = ServicePersistenceUtil.getResourcePath(axisService);
                configRegistry.put(serviceResourcePath, service);

                //Persist Service Operations
                for (Iterator iterator = axisService.getOperations(); iterator.hasNext(); ) {
                    AxisOperation axisOperation = (AxisOperation) iterator.next();
                    writeAxisDescription(axisOperation, axisOperation.getName().getLocalPart(), serviceResourcePath);
                    writeParameters(axisOperation.getParameters(), serviceResourcePath);
                }

                //Persist Service Bindings
                Map endPointMap = axisService.getEndpoints();
                for (Object object : endPointMap.entrySet()) {
                    Map.Entry entry = (Map.Entry) object;
                    AxisBinding axisBinding = ((AxisEndpoint) entry.getValue()).getBinding();
                    handleNewBindingAddition(serviceResourcePath, axisBinding);
                }

                //Persist the Service Policies
                List<Resource> servicePolicies = getServicePolicies(axisService);
                for (Resource servicePolicy : servicePolicies) {
                    configRegistry.put(serviceResourcePath + RegistryResources.POLICIES + servicePolicy
                            .getProperty(RegistryResources.ModuleProperties.POLICY_UUID), servicePolicy);
                }

                //If the service scope='soapsession', engage addressing if not already engaged.
                if (axisService.getScope().equals(Constants.SCOPE_SOAP_SESSION)) {
                    if (!axisService.isEngaged(ADDRESSING_MODULE)) {
                        axisService.engageModule(axisService.getAxisConfiguration().getModule(ADDRESSING_MODULE));
                    }
                }

                //Persist Modules Engaged to this service
                for (AxisModule axisModule : axisService.getEngagedModules()) {
                    String moduleResourcePath = ServicePersistenceUtil.getResourcePath(axisModule);
                    if (!isGloballyEngaged(moduleResourcePath) &&
                            !axisService.getParent().isEngaged(axisModule.getName())) {
                        configRegistry.addAssociation(serviceResourcePath, moduleResourcePath,
                                RegistryResources.Associations.ENGAGED_MODULES);
                    }
                }

                //Persist operation-module engagements
                for (Iterator iterator = axisService.getOperations(); iterator.hasNext(); ) {
                    AxisOperation axisOperation = (AxisOperation) iterator.next();

                    for (Object o : axisOperation.getEngagedModules()) {
                        AxisModule axisModule = (AxisModule) o;
                        String moduleResourcePath = ServicePersistenceUtil.getResourcePath(axisModule);
                        if (!isGloballyEngaged(moduleResourcePath) && !axisService.getParent()
                                .isEngaged(axisModule.getName()) && !axisService.isEngaged(axisModule.getName())) {
                            configRegistry.addAssociation(ServicePersistenceUtil.getResourcePath(axisOperation),
                                    moduleResourcePath, RegistryResources.Associations.ENGAGED_MODULES);
                        }
                    }
                }

                //Persist service parameters
                writeParameters(axisService.getParameters(), serviceResourcePath);

                //Persist transport associations
                if (!axisService.isEnableAllTransports()) {
                    List<String> exposedTransports = axisService.getExposedTransports();
                    for (String exposedTransport : exposedTransports) {
                        Resource transportResource = getTransportResource(exposedTransport, true);
                        if (transportResource == null) {
                            throw new SecurityConfigException("The configuration resource " + "for " + exposedTransport
                                    + " transport does not exist");
                        }
                        configRegistry.addAssociation(serviceResourcePath, transportResource.getPath(),
                                RegistryResources.Associations.EXPOSED_TRANSPORTS);
                    }
                }

                if (configRegistry.resourceExists(serviceResourcePath)) {
                    service = (Collection) configRegistry.get(serviceResourcePath);
                    service.addProperty(RegistryResources.SUCCESSFULLY_ADDED, "true");
                    configRegistry.put(serviceResourcePath, service);
                }
                configRegistry.commitTransaction();

                if (log.isDebugEnabled()) {
                    log.debug("Added new service - " + axisService.getName());
                }
            } catch (RegistryException e) {
                throw new SecurityConfigException(
                        "Error occurred accessing registry while adding service " + axisService.getName(), e);
            } catch (XMLStreamException ex) {
                throw new SecurityConfigException(
                        "Error occurred when getting service policy while adding service " + axisService.getName(), ex);
            } catch (AxisFault axisFault) {
                throw new SecurityConfigException(
                        "Error occurred when persisting service parameters while adding service " + axisService
                                .getName(), axisFault);
            }
        }
    }

    /**
     * Handle new module addition
     *
     * @param axisModule    Axis module
     * @param moduleName    Module name
     * @param moduleVersion Module version
     * @throws Exception
     */
    public void handleNewModuleAddition(AxisModule axisModule, String moduleName, String moduleVersion)
            throws Exception {

        configRegistry.beginTransaction();
        Resource module = configRegistry.newCollection();
        module.addProperty(RegistryResources.ModuleProperties.NAME, moduleName);
        if (!moduleVersion.equals(RegistryResources.ModuleProperties.UNDEFINED)) {
            module.addProperty(RegistryResources.ModuleProperties.VERSION, moduleVersion);
        }
//        AxisConfigurator configurator = axisConfig.getConfigurator();
        boolean isGloballyEngaged = false;
//                    if(configurator instanceof CarbonAxisConfigurator) {
//                        isGloballyEngaged = ((CarbonAxisConfigurator) configurator).isGlobalyEngaged(axisModule);
//                    } else if (configurator instanceof TenantAxisConfigurator) {
//                        isGloballyEngaged = ((TenantAxisConfigurator) configurator).isGlobalyEngaged(axisModule);
//                    }
        module.addProperty(RegistryResources.ModuleProperties.GLOBALLY_ENGAGED, String.valueOf(isGloballyEngaged));

        String registryResourcePath = RegistryResources.MODULES + moduleName + "/" + moduleVersion;
        configRegistry.put(registryResourcePath, module);

        // add the module parameters
        writeParameters(axisModule.getParameters(), registryResourcePath);

        // Persist module policies
        List<Resource> modulePolicies = getModulePolicies(axisModule);
        for (Resource modulePolicy : modulePolicies) {
            configRegistry.put(RegistryResources.MODULES + moduleName + "/" + moduleVersion
                            + RegistryResources.POLICIES
                            + modulePolicy.getProperty(RegistryResources.ModuleProperties.POLICY_UUID),
                    modulePolicy);
        }
        module.addProperty(RegistryResources.SUCCESSFULLY_ADDED, "true");
        configRegistry.put(registryResourcePath, module);

        configRegistry.commitTransaction();
        if (log.isDebugEnabled()) {
            log.debug("Added new module - " + axisModule.getName() + "-" + axisModule.getVersion().toString());
        }

    }

    public Resource getModule(String moduleName, String moduleVersion) throws Exception {

        String modulePath = RegistryResources.MODULES + moduleName + "/" + moduleVersion;
        if (configRegistry.resourceExists(modulePath)) {
            Resource resource = configRegistry.get(modulePath);
            if (resource.getProperty(RegistryResources.SUCCESSFULLY_ADDED) != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Successfully retrieved " + moduleName + " module resource from registry");
                }

                return resource;
            }
        }
        return null;
    }

    /**
     * Handle initialization of an already existing module in registry. Writes all parameters
     * and engaged policies into the registry.
     *
     * @param moduleResource resource for the module
     * @param axisModule     AxisModule instance
     * @throws Exception on registry transaction error
     */
    public void handleExistingModuleInit(Resource moduleResource, AxisModule axisModule) throws Exception {

        String paramResource = moduleResource.getPath() + RegistryResources.PARAMETERS;
        configRegistry.beginTransaction();

        // Add the Module Parameters
        if (configRegistry.resourceExists(paramResource)) {
            Collection parameters = (Collection) configRegistry.get(paramResource);
            for (String param : parameters.getChildren()) {
                Resource resource = configRegistry.get(param);
                if (!(resource instanceof Collection)) {
                    StAXOMBuilder builder = new StAXOMBuilder(resource.getContentStream());
                    Parameter parameter = ParameterUtil.createParameter(builder
                            .getDocumentElement());
                    Parameter p = axisModule.getParameter(resource
                            .getProperty(RegistryResources.NAME));
                    if (!(p != null && p.isLocked())) {
                        axisModule.addParameter(parameter);
                    }
                }
                resource.discard();
            }
            parameters.discard();
        }
        axisModule.getPolicySubject().clear();

        // Load policies from registry into AxisModule.
        String modulePath = ServicePersistenceUtil.getResourcePath(axisModule);
        String policiesResource = modulePath + RegistryResources.POLICIES;
        if (configRegistry.resourceExists(policiesResource)) {
            Collection policies = (Collection) configRegistry.get(policiesResource);
            for (String policyResource : policies.getChildren()) {
                Resource resource = configRegistry.get(policyResource);
                if (!(resource instanceof Collection)) {
                    Policy policy = PolicyEngine.getPolicy(resource.getContentStream());
                    axisModule.getPolicySubject().attachPolicy(policy);
                }
                resource.discard();
            }
            policies.discard();
        }

        handleGlobalParams(axisModule, moduleResource);
        configRegistry.commitTransaction();

        if (log.isDebugEnabled()) {
            log.debug("Initialized module - " + Utils
                    .getModuleName(axisModule.getName(), axisModule.getVersion().toString()));
        }

    }

    /**
     * Get module policies
     *
     * @param axisModule Axis module
     * @return List of resources
     * @throws Exception
     */
    private List<Resource> getModulePolicies(AxisModule axisModule) throws Exception {

        List<Resource> modulePolicies = new ArrayList<Resource>();
        PolicySubject modulePolicySubject = axisModule.getPolicySubject();
        List<PolicyComponent> policyList = new ArrayList<PolicyComponent>(
                modulePolicySubject.getAttachedPolicyComponents());

        // Get the merged module policy
        Policy policy = null;
        for (Object policyElement : policyList) {
            if (policyElement instanceof Policy) {
                policy = (policy == null) ?
                        (Policy) policyElement : policy.merge((Policy) policyElement);
            } else {
                PolicyReference policyReference = (PolicyReference) policyElement;
                String key = policyReference.getURI();
                int pos = key.indexOf("#");
                if (pos == 0) {
                    key = key.substring(1);
                } else if (pos > 0) {
                    key = key.substring(0, pos);
                }

                PolicyComponent attachedPolicyComponent = modulePolicySubject.getAttachedPolicyComponent(key);
                if (attachedPolicyComponent != null && attachedPolicyComponent instanceof Policy) {
                    policy = (Policy) attachedPolicyComponent;
                }
            }
        }

        if (policy != null) {
            if (policy.getId() == null) {
                policy.setId(UUIDGenerator.getUUID());
            }
            // Create a configRegistry resource from the merged module policy
            Resource policyResource = createPolicyResource(policy, policy.getId(), PolicyInclude.AXIS_MODULE_POLICY);
            policyResource.setProperty(RegistryResources.ModuleProperties.VERSION, axisModule.getVersion().toString());
            modulePolicies.add(policyResource);
        }

        return modulePolicies;
    }

    /**
     * Handle global parameters
     *
     * @param axisModule     Axis module
     * @param moduleResource Module resource
     * @throws AxisFault
     */
    public static void handleGlobalParams(AxisModule axisModule, Resource moduleResource) throws AxisFault {

        if (Boolean.parseBoolean(moduleResource.getProperty(RegistryResources.ModuleProperties.GLOBALLY_ENGAGED))) {
            axisModule.addParameter(new Parameter(GLOBALLY_ENGAGED_PARAM_NAME, Boolean.TRUE.toString()));
            axisModule.getParent().engageModule(axisModule);
        }
        if (Boolean.parseBoolean(moduleResource.getProperty(GLOBALLY_ENGAGED_CUSTOM))) {
            axisModule.addParameter(new Parameter(GLOBALLY_ENGAGED_PARAM_NAME, Boolean.TRUE.toString()));
        }
    }

    /**
     * Get transport resource
     *
     * @param name     Transport resource name
     * @param listener Is Listener
     * @return
     * @throws RegistryException
     */
    private Resource getTransportResource(String name, boolean listener) throws RegistryException {

        String path = RegistryResources.TRANSPORTS + name + "/";
        if (listener) {
            path += TRANSPORT_LISTENER;
        } else {
            path += TRANSPORT_SENDER;
        }
        if (configRegistry.resourceExists(path)) {
            return configRegistry.get(path);
        }
        return null;
    }

    /**
     * Handle new binding addition
     *
     * @param serviceResourcePath Service resource path
     * @param axisBinding         Axis binding
     * @throws RegistryException
     */
    private void handleNewBindingAddition(String serviceResourcePath, AxisBinding axisBinding)
            throws RegistryException {

        Iterator operations = axisBinding.getChildren();
        while (operations.hasNext()) {
            AxisBindingOperation bo = (AxisBindingOperation) operations.next();
            writeAxisDescription(bo, bo.getName().getLocalPart(),
                    serviceResourcePath + RegistryResources.ServiceProperties.BINDINGS + axisBinding.getName()
                            .getLocalPart() + RegistryResources.ServiceProperties.OPERATIONS + "/" + bo.getName()
                            .getLocalPart());
        }
    }

    /**
     * Load policies
     *
     * @param axisDescription Axis description
     * @param policyIdList    Policy id list
     * @param servicePath     Service path
     * @throws RegistryException
     */
    private void loadPolicies(AxisDescription axisDescription, List policyIdList, String servicePath)
            throws RegistryException {

        if (axisDescription == null) {
            return;
        }
        axisDescription.getPolicySubject().clear();

        if (policyIdList != null) {
            for (Object servicePolicy : policyIdList) {

                String currentPolicyUUID = (String) servicePolicy;
                String policyResourcePath = servicePath + RegistryResources.POLICIES + currentPolicyUUID;
                if (configRegistry.resourceExists(policyResourcePath)) {

                    Resource policyResource = configRegistry.get(policyResourcePath);
                    if (!(policyResource instanceof org.wso2.carbon.registry.core.Collection)) {
                        Policy policy = PolicyEngine.getPolicy(policyResource.getContentStream());
                        axisDescription.getPolicySubject().attachPolicy(policy);
                    }
                    policyResource.discard();
                } else {
                    log.warn("Failed to load Policy with ID " + currentPolicyUUID + ". The Policy does not exist.");
                }
            }
        }
    }

    /**
     * Get service policies
     *
     * @param axisService   Axis service
     * @return Resource list
     * @throws Exception
     */
    private List<Resource> getServicePolicies(AxisService axisService) throws XMLStreamException, RegistryException {

        List<Resource> policyResources = new ArrayList<Resource>();
        String serviceResourcePath = ServicePersistenceUtil.getResourcePath(axisService);

        // Get Service Policy
        List<PolicyComponent> servicePolicyList = new ArrayList<PolicyComponent>(axisService
                .getPolicySubject().getAttachedPolicyComponents());
        Policy servicePolicy = PolicyUtil.getMergedPolicy(servicePolicyList, axisService);

        if (servicePolicy != null) {
            // Add this policy as a resource to the list
            addPolicyResource(policyResources, servicePolicy, PolicyInclude.AXIS_SERVICE_POLICY);
            // Refer this policy from the service
            setResourcePolicyId(serviceResourcePath, servicePolicy.getId());
        }

        // Get Service Operation Policies
        Iterator serviceOperations = axisService.getOperations();
        while (serviceOperations.hasNext()) {
            AxisOperation axisOperation = (AxisOperation) serviceOperations.next();
            String resourcePath = ServicePersistenceUtil.getResourcePath(axisOperation);
            if (!configRegistry.resourceExists(resourcePath)) {
                continue;
            }
            Resource operationResource = configRegistry.get(resourcePath);

            // Get the operation policy
            List<PolicyComponent> opPolicyList = new ArrayList<PolicyComponent>(axisOperation
                    .getPolicySubject().getAttachedPolicyComponents());
            Policy operationPolicy = PolicyUtil.getMergedPolicy(opPolicyList, axisOperation);

            if (operationPolicy != null) {
                // Add this policy as a resource to the list
                addPolicyResource(policyResources, operationPolicy, PolicyInclude.AXIS_OPERATION_POLICY);
                // Refer this policy from the operation resource
                operationResource.setProperty(RegistryResources.ServiceProperties.POLICY_UUID, operationPolicy.getId());
            }

            if (!(axisOperation instanceof OutOnlyAxisOperation)) {
                // Get Service Operation Message Policies
                AxisMessage axisInMessage = axisOperation.getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE);

                // Get the message in policy
                List<PolicyComponent> messageInPolicyList = new ArrayList<PolicyComponent>(
                        axisInMessage.getPolicySubject().getAttachedPolicyComponents());
                Policy messageInPolicy = PolicyUtil.getMergedPolicy(messageInPolicyList, axisInMessage);

                if (messageInPolicy != null) {
                    // Add this policy as a resource to the list
                    addPolicyResource(policyResources, messageInPolicy, PolicyInclude.AXIS_MESSAGE_POLICY);
                    // Refer this policy from the operation resource
                    operationResource.setProperty(RegistryResources.ServiceProperties.MESSAGE_IN_POLICY_UUID,
                            messageInPolicy.getId());
                }
            }

            // Get the message out policy
            if (!(axisOperation instanceof InOnlyAxisOperation)) {
                AxisMessage axisOutMessage = axisOperation.getMessage(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
                List<PolicyComponent> messageOutPolicyList = new ArrayList<PolicyComponent>(
                        axisOutMessage.getPolicySubject().getAttachedPolicyComponents());
                Policy messageOutPolicy = PolicyUtil.getMergedPolicy(messageOutPolicyList, axisOutMessage);

                if (messageOutPolicy != null) {
                    // Add this policy as a resource to the list
                    addPolicyResource(policyResources, messageOutPolicy, PolicyInclude.AXIS_MESSAGE_POLICY);
                    // Refer this policy from the operation resource
                    operationResource.setProperty(RegistryResources.ServiceProperties.MESSAGE_OUT_POLICY_UUID,
                            messageOutPolicy.getId());
                }
            }

            // Update the operation resource in configRegistry
            configRegistry.put(serviceResourcePath + RegistryResources.ServiceProperties.OPERATIONS +
                    axisOperation.getName().getLocalPart(), operationResource);
            operationResource.discard();
        }

        // Get binding policies
        Map endPointMap = axisService.getEndpoints();

        /**
         * We don't have a way of accessing all bindings directly from axis service. Therefore,
         * we have to access those trough endpoints. So the same binding can be found again and
         * again. To remove that overhead, we memorize the treated bindings.
         */
        ArrayList<String> bindingsList = new ArrayList<String>();
        for (Object o : endPointMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            AxisBinding currentAxisBinding = ((AxisEndpoint) entry.getValue()).getBinding();

            if (bindingsList.contains(currentAxisBinding.getName().getLocalPart())) {
                continue;
            }
            // If we process this binding, add it's name to our list
            bindingsList.add(currentAxisBinding.getName().getLocalPart());

            // Get current binding Policy
            List<PolicyComponent> bindingPolicyList = new ArrayList<PolicyComponent>(
                    currentAxisBinding.getPolicySubject().getAttachedPolicyComponents());
            Policy bindingPolicy = PolicyUtil.getMergedPolicy(bindingPolicyList, currentAxisBinding);

            if (bindingPolicy != null) {
                // Add this policy as a resource to the list
                addPolicyResource(policyResources, bindingPolicy, PolicyInclude.BINDING_POLICY);
                // Refer this policy from the binding resource
                setResourcePolicyId(serviceResourcePath + RegistryResources.ServiceProperties.BINDINGS +
                        currentAxisBinding.getName().getLocalPart(), bindingPolicy.getId());
            }

            // Get Binding Operation Policies
            Iterator operations = currentAxisBinding.getChildren();
            while (operations.hasNext()) {
                AxisBindingOperation currentOperation = (AxisBindingOperation) operations.next();
                String opPath = getBindingOperationPath(serviceResourcePath, currentOperation);
                if (!configRegistry.resourceExists(opPath)) {
                    continue;
                }
                Resource bindingOperationResource = configRegistry.get(opPath);

                // Get current binding operation policy
                List<PolicyComponent> boPolicyList = new ArrayList<PolicyComponent>(
                        currentOperation.getPolicySubject().getAttachedPolicyComponents());
                Policy boPolicy = PolicyUtil.getMergedPolicy(boPolicyList, currentOperation);

                if (boPolicy != null) {
                    // Add this policy as a resource to the list
                    addPolicyResource(policyResources, boPolicy, PolicyInclude.BINDING_OPERATION_POLICY);
                    // Refer this policy from the binding operation
                    bindingOperationResource.setProperty(RegistryResources.ServiceProperties.POLICY_UUID,
                            boPolicy.getId());
                }

                // Get Binding Operation Message Policies
                AxisDescription boMessageIn = currentOperation.getChild(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
                if (boMessageIn != null) {
                    List<PolicyComponent> boMessageInPolicyList = new ArrayList<PolicyComponent>(
                            boMessageIn.getPolicySubject().getAttachedPolicyComponents());
                    Policy boMessageInPolicy = PolicyUtil.getMergedPolicy(boMessageInPolicyList, boMessageIn);

                    if (boMessageInPolicy != null) {
                        // Add this policy as a resource to the list
                        addPolicyResource(policyResources, boMessageInPolicy, PolicyInclude.BINDING_INPUT_POLICY);
                        // Refer this policy from the binding operation
                        bindingOperationResource.setProperty(RegistryResources.ServiceProperties.MESSAGE_IN_POLICY_UUID,
                                boMessageInPolicy.getId());
                    }
                }

                // Get binding operaion out policy
                AxisDescription boMessageOut = currentOperation
                        .getChild(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
                if (boMessageOut != null) {
                    List<PolicyComponent> boMessageOutPolicyList = new ArrayList<PolicyComponent>(
                            boMessageOut.getPolicySubject().getAttachedPolicyComponents());
                    Policy boMessageOutPolicy = PolicyUtil
                            .getMergedPolicy(boMessageOutPolicyList, boMessageOut);

                    if (boMessageOutPolicy != null) {
                        // Add this policy as a resource to the list
                        addPolicyResource(policyResources, boMessageOutPolicy, PolicyInclude.BINDING_OUTPUT_POLICY);
                        // Refer this policy from the binding operation
                        bindingOperationResource.setProperty(RegistryResources.ServiceProperties.
                                MESSAGE_OUT_POLICY_UUID, boMessageOutPolicy.getId());
                    }
                }

                // Update binding operation resource in configRegistry
                configRegistry.put(getBindingOperationPath(serviceResourcePath, currentOperation),
                        bindingOperationResource);
                bindingOperationResource.discard();
            }
        }
        return policyResources;
    }

    /**
     * Get property values
     *
     * @param resourcePath Resource path
     * @param property     Property
     * @return
     * @throws RegistryException
     */
    private List getPropertyValues(String resourcePath, String property) throws RegistryException {

        Resource resource = configRegistry.get(resourcePath);
        List values = resource.getPropertyValues(property);
        resource.discard();
        return values;
    }

    /**
     * Write parameters to registry
     *
     * @param paramList Parameter list
     * @param adPath    path
     * @throws Exception
     */
    private void writeParameters(ArrayList<Parameter> paramList, String adPath) throws AxisFault, RegistryException {

        for (Parameter parameter : paramList) {
            String paramName = parameter.getName();
            if (paramName != null && paramName.trim().length() != 0) {
                if (parameter.getParameterElement() == null && parameter.getValue() != null && parameter
                        .getValue() instanceof String) {
                    parameter = ParameterUtil.createParameter(paramName.trim(), (String) parameter.getValue());
                }
                if (parameter.getParameterElement() != null) {
                    Resource paramResource = configRegistry.newResource();
                    paramResource.setContent(parameter.getParameterElement().toString());
                    paramResource.addProperty(RegistryResources.NAME, parameter.getName());
                    configRegistry.put(adPath + RegistryResources.PARAMETERS + parameter.getName(), paramResource);
                    paramResource.discard();
                }
            }
        }
    }

    /**
     * Write axis description to registry
     *
     * @param axisDescription Axis description
     * @param nameProperty    Name of the property
     * @param path            Registry path
     * @throws RegistryException
     */
    private void writeAxisDescription(AxisDescription axisDescription, String nameProperty, String path)
            throws RegistryException {

        Collection collection = configRegistry.newCollection();
        String doc = axisDescription.getDocumentation();
        if (doc != null) {
            collection.setProperty(RegistryResources.ServiceProperties.DOCUMENTATION, doc);
        }
        collection.setProperty(RegistryResources.NAME, nameProperty);
        configRegistry.put(path, collection);
    }

    /**
     * Get binding operation path
     *
     * @param servicePath          Service path
     * @param axisBindingOperation Axis binding operation
     * @return Binding operation path
     */
    private static String getBindingOperationPath(String servicePath, AxisBindingOperation axisBindingOperation) {

        AxisBinding binding = axisBindingOperation.getAxisBinding();
        return servicePath + RegistryResources.ServiceProperties.BINDINGS + binding.getName().getLocalPart()
                + RegistryResources.ServiceProperties.OPERATIONS + axisBindingOperation.getName().getLocalPart();
    }

    /**
     * Is globally engaged
     *
     * @param resourcePath  Resource path
     * @return Is global engaged
     * @throws RegistryException
     */
    private boolean isGloballyEngaged(String resourcePath) throws RegistryException {

        if (configRegistry.resourceExists(resourcePath)) {
            Resource resource = configRegistry.get(resourcePath);
            boolean globallyEngaged = Boolean.parseBoolean(resource.getProperty(
                    RegistryResources.ModuleProperties.GLOBALLY_ENGAGED));
            resource.discard();
            return globallyEngaged;
        }
        return false;
    }

    /**
     * Add policy resource
     *
     * @param policyResources Policy resource
     * @param policy          Policy
     * @param policyType      Policy type
     * @throws Exception
     */
    private void addPolicyResource(List<Resource> policyResources, Policy policy, int policyType)
            throws XMLStreamException, RegistryException {

        if (policy.getId() == null) {
            policy.setId(UUID.randomUUID().toString());
            policyResources.add(createPolicyResource(policy, policy.getId(), policyType));
        } else {
            policyResources.add(createPolicyResource(policy, policy.getId(), policyType));
        }
    }

    /**
     * Create policy resource
     *
     * @param policy     Policy
     * @param policyId   Policy id
     * @param policyType Policy type
     * @return resource
     * @throws RegistryException
     * @throws XMLStreamException
     */
    private Resource createPolicyResource(Policy policy, String policyId, int policyType)
            throws RegistryException, XMLStreamException {

        Resource policyResource = configRegistry.newResource();
        policyResource.setProperty(RegistryResources.ServiceProperties.POLICY_UUID, policyId);
        // Set the policy as a string in the resource
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        policy.serialize(writer);
        writer.flush();
        policyResource.setContent(outputStream.toString());
        policyResource.setProperty(RegistryResources.ServiceProperties.POLICY_TYPE, "" + policyType);
        return policyResource;
    }

    /**
     * Set resource policy id
     *
     * @param resourcePath Resource path
     * @param policyId     Policy id
     * @throws RegistryException
     */
    private void setResourcePolicyId(String resourcePath, String policyId) throws RegistryException {

        if (configRegistry.resourceExists(resourcePath)) {
            Resource resource = configRegistry.get(resourcePath);
            resource.setProperty(RegistryResources.ServiceProperties.POLICY_UUID, policyId);
            // Update the service resource in configRegistry
            configRegistry.put(resourcePath, resource);
            resource.discard();
        }
    }

}