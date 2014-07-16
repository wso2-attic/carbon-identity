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

public class Certificate {
    private CertificateMetaInfo certificateMetaInfo;
    private X509Certificate publicCertificate;
    private int tenantID;
    private String userStoreDomain;

    public Certificate(String serialNo, X509Certificate publicCertificate, String status, int tenantID, String username, Date issuedDate, Date expiaryDate, String userStoreDomain) {
        this.certificateMetaInfo = new CertificateMetaInfo(serialNo, issuedDate, expiaryDate, username, status);
        this.publicCertificate = publicCertificate;
        this.tenantID = tenantID;
        this.userStoreDomain = userStoreDomain;
    }

    public Date getExpiryDate() {
        return certificateMetaInfo.getExpiryDate();
    }

    public void setExpiryDate(Date expiryDate) {
        certificateMetaInfo.setExpiryDate(expiryDate);
    }

    public String getUserStoreDomain() {
        return userStoreDomain;
    }

    public void setUserStoreDomain(String userStoreDomain) {
        this.userStoreDomain = userStoreDomain;
    }

    public Date getIssuedDate() {
        return certificateMetaInfo.getIssuedDate();
    }

    public void setIssuedDate(Date issuedDate) {
        certificateMetaInfo.getIssuedDate();
    }

    public X509Certificate getPublicCertificate() {
        return publicCertificate;
    }

    public void setPublicCertificate(X509Certificate publicCertificate) {
        this.publicCertificate = publicCertificate;
    }

    public int getTenantID() {
        return tenantID;
    }

    public void setTenantID(int tenantID) {
        this.tenantID = tenantID;
    }

    public String getUsername() {
        return certificateMetaInfo.getUsername();
    }

    public void setUsername(String username) {
        certificateMetaInfo.setUsername(username);
    }

    public Date getExpiaryDate() {
        return certificateMetaInfo.getExpiryDate();
    }

    public void setExpiaryDate(Date expiaryDate) {
        certificateMetaInfo.setExpiryDate(expiaryDate);
    }

    public String getStatus() {
        return certificateMetaInfo.getStatus();
    }

    public void setStatus(String status) {
        certificateMetaInfo.setStatus(status);
    }

    public String getSerialNo() {
        return certificateMetaInfo.getSerialNo();
    }

    public void setSerialNo(String serialNo) {
        certificateMetaInfo.setSerialNo(serialNo);
    }

    public CertificateMetaInfo getCertificateMetaInfo() {
        return certificateMetaInfo;
    }
}
