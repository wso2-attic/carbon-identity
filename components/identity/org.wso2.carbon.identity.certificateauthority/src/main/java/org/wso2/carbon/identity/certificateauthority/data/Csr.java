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

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.util.Date;


public class Csr {
    private String department;
    private String city;
    private String country;
    private PKCS10CertificationRequest csrRequest;
    private String state;
    private int userStoreId;
    private CsrMetaInfo csrMetaInfo;
    private int tenantID;

    public Csr(String commonName, String organization, PKCS10CertificationRequest csrRequest, String serialNo, String status, String userName, int tenantID, int userStoreId, Date reqestedDate) {
        csrMetaInfo = new CsrMetaInfo(serialNo, commonName, organization, status, reqestedDate, userName);
        this.csrRequest = csrRequest;
        this.tenantID = tenantID;
        this.userStoreId = userStoreId;
    }

    public Csr(String commonName, String department, String organization, String city, String state, String country, PKCS10CertificationRequest csrRequest, String serialNo, String status, String username, int tenantID, int userStoreId, Date reqestedDate) {

        csrMetaInfo = new CsrMetaInfo(serialNo, commonName, organization, status, reqestedDate, username);
        this.department = department;
        this.city = city;
        this.state = state;
        this.country = country;
        this.csrRequest = csrRequest;
        this.tenantID = tenantID;
        this.userStoreId = userStoreId;
    }

    public int getUserStoreId() {
        return userStoreId;
    }

    public void setUserStoreId(int userStoreId) {
        this.userStoreId = userStoreId;
    }

    public String getUserName() {
        return csrMetaInfo.getUserName();
    }

    public void setUserName(String userName) {
        csrMetaInfo.setUserName(userName);
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public PKCS10CertificationRequest getCsrRequest() {
        return csrRequest;
    }

    public void setCsrRequest(PKCS10CertificationRequest csrRequest) {
        this.csrRequest = csrRequest;
    }

    public int getTenantID() {
        return this.tenantID;
    }

    public void setTenantID(int tenantID) {
        this.tenantID = tenantID;
    }

    public Date getReqestedDate() {
        return csrMetaInfo.getRequestedDate();
    }

    public void setReqestedDate(Date reqestedDate) {
        csrMetaInfo.getRequestedDate();
    }

    public String getSerialNo() {
        return csrMetaInfo.getSerialNo();
    }

    public void setSerialNo(String serialNo) {
        csrMetaInfo.setSerialNo(serialNo);
    }

    public String getCommonName() {
        return csrMetaInfo.getCommonName();
    }

    public void setCommonName(String commonName) {
        csrMetaInfo.setCommonName(commonName);
    }

    public String getOrganization() {
        return csrMetaInfo.getOrganization();
    }

    public void setOrganization(String organization) {
        csrMetaInfo.setOrganization(organization);
    }

    public String getStatus() {
        return csrMetaInfo.getStatus();
    }

    public void setStatus(String status) {
        csrMetaInfo.setStatus(status);
    }

}
