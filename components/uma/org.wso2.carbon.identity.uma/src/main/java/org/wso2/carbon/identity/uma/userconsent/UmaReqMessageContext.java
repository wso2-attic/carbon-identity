package org.wso2.carbon.identity.uma.userconsent;

import org.wso2.carbon.identity.uma.model.UmaRptRequest;
import org.wso2.carbon.identity.uma.model.UmaRptResponse;

public class UmaReqMessageContext {

    private UmaRptRequest umaRptRequest;

    private UmaRptResponse umaRptResponse;

    private String authorizedUser;

    private String tenantDomain;

    private int tenantID;

    private long validityPeriod;


    public UmaRptRequest getUmaRptRequest() {
        return umaRptRequest;
    }

    public void setUmaRptRequest(UmaRptRequest umaRptRequest) {
        this.umaRptRequest = umaRptRequest;
    }

    public UmaRptResponse getUmaRptResponse() {
        return umaRptResponse;
    }

    public void setUmaRptResponse(UmaRptResponse umaRptResponse) {
        this.umaRptResponse = umaRptResponse;
    }

    public String getAuthorizedUser() {
        return authorizedUser;
    }

    public void setAuthorizedUser(String authorizedUser) {
        this.authorizedUser = authorizedUser;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public int getTenantID() {
        return tenantID;
    }

    public void setTenantID(int tenantID) {
        this.tenantID = tenantID;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }
}
