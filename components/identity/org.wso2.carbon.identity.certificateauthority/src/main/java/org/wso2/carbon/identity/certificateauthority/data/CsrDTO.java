package org.wso2.carbon.identity.certificateauthority.data;

public class CsrDTO {

    private String department;
    private String city;
    private String country;
    private String csrRequest;
    private String state;
    private String userStoreDomain;
    private CsrMetaInfo csrMetaInfo;
    private int tenantID;

    public CsrDTO(String department, String city, String country, String csrRequest, String state, String userStoreDomain, CsrMetaInfo csrMetaInfo, int tenantID) {
        this.department = department;
        this.city = city;
        this.country = country;
        this.csrRequest = csrRequest;
        this.state = state;
        this.userStoreDomain = userStoreDomain;
        this.csrMetaInfo = csrMetaInfo;
        this.tenantID = tenantID;
    }

    public String getDepartment() {
        return department;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getCsrRequest() {
        return csrRequest;
    }

    public String getState() {
        return state;
    }

    public String getUserStoreDomain() {
        return userStoreDomain;
    }

    public CsrMetaInfo getCsrMetaInfo() {
        return csrMetaInfo;
    }

    public int getTenantID() {
        return tenantID;
    }
}
