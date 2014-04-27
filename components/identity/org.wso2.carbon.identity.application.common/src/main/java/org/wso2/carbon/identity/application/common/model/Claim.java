package org.wso2.carbon.identity.application.common.model;

public class Claim {

    private String claimUri;
    private int claimId;
    private boolean requested;

    /**
     * 
     * @return
     */
    public String getClaimUri() {
        return claimUri;
    }

    /**
     * 
     * @param claimUri
     */
    public void setClaimUri(String claimUri) {
        this.claimUri = claimUri;
    }

    /**
     * 
     * @return
     */
    public boolean isRequested() {
        return requested;
    }

    /**
     * 
     * @param requested
     */
    public void setRequested(boolean requested) {
        this.requested = requested;
    }

    /**
     * 
     * @return
     */
	public int getClaimId() {
		return claimId;
	}

	/**
	 * 
	 * @param claimId
	 */
	public void setClaimId(int claimId) {
		this.claimId = claimId;
	}
}
