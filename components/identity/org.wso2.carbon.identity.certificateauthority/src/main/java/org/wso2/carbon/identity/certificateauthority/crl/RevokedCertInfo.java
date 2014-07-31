/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.identity.certificateauthority.crl;


public class RevokedCertInfo {

    public static final int NOT_REVOKED = -1;
    public static final int REVOCATION_REASON_UNSPECIFIED = 0;
    public static final int REVOCATION_REASON_KEYCOMPROMISE = 1;
    public static final int REVOCATION_REASON_CACOMPROMISE = 2;
    public static final int REVOCATION_REASON_AFFILIATIONCHANGED = 3;
    public static final int REVOCATION_REASON_SUPERSEDED = 4;
    public static final int REVOCATION_REASON_CESSATIONOFOPERATION = 5;
    public static final int REVOCATION_REASON_CERTIFICATEHOLD = 6;
    // Value 7 is not used, see RFC5280
    public static final int REVOCATION_REASON_REMOVEFROMCRL = 8;
    public static final int REVOCATION_REASON_PRIVILEGESWITHDRAWN = 9;
    public static final int REVOCATION_REASON_AACOMPROMISE = 10;
}
