/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *    WSO2 Inc. licenses this file to you under the Apache License,
 *    Version 2.0 (the "License"); you may not use this file except
 *    in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.sso.saml.artifact.resolution.validators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.ArtifactResolve;
import org.opensaml.saml2.core.RequestAbstractType;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.registry.core.Registry;

public class ArtifactResolveValidator {

    private static Log log = LogFactory.getLog(ArtifactResolveValidator.class);

    /**
     * Process and validate the <ArtifactResolve> according to the SAML Specification
     *
     * @param artifactResolve ArtifactResolve
     * @return
     */
    public boolean validate(ArtifactResolve artifactResolve) throws IdentityException {

        if (artifactResolve.getIssuer() == null || artifactResolve.getIssuer().getValue() == null) {
            if (log.isDebugEnabled()) {
                log.debug("Validation Failed. Issuer Empty");
            }
            return false;
        } else if (!(artifactResolve.getVersion().equals(SAMLVersion.VERSION_20))) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid SAML Version : " + artifactResolve.getVersion());
            }
            return false;
        } else if (artifactResolve.getArtifact() == null) {
            if (log.isDebugEnabled()) {
                log.debug("Validation Failed. Artifact Empty");
            }
            return false;
        } else if (artifactResolve.getArtifact().getArtifact() == null) {
            if (log.isDebugEnabled()) {
                log.debug("Validation Failed. Artifact Empty");
            }
            return false;
        } else {
            String domainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
            Registry registry = (Registry) PrivilegedCarbonContext.getThreadLocalCarbonContext().getRegistry(RegistryType.SYSTEM_CONFIGURATION);
            SAMLSSOServiceProviderDO serviceProviderDO = persistenceManager.getServiceProvider(registry, artifactResolve.getIssuer().getValue());
            String alias = serviceProviderDO.getCertAlias();
            return SAMLSSOUtil.validateXMLSignature((RequestAbstractType)artifactResolve, alias, domainName);
        }
    }
}
