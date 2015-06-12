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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.ArtifactResolve;
import org.opensaml.saml2.core.ArtifactResponse;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.artifact.resolution.exception.ArtifactResolutionException;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

import javax.xml.stream.XMLStreamException;

/**
 * This is the service class which receives the SAML Artifact Resolve Request
 * For a valid SAML Artifact Resolve request, this will respond with a valid Artifact Response
 */
public class SAMLSSOArtifactResolutionService {

    private static Log log = LogFactory.getLog(SAMLSSOArtifactResolutionService.class);

    /**
     * Receives the SOAP message containing the <ArtifactResolve>
     * @param omElement Artifact Resolve message
     * @return
     * @throws Exception
     */
    public OMElement resolveArtifact(OMElement omElement) throws ArtifactResolutionException {

        try {
            String xmlString = omElement.toString();

            if (log.isDebugEnabled()) {
                log.debug(xmlString);
            }

            ArtifactResolve artifactResolve = (ArtifactResolve)SAMLSSOUtil.unmarshall(xmlString);
            ArtifactResponse artifactResponse = new SAMLSSOArtifactResolver().resolveArtifact(artifactResolve);
            OMElement responseElement = AXIOMUtil.stringToOM(SAMLSSOUtil.marshall(artifactResponse));
            return responseElement;

        } catch (IdentityException e) {
            log.error("Error while resolving artifact", e);
            throw new ArtifactResolutionException(
                    SAMLSSOArtifactResolutionConstants.Notification.EXCEPTION_STATUS_ARTIFACT_RESOLVE);
        } catch (XMLStreamException e) {
            log.error("Error while building SOAP response", e);
            throw new ArtifactResolutionException(
                    SAMLSSOArtifactResolutionConstants.Notification.EXCEPTION_STATUS_SOAP_RESPONSE);
        } catch (ClassCastException e) {
            log.error("Error while extracting ArtifactResolve", e);
            throw new ArtifactResolutionException(
                    SAMLSSOArtifactResolutionConstants.Notification.NO_ARTIFACT_RESOLVE_STATUS);
        }
    }
}
