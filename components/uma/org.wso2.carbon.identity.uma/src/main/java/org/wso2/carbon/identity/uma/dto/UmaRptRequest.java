package org.wso2.carbon.identity.uma.dto;

import org.wso2.carbon.identity.uma.beans.authz.ClaimTokenBean;
import org.wso2.carbon.identity.uma.beans.authz.UmaRptRequestPayloadBean;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class UmaRptRequest extends UmaRequest{

    private UmaRptRequestPayloadBean requestPayloadBean;

    private String resourceId;

    private String[] requestedScopes;


    public UmaRptRequest(HttpServletRequest request, UmaRptRequestPayloadBean payloadBean){
        super(request);
        requestPayloadBean = payloadBean;

    }

    public UmaRptRequestPayloadBean getRequestPayloadBean() {
        return requestPayloadBean;
    }

    public void setRequestPayloadBean(UmaRptRequestPayloadBean requestPayloadBean) {
        this.requestPayloadBean = requestPayloadBean;
    }

    public String getPermissionTicket() {
        return (requestPayloadBean != null) ? requestPayloadBean.getTicket() : null;
    }


    public String getRpt() {
       return (requestPayloadBean != null) ? requestPayloadBean.getRpt() : null;
    }

    public List<ClaimTokenBean> getClaimTokens() {
        return (requestPayloadBean != null) ? requestPayloadBean.getClaim_tokens() : null;
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
}
