package org.wso2.carbon.identity.uma.model;

public class UmaRptResponse {

    private String rptType;
    private String RPT;
    private boolean error;
    private String errorCode;
    private String errorMsg;
    private long expiryTime;
    private long expiraryTimeInMillis;

    public String getRptType() {
        return rptType;
    }

    public void setRptType(String rptType) {
        this.rptType = rptType;
    }

    public String getRPT() {
        return RPT;
    }

    public void setRPT(String RPT) {
        this.RPT = RPT;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public long getExpiraryTimeInMillis() {
        return expiraryTimeInMillis;
    }

    public void setExpiraryTimeInMillis(long expiraryTimeInMillis) {
        this.expiraryTimeInMillis = expiraryTimeInMillis;
    }
}
