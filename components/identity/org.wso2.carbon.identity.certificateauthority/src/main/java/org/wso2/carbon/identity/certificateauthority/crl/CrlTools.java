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

import com.hazelcast.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;

public class CrlTools {
    static Log log = LogFactory.getLog(CrlTools.class);

    /**
     * returns a crl from a crl byte array
     *
     * @param crl byte array of the crl
     * @return x509CRL object
     * @throws CRLException
     * @throws CertificateException
     */
    public static X509CRL getCRLfromByteArray(byte[] crl) throws CRLException, CertificateException {
        log.trace(">getCRLfromByteArray");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509CRL x509crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(crl));
        log.trace("<getCRLfromByteArray");

        return x509crl;
    } // getCRLfromByteArray

    /**
     * returns a crl from base 64 encoded crl
     *
     * @param base64Crl
     * @return X509CRL object
     * @throws CertificateException
     */
    public static X509CRL getCRL(String base64Crl) throws CertificateException {
        X509CRL crl = null;
        try {
            crl = CrlTools.getCRLfromByteArray(Base64.decode(base64Crl.getBytes()));
        } catch (CRLException ce) {
            log.error("Can't decode CRL.", ce);
            return null;
        }
        return crl;
    }

}