/*
 * Copyright (c) 2007, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sts.mgt.admin;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.impl.SAMLTokenIssuerConfig;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.provider.AttributeCallbackHandler;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.provider.IdentityProviderUtil;
import org.wso2.carbon.identity.sts.mgt.IPPasswordCallbackHandler;
import org.wso2.carbon.identity.sts.mgt.STSMgtConstants;
import org.wso2.carbon.identity.sts.mgt.internal.IdentitySTSMgtServiceComponent;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.security.config.SecurityConfigAdmin;
import org.wso2.carbon.security.config.SecurityServiceAdmin;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.ServerException;

import javax.security.auth.callback.CallbackHandler;

/**
 * This will allow setting the SecurityTokenService security configuration
 */
public class STSConfigAdmin {

    private static final Log log = LogFactory.getLog(STSConfigAdmin.class);

    private STSConfigAdmin() {
    }

    public static void configureService(String serviceName) throws IdentityProviderException {
        try {
            AxisConfiguration axisConfig = IdentitySTSMgtServiceComponent.getConfigurationContext()
                                                                         .getAxisConfiguration();

            SecurityConfigAdmin admin = new SecurityConfigAdmin(axisConfig,
                                                                IdentitySTSMgtServiceComponent.getRegistryService()
                                                                                              .getConfigSystemRegistry(),
                                                                new IPPasswordCallbackHandler());

            ServerConfiguration serverConfig = ServerConfiguration.getInstance();
            String ksName =
                    serverConfig.getFirstProperty(STSMgtConstants.ServerConfigProperty.SECURITY_KEYSTORE_LOCATION);
            ksName = ksName.substring(ksName.lastIndexOf("/") + 1);

            if (log.isDebugEnabled()) {
                log.debug("Applying identity security policy for service " + serviceName);
            }

            if (IdentityProviderUtil.isIntial()) {

                if (IdentityConstants.SERVICE_NAME_STS_UT.equals(serviceName)) {
                    admin.applySecurity(IdentityConstants.SERVICE_NAME_STS_UT, STSMgtConstants.Policy.POLICY_SCENARIO19,
                                        null, null, null, null);
                } else if (IdentityConstants.OpenId.SERVICE_NAME_STS_OPENID.equals(serviceName)) {
                    admin.applySecurity(IdentityConstants.OpenId.SERVICE_NAME_STS_OPENID,
                                        STSMgtConstants.Policy.POLICY_SCENARIO19, null, null, null, null);
                } else if (IdentityConstants.SERVICE_NAME_STS_IC.equals(serviceName)) {
                    admin.applySecurity(IdentityConstants.SERVICE_NAME_STS_IC, STSMgtConstants.Policy.POLICY_SCENARIO18,
                                        null, new String[] { ksName }, ksName, null);
                } else if (IdentityConstants.OpenId.SERVICE_NAME_STS_IC_OPENID.equals(serviceName)) {
                    admin.applySecurity(IdentityConstants.OpenId.SERVICE_NAME_STS_IC_OPENID,
                                        STSMgtConstants.Policy.POLICY_SCENARIO18, null, new String[] { ksName }, ksName,
                                        null);
                } else if (IdentityConstants.SERVICE_NAME_STS_UT_SYMM.equals(serviceName)) {
                    admin.applySecurity(IdentityConstants.SERVICE_NAME_STS_UT_SYMM,
                                        STSMgtConstants.Policy.POLICY_SCENARIO18, null, new String[] { ksName }, ksName,
                                        null);
                } else if (IdentityConstants.SERVICE_NAME_STS_IC_SYMM.equals(serviceName)) {
                    admin.applySecurity(IdentityConstants.SERVICE_NAME_STS_IC_SYMM,
                                        STSMgtConstants.Policy.POLICY_SCENARIO18, null, new String[] { ksName }, ksName,
                                        null);
                }
            }

            if (IdentityConstants.SERVICE_NAME_STS_UT.equals(serviceName)) {
                overrideCallbackHandler(axisConfig, IdentityConstants.SERVICE_NAME_STS_UT);
            } else if (IdentityConstants.SERVICE_NAME_STS_UT_SYMM.equals(serviceName)) {
                overrideCallbackHandler(axisConfig, IdentityConstants.SERVICE_NAME_STS_UT_SYMM);
            } else if (IdentityConstants.OpenId.SERVICE_NAME_STS_OPENID.equals(serviceName)) {
                overrideCallbackHandler(axisConfig, IdentityConstants.OpenId.SERVICE_NAME_STS_OPENID);
            } else if (IdentityConstants.SERVICE_NAME_STS_IC.equals(serviceName)) {
                overrideCallbackHandler(axisConfig, IdentityConstants.SERVICE_NAME_STS_IC);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("errorInChangingSecurityConfiguration", e);
            throw new IdentityProviderException("errorInChangingSecurityConfiguration", e);
        }

    }

    /**
     * Configures the STS service for the super tenant.
     *
     * @throws IdentityProviderException
     */
    public static void configureGenericSTS() throws IdentityProviderException {
        AxisConfiguration config = IdentitySTSMgtServiceComponent.getConfigurationContext().getAxisConfiguration();
        configureGenericSTS(config);
    }

    /**
     * Configures the STS service. STS service of different tenants can be configured by providing the AxisConfiguration instances
     * of corresponding tenants.
     *
     * @param config AxisConfiguration instance of the tenant.
     * @throws IdentityProviderException
     */
    public static void configureGenericSTS(AxisConfiguration config) throws IdentityProviderException {
        try {
            AxisService stsService = config.getService(ServerConstants.STS_NAME);
            if (stsService == null) {
                return;
            }
            Parameter origParam = stsService.getParameter(SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG.getLocalPart());
            if (origParam != null) {
                OMElement samlConfigElem = origParam.getParameterElement().getFirstChildWithName(
                        SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG);
                SAMLTokenIssuerConfig samlConfig = new SAMLTokenIssuerConfig(samlConfigElem);
                samlConfig.setCallbackHandlerName(AttributeCallbackHandler.class.getName());
                if (log.isDebugEnabled()) {
                    log.debug("Configured the SAML callback handler: " + AttributeCallbackHandler.class.getName() +
                              " in the service " + stsService.getName() + " for tenant " +
                              PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain() +
                              "[" + PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId() + "]");
                }

                ServerConfiguration serverConfig = ServerConfiguration.getInstance();
                String ttl = serverConfig.getFirstProperty(STSMgtConstants.ServerConfigProperty.STS_TIME_TO_LIVE);

                if (StringUtils.isNotBlank(ttl)) {
                    try {
                        samlConfig.setTtl(Long.parseLong(ttl));
                        if (log.isDebugEnabled()) {
                            log.debug("STSTimeToLive read from carbon.xml " + ttl);
                        }
                    } catch (NumberFormatException e) {
                        log.error("Error while reading STSTimeToLive from carbon.xml", e);
                    }
                }

                setSTSParameter(samlConfig, config);
            }
        } catch (Exception e) {
            log.error("Error while setting password callback to the STS", e);
            throw new IdentityProviderException(e.getMessage(), e);
        }
    }

    public static void configureService(AxisConfiguration config, Registry registry)
            throws IdentityProviderException {
        AxisConfiguration axisConfig = IdentitySTSMgtServiceComponent.getConfigurationContext().getAxisConfiguration();

        try {
            ServerConfiguration serverConfig = ServerConfiguration.getInstance();
            String ksName =
                    serverConfig.getFirstProperty(STSMgtConstants.ServerConfigProperty.SECURITY_KEYSTORE_LOCATION);
            ksName = ksName.substring(ksName.lastIndexOf("/") + 1);

            SecurityConfigAdmin admin = new SecurityConfigAdmin(config, registry, new IPPasswordCallbackHandler());
            if (log.isDebugEnabled()) {
                log.debug("Applying identity security policy for Identity STS services");
            }

            if (IdentityProviderUtil.isIntial()) {
                if (axisConfig.getService(IdentityConstants.SERVICE_NAME_STS_UT) != null) {
                    admin.applySecurity(IdentityConstants.SERVICE_NAME_STS_UT, STSMgtConstants.Policy.POLICY_SCENARIO19,
                                        null, null, null, null);
                }
                if (axisConfig.getService(IdentityConstants.OpenId.SERVICE_NAME_STS_OPENID) != null) {
                    admin.applySecurity(IdentityConstants.OpenId.SERVICE_NAME_STS_OPENID,
                                        STSMgtConstants.Policy.POLICY_SCENARIO19, null, null, null, null);
                }
                if (axisConfig.getService(IdentityConstants.SERVICE_NAME_STS_IC) != null) {
                    admin.applySecurity(IdentityConstants.SERVICE_NAME_STS_IC, STSMgtConstants.Policy.POLICY_SCENARIO18,
                                        null, new String[] { ksName }, ksName, null);
                }
                if (axisConfig.getService(IdentityConstants.OpenId.SERVICE_NAME_STS_IC_OPENID) != null) {
                    admin.applySecurity(IdentityConstants.OpenId.SERVICE_NAME_STS_IC_OPENID,
                                        STSMgtConstants.Policy.POLICY_SCENARIO18, null, new String[] { ksName }, ksName,
                                        null);
                }
                if (axisConfig.getService(IdentityConstants.SERVICE_NAME_STS_UT_SYMM) != null) {
                    admin.applySecurity(IdentityConstants.SERVICE_NAME_STS_UT_SYMM,
                                        STSMgtConstants.Policy.POLICY_SCENARIO18, null, new String[] { ksName }, ksName,
                                        null);
                }
                if (axisConfig.getService(IdentityConstants.SERVICE_NAME_STS_IC_SYMM) != null) {
                    admin.applySecurity(IdentityConstants.SERVICE_NAME_STS_IC_SYMM,
                                        STSMgtConstants.Policy.POLICY_SCENARIO18, null, new String[] { ksName }, ksName,
                                        null);
                }
            }

            if (axisConfig.getService(IdentityConstants.SERVICE_NAME_STS_UT) != null) {
                overrideCallbackHandler(axisConfig, IdentityConstants.SERVICE_NAME_STS_UT);
            }
            if (axisConfig.getService(IdentityConstants.SERVICE_NAME_STS_UT_SYMM) != null) {
                overrideCallbackHandler(axisConfig, IdentityConstants.SERVICE_NAME_STS_UT_SYMM);
            }
            if (axisConfig.getService(IdentityConstants.OpenId.SERVICE_NAME_STS_OPENID) != null) {
                overrideCallbackHandler(axisConfig, IdentityConstants.OpenId.SERVICE_NAME_STS_OPENID);
            }
            if (axisConfig.getService(IdentityConstants.SERVICE_NAME_STS_IC) != null) {
                overrideCallbackHandler(axisConfig, IdentityConstants.SERVICE_NAME_STS_IC);
            }

        } catch (Exception e) {
            log.error("errorInChangingSecurityConfiguration", e);
            throw new IdentityProviderException("errorInChangingSecurityConfiguration", e);
        }

    }

    /**
     * Override WSAS callback handler to be able to auth users with usermanager.
     *
     * @param axisConfig
     * @throws AxisFault
     */
    public static void overrideCallbackHandler(AxisConfiguration axisConfig, String service) throws AxisFault {
        AxisService sts = axisConfig.getService(service);
        Parameter cbHandlerParam = sts.getParameter(WSHandlerConstants.PW_CALLBACK_REF);
        if (cbHandlerParam != null) {
            sts.removeParameter(cbHandlerParam);
            if (log.isDebugEnabled()) {
                log.debug("removedParameter");
            }
        }

        Parameter param = getPasswordCallBackRefParameter();

        sts.addParameter(param);

        if (log.isDebugEnabled()) {
            log.debug("addedParameter");
        }
    }

    public static void overrideCallbackHandler(AxisService service) throws AxisFault {
        Parameter cbHandlerParam = service.getParameter(WSHandlerConstants.PW_CALLBACK_REF);
        CallbackHandler handler = null;

        if (cbHandlerParam != null) {
            handler = (CallbackHandler) cbHandlerParam;
            service.removeParameter(cbHandlerParam);
            if (log.isDebugEnabled()) {
                log.debug("removedParameter");
            }
        }

        CallbackHandler cb = null;
        if (handler != null) {
            cb = handler;
        } else {
            cb = new IPPasswordCallbackHandler();
        }

        Parameter param = new Parameter();
        param.setName(WSHandlerConstants.PW_CALLBACK_REF);
        param.setValue(cb);
        service.addParameter(param);

        if (log.isDebugEnabled()) {
            log.debug("addedParameter");
        }
    }

    public static Parameter getPasswordCallBackRefParameter() throws AxisFault {
        Parameter param = new Parameter();
        param.setName(WSHandlerConstants.PW_CALLBACK_REF);
        try {
            param.setValue(new IPPasswordCallbackHandler());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AxisFault(e.getMessage(), e);
        }
        return param;
    }

    private static void setSTSParameter(SAMLTokenIssuerConfig samlConfig, AxisConfiguration config)
            throws AxisFault, ServerException {
        try {
            new SecurityServiceAdmin(config)
                    .setServiceParameterElement(ServerConstants.STS_NAME, samlConfig.getParameter());
        } catch (ServerException e) {
            throw new AxisFault("Error configuring STS parameters.", e);
        }

    }

}
