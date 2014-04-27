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
package org.wso2.carbon.identity.application.mgt.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.wso2.carbon.identity.application.common.model.xsd.ApplicationPermission;
import org.wso2.carbon.identity.application.common.model.xsd.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.xsd.Claim;
import org.wso2.carbon.identity.application.common.model.xsd.ClaimConfiguration;
import org.wso2.carbon.identity.application.common.model.xsd.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.xsd.FederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.xsd.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationRequest;
import org.wso2.carbon.identity.application.common.model.xsd.InboundProvisioningConfiguration;
import org.wso2.carbon.identity.application.common.model.xsd.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.xsd.LocalAuthenticator;
import org.wso2.carbon.identity.application.common.model.xsd.LocalRole;
import org.wso2.carbon.identity.application.common.model.xsd.OutboundProvisioningConfiguration;
import org.wso2.carbon.identity.application.common.model.xsd.PermissionsAndRoleConfiguration;
import org.wso2.carbon.identity.application.common.model.xsd.ProvisioningConnector;
import org.wso2.carbon.identity.application.common.model.xsd.RequestPathAuthenticator;
import org.wso2.carbon.identity.application.common.model.xsd.RoleMapping;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;

public class ApplicationBean {

    public static final String AUTH_TYPE_DEFAULT = "default";
    public static final String AUTH_TYPE_LOCAL = "local";
    public static final String AUTH_TYPE_FEDERATED = "federated";
    public static final String AUTH_TYPE_FLOW = "flow";
    public static final String IDP_LOCAL_NAME = "LOCAL";

    public static final String LOCAL_IDP = "wso2carbon-local-idp";

    private ServiceProvider serviceProvider;
    private FederatedIdentityProvider[] federatedIdentityProviders;
    private LocalAuthenticator[] localAuthenticators;
    private RequestPathAuthenticator[] requestPathAuthenticators;
    private Map<String, String> roleMap;
    private Map<String, String> claimMap;
    private Map<String, String> requestedClaims = new HashMap<String, String>();
    private String samlIssuer;
    private String oauthAppName;
    private String wstrustEp;
    private String passivests;
    private String openid;
    private String[] claimUris;

    public ApplicationBean() {

    }

    public void reset() {
        serviceProvider = null;
        federatedIdentityProviders = null;
        localAuthenticators = null;
        requestPathAuthenticators = null;
        roleMap = null;
        claimMap = null;
        requestedClaims = new HashMap<String, String>();;
        samlIssuer = null;
        oauthAppName = null;
        wstrustEp = null;
        passivests = null;
        openid = null;
    }

    /**
     * 
     * @return
     */
    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    /**
     * 
     * @param serviceProvider
     */
    public void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    /**
     * 
     * @return
     */
    public String getAuthenticationType() {
        return serviceProvider.getLocalAndOutBoundAuthenticationConfig().getAuthenticationType();
    }

    /**
     * 
     * @param type
     */
    public void setAuthenticationType(String type) {
        serviceProvider.getLocalAndOutBoundAuthenticationConfig().setAuthenticationType(type);
    }

    /**
     * 
     * @param type
     * @return
     */
    public String getStepZeroAuthenticatorName(String type) {
        if (AUTH_TYPE_LOCAL.equalsIgnoreCase(type)) {
            if (serviceProvider.getLocalAndOutBoundAuthenticationConfig().getAuthenticationSteps() != null
                    && serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                            .getAuthenticationSteps().length > 0
                    && serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                            .getAuthenticationSteps()[0].getLocalAuthenticators() != null
                    && serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                            .getAuthenticationSteps()[0].getLocalAuthenticators()[0] != null) {
                return serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                        .getAuthenticationSteps()[0].getLocalAuthenticators()[0].getName();
            }
        }

        if (AUTH_TYPE_FEDERATED.equalsIgnoreCase(type)) {
            if (serviceProvider.getLocalAndOutBoundAuthenticationConfig().getAuthenticationSteps() != null
                    && serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                            .getAuthenticationSteps().length > 0
                    && serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                            .getAuthenticationSteps()[0].getFederatedIdentityProviders() != null
                    && serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                            .getAuthenticationSteps()[0].getFederatedIdentityProviders().length > 0
                    && serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                            .getAuthenticationSteps()[0].getFederatedIdentityProviders()[0] != null) {
                return serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                        .getAuthenticationSteps()[0].getFederatedIdentityProviders()[0]
                        .getIdentityProviderName();
            }
        }

        return null;
    }

    public void setStepZeroAuthenticatorName(String type, String name) {
        if (AUTH_TYPE_LOCAL.equalsIgnoreCase(type)) {
            LocalAuthenticator localAuthenticator = new LocalAuthenticator();
            localAuthenticator.setName(name);
            AuthenticationStep authStep = new AuthenticationStep();
            authStep.setLocalAuthenticators(new LocalAuthenticator[] { localAuthenticator });

        }

    }

    /**
     * 
     * @return
     */
    public FederatedIdentityProvider[] getFederatedIdentityProviders() {
        return federatedIdentityProviders;
    }

    /**
     * 
     * @param federatedIdentityProviders
     */
    public void setFederatedIdentityProviders(FederatedIdentityProvider[] federatedIdentityProviders) {
        this.federatedIdentityProviders = federatedIdentityProviders;
    }

    /**
     * 
     * @return
     */
    public LocalAuthenticator[] getLocalAuthenticators() {
        return localAuthenticators;
    }

    /**
     * 
     * @param localAuthenticators
     */
    public void setLocalAuthenticators(LocalAuthenticator[] localAuthenticators) {
        this.localAuthenticators = localAuthenticators;
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
     * @param requestPathAuthenticators
     */
    public void setRequestPathAuthenticators(RequestPathAuthenticator[] requestPathAuthenticators) {
        this.requestPathAuthenticators = requestPathAuthenticators;
    }

    /**
     * 
     * @return
     */
    public List<String> getPermissions() {

        List<String> permList = new ArrayList<String>();

        if (serviceProvider != null && serviceProvider.getPermissionAndRoleConfiguration() != null) {
            PermissionsAndRoleConfiguration permissionAndRoleConfig = serviceProvider
                    .getPermissionAndRoleConfiguration();
            if (permissionAndRoleConfig != null) {
                ApplicationPermission[] permissions = permissionAndRoleConfig.getPermissions();
                if (permissions != null && permissions.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i] != null) {
                            permList.add(permissions[i].getValue());
                        }
                    }
                }
            }

        }

        return permList;
    }

    /**
     * 
     * @param permissions
     */
    public void setPermissions(String[] permissions) {
        ApplicationPermission[] applicationPermission = new ApplicationPermission[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            ApplicationPermission appPermission = new ApplicationPermission();
            appPermission.setValue(permissions[i]);
        }
        serviceProvider.getPermissionAndRoleConfiguration().setPermissions(applicationPermission);
    }

    /**
     * 
     * @return
     */
    public String getRoleClaimUri() {
        if (serviceProvider.getClaimConfiguration() != null) {
            return serviceProvider.getClaimConfiguration().getRoleClaimURI();
        } else {
            return null;
        }
    }

    /**
     * 
     * @param roleClaimUri
     */
    public void setRoleClaimUri(String roleClaimUri) {

        if (roleClaimUri != null) {
            if (serviceProvider.getClaimConfiguration() != null) {
                serviceProvider.getClaimConfiguration().setRoleClaimURI(roleClaimUri);
            } else {
                ClaimConfiguration claimConfig = new ClaimConfiguration();
                claimConfig.setRoleClaimURI(roleClaimUri);
                serviceProvider.setClaimConfiguration(claimConfig);
            }
        }
    }

    /**
     * 
     * @return
     */
    public String getUserClaimUri() {
        if (serviceProvider.getClaimConfiguration() != null) {
            return serviceProvider.getClaimConfiguration().getUserClaimURI();
        } else {
            return null;
        }
    }

    /**
     * 
     * @param roleClaimUri
     */
    public void setUserClaimUri(String userClaimUri) {

        if (userClaimUri != null) {
            if (serviceProvider.getClaimConfiguration() != null) {
                serviceProvider.getClaimConfiguration().setUserClaimURI(userClaimUri);
            } else {
                ClaimConfiguration claimConfig = new ClaimConfiguration();
                claimConfig.setUserClaimURI(userClaimUri);
                serviceProvider.setClaimConfiguration(claimConfig);
            }
        }
    }

    /**
     * 
     * @return
     */
    public Map<String, String> getRoleMapping() {

        if (serviceProvider.getPermissionAndRoleConfiguration() == null) {
            return new HashMap<String, String>();
        }

        RoleMapping[] roleMapping = serviceProvider.getPermissionAndRoleConfiguration()
                .getRoleMappings();

        if (roleMap != null) {
            return roleMap;
        }

        roleMap = new HashMap<String, String>();

        if (roleMapping != null) {
            for (int i = 0; i < roleMapping.length; i++) {
                roleMap.put(roleMapping[i].getLocalRole().getLocalRoleName(),
                        roleMapping[i].getRemoteRole());
            }
        }

        return roleMap;
    }

    /**
     * 
     * @param roleMapping
     */
    public void addRoleMapping(String spRole, String localRole) {
        roleMap.put(localRole, spRole);
    }

    /**
     * 
     * @return
     */
    public Map<String, String> getClaimMapping() {

        if (serviceProvider.getClaimConfiguration() == null) {
            return new HashMap<String, String>();
        }

        ClaimMapping[] claimMapping = serviceProvider.getClaimConfiguration().getClaimMappings();

        if (claimMap != null) {
            return claimMap;
        }

        claimMap = new HashMap<String, String>();

        if (claimMapping != null) {
            for (int i = 0; i < claimMapping.length; i++) {
                if (claimMapping[i] != null && claimMapping[i].getSpClaim() != null
                        && claimMapping[i].getLocalClaim() != null) {
                    claimMap.put(claimMapping[i].getLocalClaim().getClaimUri(), claimMapping[i]
                            .getSpClaim().getClaimUri());
                    if (claimMapping[i].getSpClaim().getRequested()) {
                        requestedClaims.put(claimMapping[i].getSpClaim().getClaimUri(), "true");
                    } else {
                        requestedClaims.put(claimMapping[i].getSpClaim().getClaimUri(), "false");
                    }
                }
            }
        }

        return claimMap;
    }

    /**
     * 
     * @return
     */
    public String getSAMLIssuer() {

        if (samlIssuer != null) {
            return samlIssuer;
        }

        InboundAuthenticationRequest[] authRequest = serviceProvider
                .getInboundAuthenticationConfig().getInboundAuthenticationRequests();

        if (authRequest != null) {
            for (int i = 0; i < authRequest.length; i++) {
                if ("samlsso".equalsIgnoreCase(authRequest[i].getType())) {
                    samlIssuer = authRequest[i].getClientID();
                    break;
                }
            }
        }

        return samlIssuer;
    }

    /**
     * 
     * @param issuerName
     */
    public void setSAMLIssuer(String issuerName) {
        this.samlIssuer = issuerName;
    }

    /**
     * 
     * @param oauthAppName
     */
    public void setOIDCAppName(String oauthAppName) {
        this.oauthAppName = oauthAppName;
    }

    /**
     * 
     * @return
     */
    public String getOIDCClientId() {

        if (oauthAppName != null) {
            return oauthAppName;
        }

        InboundAuthenticationRequest[] authRequest = serviceProvider
                .getInboundAuthenticationConfig().getInboundAuthenticationRequests();

        if (authRequest != null) {
            for (int i = 0; i < authRequest.length; i++) {
                if ("oauth2".equalsIgnoreCase(authRequest[i].getType())) {
                    oauthAppName = authRequest[i].getClientID();
                    break;
                }
            }
        }

        return oauthAppName;
    }

    /**
     * 
     * @return
     */
    public String getOpenIDRealm() {

        if (openid != null) {
            return openid;
        }

        InboundAuthenticationRequest[] authRequest = serviceProvider
                .getInboundAuthenticationConfig().getInboundAuthenticationRequests();
        if (authRequest != null) {
            for (int i = 0; i < authRequest.length; i++) {
                if ("openid".equalsIgnoreCase(authRequest[i].getType())) {
                    openid = authRequest[i].getClientID();
                    break;
                }
            }
        }
        return openid;
    }

    /**
     * 
     * @return
     */
    public String getWstrustSP() {
        if (wstrustEp != null) {
            return wstrustEp;
        }

        InboundAuthenticationRequest[] authRequest = serviceProvider
                .getInboundAuthenticationConfig().getInboundAuthenticationRequests();
        if (authRequest != null) {
            for (int i = 0; i < authRequest.length; i++) {
                if ("wstrust".equalsIgnoreCase(authRequest[i].getType())) {
                    wstrustEp = authRequest[i].getClientID();
                    break;
                }
            }
        }

        return wstrustEp;
    }

    /**
     * 
     * @return
     */
    public String getPassiveSTSRealm() {

        if (passivests != null) {
            return passivests;
        }

        InboundAuthenticationRequest[] authRequest = serviceProvider
                .getInboundAuthenticationConfig().getInboundAuthenticationRequests();

        if (authRequest != null) {
            for (int i = 0; i < authRequest.length; i++) {
                if ("passivests".equalsIgnoreCase(authRequest[i].getType())) {
                    passivests = authRequest[i].getClientID();
                    break;
                }
            }
        }

        return passivests;
    }

    /**
     * 
     * @return
     */
    public String[] getClaimUris() {
        return claimUris;
    }

    public void setClaimUris(String[] claimUris) {
        this.claimUris = claimUris;
    }

    /**
     * 
     * @param request
     */
    public void updateOutBoundAuthenticationConfig(HttpServletRequest request) {

        String[] authSteps = request.getParameterValues("auth_step");

        if (authSteps != null && authSteps.length > 0) {
            List<AuthenticationStep> authStepList = new ArrayList<AuthenticationStep>();

            for (String authstep : authSteps) {
                AuthenticationStep authStep = new AuthenticationStep();
                authStep.setStepOrder(Integer.parseInt(authstep));

                String[] localAuthenticatorNames = request.getParameterValues("step_" + authstep
                        + "_local_auth");

                if (localAuthenticatorNames != null && localAuthenticatorNames.length > 0) {
                    List<LocalAuthenticator> localAuthList = new ArrayList<LocalAuthenticator>();
                    for (String name : localAuthenticatorNames) {
                        if (name != null) {
                            LocalAuthenticator localAuth = new LocalAuthenticator();
                            localAuth.setName(name);
                            localAuthList.add(localAuth);
                        }
                    }

                    if (localAuthList.size() > 0) {
                        authStep.setLocalAuthenticators(localAuthList
                                .toArray(new LocalAuthenticator[localAuthList.size()]));
                    }

                }

                String[] federatedIdpNames = request.getParameterValues("step_" + authstep
                        + "_fed_auth");

                if (federatedIdpNames != null && federatedIdpNames.length > 0) {
                    List<FederatedIdentityProvider> fedIdpList = new ArrayList<FederatedIdentityProvider>();
                    for (String name : federatedIdpNames) {
                        if (name != null) {
                            FederatedIdentityProvider idp = new FederatedIdentityProvider();
                            idp.setIdentityProviderName(name);

                            FederatedAuthenticator authenticator = new FederatedAuthenticator();
                            authenticator.setName(request.getParameter("step_" + authstep + "_idp_"
                                    + name + "_fed_authenticator"));
                            idp.setDefaultAuthenticator(authenticator);
                            idp.setFederatedAuthenticators(new FederatedAuthenticator[] { authenticator });

                            fedIdpList.add(idp);
                        }
                    }

                    if (fedIdpList.size() > 0) {
                        authStep.setFederatedIdentityProviders(fedIdpList
                                .toArray(new FederatedIdentityProvider[fedIdpList.size()]));
                    }
                }

                if ((authStep.getFederatedIdentityProviders() != null && authStep
                        .getFederatedIdentityProviders().length > 0)
                        || (authStep.getLocalAuthenticators() != null && authStep
                                .getLocalAuthenticators().length > 0)) {
                    authStepList.add(authStep);
                }

            }

            if (serviceProvider.getLocalAndOutBoundAuthenticationConfig() == null) {
                serviceProvider
                        .setLocalAndOutBoundAuthenticationConfig(new LocalAndOutboundAuthenticationConfig());
            }

            if (authStepList != null && authStepList.size() > 0) {
                serviceProvider.getLocalAndOutBoundAuthenticationConfig().setAuthenticationSteps(
                        authStepList.toArray(new AuthenticationStep[authStepList.size()]));
            }

        }

    }

    /**
     * 
     * @param request
     */
    public void update(HttpServletRequest request) {

        // update basic info.
        serviceProvider.setApplicationName(request.getParameter("spName"));
        serviceProvider.setDescription(request.getParameter("sp-description"));

        if (serviceProvider.getLocalAndOutBoundAuthenticationConfig() == null) {
            // create fresh one.
            serviceProvider
                    .setLocalAndOutBoundAuthenticationConfig(new LocalAndOutboundAuthenticationConfig());
        }

        // authentication type : default, local, federated or advanced.
        serviceProvider.getLocalAndOutBoundAuthenticationConfig().setAuthenticationType(
                request.getParameter("auth_type"));

        // update inbound provisioning data.
        String provisioningUserStore = request.getParameter("scim-inbound-userstore");
        InboundProvisioningConfiguration inBoundProConfig = new InboundProvisioningConfiguration();
        inBoundProConfig.setProvisioningUserStore(provisioningUserStore);
        serviceProvider.setInboundProvisioningConfiguration(inBoundProConfig);

        // update outbound provisioning data.
        String[] provisioningProviders = request.getParameterValues("provisioning_idp");

        if (provisioningProviders != null && provisioningProviders.length > 0) {

            List<FederatedIdentityProvider> provisioningIdps = new ArrayList<FederatedIdentityProvider>();

            for (String proProvider : provisioningProviders) {
                String connector = request.getParameter("provisioning_con_idp_" + proProvider);
                if (connector != null) {
                    FederatedIdentityProvider proIdp = new FederatedIdentityProvider();
                    proIdp.setIdentityProviderName(proProvider);
                    ProvisioningConnector proCon = new ProvisioningConnector();
                    proCon.setName(connector);
                    proIdp.setDefaultProvisioinongConnector(proCon);
                    provisioningIdps.add(proIdp);
                }
            }

            if (provisioningIdps.size() > 0) {
                OutboundProvisioningConfiguration outboundProConfig = new OutboundProvisioningConfiguration();
                outboundProConfig.setProvisioningIdentityProviders(provisioningIdps
                        .toArray(new FederatedIdentityProvider[provisioningIdps.size()]));
                serviceProvider.setOutboundProvisioningConfiguration(outboundProConfig);
            }
        }

        // get all request-path authenticators.
        String[] requestPathAuthenticators = request.getParameterValues("req_path_auth");

        if (requestPathAuthenticators != null && requestPathAuthenticators.length > 0) {
            List<RequestPathAuthenticator> reqAuthList = new ArrayList<RequestPathAuthenticator>();
            for (String name : requestPathAuthenticators) {
                if (name != null) {
                    RequestPathAuthenticator reqAuth = new RequestPathAuthenticator();
                    reqAuth.setName(name);
                    reqAuthList.add(reqAuth);
                }
            }

            if (reqAuthList.size() > 0) {
                serviceProvider.setRequestPathAuthenticators(reqAuthList
                        .toArray(new RequestPathAuthenticator[reqAuthList.size()]));
            }
        }

        List<InboundAuthenticationRequest> authRequestList = new ArrayList<InboundAuthenticationRequest>();

        // update in-bound authentication configuration.

        if (samlIssuer != null) {
            InboundAuthenticationRequest samlAuthenticationRequest = new InboundAuthenticationRequest();
            samlAuthenticationRequest.setClientID(samlIssuer);
            samlAuthenticationRequest.setType("samlsso");
            authRequestList.add(samlAuthenticationRequest);
        }

        if (oauthAppName != null) {
            InboundAuthenticationRequest opicAuthenticationRequest = new InboundAuthenticationRequest();
            opicAuthenticationRequest.setClientID(oauthAppName);
            opicAuthenticationRequest.setType("oauth2");
            authRequestList.add(opicAuthenticationRequest);
        }

        if (wstrustEp != null) {
            InboundAuthenticationRequest opicAuthenticationRequest = new InboundAuthenticationRequest();
            opicAuthenticationRequest.setClientID(wstrustEp);
            opicAuthenticationRequest.setType("wstrust");
            authRequestList.add(opicAuthenticationRequest);
        }

        String passiveSTSRealm = request.getParameter("passiveSTSRealm");

        if (passiveSTSRealm != null) {
            InboundAuthenticationRequest opicAuthenticationRequest = new InboundAuthenticationRequest();
            opicAuthenticationRequest.setClientID(passiveSTSRealm);
            opicAuthenticationRequest.setType("passivests");
            authRequestList.add(opicAuthenticationRequest);
        }

        String openidRealm = request.getParameter("openidRealm");

        if (openidRealm != null) {
            InboundAuthenticationRequest opicAuthenticationRequest = new InboundAuthenticationRequest();
            opicAuthenticationRequest.setClientID(openidRealm);
            opicAuthenticationRequest.setType("openid");
            authRequestList.add(opicAuthenticationRequest);
        }

        if (serviceProvider.getInboundAuthenticationConfig() == null) {
            serviceProvider.setInboundAuthenticationConfig(new InboundAuthenticationConfig());
        }

        if (authRequestList.size() > 0) {
            serviceProvider.getInboundAuthenticationConfig()
                    .setInboundAuthenticationRequests(
                            authRequestList
                                    .toArray(new InboundAuthenticationRequest[authRequestList
                                            .size()]));
        }

        // update local and out-bound authentication.
        if (AUTH_TYPE_DEFAULT.equalsIgnoreCase(serviceProvider
                .getLocalAndOutBoundAuthenticationConfig().getAuthenticationType())) {
            serviceProvider.getLocalAndOutBoundAuthenticationConfig().setAuthenticationSteps(null);
        } else if (AUTH_TYPE_LOCAL.equalsIgnoreCase(serviceProvider
                .getLocalAndOutBoundAuthenticationConfig().getAuthenticationType())) {
            AuthenticationStep authStep = new AuthenticationStep();
            LocalAuthenticator localAuthenticator = new LocalAuthenticator();
            localAuthenticator.setName(request.getParameter("local_authenticator"));
            authStep.setLocalAuthenticators(new LocalAuthenticator[] { localAuthenticator });
            serviceProvider.getLocalAndOutBoundAuthenticationConfig().setAuthenticationSteps(
                    new AuthenticationStep[] { authStep });
        } else if (AUTH_TYPE_FEDERATED.equalsIgnoreCase(serviceProvider
                .getLocalAndOutBoundAuthenticationConfig().getAuthenticationType())) {
            AuthenticationStep authStep = new AuthenticationStep();
            FederatedIdentityProvider idp = new FederatedIdentityProvider();
            idp.setIdentityProviderName(request.getParameter("fed_idp"));
            authStep.setFederatedIdentityProviders(new FederatedIdentityProvider[] { idp });
            serviceProvider.getLocalAndOutBoundAuthenticationConfig().setAuthenticationSteps(
                    new AuthenticationStep[] { authStep });
        } else if (AUTH_TYPE_FLOW.equalsIgnoreCase(serviceProvider
                .getLocalAndOutBoundAuthenticationConfig().getAuthenticationType())) {
            // already updated.
        }

        // update application permissions.
        PermissionsAndRoleConfiguration permAndRoleConfig = new PermissionsAndRoleConfiguration();
        String[] permissions = request.getParameterValues("app_permission");
        List<ApplicationPermission> appPermList = new ArrayList<ApplicationPermission>();

        if (permissions != null && permissions.length > 0) {
            for (String permission : permissions) {
                if (permission != null) {
                    ApplicationPermission appPermission = new ApplicationPermission();
                    appPermission.setValue(permission);
                    appPermList.add(appPermission);
                }
            }
        }

        if (appPermList.size() > 0) {
            permAndRoleConfig.setPermissions(appPermList
                    .toArray(new ApplicationPermission[appPermList.size()]));
        }

        // update role mapping.
        int roleMappingCount = Integer.parseInt(request.getParameter("number_of_rolemappings"));
        List<RoleMapping> roleMappingList = new ArrayList<RoleMapping>();

        for (int i = 0; i < roleMappingCount; i++) {
            RoleMapping mapping = new RoleMapping();
            LocalRole localRole = new LocalRole();
            localRole.setLocalRoleName(request.getParameter("idpRole_" + i));
            mapping.setLocalRole(localRole);
            mapping.setRemoteRole(request.getParameter("spRole_" + i));
            if (mapping.getLocalRole() != null && mapping.getRemoteRole() != null) {
                roleMappingList.add(mapping);
            }
        }

        permAndRoleConfig.setRoleMappings(roleMappingList.toArray(new RoleMapping[roleMappingList
                .size()]));
        serviceProvider.setPermissionAndRoleConfiguration(permAndRoleConfig);

        // update claim configuration.
        int claimCount = Integer.parseInt(request.getParameter("number_of_claimmappings"));
        List<ClaimMapping> claimMappingList = new ArrayList<ClaimMapping>();

        for (int i = 0; i < claimCount; i++) {
            ClaimMapping mapping = new ClaimMapping();

            Claim localClaim = new Claim();
            localClaim.setClaimUri(request.getParameter("idpClaim_" + i));

            Claim spClaim = new Claim();
            spClaim.setClaimUri(request.getParameter("spClaim_" + i));

            String requested = request.getParameter("spClaim_req_" + i);
            if (requested != null && "on".equals(requested)) {
                spClaim.setRequested(true);
            } else {
                spClaim.setRequested(false);
            }

            mapping.setLocalClaim(localClaim);
            mapping.setSpClaim(spClaim);

            if (mapping.getLocalClaim().getClaimUri() != null
                    && mapping.getSpClaim().getClaimUri() != null) {
                claimMappingList.add(mapping);
            }
        }

        if (serviceProvider.getClaimConfiguration() == null) {
            serviceProvider.setClaimConfiguration(new ClaimConfiguration());
        }

        serviceProvider.getClaimConfiguration().setClaimMappings(
                claimMappingList.toArray(new ClaimMapping[claimMappingList.size()]));

        serviceProvider.getClaimConfiguration().setRoleClaimURI(request.getParameter("roleClaim"));

    }

    /**
     * 
     * @return
     */
    public Map<String, String> getRequestedClaims() {
        return requestedClaims;
    }

    /**
     * 
     * @param wstrustEp
     */
    public void setWstrustEp(String wstrustEp) {
        this.wstrustEp = wstrustEp;
    }

    /**
     * 
     * @param passivests
     */
    public void setPassivests(String passivests) {
        this.passivests = passivests;
    }

    /**
     * 
     * @param openid
     */
    public void setOpenid(String openid) {
        this.openid = openid;
    }

    /**
     * 
     * @param request
     */
    public void updateLocalSp(HttpServletRequest request) {

        // update basic info.
        serviceProvider.setApplicationName(request.getParameter("spName"));
        serviceProvider.setDescription(request.getParameter("sp-description"));

        String provisioningUserStore = request.getParameter("scim-inbound-userstore");
        InboundProvisioningConfiguration inBoundProConfig = new InboundProvisioningConfiguration();
        inBoundProConfig.setProvisioningUserStore(provisioningUserStore);
        serviceProvider.setInboundProvisioningConfiguration(inBoundProConfig);

        String[] provisioningProviders = request.getParameterValues("provisioning_idp");
        List<FederatedIdentityProvider> provisioningIdps = new ArrayList<FederatedIdentityProvider>();

        if (serviceProvider.getOutboundProvisioningConfiguration() == null) {
            serviceProvider
                    .setOutboundProvisioningConfiguration(new OutboundProvisioningConfiguration());
        }

        if (provisioningProviders != null && provisioningProviders.length > 0) {
            for (String proProvider : provisioningProviders) {
                String connector = request.getParameter("provisioning_con_idp_" + proProvider);
                if (connector != null) {
                    FederatedIdentityProvider proIdp = new FederatedIdentityProvider();
                    proIdp.setIdentityProviderName(proProvider);
                    ProvisioningConnector proCon = new ProvisioningConnector();
                    proCon.setName(connector);
                    proIdp.setDefaultProvisioinongConnector(proCon);
                    provisioningIdps.add(proIdp);
                }
            }

            if (provisioningIdps.size() > 0) {
                OutboundProvisioningConfiguration outboundProConfig = new OutboundProvisioningConfiguration();
                outboundProConfig.setProvisioningIdentityProviders(provisioningIdps
                        .toArray(new FederatedIdentityProvider[provisioningIdps.size()]));
                serviceProvider.setOutboundProvisioningConfiguration(outboundProConfig);
            }
        }

    }
}
