/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sts.passive.processors;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasConstants;
import org.apache.rahas.RahasData;
import org.apache.rahas.TrustException;
import org.apache.rahas.TrustUtil;
import org.apache.rahas.client.STSClient;
import org.apache.rahas.impl.SAMLTokenIssuerConfig;
import org.apache.rahas.impl.TokenIssuerUtil;
import org.apache.ws.secpolicy.Constants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.core.util.KeyStoreUtil;
import org.wso2.carbon.identity.provider.AttributeCallbackHandler;
import org.wso2.carbon.identity.sts.passive.RequestToken;
import org.wso2.carbon.identity.sts.passive.ResponseToken;
import org.wso2.carbon.identity.sts.passive.internal.IdentityPassiveSTSServiceComponent;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.security.keystore.KeyStoreAdmin;
import org.wso2.carbon.security.keystore.service.KeyStoreData;
import org.wso2.carbon.security.util.RampartConfigUtil;
import org.wso2.carbon.security.util.ServerCrypto;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

public abstract class RequestProcessor {

    protected RahasData rahasData = null;

    private static final Log log = LogFactory.getLog(RequestProcessor.class);

    protected OMElement getRST(String appliesTo, String attrs, String dialect) throws Exception {
        OMFactory factory = null;
        OMElement element = null;
        OMElement claims = null;
        STSClient client = null;
        String[] attributes = null;
        String requestType;

        if(dialect == null){
            dialect = UserCoreConstants.DEFAULT_CARBON_DIALECT;
        }

        if (attrs != null) {
            attributes = attrs.split(",");
        }

        requestType = TrustUtil.getWSTNamespaceForRSTRequestTye(RahasConstants.VERSION_05_12)
                      + RahasConstants.REQ_TYPE_ISSUE;

        factory = OMAbstractFactory.getOMFactory();
        element = factory.createOMElement(Constants.RST_TEMPLATE);
        TrustUtil.createTokenTypeElement(RahasConstants.VERSION_05_12, element).setText(
                RahasConstants.TOK_TYPE_SAML_10);
        TrustUtil.createKeyTypeElement(RahasConstants.VERSION_05_12, element,
                                       RahasConstants.KEY_TYPE_SYMM_KEY);
        TrustUtil.createKeySizeElement(RahasConstants.VERSION_05_12, element, 256);

        if (attributes != null && attributes.length > 0) {
            claims = TrustUtil.createClaims(RahasConstants.VERSION_05_12, element,
                                            dialect);
            for (int i = 0; i < attributes.length; i++) {
                addClaimType(claims, attributes[i]);
            }
        }

        client = new STSClient(MessageContext.getCurrentMessageContext().getConfigurationContext());
        client.setVersion(RahasConstants.VERSION_05_12);
        client.setRstTemplate(element);
        return client.createIssueRequest(requestType, appliesTo);
    }

    public abstract ResponseToken process(RequestToken request) throws TrustException;

    private void addClaimType(OMElement parent, String uri) {
        OMElement element = null;
        element = parent.getOMFactory().createOMElement(
                new QName("http://schemas.xmlsoap.org/ws/2005/05/identity", "ClaimType", "wsid"),
                parent);
        element.addAttribute(parent.getOMFactory().createOMAttribute("Uri", null, uri));
    }

    protected SAMLTokenIssuerConfig getSAMLTokenIssuerConfig(AxisService service, boolean isSuperTenant) throws Exception {
        UserRegistry systemRegistry = null;
        String keyAlias = null;
        String keyPassword = null;
        KeyStoreAdmin admin = null;
        KeyStoreData[] keystores = null;
        String privateKeyAlias = null;
        String keyStoreName = null;
        String issuerName = null;
        ServerConfiguration serverConfig = null;

        systemRegistry = (UserRegistry) IdentityPassiveSTSServiceComponent.getGovernanceSystemRegistry();

        if (systemRegistry == null || service == null) {
            if (log.isDebugEnabled()) {
                log.debug("systemRegistry not set or STS service is unavialable");
            }
            return null;
        }

        serverConfig = ServerConfiguration.getInstance();
        keyAlias = serverConfig.getFirstProperty("Security.KeyStore.KeyAlias");
        keyPassword = serverConfig.getFirstProperty("Security.KeyStore.KeyPassword");
        issuerName = serverConfig.getFirstProperty("HostName");

        if (issuerName == null) {
            // HostName not set :-( use wso2wsas-sts
            issuerName = "Identity-passive-sts";
        }

        admin = new KeyStoreAdmin(MultitenantConstants.SUPER_TENANT_ID, systemRegistry);
        keystores = admin.getKeyStores(isSuperTenant);

        for (int i = 0; i < keystores.length; i++) {
            if (KeyStoreUtil.isPrimaryStore(keystores[i].getKeyStoreName())) {
                keyStoreName = keystores[i].getKeyStoreName();
                privateKeyAlias = KeyStoreUtil.getPrivateKeyAlias(KeyStoreManager.getInstance(
                        MultitenantConstants.SUPER_TENANT_ID).getKeyStore(keyStoreName));
                break;
            }
        }

        if (privateKeyAlias != null) {

            String cryptoProvider = ServerCrypto.class.getName();

            Properties props = RampartConfigUtil.getServerCryptoProperties(
                    new String[]{keyStoreName}, keyStoreName, privateKeyAlias);

            SAMLTokenIssuerConfig stsSamlConfig = new SAMLTokenIssuerConfig(issuerName,
                                                                            cryptoProvider, props);
            stsSamlConfig.setIssuerKeyAlias(keyAlias);
            stsSamlConfig.setIssuerKeyPassword(keyPassword);
            stsSamlConfig.setAddRequestedAttachedRef(true);
            stsSamlConfig.setAddRequestedUnattachedRef(true);
            stsSamlConfig.setKeyComputation(2);
            stsSamlConfig.setProofKeyType(TokenIssuerUtil.BINARY_SECRET);
            stsSamlConfig.setCallbackHandlerName(AttributeCallbackHandler.class.getName());

            String resourcePath = null;
            resourcePath = RegistryResources.SERVICE_GROUPS + ServerConstants.STS_NAME
                           + RegistryResources.SERVICES + ServerConstants.STS_NAME + "/trustedServices";
            if (systemRegistry.resourceExists(resourcePath)) {
                Resource trustedService = null;
                Properties properties = null;
                Iterator iterator = null;
                trustedService = systemRegistry.get(resourcePath);
                properties = trustedService.getProperties();
                if (properties != null && !properties.isEmpty()) {
                    iterator = properties.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Entry entry = (Entry) iterator.next();
                        if(RegistryUtils.isHiddenProperty(entry.getKey().toString())){
                            continue;
                        }
                        stsSamlConfig.addTrustedServiceEndpointAddress((String) entry.getKey(),
                                                                       (String) ((List) entry.getValue()).get(0));
                    }
                }
            }

            return stsSamlConfig;
        }
        return null;
    }
}
