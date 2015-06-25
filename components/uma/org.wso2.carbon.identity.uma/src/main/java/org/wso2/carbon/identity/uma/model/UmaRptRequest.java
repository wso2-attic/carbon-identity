package org.wso2.carbon.identity.uma.model;

import org.wso2.carbon.identity.uma.beans.UmaRptRequestPayloadBean;

import java.util.Map;

public class UmaRptRequest {

    private UmaRptRequestPayloadBean requestPayloadBean;

    private String permissionTicket;

    private String rpt;

    private Map<String, String> claims;


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

    public Map<String, String> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, String> claims) {
        this.claims = claims;
    }
}
