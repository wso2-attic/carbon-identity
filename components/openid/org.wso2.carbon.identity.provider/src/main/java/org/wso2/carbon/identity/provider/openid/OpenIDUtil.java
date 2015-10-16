/*
 * Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provider.openid;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains OpenID related utility functions
 */
public class OpenIDUtil {

    private static final Set<Character> UNRESERVED_CHARACTERS = new HashSet<Character>();
    private static final Log log = LogFactory.getLog(OpenIDUtil.class);
    private static Map<String, String> axMapping = new HashMap<String, String>();

    static {
        for (char c = 'a'; c <= 'z'; c++)
            UNRESERVED_CHARACTERS.add(Character.valueOf(c));

        for (char c = 'A'; c <= 'A'; c++)
            UNRESERVED_CHARACTERS.add(Character.valueOf(c));

        for (char c = '0'; c <= '9'; c++)
            UNRESERVED_CHARACTERS.add(Character.valueOf(c));

        UNRESERVED_CHARACTERS.add(Character.valueOf('-'));
        UNRESERVED_CHARACTERS.add(Character.valueOf('.'));
        UNRESERVED_CHARACTERS.add(Character.valueOf('_'));
        UNRESERVED_CHARACTERS.add(Character.valueOf('~'));
    }

    private OpenIDUtil() {
    }

    /**
     * Generate OpenID for a given user.
     *
     * @param user User
     * @return Generated OpenID
     * @throws IdentityProviderException
     */
    public static String generateOpenID(String user) throws IdentityProviderException {

        ServerConfiguration serverConfig = null;
        String openIDUserUrl = null;
        String openID = null;
        URI uri = null;
        URL url = null;
        String encodedUser = null;

        serverConfig = ServerConfiguration.getInstance();
        openIDUserUrl = getOpenIDServerURL();

        encodedUser = normalizeUrlEncoding(user);

        openID = String.format(openIDUserUrl, encodedUser);

        try {
            uri = new URI(openID);
        } catch (URISyntaxException e) {
            log.error("Invalid OpenID URL :" + openID, e);
            throw new IdentityProviderException("Invalid OpenID URL :" + openID, e);
        }

        try {
            url = uri.normalize().toURL();
            if (url.getQuery() != null || url.getRef() != null) {
                log.error("Invalid user name for OpenID :" + openID);
                throw new IdentityProviderException("Invalid user name for OpenID :" + openID);
            }
        } catch (MalformedURLException e) {
            log.error("Malformed OpenID URL :" + openID, e);
            throw new IdentityProviderException("Malformed OpenID URL :" + openID, e);
        }

        openID = url.toString();

        if (log.isDebugEnabled()) {
            log.debug("OpenID generated successfully : " + openID);
        }

        return openID;
    }

    /**
     * @param text
     * @return
     */
    private static String normalizeUrlEncoding(String text) {

        if (text == null) {
            return null;
        }

        int len = text.length();
        StringBuilder normalized = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char current = text.charAt(i);
            if (current == '%' && i < len - 2) {
                String percentCode = text.substring(i, i + 3).toUpperCase();
                try {
                    String str = URLDecoder.decode(percentCode, "ISO-8859-1");
                    char chr = str.charAt(0);
                    if (UNRESERVED_CHARACTERS.contains(Character.valueOf(chr))) {
                        normalized.append(chr);
                    } else {
                        normalized.append(percentCode);
                    }
                } catch (UnsupportedEncodingException e) {
                    normalized.append(percentCode);
                }
                i += 2;
            } else {
                normalized.append(current);
            }
        }
        return normalized.toString();
    }

    /**
     * This provides a mapping between http://schema.openid.net/ and
     * http://axschema.org
     *
     * @param val schema name-space URL
     * @return mapped value
     */
    public static String getMappedAxSchema(String val) {
        if (axMapping.containsKey(val)) {
            return axMapping.get(val);
        }

        return val;
    }

    /**
     * Find the OpenID corresponding to the given user name.
     *
     * @param userName User name
     * @return OpenID corresponding the given user name.
     * @throws IdentityProviderException
     */
    public static String getOpenID(String userName) throws IdentityProviderException {
        return generateOpenID(userName);
    }

    /**
     * @param openID
     * @return
     * @throws Exception
     */
    public static String getUserName(String openID) throws MalformedURLException {

        // openIDPattern = https://openid:9443/openid/admin
        String openIDUrlPath = new URL(openID).getPath();
        String contextPath = "/openid";
        return openIDUrlPath.substring(openIDUrlPath.indexOf(contextPath) + contextPath.length() + 1,
                                       openIDUrlPath.length());
    }

    /**
     * Verify user name/password authentication.
     *
     * @param username User name
     * @param password Password
     * @return true if user successfully authenticated
     */
    public static boolean doLogin(String username, String password) {
        try {
            UserStoreManager userStore = IdentityTenantUtil.getRealm(null, username).getUserStoreManager();
            return userStore.authenticate(username, password);
        } catch (Exception e) {
            log.error("Error while authenticating user", e);
            return false;
        }

    }

    public static String getOpenIDServerURL() {
        // Read from OpenID configuration in identity.xml
        String openIDServerURL = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_SERVER_URL);
        // If configuration are not defined,  build URL from server configurations.
        if (StringUtils.isBlank(openIDServerURL)) {
            openIDServerURL = IdentityUtil.getServerURL(OpenIDServerConstants.OPENID_SERVER, true);
        }
        return openIDServerURL;
    }

    public static String getOpenIDUserPattern() {
        // Read from OpenID configuration in identity.xml
        String openIDUserPattern = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_USER_PATTERN);
        // If configuration are not defined,  build URL from server configurations.
        if (StringUtils.isBlank(openIDUserPattern)) {
            openIDUserPattern = IdentityUtil.getServerURL(OpenIDServerConstants.OPENID, true);
        }
        return openIDUserPattern;
    }
}