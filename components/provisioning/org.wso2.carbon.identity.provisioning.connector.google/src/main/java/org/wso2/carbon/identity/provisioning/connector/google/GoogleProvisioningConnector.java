/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provisioning.connector.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import com.google.api.services.admin.directory.model.Users;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.core.util.IdentityIOStreamUtils;
import org.wso2.carbon.identity.provisioning.AbstractOutboundProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.ProvisionedIdentifier;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class GoogleProvisioningConnector extends AbstractOutboundProvisioningConnector {

    private static final long serialVersionUID = -6152718786151333233L;

    private static final Log log = LogFactory.getLog(GoogleProvisioningConnector.class);
    private static SecureRandom random = new SecureRandom();
    private static File googlePrvKey = null;
    private GoogleProvisioningConnectorConfig configHolder;

    @Override
    /**
     *
     */
    public void init(Property[] provisioningProperties) throws IdentityProvisioningException {
        Properties configs = new Properties();

        if (provisioningProperties != null && provisioningProperties.length > 0) {
            for (Property property : provisioningProperties) {

                if (GoogleConnectorConstants.PRIVATE_KEY.equals(property.getName())) {
                    FileOutputStream fos = null;
                    try {
                        byte[] decodedBytes = Base64Utils.decode(property.getValue());
                        googlePrvKey = new File("googlePrvKey");
                        fos = new FileOutputStream(googlePrvKey);
                        fos.write(decodedBytes);
                    } catch (IOException e) {
                        log.error("Error while generating private key file object", e);
                    }finally {
                        IdentityIOStreamUtils.closeOutputStream(fos);
                    }
                }
                configs.put(property.getName(), property.getValue());

                if (IdentityProvisioningConstants.JIT_PROVISIONING_ENABLED.equals(property
                                                                                          .getName()) && "1".equals(property.getValue())) {
                    jitProvisioningEnabled = true;


                }
            }
        }

        configHolder = new GoogleProvisioningConnectorConfig(configs);
    }

    @Override
    public String getClaimDialectUri() throws IdentityProvisioningException {
        // dialect uri is service provider specific - not governed by the
        // connector.
        return null;
    }

    @Override
    public ProvisionedIdentifier provision(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {

        ProvisionedIdentifier identifier = null;

        if (provisioningEntity != null) {

            if (provisioningEntity.isJitProvisioning() && !isJitProvisioningEnabled()) {
                log.debug("JIT provisioning disabled for Google connector");
                return null;
            }

            if (provisioningEntity.getEntityType() == ProvisioningEntityType.USER) {
                if (provisioningEntity.getOperation() == ProvisioningOperation.DELETE) {
                    deleteUser(provisioningEntity);

                    // creates a provisioned identifier for the de-provisioned user.
                    identifier = new ProvisionedIdentifier();
                    identifier.setIdentifier(null);

                } else if (provisioningEntity.getOperation() == ProvisioningOperation.POST) {
                    String provisionedId = createUser(provisioningEntity);

                    // creates a provisioned identifier for the provisioned user.
                    identifier = new ProvisionedIdentifier();
                    identifier.setIdentifier(provisionedId);

                } else if (provisioningEntity.getOperation() == ProvisioningOperation.PUT) {
                    updateUser(provisioningEntity);
                } else {
                    log.warn("Unsupported provisioning opertaion for Google Provisioning Connector.");
                }
            } else {
                log.warn("Unsupported provisioning opertaion for Google Provisioning Connector.");
            }
        }
        return identifier;
    }

    protected void updateUser(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (isDebugEnabled) {
            log.debug("Triggering update operation for Google Provisioning Connector");
        }

        ProvisionedIdentifier provisionedIdentifier = provisioningEntity.getIdentifier();

        if (provisionedIdentifier != null && provisionedIdentifier.getIdentifier() != null) {

            User updateUser = updateGoogleUser(provisioningEntity);

            if (updateUser == null) {
                return;
            }

            Directory.Users.Update request;

            try {
                request = getDirectoryService().users().update(
                        provisionedIdentifier.getIdentifier(), updateUser);
                request.execute();

            } catch (IOException e) {
                throw new IdentityProvisioningException("Error while updating Google user : "
                                                        + provisioningEntity.getEntityName(), e);
            }

            if (isDebugEnabled) {
                log.debug("updating user :" + provisioningEntity.getEntityName()
                          + " with the primaryEmail : " + provisionedIdentifier.getIdentifier());
            }
        } else {
            throw new IdentityProvisioningException(
                    "Cannot updating Google user, provisionedIdentifier is invalide.");
        }

        if (log.isTraceEnabled()) {
            log.trace("Ending updatingUser() of " + GoogleProvisioningConnector.class);
        }

    }

    protected String createUser(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (isDebugEnabled) {
            log.debug("Triggering create operation for Google Provisioning Connector");
        }

        User createdUser = null;
        try {
            User newUser = new User();

            newUser = buildGoogleUser(provisioningEntity);
            if (isDebugEnabled) {
                log.debug("New google user to be created : " + newUser.toPrettyString());
            }

            Directory.Users.Insert request = getDirectoryService().users().insert(newUser);
            createdUser = request.execute();

        } catch (IOException e) {
            throw new IdentityProvisioningException("Error while creating user : "
                                                    + provisioningEntity.getEntityName(), e);
        }

        if (isDebugEnabled) {
            log.debug("Returning created user's email : " + createdUser.getPrimaryEmail());
        }

        if (log.isTraceEnabled()) {
            log.trace("Ending createUser() of " + GoogleProvisioningConnector.class);
        }
        return createdUser.getPrimaryEmail();
    }

    /**
     * Delete provisioned user from google account
     *
     * @param provisioningEntity
     * @throws IdentityProvisioningException
     */
    protected void deleteUser(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (isDebugEnabled) {
            log.debug("Triggering delete operation for Google Provisioning Connector");
        }

        ProvisionedIdentifier provisionedIdentifier = provisioningEntity.getIdentifier();
        if (provisionedIdentifier != null && provisionedIdentifier.getIdentifier() != null) {
            User deletingUser = new User();
            deletingUser.setPrimaryEmail(provisionedIdentifier.getIdentifier());

            Directory.Users.Delete request;
            try {
                request = getDirectoryService().users().delete(
                        provisionedIdentifier.getIdentifier());
                request.execute();

            } catch (IOException e) {
                if (((GoogleJsonResponseException) e).getStatusCode() == 404) {
                    log.warn("Exception while deleting user from google. User may be already deleted from google");
                    if (log.isDebugEnabled()) {
                        log.debug("Exception while deleting user from google. User may be already deleted from google", e);
                    }
                } else {
                    throw new IdentityProvisioningException("Error while deleting Google user : "
                                                            + provisioningEntity.getEntityName(), e);
                }
            }

            if (isDebugEnabled) {
                log.debug("Deleted user :" + provisioningEntity.getEntityName()
                          + " with the primaryEmail : " + provisionedIdentifier.getIdentifier());
            }
        } else {
            throw new IdentityProvisioningException(
                    "Cannot delete Google user, provisionedIdentifier is invalide.");
        }

        if (log.isTraceEnabled()) {
            log.trace("Ending deleteUser() of " + GoogleProvisioningConnector.class);
        }
    }

    /**
     * @return
     * @throws IdentityProvisioningException
     */
    protected String listUsers(String query) throws IdentityProvisioningException {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (isDebugEnabled) {
            log.debug("Starting listUsers() of " + GoogleProvisioningConnector.class);
        }

        StringBuilder sb = new StringBuilder();
        List<User> allUsers = new ArrayList<>();
        Directory.Users.List request;
        try {
            request = getDirectoryService().users().list().setCustomer("my_customer");

            // Get all users
            do {
                try {
                    Users currentPage = request.execute();
                    allUsers.addAll(currentPage.getUsers());
                    request.setPageToken(currentPage.getNextPageToken());
                } catch (IOException e) {
                    log.error("Error while retrieving user info, continue to retrieve", e);
                    request.setPageToken(null);
                }
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);

            // Print all users
            for (User currentUser : allUsers) {
                sb.append(currentUser.getPrimaryEmail() + "\n");
                if (isDebugEnabled) {
                    log.debug("List Google users : " + currentUser.getPrimaryEmail());
                }
            }
        } catch (IOException e) {
            throw new IdentityProvisioningException(e);
        }

        if (isDebugEnabled) {
            log.debug("Ending listUsers() of " + GoogleProvisioningConnector.class);
        }
        return sb.toString();
    }

    /**
     * Build and returns a Directory service object authorized with the service accounts that act on
     * behalf of the given user.
     *
     * @return Directory service object that is ready to make requests.
     * @throws IdentityProvisioningException
     */
    protected Directory getDirectoryService() throws IdentityProvisioningException {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (isDebugEnabled) {
            log.debug("Starting getDirectoryService() of " + GoogleProvisioningConnector.class);
        }

        String serviceAccountEmailKey = "google_prov_service_acc_email";
        String adminEmailKey = "google_prov_admin_email";
        String privateKeyKey = "google_prov_private_key";
        String applicationNameKey = "google_prov_application_name";

        /** Email of the Service Account */
        String serviceAccountId = this.configHolder.getValue(serviceAccountEmailKey);
        /** Admin email */
        String serviceAccountUser = this.configHolder.getValue(adminEmailKey);
        /** Path to the Service Account's Private Key file */
        String serviceAccountPrivateKeyString = this.configHolder.getValue(privateKeyKey);
        /** Application name */
        String applicationName = this.configHolder.getValue(applicationNameKey);

        HttpTransport httpTransport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();

        if (isDebugEnabled) {
            log.debug("serviceAccountId" + serviceAccountId);
            log.debug("setServiceAccountScopes"
                      + Arrays.asList(DirectoryScopes.ADMIN_DIRECTORY_USER));
            log.debug("setServiceAccountUser" + serviceAccountUser);
        }

        Directory service = null;
        try {
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport).setJsonFactory(jsonFactory)
                    .setServiceAccountId(serviceAccountId)
                    .setServiceAccountScopes(Arrays.asList(DirectoryScopes.ADMIN_DIRECTORY_USER))
                    .setServiceAccountUser(serviceAccountUser)
                    .setServiceAccountPrivateKeyFromP12File(googlePrvKey).build();

            service = new Directory.Builder(httpTransport, jsonFactory, credential)
                    .setHttpRequestInitializer(credential).setApplicationName(applicationName)
                    .build();

        } catch (GeneralSecurityException | IOException e) {
            throw new IdentityProvisioningException("Error while obtaining connection from google",
                                                    e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Ending getDirectoryService() of " + GoogleProvisioningConnector.class);
        }
        return service;
    }

    /**
     * Buld Google user object to provision
     *
     * @param provisioningEntity
     * @return
     */
    protected User buildGoogleUser(ProvisioningEntity provisioningEntity) throws IdentityProvisioningException {
        User newUser = new User();
        UserName username = new UserName();

        List<String> wso2IsUsernames = getUserNames(provisioningEntity.getAttributes());
        String wso2IsUsername = null;

        if (CollectionUtils.isNotEmpty(wso2IsUsernames)) {
            // first element must be the user name.
            wso2IsUsername = wso2IsUsernames.get(0);
        }

        String primaryEmailClaimKey = "google_prov_email_claim_dropdown";
        String domainNameKey = "google_prov_domain_name";

        String defaultFamilyNameKey = "google_prov_familyname";
        String defaultGivenNameKey = "google_prov_givenname";

        String familyNameClaimKey = "google_prov_familyname_claim_dropdown";
        String givenNameClaimKey = "google_prov_givenname_claim_dropdown";
        String provisioningPatternKey = "google_prov_pattern";
        String provisioningSeparatorKey = "google_prov_separator";
        String idpName_key = "identityProviderName";
        String userIdClaimUriKey = "userIdClaimUri";

        Map<String, String> requiredAttributes = getSingleValuedClaims(provisioningEntity
                                                                               .getAttributes());

        /** Provisioning Pattern */
        String provisioningPattern = this.configHolder.getValue(provisioningPatternKey);
        String provisioningSeparator = this.configHolder.getValue(provisioningSeparatorKey);
        String idpName = this.configHolder.getValue(idpName_key);
        String userIdClaimURL = this.configHolder.getValue(userIdClaimUriKey);
        String provisioningDomain = this.configHolder.getValue(domainNameKey);


        String userId = provisioningEntity.getEntityName();

        if (StringUtils.isNotBlank(requiredAttributes.get(userIdClaimURL))) {
            userId = requiredAttributes.get(userIdClaimURL);
        }

        String userIdFromPattern = null;

        if (provisioningPattern != null) {
            userIdFromPattern = buildUserId(provisioningEntity, provisioningPattern,
                                            provisioningSeparator, idpName);
        }

        if (StringUtils.isNotBlank(userIdFromPattern)) {
            userId = userIdFromPattern;
        }

        if(StringUtils.isEmpty(userId)){
            throw new IdentityProvisioningException("Could not find Provisioning User Identification");
        }

        if (StringUtils.isNotBlank(provisioningDomain) && !userId.endsWith(provisioningDomain)) {
            userId = userId.replaceAll("@", ".").concat("@").concat(provisioningDomain);
        }

        // Set given name
        String givenNameClaim = this.configHolder.getValue(givenNameClaimKey);
        String givenNameValue = requiredAttributes.get(givenNameClaim);
        if (StringUtils.isBlank(givenNameValue)) {
            String defaultGivenNameValue = this.configHolder.getValue(defaultGivenNameKey);
            if (StringUtils.isNotBlank(defaultGivenNameValue)) {
                givenNameValue = defaultGivenNameValue;
            } else {
                givenNameValue = wso2IsUsername;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("New Google user given name : " + givenNameValue);
        }
        username.setGivenName(givenNameValue);

        // Set family name
        String familyNameClaim = this.configHolder.getValue(familyNameClaimKey);
        String familyNameValue = requiredAttributes.get(familyNameClaim);
        if (StringUtils.isBlank(familyNameValue)) {
            String defaultFamilyNameValue = this.configHolder.getValue(defaultFamilyNameKey);
            if (StringUtils.isNotBlank(defaultFamilyNameValue)) {
                familyNameValue = defaultFamilyNameValue;
            } else {
                familyNameValue = wso2IsUsername;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("New Google user family name : " + familyNameValue);
        }
        username.setFamilyName(familyNameValue);

        newUser.setName(username);
        newUser.setPassword(generatePassword());

        //set primary email
        if (log.isDebugEnabled()) {
            log.debug("New Google user primary email : " + userId);
        }
        newUser.setPrimaryEmail(userId);

        return newUser;
    }

    /**
     * Buld Google user object to provision
     *
     * @param provisioningEntity
     * @return
     */
    protected User updateGoogleUser(ProvisioningEntity provisioningEntity) {

        User updateUser = new User();
        updateUser.setPrimaryEmail(provisioningEntity.getIdentifier().getIdentifier());
        UserName username = new UserName();

        String defaultFamilyNameKey = "google_prov_familyname";
        String defaultGivenNameKey = "google_prov_givenname";

        String familyNameClaimKey = "google_prov_familyname_claim_dropdown";
        String givenNameClaimKey = "google_prov_givenname_claim_dropdown";

        Map<String, String> requiredAttributes = getSingleValuedClaims(provisioningEntity
                                                                               .getAttributes());

        if (MapUtils.isEmpty(requiredAttributes)) {
            return null;
        }

        // Set given name
        String givenNameClaim = this.configHolder.getValue(givenNameClaimKey);
        String givenNameValue = requiredAttributes.get(givenNameClaim);
        if (StringUtils.isBlank(givenNameValue)) {
            String defaultGivenNameValue = this.configHolder.getValue(defaultGivenNameKey);
            if (StringUtils.isNotBlank(defaultGivenNameValue)) {
                givenNameValue = defaultGivenNameValue;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("New Google user given name : " + givenNameValue);
        }
        username.setGivenName(givenNameValue);

        // Set family name
        String familyNameClaim = this.configHolder.getValue(familyNameClaimKey);
        String familyNameValue = requiredAttributes.get(familyNameClaim);
        if (StringUtils.isBlank(familyNameValue)) {
            String defaultFamilyNameValue = this.configHolder.getValue(defaultFamilyNameKey);
            if (StringUtils.isNotBlank(defaultFamilyNameValue)) {
                familyNameValue = defaultFamilyNameValue;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("New Google user family name : " + familyNameValue);
        }
        username.setFamilyName(familyNameValue);

        updateUser.setName(username);
        updateUser.setPassword(generatePassword());

        return updateUser;
    }

    /**
     * Generates (random) password for user to be provisioned
     *
     * @return
     */
    protected String generatePassword() {
        return new BigInteger(130, random).toString(32);
    }

}
