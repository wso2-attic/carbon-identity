/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

/**
 *
 */
public class OpenIDRelyingPartyData {

    private static Log log = LogFactory.getLog(OpenIDRelyingPartyData.class);
    private String opValidationPolicy;
    private KeyStore opKeyStore;
    private String mappedHostName;
    private String mappedPortNumber;
    private String mappingHostName;
    private String mappingPortNumber;

    /**
     * {@inheritDoc}
     */
    public void loadData(FilterConfig filterConfig) throws ServletException {

        loadOpenIDHostMapping(filterConfig);

        opValidationPolicy = filterConfig
                .getInitParameter(TokenVerifierConstants.OP_VALIDATION_POLICY);

        if (opValidationPolicy != null
                && (opValidationPolicy.equals(TokenVerifierConstants.WHITE_LIST) || opValidationPolicy
                .equals(TokenVerifierConstants.BLACK_LIST))) {

            String opStoreFilePath = filterConfig
                    .getInitParameter(TokenVerifierConstants.OP_KEY_STORE);
            String opStorePass = filterConfig
                    .getInitParameter(TokenVerifierConstants.OP_STORE_PASS);
            String opStoreType = filterConfig
                    .getInitParameter(TokenVerifierConstants.OP_STORE_TYPE);
            FileInputStream inputStream = null;
            String realPath = null;

            try {
                opKeyStore = KeyStore.getInstance(opStoreType);
                realPath = filterConfig.getServletContext().getRealPath(opStoreFilePath);
                inputStream = new FileInputStream(realPath);
                opKeyStore.load(inputStream, opStorePass.toCharArray());
            } catch (Exception e) {
                throw new ServletException("Cannot load OP key store" + opStoreFilePath + " and "
                        + opStorePass, e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error("Error while closing in inputstream" + realPath, e);
                    }
                }
            }
        }
    }

    /**
     * When the RP hosted Tomcat is behind an Apache serever, OpenID verification fails since
     * return_to url mismatches with the returning url. To avoid that - only when the Tomcat is
     * behind an Apache frontend we need to provide a host/port mapping in the web.xml.
     *
     * @param filterConfig Filter configurations
     */
    protected void loadOpenIDHostMapping(FilterConfig filterConfig) {
        mappedHostName = filterConfig.getInitParameter(TokenVerifierConstants.MAPPED_HOST);
        mappedPortNumber = filterConfig.getInitParameter(TokenVerifierConstants.MAPPED_PORT);
        mappingHostName = filterConfig.getInitParameter(TokenVerifierConstants.MAPPING_HOST);
        mappingPortNumber = filterConfig.getInitParameter(TokenVerifierConstants.MAPPING_PORT);
    }

    public String getOpValidationPolicy() {
        return opValidationPolicy;
    }

    public void setOpValidationPolicy(String opValidationPolicy) {
        this.opValidationPolicy = opValidationPolicy;
    }

    public KeyStore getOpKeyStore() {
        return opKeyStore;
    }

    public void setOpKeyStore(KeyStore opKeyStore) {
        this.opKeyStore = opKeyStore;
    }

    public String getMappedHostName() {
        return mappedHostName;
    }

    public void setMappedHostName(String mappedHostName) {
        this.mappedHostName = mappedHostName;
    }

    public String getMappedPortNumber() {
        return mappedPortNumber;
    }

    public void setMappedPortNumber(String mappedPortNumber) {
        this.mappedPortNumber = mappedPortNumber;
    }

    public String getMappingHostName() {
        return mappingHostName;
    }

    public void setMappingHostName(String mappingHostName) {
        this.mappingHostName = mappingHostName;
    }

    public String getMappingPortNumber() {
        return mappingPortNumber;
    }

    public void setMappingPortNumber(String mappingPortNumber) {
        this.mappingPortNumber = mappingPortNumber;
    }

}