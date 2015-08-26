/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
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
package org.wso2.carbon.identity.webfinger;

import com.google.common.base.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.wso2.carbon.identity.webfinger.internal.WebFingerServiceComponentHolder;
import org.wso2.carbon.user.api.UserStoreException;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class URLNormalizer {


    private static final Log log = LogFactory.getLog(URLNormalizer.class);

    private static final Pattern pattern = Pattern.compile("^" +
                    "((https|acct|http|mailto|tel|device):(//)?)?" + // scheme
                    "(" +
                    "(([^@]+)@)?" + // userinfo
                    "(([^\\?#:/]+)" + // host
                    "(:(\\d*))?)" + // port
                    ")" +
                    "([^\\?#]+)?" + // path
                    "(\\?([^#]+))?" + // query
                    "(#(.*))?" +  // fragment
                    "$"
    );


    /**
     * Private constructor to prevent instantiation.
     */
    private URLNormalizer() {
    }

    public static WebFingerRequest normalizeResource(WebFingerRequest request) throws WebFingerEndPointException{
        String resource = request.getResource();
        if (Strings.isNullOrEmpty(resource)) {
            log.warn("Can't normalize null or empty URI: " + resource);
            throw new WebFingerEndPointException(WebFingerConstants.ERROR_CODE_INVALID_RESOURCE, "Null or empty URI.");

        } else {
            URI resourceURI = URI.create(resource);
            String scheme = resourceURI.getScheme();
            if(scheme.equals("acct")){
                return matchPattern(request);
            }
            request.setScheme(resourceURI.getScheme());
            request.setUserInfo(resourceURI.getUserInfo());
            request.setHost(resourceURI.getHost());
            request.setPort(resourceURI.getPort());
            request.setPath(resourceURI.getPath());
            request.setQuery(resourceURI.getQuery());
            request.setQuery(resourceURI.getFragment());
        }
        return request;
    }
    /**
     * Normalize the resource string as per OIDC Discovery.
     *
     * @param request
     * @return the WebFingerRequest, after assigning the relevant fields to the properties in the WebFingerRequest
     */
    private static WebFingerRequest matchPattern(WebFingerRequest request) throws WebFingerEndPointException {
        String identifier = request.getResource();
        if (Strings.isNullOrEmpty(identifier)) {
            log.warn("Can't normalize null or empty URI: " + identifier);
            throw new WebFingerEndPointException(WebFingerConstants.ERROR_CODE_INVALID_RESOURCE, "Null or empty URI.");

        } else {

            Matcher m = pattern.matcher(identifier);
            if (m.matches()) {
                request.setScheme(m.group(2));
                request.setUserInfo(m.group(6));
                validateTenant(request.getUserInfo());
                request.setHost((m.group(8)));
                String port = m.group(10);
                if (!Strings.isNullOrEmpty(port)) {
                    request.setPort((Integer.parseInt(port)));
                }
                request.setPath((m.group(11)));
                request.setQuery((m.group(13)));
                request.setFragment(m.group(15)); // we throw away the hash, but this is the group it would be if we
                // kept it
            } else {
                // doesn't match the pattern, throw it out
                log.warn("Parser couldn't match input: " + identifier);
                throw new WebFingerEndPointException(WebFingerConstants.ERROR_CODE_INVALID_RESOURCE, "URI in wrong " +
                        "format.");
            }

            if (Strings.isNullOrEmpty(request.getScheme())) {
                if (!Strings.isNullOrEmpty(request.getUserInfo())
                        && Strings.isNullOrEmpty(request.getPath())
                        && Strings.isNullOrEmpty(request.getQuery())
                        && request.getPort() < 0) {
                    // scheme empty, userinfo is not empty, path/query/port are empty
                    // set to "acct" (rule 2)
                    request.setScheme("acct");

                } else {
                    // scheme is empty, but rule 2 doesn't apply
                    // set scheme to "https" (rule 3)
                    request.setScheme("https");
                }
            }

            // fragment must be stripped (rule 4)
            request.setFragment(null);

            return request;
        }


    }
    public static void validateTenant(String userInfo) throws WebFingerEndPointException {
        try {

            int tenantId = WebFingerServiceComponentHolder.getRealmService().getTenantManager().getTenantId(userInfo);
            if (tenantId < 0) {
                throw new WebFingerEndPointException(WebFingerConstants.ERROR_CODE_INVALID_TENANT, "The tenant domain" +
                        " is not valid.");
            }
        } catch (UserStoreException e) {
            throw new WebFingerEndPointException(WebFingerConstants.ERROR_CODE_INVALID_TENANT, e.getMessage());
        }
    }

    /**
     * This is to be used at the Relying party.
     * Not used in the Web Finger host.
     */
    public static WebFingerRequest serializeURL(WebFingerRequest request) {
        if (request.getScheme() != null &&
                (request.getScheme().equals("acct") ||
                        request.getScheme().equals("mailto") ||
                        request.getScheme().equals("tel") ||
                        request.getScheme().equals("device")
                )) {

            // serializer copied from HierarchicalUriComponents but with "//" removed

            StringBuilder uriBuilder = new StringBuilder();

            if (request.getScheme() != null) {
                uriBuilder.append(request.getScheme());
                uriBuilder.append(':');
            }

            if (request.getUserInfo() != null || request.getHost() != null) {
                if (request.getUserInfo() != null) {
                    uriBuilder.append(request.getUserInfo());
                    uriBuilder.append('@');
                }
                if (request.getHost() != null) {
                    uriBuilder.append(request.getHost());
                }
                if (request.getPort() != -1) {
                    uriBuilder.append(':');
                    uriBuilder.append(request.getPort());
                }
            }

            String path = request.getPath();
            if (StringUtils.hasLength(path)) {
                if (uriBuilder.length() != 0 && path.charAt(0) != '/') {
                    uriBuilder.append('/');
                }
                uriBuilder.append(path);
            }

            String query = request.getQuery();
            if (query != null) {
                uriBuilder.append('?');
                uriBuilder.append(query);
            }

            if (request.getFragment() != null) {
                uriBuilder.append('#');
                uriBuilder.append(request.getFragment());
            }

            request.setUri(uriBuilder.toString());
        }
        return request;
    }


}
