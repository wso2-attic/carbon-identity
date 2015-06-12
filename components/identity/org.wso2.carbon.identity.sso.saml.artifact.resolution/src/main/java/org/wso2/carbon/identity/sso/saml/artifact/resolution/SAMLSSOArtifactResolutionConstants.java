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

/**
 * This class contains the constants relevant to SAML Artifact Resolution
 */
public class SAMLSSOArtifactResolutionConstants {

    public static final int DEFAULT_ARTIFACT_LIFETIME = 1000 * 60;

    public class Notification {

        public static final String NO_ARTIFACT_RESOLVE_STATUS = "Artifact Resolve not found in the request";
        public static final String EXCEPTION_STATUS_ARTIFACT_RESOLVE = "Error while resolving SAML artifact";
        public static final String EXCEPTION_STATUS_SOAP_RESPONSE = "Error while building soap response";
    }
}
