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

package org.wso2.carbon.identity.sso.saml.artifact.resolution.builders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.signature.XMLSignature;
import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.ArtifactResponseBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.artifact.resolution.validators.ArtifactResolveValidator;
import org.wso2.carbon.identity.sso.saml.builders.SignKeyDataHolder;
import org.wso2.carbon.identity.sso.saml.persistence.model.SAMLSSOArtifactResponse;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

/**
 * This class is responsible for building SAML Artifact Response
 */
public class SAMLArtifactResponseBuilder {

    private static Log log = LogFactory.getLog(SAMLArtifactResponseBuilder.class);

    /**
     * Build the <ArtifactResponse> for a valid or invalid Artifact
     * In both cases;
     * <Status> element MUST include a <StatusCode> with the value urn:oasis:names:tc:SAML:2.0:status:Success
     *
     * @param artifactResolve
     * @param samlssoArtifactResponse
     * @return
     * @throws IdentityException
     */
    public ArtifactResponse buildArtifactResponse(ArtifactResolve artifactResolve, SAMLSSOArtifactResponse samlssoArtifactResponse)
            throws IdentityException {

        if (log.isDebugEnabled()) {
            log.debug("Generating the Artifact Response...");
        }

        ArtifactResponse artifactResponse = new ArtifactResponseBuilder().buildObject();
        artifactResponse.setID(SAMLSSOUtil.createID());
        artifactResponse.setInResponseTo(artifactResolve.getID());
        artifactResponse.setIssuer(SAMLSSOUtil.getIssuer());
        artifactResponse.setIssueInstant(new DateTime());
        artifactResponse.setVersion(SAMLVersion.VERSION_20);

        Status status = new StatusBuilder().buildObject();
        StatusCode statusCode = new StatusCodeBuilder().buildObject();
        statusCode.setValue(SAMLSSOConstants.StatusCodes.SUCCESS_CODE);
        status.setStatusCode(statusCode);
        artifactResponse.setStatus(status);

        boolean isValidArtifactResolve = new ArtifactResolveValidator().validate(artifactResolve);

        if (samlssoArtifactResponse != null && isValidArtifactResolve) {
            String samlResponse = samlssoArtifactResponse.getSamlMessage();
            Response unmarshalledResponse = (Response) SAMLSSOUtil.unmarshall(samlResponse);
            artifactResponse.setMessage(unmarshalledResponse);
            SAMLSSOUtil.signResponse(artifactResponse, XMLSignature.ALGO_ID_SIGNATURE_RSA,
                    new SignKeyDataHolder(samlssoArtifactResponse.getSubject()));
        }
        return artifactResponse;
    }
}
