/*
 *Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.application.common.model;


public class ServiceProvider {

    private int applicationID = 0;
    private String applicationName;
    private String description;
    private ServiceProviderOwner owner;
    private InboundAuthenticationConfig inboundAuthenticationConfig;
    private LocalAndOutboundAuthenticationConfig localAndOutBoundAuthenticationConfig;
    private RequestPathAuthenticator[] requestPathAuthenticators;
    private InboundProvisioningConfiguration inboundProvisioningConfiguration;
    private OutboundProvisioningConfiguration outboundProvisioningConfiguration;
    private ClaimConfiguration claimConfiguration;
    private PermissionsAndRoleConfiguration permissionAndRoleConfiguration;

    /**
     * 
     * @return
     */
    public int getApplicationID() {
        return applicationID;
    }

    /**
     * 
     * @param applicationID
     */
    public void setApplicationID(int applicationID) {
        this.applicationID = applicationID;
    }

    /**
     * 
     * @return
     */
    public InboundAuthenticationConfig getInboundAuthenticationConfig() {
        return inboundAuthenticationConfig;
    }

    /**
     * 
     * @param inboundAuthenticationConfig
     */
    public void setInboundAuthenticationConfig(
            InboundAuthenticationConfig inboundAuthenticationConfig) {
        this.inboundAuthenticationConfig = inboundAuthenticationConfig;
    }

    /**
     * 
     * @return
     */
    public LocalAndOutboundAuthenticationConfig getLocalAndOutBoundAuthenticationConfig() {
        return localAndOutBoundAuthenticationConfig;
    }

    /**
     * 
     * @param localAndOutBoundAuthenticationConfig
     */
    public void setLocalAndOutBoundAuthenticationConfig(
            LocalAndOutboundAuthenticationConfig localAndOutBoundAuthenticationConfig) {
        this.localAndOutBoundAuthenticationConfig = localAndOutBoundAuthenticationConfig;
    }

    /**
     * 
     * @param requestPathAuthenticators
     */
    public void setRequestPathAuthenticators(RequestPathAuthenticator[] requestPathAuthenticators) {
        this.requestPathAuthenticators = requestPathAuthenticators;
    }

    /**
     * 
     * @return
     */
    public RequestPathAuthenticator[] getRequestPathAuthenticators() {
        return requestPathAuthenticators;
    }

    /**
     * 
     * @return
     */
    public InboundProvisioningConfiguration getInboundProvisioningConfiguration() {
        return inboundProvisioningConfiguration;
    }

    /**
     * 
     * @param inboundProvisioningConfiguration
     */
    public void setInboundProvisioningConfiguration(
            InboundProvisioningConfiguration inboundProvisioningConfiguration) {
        this.inboundProvisioningConfiguration = inboundProvisioningConfiguration;
    }

    /**
     * 
     * @return
     */
    public OutboundProvisioningConfiguration getOutboundProvisioningConfiguration() {
        return outboundProvisioningConfiguration;
    }

    /**
     * 
     * @param outboundProvisioningConfiguration
     */
    public void setOutboundProvisioningConfiguration(
            OutboundProvisioningConfiguration outboundProvisioningConfiguration) {
        this.outboundProvisioningConfiguration = outboundProvisioningConfiguration;
    }

    /**
     * 
     * @return
     */
    public ClaimConfiguration getClaimConfiguration() {
        return claimConfiguration;
    }

    /**
     * 
     * @param claimConfiguration
     */
    public void setClaimConfiguration(ClaimConfiguration claimConfiguration) {
        this.claimConfiguration = claimConfiguration;
    }

    /**
     * 
     * @return
     */
    public PermissionsAndRoleConfiguration getPermissionAndRoleConfiguration() {
        return permissionAndRoleConfiguration;
    }

    /**
     * 
     * @param permissionAndRoleConfiguration
     */
    public void setPermissionAndRoleConfiguration(
            PermissionsAndRoleConfiguration permissionAndRoleConfiguration) {
        this.permissionAndRoleConfiguration = permissionAndRoleConfiguration;
    }

    /**
     * 
     * @return
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * 
     * @param applicationName
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * 
     * @return
     */
    public ServiceProviderOwner getOwner() {
        return owner;
    }

    /**
     * 
     * @param owner
     */
    public void setOwner(ServiceProviderOwner owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}