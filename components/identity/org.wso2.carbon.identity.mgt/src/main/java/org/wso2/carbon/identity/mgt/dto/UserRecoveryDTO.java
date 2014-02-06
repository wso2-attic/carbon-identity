package org.wso2.carbon.identity.mgt.dto;

import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * This object contains the information of the created user account. This
 * information can be sent to the user to complete the user registration
 * process. Information are such as the temporary password, confirmation code
 * etc
 * 
 * @author sga
 * 
 */
public class UserRecoveryDTO {


    private String userId;
    private String tenantDomain;
    private int tenantId;
	private String temporaryPassword;
	private String confirmationCode;
	private String notificationType;
	private String notification;

	public UserRecoveryDTO(UserDTO userDTO) {
		this.userId = userDTO.getUserId();
        this.tenantDomain = userDTO.getTenantDomain();
        this.tenantId = userDTO.getTenantId();
	}

    public UserRecoveryDTO(String userId) {
        this.userId = userId;
        this.tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        this.tenantId = MultitenantConstants.SUPER_TENANT_ID;
    }


	/**
	 * Returns the temporary password of the created account
	 * @return
	 */
	public String getTemporaryPassword() {
		return temporaryPassword;
	}

	public UserRecoveryDTO setTemporaryPassword(String temporaryPassword) {
		this.temporaryPassword = temporaryPassword;
		return this;
	}

	/**
	 * Returns the confirmation code for the created account
	 * @return
	 */
	public String getConfirmationCode() {
		return confirmationCode;
	}

	public UserRecoveryDTO setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
		return this;
	}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }
}
