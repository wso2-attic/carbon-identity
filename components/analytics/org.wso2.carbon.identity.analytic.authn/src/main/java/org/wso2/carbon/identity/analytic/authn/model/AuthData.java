package org.wso2.carbon.identity.analytic.authn.model;

import java.util.Date;

public class AuthData {

    private String id;
    private String username;
    private String userstoreDomain;
    private String tenantDomain;
    private String remoteIp;
    private String authType;
    private String serviceProvider;
    private Date timestamp;
    private AuthnStep[] authnSteps;
    private boolean authnSuccess;

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public String getUserstoreDomain() {

        return userstoreDomain;
    }

    public void setUserstoreDomain(String userstoreDomain) {

        this.userstoreDomain = userstoreDomain;
    }

    public String getTenantDomain() {

        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {

        this.tenantDomain = tenantDomain;
    }

    public String getRemoteIp() {

        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {

        this.remoteIp = remoteIp;
    }

    public String getAuthType() {

        return authType;
    }

    public void setAuthType(String authType) {

        this.authType = authType;
    }

    public String getServiceProvider() {

        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {

        this.serviceProvider = serviceProvider;
    }

    public Date getTimestamp() {

        return timestamp;
    }

    public void setTimestamp(Date timestamp) {

        this.timestamp = timestamp;
    }

    public AuthnStep[] getAuthnSteps() {

        return authnSteps;
    }

    public void setAuthnSteps(AuthnStep[] authnSteps) {

        this.authnSteps = authnSteps;
    }

    public boolean isAuthnSuccess() {

        return authnSuccess;
    }

    public void setAuthnSuccess(boolean authnSuccess) {

        this.authnSuccess = authnSuccess;
    }
}
