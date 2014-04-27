/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework.handler.claims.impl;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.claim.mgt.ClaimManagerHandler;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ApplicationConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.claims.ClaimHandler;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;

public class DefaultClaimHandler implements ClaimHandler {
	
	private static Log log = LogFactory.getLog(DefaultClaimHandler.class);
	private static volatile DefaultClaimHandler instance;
	
	public static DefaultClaimHandler getInstance() {
		if (instance == null) {
			synchronized (DefaultClaimHandler.class) {
				if (instance == null) {
					instance = new DefaultClaimHandler();
				}
			}
		}
		return instance;
	}

	public Map<String, String> handle(AuthenticationContext context, ExternalIdPConfig externalIdPConfig, Map<String, String> extAttributes)
							throws FrameworkException {
		
		if (extAttributes == null || extAttributes.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("No attributes given. Returning");
			}
			return null;
		}
		
		ApplicationConfig appConfig = context.getSequenceConfig().getApplicationConfig();

		String requestingClientType = context.getRequestType();

        String idPDialect = null;
        Map<Integer,StepConfig> stepConfigs = context.getSequenceConfig().getStepMap();
        for(Map.Entry<Integer,StepConfig> entry : stepConfigs.entrySet()){
            StepConfig stepConfig = entry.getValue();
            List<AuthenticatorConfig> authnConfigList = stepConfig.getAuthenticatorList();
            for(AuthenticatorConfig authnConfig : authnConfigList){
                ApplicationAuthenticator appAuthn = authnConfig.getApplicationAuthenticator();
                if(appAuthn instanceof FederatedApplicationAuthenticator){
                    idPDialect = appAuthn.getClaimDialectURIIfStandard();
                }
            }
        }

        Set<String> standardDialects = null;
        try {
            standardDialects = ClaimManagerHandler.getInstance().getAllClaimDialects();
        } catch (Exception e) {
            throw new FrameworkException("Error occurred while retrieving all claim dialects");
        }

        // Do NOT do any conversion if the external IDP and Application speak the same language
        String spDialect = getDialectUri(requestingClientType);
        if(idPDialect != null && standardDialects.contains(idPDialect) && spDialect != null && spDialect != null &&
                standardDialects.contains(spDialect) && spDialect.equals(idPDialect)){
            log.debug("Federated IDP and SP are using common dialect. NO claim mapping required. Continuing!");
            return extAttributes;
        }

		// ---------- Starting IDP to IDP Mapping ---------------------//
		
		// first role mapping from External IDP to local IDP
		mapRolesOfExtIdpToLocalIdp(extAttributes, externalIdPConfig);
		
		// Claim Mapping from External IDP to local IDP

		Map<String, String> mappedWso2Claims = getMappedWSO2Claims(context, extAttributes, idPDialect, standardDialects);


		// add the metadata as claims 
		mappedWso2Claims.put(FrameworkConstants.CLAIM_URI_WSO2_EXT_IDP, context.getExternalIdP().getIdPName());
		
		// ----------- END IDP to IDP Mapping ----------------------- //
		
		// ---------- Starting IDP to App Mappings -------------------//
		
		// first do the claims mapping
		Map<String, String> appClaimMappings = appConfig.getClaimMappings();
		Map<String, String> mappedAppClaims = getMappedAppClaims(mappedWso2Claims, appClaimMappings, requestingClientType);
		
		// then we do the IDP to App Role mapping
		mapRolesOfLocalIdpToApp(mappedAppClaims, appConfig);
		
		// ----------- END IDP to IDP Mapping ----------------------- //
		
		return mappedAppClaims;
	}
	
	@Override
	public Map<String, String> handle(AuthenticationContext context) throws FrameworkException {
		
		try {
			
			ClaimManager claimManager = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
					.getClaimManager();
			ClaimMapping[] mappings = claimManager.getAllClaimMappings("http://wso2.org/claims");
			
			List<String> allClaims = new ArrayList<String>();
			for (ClaimMapping map : mappings) {
				allClaims.add(map.getClaim().getClaimUri());
			}

			UserStoreManager userstore = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
					.getUserStoreManager();
			
			Map<String, String> wso2Claims = userstore.getUserClaimValues(context.getSubject(),
					allClaims.toArray(new String[allClaims.size()]), null);
			
			// first do the claims mapping
			ApplicationConfig appConfig = context.getSequenceConfig().getApplicationConfig();
			Map<String, String> appClaimDialect = new HashMap<String,String> ();
			if(appConfig != null) {
				appClaimDialect = appConfig.getClaimMappings();
			}
			Map<String, String> mappedAppClaims = getMappedAppClaims(wso2Claims, appClaimDialect, context.getRequestType());
			if(mappedAppClaims == null || mappedAppClaims.size() == 0){
                mappedAppClaims = wso2Claims;
            }

			// then we do the IDP to App Role mapping
			if(appConfig != null) {
				mapRolesOfLocalIdpToApp(mappedAppClaims, appConfig);
			}
			
			return mappedAppClaims;

		} catch (UserStoreException e) {
			throw new FrameworkException(e.getMessage(), e);
		}
	}

    /**
     * Returns the claims in the WSO2 Dialect
     *
     * @param extClaims
     * @param idPDialect
     * @param standardDialects
     * @return
     */
    private Map<String, String> getMappedWSO2Claims(AuthenticationContext context, Map<String, String> extClaims,
                                                    String idPDialect, Set<String> standardDialects)
            throws FrameworkException {

        Set<org.wso2.carbon.claim.mgt.ClaimMapping> standardToCarbonMappings = null;
        if(idPDialect != null && standardDialects.contains(idPDialect)){
            try {
                standardToCarbonMappings = ClaimManagerHandler.getInstance()
                        .getMappingsFromOtherDialectToCarbon(idPDialect, extClaims.keySet());
            } catch (Exception e) {
                throw new FrameworkException("Error occurred while mapping claims from other dialect to Carbon dialect");
            }
        }
        Set<org.wso2.carbon.identity.application.common.model.ClaimMapping> idPToCarbonClaimMappings = null;
        if(standardToCarbonMappings == null){
            idPToCarbonClaimMappings = new HashSet<org.wso2.carbon.identity.application.common.model.ClaimMapping>(
                    Arrays.asList(context.getExternalIdP().getClaimMappings()));
        }

        Map<String, String> mappedWso2Claims = null;
        Map<String,String> claimMappings = null;
        if(standardToCarbonMappings != null){
            claimMappings = new HashMap<String, String>();
            for(org.wso2.carbon.claim.mgt.ClaimMapping claimMapping : standardToCarbonMappings){
                claimMappings.put(claimMapping.getNonCarbonClaimURI(), claimMapping.getCarbonClaimURI());
            }
        } else if(idPToCarbonClaimMappings != null){
            claimMappings = new HashMap<String, String>();
            for(org.wso2.carbon.identity.application.common.model.ClaimMapping claimMapping : idPToCarbonClaimMappings){
                claimMappings.put(claimMapping.getIdpClaim().getClaimUri(), claimMapping.getLocalClaim().getClaimUri());
            }
        }

        Map<String, String> wso2Claims = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : extClaims.entrySet()) {
            String extClaimUri = entry.getKey();
            String claimValue = entry.getValue();
            if(claimMappings.containsKey(extClaimUri)){
                wso2Claims.put(claimMappings.get(extClaimUri), claimValue);
            }
            if(log.isDebugEnabled()) {
                log.debug("Claim Mapping: ExtIDPClaimUri-" + extClaimUri +
                        " WSO2ClaimUri-" + claimMappings.get(extClaimUri));
            }
        }
        return wso2Claims;
    }

    /**
     * Returns received claims in the application's dialect.
     *
     * @param mappedWso2Claims
     * @param appClaimDialect
     * @param requestingClientType
     * @return
     */
    private Map<String, String> getMappedAppClaims(Map<String, String> mappedWso2Claims, Map<String, String> appClaimDialect,
                                                   String requestingClientType) throws FrameworkException {

        Map<String, String> appClaims = new HashMap<String, String>();

        String requestingDialect = getDialectUri(requestingClientType);
        Set<String> standardDialects = null;
        if(requestingDialect != null){
            try {
                standardDialects = ClaimManagerHandler.getInstance().getAllClaimDialects();
            } catch (Exception e) {
                throw new FrameworkException("Error occurred while retrieving standard claim dialects");
            }
        }
        if(requestingClientType != null && standardDialects.contains(requestingDialect)){ // the application is using a standard dialect

            Set<org.wso2.carbon.claim.mgt.ClaimMapping> claimMappings = null;
            try {
                claimMappings = ClaimManagerHandler.getInstance().getMappingsFromCarbonDialectToOther(
                        requestingDialect, mappedWso2Claims.keySet());
            } catch (Exception e) {
                throw new FrameworkException("Error occurred while mapping carbon claim URIs to non carbon claim URIs");
            }
            Map<String,String> claimMappingsMap = new HashMap<String, String>();
            for(org.wso2.carbon.claim.mgt.ClaimMapping claimMapping:claimMappings){
                claimMappingsMap.put(claimMapping.getCarbonClaimURI(), claimMapping.getNonCarbonClaimURI());
            }
            for(Map.Entry<String, String> claim : mappedWso2Claims.entrySet()) {
                appClaims.put(claimMappingsMap.get(claim.getKey()), claim.getValue());
            }
        } else { // the application has its own custom dialect
            if(appClaimDialect != null){
                for(Map.Entry<String, String> claim : mappedWso2Claims.entrySet()) {
                    String appClaimUri = appClaimDialect.get(claim.getKey());
                    appClaims.put(appClaimUri, claim.getValue());
                }
            }

        }
        return appClaims;
    }

    /**
     * Returns the claim dialect URI based on the client type
     * @param clientType
     * @return
     */
    private String getDialectUri(String clientType) {

        if(FrameworkConstants.RequestType.CLAIM_TYPE_OIDC.equals(clientType)) {
            return "http://wso2.org/claims";
        } else if(FrameworkConstants.RequestType.CLAIM_TYPE_STS.equals(clientType)) {
            return "http://schemas.xmlsoap.org/ws/2005/05/identity";
        } else if(FrameworkConstants.RequestType.CLAIM_TYPE_OPENID.equals(clientType)) {
            return "http://axschema.org";
        } else if(FrameworkConstants.RequestType.CLAIM_TYPE_SCIM.equals(clientType)) {
            return "urn:scim:schemas:core:1.0";
        } else if(FrameworkConstants.RequestType.CLAIM_TYPE_WSO2.equals(clientType)){
            return "http://wso2.org/claims";
        } else if(FrameworkConstants.RequestType.CLAIM_TYPE_SAML_SSO.equals(clientType)){
            return "http://wso2.org/claims";
        }
        return null;
    }

    /**
     * Mapped the Local IDP WSO2 Claims to the Applications claim dialect.
     *
     * @param claims
     * @param appConfig
     */
    private void mapRolesOfLocalIdpToApp(Map<String, String> claims, ApplicationConfig appConfig) {

        String roleClaim = appConfig.getRoleClaim();
        Map<String, String> roleMapping = appConfig.getRoleMappings();

        if(roleClaim != null && roleMapping != null) {
            String roles = claims.get(roleClaim);
            List<String> appRoles = new ArrayList<String>();

            if(roles != null) {
                String[] roleArray = roles.split(",");
                for(String idpRole : roleArray) {
                    appRoles.add(roleMapping.get(idpRole));

                    if(log.isDebugEnabled()) {
                        log.debug("Local IDP to Application Claim mapping: " + idpRole + " to " + roleMapping.get(idpRole));
                    }
                }
            }

            StringBuffer appRoleClaimValue = null;
            for(String role : appRoles) {
                if(appRoleClaimValue == null) {
                    appRoleClaimValue = new StringBuffer(role);
                } else {
                    appRoleClaimValue.append("," + appRoleClaimValue);
                }
            }

            // *** Important *** setting back the value
            if(appRoleClaimValue != null) {
                claims.put(roleClaim, appRoleClaimValue.toString());
            }
        }

    }

    /**
     * Maps the external IDP roles to local IDPs roles. The external IDP sends
     * its roles as a claim with comma separated values. Here we read the Role
     * mappings and find out the mapped local Role and re-construct the value of
     * the role claim.
     *
     * @param extAttributes
     * @param externalIdPConfig
     */
    private void mapRolesOfExtIdpToLocalIdp(Map<String, String> extAttributes, ExternalIdPConfig externalIdPConfig) {

        String roleClaimUri = externalIdPConfig.getRoleClaimUri();
        RoleMapping[] roleMappings = externalIdPConfig.getRoleMappings();

        if (roleClaimUri != null && roleMappings != null) {
            List<String> internalRoles = new ArrayList<String>();

            // the values comes as a comma separated list
            String externalRoles = extAttributes.get(roleClaimUri);
            if (externalRoles != null) {
                String[] externalRole = externalRoles.split(",");

                for (String role : externalRole) {
                    for (RoleMapping map : roleMappings) {
                        if (role.equals(map.getRemoteRole())) { // if role has a mapping
                            internalRoles.add(map.getLocalRole().getLocalRoleName());

                            if (log.isDebugEnabled()) {
                                log.debug("External IDP to Local IDP role mapping: " + role
                                        + " to " + map.getLocalRole().getLocalRoleName());
                            }
                        }
                    }
                }
            }
            // constructing the converted roles string
            StringBuffer finalInternalRoles = null;
            for (String internalRole : internalRoles) {
                if (finalInternalRoles == null) {
                    finalInternalRoles = new StringBuffer(internalRole);
                } else {
                    finalInternalRoles.append("," + internalRole);
                }
            }

            // *important* Setting back the mapped claims
            if(finalInternalRoles != null) {
                extAttributes.put(roleClaimUri, finalInternalRoles.toString());
            }
        }
    }
}
