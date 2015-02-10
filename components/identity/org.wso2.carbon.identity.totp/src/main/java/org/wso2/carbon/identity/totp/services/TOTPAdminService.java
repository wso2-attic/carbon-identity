package org.wso2.carbon.identity.totp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.totp.TOTPDTO;
import org.wso2.carbon.identity.totp.TOTPKeyGenerator;
import org.wso2.carbon.identity.totp.exception.TOTPException;


public class TOTPAdminService {

    private static Log log = LogFactory.getLog(TOTPAdminService.class);

    /**
     * Generate TOTP Token for the give user
     *
     * @param username username of the user
     * @return
     * @throws TOTPException
     */
    public String initTOTP(String username) throws TOTPException {
        TOTPDTO totpdto = null;
        try {
            totpdto = TOTPKeyGenerator.getInstance().generateTOTPKeyLocal(username);
            return totpdto.getQRCodeURL();
        } catch (TOTPException e) {
            log.error("TOTPAdminService failed to generateTOTP key for the user : " + username, e);
            throw new TOTPException("TOTPAdminService failed to generateTOTP key for the user : " + username, e);
        }
    }

    /**
     * reset TOTP credentials of the user
     *
     * @param username of the user
     * @return
     * @throws TOTPException
     */
    public boolean resetTOTP(String username) throws TOTPException {
        return TOTPKeyGenerator.getInstance().resetLocal(username);
    }

}
