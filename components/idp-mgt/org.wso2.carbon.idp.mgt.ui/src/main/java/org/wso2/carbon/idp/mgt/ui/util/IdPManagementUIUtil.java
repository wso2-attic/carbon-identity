/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.idp.mgt.ui.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.idp.xsd.Claim;
import org.wso2.carbon.identity.application.common.model.idp.xsd.ClaimConfiguration;
import org.wso2.carbon.identity.application.common.model.idp.xsd.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.idp.xsd.FacebookFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.idp.xsd.FederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.idp.xsd.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.idp.xsd.JustInTimeProvisioningConfiguration;
import org.wso2.carbon.identity.application.common.model.idp.xsd.LocalRole;
import org.wso2.carbon.identity.application.common.model.idp.xsd.OpenIDConnectFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.idp.xsd.OpenIDFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.idp.xsd.PassiveSTSFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.idp.xsd.PermissionsAndRoleConfiguration;
import org.wso2.carbon.identity.application.common.model.idp.xsd.ProvisioningConnector;
import org.wso2.carbon.identity.application.common.model.idp.xsd.ProvisioningProperty;
import org.wso2.carbon.identity.application.common.model.idp.xsd.RoleMapping;
import org.wso2.carbon.identity.application.common.model.idp.xsd.SAMLFederatedAuthenticator;
import org.wso2.carbon.ui.CarbonUIUtil;

public class IdPManagementUIUtil {

    private static Log log = LogFactory.getLog(IdPManagementUIUtil.class);

    /**
     * Validates an URI.
     * 
     * @param uriString URI String
     * @return <code>true</code> if valid URI, <code>false</code> otherwise
     */
    public static boolean validateURI(String uriString) {

        if (uriString != null) {
            try {
                new URL(uriString);
            } catch (MalformedURLException e) {
                log.debug(e.getMessage(), e);
                return false;
            }
        } else {
            String errorMsg = "Invalid URL: \'NULL\'";
            log.debug(errorMsg);
            return false;
        }
        return true;
    }

    public static String getOpenIDUrl(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/openid");
    }

    public static String getSAML2SSOUrl(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/samlsso");
    }

    public static String getOAuth2AuthzEPURL(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/oauth2/authorize");
    }

    public static String getOAuth2TokenEPURL(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/oauth2/token");
    }

    public static String getPassiveSTSURL(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/services/wso2carbon-sts");
    }

    /**
     * 
     * @param request
     * @return
     * @throws Exception
     */
    public static FederatedIdentityProvider buildeFederatedIdentityProvider(
            HttpServletRequest request) throws Exception {

        FederatedIdentityProvider fedIdp = new FederatedIdentityProvider();

        if (ServletFileUpload.isMultipartContent(request)) {
            ServletRequestContext servletContext = new ServletRequestContext(request);
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            List items = upload.parseRequest(servletContext);
            Map<String, String> paramMap = new HashMap<String, String>();
            List<String> idpClaims = new ArrayList<String>();
            List<String> idpRoles = new ArrayList<String>();

            FederatedIdentityProvider oldIdentityProvider = (FederatedIdentityProvider) request
                    .getSession().getAttribute("identityProvider");

            if (ServletFileUpload.isMultipartContent(request)) {

                for (Object item : items) {
                    DiskFileItem diskFileItem = (DiskFileItem) item;
                    if (diskFileItem != null) {
                        byte[] value = diskFileItem.get();
                        String key = diskFileItem.getFieldName();
                        if ("certFile".equals(key)) {
                            paramMap.put(key, Base64.encode(value));
                        } else if ("google_prov_private_key".equals(key)) {
                            paramMap.put(key, Base64.encode(value));
                        } else if (key.startsWith("claimrowname_")) {
                            idpClaims.add(new String(value));
                        } else if (key.startsWith("rolerowname_")) {
                            idpRoles.add(new String(value));
                        } else {
                            paramMap.put(key, new String(value));
                        }

                        String updatedValue = paramMap.get(key);

                        if (updatedValue != null && updatedValue.trim().length() == 0) {
                            paramMap.put(key, null);
                        }
                    }
                }
            }

            if (oldIdentityProvider != null && oldIdentityProvider.getCertificate() != null) {
                paramMap.put("oldCertFile", oldIdentityProvider.getCertificate());
            }

            // build identity provider basic information.
            buildBasicInformation(fedIdp, paramMap);

            // build out-bound authentication configuration.
            buildOutboundAuthenticationConfiguration(fedIdp, paramMap);

            // build claim configuration.
            if (oldIdentityProvider != null
                    && oldIdentityProvider.getClaimConfiguration().getClaimMappings() != null) {
                buildClaimConfiguration(fedIdp, paramMap, idpClaims, oldIdentityProvider
                        .getClaimConfiguration().getClaimMappings());
            } else {
                buildClaimConfiguration(fedIdp, paramMap, idpClaims, null);
            }

            // build role configuration.
            if (oldIdentityProvider != null
                    && oldIdentityProvider.getPermissionAndRoleConfiguration() != null
                    && oldIdentityProvider.getPermissionAndRoleConfiguration().getRoleMappings() != null) {
                buildRoleConfiguration(fedIdp, paramMap, idpRoles, oldIdentityProvider
                        .getPermissionAndRoleConfiguration().getRoleMappings());
            } else {
                buildRoleConfiguration(fedIdp, paramMap, idpRoles, null);
            }

            // build in-bound provisioning configuration.
            buildInboundProvisioningConfiguration(fedIdp, paramMap);

            // build out-bound provisioning configuration.
            buildOutboundProvisioningConfiguration(fedIdp, paramMap);

        } else {
            throw new Exception("Invalid Content Type: Not multipart/form-data");
        }

        return fedIdp;
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildOutboundProvisioningConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {

        // build SPML provisioning configuration.
        buildSPMLProvisioningConfiguration(fedIdp, paramMap);

        // build Google provisioning configuration.
        buildGoogleProvisioningConfiguration(fedIdp, paramMap);

        // build SCIM provisioning configuration.
        buildSCIMProvisioningConfiguration(fedIdp, paramMap);

        // build Salesforce provisioning configuration.
        buildSalesforceProvisioningConfiguration(fedIdp, paramMap);
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildSPMLProvisioningConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {

        ProvisioningConnector proConnector = new ProvisioningConnector();
        proConnector.setName("spml");

        ProvisioningProperty userNameProp = null;
        ProvisioningProperty passwordProp = null;
        ProvisioningProperty endPointProp = null;

        if (paramMap.get("spml-username") != null) {
            userNameProp = new ProvisioningProperty();
            userNameProp.setName("spml-username");
            userNameProp.setValue(paramMap.get("spml-username"));
        }

        if (paramMap.get("spml-password") != null) {
            passwordProp = new ProvisioningProperty();
            passwordProp.setConfidential(true);
            passwordProp.setName("spml-password");
            passwordProp.setValue(paramMap.get("spml-password"));
        }

        if (paramMap.get("spml-ep") != null) {
            endPointProp = new ProvisioningProperty();
            endPointProp.setName("spml-ep");
            endPointProp.setValue(paramMap.get("spml-ep"));
        }

        ProvisioningProperty[] proProperties = new ProvisioningProperty[] { userNameProp,
                passwordProp, endPointProp };

        proConnector.setProvisioningProperties(proProperties);

        ProvisioningConnector[] proConnectors = fedIdp.getProvisoningConnectors();

        if (proConnector.getName() != null) {
            if (proConnectors == null || proConnectors.length == 0) {
                fedIdp.setProvisoningConnectors((new ProvisioningConnector[] { proConnector }));
            } else {
                fedIdp.setProvisoningConnectors(concatArrays(
                        new ProvisioningConnector[] { proConnector }, proConnectors));
            }
        }

    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildGoogleProvisioningConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {
        ProvisioningConnector proConnector = new ProvisioningConnector();
        proConnector.setName("googleapps");

        ProvisioningProperty domainName = null;
        ProvisioningProperty emailClaim = null;
        ProvisioningProperty givenNameClaim = null;
        ProvisioningProperty givenNameDefaultVal = null;
        ProvisioningProperty familyNameClaim = null;
        ProvisioningProperty familyNameDefault = null;
        ProvisioningProperty serviceAccEmail = null;
        ProvisioningProperty privateKey = null;
        ProvisioningProperty adminEmail = null;
        ProvisioningProperty appName = null;

        if (paramMap.get("googleProvEnabled") != null
                && "on".equals(paramMap.get("googleProvEnabled"))) {
            proConnector.setEnabled(true);
        } else {
            proConnector.setEnabled(false);
        }

        if (paramMap.get("googleProvDefault") != null
                && "on".equals(paramMap.get("googleProvDefault"))) {
            fedIdp.setDefaultProvisioinongConnector(proConnector);
        }

        if (paramMap.get("google_prov_domain_nam") != null) {
            domainName = new ProvisioningProperty();
            domainName.setName("google_prov_domain_nam");
            domainName.setValue(paramMap.get("google_prov_domain_nam"));
        }

        if (paramMap.get("google_prov_email_claim_dropdown") != null) {
            emailClaim = new ProvisioningProperty();
            emailClaim.setName("google_prov_email_claim_dropdown");
            emailClaim.setValue(paramMap.get("google_prov_email_claim_dropdown"));
        }

        if (paramMap.get("google_prov_givenname_claim_dropdown") != null) {
            givenNameClaim = new ProvisioningProperty();
            givenNameClaim.setName("google_prov_givenname_claim_dropdown");
            givenNameClaim.setValue(paramMap.get("google_prov_givenname_claim_dropdown"));
        }

        if (paramMap.get("google_prov_givenname") != null) {
            givenNameDefaultVal = new ProvisioningProperty();
            givenNameDefaultVal.setName("google_prov_givenname");
            givenNameDefaultVal.setValue(paramMap.get("google_prov_givenname"));
        }

        if (paramMap.get("google_prov_familyname_claim_dropdown") != null) {
            familyNameClaim = new ProvisioningProperty();
            familyNameClaim.setName("google_prov_familyname_claim_dropdown");
            familyNameClaim.setValue(paramMap.get("google_prov_familyname_claim_dropdown"));
        }

        if (paramMap.get("google_prov_familyname") != null) {
            familyNameDefault = new ProvisioningProperty();
            familyNameDefault.setName("google_prov_familyname");
            familyNameDefault.setValue(paramMap.get("google_prov_familyname"));
        }

        if (paramMap.get("google_prov_service_acc_email") != null) {
            serviceAccEmail = new ProvisioningProperty();
            serviceAccEmail.setName("google_prov_service_acc_email");
            serviceAccEmail.setValue(paramMap.get("google_prov_service_acc_email"));
        }

        if (paramMap.get("google_prov_private_key") != null) {
            privateKey = new ProvisioningProperty();
            privateKey.setName("google_prov_private_key");
            privateKey.setValue(paramMap.get("google_prov_private_key"));

        }

        if (paramMap.get("google_prov_admin_email") != null) {
            adminEmail = new ProvisioningProperty();
            adminEmail.setName("google_prov_admin_email");
            adminEmail.setValue(paramMap.get("google_prov_admin_email"));
        }

        if (paramMap.get("google_prov_application_name") != null) {
            appName = new ProvisioningProperty();
            appName.setName("google_prov_application_name");
            appName.setValue(paramMap.get("google_prov_application_name"));
        }

        ProvisioningProperty[] proProperties = new ProvisioningProperty[] { appName, adminEmail,
                privateKey, serviceAccEmail, familyNameDefault, familyNameClaim,
                givenNameDefaultVal, givenNameClaim, emailClaim, domainName };

        proConnector.setProvisioningProperties(proProperties);

        ProvisioningConnector[] proConnectors = fedIdp.getProvisoningConnectors();

        if (proConnector.getName() != null) {
            if (proConnectors == null || proConnectors.length == 0) {
                fedIdp.setProvisoningConnectors(new ProvisioningConnector[] { proConnector });
            } else {
                fedIdp.setProvisoningConnectors(concatArrays(
                        new ProvisioningConnector[] { proConnector }, proConnectors));
            }
        }
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildSCIMProvisioningConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {
        ProvisioningConnector proConnector = new ProvisioningConnector();
        proConnector.setName("scim");

        ProvisioningProperty userNameProp = null;
        ProvisioningProperty passwordProp = null;
        ProvisioningProperty userEpProp = null;
        ProvisioningProperty groupEpProp = null;

        if (paramMap.get("scim-username") != null) {
            userNameProp = new ProvisioningProperty();
            userNameProp.setName("scim-username");
            userNameProp.setValue(paramMap.get("scim-username"));
        }

        if (paramMap.get("scim-password") != null) {
            passwordProp = new ProvisioningProperty();
            passwordProp.setConfidential(true);
            passwordProp.setName("scim-password");
            passwordProp.setValue(paramMap.get("scim-password"));
        }

        if (paramMap.get("scim-user-ep") != null) {
            userEpProp = new ProvisioningProperty();
            userEpProp.setName("scim-user-ep");
            userEpProp.setValue(paramMap.get("scim-user-ep"));
        }

        if (paramMap.get("scim-user-ep") != null) {
            groupEpProp = new ProvisioningProperty();
            groupEpProp.setName("scim-user-ep");
            groupEpProp.setValue(paramMap.get("scim-user-ep"));
        }

        ProvisioningProperty[] proProperties = new ProvisioningProperty[] { userNameProp,
                passwordProp, userEpProp, groupEpProp };

        proConnector.setProvisioningProperties(proProperties);

        ProvisioningConnector[] proConnectors = fedIdp.getProvisoningConnectors();

        if (proConnector.getName() != null) {
            if (proConnectors == null || proConnectors.length == 0) {
                fedIdp.setProvisoningConnectors(new ProvisioningConnector[] { proConnector });
            } else {
                fedIdp.setProvisoningConnectors(concatArrays(
                        new ProvisioningConnector[] { proConnector }, proConnectors));
            }
        }

    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildSalesforceProvisioningConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {

        ProvisioningConnector proConnector = new ProvisioningConnector();
        proConnector.setName("salesforce");

        ProvisioningProperty userNameProp = null;
        ProvisioningProperty passwordProp = null;
        ProvisioningProperty clentIdProp = null;
        ProvisioningProperty clientSecretProp = null;
        ProvisioningProperty apiVersionProp = null;
        ProvisioningProperty domainNameProp = null;

        if (paramMap.get("sf-username") != null) {
            userNameProp = new ProvisioningProperty();
            userNameProp.setName("sf-username");
            userNameProp.setValue(paramMap.get("sf-username"));
        }

        if (paramMap.get("sf-password") != null) {
            passwordProp = new ProvisioningProperty();
            passwordProp.setConfidential(true);
            passwordProp.setName("sf-password");
            passwordProp.setValue(paramMap.get("sf-password"));
        }

        if (paramMap.get("sf-clientid") != null) {
            clentIdProp = new ProvisioningProperty();
            clentIdProp.setName("sf-clientid");
            clentIdProp.setValue(paramMap.get("sf-clientid"));
        }

        if (paramMap.get("sf-client-secret") != null) {
            clientSecretProp = new ProvisioningProperty();
            clientSecretProp.setConfidential(true);
            clientSecretProp.setName("sf-client-secret");
            clientSecretProp.setValue(paramMap.get("sf-client-secret"));
        }

        if (paramMap.get("sf-clientid") != null) {
            clentIdProp = new ProvisioningProperty();
            clentIdProp.setName("sf-clientid");
            clentIdProp.setValue(paramMap.get("sf-clientid"));
        }

        if (paramMap.get("sf-api-version") != null) {
            apiVersionProp = new ProvisioningProperty();
            apiVersionProp.setName("sf-api-version");
            apiVersionProp.setValue(paramMap.get("sf-api-version"));
        }

        if (paramMap.get("sf-domain-name") != null) {
            domainNameProp = new ProvisioningProperty();
            domainNameProp.setName("sf-domain-name");
            domainNameProp.setValue(paramMap.get("sf-domain-name"));
        }

        ProvisioningProperty[] proProperties = new ProvisioningProperty[] { userNameProp,
                passwordProp, clentIdProp, clientSecretProp, apiVersionProp, domainNameProp };

        proConnector.setProvisioningProperties(proProperties);

        ProvisioningConnector[] proConnectors = fedIdp.getProvisoningConnectors();

        if (proConnector.getName() != null) {
            if (proConnectors == null || proConnectors.length == 0) {
                fedIdp.setProvisoningConnectors(new ProvisioningConnector[] { proConnector });
            } else {
                fedIdp.setProvisoningConnectors(concatArrays(
                        new ProvisioningConnector[] { proConnector }, proConnectors));
            }
        }
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildClaimConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap, List<String> idpClaims, ClaimMapping[] currentClaimMapping)
            throws IdentityApplicationManagementException {

        ClaimConfiguration claimConfiguration = new ClaimConfiguration();

        if (idpClaims != null && idpClaims.size() > 0) {
            List<Claim> idPClaimList = new ArrayList<Claim>();
            for (Iterator<String> iterator = idpClaims.iterator(); iterator.hasNext();) {
                String claimUri = iterator.next();
                Claim idpClaim = new Claim();
                idpClaim.setClaimUri(claimUri);
                idPClaimList.add(idpClaim);
            }
            claimConfiguration.setIdpClaims(idPClaimList.toArray(new Claim[idPClaimList.size()]));
        }

        claimConfiguration.setUserClaimURI(paramMap.get("user_id_claim_dropdown"));
        claimConfiguration.setRoleClaimURI(paramMap.get("role_claim_dropdown"));

        String claimMappingFromFile = paramMap.get("claimMappingFile");

        if (claimMappingFromFile != null) {
            String[] claimMappings;
            claimMappings = claimMappingFromFile.replaceAll("\\s", "").split(",");

            if (claimMappings != null && claimMappings.length > 0) {
                Set<ClaimMapping> claimMappingList = new HashSet<ClaimMapping>();
                for (int i = 0; i < claimMappings.length; i++) {
                    String claimMappingString = claimMappings[i];
                    if (claimMappingString != null) {
                        String[] splitClaimMapping = claimMappingString.split("-");
                        if (splitClaimMapping != null && splitClaimMapping.length == 2) {
                            String idPClaimURI = splitClaimMapping[0];
                            String localClaimURI = splitClaimMapping[1];

                            ClaimMapping mapping = new ClaimMapping();

                            Claim providerClaim = new Claim();
                            providerClaim.setClaimUri(idPClaimURI);

                            Claim localClaim = new Claim();
                            localClaim.setClaimUri(localClaimURI);

                            mapping.setIdpClaim(providerClaim);
                            mapping.setLocalClaim(localClaim);
                            claimMappingList.add(mapping);
                        }
                    }
                }

                claimConfiguration.setClaimMappings(claimMappingList
                        .toArray(new ClaimMapping[claimMappingList.size()]));
            }
        }

        // String deleteClaimMapping = paramMap.get("deleteClaimMappings");

        fedIdp.setClaimConfiguration(claimConfiguration);
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     */
    private static void buildBasicInformation(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) {

        String oldCertFile = null;
        String certFile = null;
        String deletePublicCert = null;

        // set identity provider name.
        fedIdp.setIdentityProviderName(paramMap.get("idPName"));

        // if this primary - then - true
        if ("on".equals(paramMap.get("primary"))) {
            fedIdp.setPrimary(true);
        } else {
            fedIdp.setPrimary(false);
        }

        // set the home realm identifier of the identity provider.
        fedIdp.setHomeRealmId(paramMap.get("realmId"));

        // set the token end-point alias - in SAML request for OAuth.
        fedIdp.setAlias(paramMap.get("tokenEndpointAlias"));

        // get the value of the old certificate - if this is an update.
        if (paramMap.get("oldCertFile") != null) {
            oldCertFile = paramMap.get("oldCertFile");
        }

        // get the value of the uploaded certificate.
        if (paramMap.get("certFile") != null) {
            certFile = paramMap.get("certFile");
        }

        // check whether the certificate being deleted.
        if (paramMap.get("deletePublicCert") != null) {
            deletePublicCert = paramMap.get("deletePublicCert");
        }

        // if there is no new certificate and not a delete - use the old one.
        if (oldCertFile != null && certFile == null
                && (deletePublicCert == null || deletePublicCert.equals("false"))) {
            certFile = oldCertFile;
        }

        // set public certificate of the identity provider.
        fedIdp.setCertificate(certFile);
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildOutboundAuthenticationConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {
        // build OpenID authentication configuration.
        buildOpenIDAuthenticationConfiguration(fedIdp, paramMap);

        // build Facebook authentication configuration.
        buildFacebookAuthenticationConfiguration(fedIdp, paramMap);

        // build OpenID Connect authentication configuration.
        buildOpenIDConnectAuthenticationConfiguration(fedIdp, paramMap);

        // build SAML authentication configuration.
        buildSAMLAuthenticationConfiguration(fedIdp, paramMap);

        // build passive STS authentication configuration.
        buildPassiveSTSAuthenticationConfiguration(fedIdp, paramMap);

    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildOpenIDAuthenticationConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {

        OpenIDFederatedAuthenticator openidAuthenticator = new OpenIDFederatedAuthenticator();
        openidAuthenticator.setName("openid");

        if ("on".equals(paramMap.get("openIdEnabled"))) {
            openidAuthenticator.setEnabled(true);
        }

        if ("on".equals(paramMap.get("openIdDefault"))) {
            fedIdp.setDefaultAuthenticator(openidAuthenticator);
            ;
        }

        if ("1".equals(paramMap.get("open_id_user_id_location"))) {
            openidAuthenticator.setUsetIdInClaim(true);
        } else {
            openidAuthenticator.setUsetIdInClaim(false);
        }

        openidAuthenticator.setOpenIDServerUrl(paramMap.get("openIdUrl"));
        openidAuthenticator.setOpenIDRealm(paramMap.get("realmId"));

        FederatedAuthenticator[] authenticators = fedIdp.getFederatedAuthenticators();

        if (openidAuthenticator.getOpenIDServerUrl() != null) {
            // openIdUrl is mandatory for out-bound openid configuration.
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticators(new FederatedAuthenticator[] { openidAuthenticator });
            } else {
                fedIdp.setFederatedAuthenticators(concatArrays(
                        new FederatedAuthenticator[] { openidAuthenticator }, authenticators));
            }
        }
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildFacebookAuthenticationConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {

        FacebookFederatedAuthenticator facebookAuthenticator = new FacebookFederatedAuthenticator();
        facebookAuthenticator.setName("facebook");

        if ("on".equals(paramMap.get("fbAuthEnabled"))) {
            facebookAuthenticator.setEnabled(true);
        }

        if ("on".equals(paramMap.get("fbAuthDefault"))) {
            fedIdp.setDefaultAuthenticator(facebookAuthenticator);
        }

        facebookAuthenticator.setClientId(paramMap.get("fbClientId"));
        facebookAuthenticator.setClientSecret(paramMap.get("fbClientSecret"));

        if ("on".equals(paramMap.get("fbUserIdInClaims"))) {
            facebookAuthenticator.setUsetIdInClaim(true);
        } else {
            facebookAuthenticator.setUsetIdInClaim(false);
        }

        FederatedAuthenticator[] authenticators = fedIdp.getFederatedAuthenticators();

        if (facebookAuthenticator.getClientId() != null
                && facebookAuthenticator.getClientSecret() != null) {
            // facebook authenticator cannot exist without client id and client secret.
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticators(new FederatedAuthenticator[] { facebookAuthenticator });
            } else {
                fedIdp.setFederatedAuthenticators(concatArrays(
                        new FederatedAuthenticator[] { facebookAuthenticator }, authenticators));
            }
        }
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildOpenIDConnectAuthenticationConfiguration(
            FederatedIdentityProvider fedIdp, Map<String, String> paramMap)
            throws IdentityApplicationManagementException {

        OpenIDConnectFederatedAuthenticator oidcAuthenticator = new OpenIDConnectFederatedAuthenticator();
        oidcAuthenticator.setName("openidconnect");

        if ("on".equals(paramMap.get("oidcEnabled"))) {
            oidcAuthenticator.setEnabled(true);
        }

        if ("on".equals(paramMap.get("oidcDefault"))) {
            fedIdp.setDefaultAuthenticator(oidcAuthenticator);
        }

        oidcAuthenticator.setAuthzEndpointUrl(paramMap.get("authzUrl"));
        oidcAuthenticator.setTokenEndpointUrl(paramMap.get("tokenUrl"));
        oidcAuthenticator.setClientId(paramMap.get("clientId"));
        oidcAuthenticator.setClientSecret(paramMap.get("clientSecret"));

        if ("1".equals(paramMap.get("oidc_user_id_location"))) {
            oidcAuthenticator.setUsetIdInClaim(true);

        } else {
            oidcAuthenticator.setUsetIdInClaim(true);
        }

        FederatedAuthenticator[] authenticators = fedIdp.getFederatedAuthenticators();

        if (oidcAuthenticator.getAuthzEndpointUrl() != null
                && oidcAuthenticator.getTokenEndpointUrl() != null
                && oidcAuthenticator.getClientId() != null
                && oidcAuthenticator.getClientSecret() != null) {
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticators(new FederatedAuthenticator[] { oidcAuthenticator });
            } else {
                fedIdp.setFederatedAuthenticators(concatArrays(
                        new FederatedAuthenticator[] { oidcAuthenticator }, authenticators));
            }
        }
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildPassiveSTSAuthenticationConfiguration(
            FederatedIdentityProvider fedIdp, Map<String, String> paramMap)
            throws IdentityApplicationManagementException {

        PassiveSTSFederatedAuthenticator passiveSTS = new PassiveSTSFederatedAuthenticator();
        passiveSTS.setName("passivests");

        if ("on".equals(paramMap.get("passiveSTSEnabled"))) {
            passiveSTS.setEnabled(true);
        }

        if ("on".equals(paramMap.get("passiveSTSDefault"))) {
            fedIdp.setDefaultAuthenticator(passiveSTS);
        }

        passiveSTS.setPassiveSTSRealm(paramMap.get("passiveSTSRealm"));

        passiveSTS.setPassiveSTSUrl(paramMap.get("passiveSTSUrl"));

        if ("1".equals(paramMap.get("passive_sts_user_id_location"))) {
            passiveSTS.setUsetIdInClaim(true);
        } else {
            passiveSTS.setUsetIdInClaim(true);
        }

        FederatedAuthenticator[] authenticators = fedIdp.getFederatedAuthenticators();

        if (passiveSTS.getPassiveSTSRealm() != null && passiveSTS.getPassiveSTSUrl() != null) {
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticators(new FederatedAuthenticator[] { passiveSTS });
            } else {
                fedIdp.setFederatedAuthenticators(concatArrays(
                        new FederatedAuthenticator[] { passiveSTS }, authenticators));
            }
        }

    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildSAMLAuthenticationConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {

        SAMLFederatedAuthenticator samlAuthenticator = new SAMLFederatedAuthenticator();
        samlAuthenticator.setName("samlsso");

        if ("on".equals(paramMap.get("saml2SSOEnabled"))) {
            samlAuthenticator.setEnabled(true);
        }

        if ("on".equals(paramMap.get("saml2SSODefault"))) {
            fedIdp.setDefaultAuthenticator(samlAuthenticator);
        }

        if ("on".equals(paramMap.get("authnRequestSigned"))) {
            samlAuthenticator.setAuthnRequestSigned(true);
        } else {
            samlAuthenticator.setAuthnRequestSigned(false);
        }

        if ("on".equals(paramMap.get("sloEnabled"))) {
            samlAuthenticator.setLogoutEnabled(true);
        } else {
            samlAuthenticator.setLogoutEnabled(false);
        }

        if ("on".equals(paramMap.get("logoutRequestSigned"))) {
            samlAuthenticator.setLogoutRequestSigned(true);
        } else {
            samlAuthenticator.setLogoutRequestSigned(false);
        }

        if ("on".equals(paramMap.get("authnResponseSigned"))) {
            samlAuthenticator.setAuthnResponseSigned(true);
        } else {
            samlAuthenticator.setAuthnResponseSigned(false);
        }

        if ("1".equals(paramMap.get("saml2_sso_user_id_location"))) {
            samlAuthenticator.setUsetIdInClaim(true);
        } else {
            samlAuthenticator.setUsetIdInClaim(false);
        }

        samlAuthenticator.setIdpEntityId(paramMap.get("idPEntityId"));
        samlAuthenticator.setSpEntityId(paramMap.get("spEntityId"));
        samlAuthenticator.setSaml2SSOUrl(paramMap.get("ssoUrl"));
        samlAuthenticator.setLogoutRequestUrl(paramMap.get("logoutUrl"));

        FederatedAuthenticator[] authenticators = fedIdp.getFederatedAuthenticators();

        if (samlAuthenticator.getSaml2SSOUrl() != null && samlAuthenticator.getSpEntityId() != null
                && samlAuthenticator.getIdpEntityId() != null) {
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticators(new FederatedAuthenticator[] { samlAuthenticator });
            } else {
                fedIdp.setFederatedAuthenticators(concatArrays(
                        new FederatedAuthenticator[] { samlAuthenticator }, authenticators));
            }
        }
    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @param idpRoles
     * @param currentRoleMapping
     * @throws IdentityProviderMgtException
     */
    private static void buildRoleConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap, List<String> idpRoles, RoleMapping[] currentRoleMapping)
            throws IdentityApplicationManagementException {

        PermissionsAndRoleConfiguration roleConfiguration = new PermissionsAndRoleConfiguration();

        roleConfiguration.setIdpRoles(idpRoles.toArray(new String[idpRoles.size()]));

        String roleMappingFromFile = paramMap.get("roleMappingFile");

        if (roleMappingFromFile != null) {
            String[] roleMappings;
            roleMappings = roleMappingFromFile.replaceAll("\\s", "").split(",");

            if (roleMappings != null && roleMappings.length > 0) {
                Set<RoleMapping> roleMappingList = new HashSet<RoleMapping>();
                for (int i = 0; i < roleMappings.length; i++) {
                    String roleMappingString = roleMappings[i];
                    String[] splitRoleMapping = roleMappingString.split(":");
                    if (splitRoleMapping != null && splitRoleMapping.length == 2) {
                        String idPRoleName = splitRoleMapping[0];
                        String localRoleString = splitRoleMapping[1];
                        String[] splitLocalRole = localRoleString.split("/");
                        String userStoreId = null;
                        String localRoleName = null;
                        LocalRole localRole = null;
                        if (splitLocalRole != null && splitLocalRole.length == 2) {
                            userStoreId = splitLocalRole[0];
                            localRoleName = splitLocalRole[1];
                            localRole = new LocalRole();
                            localRole.setUserStoreId(userStoreId);
                            localRole.setLocalRoleName(localRoleName);
                        } else {
                            localRoleName = localRoleString;
                            localRole = new LocalRole();
                            localRole.setLocalRoleName(localRoleName);
                        }

                        RoleMapping roleMapping = new RoleMapping();
                        roleMapping.setLocalRole(localRole);
                        roleMapping.setRemoteRole(idPRoleName);

                        roleMappingList.add(roleMapping);
                    }
                }
                roleConfiguration.setRoleMappings(roleMappingList
                        .toArray(new RoleMapping[roleMappingList.size()]));
            }
        }

        String deleteRoleMapping = paramMap.get("deleteRoleMappings");

        if (currentRoleMapping != null && fedIdp.getPermissionAndRoleConfiguration() == null
                && (deleteRoleMapping == null || "false".equals(deleteRoleMapping))) {
            roleConfiguration.setRoleMappings(currentRoleMapping);
        }

        fedIdp.setPermissionAndRoleConfiguration(roleConfiguration);

    }

    /**
     * 
     * @param fedIdp
     * @param paramMap
     * @throws IdentityProviderMgtException
     */
    private static void buildInboundProvisioningConfiguration(FederatedIdentityProvider fedIdp,
            Map<String, String> paramMap) throws IdentityApplicationManagementException {

        String provisioning = paramMap.get("provisioning");
        JustInTimeProvisioningConfiguration jitProvisioningConfiguration = new JustInTimeProvisioningConfiguration();

        if ("provision_disabled".equals(provisioning)) {
            jitProvisioningConfiguration.setProvisioningEnabled(false);
        } else if ("provision_static".equals(provisioning)
                || "provision_dynamic".equals(provisioning)) {
            jitProvisioningConfiguration.setProvisioningEnabled(true);
        }

        jitProvisioningConfiguration.setProvisioningUserStore(paramMap
                .get("provision_static_dropdown"));

        if (paramMap.get("provision_dynamic_dropdown") != null) {
            if (!"--- Select Claim URI ---".equals(paramMap.get("provision_dynamic_dropdown"))) {
                jitProvisioningConfiguration.setProvisioningUserStore(paramMap
                        .get("provision_dynamic_dropdown"));
            }
        }

        fedIdp.setJustInTimeProvisioningConfiguration(jitProvisioningConfiguration);

    }

    /**
     * 
     * @param o1
     * @param o2
     * @return
     */
    private static ProvisioningConnector[] concatArrays(ProvisioningConnector[] o1,
            ProvisioningConnector[] o2) {
        ProvisioningConnector[] ret = new ProvisioningConnector[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    /**
     * 
     * @param o1
     * @param o2
     * @return
     */
    private static FederatedAuthenticator[] concatArrays(FederatedAuthenticator[] o1,
            FederatedAuthenticator[] o2) {
        FederatedAuthenticator[] ret = new FederatedAuthenticator[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }
}
