package org.wso2.carbon.identity.mgt.handler.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.IdentityErrorMsgContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimsDO;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class AccountLockEventHandler extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(AccountLockEventHandler.class);

    // the event types registered in this handler
    protected ArrayList<String> registeredEventList;

    @Override
    public void init() {

        registeredEventList = new ArrayList<String>() {{
            add(IdentityMgtConstants.Event.PRE_AUTHENTICATION);
        }};
    }


    @Override
    public boolean isRegistered(IdentityMgtEvent event) {

        HashMap<String, Object> properties = event.getEventProperties();
        IdentityMgtConfig config = (IdentityMgtConfig) properties.get(IdentityMgtConstants.EventProperty.IDENTITY_MGT_CONFIG);

        if (config.isAccountLockEnable()) {
            //check whether the event type is registered to this handler
            for (String eventName : registeredEventList) {
                if (eventName.equals(event.getEventName())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws UserStoreException {

        HashMap<String, Object> properties = identityMgtEvent.getEventProperties();

        String userName = (String) properties.get(IdentityMgtConstants.EventProperty.USER_NAME);
        UserStoreManager userStoreManager = (UserStoreManager) properties.get(IdentityMgtConstants.EventProperty.USER_STORE_MANAGER);
        UserIdentityDataStore module = (UserIdentityDataStore) properties.get(IdentityMgtConstants.EventProperty.MODULE);
        IdentityMgtConfig config = (IdentityMgtConfig) properties.get(IdentityMgtConstants.EventProperty.IDENTITY_MGT_CONFIG);

        IdentityUtil.clearIdentityErrorMsg();

        if (!config.isEnableAuthPolicy()) {
            return true;
        }

        String domainName = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
        String usernameWithDomain = UserCoreUtil.addDomainToName(userName, domainName);
        boolean isUserExistInCurrentDomain = userStoreManager.isExistingUser(usernameWithDomain);

        if (!isUserExistInCurrentDomain) {

            IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(UserCoreConstants.ErrorCode.USER_DOES_NOT_EXIST);
            IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);

            if (log.isDebugEnabled()) {
                log.debug("Username :" + userName + "does not exists in the system, ErrorCode :" + UserCoreConstants.ErrorCode.USER_DOES_NOT_EXIST);
            }
            if (config.isAuthPolicyAccountExistCheck()) {
                throw new UserStoreException(UserCoreConstants.ErrorCode.USER_DOES_NOT_EXIST);
            }
        } else {

            UserIdentityClaimsDO userIdentityDTO = module.load(userName, userStoreManager);

            // if the account is locked, should not be able to log in
            if (userIdentityDTO != null && userIdentityDTO.isAccountLocked()) {

                // If unlock time is specified then unlock the account.
                if ((userIdentityDTO.getUnlockTime() != 0) && (System.currentTimeMillis() >= userIdentityDTO.getUnlockTime())) {

                    userIdentityDTO.setAccountLock(false);
                    userIdentityDTO.setUnlockTime(0);

                    try {
                        module.store(userIdentityDTO, userStoreManager);
                    } catch (IdentityException e) {
                        //throw new UserStoreException("Error while saving user : " + userName, e);
                    }
                } else {
                    IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(UserCoreConstants.ErrorCode.USER_IS_LOCKED,
                            userIdentityDTO.getFailAttempts(), config.getAuthPolicyMaxLoginAttempts());
                    IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);
                    String errorMsg = "User account is locked for user : " + userName
                            + ". cannot login until the account is unlocked ";
                    //log.warn(errorMsg);

                    throw new UserStoreException(UserCoreConstants.ErrorCode.USER_IS_LOCKED + " "
                            + errorMsg);
                }

            }
        }


        return true;
    }
}
