/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.sso.saml.builders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Base64;
import org.opensaml.common.SAMLObject;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.persistence.ArtifactStorageRegistry;
import org.wso2.carbon.identity.sso.saml.persistence.model.SAMLSSOArtifactResponse;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SAMLArtifactBuilder {

    private static Log log = LogFactory.getLog(SAMLArtifactBuilder.class);

    /**
     * Build the artifact and store it in the Artifact Storage along with the SAML response
     * @param subject SAML request subject
     * @return the Base 64 encoded artifact string
     * @throws IdentityException
     */
    public String buildArtifact(SAMLObject response, String subject) throws IdentityException, NoSuchAlgorithmException {

        String artifact = buildSAML2Artifact();
        SAMLSSOArtifactResponse artifactResponse = new SAMLSSOArtifactResponse();
        artifactResponse.setArtifact(artifact);
        artifactResponse.setSamlMessage(SAMLSSOUtil.marshall(response));
        artifactResponse.setSubject(subject);
        ArtifactStorageRegistry.getInstance().getArtifactStorage().store(artifactResponse);
        return artifact;
    }

    /**
     * Build the SAML V2.0 Artifact type of Type Code 0x0004
     * Artifact length : 44 bytes
     *
     * SAML V2.0 defines an artifact type of type code 0x0004
     * Identification:urn:oasis:names:tc:SAML:2.0:artifact-04
     *
     * SAML_artifact := B64(TypeCode EndpointIndex RemainingArtifact)
     * TypeCode := Byte1Byte2
     * EndpointIndex := Byte1Byte2
     *
     * TypeCode := 0x0004
     * RemainingArtifact := SourceID MessageHandle
     * SourceID := 20-byte_sequence
     * MessageHandle := 20-byte_sequence
     *
     * @return SAML V2.0 Artifact type of Type Code 0x0004
     */
    private String buildSAML2Artifact() throws IdentityException, NoSuchAlgorithmException {

        if(log.isDebugEnabled()) {
            log.debug("Building Artifact");
        }
        //Endpoint Index
        byte[] endpointIndex = {0, 0};

        //Source ID
        MessageDigest sha1Digester = MessageDigest.getInstance("SHA-1");
        String issuerID = SAMLSSOUtil.getIssuer().getValue();
        byte[] sourceID = sha1Digester.digest(issuerID.getBytes());

        //MessageHandle
        SecureRandom handleGenerator = SecureRandom.getInstance("SHA1PRNG");
        byte[] messageHandle;
        messageHandle = new byte[20];
        handleGenerator.nextBytes(messageHandle);

        byte[] artifactByteArray = new byte[44];
        System.arraycopy(SAMLSSOConstants.SAML2_ARTIFACT_TYPE_CODE, 0, artifactByteArray, 0, 2);
        System.arraycopy(endpointIndex, 0, artifactByteArray, 2, 2);
        System.arraycopy(sourceID, 0, artifactByteArray, 4, 20);
        System.arraycopy(messageHandle, 0, artifactByteArray, 24, 20);
        return new String(Base64.encode(artifactByteArray));
    }
}
