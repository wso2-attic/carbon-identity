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

package org.wso2.carbon.identity.certificateauthority.data;

import java.security.cert.X509Certificate;
import java.util.Date;

public class CertData {
    private String SerialNo;
    private X509Certificate[] certificateChain;
    private Date issuedDate;
    private Date expiaryDate;

    public CertData(String serialNo, X509Certificate[] certificateChain, Date issuedDate, Date expiaryDate) {
        SerialNo = serialNo;
        this.certificateChain = certificateChain;
        this.issuedDate = issuedDate;
        this.expiaryDate = expiaryDate;
    }

    public String getSerialNo() {
        return SerialNo;
    }

    public X509Certificate[] getCertificateChain() {
        return certificateChain;
    }

    public Date getIssuedDate() {
        return issuedDate;
    }

    public Date getExpiaryDate() {
        return expiaryDate;
    }
}
