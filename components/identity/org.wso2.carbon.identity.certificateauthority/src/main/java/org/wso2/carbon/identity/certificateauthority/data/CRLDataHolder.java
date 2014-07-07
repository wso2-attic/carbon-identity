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

import com.hazelcast.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.certificateauthority.crl.CrlTools;

import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.util.Date;

public class CRLDataHolder {

    Log log = LogFactory.getLog(CRLDataHolder.class);
    Date thisUpdate;
    Date nextUpdate;
    String base64Crl;
    int tenantID;
    int crlNumber;
    int deltaCrlIndicator;

    public CRLDataHolder(Date thisUpdate, Date nextUpdate, String base64Crl, int tenantID, int crlNumber, int deltaCrlIndicator) {
        this.thisUpdate = thisUpdate;
        this.nextUpdate = nextUpdate;
        this.base64Crl = base64Crl;
        this.tenantID = tenantID;
        this.crlNumber = crlNumber;
        this.deltaCrlIndicator = deltaCrlIndicator;
    }

    public int getCrlNumber() {
        return crlNumber;
    }

    public void setCrlNumber(int crlNumber) {
        this.crlNumber = crlNumber;
    }

    public int getDeltaCrlIndicator() {
        return deltaCrlIndicator;
    }

    public void setDeltaCrlIndicator(int deltaCrlIndicator) {
        this.deltaCrlIndicator = deltaCrlIndicator;
    }

    public int getTenantID() {
        return tenantID;
    }

    public void setTenantID(int tenantID) {
        this.tenantID = tenantID;
    }

    public Date getNextUpdate() {
        return nextUpdate;
    }

    public void setNextUpdate(Date nextUpdate) {
        this.nextUpdate = nextUpdate;
    }

    public Date getThisUpdate() {
        return thisUpdate;
    }

    public void setThisUpdate(Date thisUpdate) {
        this.thisUpdate = thisUpdate;
    }

    public String getBase64Crl() {
        return base64Crl;
    }

    public void setBase64Crl(String base64Crl) {
        this.base64Crl = base64Crl;
    }

    /**
     * get X509CRL
     *
     * @return X509CRL
     * @throws CertificateException
     */
    public X509CRL getCRL() throws CertificateException {
        X509CRL crl = null;
        try {
            String b64Crl = getBase64Crl();
            crl = CrlTools.getCRLfromByteArray(Base64.decode(b64Crl.getBytes()));
        } catch (CRLException ce) {
            log.error("Can't decode CRL.", ce);
            return null;
        }
        return crl;
    }

    /**
     * set x509CRL
     *
     * @param incrl
     */
    public void setCRL(X509CRL incrl) {
        try {
            String b64Crl = new String(Base64.encode((incrl).getEncoded()));
            setBase64Crl(b64Crl);
        } catch (CRLException ce) {
            log.error("Can't extract DER encoded CRL.", ce);
        }
    }

    public byte[] getCRLBytes() {
        byte[] crl = null;
        String b64Crl = getBase64Crl();
        crl = Base64.decode(b64Crl.getBytes());
        return crl;
    }
}
