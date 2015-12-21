/*
 *
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.sso.saml.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO;
import org.wso2.carbon.ui.util.CharacterEncoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Util {

    private static final Set<Character> UNRESERVED_CHARACTERS = new HashSet<Character>();
    private static final Log log = LogFactory.getLog(Util.class);
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
    private static int singleLogoutRetryCount = 5;
    private static long singleLogoutRetryInterval = 60000;

    private Util() {
    }

    public static int getSingleLogoutRetryCount() {
        return singleLogoutRetryCount;
    }

    public static void setSingleLogoutRetryCount(int singleLogoutRetryCount) {
        Util.singleLogoutRetryCount = singleLogoutRetryCount;
    }

    public static long getSingleLogoutRetryInterval() {
        return singleLogoutRetryInterval;
    }

    public static void setSingleLogoutRetryInterval(long singleLogoutRetryInterval) {
        Util.singleLogoutRetryInterval = singleLogoutRetryInterval;
    }

    /**
     * This check if the status code is 2XX, check value between 200 and 300
     *
     * @param status
     * @return
     */
    public static boolean isHttpSuccessStatusCode(int status) {
        return status >= 200 && status < 300;
    }

    public static SAMLSSOServiceProviderDTO[] doPaging(int pageNumber,
                                                       SAMLSSOServiceProviderDTO[] serviceProviderSet) {

        int itemsPerPageInt = SAMLSSOProviderConstants.DEFAULT_ITEMS_PER_PAGE;
        SAMLSSOServiceProviderDTO[] returnedServiceProviderSet;

        int startIndex = pageNumber * itemsPerPageInt;
        int endIndex = (pageNumber + 1) * itemsPerPageInt;
        if (serviceProviderSet.length > itemsPerPageInt) {

            returnedServiceProviderSet = new SAMLSSOServiceProviderDTO[itemsPerPageInt];
        } else {
            returnedServiceProviderSet = new SAMLSSOServiceProviderDTO[serviceProviderSet.length];
        }

        for (int i = startIndex, j = 0; i < endIndex && i < serviceProviderSet.length; i++, j++) {
            returnedServiceProviderSet[j] = serviceProviderSet[i];
        }

        return returnedServiceProviderSet;
    }

    public static SAMLSSOServiceProviderDTO[] doFilter(String filter,
                                                       SAMLSSOServiceProviderDTO[] serviceProviderSet) {
        String regPattern = filter.replace("*", ".*");
        List<SAMLSSOServiceProviderDTO> list = new ArrayList<SAMLSSOServiceProviderDTO>();
        for (SAMLSSOServiceProviderDTO serviceProvider : serviceProviderSet) {
            if (serviceProvider.getIssuer().toLowerCase().matches(regPattern.toLowerCase())) {
                list.add(serviceProvider);
            }
        }
        SAMLSSOServiceProviderDTO[] filteredProviders = new SAMLSSOServiceProviderDTO[list.size()];
        for (int i = 0; i < list.size(); i++) {
            filteredProviders[i] = list.get(i);

        }

        return filteredProviders;
    }

    public static String getUserNameFromOpenID(String openid) throws IdentityException {
        String caller = null;
        String path = null;
        URI uri = null;
        String contextPath = "/openid/";

        try {
            uri = new URI(openid);
            path = uri.getPath();
        } catch (URISyntaxException e) {
            throw IdentityException.error("Invalid OpenID", e);
        }
        caller = path.substring(path.indexOf(contextPath) + contextPath.length(), path.length());
        return caller;
    }

    /**
     * Find the OpenID corresponding to the given user name.
     *
     * @param userName User name
     * @return OpenID corresponding the given user name.
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    public static String getOpenID(String userName) throws IdentityException {
        return generateOpenID(userName);
    }

    /**
     * Generate OpenID for a given user.
     *
     * @param user User
     * @return Generated OpenID
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    public static String generateOpenID(String user) throws IdentityException {
        String openIDUserUrl = null;
        String openID = null;
        URI uri = null;
        URL url = null;
        openIDUserUrl = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_USER_PATTERN);
        user = normalizeUrlEncoding(user);
        openID = openIDUserUrl + user;
        try {
            uri = new URI(openID);
        } catch (URISyntaxException e) {
            throw IdentityException.error("Invalid OpenID URL :" + openID, e);
        }
        try {
            url = uri.normalize().toURL();
            if (url.getQuery() != null || url.getRef() != null) {
                throw IdentityException.error("Invalid user name for OpenID :" + openID);
            }
        } catch (MalformedURLException e) {
            throw IdentityException.error("Malformed OpenID URL :" + openID, e);
        }
        openID = url.toString();
        return openID;
    }

    private static String normalizeUrlEncoding(String text) {

        if (text == null)
            return null;

        int len = text.length();
        StringBuilder normalized = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char current = text.charAt(i);
            if (current == '%' && i < len - 2) {
                String percentCode = text.substring(i, i + 3).toUpperCase();
                try {
                    String str = URLDecoder.decode(percentCode, "ISO-8859-1");
                    char chr = str.charAt(0);
                    if (UNRESERVED_CHARACTERS.contains(Character.valueOf(chr)))
                        normalized.append(chr);
                    else
                        normalized.append(percentCode);
                } catch (UnsupportedEncodingException e) {
                    if(log.isDebugEnabled()){
                        log.debug("Url Encoding not supported.", e);
                    }
                    normalized.append(percentCode);
                }
                i += 2;
            } else {
                normalized.append(current);
            }
        }
        return normalized.toString();
    }

}
