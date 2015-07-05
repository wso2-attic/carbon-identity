package org.wso2.carbon.identity.uma.dto.context;

import org.wso2.carbon.identity.uma.dto.UmaRptRequest;
import org.wso2.carbon.identity.uma.dto.UmaRptResponse;

public class UmaAuthzMessageContext extends UmaMessageContext {

    private String resourceId;

    private String[] requestedScopes;

    private String authorizedUser;

    private long validityPeriod;


    public UmaAuthzMessageContext(UmaRptRequest umaRptRequest) {
        super(umaRptRequest);
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String[] getRequestedScopes() {
        return requestedScopes;
    }

    public void setRequestedScopes(String[] requestedScopes) {
        this.requestedScopes = requestedScopes;
    }

    public UmaRptRequest getUmaRptRequest() {
        return (UmaRptRequest)umaRequest;
    }

    public void setUmaRptRequest(UmaRptRequest umaRptRequest) {
        super.setUmaRequest(umaRptRequest);
    }

    public UmaRptResponse getUmaRptResponse() {
        return (UmaRptResponse)umaResponse;
    }

    public void setUmaRptResponse(UmaRptResponse umaRptResponse) {
        super.setUmaResponse(umaRptResponse);
    }

    public String getAuthorizedUser() {
        return authorizedUser;
    }

    public void setAuthorizedUser(String authorizedUser) {
        this.authorizedUser = authorizedUser;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }
}
