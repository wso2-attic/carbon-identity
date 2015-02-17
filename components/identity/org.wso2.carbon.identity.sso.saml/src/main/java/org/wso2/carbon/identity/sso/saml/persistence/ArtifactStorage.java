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

package org.wso2.carbon.identity.sso.saml.persistence;

import org.wso2.carbon.identity.sso.saml.persistence.model.SAMLSSOArtifactResponse;

public interface ArtifactStorage {

    /**
     * Get priority for Artifact Storage
     * @return
     */
    public int getPriority();

    /**
     * Set priority for Artifact Storage
     * @param priority
     */
    public void setPriority(int priority);

    /**
     * Store the artifact and SAMLSSOArtifactResponse in the Artifact Storage
     * @param artifactResponse
     */
    public void store (SAMLSSOArtifactResponse artifactResponse);

    /**
     * Retrieve the SAMLSSOArtifactResponse from the Artifact Storage
     * @param artifact
     * @return
     */
    public SAMLSSOArtifactResponse retrieve(String artifact);

    /**
     * Clear the entry corresponding to the artifact
     * To ensure One-Time-Use property of the artifact
     * @param artifact
     */
    public void clearEntry(String artifact);
}
