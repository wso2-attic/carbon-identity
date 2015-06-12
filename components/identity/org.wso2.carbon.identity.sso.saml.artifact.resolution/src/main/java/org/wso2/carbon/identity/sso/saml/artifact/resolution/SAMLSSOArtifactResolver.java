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

package org.wso2.carbon.identity.sso.saml.artifact.resolution;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.Artifact;
import org.opensaml.saml2.core.ArtifactResolve;
import org.opensaml.saml2.core.ArtifactResponse;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.artifact.resolution.builders.SAMLArtifactResponseBuilder;
import org.wso2.carbon.identity.sso.saml.persistence.ArtifactStorage;
import org.wso2.carbon.identity.sso.saml.persistence.ArtifactStorageRegistry;
import org.wso2.carbon.identity.sso.saml.persistence.model.SAMLSSOArtifactResponse;

/**
 * This class is responsible for validating and processing the received Artifact Resolve request
 */
public class SAMLSSOArtifactResolver {

    private static Log log = LogFactory.getLog(SAMLSSOArtifactResolver.class);

    public ArtifactResponse resolveArtifact(ArtifactResolve artifactResolve) throws IdentityException {

        SAMLSSOArtifactResponse samlSsoArtifactResponse = null;

        if (artifactResolve.getArtifact() != null) {
            Artifact artifact = artifactResolve.getArtifact();
            String artifactStr = artifact.getArtifact();

            if (artifactStr != null) {
                ArtifactStorage artifactStorage = ArtifactStorageRegistry.getInstance().getArtifactStorage();
                samlSsoArtifactResponse = artifactStorage.retrieve(artifactStr);
                if (samlSsoArtifactResponse != null) {
                    artifactStorage.clearEntry(artifactStr);  // Ensure one time use property
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid Artifact");
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Invalid Artifact Resolve");
            }
        }

        SAMLArtifactResponseBuilder artifactResponseBuilder = new SAMLArtifactResponseBuilder();
        ArtifactResponse artifactResponse = artifactResponseBuilder.buildArtifactResponse(artifactResolve, samlSsoArtifactResponse);
        return artifactResponse;
    }
}
