package org.wso2.carbon.identity.user.profile.mgt.listener;

import org.wso2.carbon.identity.base.IdentityValidationUtil;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.Map;

public class ProfileMgtEventListener extends AbstractIdentityUserOperationEventListener{

    @Override
    public int getExecutionOrderId() {
        return 51;
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValues(String userName, Map<String, String> claims, String profileName,
                                           UserStoreManager userStoreManager) throws UserStoreException {
    //The following black listed patterns contain possible invalid inputs for profile which could be used for a stored
    //XSS attack.
        if(!IdentityValidationUtil.isValid(profileName,new String[]{"ALPHANUMERICS_ONLY","DIGITS_ONLY"},new String[]{
                "WHITESPACE_EXISTS","URI_RESERVED_EXISTS","HTML_META_EXISTS","XML_META_EXISTS","REGEX_META_EXISTS",
                "URL"})){
            throw new UserStoreException("profile name contains invalid characters!");
        }
        return true;
    }
}
