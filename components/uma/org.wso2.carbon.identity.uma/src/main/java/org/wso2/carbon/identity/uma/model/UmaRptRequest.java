package org.wso2.carbon.identity.uma.model;

import org.wso2.carbon.identity.uma.beans.ClaimTokenBean;
import org.wso2.carbon.identity.uma.beans.UmaRptRequestPayloadBean;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UmaRptRequest {
    private static final String TENANT_DOMAIN = "tenantDomain";

    private HttpServletRequest httpServletRequest;

    private UmaRptRequestPayloadBean requestPayloadBean;

    private String permissionTicket;

    private String rpt;

    private String tenantDomain;

    private RequestParameter[] requestParameters;

    private List<ClaimTokenBean> claimTokens;

    private String resourceId;

    private String[] requestedScopes;


    public UmaRptRequest(HttpServletRequest request, UmaRptRequestPayloadBean payloadBean){
        this.httpServletRequest = request;
        requestPayloadBean = payloadBean;

        // populate the request params from the RPT Request payload bean
        if (requestPayloadBean != null){
            this.permissionTicket = requestPayloadBean.getTicket();
            this.rpt = requestPayloadBean.getRpt();
            this.claimTokens = requestPayloadBean.getClaim_tokens();
        }

        // set the tenant domain from the request parameters
        tenantDomain = request.getParameter(TENANT_DOMAIN);

        // Store all request parameters
        if (request.getParameterNames() != null) {
            List<RequestParameter> requestParameterList = new ArrayList<RequestParameter>();
            while (request.getParameterNames().hasMoreElements()) {
                String key = request.getParameterNames().nextElement();
                String value = request.getParameter(key);
                requestParameterList.add(new RequestParameter(key, value));
            }
            requestParameters =
                    requestParameterList.toArray(new RequestParameter[requestParameterList.size()]);
        }
    }

    public UmaRptRequestPayloadBean getRequestPayloadBean() {
        return requestPayloadBean;
    }

    public void setRequestPayloadBean(UmaRptRequestPayloadBean requestPayloadBean) {
        this.requestPayloadBean = requestPayloadBean;
    }

    public String getPermissionTicket() {
        return permissionTicket;
    }

    public void setPermissionTicket(String permissionTicket) {
        this.permissionTicket = permissionTicket;
    }

    public String getRpt() {
        return rpt;
    }

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public RequestParameter[] getRequestParameters() {
        return requestParameters;
    }

    public List<ClaimTokenBean> getClaimTokens() {
        return claimTokens;
    }

    public String getTenantDomain() {
        return tenantDomain;
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
