package org.wso2.carbon.identity.totp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.totp.TOTPDTO;
import org.wso2.carbon.identity.totp.exception.TOTPException;
import org.wso2.carbon.identity.totp.TOTPKeyGenerator;

//user service as package
public class TOTPAdminService {

    private static Log log = LogFactory.getLog(TOTPAdminService.class);
    
    public String initTOTP(String username) throws TOTPException {
        TOTPDTO totpdto = null;
        try {
            totpdto = TOTPKeyGenerator.getInstance().generateTOTPKeyLocal(username);
            return totpdto.getQRCodeURL();
        } catch (TOTPException e) {
            log.error("TOTPAdminService failed to generateTOTP key",e);
            throw new TOTPException("TOTPAdminService failed to generateTOTP key",e);
        }
    }
    
    public boolean resetTOTP(String username) throws TOTPException {
        
        return TOTPKeyGenerator.getInstance().resetLocal(username);
    }

}
