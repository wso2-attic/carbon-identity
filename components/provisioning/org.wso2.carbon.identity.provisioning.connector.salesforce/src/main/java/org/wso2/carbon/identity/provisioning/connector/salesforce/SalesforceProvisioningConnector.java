/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provisioning.connector.salesforce;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.provisioning.AbstractOutboundProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.ProvisionedIdentifier;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class SalesforceProvisioningConnector extends AbstractOutboundProvisioningConnector {

    private static final long serialVersionUID = 8465869197181038416L;

    private static final Log log = LogFactory.getLog(SalesforceProvisioningConnector.class);
    private SalesforceProvisioningConnectorConfig configHolder;

    @Override
    /**
     *
     */
    public void init(Property[] provisioningProperties) throws IdentityProvisioningException {
        Properties configs = new Properties();

        if (provisioningProperties != null && provisioningProperties.length > 0) {
            for (Property property : provisioningProperties) {
                configs.put(property.getName(), property.getValue());
                if (IdentityProvisioningConstants.JIT_PROVISIONING_ENABLED.equals(property
                        .getName()) && "1".equals(property.getValue())) {
                    jitProvisioningEnabled = true;
                }
            }
        }

        configHolder = new SalesforceProvisioningConnectorConfig(configs);
    }

    @Override
    /**
     *
     */
    public ProvisionedIdentifier provision(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {
        String provisionedId = null;

        if (provisioningEntity != null) {

            if (provisioningEntity.isJitProvisioning() && !isJitProvisioningEnabled()) {
                log.debug("JIT provisioning disabled for Salesforce connector");
                return null;
            }

            if (provisioningEntity.getEntityType() == ProvisioningEntityType.USER) {
                if (provisioningEntity.getOperation() == ProvisioningOperation.DELETE) {
                    deleteUser(provisioningEntity);
                } else if (provisioningEntity.getOperation() == ProvisioningOperation.POST) {
                    provisionedId = createUser(provisioningEntity);
                } else if (provisioningEntity.getOperation() == ProvisioningOperation.PUT) {
                    update(provisioningEntity.getIdentifier().getIdentifier(),
                            buildJsonObject(provisioningEntity));
                } else {
                    log.warn("Unsupported provisioning opertaion.");
                }
            } else {
                log.warn("Unsupported provisioning opertaion.");
            }
        }

        // creates a provisioned identifier for the provisioned user.
        ProvisionedIdentifier identifier = new ProvisionedIdentifier();
        identifier.setIdentifier(provisionedId);
        return identifier;
    }

    /**
     * @param provisioningEntity
     * @return
     * @throws IdentityProvisioningException
     */
    private JSONObject buildJsonObject(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {

        boolean isDebugEnabled = log.isDebugEnabled();

        String provisioningPatternKey = "sf-prov-pattern";
        String provisioningSeparatorKey = "sf-prov-separator";
        String idpName_key = "identityProviderName";
        String userIdClaimUriKey = "userIdClaimUri";
        String provisioningDomainKey = "sf-prov-domainName";

        String provisioningPattern = this.configHolder.getValue(provisioningPatternKey);
        String provisioningSeparator = this.configHolder.getValue(provisioningSeparatorKey);
        String idpName = this.configHolder.getValue(idpName_key);

        JSONObject user = new JSONObject();

        try {
            /**
             * Mandatory properties : 12 and this will vary according to API Version
             *
             * Alias, Email, EmailEncodingKey, LanguageLocaleKey, LastName, LocaleSidKey, ProfileId,
             * TimeZoneSidKey, User-name, UserPermissionsCallCenterAutoLogin,
             * UserPermissionsMarketingUser, UserPermissionsOfflineUser
             **/

            Map<String, String> requiredAttributes = getSingleValuedClaims(provisioningEntity
                    .getAttributes());

            String userIdClaimURL = this.configHolder.getValue(userIdClaimUriKey);
            String provisioningDomain = this.configHolder.getValue(provisioningDomainKey);
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

            if (StringUtils.isBlank(userId)) {
                throw new IdentityProvisioningException("Cannot Find Username Attribute for Provisioning");
            }
            
            if (StringUtils.isNotBlank(provisioningDomain) && !userId.endsWith(provisioningDomain)) {
                userId = userId.replaceAll("@", ".").concat("@").concat(provisioningDomain);
            }
            requiredAttributes.put(SalesforceConnectorConstants.USERNAME_ATTRIBUTE, userId);

            Iterator<Entry<String, String>> iterator = requiredAttributes.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, String> mapEntry = iterator.next();
                if ("true".equals(mapEntry.getValue())) {
                    user.put(mapEntry.getKey(), true);
                } else if ("false".equals(mapEntry.getValue())) {
                    user.put(mapEntry.getKey(), false);
                } else {
                    user.put(mapEntry.getKey(), mapEntry.getValue());
                }
                if (isDebugEnabled) {
                    log.debug("The key is: " + mapEntry.getKey() + ",value is :"
                            + mapEntry.getValue());
                }
            }

            if (isDebugEnabled) {
                log.debug("JSON object of User\n" + user.toString(2));
            }

        } catch (JSONException e) {
            log.error("Error while creating JSON body");
            throw new IdentityProvisioningException(e);
        }

        return user;
    }

    /**
     * @param provisioningEntity
     * @return
     * @throws IdentityProvisioningException
     */
    private String createUser(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {

        boolean isDebugEnabled = log.isDebugEnabled();

        HttpClient httpclient = new HttpClient();
        JSONObject user = buildJsonObject(provisioningEntity);

        PostMethod post = new PostMethod(this.getUserObjectEndpoint());
        setAuthorizationHeader(post);

        try {
            post.setRequestEntity(new StringRequestEntity(user.toString(),
                    SalesforceConnectorConstants.CONTENT_TYPE_APPLICATION_JSON, null));
        } catch (UnsupportedEncodingException e) {
            log.error("Error in encoding provisioning request");
            throw new IdentityProvisioningException(e);
        }

        String provisionedId = null;

        try {

            httpclient.executeMethod(post);

            if (isDebugEnabled) {
                log.debug("HTTP status " + post.getStatusCode() + " creating user");
            }

            if (post.getStatusCode() == HttpStatus.SC_CREATED) {
                JSONObject response = new JSONObject(new JSONTokener(new InputStreamReader(
                        post.getResponseBodyAsStream())));
                if (isDebugEnabled) {
                    log.debug("Create response: " + response.toString(2));
                }

                if (response.getBoolean("success")) {
                    provisionedId = response.getString("id");
                    if (isDebugEnabled) {
                        log.debug("New record id " + provisionedId);
                    }
                }
            } else {
                log.error("recieved response status code :" + post.getStatusCode()
                        + " text : " + post.getStatusText());
                if (isDebugEnabled) {
                    log.debug("Error response : " + readResponse(post));
                }
            }
        } catch (IOException | JSONException e) {
            throw new IdentityProvisioningException("Error in invoking provisioning operation for the user", e);
        } finally {
            post.releaseConnection();
        }

        if (isDebugEnabled) {
            log.debug("Returning created user's ID : " + provisionedId);
        }

        return provisionedId;
    }

    private String readResponse(PostMethod post) throws IOException {
        InputStream is = post.getResponseBodyAsStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        rd.close();
        return response.toString();
    }

    /**
     * @param provisioningEntity
     * @throws IdentityProvisioningException
     */
    private void deleteUser(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {

        JSONObject entity = new JSONObject();
        try {
            entity.put(SalesforceConnectorConstants.IS_ACTIVE, false);
            entity.put(SalesforceConnectorConstants.USERNAME_ATTRIBUTE, alterUsername(provisioningEntity));
            update(provisioningEntity.getIdentifier().getIdentifier(), entity);
        } catch (JSONException e) {
            log.error("Error while creating JSON body");
            throw new IdentityProvisioningException(e);
        }
    }

    /**
     * @param provsionedId
     * @param entity
     * @return
     * @throws IdentityProvisioningException
     */
    private void update(String provsionedId, JSONObject entity)
            throws IdentityProvisioningException {

        boolean isDebugEnabled = log.isDebugEnabled();

        try {

            PostMethod patch = new PostMethod(this.getUserObjectEndpoint() + provsionedId) {
                @Override
                public String getName() {
                    return "PATCH";
                }
            };

            setAuthorizationHeader(patch);
            patch.setRequestEntity(new StringRequestEntity(entity.toString(), "application/json",
                    null));

            try {
                HttpClient httpclient = new HttpClient();
                httpclient.executeMethod(patch);
                if (patch.getStatusCode() == HttpStatus.SC_OK
                        || patch.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                    if (isDebugEnabled) {

                        log.debug("HTTP status " + patch.getStatusCode() + " updating user "
                                + provsionedId + "\n\n");
                    }
                } else {
                    log.error("recieved response status code :" + patch.getStatusCode()
                            + " text : " + patch.getStatusText());
                    if (isDebugEnabled) {
                        log.debug("Error response : " + readResponse(patch));
                    }
                }

            } catch (IOException e) {
                log.error("Error in invoking provisioning request");
                throw new IdentityProvisioningException(e);
            } finally {
                patch.releaseConnection();
            }

        } catch (UnsupportedEncodingException e) {
            log.error("Error in encoding provisioning request");
            throw new IdentityProvisioningException(e);
        }
    }

    /**
     * adding OAuth authorization headers to a httpMethod
     *
     * @param httpMethod method which wants to add Authorization header
     */
    private void setAuthorizationHeader(HttpMethodBase httpMethod)
            throws IdentityProvisioningException {

        boolean isDebugEnabled = log.isDebugEnabled();

        String accessToken = authenticate();
        if (StringUtils.isNotBlank(accessToken)) {
            httpMethod.setRequestHeader(SalesforceConnectorConstants.AUTHORIZATION_HEADER_NAME,
                    SalesforceConnectorConstants.AUTHORIZATION_HEADER_OAUTH + " " + accessToken);

            if (isDebugEnabled) {
                log.debug("Setting authorization header for method : " + httpMethod.getName()
                        + " as follows,");
                Header authorizationHeader = httpMethod
                        .getRequestHeader(SalesforceConnectorConstants.AUTHORIZATION_HEADER_NAME);
                log.debug(authorizationHeader.getName() + ": " + authorizationHeader.getValue());
            }
        } else {
            throw new IdentityProvisioningException("Authentication failed");
        }

    }

    /**
     * authenticate to salesforce API.
     */
    private String authenticate() throws IdentityProvisioningException {

        boolean isDebugEnabled = log.isDebugEnabled();

        HttpClient httpclient = new HttpClient();

        String url = configHolder.getValue(SalesforceConnectorConstants.PropertyConfig.OAUTH2_TOKEN_ENDPOINT);

        PostMethod post = new PostMethod(StringUtils.isNotBlank(url) ?
                url : IdentityApplicationConstants.SF_OAUTH2_TOKEN_ENDPOINT);

        post.addParameter(SalesforceConnectorConstants.CLIENT_ID,
                configHolder.getValue(SalesforceConnectorConstants.PropertyConfig.CLIENT_ID));
        post.addParameter(SalesforceConnectorConstants.CLIENT_SECRET,
                configHolder.getValue(SalesforceConnectorConstants.PropertyConfig.CLIENT_SECRET));
        post.addParameter(SalesforceConnectorConstants.PASSWORD,
                configHolder.getValue(SalesforceConnectorConstants.PropertyConfig.PASSWORD));
        post.addParameter(SalesforceConnectorConstants.GRANT_TYPE,
                SalesforceConnectorConstants.GRANT_TYPE_PASSWORD);
        post.addParameter(SalesforceConnectorConstants.USERNAME,
                configHolder.getValue(SalesforceConnectorConstants.PropertyConfig.USERNAME));

        StringBuilder sb = new StringBuilder();
        try {
            // send the request
            int responseStatus = httpclient.executeMethod(post);
            if (isDebugEnabled) {
                log.debug("Authentication to salesforce returned with response code: "
                        + responseStatus);
            }

            sb.append("HTTP status " + post.getStatusCode() + " creating user\n\n");

            if (post.getStatusCode() == HttpStatus.SC_OK) {
                JSONObject response = new JSONObject(new JSONTokener(new InputStreamReader(
                        post.getResponseBodyAsStream())));
                if (isDebugEnabled) {
                    log.debug("Authenticate response: " + response.toString(2));
                }

                Object attributeValObj = response.opt("access_token");
                if (attributeValObj instanceof String) {
                    if (isDebugEnabled) {
                        log.debug("Access token is : " + (String) attributeValObj);
                    }
                    return (String) attributeValObj;
                } else {
                    log.error("Authentication response type : " + attributeValObj.toString()
                            + " is invalide");
                }
            } else {
                log.error("recieved response status code :" + post.getStatusCode() + " text : "
                        + post.getStatusText());
            }
        } catch (JSONException | IOException e) {
            throw new IdentityProvisioningException("Error in decoding response to JSON", e);
        } finally {
            post.releaseConnection();
        }

        return "";
    }

    /**
     * builds salesforce user end point using configurations
     *
     * @return
     */
    private String getUserObjectEndpoint() {

        boolean isDebugEnabled = log.isDebugEnabled();

        String url = configHolder.getValue(SalesforceConnectorConstants.PropertyConfig.DOMAIN_NAME)
                + SalesforceConnectorConstants.CONTEXT_SERVICES_DATA
                + configHolder.getValue(SalesforceConnectorConstants.PropertyConfig.API_VERSION)
                + SalesforceConnectorConstants.CONTEXT_SOOBJECTS_USER;
        if (isDebugEnabled) {
            log.debug("Built user endpoint url : " + url);
        }

        return url;
    }

    /**
     * Builds Salesforce query point using configurations
     *
     * @return
     */
    private String getDataQueryEndpoint() {
        if (log.isTraceEnabled()) {
            log.trace("Starting getDataQueryEndpoint() of " + SalesforceProvisioningConnector.class);
        }
        boolean isDebugEnabled = log.isDebugEnabled();

        String url = configHolder.getValue(SalesforceConnectorConstants.PropertyConfig.DOMAIN_NAME)
                + SalesforceConnectorConstants.CONTEXT_SERVICES_DATA
                + configHolder.getValue(SalesforceConnectorConstants.PropertyConfig.API_VERSION)
                + SalesforceConnectorConstants.CONTEXT_QUERY;
        if (isDebugEnabled) {
            log.debug("Built query endpoint url : " + url);
        }

        return url;
    }

    /**
     * @return
     * @throws IdentityProvisioningException
     */
    public String listUsers(String query) throws IdentityProvisioningException {

        if (log.isTraceEnabled()) {
            log.trace("Starting listUsers() of " + SalesforceProvisioningConnector.class);
        }
        boolean isDebugEnabled = log.isDebugEnabled();

        if (StringUtils.isBlank(query)) {
            query = SalesforceConnectorDBQueries.SALESFORCE_LIST_USER_SIMPLE_QUERY;
        }

        HttpClient httpclient = new HttpClient();
        GetMethod get = new GetMethod(this.getDataQueryEndpoint());
        setAuthorizationHeader(get);

        // set the SOQL as a query param
        NameValuePair[] params = new NameValuePair[1];
        params[0] = new NameValuePair("q", query);
        get.setQueryString(params);

        StringBuilder sb = new StringBuilder();
        try {
            httpclient.executeMethod(get);
            if (get.getStatusCode() == HttpStatus.SC_OK) {

                JSONObject response = new JSONObject(new JSONTokener(new InputStreamReader(
                        get.getResponseBodyAsStream())));
                if (isDebugEnabled) {
                    log.debug("Query response: " + response.toString(2));
                }

                // Build the returning string
                sb.append(response.getString("totalSize") + " record(s) returned\n\n");
                JSONArray results = response.getJSONArray("records");
                for (int i = 0; i < results.length(); i++) {
                    sb.append(results.getJSONObject(i).getString("Id") + ", "

                            + results.getJSONObject(i).getString("Alias") + ", "
                            + results.getJSONObject(i).getString("Email") + ", "
                            + results.getJSONObject(i).getString("LastName") + ", "
                            + results.getJSONObject(i).getString("Name") + ", "
                            + results.getJSONObject(i).getString("ProfileId") + ", "
                            + results.getJSONObject(i).getString("Username") + "\n");
                }
                sb.append("\n");
            } else {
                log.error("recieved response status code:" + get.getStatusCode() + " text : "
                        + get.getStatusText());
            }
        } catch (JSONException | IOException e) {
            log.error("Error in invoking provisioning operation for the user listing");
            throw new IdentityProvisioningException(e);
        }finally {
            get.releaseConnection();
        }

        if (isDebugEnabled) {
            log.debug("Returning string : " + sb.toString());
        }

        if (log.isTraceEnabled()) {
            log.trace("Ending listUsers() of " + SalesforceProvisioningConnector.class);
        }
        return sb.toString();
    }

    /**
     * Alter username while changing user to active state to inactive state. This is necessary when adding previously
     * deleted users.
     *
     * @param provisioningEntity
     * @return
     * @throws IdentityProvisioningException
     */
    protected String alterUsername(ProvisioningEntity provisioningEntity) throws IdentityProvisioningException {

        if (StringUtils.isBlank(provisioningEntity.getEntityName())) {
            throw new IdentityProvisioningException("Could Not Find Entity Name from Provisioning Entity");
        }
        String alteredUsername =
                SalesforceConnectorConstants.SALESFORCE_OLD_USERNAME_PREFIX + provisioningEntity.getEntityName();

        if (log.isDebugEnabled()) {
            log.debug("Alter username: " + provisioningEntity.getEntityName() + " to: " + alteredUsername +
                      "while deleting user");
        }
        return alteredUsername;
    }
}
