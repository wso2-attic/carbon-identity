/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.security.config;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.ws.security.util.UUIDGenerator;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.utils.Transaction;
import org.wso2.carbon.security.SecurityServiceHolder;
import org.wso2.carbon.utils.ServerException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SecurityServiceAdmin {

    private static Log log = LogFactory.getLog(SecurityServiceAdmin.class);
    protected AxisConfiguration axisConfig = null;
    private Registry registry = null;

    public SecurityServiceAdmin(AxisConfiguration config) {

        this.axisConfig = config;
        this.registry = SecurityServiceHolder.getRegistry();
    }

    public SecurityServiceAdmin(AxisConfiguration config, Registry registry) {

        this.axisConfig = config;
        this.registry = SecurityServiceHolder.getRegistry();
    }

    /**
     * Add security policy to all bindings
     *
     * @param axisService Axis service
     * @param policy      Security Policy
     * @throws ServerException
     */
    public void addSecurityPolicyToAllBindings(AxisService axisService, Policy policy) throws ServerException {

        try {
            if (policy.getId() == null) {
                policy.setId(UUIDGenerator.getUUID());
            }
            this.registry = SecurityServiceHolder.getRegistry();
            String servicePath = getRegistryServicePath(axisService);

            String policyResourcePath = servicePath + RegistryResources.POLICIES + policy.getId();
            if (!registry.resourceExists(policyResourcePath)) {

                Resource policyResource = registry.newResource();
                policyResource.setProperty(RegistryResources.ServiceProperties.POLICY_UUID, policy.getId());
                //TODO check with registry team, do a performance improvement
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(baos);
                policy.serialize(writer);
                writer.flush();
                policyResource.setContent(baos.toString());
                policyResource.setProperty(RegistryResources.ServiceProperties.POLICY_TYPE,
                        "" + PolicyInclude.BINDING_POLICY);
                registry.put(policyResourcePath, policyResource);
            }

            Map endPointMap = axisService.getEndpoints();
            List<String> lst = new ArrayList<String>();
            for (Object o : endPointMap.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                AxisEndpoint point = (AxisEndpoint) entry.getValue();
                AxisBinding binding = point.getBinding();
                binding.getPolicySubject().attachPolicy(policy);
                String bindingName = binding.getName().getLocalPart();
                if (lst.contains(bindingName)) {
                    continue;
                } else {
                    lst.add(bindingName);
                }
            }

            boolean transactionStarted = Transaction.isStarted();
            if (!transactionStarted) {
                registry.beginTransaction();
            }

            for (String bindingName : lst) {
                String bindingResourcePath = servicePath + RegistryResources.ServiceProperties.BINDINGS + bindingName;
                Resource bindingResource;
                if (registry.resourceExists(bindingResourcePath)) {
                    bindingResource = registry.get(bindingResourcePath);
                } else {
                    bindingResource = registry.newResource();
                }
                bindingResource.addProperty(RegistryResources.ServiceProperties.POLICY_UUID, policy.getId());
                registry.put(bindingResourcePath, bindingResource);
            }
            if (!transactionStarted) {
                registry.commitTransaction();
            }
        } catch (RegistryException e) {
            try {
                registry.rollbackTransaction();
            } catch (Exception ex) {
                throw new ServerException("Error occurred while rollback transaction", ex);
            }
            throw new ServerException(
                    "Error occurred while adding security policy to all bindings for service " + axisService.getName(),
                    e);
        } catch (XMLStreamException e) {
            try {
                registry.rollbackTransaction();
            } catch (Exception ex) {
                throw new ServerException("Error occurred while rollback transaction", ex);
            }
            throw new ServerException(
                    "Error occurred while adding security policy to all bindings for service " + axisService.getName(),
                    e);
        }
    }

    /**
     * Get registry service path
     *
     * @param service Axis service
     * @return Registry service path
     */
    private String getRegistryServicePath(AxisService service) {

        return RegistryResources.SERVICE_GROUPS + service.getAxisServiceGroup().getServiceGroupName() +
                RegistryResources.SERVICES + service.getName();
    }

    /**
     * Remove security policy
     *
     * @param axisService Axis service
     * @param uuid        UUID
     * @throws ServerException
     */
    public void removeSecurityPolicyFromAllBindings(AxisService axisService, String uuid) throws ServerException {

        try {
            String servicePath = getRegistryServicePath(axisService);
            Map endPointMap = axisService.getEndpoints();
            List<String> lst = new ArrayList<String>();
            for (Object o : endPointMap.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                AxisEndpoint point = (AxisEndpoint) entry.getValue();
                AxisBinding binding = point.getBinding();
                binding.getPolicySubject().detachPolicyComponent(uuid);
                String bindingName = binding.getName().getLocalPart();

                if (lst.contains(bindingName)) {
                    continue;
                } else {
                    lst.add(bindingName);
                }
            }

            boolean transactionStarted = Transaction.isStarted();
            if (!transactionStarted) {
                registry.beginTransaction();
            }

            for (String bindingName : lst) {
                String bindingResourcePath = servicePath + RegistryResources.ServiceProperties.BINDINGS + bindingName;
                Resource bindingResource = registry.get(bindingResourcePath);
                List uuids = bindingResource.getPropertyValues(RegistryResources.ServiceProperties.POLICY_UUID);
                uuids.remove(uuid);
                bindingResource.setProperty(RegistryResources.ServiceProperties.POLICY_UUID, uuids);
                registry.put(bindingResourcePath, bindingResource);
            }
            if (!transactionStarted) {
                registry.commitTransaction();
            }
        } catch (RegistryException e) {
            try {
                registry.rollbackTransaction();
            } catch (Exception ex) {
                log.error("Error occurred while rolling back transaction", ex);
            }
            String error = "Error occurred while removing security policy from all bindings for service " + axisService
                    .getName();
            throw new ServerException(error, e);
        }
    }

    /**
     * Set service parameter element
     *
     * @param serviceName Axis service name
     * @param parameter   Parameters
     * @throws AxisFault
     */
    public void setServiceParameterElement(String serviceName, Parameter parameter) throws AxisFault {

        AxisService axisService = axisConfig.getService(serviceName);
        if (axisService == null) {
            throw new AxisFault("Invalid service name '" + serviceName + "'");
        }
        Parameter serviceParameter = axisService.getParameter(parameter.getName());
        if (serviceParameter != null) {
            if (!serviceParameter.isLocked()) {
                axisService.addParameter(parameter);
            }
        } else {
            axisService.addParameter(parameter);
        }
    }

}