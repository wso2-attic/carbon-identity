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

package org.wso2.carbon.identity.certificateauthority;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.wso2.carbon.identity.certificateauthority.dao.CertificateDAO;
import org.wso2.carbon.identity.certificateauthority.dao.CsrDAO;
import org.wso2.carbon.identity.certificateauthority.data.Certificate;
import org.wso2.carbon.identity.certificateauthority.data.Csr;
import org.wso2.carbon.identity.certificateauthority.data.CsrStatus;
import org.wso2.carbon.identity.certificateauthority.utils.CAUtils;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class ScepServices {

    private CsrDAO csrDAO = new CsrDAO();
    private CertificateDAO certDao = new CertificateDAO();

    public void addCsr(PKCS10CertificationRequest certReq, String transId, int tenantId)
            throws CaException {
        csrDAO.addCsrFromScep(certReq, transId, tenantId);
    }

    public X509Certificate getCertificate(int tenantId, String transactionId)
            throws CaException {
        Csr csr = csrDAO.getCsrWithTransactionId(transactionId);
        if (CsrStatus.SIGNED.toString().equals(csr.getStatus())) {
            Certificate cert = certDao.getCertificate(csr.getSerialNo());
            if (cert != null) {
                return cert.getPublicCertificate();
            }
        }
        return null;
    }

    public X509Certificate getCertificate(String serialNo) throws CaException {
        Certificate cert = certDao.getCertificate(serialNo);
        if (cert != null) {
            return cert.getPublicCertificate();
        }
        return null;
    }

    public X509Certificate getCaCert(int tenantId) {
        return CAUtils.getConfiguredCaCert(tenantId);
    }

    public PrivateKey getCaKey(int tenantId) {
        return CAUtils.getConfiguredPrivateKey();
    }


}
